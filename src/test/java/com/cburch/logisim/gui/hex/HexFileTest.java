package com.cburch.logisim.gui.hex;

import com.cburch.logisim.Main;
import com.cburch.logisim.std.memory.MemContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HexFileTest {
  private static int addressSize;
  private static int wordSize;
  private static MemContents memoryContents;
  private static HashMap<Long, Long> vals;

  private void compare(
      boolean autodetect,
      String desc,
      File tmp,
      int addressSize,
      int wordSize,
      HashMap<Long, Long> vals)
      throws IOException {
    final var dst = MemContents.create(addressSize, wordSize);
    if (desc.startsWith("Binary") || desc.startsWith("ASCII") || !autodetect) {
      // these can't be auto-detected
      if (!HexFile.open(dst, tmp, desc)) {
        throw new IOException("Failed to load: " + tmp.toString());
      }
    } else {
      // auto-detect should figure out the correct format
      if (!HexFile.open(dst, tmp)) {
        throw new IOException("Failed to load: " + tmp.toString());
      }
    }

    final var memEnd = dst.getLastOffset();
    for (long address = 0; address < memEnd; address++) {
      assertEquals(vals.getOrDefault(address, 0L), dst.get(address));
    }
  }

  @BeforeAll
  public static void setUp() {
    Main.headless = true;
    var rng = new Random(1234L);
    addressSize = rng.nextInt(14) + 1;
    wordSize = rng.nextInt(64) + 1;

    memoryContents = MemContents.create(addressSize, wordSize);

    vals = new HashMap<>();
    final var count = rng.nextInt(1 << addressSize);
    final var mask = (1L << wordSize) - 1;
    for (var i = 0; i < count; i++) {
      final long a = rng.nextInt(1 << addressSize);
      final var v = (rng.nextLong() & mask);
      vals.put(a, v);
      memoryContents.set(a, v);
    }
  }

  public static int[] formatIndexes() {
    final var max = HexFile.formatDescriptions.length;
    final var indexes = new int[max];
    for (var index = 0; index < max; index++) {
      indexes[index] = index;
    }
    return indexes;
  }

  /**
   * Test method for {@link
   * com.cburch.logisim.gui.hex.HexFile}
   */
  @ParameterizedTest
  @MethodSource(value = "formatIndexes")
  public final void testSaveLoadMemoryContents(int index) throws IOException, InterruptedException {
    final var desc = HexFile.formatDescriptions[index];
    final var tmp = File.createTempFile("hexfile-" + index + "-", ".dat");
    HexFile.save(tmp, memoryContents, desc);

    compare(true, desc, tmp, addressSize, wordSize, vals);

    if (desc.startsWith("Binary")) {
      final var endian = desc.endsWith("big-endian") ? "big-endian" : "little-endian";

      final var other = new File(tmp + ".xxd");
      final String[] cmd1 = {"xxd", tmp.toString(), other.toString()};
      Runtime.getRuntime().exec(cmd1).waitFor();
      compare(false, "v3.0 hex bytes addressed " + endian, other, addressSize, wordSize, vals);

      final var plain = new File(tmp + ".xxd-plain");
      final String[] cmd2 = {"xxd", "-p", tmp.toString(), plain.toString()};
      Runtime.getRuntime().exec(cmd2).waitFor();
      compare(false, "v3.0 hex bytes plain " + endian, plain, addressSize, wordSize, vals);
    }
  }
}
