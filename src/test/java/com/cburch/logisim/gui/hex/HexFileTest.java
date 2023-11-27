package com.cburch.logisim.gui.hex;

import com.cburch.logisim.std.memory.MemContents;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HexFileTest {

  @TempDir File tempDir;

  /**
   * Tests to see if the values matches the contents of the memory saved in savedFile. If there is a
   * mismatch, a junit assertion failure occurs.
   *
   * @param autodetect allows autodetect if true
   * @param desc the file type description
   * @param savedFile the file containing saved memory to be read
   * @param addressSize the size of the address
   * @param wordSize the size of a word of memory
   * @param values a HashMap from address to expected value (0 values may be omitted)
   * @throws IOException
   */
  private void compare(
      boolean autodetect,
      String desc,
      File savedFile,
      int addressSize,
      int wordSize,
      HashMap<Long, Long> values)
      throws IOException {
    final var memory = MemContents.create(addressSize, wordSize, true);
    if (desc.startsWith("Binary") || desc.startsWith("ASCII") || !autodetect) {
      // these can't be auto-detected
      if (!HexFile.open(memory, savedFile, desc)) {
        throw new IOException("Failed to load: " + savedFile.toString());
      }
    } else {
      // auto-detect should figure out the correct format
      if (!HexFile.open(memory, savedFile)) {
        throw new IOException("Failed to load: " + savedFile.toString());
      }
    }

    final var memEnd = memory.getLastOffset();
    for (long address = 0; address < memEnd; address++) {
      assertEquals(values.getOrDefault(address, 0L), memory.get(address));
    }
  }

  /**
   * returns an array of triples of [description index, address size, and word size] which should be
   * tested
   */
  public static int[][] formatTriples() {
    final var max = HexFile.formatDescriptions.length;
    final int[] aSizes = {1, 11};
    final int[] wSizes = {1, 8, 61, 64};
    final var triples = new int[max * aSizes.length * wSizes.length][3];
    var tIndex = 0;
    for (var index = 0; index < max; index++) {
      for (final var aSize : aSizes) {
        for (final var wSize : wSizes) {
          triples[tIndex] = new int[] {index, aSize, wSize};
          ++tIndex;
        }
      }
    }
    return triples;
  }

  /** Test method for {@link com.cburch.logisim.gui.hex.HexFile} */
  @ParameterizedTest
  @MethodSource(value = "formatTriples")
  public final void testSaveLoadMemoryContents(int[] triple)
      throws IOException, InterruptedException {
    final var index = triple[0];
    final var addressSize = triple[1];
    final var wordSize = triple[2];

    // check that the triple makes sense
    assertTrue(addressSize > 0);
    assertTrue(wordSize > 0);
    assertTrue(wordSize <= 64);
    assertTrue(index >= 0);
    assertTrue(index < HexFile.formatDescriptions.length);

    final var rng = new Random((long) index * addressSize * wordSize + 1);

    final var memoryContents = MemContents.create(addressSize, wordSize, false);

    final var values = new HashMap<Long, Long>();
    // (1L << size) doesn't work if size is 64
    final var addressMask = (2L << (addressSize - 1)) - 1L;
    final var wordMask = (2L << (wordSize - 1)) - 1L;
    final var count = addressMask / 2 + 1;
    for (var i = 0; i < count; i++) {
      final var a = rng.nextLong() & addressMask;
      final var v = rng.nextLong() & wordMask;
      values.put(a, v);
      memoryContents.set(a, v);
    }
    final var desc = HexFile.formatDescriptions[index];
    final var tempFile = new File(tempDir, "hexfile-" + index + ".dat");
    HexFile.save(tempFile, memoryContents, desc);

    compare(true, desc, tempFile, addressSize, wordSize, values);

    //check whether the OS is XXD compatible(MacOs or linux).
    String os = System.getProperty("os.name");
    if(os.toLowerCase().contains("windows")) return;

    if (desc.startsWith("Binary")) {
      final var endian = desc.endsWith("big-endian") ? "big-endian" : "little-endian";

      final var otherFile = new File(tempFile + ".xxd");
      final String[] cmd1 = {"xxd", tempFile.toString(), otherFile.toString()};
      Runtime.getRuntime().exec(cmd1).waitFor();
      compare(
          false, "v3.0 hex bytes addressed " + endian, otherFile, addressSize, wordSize, values);

      final var plainFile = new File(tempFile + ".xxd-plain");
      final String[] cmd2 = {"xxd", "-p", tempFile.toString(), plainFile.toString()};
      Runtime.getRuntime().exec(cmd2).waitFor();
      compare(false, "v3.0 hex bytes plain " + endian, plainFile, addressSize, wordSize, values);
    }
  }
}
