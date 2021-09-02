/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.download;

import com.cburch.logisim.fpga.data.MappableResourcesContainer;

public interface VendorDownload {

  int GetNumberOfStages();
  /* This handle returns the number of stages to be performed
   * e.g. Sythesys, Place , Route , Bitfile gives 4 stages
   */
  String GetStageMessage(int stage);
  /* This handle return the string that needs to be shown in the GUI
   *
   */
  ProcessBuilder PerformStep(int stage);
  /* This handle returns a process builder for all actions for stage <stage>, e.g. Syntesys....
   */
  boolean readyForDownload();
  /* This handle returns true in case a bitfile exists that can be Downloaded
   */

  ProcessBuilder DownloadToBoard();
  /* This handle performs the actual download
   */
  boolean CreateDownloadScripts();
  /* This handle creates all the scripts required to to synthesis P&R bitstream generation
   */
  void SetMapableResources(MappableResourcesContainer resources);

  boolean BoardConnected();
  /*
   * This handle returns true if a board is connected
   */
}
