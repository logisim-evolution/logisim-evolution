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
import java.util.Iterator;

import javax.swing.JOptionPane;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
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

	private static class LoadLibraries extends Action {
		private Library[] libs;

		LoadLibraries(Library[] libs) {
			this.libs = libs;
		}

		@Override
		public void doIt(Project proj) {
			HashSet<String> LibNames = new HashSet<String>();
			HashSet<String> ToolList = new HashSet<String>();
			HashMap<String,String> Error = new HashMap<String,String>();
			for (Library lib : proj.getLogisimFile().getLibraries()) {
				BuildLibraryList(lib,LibNames);
			}
			BuildToolList(proj.getLogisimFile(),ToolList);
			for (int i = 0; i < libs.length; i++) {
				if (LibNames.contains(libs[i].getName().toUpperCase())) {
                	JOptionPane.showMessageDialog(null, "\""+libs[i].getName()+"\": "+Strings.get("LibraryAlreadyLoaded"));
				} else {
					RemovePresentLibraries(libs[i],LibNames);
					if (LibraryIsConform(libs[i],new HashSet<String> (),new HashSet<String>(),Error)) {
						HashSet<String> AddedToolList = new HashSet<String>();
						BuildToolList(libs[i],AddedToolList);
						for (String tool : AddedToolList)
							if (ToolList.contains(tool))
								Error.put(tool, "LibraryMultipleToolError");
						if (Error.keySet().isEmpty()) {
							BuildLibraryList(libs[i],LibNames);
							ToolList.addAll(AddedToolList);
							proj.getLogisimFile().addLibrary(libs[i]);
						} else
							ShowErrors(libs[i].getName(),Error);
					} else
						ShowErrors(libs[i].getName(),Error);
				}
			}
		}
		
		private static void ShowErrors(String LibName,HashMap<String,String> Error) {
			String ErrorMessage = Strings.get("LibLoadErrors")+" \""+LibName+"\":\n\n";
			int item = 0;
			for (String myerror : Error.keySet()) {
				item++;
				ErrorMessage = ErrorMessage.concat( item+") "+Strings.get(Error.get(myerror))+" \""+myerror+"\".\n");
			}
			JOptionPane.showMessageDialog(null, ErrorMessage.toString());
		}
		
		private static void BuildToolList(Library lib, HashSet<String> Tools) {
			Iterator<? extends Tool> tooliter = lib.getTools().iterator();
			while (tooliter.hasNext()) {
				Tool tool = tooliter.next();
				Tools.add(tool.getName().toUpperCase());
			}
			for (Library sublib : lib.getLibraries())
				BuildToolList(sublib,Tools);
		}
		
		private static boolean LibraryIsConform(Library lib, HashSet<String> Names, HashSet<String> Tools, HashMap<String,String> Error) {
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
		
		private static void BuildLibraryList(Library lib, HashSet<String> Names) {
			Names.add(lib.getName().toUpperCase());
			for (Library sublib : lib.getLibraries()) {
				BuildLibraryList(sublib,Names);
			}
		}
		
		private static void RemovePresentLibraries(Library lib, HashSet<String> KnownLibs) {
			HashSet<String> ToBeRemoved = new HashSet<String>();
			for (Library sublib : lib.getLibraries()) {
				RemovePresentLibraries(sublib,KnownLibs);
				if (KnownLibs.contains(sublib.getName().toUpperCase())) {
					ToBeRemoved.add(sublib.getName());
				}
			}
			for (String remove : ToBeRemoved)
				lib.removeLibrary(remove);
		}

		@Override
		public String getName() {
			if (libs.length == 1) {
				return Strings.get("loadLibraryAction");
			} else {
				return Strings.get("loadLibrariesAction");
			}
		}

		@Override
		public void undo(Project proj) {
			for (int i = libs.length - 1; i >= 0; i--) {
				proj.getLogisimFile().removeLibrary(libs[i]);
			}
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

	public static Action loadLibraries(Library[] libs) {
		return new LoadLibraries(libs);
	}

	public static Action loadLibrary(Library lib) {
		return new LoadLibraries(new Library[] { lib });
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
