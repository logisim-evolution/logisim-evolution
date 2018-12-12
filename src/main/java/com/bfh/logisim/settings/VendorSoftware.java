package com.bfh.logisim.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import com.cburch.logisim.prefs.AppPreferences;

public class VendorSoftware {
	public static final char VendorAltera = 0;
	public static final char VendorXilinx = 1;
	public static final char VendorVivado = 2;
	public static final char VendorUnknown = 255;
	public static String[] Vendors = { "Altera", "Xilinx", "Vivado" };
	private static String XilinxName = "XilinxToolsPath";
	private static String AlteraName = "AlteraToolsPath";
	private static String VivadoName = "VivadoToolsPath";
	public static String Unknown = "Unknown";

    private char vendor;
    private String name;
    private String[] bin;

    public VendorSoftware(char vendor, String name, String[] bin) {
        this.vendor = vendor;
        this.name = name;
        this.bin = bin;
    }

    public String getToolPath() {
    	switch (vendor) {
    		case VendorAltera : return AppPreferences.QuartusToolPath.get();
    		case VendorXilinx : return AppPreferences.ISEToolPath.get();
    		case VendorVivado : return AppPreferences.VivadoToolPath.get();
    	}
        return "Unknown";
    }

    public String getName() {
        return name;
    }

    public char getVendor() {
        return vendor;
    }

    public String[] getBinaries() {
        return bin;
    }

    public String getBinaryPath(int binPos) {
        return getToolPath() + File.separator + bin[binPos];
    }

	public static LinkedList<String> getVendorStrings() {
		LinkedList<String> result = new LinkedList<String>();

		result.add(VendorSoftware.Vendors[0]);
		result.add(VendorSoftware.Vendors[1]);
		result.add(VendorSoftware.Vendors[2]);

		return result;
	}

	public static VendorSoftware getSoftware(char vendor) {
		switch(vendor) {
		   case VendorAltera : VendorSoftware altera = new VendorSoftware(VendorAltera, 
				   							AlteraName, load(VendorAltera));
		   								 return altera;
		   case VendorXilinx : VendorSoftware ise = new VendorSoftware(VendorXilinx, 
				   							XilinxName, load(VendorXilinx));
		   								 return ise;
		   case VendorVivado : VendorSoftware vivado = new VendorSoftware(VendorVivado, 
				   							VivadoName, load(VendorVivado));
		   								 return vivado;
		   default : return null;
		}
	}

	public static String GetToolPath(char vendor) {
		switch(vendor) {
			case VendorAltera : return AppPreferences.QuartusToolPath.get();
			case VendorXilinx : return AppPreferences.ISEToolPath.get();
			case VendorVivado : return AppPreferences.VivadoToolPath.get();
			default : return null;
		}
	}
	
	public static boolean setToolPath(char vendor, String path) {
		if (!toolsPresent(vendor,path))
			return false;
		switch(vendor) {
			case VendorAltera : AppPreferences.QuartusToolPath.set(path);
			                    return true;
			case VendorXilinx : AppPreferences.ISEToolPath.set(path);
			                    return true;
			case VendorVivado : AppPreferences.VivadoToolPath.set(path);
            	 		        return true;
			default : return false;
		}
	}
	
    private static String CorrectPath(String path) {
		if (path.endsWith(File.separator))
			return path;
		else
			return path + File.separator;
	}

	private static String[] load(char vendor) {
		ArrayList<String> progs = new ArrayList<>();
		String windowsExtension = ".exe";
		if (vendor == VendorAltera) {
			progs.add("quartus_sh");
			progs.add("quartus_pgm");
			progs.add("quartus_map");
		}
		else if (vendor == VendorXilinx) {
			progs.add("xst");
			progs.add("ngdbuild");
			progs.add("map");
			progs.add("par");
			progs.add("bitgen");
			progs.add("impact");
			progs.add("cpldfit");
			progs.add("hprep6");
		}
		else if (vendor == VendorVivado) {
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

	public static boolean toolsPresent(char vendor, String path) {
		String[] tools = load(vendor);
		for (int i = 0 ; i < tools.length ; i++) {
			File test = new File(CorrectPath(path+tools[i]));
			if (!test.exists())
				return false;
		}
		return true;
	}
}
