/*
 * Based on code from Digital.
 * Copyright (c) 2021 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class TelnetServer implements InstanceData {
  private final ServerSocket serverSocket;
  private ByteBuffer buffer;
  private final ServerThread serverThread;
  private boolean telnetEscape;
  private ClientThread client;
  private Value lastClock;
  private InstanceState instanceState;

  TelnetServer(int port, int bufferSize) throws IOException {
    buffer = new ByteBuffer(bufferSize);
    serverSocket = new ServerSocket(port);
    serverThread = new ServerThread();
    serverThread.start();
  }

  public int getPort() {
    return serverSocket.getLocalPort();
  }

  public Value setLastClock(Value newClock) {
    final var ret = lastClock;
    lastClock = newClock;
    return ret;
  }

  void send(int value) {
    if (client != null) {
      client.send(value);
    }
  }

  int getData() {
    return buffer.peek();
  }

  void deleteOldest() {
    buffer.delete();
  }

  public void deleteAll() {
    buffer.deleteAll();
  }

  boolean hasData() {
    return buffer.hasData();
  }

  private void setClient(ClientThread client) {
    this.client = client;
  }

  void setTelnetEscape(boolean telnetEscape) {
    this.telnetEscape = telnetEscape;
  }

  public int getBufferSize() {
    return buffer.getBufferSize();
  }

  void setBufferSize(int bufferSize) {
    buffer = new ByteBuffer(bufferSize);
  }

  private void dataReceived(int data) {
    buffer.put((byte) data);
    if (instanceState != null) {
      instanceState.fireInvalidated();
    }
  }

  boolean isDead() {
    return !serverThread.isAlive();
  }

  @Override
  public Object clone() {
    return null;
  }

  public InstanceState getInstanceState() {
    return instanceState;
  }

  public void setInstanceState(InstanceState instanceState) {
    this.instanceState = instanceState;
  }

  private final class ServerThread extends Thread {

    private ServerThread() {
      setDaemon(true);
    }

    @Override
    public void run() {
      try {
        while (true) {
          Socket client = serverSocket.accept();
          ClientThread cl = new ClientThread(client, TelnetServer.this);
          cl.start();
          setClient(cl);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  private static final class ClientThread extends Thread {

    private static final int ECHO = 1;
    private static final int SGA = 3;
    private static final int WILL = 251;
    private static final int WONT = 252;
    private static final int DO = 253;
    private static final int DONT = 254;
    private static final int IAC = 255;

    private final InputStream in;
    private final OutputStream out;
    private final Socket client;
    private final TelnetServer server;

    private ClientThread(Socket client, TelnetServer server) throws IOException {
      setDaemon(true);
      in = client.getInputStream();
      out = client.getOutputStream();
      if (server.telnetEscape) {
        out.write(IAC);
        out.write(WILL);
        out.write(SGA);
        out.write(IAC);
        out.write(WILL);
        out.write(ECHO);
        out.flush();
      }
      this.client = client;
      this.server = server;
    }

    @Override
    public void run() {
      try {
        while (true) {
          int data = in.read();
          if (data < 0) {
            break;
          }
          if (data == IAC && server.telnetEscape) {
            int command = in.read();
            int option = in.read();
          } else {
            server.dataReceived(data);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        client.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void send(int value) {
      try {
        out.write(value);
        out.flush();
      } catch (IOException e) {
        e.printStackTrace();
        // not really a problem
      }
    }
  }

  static class ByteBuffer {
    private final byte[] data;
    private final int size;
    private int inBuffer;
    private int newest;
    private int oldest;

    /**
     * Creates a new instance
     *
     * @param size the size of the buffer
     */
    public ByteBuffer(int size) {
      data = new byte[size];
      this.size = size;
    }

    public int getBufferSize() {
      return size;
    }

    /**
     * Adds a byte at the top of the buffer
     *
     * @param value the byte value
     */
    synchronized public void put(byte value) {
      if (inBuffer < size) {
        data[newest] = value;
        newest = inc(newest);
        inBuffer++;
      }
    }

    /**
     * @return the byte at the tail of the buffer
     */
    synchronized public byte peek() {
      if (inBuffer > 0) {
        return data[oldest];
      } else {
        return -1;
      }
    }

    /**
     * deletes a byte from the tail of the buffer
     */
    synchronized public void delete() {
      if (inBuffer > 0) {
        oldest = inc(oldest);
        inBuffer--;
      }
    }

    /**
     * deletes all buffered data
     */
    synchronized public void deleteAll() {
      oldest = 0;
      newest = 0;
      inBuffer = 0;
    }

    /**
     * @return true if there is data available
     */
    synchronized public boolean hasData() {
      return inBuffer > 0;
    }

    private int inc(int n) {
      n++;
      if (n >= size) {
        n = 0;
      }
      return n;
    }

  }

  /**
   * Simple singleton to hold the server instances. Usage of this singleton allows the telnet client
   * to stay connected also if the simulation is not running.
   */
  public static final class ServerHolder {
    /**
     * The singleton instance
     */
    public static final ServerHolder INSTANCE = new ServerHolder();

    private final HashMap<Integer, TelnetServer> serverMap;

    private ServerHolder() {
      serverMap = new HashMap<>();
    }

    /**
     * Returns a server.
     *
     * @param port the port
     * @return the server
     * @throws IOException IOException
     */
    public TelnetServer getServer(int port, int bufferSize) throws IOException {
      TelnetServer server = serverMap.get(port);
      if (server == null || server.isDead()) {
        server = new TelnetServer(port, bufferSize);
        serverMap.put(port, server);
      } else {
        server.deleteAll();
        server.setBufferSize(bufferSize);
      }
      return server;
    }
  }
}