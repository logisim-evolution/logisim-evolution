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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LibraryTools {
  public static void showErrors(String LibName, Map<String, String> Messages) {
    OptionPane.showMessageDialog(
        null,
        message(LibName, Messages),
        S.get("LibLoadErrors") + " " + LibName + " !",
        OptionPane.ERROR_MESSAGE);
  }

  private static String message(String LibName, Map<String, String> Messages) {
    var Message = "";
    var item = 0;
    for (final var myerror : Messages.keySet()) {
      item++;
      Message = Message.concat(item + ") " + Messages.get(myerror) + " \"" + myerror + "\".\n");
    }
    return Message;
  }

  public static void buildToolList(Library lib, Set<String> Tools) {
    for (final var tool : lib.getTools()) {
      Tools.add(tool.getName().toUpperCase());
    }
    for (final var sublib : lib.getLibraries()) {
      buildToolList(sublib, Tools);
    }
  }

  public static boolean buildToolList(Library lib, Map<String, AddTool> Tools) {
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

  // FIXME: why `upperCaseName` even matters here if we do case insensitive comparision?
  public static Circuit getCircuitFromLibs(Library lib, String upperCaseName) {
    Circuit ret = null;
    if (lib instanceof LogisimFile llib) {
      for (final var circ : llib.getCircuits()) {
        if (circ.getName().equalsIgnoreCase(upperCaseName)) return circ;
      }
    }
    for (final var libs : lib.getLibraries()) {
      if (libs instanceof LoadedLibrary lib1) {
        ret = getCircuitFromLibs(lib1.getBase(), upperCaseName);
      } else ret = getCircuitFromLibs(libs, upperCaseName);
      if (ret != null) return ret;
    }
    return null;
  }

  // FIXME: method name is odd.
  public static List<String> libraryCanBeMerged(Set<String> SourceTools, Set<String> NewTools) {
    final var ret = new ArrayList<String>();
    for (final var This : NewTools) {
      if (SourceTools.contains(This)) {
        ret.add(This);
      }
    }
    return ret;
  }

  // Why name case matters that it is reflected in argument `uppercasedNames` name?
  public static Map<String, String> getToolLocation(Library lib, String location, List<String> upercasedNames) {
    final var toolIter = lib.getTools().iterator();
    final var ret = new HashMap<String, String>();
    final var MyLocation = (location.isEmpty()) ? lib.getName() : location + "->" + lib.getName();
    while (toolIter.hasNext()) {
      final var tool = toolIter.next();
      if (upercasedNames.contains(tool.getName().toUpperCase())) {
        ret.put(tool.getName(), MyLocation);
      }
    }
    for (final var sublib : lib.getLibraries()) {
      ret.putAll(getToolLocation(sublib, MyLocation, upercasedNames));
    }
    return ret;
  }

  public static boolean isLibraryConform(Library lib, Set<String> names, Set<String> tools, Map<String, String> error) {
    final var toolIter = lib.getTools().iterator();
    var hasErrors = false;
    while (toolIter.hasNext()) {
      final var tool = toolIter.next();
      if (tools.contains(tool.getName().toUpperCase())) {
        hasErrors = true;
        if (!error.containsKey(tool.getName())) {
          error.put(tool.getName(), S.get("LibraryHasDuplicatedTools"));
        }
      }
      tools.add(tool.getName().toUpperCase());
    }
    for (final var sublib : lib.getLibraries()) {
      if (names.contains(sublib.getName().toUpperCase())) {
        hasErrors = true;
        if (!error.containsKey(sublib.getName())) {
          error.put(sublib.getName(), S.get("LibraryHasDuplicatedSublibraries"));
        }
      }
      names.add(sublib.getName().toUpperCase());
      hasErrors |= !isLibraryConform(sublib, names, tools, error);
    }
    return !hasErrors;
  }

  public static void buildLibraryList(Library lib, Map<String, Library> Names) {
    Names.put(lib.getName().toUpperCase(), lib);
    for (final var sublib : lib.getLibraries()) {
      buildLibraryList(sublib, Names);
    }
  }

  public static void removePresentLibraries(Library lib, Map<String, Library> knownLibs, boolean addToSet) {
    /* we work top -> down */
    final var toBeRemoved = new HashSet<String>();
    for (final var sublib : lib.getLibraries()) {
      if (knownLibs.containsKey(sublib.getName().toUpperCase())) {
        toBeRemoved.add(sublib.getName());
      } else if (addToSet) {
        knownLibs.put(sublib.getName().toUpperCase(), sublib);
      }
    }
    for (final var remove : toBeRemoved) {
      lib.removeLibrary(remove);
    }
    for (final var sublib : lib.getLibraries())
      removePresentLibraries(sublib, knownLibs, addToSet);
  }
}
