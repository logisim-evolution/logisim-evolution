package com.bfh.logisim.download;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.FPGAClass;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.settings.Settings;

import java.io.File;
import java.util.ArrayList;

public class VivadoDownload {

    public static boolean Download(Settings MySettings,
                                   BoardInformation BoardInfo, String scriptPath, String xdcPath,
                                   String ProjectPath, String SandboxPath, FPGAReport MyReporter) {
        return false;
    }

    public static boolean GenerateScripts(FPGAReport myReporter,
                                             String projectPath, String scriptPath, String xdcPath,
                                             Netlist rootNetlist, MappableResourcesContainer mapInfo,
                                             BoardInformation boardInfo, ArrayList<String> entities,
                                             ArrayList<String> architectures, String HDLType,
                                             boolean writeToFlash) {

        // create project files
        File tclCreateProjectFile = FileWriter.GetFilePointer(scriptPath, CREATE_PROJECT_TCL, myReporter);
        File xdcFile = FileWriter.GetFilePointer(xdcPath, XDC_FILE, myReporter);
        if (tclCreateProjectFile == null || xdcFile == null) {
            tclCreateProjectFile = new File(scriptPath + CREATE_PROJECT_TCL);
            xdcFile = new File(xdcPath, XDC_FILE);
            return tclCreateProjectFile.exists() && xdcFile.exists();
        }

        // fill create project TCL script
        ArrayList<String> contents = new ArrayList<String>();
        contents.add("create_project vivadoproject \"" + scriptPath + File.separator + "vivadoproject\"");
        contents.add("set_property part " +
                boardInfo.fpga.getPart() +
                boardInfo.fpga.getPackage() +
                boardInfo.fpga.getSpeedGrade() +
                " [current_project]");
        contents.add("set_property target_language VHDL [current_project]");

        // add all entities and architectures
        for (String entity : entities) {
            contents.add("add_files \"" + entity + "\"");
        }
        for (String architecture : architectures) {
            contents.add("add_files \"" + architecture + "\"");
        }

        // add xdc constraints
        contents.add("add_files -fileset constrs_1 \"" + xdcFile.getAbsolutePath() + "\"");

        if (!FileWriter.WriteContents(tclCreateProjectFile, contents, myReporter))
            return false;
        contents.clear();

        // fill the UCF file
        contents.addAll(mapInfo.GetFPGAPinLocs(FPGAClass.VendorVivado));
        return FileWriter.WriteContents(xdcFile, contents, myReporter);
    }

    private final static String CREATE_PROJECT_TCL = "vivadoCreateProject.tcl";
    private final static String XDC_FILE = "vivadoConstraints.xdc";
}
