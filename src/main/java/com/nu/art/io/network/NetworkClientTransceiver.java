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

package com.nu.art.io.network;

import com.nu.art.io.PacketSerializer;
import com.nu.art.io.SocketWrapper;

import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkClientTransceiver
	extends NetworkTransceiver {

	private final String serverIpAddress;

	private int timeout = 30000;

	public NetworkClientTransceiver(String serverIpAddress, int serverPort, String name, PacketSerializer packetSerializer) {
		super(name, packetSerializer, serverPort);
		this.serverIpAddress = serverIpAddress;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	protected SocketWrapper connectImpl()
		throws Exception {
		setOneShot();
		logDebug("Connecting on: " + serverIpAddress + ":" + port);
		Socket socket = new Socket();
		socket.bind(null);
		socket.connect((new InetSocketAddress(serverIpAddress, port)), this.timeout);

		return new WifiSocketWrapper(socket);
	}

	@Override
	protected String extraLog() {
		return serverIpAddress + ":" + port;
	}

	@Override
	public String getRemoteAddress() {
		return serverIpAddress;
	}
}
