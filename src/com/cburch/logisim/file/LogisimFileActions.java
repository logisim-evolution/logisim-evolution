/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JOptionPane;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.LibraryTools;
import com.cburch.logisim.tools.Tool;

public class LogisimFileActions {
	private static class AddCircuit extends Action {
		private Circuit circuit;

		AddCircuit(Circuit circuit) {
			this.circuit = circuit;
		}

		@Override
		public void doIt(Project proj) {
			proj.getLogisimFile().addCircuit(circuit);
		}

		@Override
		public String getName() {
			return Strings.get("addCircuitAction");
		}

		@Override
		public void undo(Project proj) {
			proj.getLogisimFile().removeCircuit(circuit);
		}
	}
	
	private static class MergeFile extends Action {
		private ArrayList<Library> MergedLibraries = new ArrayList<Library>();
		private ArrayList<Circuit> MergedCircuits = new ArrayList<Circuit>();
		
		MergeFile(LogisimFile mergelib,
				  LogisimFile source) {
			HashSet<String> LibNames = new HashSet<String>();
			HashSet<String> ToolList = new HashSet<String>();
			HashSet<Circuit> NotImportedList = new HashSet<Circuit>();
			HashMap<String,String> Error = new HashMap<String,String>();
			for (Library lib : source.getLibraries()) {
				LibraryTools.BuildLibraryList(lib,LibNames);
			}
			LibraryTools.BuildToolList(source,ToolList);
			LibraryTools.RemovePresentLibraries(mergelib,LibNames,false);
			if (LibraryTools.LibraryIsConform(mergelib,new HashSet<String> (),new HashSet<String>(),Error)) {
				/* Okay the library is now ready for merge */
				for (Library lib : mergelib.getLibraries()) {
					MergedLibraries.add(lib);
				}
				/* Okay merged the missing libraries, now add the circuits */
				for (Circuit circ : mergelib.getCircuits()) {
					if (ToolList.contains(circ.getName().toUpperCase())) {
						Error.put(circ.getName(), Strings.get("CircNotImportedWarning"));
						NotImportedList.add(circ);
					} else {
						MergedCircuits.add(circ);
					}
				}
				if (!Error.isEmpty())
					LibraryTools.ShowWarnings(mergelib.getName(),Error);
				if (!NotImportedList.isEmpty()) {
					HashMap<String,Circuit> Replacements = new HashMap<String,Circuit>();
					/* Some circuits have not been imported, we have to update the circuits */
					/* first stage, make a map of circuits in the current set */
					for (Circuit nicirc : NotImportedList) {
						for (Circuit curcirc : source.getCircuits()) {
							if (curcirc.getName().toUpperCase().equals(nicirc.getName().toUpperCase())) {
								Replacements.put(curcirc.getName().toUpperCase(), curcirc);
							}
						}
					}
					/* Second stage, iterate over all subcircuits and replace them when required */
					for (Circuit importedCirc : MergedCircuits) {
						for (Component comp : importedCirc.getNonWires()) {
							if (comp instanceof SubcircuitFactory) {
								SubcircuitFactory fact = (SubcircuitFactory) comp;
								if (Replacements.containsKey(fact.getName().toUpperCase())) {
									fact.setSubcircuit(Replacements.get(fact.getName().toUpperCase()));
								}
							}
						}
					}
				}
			} else LibraryTools.ShowErrors(mergelib.getName(),Error);
		}

		@Override
		public void doIt(Project proj) {
			for (Library lib : MergedLibraries)
				proj.getLogisimFile().addLibrary(lib);
			for (Circuit circ : MergedCircuits)
				proj.getLogisimFile().addCircuit(circ);
		}

		@Override
		public String getName() {
			return Strings.get("mergeFileAction");
		}

		@Override
		public boolean isModification() {
			return (MergedLibraries.size() > 0) ||
				   (MergedCircuits.size() > 0);
		}
		
		@Override
		public void undo(Project proj) {
			for (Library lib : MergedLibraries)
				proj.getLogisimFile().removeLibrary(lib);
			for (Circuit circ : MergedCircuits)
				proj.getLogisimFile().removeCircuit(circ);
		}
	}

	private static class LoadLibraries extends Action {
		private ArrayList<Library> MergedLibs = new ArrayList<Library>();

		LoadLibraries(Library[] libs, LogisimFile source) {
			HashSet<String> LibNames = new HashSet<String>();
			HashSet<String> ToolList = new HashSet<String>();
			HashMap<String,String> Error = new HashMap<String,String>();
			for (Library lib : source.getLibraries()) {
				LibraryTools.BuildLibraryList(lib,LibNames);
			}
			LibraryTools.BuildToolList(source,ToolList);
			for (int i = 0; i < libs.length; i++) {
				if (LibNames.contains(libs[i].getName().toUpperCase())) {
                	JOptionPane.showMessageDialog(null, "\""+libs[i].getName()+"\": "+Strings.get("LibraryAlreadyLoaded"),
                			Strings.get("LibLoadErrors")+" "+libs[i].getName()+" !", JOptionPane.WARNING_MESSAGE);
				} else {
					LibraryTools.RemovePresentLibraries(libs[i],LibNames,false);
					if (LibraryTools.LibraryIsConform(libs[i],new HashSet<String> (),new HashSet<String>(),Error)) {
						HashSet<String> AddedToolList = new HashSet<String>();
						LibraryTools.BuildToolList(libs[i],AddedToolList);
						for (String tool : AddedToolList)
							if (ToolList.contains(tool))
								Error.put(tool, Strings.get("LibraryMultipleToolError"));
						if (Error.keySet().isEmpty()) {
							LibraryTools.BuildLibraryList(libs[i],LibNames);
							ToolList.addAll(AddedToolList);
							MergedLibs.add(libs[i]);
						} else
							LibraryTools.ShowErrors(libs[i].getName(),Error);
					} else
						LibraryTools.ShowErrors(libs[i].getName(),Error);
				}
			}
		}

		@Override
		public void doIt(Project proj) {
			for (Library lib : MergedLibs)
				proj.getLogisimFile().addLibrary(lib);
			
		}
		
		@Override
		public boolean isModification() {
			return MergedLibs.size() > 0;
		}
		
		@Override
		public String getName() {
			if (MergedLibs.size() <= 1) {
				return Strings.get("loadLibraryAction");
			} else {
				return Strings.get("loadLibrariesAction");
			}
		}

		@Override
		public void undo(Project proj) {
			for (Library lib : MergedLibs)
				proj.getLogisimFile().removeLibrary(lib);
		}
	}

	private static class MoveCircuit extends Action {
		private AddTool tool;
		private int fromIndex;
		private int toIndex;

		MoveCircuit(AddTool tool, int toIndex) {
			this.tool = tool;
			this.toIndex = toIndex;
		}

		@Override
		public Action append(Action other) {
			MoveCircuit ret = new MoveCircuit(tool,
					((MoveCircuit) other).toIndex);
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
			return Strings.get("moveCircuitAction");
		}

		@Override
		public boolean shouldAppendTo(Action other) {
			return other instanceof MoveCircuit
					&& ((MoveCircuit) other).tool == this.tool;
		}

		@Override
		public void undo(Project proj) {
			proj.getLogisimFile().moveCircuit(tool, fromIndex);
		}
	}

	private static class RemoveCircuit extends Action {
		private Circuit circuit;
		private int index;

		RemoveCircuit(Circuit circuit) {
			this.circuit = circuit;
		}

		@Override
		public void doIt(Project proj) {
			index = proj.getLogisimFile().getCircuits().indexOf(circuit);
			proj.getLogisimFile().removeCircuit(circuit);
		}

		@Override
		public String getName() {
			return Strings.get("removeCircuitAction");
		}

		@Override
		public void undo(Project proj) {
			proj.getLogisimFile().addCircuit(circuit, index);
		}
	}

	private static class RevertAttributeValue {
		private AttributeSet attrs;
		private Attribute<Object> attr;
		private Object value;

		RevertAttributeValue(AttributeSet attrs, Attribute<Object> attr,
				Object value) {
			this.attrs = attrs;
			this.attr = attr;
			this.value = value;
		}
	}

	private static class RevertDefaults extends Action {
		private Options oldOpts;
		private ArrayList<Library> libraries = null;
		private ArrayList<RevertAttributeValue> attrValues;

		RevertDefaults() {
			libraries = null;
			attrValues = new ArrayList<RevertAttributeValue>();
		}

		private void copyToolAttributes(Library srcLib, Library dstLib) {
			for (Tool srcTool : srcLib.getTools()) {
				AttributeSet srcAttrs = srcTool.getAttributeSet();
				Tool dstTool = dstLib.getTool(srcTool.getName());
				if (srcAttrs != null && dstTool != null) {
					AttributeSet dstAttrs = dstTool.getAttributeSet();
					for (Attribute<?> attrBase : srcAttrs.getAttributes()) {
						@SuppressWarnings("unchecked")
						Attribute<Object> attr = (Attribute<Object>) attrBase;
						Object srcValue = srcAttrs.getValue(attr);
						Object dstValue = dstAttrs.getValue(attr);
						if (!dstValue.equals(srcValue)) {
							dstAttrs.setValue(attr, srcValue);
							attrValues.add(new RevertAttributeValue(dstAttrs,
									attr, dstValue));
						}
					}
				}
			}
		}

		@Override
		public void doIt(Project proj) {
			LogisimFile src = ProjectActions.createNewFile(proj);
			LogisimFile dst = proj.getLogisimFile();

			copyToolAttributes(src, dst);
			for (Library srcLib : src.getLibraries()) {
				Library dstLib = dst.getLibrary(srcLib.getName());
				if (dstLib == null) {
					String desc = src.getLoader().getDescriptor(srcLib);
					dstLib = dst.getLoader().loadLibrary(desc);
					proj.getLogisimFile().addLibrary(dstLib);
					if (libraries == null)
						libraries = new ArrayList<Library>();
					libraries.add(dstLib);
				}
				copyToolAttributes(srcLib, dstLib);
			}

			Options newOpts = proj.getOptions();
			oldOpts = new Options();
			oldOpts.copyFrom(newOpts, dst);
			newOpts.copyFrom(src.getOptions(), dst);
		}

		@Override
		public String getName() {
			return Strings.get("revertDefaultsAction");
		}

		@Override
		public void undo(Project proj) {
			proj.getOptions().copyFrom(oldOpts, proj.getLogisimFile());

			for (RevertAttributeValue attrValue : attrValues) {
				attrValue.attrs.setValue(attrValue.attr, attrValue.value);
			}

			if (libraries != null) {
				for (Library lib : libraries) {
					proj.getLogisimFile().removeLibrary(lib);
				}
			}
		}
	}

	private static class SetMainCircuit extends Action {
		private Circuit oldval;
		private Circuit newval;

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
			return Strings.get("setMainCircuitAction");
		}

		@Override
		public void undo(Project proj) {
			proj.getLogisimFile().setMainCircuit(oldval);
		}
	}

	private static class UnloadLibraries extends Action {
		private Library[] libs;

		UnloadLibraries(Library[] libs) {
			this.libs = libs;
		}

		@Override
		public void doIt(Project proj) {
			for (int i = libs.length - 1; i >= 0; i--) {
				proj.getLogisimFile().removeLibrary(libs[i]);
			}
		}

		@Override
		public String getName() {
			if (libs.length == 1) {
				return Strings.get("unloadLibraryAction");
			} else {
				return Strings.get("unloadLibrariesAction");
			}
		}

		@Override
		public void undo(Project proj) {
			for (int i = 0; i < libs.length; i++) {
				proj.getLogisimFile().addLibrary(libs[i]);
			}
		}
	}

	public static Action addCircuit(Circuit circuit) {
		return new AddCircuit(circuit);
	}
	
	public static Action MergeFile(LogisimFile mergelib, LogisimFile source) {
		return new MergeFile(mergelib,source);
	}

	public static Action loadLibraries(Library[] libs, LogisimFile source) {
		return new LoadLibraries(libs,source);
	}

	public static Action loadLibrary(Library lib, LogisimFile source) {
		return new LoadLibraries(new Library[] { lib }, source);
	}

	public static Action moveCircuit(AddTool tool, int toIndex) {
		return new MoveCircuit(tool, toIndex);
	}

	public static Action removeCircuit(Circuit circuit) {
		return new RemoveCircuit(circuit);
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
		return new UnloadLibraries(new Library[] { lib });
	}

	private LogisimFileActions() {
	}
}
