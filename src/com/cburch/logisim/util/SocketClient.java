/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/
package com.cburch.logisim.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socket client to talk to the binder.
 *
 * @author christian.mueller@heig-vd.ch
 */
public class SocketClient {

	final static Logger logger = LoggerFactory.getLogger(SocketClient.class);

	private static ServerSocket server = null;

	private boolean connected = false;

	private Socket socket;

	private BufferedReader socket_reader;
	private PrintWriter socket_writer;

	public SocketClient() {

		if (server == null) {
			try {
				server = new ServerSocket(0);
			} catch (IOException e) {
				logger.error("Cannot create server socket");
				e.printStackTrace();
				return;
			}
		}
	}

	public int getServerPort() {
		if (server != null) {
			return server.getLocalPort();
		}

		return 0;
	}

	public Socket getSocket() {
		return socket;
	}

	public Boolean isConnected() {
		return connected;
	}

	public String receive() {

		try {
			return socket_reader.readLine();
		} catch (Exception e) {
			logger.error("Cannot read from socket : {}", e.getMessage());
			return null;
		}
	}

	public void send(String message) {

		try {
			socket_writer.println(message);
		} catch (Exception e) {
			logger.error("Cannot write {} to socket {}", message,
					e.getMessage());
		}
	}

	public void start() {

		try {
			socket = server.accept();

			socket_reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			socket_writer = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(socket.getOutputStream())), true);

			connected = true;
			return;

		} catch (IOException e) {
			logger.error("Error at accepting new client");
		}
		connected = false;
	}

	public void stop() {
		if (!isConnected())
			return;

		try {
			socket.close();
			connected = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}