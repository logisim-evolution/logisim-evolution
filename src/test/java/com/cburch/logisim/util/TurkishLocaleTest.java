/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.rv32im.RV32imIntegerRegisterImmediateInstructions;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** In Turkish, "I" lowercases to a dotless "ı" and "i" uppercases to a dotted "İ". */
class TurkishLocaleTest {
  private Locale savedLocale;
  private boolean savedUpperCase;

  @BeforeEach
  void useTurkishLocale() {
    // Set the locale last, loading the preferences applies the locale stored in them.
    savedUpperCase = AppPreferences.VhdlKeywordsUpperCase.get();
    AppPreferences.VhdlKeywordsUpperCase.set(true);
    savedLocale = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
  }

  @AfterEach
  void restoreLocale() {
    Locale.setDefault(savedLocale);
    AppPreferences.VhdlKeywordsUpperCase.set(savedUpperCase);
  }

  @Test
  void vhdlKeywordPlaceholdersAreFound() {
    final var line = LineBuffer.getBuffer().addVhdlKeywords().add("{{entity}} {{is}}").get(0);
    assertEquals("ENTITY IS", line);
  }

  @Test
  void uppercaseVhdlKeywordIsDetected() {
    assertTrue(CorrectLabel.isKeyword("IS", HdlGeneratorFactory.VHDL, false));
  }

  @Test
  void assemblerKnowsInstructionsWithLetterI() {
    final var instructions = new RV32imIntegerRegisterImmediateInstructions();
    assertEquals(4, instructions.getInstructionSizeInBytes("addi"));
  }
}
