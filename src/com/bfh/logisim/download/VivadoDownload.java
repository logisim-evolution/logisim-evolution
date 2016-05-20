package com.bfh.logisim.download;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.FPGAClass;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGeneratorFactory;
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
                                             String sandBoxPath, Netlist rootNetlist, MappableResourcesContainer mapInfo,
                                             BoardInformation boardInfo, ArrayList<String> entities,
                                             ArrayList<String> architectures, String HDLType,
                                             boolean writeToFlash) {

        // create project files
        File createProjectFile = FileWriter.GetFilePointer(scriptPath, CREATE_PROJECT_TCL, myReporter);
        File xdcFile = FileWriter.GetFilePointer(xdcPath, XDC_FILE, myReporter);
        File generateBitstreamFile = FileWriter.GetFilePointer(scriptPath, GENERATE_BITSTREAM_FILE, myReporter);
        File loadBitstreamFile = FileWriter.GetFilePointer(scriptPath, LOAD_BITSTEAM_FILE, myReporter);
        if (createProjectFile == null || xdcFile == null || generateBitstreamFile == null || loadBitstreamFile == null) {
            createProjectFile = new File(scriptPath + CREATE_PROJECT_TCL);
            xdcFile = new File(xdcPath, XDC_FILE);
            generateBitstreamFile = new File(scriptPath, GENERATE_BITSTREAM_FILE);
            loadBitstreamFile = new File(scriptPath, LOAD_BITSTEAM_FILE);
            return createProjectFile.exists() && xdcFile.exists() && generateBitstreamFile.exists() && loadBitstreamFile.exists();
        }

        String vivadoProjectPath = sandBoxPath + File.separator + VIVADO_PROJECT_NAME;

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
        contents.add("exit");
        if (!FileWriter.WriteContents(createProjectFile, contents, myReporter))
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
        contents.add("exit");
        if (!FileWriter.WriteContents(generateBitstreamFile, contents, myReporter))
            return false;
        contents.clear();

        // load bitstream
        String lindex = "[lindex [get_hw_devices] 0]";
        contents.add("open_hw");
        contents.add("connect_hw_server");
        contents.add("open_hw_target");
        contents.add("set_property PROGRAM.FILE {" + vivadoProjectPath + File.separator + VIVADO_PROJECT_NAME + ".runs"
                + File.separator + "impl_1" + File.separator + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".bit} " + lindex);
        contents.add("current_hw_device " + lindex);
        contents.add("refresh_hw_device -update_hw_probes false " + lindex);
        contents.add("program_hw_device " + lindex);
        contents.add("exit");
        return FileWriter.WriteContents(loadBitstreamFile, contents, myReporter);
    }

    private final static String CREATE_PROJECT_TCL = "vivadoCreateProject.tcl";
    private final static String GENERATE_BITSTREAM_FILE = "vivadoGenerateBitStream.tcl";
    private final static String LOAD_BITSTEAM_FILE = "vivadoLoadBitStream.tcl";
    private final static String XDC_FILE = "vivadoConstraints.xdc";
    private final static String VIVADO_PROJECT_NAME = "vivadoproject";
}
