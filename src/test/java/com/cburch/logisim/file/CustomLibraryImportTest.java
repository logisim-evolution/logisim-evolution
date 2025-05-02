/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.Main;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class CustomLibraryImportTest {
  private static final String VALID_LOGISIM_LIBRARY_XML = """
          <?xml version="1.0" encoding="UTF-8" standalone="no"?><project source="4.0.0dev" version="1.0">
          This file is intended to be loaded by Logisim-evolution v4.0.0dev(https://github.com/logisim-evolution/).
          
          <lib desc="#Wiring" name="0"><tool name="Pin"><a name="appearance" val="classic"/></tool></lib><lib desc="#Gates" name="1"/><lib desc="#Plexers" name="2"/><lib desc="#Arithmetic" name="3"/><lib desc="#Memory" name="4"/><lib desc="#I/O" name="5"/><lib desc="#TTL" name="6"/><lib desc="#TCL" name="7"/><lib desc="#Base" name="8"/><lib desc="#BFH-Praktika" name="9"/><lib desc="#Input/Output-Extra" name="10"/><lib desc="#Soc" name="11"/><main name="FourAdder"/><options><a name="gateUndefined" val="ignore"/><a name="simlimit" val="1000"/><a name="simrand" val="0"/></options><mappings><tool lib="8" map="Button2" name="Poke Tool"/><tool lib="8" map="Button3" name="Menu Tool"/><tool lib="8" map="Ctrl Button1" name="Menu Tool"/></mappings><toolbar><tool lib="8" name="Poke Tool"/><tool lib="8" name="Edit Tool"/><tool lib="8" name="Wiring Tool"/><tool lib="8" name="Text Tool"/><sep/><tool lib="0" name="Pin"/><tool lib="0" name="Pin"><a name="facing" val="west"/><a name="output" val="true"/></tool><sep/><tool lib="1" name="NOT Gate"/><tool lib="1" name="AND Gate"/><tool lib="1" name="OR Gate"/><tool lib="1" name="XOR Gate"/><tool lib="1" name="NAND Gate"/><tool lib="1" name="NOR Gate"/><sep/><tool lib="4" name="D Flip-Flop"/><tool lib="4" name="Register"/></toolbar><circuit name="FourAdder"><a name="appearance" val="logisim_evolution"/><a name="circuit" val="FourAdder"/><a name="circuitnamedboxfixedsize" val="true"/><a name="simulationFrequency" val="1.0"/><comp lib="0" loc="(110,140)" name="Pin"><a name="appearance" val="NewPins"/></comp><comp lib="0" loc="(110,180)" name="Pin"><a name="appearance" val="NewPins"/></comp><comp lib="0" loc="(110,210)" name="Pin"><a name="appearance" val="NewPins"/></comp><comp lib="0" loc="(110,250)" name="Pin"><a name="appearance" val="NewPins"/></comp><comp lib="0" loc="(290,190)" name="Pin"><a name="appearance" val="NewPins"/><a name="facing" val="west"/><a name="output" val="true"/></comp><comp lib="1" loc="(190,160)" name="AND Gate"/><comp lib="1" loc="(190,230)" name="AND Gate"/><comp lib="1" loc="(260,190)" name="AND Gate"/><wire from="(110,140)" to="(140,140)"/><wire from="(110,180)" to="(140,180)"/><wire from="(110,210)" to="(140,210)"/><wire from="(110,250)" to="(140,250)"/><wire from="(190,160)" to="(210,160)"/><wire from="(190,230)" to="(210,230)"/><wire from="(210,160)" to="(210,170)"/><wire from="(210,210)" to="(210,230)"/><wire from="(260,190)" to="(290,190)"/></circuit></project>
          """;

  private static final String RANDOM_FILE_DATA = "Lorem Ipsum";

  private static class TestFile {
    public String contents;
    public String fileName;

    public TestFile(String contents, String fileName) {
      this.contents = contents;
      this.fileName = fileName;
    }
  }


  /**
   * Test method for {@link com.cburch.logisim.file.Loader#loadCustomStartupLibraries(String)}
  */
  @Test
  public final void testLoadCustomStartupLibraries() {
    // Make the program run headless
    Main.headless = true;

    // Create a loader to test loading files with
    var testLoader = new Loader(null);

    // Test for an invalid file type
    TestFile[] files = new TestFile[]{new TestFile(RANDOM_FILE_DATA, "not_a_logisim_file.txt")};
    String testDirectoryPath = generateTestDefaultLibrary(files);
    var loadedLibraries = testLoader.loadCustomStartupLibraries(testDirectoryPath);
    assertEquals(0, loadedLibraries.length);

    // Test for if the directory does not exist
    String fakeDirectory = findFakeDirectory();
    loadedLibraries = testLoader.loadCustomStartupLibraries(fakeDirectory);
    assertEquals(0, loadedLibraries.length);

    // Test for if the directory contains no files
    files = new TestFile[0];
    testDirectoryPath = generateTestDefaultLibrary(files);
    loadedLibraries = testLoader.loadCustomStartupLibraries(testDirectoryPath);
    assertEquals(0, loadedLibraries.length);

    // Test if the file given is invalid
    files = new TestFile[]{new TestFile(RANDOM_FILE_DATA, "bad_file.circ")};
    testDirectoryPath = generateTestDefaultLibrary(files);
    loadedLibraries = testLoader.loadCustomStartupLibraries(testDirectoryPath);
    assertEquals(0, loadedLibraries.length);

    // Test if the system works correctly under normal circumstances
    files = new TestFile[]{new TestFile(VALID_LOGISIM_LIBRARY_XML, "good_file.circ")};
    testDirectoryPath = generateTestDefaultLibrary(files);
    loadedLibraries = testLoader.loadCustomStartupLibraries(testDirectoryPath);
    assertEquals(1, loadedLibraries.length);
  }

  private String findFakeDirectory() {
    String directoryPath;
    while (true) {
      directoryPath = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID();
      File directory = new File(directoryPath);
      if (!directory.exists()) break;
    }
    return directoryPath;
  }

  private String generateTestDefaultLibrary(TestFile[] files) {
    String directoryPath;
    while (true) {
      directoryPath = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID();
      File directory = new File(directoryPath);
      if (directory.mkdir()) break;
    }

    for (TestFile file : files) {
      String fileName = directoryPath + File.separator + file.fileName;
      File newDirectoryFile = new File(fileName);
      try {
        boolean fileCreatedSuccess = newDirectoryFile.createNewFile();
        if (!fileCreatedSuccess) throw new IOException("Unable to create new directory");

        FileWriter fileWriter = new FileWriter(fileName);
        fileWriter.write(file.contents);
        fileWriter.close();

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return directoryPath;
  }
}
