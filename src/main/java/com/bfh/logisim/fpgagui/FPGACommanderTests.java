package com.bfh.logisim.fpgagui;

import java.io.File;

import com.bfh.logisim.fpgaboardeditor.BoardReaderClass;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.gui.menu.MenuSimulate;
import com.cburch.logisim.proj.Project;

public class FPGACommanderTests extends FPGACommanderBase {

	private String circuitTestName = null;
	private String circuitPathMap = null;
	private boolean writeToFlash = false;
	private int defaultFrequencyinTbl = 7;

	public FPGACommanderTests(Project project, String pathMap, String circuit, String boardName) {
		MyReporter = new FPGAReportNoGui();
		MyProject = project;
		circuitTestName = circuit;
		circuitPathMap = pathMap;
		MyBoardInformation = new BoardReaderClass("url:resources/logisim/boards/" +
				boardName + ".xml").GetBoardInformation();
		MyBoardInformation.setBoardName(boardName);
	}

	public boolean StartTests() {
		return DownLoad(false, circuitTestName);
	}

	@Override
	protected boolean DownLoad(boolean skipVHDL, String CircuitName) {
		if (!canDownload() || !skipVHDL ) {
			if (!performDRC(CircuitName,HDLGeneratorFactory.VHDL)) {
				return false;
			}

			if (!MapDesign(CircuitName)) {
				return false;
			}

			ComponentMapParser cmp = new ComponentMapParser(new File(circuitPathMap),
					MyMappableResources, MyBoardInformation);
			cmp.parseFile();

			if (!MapDesignCheckIOs()) {
				return false;
			}

			if (!writeHDL(CircuitName,
					MenuSimulate.SupportedTickFrequencies[defaultFrequencyinTbl])) {
				return false;
			}

			if (canDownload() || skipVHDL) {
				return DownLoadDesign(!canDownload(),skipVHDL,
						CircuitName, writeToFlash, false);
			}
		}

		return false;
	}
}
