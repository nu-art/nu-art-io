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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class NetworkServerTransceiver
	extends NetworkTransceiver {

	private ServerSocket serverSocket;

	private String remoteAddress;

	public NetworkServerTransceiver(int serverPort, String name, PacketSerializer packetSerializer) {
		super(name, packetSerializer, serverPort);
	}

	@Override
	protected SocketWrapper connectImpl()
		throws Exception {

		serverSocket = new ServerSocket(port);
		Socket socket = serverSocket.accept();
		remoteAddress = socket.getInetAddress().getHostAddress();
		if(remoteAddress == null)
			remoteAddress = "127.0.0.1";

		return new WifiSocketWrapper(socket);
	}

	public void disconnectImpl() {
		try {
			if (serverSocket != null)
				serverSocket.close();
		} catch (IOException e) {
			notifyError(e);
		}
	}

	@Override
	protected String extraLog() {
		return "Port: " + port;
	}

	@Override
	public String getRemoteAddress() {
		return remoteAddress;
	}
}