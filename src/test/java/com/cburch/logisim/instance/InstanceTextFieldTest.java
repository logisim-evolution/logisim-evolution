/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.std.io.Led;
import com.cburch.logisim.tools.TextEditable;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import org.junit.jupiter.api.Test;

public class InstanceTextFieldTest {

  @Test
  public void changedTextCommitCreatesAction() {
    assertNotNull(newTextEditable().getCommitAction(null, "old", "new"));
  }

  @Test
  public void unchangedTextCommitCreatesNoAction() {
    assertNull(newTextEditable().getCommitAction(null, "same", "same"));
  }

  @Test
  public void committedTextChangeCanBeUndoneByProjectUndoManager() {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();
    final var attrs = Text.FACTORY.createAttributeSet();
    attrs.setValue(Text.ATTR_TEXT, "old");
    final var comp = Text.FACTORY.createComponent(Location.create(0, 0, false), attrs);
    final var addComponent = new CircuitMutation(circuit);
    addComponent.add(comp);
    addComponent.execute();
    final var editable = (TextEditable) comp.getFeature(TextEditable.class);

    final var action = editable.getCommitAction(circuit, "old", "new");

    assertNotNull(action);
    project.doAction(action);
    assertEquals("new", comp.getAttributeSet().getValue(Text.ATTR_TEXT));
    project.undoAction();
    assertEquals("old", comp.getAttributeSet().getValue(Text.ATTR_TEXT));
  }

  @Test
  public void standaloneTextEnablesMultilineEditing() {
    final var attrs = Text.FACTORY.createAttributeSet();
    final var comp = Text.FACTORY.createComponent(Location.create(0, 0, false), attrs);

    final var textField = (InstanceTextField) comp.getFeature(TextEditable.class);

    assertNotNull(textField);
    assertTrue(textField.isMultiline());
  }

  @Test
  public void componentLabelKeepsSingleLineEditing() {
    final var led = new Led();
    final var comp =
        led.createComponent(Location.create(100, 100, false), led.createAttributeSet());

    final var textField = (InstanceTextField) comp.getFeature(TextEditable.class);

    assertNotNull(textField);
    assertFalse(textField.isMultiline());
  }

  @Test
  public void componentCreationHonorsConfiguredLabelColor() {
    final var led = new Led();
    final var attrs = led.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, "label");
    attrs.setValue(StdAttr.LABEL_COLOR, Color.RED);
    final var comp = led.createComponent(Location.create(100, 100, false), attrs);
    final var textField = (InstanceTextField) comp.getFeature(TextEditable.class);
    final var sourceGraphics = mock(Graphics.class);
    final var labelGraphics = mock(Graphics.class);
    final var context = mock(ComponentDrawContext.class);

    assertNotNull(textField);
    when(sourceGraphics.create()).thenReturn(labelGraphics);
    when(labelGraphics.getFontMetrics()).thenReturn(mock(FontMetrics.class));
    when(context.getGraphics()).thenReturn(sourceGraphics);
    textField.draw(comp, context);

    verify(labelGraphics).setColor(Color.RED);
  }

  @Test
  public void componentWithoutLabelColorTracksThemeChanges() {
    final var originalLookAndFeel = AppPreferences.LookAndFeel.get();
    final var comp = mock(InstanceComponent.class);
    final var attrs = mock(AttributeSet.class);
    final var textField = new InstanceTextField(comp);

    when(comp.getAttributeSet()).thenReturn(attrs);
    when(attrs.containsAttribute(StdAttr.LABEL_COLOR)).thenReturn(false);
    when(attrs.containsAttribute(StdAttr.LABEL_VISIBILITY)).thenReturn(false);
    when(attrs.getValue(StdAttr.LABEL)).thenReturn("label");
    when(attrs.getValue(StdAttr.LABEL_FONT)).thenReturn(StdAttr.DEFAULT_LABEL_FONT);
    textField.update(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        0,
        0,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_CENTER);

    try {
      AppPreferences.LookAndFeel.set("TestLight");
      final var lightGraphics = draw(textField, comp);
      verify(lightGraphics).setColor(Color.BLUE);

      AppPreferences.LookAndFeel.set("TestDark");
      final var darkGraphics = draw(textField, comp);
      verify(darkGraphics).setColor(new Color(0x6C, 0xB6, 0xFF));
    } finally {
      AppPreferences.LookAndFeel.set(originalLookAndFeel);
    }
  }

  private static Graphics draw(InstanceTextField textField, InstanceComponent comp) {
    final var sourceGraphics = mock(Graphics.class);
    final var labelGraphics = mock(Graphics.class);
    final var context = mock(ComponentDrawContext.class);

    when(sourceGraphics.create()).thenReturn(labelGraphics);
    when(labelGraphics.getFontMetrics()).thenReturn(mock(FontMetrics.class));
    when(context.getGraphics()).thenReturn(sourceGraphics);
    textField.draw(comp, context);
    return labelGraphics;
  }

  private static TextEditable newTextEditable() {
    final var attrs = Text.FACTORY.createAttributeSet();
    final var comp = Text.FACTORY.createComponent(Location.create(0, 0, false), attrs);
    final var editable = (TextEditable) comp.getFeature(TextEditable.class);

    assertNotNull(editable);
    return editable;
  }
}
