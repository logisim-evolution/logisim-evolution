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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LibraryTools {
  public static void ShowErrors(String LibName, Map<String, String> Messages) {
    OptionPane.showMessageDialog(
        null,
        Message(LibName, Messages),
        S.get("LibLoadErrors") + " " + LibName + " !",
        OptionPane.ERROR_MESSAGE);
  }

  private static String Message(String LibName, Map<String, String> Messages) {
    var Message = "";
    var item = 0;
    for (final var myerror : Messages.keySet()) {
      item++;
      Message = Message.concat(item + ") " + Messages.get(myerror) + " \"" + myerror + "\".\n");
    }
    return Message;
  }

  public static void BuildToolList(Library lib, Set<String> Tools) {
    for (Tool tool : lib.getTools()) {
      Tools.add(tool.getName().toUpperCase());
    }
    for (Library sublib : lib.getLibraries()) BuildToolList(sublib, Tools);
  }

  public static boolean BuildToolList(Library lib, Map<String, AddTool> Tools) {
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
      ret &= BuildToolList(sublib, Tools);
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

  public static List<String> LibraryCanBeMerged(Set<String> SourceTools, Set<String> NewTools) {
    final var ret = new ArrayList<String>();
    for (final var This : NewTools) {
      if (SourceTools.contains(This)) {
        ret.add(This);
      }
    }
    return ret;
  }

  public static Map<String, String> GetToolLocation(Library lib, String Location, List<String> UpercaseNames) {
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
      ret.putAll(GetToolLocation(sublib, MyLocation, UpercaseNames));
    }
    return ret;
  }

  public static boolean LibraryIsConform(Library lib, Set<String> Names, Set<String> Tools, Map<String, String> Error) {
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
      HasErrors |= !LibraryIsConform(sublib, Names, Tools, Error);
    }
    return !HasErrors;
  }

  public static void BuildLibraryList(Library lib, Map<String, Library> Names) {
    Names.put(lib.getName().toUpperCase(), lib);
    for (final var sublib : lib.getLibraries()) {
      BuildLibraryList(sublib, Names);
    }
  }

  public static void RemovePresentLibraries(Library lib, Map<String, Library> KnownLibs, boolean AddToSet) {
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
      RemovePresentLibraries(sublib, KnownLibs, AddToSet);
  }
}
