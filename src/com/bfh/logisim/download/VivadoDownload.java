package com.bfh.logisim.download;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.settings.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class VivadoDownload {

    public static boolean Download(Settings MySettings,
                                   BoardInformation BoardInfo, String scriptPath, String UcfPath,
                                   String ProjectPath, String SandboxPath, FPGAReport MyReporter) {
        return false;
    }

    public static boolean GenerateScripts(FPGAReport MyReporter,
                                             String projectPath, String scriptPath, String ucfPath,
                                             Netlist rootNetlist, MappableResourcesContainer mapInfo,
                                             BoardInformation boardInfo, ArrayList<String> entities,
                                             ArrayList<String> architectures, String HDLType,
                                             boolean writeToFlash) {

        // create project TCL script
        File tclCreateProjectFile = FileWriter.GetFilePointer(scriptPath, TCL_SCRIPT_FILE, MyReporter);
        if (tclCreateProjectFile == null) {
            tclCreateProjectFile = new File(scriptPath + TCL_SCRIPT_FILE);
            return tclCreateProjectFile.exists();
        }
        ArrayList<String> contents = new ArrayList<String>();
        contents.add("create_project vivadoproject " + scriptPath + " vivadoproject");
        contents.add("set_property part " +
                boardInfo.fpga.getPart() +
                boardInfo.fpga.getPackage() +
                boardInfo.fpga.getSpeedGrade() +
                " [current_project]");
        contents.add("set_property target_language VHDL [current_project]");
        if (!FileWriter.WriteContents(tclCreateProjectFile, contents, MyReporter))
            return false;
        contents.clear();

        return false;
    }

    private final static String TCL_SCRIPT_FILE = "createProject.tcl";
}
