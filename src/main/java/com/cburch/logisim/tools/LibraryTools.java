/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
  public static void ShowErrors(String LibName, HashMap<String, String> Messages) {
    OptionPane.showMessageDialog(
        null,
        Message(LibName, Messages),
        S.get("LibLoadErrors") + " " + LibName + " !",
        OptionPane.ERROR_MESSAGE);
  }

  private static String Message(String LibName, HashMap<String, String> Messages) {
    String Message = "";
    int item = 0;
    for (String myerror : Messages.keySet()) {
      item++;
      Message = Message.concat(item + ") " + Messages.get(myerror) + " \"" + myerror + "\".\n");
    }
    return Message;
  }

  public static void BuildToolList(Library lib, HashSet<String> Tools) {
    Iterator<? extends Tool> tooliter = lib.getTools().iterator();
    while (tooliter.hasNext()) {
      Tool tool = tooliter.next();
      Tools.add(tool.getName().toUpperCase());
    }
    for (Library sublib : lib.getLibraries()) BuildToolList(sublib, Tools);
  }

  public static boolean BuildToolList(Library lib, HashMap<String, AddTool> Tools) {
    boolean ret = true;
    if (!lib.getName().equals("Base")) {
      Iterator<? extends Tool> tooliter = lib.getTools().iterator();
      while (tooliter.hasNext()) {
        Tool tool1 = tooliter.next();
        if (Tools.containsKey(tool1.getName().toUpperCase())) ret = false;
        else Tools.put(tool1.getName().toUpperCase(), (AddTool) tool1);
      }
    }
    for (Library sublib : lib.getLibraries()) {
      ret &= BuildToolList(sublib, Tools);
    }
    return ret;
  }

  public static Circuit getCircuitFromLibs(Library lib, String UpperCaseName) {
    Circuit ret = null;
    if (lib instanceof LogisimFile) {
      LogisimFile llib = (LogisimFile) lib;
      for (Circuit circ : llib.getCircuits()) {
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

  public static ArrayList<String> LibraryCanBeMerged(
      HashSet<String> SourceTools, HashSet<String> NewTools) {
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

  public static HashMap<String, String> GetToolLocation(
      Library lib, String Location, ArrayList<String> UpercaseNames) {
    Iterator<? extends Tool> tooliter = lib.getTools().iterator();
    String MyLocation;
    HashMap<String, String> ret = new HashMap<String, String>();
    if (Location.isEmpty()) MyLocation = new String(lib.getName());
    else MyLocation = new String(Location + "->" + lib.getName());
    while (tooliter.hasNext()) {
      Tool tool = tooliter.next();
      if (UpercaseNames.contains(tool.getName().toUpperCase())) {
        ret.put(tool.getName(), MyLocation);
      }
    }
    for (Library sublib : lib.getLibraries()) {
      ret.putAll(GetToolLocation(sublib, MyLocation, UpercaseNames));
    }
    return ret;
  }

  public static boolean LibraryIsConform(
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
      HasErrors |= !LibraryIsConform(sublib, Names, Tools, Error);
    }
    return !HasErrors;
  }

  public static void BuildLibraryList(Library lib, HashMap<String, Library> Names) {
    Names.put(lib.getName().toUpperCase(), lib);
    for (Library sublib : lib.getLibraries()) {
      BuildLibraryList(sublib, Names);
    }
  }

  public static void RemovePresentLibraries(
      Library lib, HashMap<String, Library> KnownLibs, boolean AddToSet) {
    /* we work top -> down */
    HashSet<String> ToBeRemoved = new HashSet<String>();
    for (Library sublib : lib.getLibraries()) {
      if (KnownLibs.keySet().contains(sublib.getName().toUpperCase())) {
        ToBeRemoved.add(sublib.getName());
      } else if (AddToSet) {
        KnownLibs.put(sublib.getName().toUpperCase(), sublib);
      }
    }
    for (String remove : ToBeRemoved) {
      lib.removeLibrary(remove);
    }
    for (Library sublib : lib.getLibraries()) RemovePresentLibraries(sublib, KnownLibs, AddToSet);
  }
}
