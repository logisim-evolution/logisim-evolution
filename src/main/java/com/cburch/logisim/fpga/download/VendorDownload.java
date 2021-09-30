/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.download;

import com.cburch.logisim.fpga.data.MappableResourcesContainer;

public interface VendorDownload {

  int getNumberOfStages();
  /* This handle returns the number of stages to be performed
   * e.g. Sythesys, Place , Route , Bitfile gives 4 stages
   */
  String getStageMessage(int stage);
  /* This handle return the string that needs to be shown in the GUI
   *
   */
  ProcessBuilder performStep(int stage);
  /* This handle returns a process builder for all actions for stage <stage>, e.g. Syntesys....
   */
  boolean readyForDownload();
  /* This handle returns true in case a bitfile exists that can be Downloaded
   */

  ProcessBuilder downloadToBoard();
  /* This handle performs the actual download
   */
  boolean createDownloadScripts();
  /* This handle creates all the scripts required to to synthesis P&R bitstream generation
   */
  void setMapableResources(MappableResourcesContainer resources);

  boolean isBoardConnected();
  /*
   * This handle returns true if a board is connected
   */
}
