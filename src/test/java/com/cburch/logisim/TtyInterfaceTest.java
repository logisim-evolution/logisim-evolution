/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.fpga.download.Download;
import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.gui.start.TtyInterface;
import com.cburch.logisim.gui.start.Startup.Task;
import com.cburch.logisim.util.LocaleManager;

/** Tests command-line parsing. */
@ExtendWith(MockitoExtension.class)
public class TtyInterfaceTest extends TestBase {

  @Test
  public void testNotTty() {
    final Startup startup = new Startup(new String[0]);
    assertThrows(AssertionError.class, () -> new TtyInterface(startup));
  }

  @Test
  public void testOpenError() throws IllegalAccessException {
    final Startup startup = new Startup("--tty table missing-file".split(" "));
    assertNotNull(startup);
    final var tty = new TtyInterface(startup);

    LocaleManager S = mock(LocaleManager.class);
    when(S.get(anyString(),anyString())).thenAnswer(i -> i.getArguments()[0]+":"+i.getArguments()[1]);
    Logger logger = mock(Logger.class);
    FieldUtils.writeField(tty,"S",S,true);
    FieldUtils.writeField(tty,"logger",logger,true);

    Loader loader = new Loader(null);
    final int rc = tty.run(loader);
    
    assertEquals(2,rc);
    verify(logger).error(anyString(),ArgumentMatchers.eq("ttyLoadError:missing-file"));
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void testFPGAFail() {
    final Startup startup = new Startup("--test-fpga a b missing-file".split(" "));
    final var tty = new TtyInterface(startup);
    Loader loader = mock(Loader.class);
    final int rc = tty.run(loader);
    assertEquals(2,rc); // due to missing file
  }

  @Test
  public void testFPGASucceed() {
    final Startup startup = new Startup("--test-fpga a b missing-file".split(" "));
    final var tty = new TtyInterface(startup);
    
    Loader loader = mock(Loader.class);
    try (MockedConstruction<Download> download = mockConstruction(Download.class,
      (mock, context) -> {
        when(mock.runTty()).thenReturn(false);
      })) {
      final int rc = tty.run(loader);
      assertEquals(2,rc);
    }
  }

  @Test
  public void testTestVectorFail() {
    final Startup startup = new Startup("--test-vector a b missing-file".split(" "));
    final var tty = new TtyInterface(startup);
    Loader loader = mock(Loader.class);
    final int rc = tty.run(loader);
    assertEquals(2,rc); // due to missing file
  }

  @Test
  public void testNewFileFormatFail() {
    final Startup startup = new Startup("--new-file-format a missing-file".split(" "));
    final var tty = new TtyInterface(startup);
    Loader loader = mock(Loader.class);
    final int rc = tty.run(loader);
    assertEquals(2,rc); // due to missing file
  }

  @Test
  public void testTestCircuitFail() {
    final Startup startup = new Startup("--test-circuit missing-file".split(" "));
    assertTrue(startup.task.compareTo(Task.GUI) > 0);
    final var tty = new TtyInterface(startup);
    Loader loader = mock(Loader.class);
    final int rc = tty.run(loader);
    assertEquals(2,rc); // due to missing file
  }

  @Test
  public void testTtyFail() {
    final Startup startup = new Startup("--tty table missing-file".split(" "));
    final var tty = new TtyInterface(startup);
    Loader loader = mock(Loader.class);
    final int rc = tty.run(loader);
    assertEquals(2,rc); // due to missing file
  }

  // FIXME: verify logger/error messages for all of above

  // FIXME: add success cases for all of above, will be easier if code is refactored 
  // to avoid cumbersome constructor mocks

  // FIXME: add tests for --tty table --load/save
}
