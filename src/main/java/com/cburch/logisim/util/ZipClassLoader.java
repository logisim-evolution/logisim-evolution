/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipClassLoader extends ClassLoader {

  private static class Request {
    final int action;
    final String resource;
    boolean responseSent;
    Object response;

    Request(int action, String resource) {
      this.action = action;
      this.resource = resource;
      this.responseSent = false;
    }

    @SuppressWarnings("unused")
    void ensureDone() {
      var aborted = false;
      synchronized (this) {
        if (!responseSent) {
          aborted = true;
          responseSent = true;
          response = null;
          notifyAll();
        }
      }
      if (aborted && DEBUG >= 1) {
        logger.error("request not handled successfully");
      }
    }

    Object getResponse() {
      synchronized (this) {
        while (!responseSent) {
          try {
            this.wait(1000);
          } catch (InterruptedException ignored) {
          }
        }
        return response;
      }
    }

    void setResponse(Object value) {
      synchronized (this) {
        response = value;
        responseSent = true;
        notifyAll();
      }
    }

    @Override
    public String toString() {
      final var act = action == REQUEST_LOAD ? "load" : action == REQUEST_FIND ? "find" : "act" + action;
      return act + ":" + resource;
    }
  }

  private class WorkThread extends UniquelyNamedThread {
    private final LinkedList<Request> requests = new LinkedList<>();
    private ZipFile zipFile = null;

    WorkThread() {
      super("ZipClassLoader-WorkThread");
    }

    @SuppressWarnings("unused")
    private void ensureZipOpen() {
      if (zipFile == null) {
        try {
          if (DEBUG >= 3) logger.debug("  open ZIP file");
          zipFile = new ZipFile(zipPath);
          if (DEBUG >= 1) logger.debug("  ZIP opened");
        } catch (IOException e) {
          if (DEBUG >= 1) logger.error("  error opening ZIP file");
        }
      }
    }

    @SuppressWarnings("unused")
    private void performFind(Request req) {
      ensureZipOpen();
      Object ret = null;
      try {
        if (zipFile != null) {
          if (DEBUG >= 3) logger.debug("  retrieve ZIP entry");
          final var res = req.resource;
          final var zipEntry = zipFile.getEntry(res);
          if (zipEntry != null) {
            final var url = "jar:" + zipPath.toURI() + "!/" + res;
            ret = new URL(url);
            if (DEBUG >= 3) logger.debug("  found: " + url);
          }
        }
      } catch (Exception ex) {
        if (DEBUG >= 3) logger.error("  error retrieving data");
        ex.printStackTrace();
      }
      req.setResponse(ret);
    }

    @SuppressWarnings("unused")
    private void performLoad(Request req) {
      BufferedInputStream bis = null;
      ensureZipOpen();
      Object ret = null;
      try {
        if (zipFile != null) {
          if (DEBUG >= 3) logger.debug("  retrieve ZIP entry");
          final var zipEntry = zipFile.getEntry(req.resource);
          if (zipEntry != null) {
            if (DEBUG >= 3) logger.debug("  load file");
            final var result = new byte[(int) zipEntry.getSize()];
            bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
            try {
              bis.read(result, 0, result.length);
              ret = result;
            } catch (IOException e) {
              if (DEBUG >= 3) logger.error("  error loading file");
            }
          }
        }
      } catch (Exception ex) {
        if (DEBUG >= 3) logger.error("  error retrieving data");
        ex.printStackTrace();
      } finally {
        if (bis != null) {
          try {
            if (DEBUG >= 3) logger.debug("  close file");
            bis.close();
          } catch (IOException ioex) {
            if (DEBUG >= 3) logger.error("  error closing data");
          }
        }
      }
      req.setResponse(ret);
    }

    @SuppressWarnings("unused")
    @Override
    public void run() {
      try {
        while (true) {
          final var request = waitForNextRequest();
          if (request == null) return;

          if (DEBUG >= 2) logger.debug("processing " + request);
          try {
            switch (request.action) {
              case REQUEST_LOAD:
                performLoad(request);
                break;
              case REQUEST_FIND:
                performFind(request);
                break;
            }
          } finally {
            request.ensureDone();
          }
          if (DEBUG >= 2) logger.debug("processed: " + request.getResponse());
        }
      } catch (Exception t) {
        if (DEBUG >= 3) {
          logger.error("uncaught: ");
          t.printStackTrace();
        }
      } finally {
        if (zipFile != null) {
          try {
            zipFile.close();
            zipFile = null;
            if (DEBUG >= 1) logger.debug("  ZIP closed");
          } catch (IOException e) {
            if (DEBUG >= 1) logger.error("Error closing ZIP file");
          }
        }
      }
    }

    private Request waitForNextRequest() {
      synchronized (bgLock) {
        long start = System.currentTimeMillis();
        while (requests.isEmpty()) {
          long elapse = System.currentTimeMillis() - start;
          if (elapse >= OPEN_TIME) {
            bgThread = null;
            return null;
          }
          try {
            bgLock.wait(OPEN_TIME);
          } catch (InterruptedException ignored) {
          }
        }
        return requests.removeFirst();
      }
    }
  }

  static final Logger logger = LoggerFactory.getLogger(ZipClassLoader.class);

  // This code was posted on a forum by "leukbr" on March 30, 2001.
  // http://forums.sun.com/thread.jspa?threadID=360060&forumID=31
  // I've modified it substantially to include a thread that keeps the file
  // open for OPEN_TIME milliseconds so time isn't wasted continually
  // opening and closing the file.
  private static final int OPEN_TIME = 5000;
  private static final int DEBUG = 0;
  // 0 = no debug messages
  // 1 = open/close ZIP file only
  // 2 = also each resource request
  // 3 = all messages while retrieving resource

  private static final int REQUEST_FIND = 0;

  private static final int REQUEST_LOAD = 1;

  private final File zipPath;
  private final Map<String, Object> classes = new HashMap<>();
  private final Object bgLock = new Object();
  private WorkThread bgThread = null;

  public ZipClassLoader(File zipFile) {
    zipPath = zipFile;
  }

  public ZipClassLoader(String zipFileName) {
    this(new File(zipFileName));
  }

  @SuppressWarnings("unused")
  @Override
  public Class<?> findClass(String className) throws ClassNotFoundException {
    var found = false;
    Object result = null;

    // check whether we have loaded this class before
    synchronized (classes) {
      found = classes.containsKey(className);
      if (found) result = classes.get(className);
    }

    // try loading it from the ZIP file if we haven't
    if (!found) {
      final var resourceName = className.replace('.', '/') + ".class";
      result = request(REQUEST_LOAD, resourceName);

      if (result instanceof byte[] data) {
        if (DEBUG >= 3) logger.debug("  define class");
        result = defineClass(className, data, 0, data.length);
        if (result != null) {
          if (DEBUG >= 3) logger.debug("  class defined");
        } else {
          if (DEBUG >= 3) logger.error("  format error");
          result = new ClassFormatError(className);
        }
      }

      synchronized (classes) {
        classes.put(className, result);
      }
    }

    if (result instanceof Class) {
      return (Class<?>) result;
    } else if (result instanceof ClassNotFoundException classNotFoundEx) {
      throw classNotFoundEx;
    } else if (result instanceof Error error) {
      throw error;
    } else {
      return super.findClass(className);
    }
  }

  @SuppressWarnings("unused")
  @Override
  public URL findResource(String resourceName) {
    if (DEBUG >= 3) logger.debug("findResource " + resourceName);
    final var ret = request(REQUEST_FIND, resourceName);
    return (ret instanceof URL url)
           ? url
           : super.findResource(resourceName);
  }

  private Object request(int action, String resourceName) {
    Request request;
    synchronized (bgLock) {
      if (bgThread == null) { // start the thread if it isn't working
        bgThread = new WorkThread();
        bgThread.start();
      }
      request = new Request(action, resourceName);
      bgThread.requests.addLast(request);
      bgLock.notifyAll();
    }
    return request.getResponse();
  }
}
