/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.vhdl.base.VhdlContent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttrTableHdlModelTest {
  @Test
  void hdlAppearanceEditsAreUndoableProjectActions() throws Exception {
    final var project = mock(Project.class);
    final var hdl = VhdlContent.create("AttrTableHdlActionTest", null);
    final var model = new AttrTableHdlModel(project, hdl);

    model.setValueRequested(asObjectAttribute(StdAttr.APPEARANCE), StdAttr.APPEAR_CLASSIC);

    final var actionCaptor = ArgumentCaptor.forClass(Action.class);
    verify(project).doAction(actionCaptor.capture());

    final var action = actionCaptor.getValue();
    action.doIt(project);
    assertSame(StdAttr.APPEAR_CLASSIC, hdl.getAppearance());

    action.undo(project);
    assertSame(StdAttr.APPEAR_EVOLUTION, hdl.getAppearance());
  }

  @SuppressWarnings("unchecked")
  private static Attribute<Object> asObjectAttribute(Attribute<?> attr) {
    return (Attribute<Object>) attr;
  }
}
