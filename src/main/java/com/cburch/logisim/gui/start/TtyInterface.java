/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.start;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.FileStatistics;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.io.Keyboard;
import com.cburch.logisim.std.io.Tty;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.DualportRam;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.UniquelyNamedThread;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TtyInterface {

  public static final int FORMAT_TABLE = 1;
  public static final int FORMAT_SPEED = 2;
  public static final int FORMAT_TTY = 4;
  public static final int FORMAT_HALT = 8;
  public static final int FORMAT_STATISTICS = 16;
  public static final int FORMAT_TABLE_TABBED = 32;
  public static final int FORMAT_TABLE_CSV = 64;
  public static final int FORMAT_TABLE_BIN = 128;
  public static final int FORMAT_TABLE_HEX = 256;
  static final Logger logger = LoggerFactory.getLogger(TtyInterface.class);
  private static boolean lastIsNewline = true;

  private static int countDigits(int num) {
    int digits = 1;
    int lessThan = 10;
    while (num >= lessThan) {
      digits++;
      lessThan *= 10;
    }
    return digits;
  }

  private static void displaySpeed(long tickCount, long elapse) {
    var hertz = (double) tickCount / elapse * 1000.0;
    double precision;
    if (hertz >= 100) precision = 1.0;
    else if (hertz >= 10) precision = 0.1;
    else if (hertz >= 1) precision = 0.01;
    else if (hertz >= 0.01) precision = 0.0001;
    else precision = 0.0000001;
    hertz = (int) (hertz / precision) * precision;
    var hertzStr = hertz == (int) hertz ? "" + (int) hertz : "" + hertz;
    System.out.printf(S.get("ttySpeedMsg") + "\n", hertzStr, tickCount, elapse);
  }

  private static void displayStatistics(LogisimFile file, Circuit circuit) {
    final var stats = FileStatistics.compute(file, circuit);
    final var total = stats.getTotalWithSubcircuits();
    var maxName = 0;
    for (final var count : stats.getCounts()) {
      final var nameLength = count.getFactory().getDisplayName().length();
      if (nameLength > maxName) maxName = nameLength;
    }
    final var fmt =
        "%"
            + countDigits(total.getUniqueCount())
            + "d\t"
            + "%"
            + countDigits(total.getRecursiveCount())
            + "d\t";
    final var fmtNormal = fmt + "%-" + maxName + "s\t%s\n";
    for (final var count : stats.getCounts()) {
      final var lib = count.getLibrary();
      final var libName = lib == null ? "-" : lib.getDisplayName();
      System.out.printf(
          fmtNormal,
          count.getUniqueCount(),
          count.getRecursiveCount(),
          count.getFactory().getDisplayName(),
          libName);
    }
    final var totalWithout = stats.getTotalWithoutSubcircuits();
    System.out.printf(
        fmt + "%s\n",
        totalWithout.getUniqueCount(),
        totalWithout.getRecursiveCount(),
        S.get("statsTotalWithout"));
    System.out.printf(
        fmt + "%s\n",
        total.getUniqueCount(),
        total.getRecursiveCount(),
        S.get("statsTotalWith"));
  }

  private static void displayTableRow(ArrayList<Value> prevOutputs, ArrayList<Value> curOutputs) {
    var shouldPrint = false;
    if (prevOutputs == null) {
      shouldPrint = true;
    } else {
      for (var i = 0; i < curOutputs.size(); i++) {
        final var a = prevOutputs.get(i);
        final var b = curOutputs.get(i);
        if (!a.equals(b)) {
          shouldPrint = true;
          break;
        }
      }
    }
    if (shouldPrint) {
      for (var i = 0; i < curOutputs.size(); i++) {
        if (i != 0) System.out.print("\t");
        System.out.print(curOutputs.get(i));
      }
      System.out.println();
    }
  }

  private static boolean displayTableRow(boolean showHeader, ArrayList<Value> prevOutputs, ArrayList<Value> curOutputs,
                                         ArrayList<String> headers, ArrayList<String> formats, int format) {
    var shouldPrint = false;
    if (prevOutputs == null) {
      shouldPrint = true;
    } else {
      for (var i = 0; i < curOutputs.size(); i++) {
        final var a = prevOutputs.get(i);
        final var b = curOutputs.get(i);
        if (!a.equals(b)) {
          shouldPrint = true;
          break;
        }
      }
    }
    if (shouldPrint) {
      var sep = "";
      if ((format & FORMAT_TABLE_TABBED) != 0) sep = "\t";
      else if ((format & FORMAT_TABLE_CSV) != 0) sep = ",";
      else // if ((format & FORMAT_TABLE_PRETTY) != 0)
        sep = " ";
      if (showHeader) {
        for (var i = 0; i < headers.size(); i++) {
          if ((format & FORMAT_TABLE_TABBED) != 0) formats.add("%s");
          else if ((format & FORMAT_TABLE_CSV) != 0) formats.add("%s");
          else { // if ((format & FORMAT_TABLE_PRETTY) != 0)
            int w = headers.get(i).length();
            w = Math.max(w, valueFormat(curOutputs.get(i), format).length());
            formats.add("%" + w + "s");
          }
        }
        for (var i = 0; i < headers.size(); i++) {
          if (i != 0) System.out.print(sep);
          System.out.printf(formats.get(i), headers.get(i));
        }
        System.out.println();
      }
      for (var i = 0; i < curOutputs.size(); i++) {
        if (i != 0) System.out.print(sep);
        System.out.printf(formats.get(i), valueFormat(curOutputs.get(i), format));
      }
      System.out.println();
    }
    return shouldPrint;
  }

  private static String valueFormat(Value v, int format) {
    if ((format & FORMAT_TABLE_BIN) != 0) {
      // everything in binary
      return v.toString();
    } else if ((format & FORMAT_TABLE_HEX) != 0) {
      // everything thing in hex, no prefixes
      return v.toHexString();
    } else {
      // under 6 bits or less in binary, no spaces
      // otherwise in hex, with prefix
      if (v.getWidth() <= 6) return v.toBinaryString();
      else return "0x" + v.toHexString();
    }
  }

  private static void ensureLineTerminated() {
    if (!lastIsNewline) {
      lastIsNewline = true;
      System.out.print('\n');
    }
  }

  private static boolean loadRam(CircuitState circState, File loadFile) throws IOException {
    if (loadFile == null) return false;

    var found = false;
    for (final var comp : circState.getCircuit().getNonWires()) {
      if (comp.getFactory() instanceof Ram ramFactory) {
        final var ramState = circState.getInstanceState(comp);
        final var m = ramFactory.getContents(ramState);
        HexFile.open(m, loadFile);
        found = true;
      } else if (comp.getFactory() instanceof DualportRam dpramFactory) {
        final var dpramState = circState.getInstanceState(comp);
        final var m = dpramFactory.getContents(dpramState);
        HexFile.open(m, loadFile);
        found = true;
      }
    }

    for (final var sub : circState.getSubstates()) {
      found |= loadRam(sub, loadFile);
    }
    return found;
  }

  private static boolean saveRam(CircuitState circState, File saveFile) throws IOException {
    if (saveFile == null) return false;

    var found = false;
    for (final var comp : circState.getCircuit().getNonWires()) {
      if (comp.getFactory() instanceof Ram ramFactory) {
        final var ramState = circState.getInstanceState(comp);
        final var m = ramFactory.getContents(ramState);
        HexFile.save(saveFile, m, "v3.0 hex words plain");
        found = true;
      } else if (comp.getFactory() instanceof DualportRam dpramFactory) {
        final var dpramState = circState.getInstanceState(comp);
        final var m = dpramFactory.getContents(dpramState);
        HexFile.save(saveFile, m, "v3.0 hex words plain");
        found = true;
      }
    }

    for (final var sub : circState.getSubstates()) {
      found |= saveRam(sub, saveFile);
    }
    return found;
  }

  private static boolean prepareForTty(CircuitState circState, ArrayList<InstanceState> keybStates) {
    var found = false;
    for (final var comp : circState.getCircuit().getNonWires()) {
      final Object factory = comp.getFactory();
      if (factory instanceof Tty ttyFactory) {
        final var ttyState = circState.getInstanceState(comp);
        ttyFactory.sendToStdout(ttyState);
        found = true;
      } else if (factory instanceof Keyboard) {
        keybStates.add(circState.getInstanceState(comp));
        found = true;
      }
    }

    for (CircuitState sub : circState.getSubstates()) {
      found |= prepareForTty(sub, keybStates);
    }
    return found;
  }

  public static void run(Startup args) {
    final var fileToOpen = args.getFilesToOpen().get(0);
    final var loader = new Loader(null);
    LogisimFile file;
    try {
      file = loader.openLogisimFile(fileToOpen, args.getSubstitutions());
    } catch (LoadFailedException e) {
      logger.error("{}", S.get("ttyLoadError", fileToOpen.getName()));
      System.exit(-1);
      return;
    }
    final var proj = new Project(file);
    if (args.isFpgaDownload()) {
      if (!args.fpgaDownload(proj)) System.exit(-1);
    }

    final var circuitToTest = args.getCircuitToTest();
    final var circuit = (circuitToTest == null || circuitToTest.length() == 0)
        ? file.getMainCircuit()
        : file.getCircuit(circuitToTest);

    var format = args.getTtyFormat();
    if ((format & FORMAT_STATISTICS) != 0) {
      format &= ~FORMAT_STATISTICS;
      displayStatistics(file, circuit);
    }
    if (format == 0) { // no simulation remaining to perform, so just exit
      System.exit(0);
    }

    final var pinNames = Analyze.getPinLabels(circuit);
    final var outputPins = new ArrayList<Instance>();
    final var inputPins = new ArrayList<Instance>();
    Instance haltPin = null;
    for (final var entry : pinNames.entrySet()) {
      final var pin = entry.getKey();
      final var pinName = entry.getValue();
      if (Pin.FACTORY.isInputPin(pin)) {
        inputPins.add(pin);
      } else {
        outputPins.add(pin);
        if (pinName.equals("halt")) {
          haltPin = pin;
        }
      }
    }
    if (haltPin == null && (format & FORMAT_TABLE) != 0) {
      doTableAnalysis(proj, circuit, pinNames, format);
      return;
    }

    CircuitState circState = CircuitState.createRootState(proj, circuit);

    // we load the ram before first propagation
    // so the first propagation emits correct values
    if (args.getLoadFile() != null) {
      try {
        final var loaded = loadRam(circState, args.getLoadFile());
        if (!loaded) {
          logger.error("{}", S.get("loadNoRamError"));
          System.exit(-1);
        }
      } catch (IOException e) {
        logger.error("{}: {}", S.get("loadIoError"), e.toString());
        System.exit(-1);
      }
    }

    // we have to do our initial propagation before the simulation starts -
    // it's necessary to populate the circuit with substates.
    circState.getPropagator().propagate();

    final var ttyFormat = args.getTtyFormat();
    final var simCode = runSimulation(circState, outputPins, haltPin, ttyFormat);

    if (args.getSaveFile() != null) {
      try {
        final var saved = saveRam(circState, args.getSaveFile());
        if (!saved) {
          logger.error("{}", S.get("saveNoRamError"));
          System.exit(-1);
        }
      } catch (IOException e) {
        logger.error("{}: {}", S.get("saveIoError"), e.toString());
        System.exit(-1);
      }
    }

    System.exit(simCode);
  }

  private static int doTableAnalysis(Project proj, Circuit circuit, Map<Instance, String> pinLabels, int format) {

    final var inputPins = new ArrayList<Instance>();
    final var inputVars = new ArrayList<Var>();
    final var inputNames = new ArrayList<String>();
    final var outputPins = new ArrayList<Instance>();
    final var outputVars = new ArrayList<Var>();
    final var outputNames = new ArrayList<String>();
    final var formats = new ArrayList<String>();
    for (final var entry : pinLabels.entrySet()) {
      final var pin = entry.getKey();
      final var width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
      final var var = new Var(entry.getValue(), width);
      if (Pin.FACTORY.isInputPin(pin)) {
        inputPins.add(pin);
        for (final var name : var) inputNames.add(name);
        inputVars.add(var);
      } else {
        outputPins.add(pin);
        for (final var name : var) outputNames.add(name);
        outputVars.add(var);
      }
    }

    final var headers = new ArrayList<String>();
    final var pinList = new ArrayList<Instance>();
    /* input pins first */
    for (final var entry : pinLabels.entrySet()) {
      final var pin = entry.getKey();
      final var pinName = entry.getValue();
      if (Pin.FACTORY.isInputPin(pin)) {
        headers.add(pinName);
        pinList.add(pin);
      }
    }
    /* output pins last */
    for (final var entry : pinLabels.entrySet()) {
      final var pin = entry.getKey();
      final var pinName = entry.getValue();
      if (!Pin.FACTORY.isInputPin(pin)) {
        headers.add(pinName);
        pinList.add(pin);
      }
    }

    final var inputCount = inputNames.size();
    final var rowCount = 1 << inputCount;

    var needTableHeader = true;
    final var valueMap = new HashMap<Instance, Value>();
    for (var i = 0; i < rowCount; i++) {
      valueMap.clear();
      final var circuitState = CircuitState.createRootState(proj, circuit);
      var incol = 0;
      for (final var pin : inputPins) {
        final var width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
        final var v = new Value[width];
        for (var b = width - 1; b >= 0; b--) {
          final var value = TruthTable.isInputSet(i, incol++, inputCount);
          v[b] = value ? Value.TRUE : Value.FALSE;
        }
        final var pinState = circuitState.getInstanceState(pin);
        Pin.FACTORY.driveInputPin(pinState, Value.create(v));
        valueMap.put(pin, Value.create(v));
      }

      final var prop = circuitState.getPropagator();
      prop.propagate();
      /*
       * TODO for the SimulatorPrototype class do { prop.step(); } while
       * (prop.isPending());
       */
      // TODO: Search for circuit state

      for (final var pin : outputPins) {
        if (prop.isOscillating()) {
          final var width = pin.getAttributeValue(StdAttr.WIDTH);
          valueMap.put(pin, Value.createError(width));
        } else {
          final var pinState = circuitState.getInstanceState(pin);
          final var outValue = Pin.FACTORY.getValue(pinState);
          valueMap.put(pin, outValue);
        }
      }
      final var currValues = new ArrayList<Value>();
      for (final var pin : pinList) {
        currValues.add(valueMap.get(pin));
      }
      displayTableRow(needTableHeader, null, currValues, headers, formats, format);
      needTableHeader = false;
    }

    return 0;
  }

  private static int runSimulation(CircuitState circState, ArrayList<Instance> outputPins, Instance haltPin, int format) {
    final var showTable = (format & FORMAT_TABLE) != 0;
    final var showSpeed = (format & FORMAT_SPEED) != 0;
    final var showTty = (format & FORMAT_TTY) != 0;
    final var showHalt = (format & FORMAT_HALT) != 0;

    ArrayList<InstanceState> keyboardStates = null;
    StdinThread stdinThread = null;
    if (showTty) {
      keyboardStates = new ArrayList<>();
      final var ttyFound = prepareForTty(circState, keyboardStates);
      if (!ttyFound) {
        logger.error("{}", S.get("ttyNoTtyError"));
        System.exit(-1);
      }
      if (keyboardStates.isEmpty()) {
        keyboardStates = null;
      } else {
        stdinThread = new StdinThread();
        stdinThread.start();
      }
    }

    var retCode = 0;
    long tickCount = 0;
    final var start = System.currentTimeMillis();
    var halted = false;
    ArrayList<Value> prevOutputs = null;
    final var prop = circState.getPropagator();
    while (true) {
      final var curOutputs = new ArrayList<Value>();
      for (final var pin : outputPins) {
        final var pinState = circState.getInstanceState(pin);
        final var val = Pin.FACTORY.getValue(pinState);
        if (pin == haltPin) {
          halted |= val.equals(Value.TRUE);
        } else if (showTable) {
          curOutputs.add(val);
        }
      }
      if (showTable) {
        displayTableRow(prevOutputs, curOutputs);
      }

      if (halted) {
        retCode = 0; // normal exit
        break;
      }
      if (prop.isOscillating()) {
        retCode = 1; // abnormal exit
        break;
      }
      if (keyboardStates != null) {
        final var buffer = stdinThread.getBuffer();
        if (buffer != null) {
          for (final var keyState : keyboardStates) {
            Keyboard.addToBuffer(keyState, buffer);
          }
        }
      }
      prevOutputs = curOutputs;
      tickCount++;
      prop.toggleClocks();
      prop.propagate();
    }
    final var elapse = System.currentTimeMillis() - start;
    if (showTty) ensureLineTerminated();
    if (showHalt || retCode != 0) {
      if (retCode == 0) {
        logger.error("{}", S.get("ttyHaltReasonPin"));
      } else if (retCode == 1) {
        logger.error("{}", S.get("ttyHaltReasonOscillation"));
      }
    }
    if (showSpeed) {
      displaySpeed(tickCount, elapse);
    }
    return retCode;
  }

  public static void sendFromTty(char c) {
    lastIsNewline = c == '\n';
    System.out.print(c);
  }

  // It's possible to avoid using the separate thread using
  // System.in.available(),
  // but this doesn't quite work because on some systems, the keyboard input
  // is not interactively echoed until System.in.read() is invoked.
  private static class StdinThread extends UniquelyNamedThread {
    private final LinkedList<char[]> queue; // of char[]

    public StdinThread() {
      super("TtyInterface-StdInThread");
      queue = new LinkedList<>();
    }

    public char[] getBuffer() {
      synchronized (queue) {
        return queue.isEmpty() ? null : queue.removeFirst();
      }
    }

    @Override
    public void run() {
      final var stdin = new InputStreamReader(System.in);
      final var buffer = new char[32];
      while (true) {
        try {
          int nbytes = stdin.read(buffer);
          if (nbytes > 0) {
            final var add = new char[nbytes];
            System.arraycopy(buffer, 0, add, 0, nbytes);
            synchronized (queue) {
              queue.addLast(add);
            }
          }
        } catch (IOException ignored) {
        }
      }
    }
  }
}
