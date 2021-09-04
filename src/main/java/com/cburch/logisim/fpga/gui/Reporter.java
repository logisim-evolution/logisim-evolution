/*
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

package com.cburch.logisim.fpga.gui;

import com.cburch.logisim.fpga.designrulecheck.SimpleDRCContainer;
import javax.swing.JProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reporter {

  public static final Reporter Report = new Reporter();
  private static final Logger logger = LoggerFactory.getLogger(Reporter.class);
  private FPGAReportTabbedPane myCommander = null;
  private JProgressBar progress = null;

  public JProgressBar getProgressBar() {
    return progress;
  }

  public void setGuiLogger(FPGAReportTabbedPane gui) {
    myCommander = gui;
  }

  public void setProgressBar(JProgressBar prog) {
    progress = prog;
  }

  public void AddErrorIncrement(String Message) {
    if (myCommander == null)
      logger.error(Message);
    else
      myCommander.AddErrors(new SimpleDRCContainer(Message, SimpleDRCContainer.LEVEL_NORMAL, true));
  }

  public void AddError(Object Message) {
    if (myCommander == null) {
      if (Message instanceof String) logger.error((String) Message);
    } else {
      if (Message instanceof String)
        myCommander.AddErrors(new SimpleDRCContainer(Message, SimpleDRCContainer.LEVEL_NORMAL));
      else myCommander.AddErrors(Message);
    }
  }

  public void addFatalErrorFmt(String fmt, Object... args) {
    AddFatalError(String.format(fmt, args));
  }

  public void AddFatalError(String Message) {
    if (myCommander == null)
      logger.error(Message);
    else
      myCommander.AddErrors(new SimpleDRCContainer(Message, SimpleDRCContainer.LEVEL_FATAL));
  }

  public void AddSevereError(String Message) {
    if (myCommander == null)
      logger.error(Message);
    else
      myCommander.AddErrors(new SimpleDRCContainer(Message, SimpleDRCContainer.LEVEL_SEVERE));
  }

  public void AddInfo(String Message) {
    if (myCommander == null)
      logger.info(Message);
    else
      myCommander.AddInfo(Message);
  }

  public void AddSevereWarning(String Message) {
    if (myCommander == null)
      logger.warn(Message);
    else
      myCommander.AddWarning(new SimpleDRCContainer(Message, SimpleDRCContainer.LEVEL_SEVERE));
  }

  public void AddWarningIncrement(String Message) {
    if (myCommander == null)
      logger.warn(Message);
    else
      myCommander.AddWarning(
          new SimpleDRCContainer(Message, SimpleDRCContainer.LEVEL_NORMAL, true));
  }

  public void AddWarning(Object Message) {
    if (myCommander == null) {
      if (Message instanceof String) logger.warn((String) Message);
    } else {
      if (Message instanceof String)
        myCommander.AddWarning(new SimpleDRCContainer(Message, SimpleDRCContainer.LEVEL_NORMAL));
      else myCommander.AddWarning(Message);
    }
  }

  public void ClsScr() {
    if (myCommander != null)
      myCommander.ClearConsole();
  }

  public void print(String Message) {
    if (myCommander == null)
      logger.info(Message);
    else
      myCommander.AddConsole(Message);
  }
}
