/*
 * cyborg-core is an extendable  module based framework for Android.
 *
 * Copyright (C) 2018  Adam van der Kruk aka TacB0sS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nu.art.io;

import com.nu.art.belog.Logger;
import com.nu.art.core.tools.ArrayTools;
import com.nu.art.core.utils.DebugFlags;
import com.nu.art.core.utils.DebugFlags.DebugFlag;
import com.nu.art.core.utils.JavaHandler;

import java.io.IOException;
import java.net.SocketException;

import static com.nu.art.io.ConnectionState.Connected;
import static com.nu.art.io.ConnectionState.Connecting;
import static com.nu.art.io.ConnectionState.Idle;

public abstract class BaseTransceiver
	extends Logger {

	public static final DebugFlag DebugFlag = DebugFlags.createFlag(BaseTransceiver.class);

	protected SocketWrapper socket;

	private ConnectionState state = Idle;

	protected final String name;

	protected final PacketSerializer packetSerializer;

	private final JavaHandler receiver;

	private final JavaHandler transmitter;

	private boolean listen = true;

	private boolean oneShot = false;

	private TransceiverListener[] listeners = {};

	private Runnable connectAndListen = new Runnable() {
		@Override
		public void run() {
			try {
				listen = true;
				while (listen) {
					logDebug("live");

					setState(Connecting);
					receiver.remove(null);

					socket = connectImpl();
					setState(Connected);

					while (socket.isConnected()) {
						try {
							logDebug("waiting for packet");
							processPacket();
						} catch (SocketException e) {
							logError("SocketException");

							break;
						} catch (IOException e) {
							logError("IOException");

							try {
								socket.close();
							} catch (IOException e1) {
								notifyError(e);
							}
						} catch (Exception e) {
							notifyError(e);
							logError("inner Exception");
						}
					}
					if (oneShot)
						break;
				}
			} catch (Exception e) {
				logError("outer Exception");
				notifyError(e);
			} finally {
				logDebug("died");

				_disconnectImpl();
			}
		}
	};

	private void _disconnectImpl() {
		logInfo("+---+ Disconnecting...");
		setState(ConnectionState.Disconnecting);
		disconnectImpl();
		try {
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			notifyError(e);
		}

		socket = null;
		setState(Idle);
	}

	@Override
	protected boolean isLoggerEnabled() {
		return DebugFlag.isEnabled();
	}

	public final void disconnect() {
		if (state == Idle) {
			logWarning("Cannot disconnect, State is Disconnected");
			return;
		}

		listen = false;
		_disconnectImpl();
	}

	protected void disconnectImpl() {}

	public BaseTransceiver(String name, PacketSerializer packetSerializer) {
		this.name = name;
		setTag(name);
		this.packetSerializer = packetSerializer;
		transmitter = new JavaHandler().start("Tx - " + name);
		receiver = new JavaHandler().start("Rx - " + name);
	}

	public final void setOneShot() {
		oneShot = true;
	}

	public final void sendPacket(final Packet packet) {
		sendPacket(packet, true);
	}

	public final void sendPacket(final Packet packet, final boolean printToLog) {
		transmitter.post(new Runnable() {
			@Override
			public void run() {
				try {
					sendPacketSync(packet, printToLog);
				} catch (IOException e) {
					notifyError(e);
				}
			}
		});
	}

	public void sendPacketSync(Packet packet)
		throws IOException {
		sendPacketSync(packet, true);
	}

	public void sendPacketSync(Packet packet, boolean printToLog)
		throws IOException {
		if (socket == null)
			throw new IOException("Socket is null ignoring packet: " + packet);

		if (printToLog)
			logInfo("Sending packet to remote device: " + packet);

		packetSerializer.serializePacket(socket.getOutputStream(), packet);
	}

	public void connect() {
		if (!isState(Idle))
			return;

		logInfo("Connecting");
		receiver.remove(connectAndListen);
		receiver.post(connectAndListen);
	}

	protected void processPacket()
		throws IOException {
		Packet packet = packetSerializer.extractPacket(socket.getInputStream());
		logDebug("Process packet: " + packet);
		notifyNewPacket(packet);
	}

	public final void addListener(TransceiverListener listener) {
		if (listener == null)
			return;

		listeners = ArrayTools.appendElement(listeners, listener);
	}

	public final void removeListener(TransceiverListener listener) {
		if (listener == null)
			return;

		listeners = ArrayTools.removeElement(listeners, listener);
	}

	protected ConnectionState getState() {
		return state;
	}

	public synchronized boolean isState(ConnectionState state) {
		return this.state == state;
	}

	public final synchronized void setState(ConnectionState newState) {
		if (state == newState)
			return;

		logDebug("State changed: " + state + " => " + newState + ": " + extraLog());
		this.state = newState;
		notifyStateChanged(newState);
	}

	protected String extraLog() {
		return "";
	}

	protected abstract SocketWrapper connectImpl()
		throws Exception;

	protected final void notifyError(Exception e) {
		for (TransceiverListener listener : listeners) {
			listener.onError(e);
		}
	}

	protected final void notifyNewPacket(Packet packet) {
		for (TransceiverListener listener : listeners) {
			listener.onIncomingPacket(packet);
		}
	}

	protected final void notifyStateChanged(ConnectionState state) {
		for (TransceiverListener listener : listeners) {
			listener.onStateChange(state);
		}
	}
}