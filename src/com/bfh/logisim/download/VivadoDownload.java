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
        File tclCreateBitStreamFile = FileWriter.GetFilePointer(scriptPath, CREATE_BITSTEAM_TCL, myReporter);
        if (tclCreateProjectFile == null || xdcFile == null || tclCreateBitStreamFile == null) {
            tclCreateProjectFile = new File(scriptPath + CREATE_PROJECT_TCL);
            xdcFile = new File(xdcPath, XDC_FILE);
            tclCreateBitStreamFile = new File(scriptPath, CREATE_BITSTEAM_TCL);
            return tclCreateProjectFile.exists() && xdcFile.exists() && tclCreateBitStreamFile.exists();
        }

        String vivadoProjectPath = scriptPath + File.separator + VIVADO_PROJECT_NAME;

        // fill create project TCL script
        ArrayList<String> contents = new ArrayList<String>();
        contents.add("create_project " + VIVADO_PROJECT_NAME + " \"" + vivadoProjectPath + "\"");
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
        if (!FileWriter.WriteContents(xdcFile, contents, myReporter))
            return false;
        contents.clear();

        // generate bitstream
        contents.add("open_project -verbose " + vivadoProjectPath + File.separator + VIVADO_PROJECT_NAME + ".xpr");
        contents.add("update_compile_order -fileset sources_1");
        contents.add("launch_runs synth_1");
        contents.add("wait_on_run synth_1");
        contents.add("launch_runs impl_1 -to_step write_bitstream -jobs 8");
        contents.add("wait_on_run impl_1");
        if (!FileWriter.WriteContents(tclCreateBitStreamFile, contents, myReporter))
            return false;
        contents.clear();

        return false;
    }

    private final static String CREATE_PROJECT_TCL = "vivadoCreateProject.tcl";
    private final static String CREATE_BITSTEAM_TCL = "vivadoCreateBitStream.tcl";
    private final static String XDC_FILE = "vivadoConstraints.xdc";
    private final static String VIVADO_PROJECT_NAME = "vivadoproject";
}
