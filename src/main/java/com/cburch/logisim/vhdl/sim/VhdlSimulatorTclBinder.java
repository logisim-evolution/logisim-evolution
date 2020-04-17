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

package com.cburch.logisim.vhdl.sim;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.tools.MessageBox;
import com.cburch.logisim.util.FileUtil;
import com.cburch.logisim.util.Softwares;
import com.cburch.logisim.vhdl.base.VhdlSimConstants;
import com.cburch.logisim.vhdl.base.VhdlSimConstants.State;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TCL binder is a TCL program creating a socket server. The signals have to be written to the
 * binder who will drive the simulation and send back the output signals.
 *
 * <p>The binder is started when the VHDL simulation is enabled. Once started, it writes a ready
 * flag to his stdout that is catched by Logisim to know the binder is ready. This way we ensure the
 * socket is started before trying to connect.
 *
 * <p>To end the binder, we send an "end" flag through the socket and wait for it to finish. This
 * causes Logisim to hang if the binder doesn't listen to the socker. That can happen when
 * unexpected behavior of the simulation occurs.
 *
 * @author christian.mueller@heig-vd.ch
 */
public class VhdlSimulatorTclBinder {

  static final Logger logger = LoggerFactory.getLogger(VhdlSimulatorTclBinder.class);

  private ProcessBuilder builder;
  private Process process;
  private Boolean running = false;

  private VhdlSimulatorTop vsim = null;

  public VhdlSimulatorTclBinder(VhdlSimulatorTop vs) {
    vsim = vs;
    Init(vs.getSocketClient().getServerPort());
  }

  private void Init(int serverPort) {
    List<String> command = new ArrayList<String>();

    command.add(
        FileUtil.correctPath(Softwares.getQuestaPath()) + Softwares.QUESTA_BIN[Softwares.VSIM]);

    command.add("-c");
    command.add("-do");
    command.add("do ../run.tcl " + serverPort);
    command.add("-errorfile");
    command.add("../questasim_errors.log");

    builder = new ProcessBuilder(command);

    Map<String, String> env = builder.environment();
    env.put("LM_LICENSE_FILE", "1650@eilic01");

    builder.directory(new File(VhdlSimConstants.SIM_PATH + "comp/"));

    /* Redirect error on stdout */
    builder.redirectErrorStream(true);
  }

  public Boolean isRunning() {
    return running;
  }

  public void start() {

    try {
      process = builder.start();
    } catch (IOException e) {
      e.printStackTrace();
      logger.error("Cannot run TCL binder to Questasim : {}", e.getMessage());

      running = false;
      return;
    }

    /* This thread checks the binder started well, it's run from now */
    new Thread(
            new Runnable() {

              @Override
              public void run() {
                /* Through this we can get the process output */
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                try {
                  String errorMessage =
                      "You may disable VHDL simulation in the simulation menu if this occurs again\n\n";

                  /* Here we check that the binder has correctly started */
                  while ((line = reader.readLine()) != null) {

                    /* Here we make sure it is possible to print something */
                    if (vsim.getProject().getFrame() != null) {
                      vsim.getProject().getFrame().getVhdlSimulatorConsole().append(line + "\n");
                    }

                    errorMessage += "\n" + line;
                    if (line.contains("TCL_BINDER_RUNNING")) {
                      running = true;

                      new Thread(
                              new Runnable() {
                                @Override
                                public void run() {
                                  Scanner sc =
                                      new Scanner(new InputStreamReader(process.getInputStream()));
                                  String nextLine;
                                  while (sc.hasNextLine()) {
                                    nextLine = sc.nextLine();
                                    if (nextLine.length() > 0)
                                      if (vsim.getProject().getFrame() != null) {
                                        vsim.getProject()
                                            .getFrame()
                                            .getVhdlSimulatorConsole()
                                            .append(nextLine + "\n");
                                      }
                                  }
                                  sc.close();
                                }
                              })
                          .start();

                      vsim.tclStartCallback();
                      return;
                    }
                  }

                  MessageBox userInfoBox =
                      new MessageBox(
                          "Error starting VHDL simulator", errorMessage, OptionPane.ERROR_MESSAGE);
                  userInfoBox.show();
                  vsim.setState(State.ENABLED);

                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
            })
        .start();
  }

  public void stop() {
    if (!running) return;

    /* We ask the binder to end itself */
    if (vsim.getSocketClient() != null) vsim.getSocketClient().send("end");

    /* Wait for the process to end */
    /*
     * FIXME: this can be a bad idea, it will crash logisim if the binder
     * doesn't end
     */
    // try {
    // process.waitFor();
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // System.err.println(e.getMessage());
    // }

    process.destroy();
    running = false;
  }
}
