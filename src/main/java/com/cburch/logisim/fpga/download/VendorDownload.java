package com.cburch.logisim.fpga.download;

import com.cburch.logisim.fpga.fpgagui.MappableResourcesContainer;

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
