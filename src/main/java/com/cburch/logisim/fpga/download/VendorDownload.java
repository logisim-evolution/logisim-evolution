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

package com.cburch.logisim.fpga.download;

import com.cburch.logisim.fpga.data.MappableResourcesContainer;

public interface VendorDownload {

  public int GetNumberOfStages();
  /* This handle returns the number of stages to be performed
   * e.g. Sythesys, Place , Route , Bitfile gives 4 stages
   */
  public String GetStageMessage(int stage);
  /* This handle return the string that needs to be shown in the GUI
   *
   */
  public ProcessBuilder PerformStep(int stage);
  /* This handle returns a process builder for all actions for stage <stage>, e.g. Syntesys....
   */
  public boolean readyForDownload();
  /* This handle returns true in case a bitfile exists that can be Downloaded
   */

  public ProcessBuilder DownloadToBoard();
  /* This handle performs the actual download
   */
  public boolean CreateDownloadScripts();
  /* This handle creates all the scripts required to to synthesis P&R bitstream generation
   */
  public void SetMapableResources(MappableResourcesContainer resources);

  public boolean BoardConnected();
  /*
   * This handle returns true if a board is connected
   */
}
