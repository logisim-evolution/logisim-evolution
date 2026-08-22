/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.file;

import static com.cburch.logisim.fpga.Strings.S;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.util.LocaleManager;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class BoardReaderClassTest {

  @Test
  void boardFileErrorsUseCurrentLocale() {
    final var originalLocale = LocaleManager.getLocale();
    try {
      LocaleManager.setLocale(Locale.ENGLISH);
      assertEquals("Error", S.get("BoardReaderErrorTitle"));
      assertEquals(
          "The selected XML file does not contain a compression code table",
          S.get("BoardReaderMissingCodeTable"));
      assertEquals(
          "The selected XML file does not contain the picture dimensions",
          S.get("BoardReaderMissingPictureDimensions"));
      assertEquals(
          "The selected XML file does not contain the picture data",
          S.get("BoardReaderMissingPictureData"));
      assertEquals(
          "The selected XML file does not contain the required FPGA parameters",
          S.get("BoardReaderMissingFpgaParameters"));

      LocaleManager.setLocale(Locale.SIMPLIFIED_CHINESE);
      assertEquals("错误", S.get("BoardReaderErrorTitle"));
      assertEquals("所选 XML 文件不包含压缩编码表", S.get("BoardReaderMissingCodeTable"));
      assertEquals("所选 XML 文件不包含图片尺寸", S.get("BoardReaderMissingPictureDimensions"));
      assertEquals("所选 XML 文件不包含图片数据", S.get("BoardReaderMissingPictureData"));
      assertEquals(
          "所选 XML 文件不包含必需的 FPGA 参数", S.get("BoardReaderMissingFpgaParameters"));
    } finally {
      LocaleManager.setLocale(originalLocale);
    }
  }
}
