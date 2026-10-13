/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import static com.cburch.logisim.verilog.YosysJson.array;
import static com.cburch.logisim.verilog.YosysJson.checkCancelled;
import static com.cburch.logisim.verilog.YosysJson.map;
import static com.cburch.logisim.verilog.YosysJson.object;
import static com.cburch.logisim.verilog.YosysJson.parameter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Word-level mapping. Vectors are always in Yosys least-significant-bit-first order. */
final class WordNetlist {
  record Terminal(String net, int x, int y, int escapeX, int escapeY) {}
  record Element(String name, String library, Map<String, String> attributes,
      List<Terminal> terminals, int height, int top, String port) {}
  record Port(String name, String label, boolean input, int x, int y) {}
  record Owner(List<Object> vector, int position) {}

  final List<Element> elements = new ArrayList<>();
  final List<Port> ports = new ArrayList<>();
  final Map<String, Integer> widths = new LinkedHashMap<>();
  final Map<List<Object>, String> vectors = new LinkedHashMap<>();
  final Set<List<Object>> sources = new LinkedHashSet<>();
  final Map<Object, Object> aliases = new HashMap<>();
  final Map<String, Object> module;
  final YosysDesign design;
  final String undefined;
  private final Set<String> labels = new HashSet<>();
  private long nextBit = -1;

  WordNetlist(YosysDesign design, String name, String undefined, Map<String, String> names) {
    this.design = design;
    this.module = design.module(name);
    this.undefined = undefined;
    if (!map(module, "memories").isEmpty()) {
      throw new ImportException("Uncollected memory: run Yosys memory -nomap -nordff");
    }
    for (var entry : map(module, "netnames").entrySet()) {
      var init = map(object(entry.getValue()), "attributes").get("init");
      if (init != null
          && (init instanceof String s ? s : parameter(init).toString(2)).contains("1")) {
        throw new ImportException(
            "Nonzero initial register value on " + entry.getKey() + "; use a reset sequence");
      }
    }
    int inputs = 0;
    int outputs = 0;
    for (var entry : map(module, "ports").entrySet()) {
      var port = object(entry.getValue());
      String direction = port.get("direction").toString();
      if (!direction.equals("input") && !direction.equals("output")) {
        throw new ImportException("Unsupported inout port " + entry.getKey());
      }
      boolean input = direction.equals("input");
      var bits = bits(port.get("bits"));
      var label = uniqueName(entry.getKey(), labels);
      ports.add(new Port(entry.getKey(), label, input, input ? 0 : 200,
          input ? 20 + 40 * inputs++ : 40 + 40 * outputs++));
      var config = attrs("appearance", "classic", "label", label, "width", bits.size(), "facing",
          input ? "east" : "west", "type", input ? "input" : "output");
      elements.add(new Element("Pin", "Wiring", config,
          List.of(term(bus(bits, input), 0, 0, -20, 0)), 60, 0, entry.getKey()));
    }
    for (var entry : map(module, "cells").entrySet()) {
      checkCancelled();
      var cell = object(entry.getValue());
      var type = cell.get("type").toString();
      try {
        if (type.equals("$scopeinfo")) {
          continue;
        }
        if (design.modules.containsKey(type)) {
          instance(entry.getKey(), cell, names);
        } else {
          lower(cell);
        }
      } catch (ImportException exception) {
        throw new ImportException(
            name + "." + entry.getKey() + " (" + type + "): " + exception.getMessage());
      }
    }
    connectBuses();
  }

  static String uniqueName(String name, Set<String> used) {
    String base = name.replaceAll("[^A-Za-z0-9_]+", "_").replaceAll("^_+|_+$", "");
    if (base.isEmpty()) {
      base = "signal";
    }
    if (!Character.isLetter(base.charAt(0))) {
      base = "n_" + base;
    }
    var candidate = base;
    int suffix = 1;
    while (!used.add(candidate.toLowerCase(Locale.ROOT))) {
      candidate = base + "_" + suffix++;
    }
    return candidate;
  }

  List<Object> bits(Object value) {
    var result = new ArrayList<Object>();
    for (var bit : array(value)) {
      if (bit instanceof Number) {
        var number = parameter(bit);
        if (number.signum() < 0) {
          throw new ImportException("Negative signal ID");
        }
        result.add(number);
      } else if (bit instanceof String text && Set.of("0", "1", "x", "z").contains(text)) {
        if (text.equals("x") || text.equals("z")) {
          if (undefined.equals("error")) {
            throw new ImportException("Undefined value requires an explicit zero/one policy");
          }
          text = undefined;
        }
        result.add(text);
      } else {
        throw new ImportException("Invalid signal bit: " + bit);
      }
    }
    return List.copyOf(result);
  }

  static List<Object> constant(long value, int width) {
    return constant(BigInteger.valueOf(value), width);
  }

  static List<Object> constant(BigInteger value, int width) {
    var result = new ArrayList<Object>();
    for (int i = 0; i < width; i++) {
      result.add(value.testBit(i) ? "1" : "0");
    }
    return List.copyOf(result);
  }

  List<Object> fresh(int width) {
    checkWidth(width);
    var result = new ArrayList<Object>();
    for (int i = 0; i < width; i++) {
      result.add(BigInteger.valueOf(nextBit--));
    }
    return List.copyOf(result);
  }

  static void checkWidth(int width) {
    if (width < 1 || width > 64) {
      throw new ImportException("Bus width must be 1..64: " + width);
    }
  }

  String bus(List<Object> bits) {
    return bus(bits, false);
  }

  String bus(List<Object> bits, boolean source) {
    checkWidth(bits.size());
    bits = List.copyOf(bits);
    if (source) {
      if (bits.stream().anyMatch(b -> b instanceof String)
          || new HashSet<>(bits).size() != bits.size() || !sources.add(bits)) {
        throw new ImportException("Repeated or constant-driven output bus");
      }
    }
    if (!vectors.containsKey(bits)) {
      var name = "b" + vectors.size();
      vectors.put(bits, name);
      widths.put(name, bits.size());
    }
    return vectors.get(bits);
  }

  static Map<String, String> attrs(Object... values) {
    var result = new LinkedHashMap<String, String>();
    for (int i = 0; i < values.length; i += 2) {
      result.put(values[i].toString(), values[i + 1].toString());
    }
    return result;
  }

  static Terminal term(String net, int x, int y, int ex, int ey) {
    return new Terminal(net, x, y, ex, ey);
  }

  void add(String name, String library, Map<String, String> attrs, Terminal... terms) {
    int min = 0;
    int max = 0;
    for (var t : terms) {
      min = Math.min(min, Math.min(t.y(), t.escapeY()));
      max = Math.max(max, Math.max(t.y(), t.escapeY()));
    }
    elements.add(new Element(name, library, attrs, List.of(terms), max + 80, -min, null));
  }

  void alias(List<Object> source, List<Object> target) {
    if (source.size() != target.size()) {
      throw new ImportException("Connection width mismatch");
    }
    for (int i = 0; i < source.size(); i++) {
      if (!source.get(i).equals(target.get(i))) {
        aliases.put(target.get(i), source.get(i));
      }
    }
  }

  List<Object> gate(String name, List<Object> a, List<Object> b) {
    return gate(name, a, b, fresh(a.size()));
  }

  List<Object> gate(String name, List<Object> a, List<Object> b, List<Object> y) {
    if (a.size() != y.size() || (b != null && b.size() != a.size())) {
      throw new ImportException("Gate width mismatch");
    }
    String an = bus(a);
    String yn = bus(y, true);
    if (b == null) {
      add(name, "Gates", attrs("width", a.size(), "size", 30), term(an, -30, 0, -50, 0),
          term(yn, 0, 0, 20, 30));
    } else {
      int ix = name.equals("XNOR Gate")                                ? -70
          : Set.of("NAND Gate", "NOR Gate", "XOR Gate").contains(name) ? -60
                                                                       : -50;
      add(name, "Gates", attrs("width", a.size(), "size", 50, "inputs", 2),
          term(an, ix, -20, ix - 20, -20), term(bus(b), ix, 20, ix - 20, 20),
          term(yn, 0, 0, 20, 40));
    }
    return y;
  }

  List<Object> extend(List<Object> a, int width, boolean signed) {
    checkWidth(width);
    if (a.isEmpty()) {
      throw new ImportException("Empty operand");
    }
    if (a.size() >= width) {
      return List.copyOf(a.subList(0, width));
    }
    var y = fresh(width);
    add("Bit Extender", "Wiring",
        attrs("in_width", a.size(), "out_width", width, "type", signed ? "sign" : "zero"),
        term(bus(a), -40, 0, -60, 0), term(bus(y, true), 0, 0, 20, 30));
    return y;
  }

  List<Object> active(List<Object> signal, boolean positive) {
    if (signal.size() != 1) {
      throw new ImportException("Control signal must be one bit");
    }
    return positive ? signal : gate("NOT Gate", signal, null);
  }

  List<Object> mux(List<Object> a, List<Object> b, List<Object> select) {
    return mux(a, b, select, fresh(a.size()));
  }

  List<Object> mux(List<Object> a, List<Object> b, List<Object> select, List<Object> y) {
    return multiMux(List.of(a, b), select, y);
  }

  List<Object> multiMux(List<List<Object>> inputs, List<Object> select, List<Object> y) {
    int count = inputs.size();
    if (select.isEmpty() || select.size() > 5 || count != 1 << select.size()) {
      throw new ImportException("Native multiplexer supports 1..5 selector bits");
    }
    if (inputs.stream().anyMatch(a -> a.size() != y.size())) {
      throw new ImportException("Multiplexer width mismatch");
    }
    int ix = count == 2 ? -30 : -40;
    int sy = count == 2 ? 20 : 5 * count;
    var terms = new ArrayList<Terminal>();
    for (int i = 0; i < count; i++) {
      int row = count == 2 ? -10 + 20 * i : 10 * i - 5 * count;
      terms.add(term(bus(inputs.get(i)), ix, row, ix - 20, row));
    }
    terms.add(term(bus(select), -20, sy, -20, sy + 10));
    terms.add(term(bus(y, true), 0, 0, 20, sy + 30));
    add("Multiplexer", "Plexers",
        attrs("width", y.size(), "select", select.size(), "enable", false, "size", 40),
        terms.toArray(Terminal[] ::new));
    return y;
  }

  void arithmetic(String name, List<Object> a, List<Object> b, List<Object> y) {
    if (a.size() != y.size() || b.size() != y.size()) {
      throw new ImportException("Arithmetic width mismatch");
    }
    add(name.equals("Mod") ? "Divider" : name, "Arithmetic", attrs("width", y.size()), 
      term(bus(a), -40, -10, -60, -10),
      term(bus(b), -40, 10, -60, 10), 
      term(bus(y, true), 
          name.equals("Mod") ? -20 : 0,  // x1: shifted left to hit the rem port
          name.equals("Mod") ? 20 : 0,   // y1: shifted down to hit the rem port
          name.equals("Mod") ? -20 : 20, // x2: stub wire offset
          name.equals("Mod") ? 40 : 40   // y2: stub wire offset
      ),
      term(bus(constant(0, (name.equals("Multiplier") || name.equals("Divider") || name.equals("Mod")) ? y.size() : 1)), -20, -20, -20, -40));
  }

  List<Object> compare(List<Object> a, List<Object> b, String relation, boolean signed) {
    if (a.size() != b.size()) {
      throw new ImportException("Comparator width mismatch");
    }
    var outputs = List.of(fresh(1), fresh(1), fresh(1));
    var terms = new ArrayList<Terminal>();
    terms.add(term(bus(a), -40, -10, -60, -10));
    terms.add(term(bus(b), -40, 10, -60, 10));
    for (int i = 0; i < 3; i++) {
      terms.add(term(bus(outputs.get(i), true), 0, 10 * i - 10, 20 + 10 * i, 40 + 10 * i));
    }
    add("Comparator", "Arithmetic",
        attrs("width", a.size(), "mode", signed ? "twosComplement" : "unsigned"),
        terms.toArray(Terminal[] ::new));
    var result = outputs.get(switch (relation) {
      case "gt", "le" -> 0;
      case "eq", "ne" -> 1;
      case "lt", "ge" -> 2;
      default -> throw new ImportException("Invalid comparison: " + relation);
    });
    return Set.of("le", "ne", "ge").contains(relation) ? gate("NOT Gate", result, null) : result;
  }

  List<Object> bool(List<Object> a) {
    return a.size() == 1 ? a : compare(a, constant(0, a.size()), "ne", false);
  }

  static BigInteger param(Map<String, Object> cell, String name, int fallback) {
    return parameter(map(cell, "parameters").getOrDefault(name, fallback));
  }

  List<Object> connection(Map<String, Object> cell, String name) {
    var value = map(cell, "connections").get(name);
    if (value == null) {
      throw new ImportException("Missing cell connection " + name);
    }
    return bits(value);
  }

  void register(Map<String, Object> cell) {
    String type = cell.get("type").toString();
    var q = connection(cell, "Q");
    var d = connection(cell, "D");
    if (q.size() != d.size() || param(cell, "WIDTH", q.size()).intValueExact() != q.size()) {
      throw new ImportException("Register width mismatch");
    }
    boolean latch = type.equals("$dlatch") || type.equals("$adlatch");
    boolean positive = param(cell, latch ? "EN_POLARITY" : "CLK_POLARITY", 1).signum() != 0;
    String trigger = latch ? positive ? "high" : "low" : positive ? "rising" : "falling";
    var clock = connection(cell, latch ? "EN" : "CLK");
    active(clock, true);
    var en = constant(1, 1);
    var clear = constant(0, 1);
    if (Set.of("$dffe", "$adffe", "$sdffe", "$sdffce").contains(type)) {
      en = active(connection(cell, "EN"), param(cell, "EN_POLARITY", 1).signum() != 0);
    }
    if (Set.of("$sdff", "$sdffe", "$sdffce").contains(type)) {
      var reset = active(connection(cell, "SRST"), param(cell, "SRST_POLARITY", 1).signum() != 0);
      d = mux(d, constant(param(cell, "SRST_VALUE", 0), q.size()), reset);
      if (type.equals("$sdffe")) {
        en = gate("OR Gate", en, reset);
      }
    }
    var storage = q;
    if (Set.of("$adff", "$adffe", "$adlatch").contains(type)) {
      clear = active(connection(cell, "ARST"), param(cell, "ARST_POLARITY", 1).signum() != 0);
      var resetValue = param(cell, "ARST_VALUE", 0);
      if (resetValue.signum() != 0) {
        for (var value : map(module, "netnames").values()) {
          var net = object(value);
          if (map(net, "attributes").containsKey("init")
              && !Collections.disjoint(array(net.get("bits")), q)) {
            throw new ImportException(
                "Initialized nonzero asynchronous-reset register is unsupported");
          }
        }
        var mask = constant(resetValue, q.size());
        d = gate("XOR Gate", d, mask);
        storage = fresh(q.size());
        gate("XOR Gate", storage, mask, q);
      }
    }
    add("Register", "Memory",
        attrs("width", q.size(), "appearance", "logisim_evolution", "trigger", trigger),
        term(bus(d), 0, 30, -20, 30), term(bus(storage, true), 60, 30, 80, 120),
        term(bus(clock), 0, 70, -20, 70), term(bus(clear), 30, 90, 30, 100),
        term(bus(en), 0, 50, -20, 50));
  }

  void lower(Map<String, Object> cell) {
    String type = cell.get("type").toString();
    if (type.equals("$mem_v2")) {
      NativeMemory.lower(this, cell);
      return;
    }
    if (Set.of("$dff", "$dffe", "$adff", "$adffe", "$sdff", "$sdffe", "$sdffce", "$dlatch",
               "$adlatch")
            .contains(type)) {
      register(cell);
      return;
    }
    var y = map(cell, "connections").containsKey("Y") ? connection(cell, "Y") : List.<Object>of();
    int width = y.size();
    switch (type) {
      case "$add", "$sub", "$mul", "$and", "$div", "$mod", "$or", "$xor", "$xnor", "$not", "$pos", "$neg" -> {
        boolean unary = Set.of("$not", "$pos", "$neg").contains(type);
        boolean signed = param(cell, "A_SIGNED", 0).signum() != 0
            && (unary || param(cell, "B_SIGNED", 0).signum() != 0);
        var a = extend(connection(cell, "A"), width, signed);
        if (type.equals("$pos")) {
          alias(a, y);
        } else if (type.equals("$not")) {
          gate("NOT Gate", a, null, y);
        } else if (type.equals("$neg")) {
          arithmetic("Subtractor", constant(0, width), a, y);
        } else {
          var b = extend(connection(cell, "B"), width, signed);
          switch (type) {
            case "$add" -> arithmetic("Adder", a, b, y);
            case "$sub" -> arithmetic("Subtractor", a, b, y);
            case "$mul" -> arithmetic("Multiplier", a, b, y);
            case "$div" -> arithmetic("Divider", a, b, y);
            case "$mod" -> arithmetic("Mod", a, b, y);
            default -> gate(type.substring(1).toUpperCase(Locale.ROOT) + " Gate", a, b, y);
          }
        }
      }
      case "$mux" -> mux(connection(cell, "A"), connection(cell, "B"), connection(cell, "S"), y);
      case "$bmux" -> {
        var a = connection(cell, "A");
        var s = connection(cell, "S");
        if (s.isEmpty()) {
          alias(a, y);
        } else {
          if (s.size() > 5 || a.size() != width * (1 << s.size())) {
            throw new ImportException("Unsupported bus multiplexer width");
          }
          var inputs = new ArrayList<List<Object>>();
          for (int i = 0; i < 1 << s.size(); i++) {
            inputs.add(a.subList(i * width, (i + 1) * width));
          }
          multiMux(inputs, s, y);
        }
      }
      case "$pmux" -> {
        if (undefined.equals("error")) {
          throw new ImportException("Parallel mux requires an undefined-value policy");
        }
        var result = connection(cell, "A");
        var b = connection(cell, "B");
        var select = connection(cell, "S");
        if (result.size() != width || b.size() != width * select.size()) {
          throw new ImportException("Parallel multiplexer width mismatch");
        }
        var seen = constant(0, 1);
        var invalid = constant(0, 1);
        for (int i = 0; i < select.size(); i++) {
          var s = List.of(select.get(i));
          invalid = gate("OR Gate", invalid, gate("AND Gate", seen, s));
          seen = gate("OR Gate", seen, s);
          result = mux(result, b.subList(i * width, (i + 1) * width), s);
        }
        mux(result, constant(undefined.equals("1") ? -1 : 0, width), invalid, y);
      }
      case "$demux" -> {
        var a = connection(cell, "A");
        var s = connection(cell, "S");
        if (s.isEmpty()) {
          alias(a, y);
          return;
        }
        if (s.size() > 5 || width != (1 << s.size()) * a.size()) {
          throw new ImportException("Unsupported demultiplexer width");
        }
        int count = 1 << s.size();
        int ox = count == 2 ? 30 : 40;
        int sy = count == 2 ? 20 : 5 * count;
        var terms = new ArrayList<Terminal>();
        terms.add(term(bus(a), 0, 0, -20, 0));
        terms.add(term(bus(s), 20, sy, 20, sy + 10));
        for (int i = 0; i < count; i++) {
          int row = count == 2 ? -10 + 20 * i : 10 * i - 5 * count;
          terms.add(term(bus(y.subList(i * a.size(), (i + 1) * a.size()), true), ox, row,
              ox + 20 + 10 * i, sy + 30 + 10 * i));
        }
        add("Demultiplexer", "Plexers",
            attrs("width", a.size(), "select", s.size(), "enable", false, "tristate", false,
                "disabled", "0"),
            terms.toArray(Terminal[] ::new));
      }
      case "$eq", "$ne", "$lt", "$le", "$gt", "$ge" -> {
        var a = connection(cell, "A");
        var b = connection(cell, "B");
        int size = Math.max(a.size(), b.size());
        boolean signed =
            param(cell, "A_SIGNED", 0).signum() != 0 && param(cell, "B_SIGNED", 0).signum() != 0;
        alias(extend(compare(extend(a, size, signed), extend(b, size, signed), type.substring(1),
                         signed),
                  width, false),
            y);
      }
      case "$logic_not", "$logic_and", "$logic_or", "$reduce_bool", "$reduce_or", "$reduce_and",
          "$reduce_xor", "$reduce_xnor" -> {
        var a = connection(cell, "A");
        List<Object> result;
        if (type.equals("$reduce_xor") || type.equals("$reduce_xnor")) {
          result = List.of(a.getFirst());
          for (int i = 1; i < a.size(); i++) {
            result = gate("XOR Gate", result, List.of(a.get(i)));
          }
          if (type.equals("$reduce_xnor")) {
            result = gate("NOT Gate", result, null);
          }
        } else if (type.equals("$reduce_and")) {
          result = compare(a, constant(-1, a.size()), "eq", false);
        } else {
          result = bool(a);
          if (type.equals("$logic_not")) {
            result = gate("NOT Gate", result, null);
          }
          if (type.equals("$logic_and") || type.equals("$logic_or")) {
            result = gate(type.equals("$logic_and") ? "AND Gate" : "OR Gate", result,
                bool(connection(cell, "B")));
          }
        }
        alias(extend(result, width, false), y);
      }
      case "$shl", "$sshl", "$shr", "$sshr" -> {
        var a = connection(cell, "A");
        var shift = connection(cell, "B");
        boolean signed = param(cell, "A_SIGNED", 0).signum() != 0;
        boolean arithmetic = type.equals("$sshr") && signed;
        int size = Math.max(a.size(), width);
        a = extend(a, size, signed);
        int sbits = Math.max(1, 32 - Integer.numberOfLeadingZeros(size - 1));
        var small = extend(shift, sbits, false);
        var result = fresh(size);
        add("Shifter", "Arithmetic",
            attrs("width", size, "shift",
                arithmetic                 ? "ar"
                    : type.endsWith("shr") ? "lr"
                                           : "ll"),
            term(bus(a), -40, -10, -60, -10), term(bus(small), -40, 10, -60, 10),
            term(bus(result, true), 0, 0, 20, 40));
        if (shift.size() > sbits) {
          var fill =
              arithmetic ? List.copyOf(Collections.nCopies(size, a.getLast())) : constant(0, size);
          result = mux(result, fill, bool(shift.subList(sbits, shift.size())));
        }
        alias(result.subList(0, width), y);
      }
      case "$slice" -> {
        int offset = param(cell, "OFFSET", 0).intValueExact();
        var a = connection(cell, "A");
        if (offset < 0 || offset > a.size() - width) {
          throw new ImportException("Invalid slice range");
        }
        alias(a.subList(offset, offset + width), y);
      }
      case "$concat" -> {
        var a = new ArrayList<>(connection(cell, "A"));
        a.addAll(connection(cell, "B"));
        alias(a, y);
      }
      case "$_BUF_" -> alias(connection(cell, "A"), y);
      case "$_NOT_" -> gate("NOT Gate", connection(cell, "A"), null, y);
      case "$_AND_", "$_OR_", "$_XOR_", "$_XNOR_", "$_NAND_", "$_NOR_" ->
        gate(type.substring(2, type.length() - 1) + " Gate", connection(cell, "A"),
            connection(cell, "B"), y);
      case "$_MUX_" -> mux(connection(cell, "A"), connection(cell, "B"), connection(cell, "S"), y);
      default -> throw new ImportException("Unsupported cell type " + type);
    }
  }

  private void instance(String name, Map<String, Object> cell, Map<String, String> names) {
    String type = cell.get("type").toString();
    var child = design.module(type);
    for (var entry : map(cell, "parameters").entrySet()) {
      var value = map(child, "parameter_default_values").get(entry.getKey());
      if (value == null || !parameter(value).equals(parameter(entry.getValue()))) {
        throw new ImportException("Unelaborated parameter " + entry.getKey());
      }
    }
    var cn = map(cell, "connections");
    var childPorts = map(child, "ports");
    if (!childPorts.keySet().containsAll(cn.keySet())) {
      throw new ImportException("Unknown module port");
    }
    var terms = new ArrayList<Terminal>();
    int inputs = 0;
    int outputs = 0;
    for (var entry : childPorts.entrySet()) {
      var port = object(entry.getValue());
      boolean input = "input".equals(port.get("direction"));
      int width = array(port.get("bits")).size();
      var data = cn.containsKey(entry.getKey()) ? bits(cn.get(entry.getKey())) : List.<Object>of();
      if (data.isEmpty()) {
        data = input ? bits(Collections.nCopies(width, "x")) : fresh(width);
      }
      if (data.size() != width) {
        throw new ImportException("Submodule port width mismatch: " + entry.getKey());
      }
      int x = input ? 0 : 200;
      int row = input ? 20 + 40 * inputs++ : 40 + 40 * outputs++;
      terms.add(term(bus(data, !input), x, row, input ? x - 20 : x + 20, input ? row : row + 10));
    }
    add(names.get(type), null, attrs("label", uniqueName(name, labels)),
        terms.toArray(Terminal[] ::new));
  }

  private Object canonical(Object bit) {
    var path = new LinkedHashSet<Object>();
    while (aliases.containsKey(bit)) {
      if (!path.add(bit)) {
        throw new ImportException("Cycle in wire aliases");
      }
      bit = aliases.get(bit);
    }
    for (var old : path) {
      aliases.put(old, bit);
    }
    return bit;
  }

  private void splitter(
      List<Object> combined, List<List<Object>> groups, List<List<Integer>> positions) {
    var config = attrs("facing", "east", "incoming", combined.size(), "fanout", groups.size(),
        "appear", "left", "spacing", 1);
    var terms = new ArrayList<Terminal>();
    terms.add(term(bus(combined), 0, 0, -20, 0));
    for (int i = 0; i < groups.size(); i++) {
      for (int bit : positions.get(i)) {
        config.put("bit" + bit, Integer.toString(i));
      }
      if (!groups.get(i).isEmpty()) {
        terms.add(
            term(bus(groups.get(i)), 20, -10 * (groups.size() - i), 40 + 10 * i, 30 + 10 * i));
      }
    }
    add("Splitter", "Wiring", config, terms.toArray(Terminal[] ::new));
  }

  private void connectBuses() {
    var owners = new HashMap<Object, Owner>();
    for (var source : sources) {
      for (int i = 0; i < source.size(); i++) {
        if (owners.putIfAbsent(canonical(source.get(i)), new Owner(source, i)) != null) {
          throw new ImportException("Multiple drivers for bit " + source.get(i));
        }
      }
    }
    var complete = new HashSet<>(sources);
    var extracts = new HashSet<List<Object>>();
    var pending = new ArrayList<>(vectors.keySet());
    for (int index = 0; index < pending.size(); index++) {
      checkCancelled();
      var vec = pending.get(index);
      if (complete.contains(vec)) {
        continue;
      }
      var canon = vec.stream().map(this::canonical).toList();
      if (canon.stream().allMatch(b -> b instanceof String)) {
        var value = BigInteger.ZERO;
        for (int i = 0; i < canon.size(); i++) {
          if (canon.get(i).equals("1")) {
            value = value.setBit(i);
          }
        }
        add("Constant", "Wiring", attrs("width", vec.size(), "value", "0x" + value.toString(16)),
            term(bus(vec), 0, 0, -20, 0));
        complete.add(vec);
        continue;
      }
      var parts = new ArrayList<List<Object>>();
      for (int i = 0; i < canon.size();) {
        Object bit = canon.get(i);
        Owner owner = owners.get(bit);
        var part = new ArrayList<Object>();
        var positions = new ArrayList<Integer>();
        if (owner == null) {
          while (i < canon.size() && !owners.containsKey(canon.get(i))) {
            bit = canon.get(i++);
            if (!(bit instanceof String)) {
              if (undefined.equals("error")) {
                throw new ImportException("Undriven bit " + bit);
              }
              bit = undefined;
            }
            part.add(bit);
          }
          bus(part);
          pending.add(List.copyOf(part));
        } else {
          var source = owner.vector();
          int position = owner.position();
          while (i < canon.size()) {
            var next = owners.get(canon.get(i));
            if (next == null || !source.equals(next.vector()) || next.position() != position) {
              break;
            }
            positions.add(position);
            part.add(source.get(position++));
            i++;
          }
          if (!part.equals(source) && extracts.add(List.copyOf(part))) {
            var remaining = new ArrayList<Integer>();
            for (int j = 0; j < source.size(); j++) {
              if (!positions.contains(j)) {
                remaining.add(j);
              }
            }
            splitter(source, List.of(part, List.of()), List.of(positions, remaining));
            complete.add(List.copyOf(part));
          }
        }
        parts.add(List.copyOf(part));
      }
      if (parts.size() == 1 && parts.getFirst().equals(vec)) {
        if (!complete.contains(vec)) {
          throw new ImportException("Unresolved bus alias");
        }
        continue;
      }
      var positions = new ArrayList<List<Integer>>();
      int offset = 0;
      for (var part : parts) {
        var indices = new ArrayList<Integer>();
        for (int i = 0; i < part.size(); i++) {
          indices.add(offset++);
        }
        positions.add(indices);
      }
      splitter(vec, parts, positions);
      complete.add(vec);
    }
  }
}
