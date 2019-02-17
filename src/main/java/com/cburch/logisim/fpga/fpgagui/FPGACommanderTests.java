package com.cburch.logisim.fpga.fpgagui;


import com.cburch.logisim.fpga.download.Download;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardReaderClass;
import com.cburch.logisim.proj.Project;

public class FPGACommanderTests extends FPGACommanderBase {

	private String circuitTestName = null;
	private String circuitPathMap = null;
	private boolean writeToFlash = false;
	private double TickFrequency;

	public FPGACommanderTests(Project project, String pathMap, String circuit, String boardName, double frequency) {
		MyReporter = new FPGAReportNoGui();
		MyProject = project;
		circuitTestName = circuit;
		circuitPathMap = pathMap;
		TickFrequency = frequency;
		MyBoardInformation = new BoardReaderClass("url:resources/logisim/boards/" +
				boardName + ".xml").GetBoardInformation();
		MyBoardInformation.setBoardName(boardName);
	}


	public boolean StartTests() {
		Download Downloader = new Download(MyProject,
				circuitTestName,
				TickFrequency,
                MyReporter,
                MyBoardInformation,
                circuitPathMap,
                writeToFlash,
                false,
                false);
		return Downloader.runtty();
	}

}
