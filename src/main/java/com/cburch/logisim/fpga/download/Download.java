package com.cburch.logisim.fpga.download;

import static com.cburch.logisim.fpga.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardInformation;
import com.cburch.logisim.fpga.fpgagui.ComponentMapDialog;
import com.cburch.logisim.fpga.fpgagui.FPGACommanderBase;
import com.cburch.logisim.fpga.fpgagui.FPGAReport;
import com.cburch.logisim.fpga.gui.DownloadProgressBar;
import com.cburch.logisim.fpga.settings.VendorSoftware;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;

public class Download extends FPGACommanderBase implements Runnable,WindowListener  {
	
	private boolean StopRequested = false;
	
	private boolean DownloadBitstream;
	private boolean DownloadOnly;
	private char Vendor;
	private boolean UseGui;
	private DownloadProgressBar MyGui;
	private VendorDownload Downloader;
	private String TopLevelSheet;
	private double TickFrequency;
	private boolean writeToFlash;
	private static int BasicSteps = 5;
	ArrayList<String> Entities = new ArrayList<String>();
	ArrayList<String> Architectures = new ArrayList<String>();
	
	private Process Executable;
	private Object lock = new Object();
	
	private ArrayList<ActionListener> Listeners = new ArrayList<ActionListener>();
	
	public Download(Project MyProject,
			        String TopLevelSheet,
			        double TickFrequency,
			        FPGAReport MyReporter, 
			        BoardInformation MyBoardInformation,
			        boolean writeToFlash,
			        boolean DownloadOnly,
			        boolean UseGui) {
		this.MyProject = MyProject;
		this.MyReporter = MyReporter;
		this.MyBoardInformation = MyBoardInformation;
		this.DownloadBitstream = AppPreferences.DownloadToBoard.get();
		this.DownloadOnly = DownloadOnly;
		this.Vendor = MyBoardInformation.fpga.getVendor();
		this.UseGui = UseGui;
		this.TopLevelSheet = TopLevelSheet;
		this.TickFrequency = TickFrequency;
		this.writeToFlash = writeToFlash;
		Circuit RootSheet = MyProject.getLogisimFile().getCircuit(TopLevelSheet);
		String Title = S.fmt("DownloadingInfo", VendorSoftware.getVendorString(Vendor));
		int steps = BasicSteps;
		switch (Vendor) {
			case VendorSoftware.VendorAltera : Downloader = new AlteraDownload(GetProjDir(TopLevelSheet),
                    		                                                   MyReporter,
                    		                                                   RootSheet.getNetList(),
			                                		                           MyBoardInformation,
			                                		                           Entities,
			                                		                           Architectures,
			                                		                           AppPreferences.HDL_Type.get());
			                                   break;
			default                          : MyReporter.AddFatalError("BUG: Tried to Download to an unknown target");
			                                   return;
		}
		if (UseGui) {
			if (Downloader != null)
				steps += Downloader.GetNumberOfStages();
			MyGui = new DownloadProgressBar(Title,steps);
			MyGui.addWindowListener(this);
			MyGui.SetStatus(S.get("FpgaDownloadInfo"));
		}
	}

	public void DoDownload() {
		new Thread(this).start();
	}
	
	public boolean CreateDownloadScripts() {
		if (Downloader != null)
			return Downloader.CreateDownloadScripts();
		return false;
	}
	
	public void AddListener(ActionListener listener) {
		if (!Listeners.contains(listener))
			Listeners.add(listener);
	}
	
	public void RemoveListener(ActionListener listener) {
		if (Listeners.contains(listener))
			Listeners.remove(Listeners.indexOf(listener));
	}
	
	private void fireEvent(ActionEvent e) {
		for (int i = 0 ; i < Listeners.size() ; i++) {
			Listeners.get(i).actionPerformed(e);
		}
	}
	
	@Override
	public void run() {
		if (PrepareDownLoad()&&VendorSoftwarePresent()&&AppPreferences.DownloadToBoard.get()) {
			try {
				String error = download();
				if (error != null)
					MyReporter.AddFatalError(error);
			} catch (IOException e) {
				MyReporter.AddFatalError(S.fmt("FPGAIOError", VendorSoftware.getVendorString(Vendor)));
				e.printStackTrace();
			} catch (InterruptedException e) {
				MyReporter.AddError(S.fmt("FPGAInterruptedError",VendorSoftware.getVendorString(Vendor)));
			}
		}
		if (UseGui) {
			MyGui.setVisible(false);
			MyGui.dispose();
		}
		fireEvent(new ActionEvent(this,1,"DownloadDone"));
	}
	
	private String download() throws IOException, InterruptedException {
		MyReporter.ClsScr();
		if (!DownloadOnly||!Downloader.readyForDownload()) {
			for (int stages = 0; stages < Downloader.GetNumberOfStages() ; stages++) {
				if (StopRequested)
					return "Interrupted";
				ProcessBuilder CurrentStage = Downloader.PerformStep(stages);
				String result = execute(Downloader.GetStageMessage(stages),CurrentStage);
				if (result != null)
					return result;
				MyGui.SetProgress(stages+BasicSteps);
			}
		}
		MyGui.SetProgress(Downloader.GetNumberOfStages()+BasicSteps-1);
		if (!DownloadBitstream)
			return null;
		Object[] options = { S.get("FPGADownloadOk"),S.get("FPGADownloadCancel") };
		if (JOptionPane.showOptionDialog(
				MyGui,
				S.get("FPGAVerifyMsg1"),
				S.get("FPGAVerifyMsg2"), JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE, null, options, options[0]) != JOptionPane.YES_OPTION) {
			return S.get("FPGADownloadAborted");
		}
		ProcessBuilder DownloadBitfile = Downloader.DownloadToBoard();
		if (StopRequested)
			return "Interrupted";
		return execute(S.get("FPGADownloadBitfile"),DownloadBitfile);
	}
	
	private String execute(String StageName, ProcessBuilder process) throws IOException, InterruptedException {
		MyGui.SetStatus(StageName);
		MyReporter.print(" ");
		MyReporter.print("==>");
		MyReporter.print("==> "+StageName);
		MyReporter.print("==>");
		synchronized (lock) {
			Executable = process.start();
		}
		InputStream is = Executable.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		while ((line = br.readLine()) != null) {
			MyReporter.print(line);
		}
		Executable.waitFor();
		isr.close();
		br.close();
		if (Executable.exitValue()!= 0) {
			return S.fmt("FPGAExecutionFailure",StageName);
		}
		return null;
	}
	
	private boolean PrepareDownLoad() {
		if (!AppPreferences.DownloadToBoard.get() && DownloadOnly) {
			MyReporter.AddError(S.get("FPGASettingSkipGenerateInvalid"));
			return false;
		}
		if (DownloadOnly)
			return true;
		/* Stage 0 DRC */
		if (UseGui)
			MyGui.SetStatus(S.get("FPGAState0"));
		if (!performDRC(TopLevelSheet, AppPreferences.HDL_Type.get())) {
			return false;
		}
		String Name = MyProject.getLogisimFile().getName();
		if (Name.contains(" ")) {
			MyReporter.AddFatalError(S.fmt("FPGANameContainsSpaces", Name));
			return false;
		}
		String Dir = MyProject.getLogisimFile().getLoader().getMainFile().toString();
		if (Dir.contains(" ")) {
			MyReporter.AddFatalError(S.fmt("FPGADirContainsSpaces", Dir));
			return false;
		}
		/* Stage 1 Is design map able on Board */
		if (UseGui) {
			MyGui.SetProgress(1);
			MyGui.SetStatus(S.get("FPGAState2"));
		}
		if (!MapDesign(TopLevelSheet)) {
			return false;
		}
		if (UseGui) {
			ComponentMapDialog MapPannel;
			if (MyProject.getLogisimFile().getLoader().getMainFile() != null) {
				MapPannel = new ComponentMapDialog(null, MyProject
						.getLogisimFile().getLoader().getMainFile()
						.getAbsolutePath());
			} else {
				MapPannel = new ComponentMapDialog(null, "");
			}
			/* Stage 2 Map design on board */
			MyGui.SetProgress(2);
			MyGui.SetStatus(S.get("FPGAState3"));
			MapPannel.SetBoardInformation(MyBoardInformation);
			MapPannel.SetMappebleComponents(MyMappableResources);
			MapPannel.run();
			if (!MapPannel.isDoneAssignment()) {
				MyReporter.AddError(S.get("FPGADownloadAborted"));
				return false;
			}
		} else {
			/* TODO: do the mapping by command line arguments in non gui mode */
		}
		if (!MapDesignCheckIOs()) {
			MyReporter.AddError(S.fmt("FPGAMapNotComplete", MyBoardInformation.getBoardName()));
			return false;
		}
		/* Stage 3 HDL generation */
		if (UseGui) {
			MyGui.SetProgress(3);
			MyGui.SetStatus(S.get("FPGAState1"));
		}
		if (!writeHDL(TopLevelSheet,TickFrequency)) {
			return false;
		}
		String ProjectPath = GetProjDir(TopLevelSheet);
		String SourcePath = ProjectPath + AppPreferences.HDL_Type.get().toLowerCase()+File.separator;
		GetVHDLFiles(ProjectPath,SourcePath,Entities,Architectures,AppPreferences.HDL_Type.get());
		if (UseGui) {
			MyGui.SetProgress(4);
			MyGui.SetStatus(S.get("FPGAState4"));
		}
		Downloader.SetMapableResources(MyMappableResources);
		/* Stage 4 Create Download Scripts */
		return CreateDownloadScripts();
	}

/*	protected boolean DownLoadDesign(boolean generateOnly, boolean downloadOnly, String CircuitName,
			boolean writeToFlash, boolean downloadDesign) {

	if (MyBoardInformation.fpga.getVendor() == VendorSoftware.VendorXilinx) {
			if (XilinxDownload.GenerateISEScripts(MyReporter, ProjectDir,
					ProjectDir + HDLPaths[ScriptPath] + File.separator,
					ProjectDir + HDLPaths[UCFPath] + File.separator,
					RootSheet.getNetList(), MyMappableResources,
					MyBoardInformation, Entities, Behaviors,
					AppPreferences.HDL_Type.get(),
					writeToFlash)
					&& !generateOnly) {
				return XilinxDownload.Download(MyBoardInformation,
						ProjectDir + HDLPaths[ScriptPath] + File.separator,
						ProjectDir + HDLPaths[UCFPath] + File.separator,
						ProjectDir, ProjectDir + HDLPaths[SandboxPath]
								+ File.separator, MyReporter, downloadDesign);
			}
		} else if (MyBoardInformation.fpga.getVendor() == VendorSoftware.VendorVivado) {
			if (VivadoDownload.GenerateScripts(MyReporter, ProjectDir,
					ProjectDir + HDLPaths[ScriptPath] + File.separator,
					ProjectDir + HDLPaths[XDCPath] + File.separator,
					ProjectDir + HDLPaths[SandboxPath] + File.separator,
					RootSheet.getNetList(), MyMappableResources,
					MyBoardInformation, Entities, Behaviors,
					AppPreferences.HDL_Type.get(),
					writeToFlash)
					&& !generateOnly) {
				return VivadoDownload.Download(
						ProjectDir + HDLPaths[ScriptPath] + File.separator,
						ProjectDir + HDLPaths[SandboxPath] + File.separator,
						MyReporter, downloadOnly, downloadDesign);
			}
		}
	}
*/


	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		MyGui.SetStatus(S.get("FPGACancelWait"));
		StopRequested = true;
		synchronized(lock) {
			if (Executable != null) {
				Executable.destroy();
			}
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	public static String GetClockFrequencyString(BoardInformation CurrentBoard) {
		long clkfreq = CurrentBoard.fpga.getClockFrequency();
		if (clkfreq % 1000000 == 0) {
			clkfreq /= 1000000;
			return Long.toString(clkfreq) + " MHz ";
		} else if (clkfreq % 1000 == 0) {
			clkfreq /= 1000;
			return Long.toString(clkfreq) + " kHz ";
		}
		return Long.toString(clkfreq);
	}
}
