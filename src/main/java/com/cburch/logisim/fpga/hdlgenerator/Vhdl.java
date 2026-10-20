package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.prefs.AppPreferences;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Logisim-evolution VHDL keyword manager. */
public class Vhdl {

  private static final String[] RESERVED_VHDL_WORDS = {
    "abs",
    "all",
    "access",
    "after",
    "alias",
    "all",
    "and",
    "architecture",
    "array",
    "assert",
    "attribute",
    "begin",
    "block",
    "body",
    "buffer",
    "bus",
    "case",
    "component",
    "configuration",
    "constant",
    "disconnect",
    "downto",
    "else",
    "elsif",
    "end",
    "endcase",
    "endgenerate",
    "endif",
    "endprocess",
    "entity",
    "exit",
    "file",
    "for",
    "function",
    "generate",
    "generic",
    "group",
    "guarded",
    "if",
    "integer",
    "impure",
    "in",
    "inertial",
    "inout",
    "is",
    "label",
    "library",
    "linkage",
    "literal",
    "loop",
    "map",
    "mod",
    "nand",
    "new",
    "next",
    "nor",
    "not",
    "null",
    "of",
    "on",
    "open",
    "or",
    "others",
    "out",
    "package",
    "port",
    "postponed",
    "procedure",
    "process",
    "pure",
    "range",
    "record",
    "register",
    "reject",
    "rem",
    "report",
    "return",
    "rol",
    "ror",
    "select",
    "severity",
    "signal",
    "shared",
    "sla",
    "sll",
    "sra",
    "srl",
    "subtype",
    "then",
    "to",
    "transport",
    "type",
    "unaffected",
    "units",
    "until",
    "use",
    "variable",
    "wait",
    "when",
    "while",
    "with",
    "xnor",
    "xor"
  };

  public static final List<String> VHDL_KEYWORDS = Arrays.asList(RESERVED_VHDL_WORDS);

  public static Set<String> getVhdlKeywords() {
    final Set<String> keywords = new TreeSet<>();
    for (final String keyword : VHDL_KEYWORDS) {
      keywords.add(
          AppPreferences.VhdlKeywordsUpperCase.get()
              ? keyword.toUpperCase(Locale.ROOT)
              : keyword.toLowerCase(Locale.ROOT)); // IMPORTANT!
      
    }
    return keywords;
  }

  public static String getVhdlKeyword(String keyword) {
    final String spaceStrippedKeyword = keyword.replace(" ", "").toLowerCase(Locale.ROOT);

    if (VHDL_KEYWORDS.contains(spaceStrippedKeyword)) {
      return AppPreferences.VhdlKeywordsUpperCase.get()
          ? keyword.toUpperCase(Locale.ROOT)
          : keyword.toLowerCase(Locale.ROOT);
    }

    System.err.println("FAILED ON: " + spaceStrippedKeyword);
    throw new IllegalArgumentException("An unknown VHDL keyword was passed: " + keyword);
  }
}
