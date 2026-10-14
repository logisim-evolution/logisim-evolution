/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import static com.cburch.logisim.verilog.WordNetlist.attrs;
import static com.cburch.logisim.verilog.WordNetlist.constant;
import static com.cburch.logisim.verilog.WordNetlist.param;
import static com.cburch.logisim.verilog.WordNetlist.term;
import static com.cburch.logisim.verilog.YosysJson.array;
import static com.cburch.logisim.verilog.YosysJson.checkCancelled;
import static com.cburch.logisim.verilog.YosysJson.map;
import static com.cburch.logisim.verilog.YosysJson.parameter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Native memory eligibility and lowering, matching the reference converter's two-state policy. */
final class NativeMemory {
  private NativeMemory() {}

  static int number(Map<String, Object> cell, String name) {
    try {
      return param(cell, name, 0).intValueExact();
    } catch (ArithmeticException exception) {
      throw new ImportException("Memory parameter out of range: " + name);
    }
  }

  static String initialization(Map<String, Object> cell) {
    long count = (long) number(cell, "WIDTH") * number(cell, "SIZE");
    if (count < 1 || count > Integer.MAX_VALUE - 8) {
      throw new ImportException("Memory initialization is too large");
    }
    var value = map(cell, "parameters").getOrDefault("INIT", "x");
    String text = value instanceof String s ? s : parameter(value).toString(2);
    if (!text.matches("[01xz]+")) {
      throw new ImportException("Invalid memory initialization");
    }
    char fill = text.charAt(0) == 'x' || text.charAt(0) == 'z' ? text.charAt(0) : '0';
    if (text.length() < count) {
      text = String.valueOf(fill).repeat((int) count - text.length()) + text;
    }
    return text.substring(text.length() - (int) count);
  }

  static String reason(Map<String, Object> cell) {
    if (!"$mem_v2".equals(cell.get("type"))) {
      return "Memory requires memory_collect";
    }
    int width = number(cell, "WIDTH");
    int size = number(cell, "SIZE");
    int abits = number(cell, "ABITS");
    int offset = number(cell, "OFFSET");
    int reads = number(cell, "RD_PORTS");
    int writes = number(cell, "WR_PORTS");
    if (width < 1 || width > 64) {
      return "Native memory data width must be 1..64";
    }
    if (size < 1 || size > 1 << 24) {
      return "Native memory depth must be 1..16777216";
    }
    if (abits < 1 || abits > 64) {
      return "Memory address width must be 1..64";
    }
    if (offset < 0
        || BigInteger.valueOf((long) offset + size).compareTo(BigInteger.ONE.shiftLeft(abits))
            > 0) {
      return "Memory range lies outside its unsigned address space";
    }
    if (reads < 1) {
      return "Memory has no read ports";
    }
    if (writes < 0 || writes > 1) {
      return "Multiple write ports require synthesized memory logic";
    }
    for (String field : List.of("RD_CLK_ENABLE", "RD_WIDE_CONTINUATION", "WR_WIDE_CONTINUATION")) {
      if (param(cell, field, 0).signum() != 0) {
        return field + " requires synthesized memory logic";
      }
    }
    var cn = map(cell, "connections");
    var lengths = Map.of("RD_ADDR", (long) reads * abits, "RD_DATA", (long) reads * width, "RD_CLK",
        (long) reads, "RD_EN", (long) reads, "RD_ARST", (long) reads, "RD_SRST", (long) reads,
        "WR_ADDR", (long) writes * abits, "WR_DATA", (long) writes * width, "WR_EN",
        (long) writes * width, "WR_CLK", (long) writes);
    for (var entry : lengths.entrySet()) {
      if (!cn.containsKey(entry.getKey())
          || array(cn.get(entry.getKey())).size() != entry.getValue()) {
        return "Memory connection width mismatch: " + entry.getKey();
      }
    }
    for (String key : List.of("RD_ARST", "RD_SRST")) {
      if (array(cn.get(key)).stream().anyMatch(b -> !b.equals("0"))) {
        return "Read reset requires logic";
      }
    }
    if (writes == 1) {
      if (!param(cell, "WR_CLK_ENABLE", 0).equals(BigInteger.ONE)) {
        return "Asynchronous write requires logic";
      }
      if (initialization(cell).contains("1")) {
        return "Nonzero RAM initialization requires logic";
      }
    }
    return null;
  }

  private record Address(List<Object> bits, List<Object> valid) {}

  private static Address address(WordNetlist nl, List<Object> bits, int size, int offset, int aw) {
    var minimum = BigInteger.ZERO;
    var maximum = BigInteger.ZERO;
    for (int i = 0; i < bits.size(); i++) {
      if (bits.get(i).equals("1")) {
        minimum = minimum.setBit(i);
      }
      if (!bits.get(i).equals("0")) {
        maximum = maximum.setBit(i);
      }
    }
    var valid = constant(1, 1);
    if (minimum.compareTo(BigInteger.valueOf(offset)) < 0) {
      valid = nl.compare(bits, constant(offset, bits.size()), "ge", false);
    }
    if (maximum.compareTo(BigInteger.valueOf((long) offset + size)) >= 0) {
      var upper = nl.compare(bits, constant((long) offset + size, bits.size()), "lt", false);
      valid = valid.equals(constant(1, 1)) ? upper : nl.gate("AND Gate", valid, upper);
    }
    var adjusted = bits;
    if (offset != 0) {
      adjusted = nl.fresh(bits.size());
      nl.arithmetic("Subtractor", bits, constant(offset, bits.size()), adjusted);
    }
    return new Address(nl.extend(adjusted, aw, false), valid);
  }

  private static void output(
      WordNetlist nl, List<Object> raw, List<Object> valid, List<Object> target) {
    if (valid.equals(constant(1, 1))) {
      nl.alias(raw, target);
    } else {
      if (nl.undefined.equals("error")) {
        throw new ImportException("Out-of-range memory reads require an undefined-value policy");
      }
      nl.mux(constant(nl.undefined.equals("1") ? -1 : 0, raw.size()), raw, valid, target);
    }
  }

  static void lower(WordNetlist nl, Map<String, Object> cell) {
    var reason = reason(cell);
    if (reason != null) {
      throw new ImportException(reason);
    }
    int width = number(cell, "WIDTH");
    int size = number(cell, "SIZE");
    int abits = number(cell, "ABITS");
    int offset = number(cell, "OFFSET");
    int reads = number(cell, "RD_PORTS");
    int aw = Math.max(2, 32 - Integer.numberOfLeadingZeros(size - 1));
    var readBits = nl.connection(cell, "RD_ADDR");
    var outputs = nl.connection(cell, "RD_DATA");
    if (number(cell, "WR_PORTS") == 0) {
      var init = initialization(cell);
      if (init.contains("x") || init.contains("z")) {
        if (nl.undefined.equals("error")) {
          throw new ImportException("Undefined ROM contents require a zero/one policy");
        }
        init = init.replace("x", nl.undefined).replace("z", nl.undefined);
      }
      var contents = new StringBuilder("addr/data: " + aw + " " + width + "\n");
      for (int i = 0; i < size; i++) {
        checkCancelled();
        contents
            .append(new BigInteger(
                init.substring(init.length() - (i + 1) * width, init.length() - i * width), 2)
                    .toString(16))
            .append(i % 16 == 15 ? '\n' : ' ');
      }
      for (int i = 0; i < reads; i++) {
        var addr = address(nl, readBits.subList(i * abits, (i + 1) * abits), size, offset, aw);
        var raw = nl.fresh(width);
        nl.add("ROM", "Memory",
            attrs("addrWidth", aw, "dataWidth", width, "appearance", "classic", "contents",
                contents, "line", "single"),
            term(nl.bus(addr.bits()), 0, 10, -20, 10), term(nl.bus(raw, true), 240, 60, 260, 170));
        output(nl, raw, addr.valid(), outputs.subList(i * width, (i + 1) * width));
      }
      return;
    }
    var writeBits = nl.connection(cell, "WR_ADDR");
    var wa = address(nl, writeBits, size, offset, aw);
    var data = nl.connection(cell, "WR_DATA");
    var enable = nl.connection(cell, "WR_EN");
    var clock = nl.connection(cell, "WR_CLK");
    for (int i = 0; i < reads; i++) {
      var read = readBits.subList(i * abits, (i + 1) * abits);
      var ra = address(nl, read, size, offset, aw);
      var raw = new ArrayList<Object>();
      for (int start = 0; start < width;) {
        int end = start + 1;
        while (end < width && enable.get(end).equals(enable.get(start))) {
          end++;
        }
        int bankWidth = end - start;
        var q = nl.fresh(bankWidth);
        raw.addAll(q);
        var we = List.of(enable.get(start));
        if (!wa.valid().equals(constant(1, 1))) {
          we = nl.gate("AND Gate", we, wa.valid());
        }
        var config = attrs("addrWidth", aw, "dataWidth", bankWidth, "appearance", "classic",
            "enables", "line", "line", "single", "databus", "bibus", "trigger",
            number(cell, "WR_CLK_POLARITY") != 0 ? "rising" : "falling", "clearpin", false, "type",
            "volatile");
        if (writeBits.equals(read)) {
          nl.add("RAM", "Memory", config, term(nl.bus(wa.bits()), 0, 10, -20, 10),
              term(nl.bus(q, true), 240, 60, 260, 170),
              term(nl.bus(data.subList(start, end)), 0, 60, -20, 60),
              term(nl.bus(we), 0, 30, -20, 30), term(nl.bus(clock), 0, 40, -20, 40));
        } else {
          nl.add("DualRAM", "Memory", config, term(nl.bus(wa.bits()), 0, 10, -20, 10),
              term(nl.bus(ra.bits()), 0, 110, -20, 110), term(nl.bus(q, true), 240, 160, 260, 230),
              term(nl.bus(data.subList(start, end)), 0, 60, -20, 60),
              term(nl.bus(constant(0, bankWidth)), 0, 160, -20, 160),
              term(nl.bus(we), 0, 30, -20, 30), term(nl.bus(constant(0, 1)), 0, 130, -20, 130),
              term(nl.bus(clock), 0, 40, -20, 40), term(nl.bus(constant(0, 1)), 0, 140, -20, 140));
        }
        start = end;
      }
      output(nl, raw, ra.valid(), outputs.subList(i * width, (i + 1) * width));
    }
  }
}
