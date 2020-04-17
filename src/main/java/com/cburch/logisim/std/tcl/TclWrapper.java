/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.tcl;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.tools.MessageBox;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TclWrapper create a TCL subprocess runnning the script specified in the TCL component
 * properties. Logisim communicates to this process through a socket. A server is created and
 * listens on a random port. This port is given to the subprocess as an argument who connects to
 * Logisim.
 *
 * @author christian.mueller@heig-vd.ch
 */
public class TclWrapper {

  public enum TclWrapperState {
    STOPPED,
    STARTING,
    RUNNING
  }

  static final Logger logger = LoggerFactory.getLogger(TclWrapper.class);

  private static final String TCL_PATH = System.getProperty("java.io.tmpdir") + "/logisim/tcl/";

  private static final String TCL_RESOURCES_PATH = "/resources/logisim/tcl/";

  private static boolean fileExists = false;

  private Process process;
  private TclComponentData tclConsole;
  private File tclContentFile;

  private TclWrapperState state = TclWrapperState.STOPPED;

  public TclWrapper(TclComponentData tclComp) {
    tclConsole = tclComp;
  }

  public void restart() {
    stop();
    start();
  }

  public void setFile(File file) {
    tclContentFile = file;
  }

  public void start() {

    /* Do not start if already running */
    if (state != TclWrapperState.STOPPED) return;

    tclContentFile =
        tclConsole.getState().getAttributeValue(TclComponentAttributes.CONTENT_FILE_ATTR);

    /* Do not start if Tcl file doesn't exist */
    if (!tclContentFile.isFile()) return;

    /* We are ready to start */
    state = TclWrapperState.STARTING;

    /* Copy TCL wrapper file into system tmp folder */
    if (!fileExists) {
      new File(TCL_PATH).mkdirs();

      try {
        Files.copy(
            this.getClass().getResourceAsStream(TCL_RESOURCES_PATH + "tcl_wrapper.tcl"),
            Paths.get(TCL_PATH + "tcl_wrapper.tcl"),
            StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        logger.error("Cannot copy TCL wrapper file : {}", e.getMessage());
        e.printStackTrace();
      }
      fileExists = true;
    }

    /* Create the TCL process */
    ProcessBuilder builder;
    List<String> command = new ArrayList<String>();

    command.add("tclsh");
    command.add(TCL_PATH + "tcl_wrapper.tcl");
    command.add("" + tclConsole.getTclClient().getServerPort());
    command.add(tclContentFile.getAbsolutePath());

    builder = new ProcessBuilder(command);

    /*
     * We want to run the process from the selected Tcl file, so if some
     * includes happens the path are correct
     */
    builder.directory(tclContentFile.getParentFile());

    /* Redirect error on stdout */
    builder.redirectErrorStream(true);

    /* Run the process */
    try {
      process = builder.start();
    } catch (IOException e) {
      e.printStackTrace();
      logger.error("Cannot run TCL wrapper for TCL console : {}", e.getMessage());

      return;
    }

    /* This thread checks the wrapper started well, it's run from now */
    new Thread(
            new Runnable() {

              @Override
              public void run() {
                /* Through this we can get the process output */
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                try {
                  String errorMessage = "";

                  /* Here we check that the wrapper has correctly started */
                  while ((line = reader.readLine()) != null) {

                    errorMessage += "\n" + line;
                    if (line.contains("TCL_WRAPPER_RUNNING")) {

                      new Thread(
                              new Runnable() {
                                public void run() {
                                  Scanner sc =
                                      new Scanner(new InputStreamReader(process.getInputStream()));
                                  // Commented out because it shouldn't be
                                  // visible to the user
                                  // Debug only??
                                  String nextLine;
                                  while (sc.hasNextLine()) {
                                    nextLine = sc.nextLine();
                                    if (nextLine.length() > 0) System.out.println(nextLine);
                                  }

                                  sc.close();
                                  stop();
                                }
                              })
                          .start();

                      tclConsole.tclWrapperStartCallback();

                      state = TclWrapperState.RUNNING;

                      return;
                    }
                  }

                  MessageBox userInfoBox =
                      new MessageBox(
                          "Error starting TCL wrapper", errorMessage, OptionPane.ERROR_MESSAGE);
                  userInfoBox.show();

                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
            })
        .start();
  }

  public void stop() {
    tclConsole.send("end");
    try {
      tclConsole.getTclClient().getSocket().close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    state = TclWrapperState.STOPPED;
  }
}
