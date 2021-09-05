/*
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

package com.cburch.logisim.file;

import static com.cburch.logisim.file.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.LibraryTools;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarFile;

public class LogisimFileActions {
  private static class AddCircuit extends Action {
    private final Circuit circuit;

    AddCircuit(Circuit circuit) {
      this.circuit = circuit;
    }

    @Override
    public void doIt(Project proj) {
      proj.getLogisimFile().addCircuit(circuit);
    }

    @Override
    public String getName() {
      return S.get("addCircuitAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().removeCircuit(circuit);
    }
  }

  private static class AddVhdl extends Action {
    private final VhdlContent vhdl;

    AddVhdl(VhdlContent vhdl) {
      this.vhdl = vhdl;
    }

    @Override
    public void doIt(Project proj) {
      proj.getLogisimFile().addVhdlContent(vhdl);
    }

    @Override
    public String getName() {
      return S.get("addVhdlAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().removeVhdl(vhdl);
    }
  }

  private static class MergeFile extends Action {
    private final ArrayList<Circuit> mergedCircuits = new ArrayList<>();
    private final ArrayList<File> jarLibs = new ArrayList<>();
    private final ArrayList<File> logiLibs = new ArrayList<>();

    MergeFile(LogisimFile mergelib, LogisimFile source) {
      final var libNames = new HashMap<String, Library>();
      final var toolList = new HashSet<String>();
      final var errors = new HashMap<String, String>();
      var canContinue = true;
      for (final var lib : source.getLibraries()) {
        LibraryTools.BuildLibraryList(lib, libNames);
      }
      LibraryTools.BuildToolList(source, toolList);
      LibraryTools.RemovePresentLibraries(mergelib, libNames, false);
      if (LibraryTools.LibraryIsConform(
          mergelib, new HashSet<>(), new HashSet<>(), errors)) {
        /* Okay the library is now ready for merge */
        for (final var lib : mergelib.getLibraries()) {
          final var newToolList = new HashSet<String>();
          LibraryTools.BuildToolList(lib, newToolList);
          final var ret = LibraryTools.LibraryCanBeMerged(toolList, newToolList);
          if (!ret.isEmpty()) {
            final var Location = "";
            final var toolNames = LibraryTools.GetToolLocation(source, Location, ret);
            for (final var key : toolNames.keySet()) {
              var solStr = S.get("LibMergeFailure2") + " a) ";
              final var errLoc = toolNames.get(key);
              final var errParts = errLoc.split("->");
              if (errParts.length > 1) {
                solStr = solStr.concat(S.get("LibMergeFailure4", errParts[1]));
              } else {
                solStr = solStr.concat(S.get("LibMergeFailure3", key));
              }
              solStr = solStr.concat(" " + S.get("LibMergeFailure5") + " b) ");
              solStr = solStr.concat(S.get("LibMergeFailure6", lib.getName()));
              errors.put(solStr, S.get("LibMergeFailure1", lib.getName(), key));
            }
            canContinue = false;
          }
          final var splits = mergelib.getLoader().getDescriptor(lib).split("#");
          final var TheFile = mergelib.getLoader().getFileFor(splits[1], null);
          if (splits[0].equals("file"))
            logiLibs.add(TheFile);
          else if (splits[0].equals("jar"))
            jarLibs.add(TheFile);
        }
        if (!canContinue) {
          LibraryTools.ShowErrors(mergelib.getName(), errors);
          logiLibs.clear();
          jarLibs.clear();
          return;
        }
        /* Okay merged the missing libraries, now add the circuits */
        for (final var circ : mergelib.getCircuits()) {
          final var circName = circ.getName().toUpperCase();
          if (toolList.contains(circName)) {
            final var ret = new ArrayList<String>();
            ret.add(circName);
            final var toolNames = LibraryTools.GetToolLocation(source, "", ret);
            for (final var key : toolNames.keySet()) {
              final var errLoc = toolNames.get(key);
              final var errParts = errLoc.split("->");
              if (errParts.length > 1) {
                var solStr = S.get("LibMergeFailure2") + " a) ";
                solStr = solStr.concat(S.get("LibMergeFailure4", errParts[1]));
                solStr = solStr.concat(" " + S.get("LibMergeFailure5") + " b) ");
                solStr = solStr.concat(S.get("LibMergeFailure8", circ.getName()));
                errors.put(solStr, S.get("LibMergeFailure7", key, errParts[1]));
                canContinue = false;
              }
            }
            if (canContinue) {
              final var circ1 = LibraryTools.getCircuitFromLibs(source, circName);
              if (circ1 == null) {
                OptionPane.showMessageDialog(
                    null,
                    "Fatal internal error: Cannot find a referenced circuit",
                    "LogosimFileAction:",
                    OptionPane.ERROR_MESSAGE);
                canContinue = false;
              } else if (!CircuitsAreEqual(circ1, circ)) {
                final var Reponse =
                    OptionPane.showConfirmDialog(
                        null,
                        S.get("FileMergeQuestion", circ.getName()),
                        S.get("FileMergeTitle"),
                        OptionPane.YES_NO_OPTION);
                if (Reponse == OptionPane.YES_OPTION) {
                  mergedCircuits.add(circ);
                }
              }
            }
          } else {
            mergedCircuits.add(circ);
          }
        }
        if (!canContinue) {
          LibraryTools.ShowErrors(mergelib.getName(), errors);
          logiLibs.clear();
          jarLibs.clear();
          mergedCircuits.clear();
          return;
        }
      } else LibraryTools.ShowErrors(mergelib.getName(), errors);
    }

    private boolean CircuitsAreEqual(Circuit orig, Circuit newone) {
      final var origComps = new HashMap<Location, Component>();
      final var newComps = new HashMap<Location, Component>();
      for (final var comp : orig.getWires()) {
        origComps.put(comp.getLocation(), comp);
      }
      for (final var comp : orig.getNonWires()) {
        origComps.put(comp.getLocation(), comp);
      }
      for (final var comp : newone.getWires()) {
        newComps.put(comp.getLocation(), comp);
      }
      for (final var comp : newone.getNonWires()) {
        newComps.put(comp.getLocation(), comp);
      }
      final var it = newComps.keySet().iterator();
      while (it.hasNext()) {
        final var loc = it.next();
        if (origComps.containsKey(loc)) {
          final var comp1 = newComps.get(loc);
          final var comp2 = newComps.get(loc);
          if (comp1.getFactory().getName().equals(comp2.getFactory().getName())) {
            if (comp1.getFactory().getName().equals("Wire")) {
              final var wir1 = (Wire) comp1;
              final var wir2 = (Wire) comp2;
              if (wir1.overlaps(wir2, true)) {
                it.remove();
                origComps.remove(loc);
              } else {
                System.out.println("No Wire Overlap");
              }
            } else {
              if (comp1.getAttributeSet().equals(comp2.getAttributeSet())) {
                it.remove();
                origComps.remove(loc);
              } else {
                System.out.println("Different component");
              }
            }
          }
        }
      }
      return origComps.isEmpty() & newComps.isEmpty();
    }

    @Override
    public void doIt(Project proj) {
      final var loader = proj.getLogisimFile().getLoader();
      /* first we are going to merge the jar libraries */
      for (final var jarLib : jarLibs) {
        String className = null;
        try (final var jarFile = new JarFile(jarLib)) {
          final var manifest = jarFile.getManifest();
          className = manifest.getMainAttributes().getValue("Library-Class");
        } catch (IOException e) {
          // if opening the JAR file failed, do nothing
        }
        // if the class name was not found, go back to the good old dialog
        if (className == null) {
          className =
              OptionPane.showInputDialog(
                  proj.getFrame(),
                  S.get("jarClassNamePrompt"),
                  S.get("jarClassNameTitle"),
                  OptionPane.QUESTION_MESSAGE);
          // if user canceled selection, abort
          if (className == null)
            continue;
        }
        final var lib = loader.loadJarLibrary(jarLib, className);
        if (lib != null) {
          proj.doAction(LogisimFileActions.loadLibrary(lib, proj.getLogisimFile()));
        }
      }
      jarLibs.clear();
      /* next we are going to load the logisimfile  libraries */
      for (final var logiLib : logiLibs) {
        final var put = loader.loadLogisimLibrary(logiLib);
        if (put != null) {
          proj.doAction(LogisimFileActions.loadLibrary(put, proj.getLogisimFile()));
        }
      }
      logiLibs.clear();
      // this we are going to do in two steps, first add the circuits with inputs,
      // outputs and wires
      for (final var circ : mergedCircuits) {
        Circuit newCircuit = null;
        var replace = false;
        for (final var circs : proj.getLogisimFile().getCircuits())
          if (circs.getName().equalsIgnoreCase(circ.getName())) {
            newCircuit = circs;
            replace = true;
          }
        if (newCircuit == null) newCircuit = new Circuit(circ.getName(), proj.getLogisimFile(), proj);
        final var result = new CircuitMutation(newCircuit);
        if (replace) result.clear();
        for (final var wir : circ.getWires()) {
          result.add(Wire.create(wir.getEnd0(), wir.getEnd1()));
        }
        for (final var comp : circ.getNonWires()) {
          if (comp.getFactory() instanceof Pin) {
            result.add(
                Pin.FACTORY.createComponent(
                    comp.getLocation(), (AttributeSet) comp.getAttributeSet().clone()));
          }
        }
        if (!replace) {
          result.execute();
          proj.doAction(LogisimFileActions.addCircuit(newCircuit));
        } else proj.doAction(result.toAction(S.getter("replaceCircuitAction")));
      }
      final var availableTools = new HashMap<String, AddTool>();
      LibraryTools.BuildToolList(proj.getLogisimFile(), availableTools);
      /* in the second step we are going to add the rest of the contents */
      for (final var circ : mergedCircuits) {
        final var newCirc = proj.getLogisimFile().getCircuit(circ.getName());
        if (newCirc != null) {
          final var result = new CircuitMutation(newCirc);
          for (final var comp : circ.getNonWires()) {
            if (!(comp.getFactory() instanceof Pin)) {
              final var current = availableTools.get(comp.getFactory().getName().toUpperCase());
              if (current != null) {
                result.add(current.getFactory().createComponent(comp.getLocation(), (AttributeSet) comp.getAttributeSet().clone()));
              } else if (comp.getFactory().getName().equals("Text")) {
                result.add(Text.FACTORY.createComponent(comp.getLocation(), (AttributeSet) comp.getAttributeSet().clone()));
              } else System.out.println("Not found:" + comp.getFactory().getName());
            }
          }
          result.execute();
        }
      }
      mergedCircuits.clear();
    }

    @Override
    public String getName() {
      return S.get("mergeFileAction");
    }

    @Override
    public boolean isModification() {
      return false;
    }

    @Override
    public void undo(Project proj) {}
  }

  private static class LoadLibraries extends Action {
    private final ArrayList<Library> mergedLibs = new ArrayList<>();

    LoadLibraries(Library[] libs, LogisimFile source) {
      final var libNames = new HashMap<String, Library>();
      final var ToolList = new HashSet<String>();
      final var errors = new HashMap<String, String>();
      for (final var lib : source.getLibraries()) {
        LibraryTools.BuildLibraryList(lib, libNames);
      }
      LibraryTools.BuildToolList(source, ToolList);
      for (final var lib : libs) {
        if (libNames.containsKey(lib.getName().toUpperCase())) {
          OptionPane.showMessageDialog(
              null,
              "\"" + lib.getName() + "\": " + S.get("LibraryAlreadyLoaded"),
              S.get("LibLoadErrors") + " " + lib.getName() + " !",
              OptionPane.WARNING_MESSAGE);
        } else {
          LibraryTools.RemovePresentLibraries(lib, libNames, false);
          if (LibraryTools.LibraryIsConform(lib, new HashSet<>(), new HashSet<>(), errors)) {
            final var addedToolList = new HashSet<String>();
            LibraryTools.BuildToolList(lib, addedToolList);
            for (final var tool : addedToolList)
              if (ToolList.contains(tool))
                errors.put(tool, S.get("LibraryMultipleToolError"));
            if (errors.keySet().isEmpty()) {
              LibraryTools.BuildLibraryList(lib, libNames);
              ToolList.addAll(addedToolList);
              mergedLibs.add(lib);
            } else
              LibraryTools.ShowErrors(lib.getName(), errors);
          } else
            LibraryTools.ShowErrors(lib.getName(), errors);
        }
      }
    }

    @Override
    public void doIt(Project proj) {
      for (final var lib : mergedLibs) {
        if (lib instanceof LoadedLibrary) {
          final var lib1 = (LoadedLibrary) lib;
          if (lib1.getBase() instanceof LogisimFile) {
            repair(proj, lib1.getBase());
          }
        } else if (lib instanceof LogisimFile) {
          repair(proj, lib);
        }
        proj.getLogisimFile().addLibrary(lib);
      }
    }

    private void repair(Project proj, Library lib) {
      final var availableTools = new HashMap<String, AddTool>();
      LibraryTools.BuildToolList(proj.getLogisimFile(), availableTools);
      if (lib instanceof LogisimFile) {
        final var thisLib = (LogisimFile) lib;
        for (final var circ : thisLib.getCircuits()) {
          for (final var tool : circ.getNonWires()) {
            if (availableTools.containsKey(tool.getFactory().getName().toUpperCase())) {
              final var current = availableTools.get(tool.getFactory().getName().toUpperCase());
              if (current != null) {
                tool.setFactory(current.getFactory());
              } else if (tool.getFactory().getName().equals("Text")) {
                final var newComp = Text.FACTORY.createComponent(tool.getLocation(), (AttributeSet) tool.getAttributeSet().clone());
                tool.setFactory(newComp.getFactory());
              } else
                System.out.println("Not found:" + tool.getFactory().getName());
            }
          }
        }
      }
      for (final var libs : lib.getLibraries()) {
        repair(proj, libs);
      }
    }

    @Override
    public boolean isModification() {
      return !mergedLibs.isEmpty();
    }

    @Override
    public String getName() {
      return (mergedLibs.size() <= 1) ? S.get("loadLibraryAction") : S.get("loadLibrariesAction");
    }

    @Override
    public void undo(Project proj) {
      for (final var lib : mergedLibs) proj.getLogisimFile().removeLibrary(lib);
    }
  }

  private static class MoveCircuit extends Action {
    private final AddTool tool;
    private int fromIndex;
    private final int toIndex;

    MoveCircuit(AddTool tool, int toIndex) {
      this.tool = tool;
      this.toIndex = toIndex;
    }

    @Override
    public Action append(Action other) {
      final var ret = new MoveCircuit(tool, ((MoveCircuit) other).toIndex);
      ret.fromIndex = this.fromIndex;
      return ret.fromIndex == ret.toIndex ? null : ret;
    }

    @Override
    public void doIt(Project proj) {
      fromIndex = proj.getLogisimFile().getTools().indexOf(tool);
      proj.getLogisimFile().moveCircuit(tool, toIndex);
    }

    @Override
    public String getName() {
      return S.get("moveCircuitAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      return other instanceof MoveCircuit && ((MoveCircuit) other).tool == this.tool;
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().moveCircuit(tool, fromIndex);
    }
  }

  private static class RemoveCircuit extends Action {
    private final Circuit circuit;
    private int index;

    RemoveCircuit(Circuit circuit) {
      this.circuit = circuit;
    }

    @Override
    public void doIt(Project proj) {
      index = proj.getLogisimFile().indexOfCircuit(circuit);
      proj.getLogisimFile().removeCircuit(circuit);
    }

    @Override
    public String getName() {
      return S.get("removeCircuitAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().addCircuit(circuit, index);
    }
  }

  private static class RemoveVhdl extends Action {
    private final VhdlContent vhdl;
    private int index;

    RemoveVhdl(VhdlContent vhdl) {
      this.vhdl = vhdl;
    }

    @Override
    public void doIt(Project proj) {
      index = proj.getLogisimFile().indexOfVhdl(vhdl);
      proj.getLogisimFile().removeVhdl(vhdl);
    }

    @Override
    public String getName() {
      return S.get("removeVhdlAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().addVhdlContent(vhdl, index);
    }
  }

  private static class RevertAttributeValue {
    private final AttributeSet attrs;
    private final Attribute<Object> attr;
    private final Object value;

    RevertAttributeValue(AttributeSet attrs, Attribute<Object> attr, Object value) {
      this.attrs = attrs;
      this.attr = attr;
      this.value = value;
    }
  }

  public static Action addVhdl(VhdlContent vhdl) {
    return new AddVhdl(vhdl);
  }

  private static class RevertDefaults extends Action {
    private Options oldOpts;
    private ArrayList<Library> libraries = null;
    private final ArrayList<RevertAttributeValue> attrValues;

    RevertDefaults() {
      libraries = null;
      attrValues = new ArrayList<>();
    }

    private void copyToolAttributes(Library srcLib, Library dstLib) {
      for (final var srcTool : srcLib.getTools()) {
        final var srcAttrs = srcTool.getAttributeSet();
        final var dstTool = dstLib.getTool(srcTool.getName());
        if (srcAttrs != null && dstTool != null) {
          final var dstAttrs = dstTool.getAttributeSet();
          for (Attribute<?> attrBase : srcAttrs.getAttributes()) {
            @SuppressWarnings("unchecked")
            final var attr = (Attribute<Object>) attrBase;
            final var srcValue = srcAttrs.getValue(attr);
            final var dstValue = dstAttrs.getValue(attr);
            if (!dstValue.equals(srcValue)) {
              dstAttrs.setValue(attr, srcValue);
              attrValues.add(new RevertAttributeValue(dstAttrs, attr, dstValue));
            }
          }
        }
      }
    }

    @Override
    public void doIt(Project proj) {
      final var src = ProjectActions.createNewFile(proj);
      final var dst = proj.getLogisimFile();

      copyToolAttributes(src, dst);
      for (final var srcLib : src.getLibraries()) {
        var dstLib = dst.getLibrary(srcLib.getName());
        if (dstLib == null) {
          final var desc = src.getLoader().getDescriptor(srcLib);
          dstLib = dst.getLoader().loadLibrary(desc);
          proj.getLogisimFile().addLibrary(dstLib);
          if (libraries == null) libraries = new ArrayList<>();
          libraries.add(dstLib);
        }
        copyToolAttributes(srcLib, dstLib);
      }

      final var newOpts = proj.getOptions();
      oldOpts = new Options();
      oldOpts.copyFrom(newOpts, dst);
      newOpts.copyFrom(src.getOptions(), dst);
    }

    @Override
    public String getName() {
      return S.get("revertDefaultsAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getOptions().copyFrom(oldOpts, proj.getLogisimFile());

      for (final var attrValue : attrValues) {
        attrValue.attrs.setValue(attrValue.attr, attrValue.value);
      }

      if (libraries != null) {
        for (final var lib : libraries) {
          proj.getLogisimFile().removeLibrary(lib);
        }
      }
    }
  }

  private static class SetMainCircuit extends Action {
    private Circuit oldval;
    private final Circuit newval;

    SetMainCircuit(Circuit circuit) {
      newval = circuit;
    }

    @Override
    public void doIt(Project proj) {
      oldval = proj.getLogisimFile().getMainCircuit();
      proj.getLogisimFile().setMainCircuit(newval);
    }

    @Override
    public String getName() {
      return S.get("setMainCircuitAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().setMainCircuit(oldval);
    }
  }

  private static class UnloadLibraries extends Action {
    private final Library[] libs;

    UnloadLibraries(Library[] libs) {
      this.libs = libs;
    }

    @Override
    public void doIt(Project proj) {
      for (var i = libs.length - 1; i >= 0; i--) {
        proj.getLogisimFile().removeLibrary(libs[i]);
      }
    }

    @Override
    public String getName() {
      return (libs.length == 1) ? S.get("unloadLibraryAction") : S.get("unloadLibrariesAction");
    }

    @Override
    public void undo(Project proj) {
      for (final var lib : libs) {
        proj.getLogisimFile().addLibrary(lib);
      }
    }
  }

  public static Action addCircuit(Circuit circuit) {
    return new AddCircuit(circuit);
  }

  public static Action MergeFile(LogisimFile mergelib, LogisimFile source) {
    return new MergeFile(mergelib, source);
  }

  public static Action loadLibraries(Library[] libs, LogisimFile source) {
    return new LoadLibraries(libs, source);
  }

  public static Action loadLibrary(Library lib, LogisimFile source) {
    return new LoadLibraries(new Library[] {lib}, source);
  }

  public static Action moveCircuit(AddTool tool, int toIndex) {
    return new MoveCircuit(tool, toIndex);
  }

  public static Action removeCircuit(Circuit circuit) {
    return new RemoveCircuit(circuit);
  }

  public static Action removeVhdl(VhdlContent vhdl) {
    return new RemoveVhdl(vhdl);
  }

  public static Action revertDefaults() {
    return new RevertDefaults();
  }

  public static Action setMainCircuit(Circuit circuit) {
    return new SetMainCircuit(circuit);
  }

  public static Action unloadLibraries(Library[] libs) {
    return new UnloadLibraries(libs);
  }

  public static Action unloadLibrary(Library lib) {
    return new UnloadLibraries(new Library[] {lib});
  }

  private LogisimFileActions() {}
}
