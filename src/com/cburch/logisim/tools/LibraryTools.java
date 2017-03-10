package com.cburch.logisim.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JOptionPane;


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
	
	public static void BuildLibraryList(Library lib, HashSet<String> Names) {
		Names.add(lib.getName().toUpperCase());
		for (Library sublib : lib.getLibraries()) {
			BuildLibraryList(sublib,Names);
		}
	}
	
	public static void RemovePresentLibraries(Library lib, HashSet<String> KnownLibs, boolean AddToSet) {
		/* we work top -> down */
		HashSet<String> ToBeRemoved = new HashSet<String>();
		for (Library sublib : lib.getLibraries()) {
			if (KnownLibs.contains(sublib.getName().toUpperCase())) {
				ToBeRemoved.add(sublib.getName());
			} else
			if (AddToSet) {
				KnownLibs.add(sublib.getName().toUpperCase());
			}
		}
		for (String remove : ToBeRemoved)
			lib.removeLibrary(remove);
		for (Library sublib : lib.getLibraries())
			RemovePresentLibraries(sublib,KnownLibs,AddToSet);
	}


}
