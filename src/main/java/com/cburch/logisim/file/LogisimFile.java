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
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UniquelyNamedThread;
import com.cburch.logisim.vhdl.base.VhdlContent;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;

public class LogisimFile extends Library implements LibraryEventSource, CircuitListener {

  @Override
  public void circuitChanged(CircuitEvent event) {
    final var act = event.getAction();
    if (act == CircuitEvent.ACTION_CHECK_NAME) {
      final var oldname = (String) event.getData();
      final var newname = event.getCircuit().getName();
      if (NameIsInUse(newname, event.getCircuit())) {
        OptionPane.showMessageDialog(
            null,
            "\"" + newname + "\": " + S.get("circuitNameExists"),
            "",
            OptionPane.ERROR_MESSAGE);
        event.getCircuit().getStaticAttributes().setValue(CircuitAttributes.NAME_ATTR, oldname);
      }
    }
  }

  // Name check Methods
  private boolean NameIsInUse(String Name, Circuit changed) {
    if (Name.isEmpty()) return false;
    for (final var mylib : getLibraries()) {
      if (NameIsInLibraries(mylib, Name)) return true;
    }
    for (final var mytool : this.getCircuits()) {
      if (Name.equalsIgnoreCase(mytool.getName()) && !mytool.equals(changed))
        return true;
    }
    return false;
  }

  private boolean NameIsInLibraries(Library lib, String Name) {
    if (Name.isEmpty()) return false;
    for (final var mylib : lib.getLibraries()) {
      if (NameIsInLibraries(mylib, Name)) return true;
    }
    for (final var mytool : lib.getTools()) {
      if (Name.equalsIgnoreCase(mytool.getName())) return true;
    }
    return false;
  }

  private static class WritingThread extends UniquelyNamedThread {
    final OutputStream out;
    final LogisimFile file;

    WritingThread(OutputStream out, LogisimFile file) {
      super("WritingThread");
      this.out = out;
      this.file = file;
    }

    @Override
    public void run() {
      file.write(out, file.loader);
      try {
        out.close();
      } catch (IOException e) {
        file.loader.showError(S.get("fileDuplicateError", e.toString()));
      }
    }
  }

  //
  // creation methods
  //
  public static LogisimFile createNew(Loader loader, Project proj) {
    final var ret = new LogisimFile(loader);
    ret.main = new Circuit("main", ret, proj);
    // The name will be changed in LogisimPreferences
    ret.tools.add(new AddTool(ret.main.getSubcircuitFactory()));
    return ret;
  }

  private static String getFirstLine(BufferedInputStream in) throws IOException {
    final var first = new byte[512];
    in.mark(first.length - 1);
    in.read(first);
    in.reset();

    int lineBreak = first.length;
    for (var i = 0; i < lineBreak; i++) {
      if (first[i] == '\n') {
        lineBreak = i;
      }
    }
    return new String(first, 0, lineBreak, StandardCharsets.UTF_8);
  }

  public static LogisimFile load(File file, Loader loader) throws IOException {
    InputStream in = new FileInputStream(file);
    Throwable firstExcept = null;
    try {
      return loadSub(in, loader, file);
    } catch (Throwable t) {
      firstExcept = t;
    } finally {
      in.close();
    }

    if (firstExcept != null) {
      // We'll now try to do it using a reader. This is to work around
      // Logisim versions prior to 2.5.1, when files were not saved using
      // UTF-8 as the encoding (though the XML file reported otherwise).
      try {
        in = new ReaderInputStream(new FileReader(file), "UTF8");
        return loadSub(in, loader, file);
      } catch (Exception t) {
        firstExcept.printStackTrace();
        loader.showError(S.get("xmlFormatError", firstExcept.toString()));
      } finally {
        try {
          in.close();
        } catch (Exception ignored) {
        }
      }
    }

    return null;
  }

  public static LogisimFile load(InputStream in, Loader loader) throws IOException {
    try {
      return loadSub(in, loader);
    } catch (SAXException e) {
      e.printStackTrace();
      loader.showError(S.get("xmlFormatError", e.toString()));
      return null;
    }
  }

  public static LogisimFile loadSub(InputStream in, Loader loader)
      throws IOException, SAXException {
    return (loadSub(in, loader, null));
  }

  public static LogisimFile loadSub(InputStream in, Loader loader, File file)
      throws IOException, SAXException {
    // fetch first line and then reset
    final var inBuffered = new BufferedInputStream(in);
    final var firstLine = getFirstLine(inBuffered);

    if (firstLine == null) {
      throw new IOException("File is empty");
    } else if (firstLine.equals("Logisim v1.0")) {
      // if this is a 1.0 file, then set up a pipe to translate to
      // 2.0 and then interpret as a 2.0 file
      throw new IOException("Version 1.0 files no longer supported");
    }

    final var xmlReader = new XmlReader(loader, file);
    /* Can set the project pointer to zero as it is fixed later */
    final var ret = xmlReader.readLibrary(inBuffered, null);
    ret.loader = loader;
    return ret;
  }

  private final EventSourceWeakSupport<LibraryListener> listeners = new EventSourceWeakSupport<>();
  private Loader loader;
  private final LinkedList<String> messages = new LinkedList<>();
  private final Options options = new Options();

  private final LinkedList<AddTool> tools = new LinkedList<>();

  private final LinkedList<Library> libraries = new LinkedList<>();

  private Circuit main = null;

  private String name;

  private boolean dirty = false;

  LogisimFile(Loader loader) {
    this.loader = loader;

    // Creates the default project name, adding an underscore if needed
    name = S.get("defaultProjectName");
    if (Projects.windowNamed(name)) {
      for (var i = 2; true; i++) {
        if (!Projects.windowNamed(name + "_" + i)) {
          name += "_" + i;
          break;
        }
      }
    }
  }

  public void addCircuit(Circuit circuit) {
    addCircuit(circuit, tools.size());
  }

  public void addCircuit(Circuit circuit, int index) {
    circuit.addCircuitListener(this);
    final var tool = new AddTool(circuit.getSubcircuitFactory());
    tools.add(index, tool);
    if (tools.size() == 1) setMainCircuit(circuit);
    fireEvent(LibraryEvent.ADD_TOOL, tool);
  }

  public void addVhdlContent(VhdlContent content) {
    addVhdlContent(content, tools.size());
  }

  public void addVhdlContent(VhdlContent content, int index) {
    final var tool = new AddTool(new VhdlEntity(content));
    tools.add(index, tool);
    fireEvent(LibraryEvent.ADD_TOOL, tool);
  }

  public void addLibrary(Library lib) {
    if (!lib.getName().equals(BaseLibrary._ID)) {
      for (final var tool : lib.getTools()) {
        final var tool1 = (AddTool) tool;
        final var atrs = tool1.getAttributeSet();
        for (Attribute<?> attr : atrs.getAttributes()) {
          if (attr == CircuitAttributes.NAME_ATTR) atrs.setReadOnly(attr, true);
        }
      }
    }
    libraries.add(lib);
    fireEvent(LibraryEvent.ADD_LIBRARY, lib);
  }

  //
  // listener methods
  //
  @Override
  public void addLibraryListener(LibraryListener what) {
    listeners.add(what);
  }

  //
  // modification actions
  //
  public void addMessage(String msg) {
    messages.addLast(msg);
  }

  @SuppressWarnings("resource")
  public LogisimFile cloneLogisimFile(Loader newloader) {
    final var reader = new PipedInputStream();
    final var writer = new PipedOutputStream();
    try {
      reader.connect(writer);
    } catch (IOException e) {
      newloader.showError(S.get("fileDuplicateError", e.toString()));
      return null;
    }
    new WritingThread(writer, this).start();
    try {
      return LogisimFile.load(reader, newloader);
    } catch (IOException e) {
      newloader.showError(S.get("fileDuplicateError", e.toString()));
      try {
        reader.close();
      } catch (IOException ignored) {
      }
      return null;
    }
  }

  public boolean contains(Circuit circ) {
    for (final var tool : tools) {
      if (tool.getFactory() instanceof SubcircuitFactory) {
        final var factory = (SubcircuitFactory) tool.getFactory();
        if (factory.getSubcircuit() == circ) return true;
      }
    }
    return false;
  }

  public boolean contains(VhdlContent content) {
    for (final var tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        final var factory = (VhdlEntity) tool.getFactory();
        if (factory.getContent() == content) return true;
      }
    }
    return false;
  }

  public boolean containsFactory(String name) {
    for (final var tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        final var factory = (VhdlEntity) tool.getFactory();
        if (factory.getContent().getName().equals(name)) return true;
      } else if (tool.getFactory() instanceof SubcircuitFactory) {
        final var factory = (SubcircuitFactory) tool.getFactory();
        if (factory.getSubcircuit().getName().equals(name)) return true;
      }
    }
    return false;
  }

  private Tool findTool(Library lib, Tool query) {
    for (final var tool : lib.getTools()) {
      if (tool.equals(query)) return tool;
    }
    return null;
  }

  Tool findTool(Tool query) {
    for (final var lib : getLibraries()) {
      final var ret = findTool(lib, query);
      if (ret != null) return ret;
    }
    return null;
  }

  private void fireEvent(int action, Object data) {
    final var e = new LibraryEvent(this, action, data);
    for (final var l : listeners) {
      l.libraryChanged(e);
    }
  }

  public AddTool getAddTool(Circuit circ) {
    for (final var tool : tools) {
      if (tool.getFactory() instanceof SubcircuitFactory) {
        final var factory = (SubcircuitFactory) tool.getFactory();
        if (factory.getSubcircuit() == circ) {
          return tool;
        }
      }
    }
    return null;
  }

  public AddTool getAddTool(VhdlContent content) {
    for (final var tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        final var factory = (VhdlEntity) tool.getFactory();
        if (factory.getContent() == content) {
          return tool;
        }
      }
    }
    return null;
  }

  public Circuit getCircuit(String name) {
    if (name == null) return null;
    for (final var tool : tools) {
      if (tool.getFactory() instanceof SubcircuitFactory) {
        final var factory = (SubcircuitFactory) tool.getFactory();
        if (name.equals(factory.getName())) return factory.getSubcircuit();
      }
    }
    return null;
  }

  public VhdlContent getVhdlContent(String name) {
    if (name == null) return null;
    for (final var tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        final var factory = (VhdlEntity) tool.getFactory();
        if (name.equals(factory.getName())) return factory.getContent();
      }
    }
    return null;
  }

  public int getCircuitCount() {
    return getCircuits().size();
  }

  public List<Circuit> getCircuits() {
    final var ret = new ArrayList<Circuit>(tools.size());
    for (final var tool : tools) {
      if (tool.getFactory() instanceof SubcircuitFactory) {
        ret.add(((SubcircuitFactory) tool.getFactory()).getSubcircuit());
      }
    }
    return ret;
  }

  public int indexOfCircuit(Circuit circ) {
    for (var i = 0; i < tools.size(); i++) {
      final var tool = tools.get(i);
      if (tool.getFactory() instanceof SubcircuitFactory) {
        final var factory = (SubcircuitFactory) tool.getFactory();
        if (factory.getSubcircuit() == circ) {
          return i;
        }
      }
    }
    return -1;
  }

  public List<VhdlContent> getVhdlContents() {
    final var ret = new ArrayList<VhdlContent>(tools.size());
    for (final var tool : tools) {
      if (tool.getFactory() instanceof VhdlEntity) {
        final var factory = (VhdlEntity) tool.getFactory();
        ret.add(factory.getContent());
      }
    }
    return ret;
  }

  public int indexOfVhdl(VhdlContent vhdl) {
    for (var i = 0; i < tools.size(); i++) {
      final var tool = tools.get(i);
      if (tool.getFactory() instanceof VhdlEntity) {
        final var factory = (VhdlEntity) tool.getFactory();
        if (factory.getContent() == vhdl) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override
  public List<Library> getLibraries() {
    return libraries;
  }

  public Loader getLoader() {
    return loader;
  }

  public Circuit getMainCircuit() {
    return main;
  }

  public String getMessage() {
    return (messages.isEmpty()) ? null : messages.removeFirst();
  }

  //
  // access methods
  //
  @Override
  public String getName() {
    return name;
  }

  public Options getOptions() {
    return options;
  }

  @Override
  public List<AddTool> getTools() {
    return tools;
  }

  public String getUnloadLibraryMessage(Library lib) {
    final var factories = new HashSet<ComponentFactory>();
    for (final var tool : lib.getTools()) {
      if (tool instanceof AddTool) {
        factories.add(((AddTool) tool).getFactory());
      }
    }

    for (final var circuit : getCircuits()) {
      for (final var comp : circuit.getNonWires()) {
        if (factories.contains(comp.getFactory())) {
          return S.get("unloadUsedError", circuit.getName());
        }
      }
    }

    final var tb = options.getToolbarData();
    final var mm = options.getMouseMappings();
    for (final var t : lib.getTools()) {
      if (tb.usesToolFromSource(t)) {
        return S.get("unloadToolbarError");
      }
      if (mm.usesToolFromSource(t)) {
        return S.get("unloadMappingError");
      }
    }

    return null;
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  public void moveCircuit(AddTool tool, int index) {
    int oldIndex = tools.indexOf(tool);
    if (oldIndex < 0) {
      tools.add(index, tool);
      fireEvent(LibraryEvent.ADD_TOOL, tool);
    } else {
      AddTool value = tools.remove(oldIndex);
      tools.add(index, value);
      fireEvent(LibraryEvent.MOVE_TOOL, tool);
    }
  }

  public void removeCircuit(Circuit circuit) {
    if (getCircuitCount() <= 1) {
      throw new RuntimeException("Cannot remove last circuit");
    }

    int index = indexOfCircuit(circuit);
    if (index >= 0) {
      final Tool circuitTool = tools.remove(index);

      if (main == circuit) {
        setMainCircuit(((SubcircuitFactory) tools.get(0).getFactory()).getSubcircuit());
      }
      fireEvent(LibraryEvent.REMOVE_TOOL, circuitTool);
    }
  }

  public void removeVhdl(VhdlContent vhdl) {
    final var index = indexOfVhdl(vhdl);
    if (index >= 0) {
      final Tool vhdlTool = tools.remove(index);
      fireEvent(LibraryEvent.REMOVE_TOOL, vhdlTool);
    }
  }

  @Override
  public boolean removeLibrary(String name) {
    int index = -1;
    for (final var lib : libraries)
      if (lib.getName().equals(name))
        index = libraries.indexOf(lib);
    if (index < 0) return false;
    libraries.remove(index);
    return true;
  }

  public void removeLibrary(Library lib) {
    libraries.remove(lib);
    fireEvent(LibraryEvent.REMOVE_LIBRARY, lib);
  }

  @Override
  public void removeLibraryListener(LibraryListener what) {
    listeners.remove(what);
  }

  public void setDirty(boolean value) {
    if (dirty != value) {
      dirty = value;
      fireEvent(LibraryEvent.DIRTY_STATE, value ? Boolean.TRUE : Boolean.FALSE);
    }
  }

  public void setMainCircuit(Circuit circuit) {
    if (circuit == null) return;
    this.main = circuit;
    fireEvent(LibraryEvent.SET_MAIN, circuit);
  }

  public void setName(String name) {
    this.name = name;
    fireEvent(LibraryEvent.SET_NAME, name);
  }

  //
  // other methods
  //
  void write(OutputStream out, LibraryLoader loader) {
    write(out, loader, null);
  }

  void write(OutputStream out, LibraryLoader loader, File dest) {
    try {
      XmlWriter.write(this, out, loader, dest);
    } catch (TransformerConfigurationException e) {
      loader.showError("internal error configuring transformer");
    } catch (ParserConfigurationException e) {
      loader.showError("internal error configuring parser");
    } catch (TransformerException e) {
      final var msg = e.getMessage();
      var err = S.get("xmlConversionError");
      if (msg == null) err += ": " + msg;
      loader.showError(err);
    }
  }
}
