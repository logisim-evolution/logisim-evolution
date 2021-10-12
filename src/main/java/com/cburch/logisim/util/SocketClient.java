/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

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

  static final Logger logger = LoggerFactory.getLogger(SocketClient.class);

  private static ServerSocket server = null;

  private boolean connected = false;

  private Socket socket;

  private BufferedReader socketReader;
  private PrintWriter socketWriter;

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
      return socketReader.readLine();
    } catch (Exception e) {
      logger.error("Cannot read from socket : {}", e.getMessage());
      return null;
    }
  }

  public void send(String message) {

    try {
      socketWriter.println(message);
    } catch (Exception e) {
      logger.error("Cannot write {} to socket {}", message, e.getMessage());
    }
  }

  public void start() {

    try {
      socket = server.accept();

      socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      socketWriter =
          new PrintWriter(
              new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

      connected = true;
      return;

    } catch (IOException e) {
      logger.error("Error at accepting new client");
    }
    connected = false;
  }

  public void stop() {
    if (!isConnected()) return;

    try {
      socket.close();
      connected = false;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
