/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static com.cburch.logisim.file.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
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
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

public final class LogisimFileActions {
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
        LibraryTools.buildLibraryList(lib, libNames);
      }
      LibraryTools.buildToolList(source, toolList);
      LibraryTools.removePresentLibraries(mergelib, libNames, false);
      if (LibraryTools.isLibraryConform(
          mergelib, new HashSet<>(), new HashSet<>(), errors)) {
        /* Okay the library is now ready for merge */
        for (final var lib : mergelib.getLibraries()) {
          final var newToolList = new HashSet<String>();
          LibraryTools.buildToolList(lib, newToolList);
          final var ret = LibraryTools.libraryCanBeMerged(toolList, newToolList);
          if (!ret.isEmpty()) {
            final var Location = "";
            final var toolNames = LibraryTools.getToolLocation(source, Location, ret);
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
          final var theFile = mergelib.getLoader().getFileFor(splits[1], null);
          if ("file".equals(splits[0]))
            logiLibs.add(theFile);
          else if ("jar".equals(splits[0]))
            jarLibs.add(theFile);
        }
        if (!canContinue) {
          LibraryTools.showErrors(mergelib.getName(), errors);
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
            final var toolNames = LibraryTools.getToolLocation(source, "", ret);
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
              } else if (!areCircuitsEqual(circ1, circ)) {
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
          LibraryTools.showErrors(mergelib.getName(), errors);
          logiLibs.clear();
          jarLibs.clear();
          mergedCircuits.clear();
          return;
        }
      } else LibraryTools.showErrors(mergelib.getName(), errors);
    }

    private boolean areCircuitsEqual(Circuit orig, Circuit newone) {
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
            if ("Wire".equals(comp1.getFactory().getName())) {
              final var wire1 = (Wire) comp1;
              final var wire2 = (Wire) comp2;
              if (wire1.overlaps(wire2, true)) {
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
      return origComps.isEmpty() && newComps.isEmpty();
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
        for (final var circs : proj.getLogisimFile().getCircuits()) {
          if (circs.getName().equalsIgnoreCase(circ.getName())) {
            newCircuit = circs;
            replace = true;
          }
        }
        if (newCircuit == null) newCircuit = new Circuit(circ.getName(), proj.getLogisimFile(), proj);
        CircuitAttributes.copyStaticAttributes(newCircuit.getStaticAttributes(), circ.getStaticAttributes());
        final var result = new CircuitMutation(newCircuit);
        if (replace) {
          result.clear();
        }
        for (final var comp : circ.getNonWires()) {
          if (comp.getFactory() instanceof Pin) {
            result.add(Pin.FACTORY.createComponent(comp.getLocation(), (AttributeSet) comp.getAttributeSet().clone()));
          }
        }
        for (final var wir : circ.getWires()) {
          result.add(Wire.create(wir.getEnd0(), wir.getEnd1()));
        }
        if (!replace) {
          result.execute();
          proj.doAction(LogisimFileActions.addCircuit(newCircuit));
        } else {
          proj.doAction(result.toAction(S.getter("replaceCircuitAction")));
        }
      }
      final var availableTools = new HashMap<String, AddTool>();
      LibraryTools.buildToolList(proj.getLogisimFile(), availableTools);
      // in the second step we are going to add the rest of the contents
      for (final var circ : mergedCircuits) {
        final var newCirc = proj.getLogisimFile().getCircuit(circ.getName());
        if (newCirc != null) {
          final var result = new CircuitMutation(newCirc);
          for (final var comp : circ.getNonWires()) {
            if (!(comp.getFactory() instanceof Pin)) {
              final var current = availableTools.get(comp.getFactory().getName().toUpperCase());
              if (current != null) {
                final var factory = current.getFactory();
                if (factory instanceof SubcircuitFactory subcirc) {
                  final var newAttrs = factory.createAttributeSet();
                  CircuitAttributes.copyInto(comp.getAttributeSet(), newAttrs);
                  result.add(factory.createComponent(comp.getLocation(), newAttrs));
                } else {
                  result.add(factory.createComponent(comp.getLocation(), (AttributeSet) comp.getAttributeSet().clone()));
                }
              } else if ("Text".equals(comp.getFactory().getName())) {
                result.add(Text.FACTORY.createComponent(comp.getLocation(), (AttributeSet) comp.getAttributeSet().clone()));
              } else System.out.println("Not found:" + comp.getFactory().getName());
            }
          }
          proj.doAction(result.toAction(S.getter("replaceCircuitAction")));
        }
      }
      // Last pass, restore the custom appearance
      for (final var circ : mergedCircuits) {
        if (circ.getAppearance().hasCustomAppearance()) {
          final var newCirc = proj.getLogisimFile().getCircuit(circ.getName());
          newCirc.getAppearance().repairCustomAppearance(circ.getAppearance().getCustomObjectsFromBottom(), proj, newCirc);
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
    public void undo(Project proj) {
      // If it does nothing, then we should never call it, so let's throw some meat.
      throw new UnsupportedOperationException();
    }
  }

  private static class LoadLibraries extends Action {
    private final List<Library> mergedLibs = new ArrayList<>();
    private final Set<String> baseLibsToEnable = new HashSet<>();

    LoadLibraries(Library[] libs, LogisimFile source) {
      final var libNames = new HashMap<String, Library>();
      final var toolList = new HashSet<String>();
      final var errors = new HashMap<String, String>();
      for (final var newLib : libs) {
        // first cleanup step: remove unused libraries from loaded library
        LibraryManager.removeUnusedLibraries(newLib);
        // second cleanup step: promote base libraries
        baseLibsToEnable.addAll(LibraryManager.getUsedBaseLibraries(newLib));
      }
      // promote the none visible base libraries to toplevel
      final var builtinLibraries = LibraryManager.getBuildinNames(source.getLoader());
      for (final var lib : source.getLibraries()) {
        final var libName = lib.getName();
        if (baseLibsToEnable.contains(libName) || !builtinLibraries.contains(libName)) {
          baseLibsToEnable.remove(libName);
        }
      }
      // remove the promoted base libraries from the loaded library
      for (final var newLib : libs) {
        LibraryManager.removeBaseLibraries(newLib, baseLibsToEnable);
      }
      for (final var lib : source.getLibraries()) {
        LibraryTools.buildLibraryList(lib, libNames);
      }
      LibraryTools.buildToolList(source, toolList);
      for (final var lib : libs) {
        if (libNames.containsKey(lib.getName().toUpperCase())) {
          OptionPane.showMessageDialog(
              null,
              "\"" + lib.getName() + "\": " + S.get("LibraryAlreadyLoaded"),
              S.get("LibLoadErrors") + " " + lib.getName() + " !",
              OptionPane.WARNING_MESSAGE);
        } else {
          LibraryTools.removePresentLibraries(lib, libNames, false);
          if (LibraryTools.isLibraryConform(lib, new HashSet<>(), new HashSet<>(), errors)) {
            final var addedToolList = new HashSet<String>();
            LibraryTools.buildToolList(lib, addedToolList);
            for (final var tool : addedToolList)
              if (toolList.contains(tool))
                errors.put(tool, S.get("LibraryMultipleToolError"));
            if (errors.keySet().isEmpty()) {
              LibraryTools.buildLibraryList(lib, libNames);
              toolList.addAll(addedToolList);
              mergedLibs.add(lib);
            } else {
              LibraryTools.showErrors(lib.getName(), errors);
              baseLibsToEnable.clear();
            }
          } else
            LibraryTools.showErrors(lib.getName(), errors);
        }
      }
    }

    @Override
    public void doIt(Project proj) {
      for (final var lib : baseLibsToEnable) {
        final var logisimFile = proj.getLogisimFile();
        logisimFile.addLibrary(logisimFile.getLoader().getBuiltin().getLibrary(lib));
      }
      for (final var lib : mergedLibs) {
        if (lib instanceof LoadedLibrary lib1) {
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
      LibraryTools.buildToolList(proj.getLogisimFile(), availableTools);
      if (lib instanceof LogisimFile thisLib) {
        for (final var circ : thisLib.getCircuits()) {
          for (final var tool : circ.getNonWires()) {
            if (availableTools.containsKey(tool.getFactory().getName().toUpperCase())) {
              final var current = availableTools.get(tool.getFactory().getName().toUpperCase());
              if (current != null) {
                tool.setFactory(current.getFactory());
              } else if ("Text".equals(tool.getFactory().getName())) {
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
      for (final var lib : baseLibsToEnable) proj.getLogisimFile().removeLibrary(lib);
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
      return other instanceof MoveCircuit circ && circ.tool == this.tool;
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

  private LogisimFileActions() {}

  public static Action addVhdl(VhdlContent vhdl) {
    return new AddVhdl(vhdl);
  }

  public static Action addCircuit(Circuit circuit) {
    return new AddCircuit(circuit);
  }

  public static Action mergeFile(LogisimFile mergelib, LogisimFile source) {
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
}
