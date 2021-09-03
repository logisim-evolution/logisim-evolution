/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import static com.cburch.logisim.tools.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LoadedLibrary;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.OptionPane;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import lombok.val;

public class LibraryTools {
  public static void ShowErrors(String libName, HashMap<String, String> messages) {
    OptionPane.showMessageDialog(
        null,
        Message(libName, messages),
        S.get("LibLoadErrors") + " " + libName + " !",
        OptionPane.ERROR_MESSAGE);
  }

  private static String Message(String libName, HashMap<String, String> messages) {
    var Message = "";
    var item = 0;
    for (val myerror : messages.keySet()) {
      item++;
      Message = Message.concat(item + ") " + messages.get(myerror) + " \"" + myerror + "\".\n");
    }
    return Message;
  }

  public static void BuildToolList(Library lib, HashSet<String> tools) {
    for (val tool : lib.getTools()) {
      tools.add(tool.getName().toUpperCase());
    }
    for (Library sublib : lib.getLibraries()) BuildToolList(sublib, tools);
  }

  public static boolean BuildToolList(Library lib, HashMap<String, AddTool> Tools) {
    var ret = true;
    if (!lib.getName().equals("Base")) {
      for (val tool1 : lib.getTools()) {
        if (Tools.containsKey(tool1.getName().toUpperCase()))
          ret = false;
        else
          Tools.put(tool1.getName().toUpperCase(), (AddTool) tool1);
      }
    }
    for (val sublib : lib.getLibraries()) {
      ret &= BuildToolList(sublib, Tools);
    }
    return ret;
  }

  public static Circuit getCircuitFromLibs(Library lib, String uppercasedName) {
    Circuit ret = null;
    if (lib instanceof LogisimFile) {
      LogisimFile llib = (LogisimFile) lib;
      for (val circ : llib.getCircuits()) {
        if (circ.getName().toUpperCase().equals(uppercasedName)) return circ;
      }
    }
    for (val libs : lib.getLibraries()) {
      if (libs instanceof LoadedLibrary) {
        val lib1 = (LoadedLibrary) libs;
        ret = getCircuitFromLibs(lib1.getBase(), uppercasedName);
      } else ret = getCircuitFromLibs(libs, uppercasedName);
      if (ret != null) return ret;
    }
    return null;
  }

  public static ArrayList<String> LibraryCanBeMerged(HashSet<String> sourceTools, HashSet<String> newTools) {
    val ret = new ArrayList<String>();
    for (val This : newTools) {
      if (sourceTools.contains(This)) {
        ret.add(This);
      }
    }
    return ret;
  }

  public static HashMap<String, String> GetToolLocation(Library lib, String location, ArrayList<String> upercasedNames) {
    val tooliter = lib.getTools().iterator();
    val ret = new HashMap<String, String>();
    val myLocation = location.isEmpty() ? lib.getName() : location + "->" + lib.getName();
    while (tooliter.hasNext()) {
      val tool = tooliter.next();
      if (upercasedNames.contains(tool.getName().toUpperCase())) {
        ret.put(tool.getName(), myLocation);
      }
    }
    for (val sublib : lib.getLibraries()) {
      ret.putAll(GetToolLocation(sublib, myLocation, upercasedNames));
    }
    return ret;
  }

  public static boolean LibraryIsConform(Library lib, HashSet<String> names, HashSet<String> tools, HashMap<String, String> error) {
    val tooliter = lib.getTools().iterator();
    var hasErrors = false;
    while (tooliter.hasNext()) {
      val tool = tooliter.next();
      if (tools.contains(tool.getName().toUpperCase())) {
        hasErrors = true;
        if (!error.containsKey(tool.getName())) {
          error.put(tool.getName(), S.get("LibraryHasDuplicatedTools"));
        }
      }
      tools.add(tool.getName().toUpperCase());
    }
    for (val sublib : lib.getLibraries()) {
      if (names.contains(sublib.getName().toUpperCase())) {
        hasErrors = true;
        if (!error.containsKey(sublib.getName())) {
          error.put(sublib.getName(), S.get("LibraryHasDuplicatedSublibraries"));
        }
      }
      names.add(sublib.getName().toUpperCase());
      hasErrors |= !LibraryIsConform(sublib, names, tools, error);
    }
    return !hasErrors;
  }

  public static void BuildLibraryList(Library lib, HashMap<String, Library> names) {
    names.put(lib.getName().toUpperCase(), lib);
    for (val sublib : lib.getLibraries()) {
      BuildLibraryList(sublib, names);
    }
  }

  public static void RemovePresentLibraries(Library lib, HashMap<String, Library> knownLibs, boolean addToSet) {
    /* we work top -> down */
    val toBeRemoved = new HashSet<String>();
    for (val sublib : lib.getLibraries()) {
      if (knownLibs.containsKey(sublib.getName().toUpperCase())) {
        toBeRemoved.add(sublib.getName());
      } else if (addToSet) {
        knownLibs.put(sublib.getName().toUpperCase(), sublib);
      }
    }
    for (val remove : toBeRemoved) {
      lib.removeLibrary(remove);
    }
    for (val sublib : lib.getLibraries())
      RemovePresentLibraries(sublib, knownLibs, addToSet);
  }
}
