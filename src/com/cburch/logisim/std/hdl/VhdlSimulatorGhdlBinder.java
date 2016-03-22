/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/
package com.cburch.logisim.std.hdl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.std.hdl.VhdlSimulator.State;
import com.cburch.logisim.tools.MessageBox;
import com.cburch.logisim.util.FileUtil;
import com.cburch.logisim.util.Softwares;

/**
 * The TCL binder is a TCL program creating a socket server. The signals have to
 * be written to the binder who will drive the simulation and send back the
 * output signals.
 * <p/>
 * The binder is started when the VHDL simulation is enabled. Once started, it
 * writes a ready flag to his stdout that is catched by Logisim to know the
 * binder is ready. This way we ensure the socket is started before trying to
 * connect.
 * <p/>
 * To end the binder, we send an "end" flag through the socket and wait for it
 * to finish. This causes Logisim to hang if the binder doesn't listen to the
 * socker. That can happen when unexpected behavior of the simulation occurs.
 *
 * @author christian.mueller@heig-vd.ch
 */
class VhdlSimulatorGhdlBinder {

	final static Logger logger = LoggerFactory
			.getLogger(VhdlSimulatorGhdlBinder.class);

	private ProcessBuilder builder;
	private Process process;
	private BufferedReader processReader;

	
	private Boolean running = false;

	private VhdlSimulator vhdlSimulator;


	private VhdlSimulatorConsole getConsole(){
		return vhdlSimulator.getProject().getFrame()
				.getVhdlSimulatorConsole();		
	}


	private ProcessBuilder ghdlProcess(List<String> command){
		String BasePath=FileUtil.correctPath(Softwares.getQuestaPath());
		command.add(0,FileUtil.correctPath(Softwares.getQuestaPath()) + "/bin/ghdl.exe");

		ProcessBuilder probuilder = new ProcessBuilder(command);

		probuilder.directory(new File(VhdlSimulator.SIM_PATH + "src/"));
		probuilder.redirectErrorStream(true);

		Map<String, String> env = probuilder.environment();
		env.put("Path", env.get("Path")+BasePath+"bin"+";"+BasePath+"lib");
		return probuilder;		
	}


	private String runCommand(List<String> command){		
		ProcessBuilder probuilder=ghdlProcess(command);
		String result="";

		Process pbuilder;
		try {
			pbuilder = probuilder.start();

			String line;
			InputStreamReader isr = new InputStreamReader(pbuilder.getInputStream());
			BufferedReader reader  = new BufferedReader(isr);

			while ((line = reader.readLine()) != null) {						
				result+=line+System.getProperty("line.separator");
			}
			result+="Exit Value: "+pbuilder.exitValue()+System.getProperty("line.separator");					

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		command.clear();
		return result;
	}

	public VhdlSimulatorGhdlBinder(VhdlSimulator vs) {

		vhdlSimulator = vs;
	}

	public void compile() {
		if(Softwares.getQuestaPath().contains("Ghdl")){
			File folder=new File(VhdlSimulator.SIM_PATH + "src/");

			VhdlSimulatorConsole console=getConsole();

			List<String> command = new ArrayList<String>();

			command.add("--clean");
			console.append("clean\n"+runCommand(command));			

			command.add("--remove");
			console.append("remove\n"+runCommand(command));			

			for (final File fileEntry : folder.listFiles()) {
				if(fileEntry.getName().toLowerCase().endsWith(".vcd")||fileEntry.getName().toLowerCase().endsWith(".pdb")){
					fileEntry.delete();
				}		
			}	

			FilenameFilter vhdFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase();
					if (lowercaseName.endsWith(".vhdl")) {
						return true;
					} else {
						return false;
					}
				}
			};	

			for (final File fileEntry : folder.listFiles(vhdFilter)) {
				if (!fileEntry.isDirectory()) {
					command.add("-a");
					command.add("\""+fileEntry.getName()+"\"");
					console.append(fileEntry.getName()+System.getProperty("line.separator"));
					console.append(runCommand(command));
				}			
			}

			command.add("-e");
			command.add("\"testbench\"");
			console.append("-e"+System.getProperty("line.separator"));
			console.append(runCommand(command));
		}
	}


	public void restart() {
		stop();
		start();
	}

	public Boolean isRunning() {
		return running;
	}

	public String receive(){
		String NextPort=vhdlSimulator.vhdlTestbench.testPatternMapping.getNextOutput();
		return NextPort;
	}

	public void send(String send){
		if(send=="sync"){
			try {
				OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
				String testPattern=vhdlSimulator.vhdlTestbench.testPatternMapping.getInputPattern();
				writer.write(testPattern+"\n");
				writer.flush();

				String line="";
				String result="";
				while (!processReader.ready()) Thread.sleep(10);
				while (processReader.ready()) {
					if ((line = processReader.readLine()) != null) {
						if(line.contains((CharSequence)new String("Data"))){
							result=line;
						} else{
							getConsole().append("ghdl.exe: "+line + "\n");							
						}
					}
				}
				
				if(result!=""){
					String reverse = new StringBuffer(line).reverse().toString();
					vhdlSimulator.vhdlTestbench.testPatternMapping.setOutputsFromPattern(reverse);
					vhdlSimulator.vhdlTestbench.testPatternMapping.resetEnumerate();						
				}


			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		else{
			String[] parameters = send.split("\\:");
			if(parameters.length>=3){
				vhdlSimulator.vhdlTestbench.testPatternMapping.setValue(parameters[1], parameters[2]);
			}
		}
	}
	
	public void start() {

		try {
			vhdlSimulator.getProject().getFrame()
			.getVhdlSimulatorConsole().clear();
			compile();
			List<String> command = new ArrayList<String>();
			command.add("-r");
			command.add("\"testbench\"");
			command.add("--vcd=Result.vcd");
			builder=ghdlProcess(command);
			vhdlSimulator.getProject().getFrame().getVhdlSimulatorConsole().append("-r"+System.getProperty("line.separator"));
			
			process = builder.start();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Cannot run TCL binder to Questasim : {}",
					e.getMessage());

			running = false;
			return;
		}


		/* This thread checks the binder started well, it's run from now */
		new Thread(new Runnable() {

			@Override
			public void run() {
				vhdlSimulator.setState(State.RUNNING);
				running=true;
				processReader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
				
				while(process.isAlive()){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				String line;
				try {
					while ((line = processReader.readLine()) != null) {
						vhdlSimulator.getProject().getFrame()
						.getVhdlSimulatorConsole().append(line + "\n");					
					}
					processReader.close();
					process.destroy();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}

				vhdlSimulator.getProject().getFrame()
				.getVhdlSimulatorConsole().append("Simulation terminated with exit value: "+
						process.exitValue() + "\n");

				
				stop();
				vhdlSimulator.setState(State.ENABLED);
				
				return;
				}
		}).start();
	}

	public void stop() {
		if (!running)
			return;

		try {
			OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
			writer.write("End\n");
			writer.flush();
		} catch (IOException e) {
		} 
		
		process.destroy();
		running = false;
	}
}