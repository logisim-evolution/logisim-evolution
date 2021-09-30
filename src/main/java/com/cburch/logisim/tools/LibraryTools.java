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
import java.util.Iterator;

public class LibraryTools {
  public static void showErrors(String LibName, HashMap<String, String> Messages) {
    OptionPane.showMessageDialog(
        null,
        Message(LibName, Messages),
        S.get("LibLoadErrors") + " " + LibName + " !",
        OptionPane.ERROR_MESSAGE);
  }

  private static String Message(String LibName, HashMap<String, String> Messages) {
    var Message = "";
    var item = 0;
    for (final var myerror : Messages.keySet()) {
      item++;
      Message = Message.concat(item + ") " + Messages.get(myerror) + " \"" + myerror + "\".\n");
    }
    return Message;
  }

  public static void buildToolList(Library lib, HashSet<String> Tools) {
    for (Tool tool : lib.getTools()) {
      Tools.add(tool.getName().toUpperCase());
    }
    for (Library sublib : lib.getLibraries()) buildToolList(sublib, Tools);
  }

  public static boolean buildToolList(Library lib, HashMap<String, AddTool> Tools) {
    var ret = true;
    if (!lib.getName().equals("Base")) {
      for (final var tool1 : lib.getTools()) {
        if (Tools.containsKey(tool1.getName().toUpperCase()))
          ret = false;
        else
          Tools.put(tool1.getName().toUpperCase(), (AddTool) tool1);
      }
    }
    for (final var sublib : lib.getLibraries()) {
      ret &= buildToolList(sublib, Tools);
    }
    return ret;
  }

  public static Circuit getCircuitFromLibs(Library lib, String UpperCaseName) {
    Circuit ret = null;
    if (lib instanceof LogisimFile) {
      LogisimFile llib = (LogisimFile) lib;
      for (final var circ : llib.getCircuits()) {
        if (circ.getName().toUpperCase().equals(UpperCaseName)) return circ;
      }
    }
    for (Library libs : lib.getLibraries()) {
      if (libs instanceof LoadedLibrary) {
        LoadedLibrary lib1 = (LoadedLibrary) libs;
        ret = getCircuitFromLibs(lib1.getBase(), UpperCaseName);
      } else ret = getCircuitFromLibs(libs, UpperCaseName);
      if (ret != null) return ret;
    }
    return null;
  }

  // FIXME: method name is odd.
  public static ArrayList<String> libraryCanBeMerged(HashSet<String> SourceTools, HashSet<String> NewTools) {
    final var ret = new ArrayList<String>();
    for (final var This : NewTools) {
      if (SourceTools.contains(This)) {
        ret.add(This);
      }
    }
    return ret;
  }

  public static HashMap<String, String> getToolLocation(
      Library lib, String Location, ArrayList<String> UpercaseNames) {
    Iterator<? extends Tool> tooliter = lib.getTools().iterator();
    String MyLocation;
    final var ret = new HashMap<String, String>();
    if (Location.isEmpty()) MyLocation = lib.getName();
    else MyLocation = Location + "->" + lib.getName();
    while (tooliter.hasNext()) {
      Tool tool = tooliter.next();
      if (UpercaseNames.contains(tool.getName().toUpperCase())) {
        ret.put(tool.getName(), MyLocation);
      }
    }
    for (final var sublib : lib.getLibraries()) {
      ret.putAll(getToolLocation(sublib, MyLocation, UpercaseNames));
    }
    return ret;
  }

  public static boolean isLibraryConform(
      Library lib, HashSet<String> Names, HashSet<String> Tools, HashMap<String, String> Error) {
    Iterator<? extends Tool> tooliter = lib.getTools().iterator();
    boolean HasErrors = false;
    while (tooliter.hasNext()) {
      Tool tool = tooliter.next();
      if (Tools.contains(tool.getName().toUpperCase())) {
        HasErrors = true;
        if (!Error.containsKey(tool.getName())) {
          Error.put(tool.getName(), S.get("LibraryHasDuplicatedTools"));
        }
      }
      Tools.add(tool.getName().toUpperCase());
    }
    for (Library sublib : lib.getLibraries()) {
      if (Names.contains(sublib.getName().toUpperCase())) {
        HasErrors = true;
        if (!Error.containsKey(sublib.getName())) {
          Error.put(sublib.getName(), S.get("LibraryHasDuplicatedSublibraries"));
        }
      }
      Names.add(sublib.getName().toUpperCase());
      HasErrors |= !isLibraryConform(sublib, Names, Tools, Error);
    }
    return !HasErrors;
  }

  public static void buildLibraryList(Library lib, HashMap<String, Library> Names) {
    Names.put(lib.getName().toUpperCase(), lib);
    for (final var sublib : lib.getLibraries()) {
      buildLibraryList(sublib, Names);
    }
  }

  public static void removePresentLibraries(
      Library lib, HashMap<String, Library> KnownLibs, boolean AddToSet) {
    /* we work top -> down */
    final var ToBeRemoved = new HashSet<String>();
    for (final var sublib : lib.getLibraries()) {
      if (KnownLibs.containsKey(sublib.getName().toUpperCase())) {
        ToBeRemoved.add(sublib.getName());
      } else if (AddToSet) {
        KnownLibs.put(sublib.getName().toUpperCase(), sublib);
      }
    }
    for (final var remove : ToBeRemoved) {
      lib.removeLibrary(remove);
    }
    for (final var sublib : lib.getLibraries())
      removePresentLibraries(sublib, KnownLibs, AddToSet);
  }
}
