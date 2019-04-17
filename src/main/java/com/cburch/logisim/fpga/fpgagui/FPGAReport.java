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

package com.cburch.logisim.fpga.fpgagui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* TODO: Add FPGA-REPORTER to console or log file */
public abstract class FPGAReport {

  static final Logger logger = LoggerFactory.getLogger(FPGAReport.class);

  public FPGAReport() {}

  public void AddErrorIncrement(String Message) {
    logger.error(Message);
  }

  public void AddError(Object Message) {
    if (Message instanceof String) logger.error((String) Message);
  }

  public void AddFatalError(String Message) {
    logger.error(Message);
  }

  public void AddSevereError(String Message) {
    logger.warn(Message);
  }

  public void AddInfo(String Message) {
    logger.info(Message);
  }

  public void AddSevereWarning(String Message) {
    logger.error(Message);
  }

  public void AddWarningIncrement(String Message) {
    logger.warn(Message);
  }

  public void AddWarning(Object Message) {
    if (Message instanceof String) logger.warn((String) Message);
  }

  public void ClsScr() {}

  public void print(String Message) {
    logger.info(Message);
  }
}
