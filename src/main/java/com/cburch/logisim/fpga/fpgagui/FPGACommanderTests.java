package com.cburch.logisim.fpga.fpgagui;

import java.io.File;

import com.cburch.logisim.fpga.fpgaboardeditor.BoardReaderClass;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.gui.menu.MenuSimulate;
import com.cburch.logisim.proj.Project;

public class FPGACommanderTests extends FPGACommanderBase {

	public boolean StartTests() {
		return false;
	}
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

	/* TODO not fixed yet for no gui 

	@Override
	protected boolean DownLoad(boolean skipVHDL, String CircuitName) {
		if (!skipVHDL ) {
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
		}
		if (VendorSoftwarePresent()) {
			return DownLoadDesign(GenerateHDLOnlySelected(),skipVHDL, CircuitName, writeToFlash, false);
		}

		return false;
	}
	*/
}
