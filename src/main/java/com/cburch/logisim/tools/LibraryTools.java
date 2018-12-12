package com.cburch.logisim.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JOptionPane;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LoadedLibrary;
import com.cburch.logisim.file.LogisimFile;


public class LibraryTools {
	public static void ShowErrors(String LibName,HashMap<String,String> Messages) {
		JOptionPane.showMessageDialog(null, Message(LibName,Messages), Strings.get("LibLoadErrors")+" "+LibName+" !", JOptionPane.ERROR_MESSAGE);
	}
	
	public static void ShowWarnings(String LibName,HashMap<String,String> Messages) {
		JOptionPane.showMessageDialog(null, Message(LibName,Messages), Strings.get("LibLoadWarnings")+" "+LibName+" !", JOptionPane.WARNING_MESSAGE);
	}

	private static String Message(String LibName,HashMap<String,String> Messages) {
		String Message = "";
		int item = 0;
		for (String myerror : Messages.keySet()) {
			item++;
			Message = Message.concat( item+") "+Strings.get(Messages.get(myerror))+" \""+myerror+"\".\n");
		}
		return  Message;
	}
	
	public static void BuildToolList(Library lib, HashSet<String> Tools) {
		Iterator<? extends Tool> tooliter = lib.getTools().iterator();
		while (tooliter.hasNext()) {
			Tool tool = tooliter.next();
			Tools.add(tool.getName().toUpperCase());
		}
		for (Library sublib : lib.getLibraries())
			BuildToolList(sublib,Tools);
	}
	
	public static boolean BuildToolList(Library lib, HashMap<String,AddTool> Tools) {
		boolean ret = true;
		if (!lib.getName().equals("Base")) {
			Iterator<? extends Tool> tooliter = lib.getTools().iterator();
			while (tooliter.hasNext()) {
			   Tool tool1 = tooliter.next();
			   if (Tools.containsKey(tool1.getName().toUpperCase()))
				   ret = false;
			   else
				   Tools.put(tool1.getName().toUpperCase(), (AddTool) tool1);
			}
		}
		for (Library sublib : lib.getLibraries()) {
			ret &= BuildToolList(sublib,Tools);
		}
		return ret;
	}
	
	public static Circuit getCircuitFromLibs(Library lib, String UpperCaseName) {
		Circuit ret = null;
		if (lib instanceof LogisimFile) {
			LogisimFile llib = (LogisimFile) lib;
			for (Circuit circ : llib.getCircuits()) {
				if (circ.getName().toUpperCase().equals(UpperCaseName))
					return circ;
			}
		}
		for (Library libs : lib.getLibraries()) {
			if (libs instanceof LoadedLibrary) {
				LoadedLibrary lib1 = (LoadedLibrary) libs;
				ret = getCircuitFromLibs(lib1.getBase(), UpperCaseName);
			} else ret = getCircuitFromLibs(libs, UpperCaseName);
			if (ret != null)
				return ret;
		}
		return null;
	}
	
	public static ArrayList<String> LibraryCanBeMerged(HashSet<String> SourceTools, HashSet<String> NewTools) {
		ArrayList<String> ret = new ArrayList<String>();
		Iterator<String> Iter = NewTools.iterator();
		while (Iter.hasNext()) {
			String This = Iter.next();
			if (SourceTools.contains(This)) {
				ret.add(This);
			}
		}
		return ret;
	}
	
	public static HashMap<String,String> GetToolLocation(Library lib, String Location , ArrayList<String> UpercaseNames) {
		Iterator<? extends Tool> tooliter = lib.getTools().iterator();
		String MyLocation;
		HashMap<String,String> ret = new HashMap<String,String>();
		if (Location.isEmpty())
			MyLocation = new String(lib.getName());
		else
			MyLocation = new String(Location+"->"+lib.getName());
	    while (tooliter.hasNext()) {
	    	Tool tool = tooliter.next();
	    	if (UpercaseNames.contains(tool.getName().toUpperCase())) {
	    		ret.put(tool.getName(), MyLocation);
	    	}
	    }
	    for (Library sublib : lib.getLibraries()) {
	    	ret.putAll(GetToolLocation(sublib, MyLocation , UpercaseNames));
	    }
	    return ret;
	}
	
	public static boolean LibraryIsConform(Library lib, HashSet<String> Names, HashSet<String> Tools, HashMap<String,String> Error) {
		Iterator<? extends Tool> tooliter=lib.getTools().iterator();
		boolean HasErrors = false;
		while (tooliter.hasNext()) {
			Tool tool = tooliter.next();
			if (Tools.contains(tool.getName().toUpperCase())) {
				HasErrors = true;
				if (!Error.containsKey(tool.getName())) {
					Error.put(tool.getName(), "LibraryHasDuplicatedTools");
				}
			}
			Tools.add(tool.getName().toUpperCase());
		}
		for (Library sublib : lib.getLibraries()) {
			if (Names.contains(sublib.getName().toUpperCase())) {
				HasErrors = true;
				if (!Error.containsKey(sublib.getName())) {
					Error.put(sublib.getName(), "LibraryHasDuplicatedSublibraries");
				}
			}
			Names.add(sublib.getName().toUpperCase());
			HasErrors |= !LibraryIsConform(sublib,Names,Tools,Error); 
		}
		return !HasErrors;
	}
	
	public static void BuildLibraryList(Library lib, HashMap<String,Library> Names) {
		Names.put(lib.getName().toUpperCase(),lib);
		for (Library sublib : lib.getLibraries()) {
			BuildLibraryList(sublib,Names);
		}
	}
	
	public static void RemovePresentLibraries(Library lib, HashMap<String,Library> KnownLibs, boolean AddToSet) {
		/* we work top -> down */
		HashSet<String> ToBeRemoved = new HashSet<String>();
		for (Library sublib : lib.getLibraries()) {
			if (KnownLibs.keySet().contains(sublib.getName().toUpperCase())) {
				ToBeRemoved.add(sublib.getName());
			} else
			if (AddToSet) {
				KnownLibs.put(sublib.getName().toUpperCase(),sublib);
			}
		}
		for (String remove : ToBeRemoved) {
			lib.removeLibrary(remove);
		}
		for (Library sublib : lib.getLibraries())
			RemovePresentLibraries(sublib,KnownLibs,AddToSet);
	}

}
