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

package com.bfh.logisim.settings;

import java.io.File;
import java.util.ArrayList;

import com.bfh.logisim.fpgaboardeditor.FPGAClass;
import com.cburch.logisim.prefs.AppPreferences;

public class Settings {
	private static String XilinxName = "XilinxToolsPath";
	private static String AlteraName = "AlteraToolsPath";
	private static String VivadoName = "VivadoToolsPath";
	public static String Unknown = "Unknown";
	public static String VHDL = "VHDL";
	public static String VERILOG = "Verilog";

	/* big TODO: add language support */
	public Settings() {
	}
	
	public static VendorSoftware getSoftware(char vendor) {
		switch(vendor) {
		   case FPGAClass.VendorAltera : VendorSoftware altera = new VendorSoftware(FPGAClass.VendorAltera, 
				   							AlteraName, load(FPGAClass.VendorAltera));
		   								 altera.setToolPath(CorrectPath(AppPreferences.QuartusToolPath.get()));
		   								 return altera;
		   case FPGAClass.VendorXilinx : VendorSoftware ise = new VendorSoftware(FPGAClass.VendorXilinx, 
				   							XilinxName, load(FPGAClass.VendorXilinx));
		   								 ise.setToolPath(CorrectPath(AppPreferences.ISEToolPath.get()));
		   								 return ise;
		   case FPGAClass.VendorVivado : VendorSoftware vivado = new VendorSoftware(FPGAClass.VendorVivado, 
				   							VivadoName, load(FPGAClass.VendorVivado));
		   								 vivado.setToolPath(CorrectPath(AppPreferences.VivadoToolPath.get()));
		   								 return vivado;
		   default : return null;
		}
	}
	
	public static String GetToolPath(char vendor) {
		switch(vendor) {
			case FPGAClass.VendorAltera : return AppPreferences.QuartusToolPath.get();
			case FPGAClass.VendorXilinx : return AppPreferences.ISEToolPath.get();
			case FPGAClass.VendorVivado : return AppPreferences.VivadoToolPath.get();
			default : return null;
		}
	}
	
	public static boolean setToolPath(char vendor, String path) {
		if (!toolsPresent(vendor,path))
			return false;
		switch(vendor) {
			case FPGAClass.VendorAltera : AppPreferences.QuartusToolPath.set(path);
			                              return true;
			case FPGAClass.VendorXilinx : AppPreferences.ISEToolPath.set(path);
			                              return true;
			case FPGAClass.VendorVivado : AppPreferences.VivadoToolPath.set(path);
            							  return true;
			default : return false;
		}
	}
	
	public static boolean toolsPresent(char vendor, String path) {
		String[] tools = load(vendor);
		for (int i = 0 ; i < tools.length ; i++) {
			File test = new File(CorrectPath(path+tools[i]));
			if (!test.exists())
				return false;
		}
		return true;
	}

	private static String[] load(char vendor) {
		ArrayList<String> progs = new ArrayList<>();
		String windowsExtension = ".exe";
		if (vendor == FPGAClass.VendorAltera) {
			progs.add("quartus_sh");
			progs.add("quartus_pgm");
			progs.add("quartus_map");
		}
		else if (vendor == FPGAClass.VendorXilinx) {
			progs.add("xst");
			progs.add("ngdbuild");
			progs.add("map");
			progs.add("par");
			progs.add("bitgen");
			progs.add("impact");
			progs.add("cpldfit");
			progs.add("hprep6");
		}
		else if (vendor == FPGAClass.VendorVivado) {
			progs.add("vivado");
            windowsExtension = ".bat";
		}

		String[] progsArray = progs.toArray(new String[0]);
		String osname = System.getProperty("os.name");
		if (osname == null)
			throw new IllegalArgumentException("no os.name");
		else {
			if (osname.toLowerCase().contains("windows")) {
				for (int i=0; i<progsArray.length; i++) {
					progsArray[i] += windowsExtension;
				}
			}
		}
		return progsArray;
	}

	private static String CorrectPath(String path) {
		if (path.endsWith(File.separator))
			return path;
		else
			return path + File.separator;
	}
}
