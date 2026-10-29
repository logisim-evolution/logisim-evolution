/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Splitter;
import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.SyntaxChecker;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AttrTableSelectionModelTest {
  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void updatesAttributeSetWhenSelectionChangesBetweenEmptyAndSelected() {
    final var project = mock(Project.class);
    final var frame = mock(Frame.class);
    final var canvas = mock(Canvas.class);
    final var selection = mock(Selection.class);
    final var circuit = mock(Circuit.class);
    final var circuitAttrs = attributeSet("circuit");
    final var selectionAttrs = attributeSet("selection");

    when(frame.getCanvas()).thenReturn(canvas);
    when(canvas.getCircuit()).thenReturn(circuit);
    when(canvas.getSelection()).thenReturn(selection);
    when(circuit.getStaticAttributes()).thenReturn(circuitAttrs);
    when(selection.getAttributeSet()).thenReturn(selectionAttrs);

    when(selection.isEmpty()).thenReturn(true);
    final var model = new AttrTableSelectionModel(project, frame);
    assertSame(circuitAttrs, model.getAttributeSet());

    when(selection.isEmpty()).thenReturn(false);
    model.updateAttributeSet();
    assertSame(selectionAttrs, model.getAttributeSet());

    when(selection.isEmpty()).thenReturn(true);
    model.updateAttributeSet();
    assertSame(circuitAttrs, model.getAttributeSet());
  }

  @Test
  void usesHdlAttributesWhenHdlEditorIsCurrent() {
    final var project = mock(Project.class);
    final var frame = mock(Frame.class);
    final var canvas = mock(Canvas.class);
    final var selection = mock(Selection.class);
    final var selectionAttrs = attributeSet("selection");
    final var hdl = VhdlContent.create("AttrTableHdlTest", null);

    when(frame.getCanvas()).thenReturn(canvas);
    when(canvas.getCircuit()).thenReturn(null);
    when(canvas.getCurrentHdl()).thenReturn(hdl);
    when(canvas.getSelection()).thenReturn(selection);
    when(selection.getAttributeSet()).thenReturn(selectionAttrs);

    final var model = new AttrTableSelectionModel(project, frame);

    assertSame(hdl.getStaticAttributes(), model.getAttributeSet());
  }

  @Test
  void recommittingNonAsciiLabelAfterSwitchToVerilogWarnsWithoutClearing() throws Exception {
    AppPreferences.HdlType.set(HdlGeneratorFactory.NONE);
    final var fixture = new Fixture();
    final var output = pinWithLabel("schön", 0);
    add(fixture.circuit, output);
    AppPreferences.HdlType.set(HdlGeneratorFactory.VERILOG);
    final var model = modelFor(fixture.project, fixture.circuit, output);
    final var expectedMessage =
        SyntaxChecker.getErrorMessage("schön", HdlGeneratorFactory.VERILOG)
            + "\n"
            + com.cburch.logisim.util.Strings.S.get("variableNameNotAcceptable");

    try (MockedStatic<OptionPane> dialogs = mockStatic(OptionPane.class)) {
      model.setValueRequested(labelAttribute(), "schön");

      dialogs.verify(() -> OptionPane.showMessageDialog(null, expectedMessage));
    }
    assertEquals("schön", output.getAttributeSet().getValue(StdAttr.LABEL));
  }

  @Test
  void recommittingCaseOnlyDuplicateAfterSwitchToVhdlWarnsWithoutClearing() throws Exception {
    AppPreferences.HdlType.set(HdlGeneratorFactory.NONE);
    final var fixture = new Fixture();
    final var firstInput = pinWithLabel("A", 0);
    final var secondInput = pinWithLabel("a", 40);
    add(fixture.circuit, firstInput);
    add(fixture.circuit, secondInput);
    AppPreferences.HdlType.set(HdlGeneratorFactory.VHDL);
    final var model = modelFor(fixture.project, fixture.circuit, firstInput);
    final var expectedMessage =
        "\"A\" : " + com.cburch.logisim.circuit.Strings.S.get("UsedLabelNameError");

    try (MockedStatic<OptionPane> dialogs = mockStatic(OptionPane.class)) {
      model.setValueRequested(labelAttribute(), "A");

      dialogs.verify(() -> OptionPane.showMessageDialog(null, expectedMessage));
    }
    assertEquals("A", firstInput.getAttributeSet().getValue(StdAttr.LABEL));
    assertEquals("a", secondInput.getAttributeSet().getValue(StdAttr.LABEL));
  }

  @Test
  void distributionCommandTracksSingleSelectionAndRejectsStaleEditor() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      final var fixture = new Fixture();
      final var attrs = SplitterFactory.instance.createAttributeSet();
      attrs.setValue(SplitterAttributes.ATTR_WIDTH,
          BitWidth.create(8));
      attrs.setValue(SplitterAttributes.ATTR_FANOUT, 3);
      final var splitter = new Splitter(
          Location.create(0, 0, true), attrs);
      add(fixture.circuit, splitter);
      final var frame = mock(Frame.class);
      final var canvas = mock(Canvas.class);
      final var selection = mock(Selection.class);
      when(frame.getCanvas()).thenReturn(canvas);
      when(frame.getEditorView()).thenReturn(Frame.EDIT_LAYOUT);
      when(canvas.getCircuit()).thenReturn(fixture.circuit);
      when(canvas.getSelection()).thenReturn(selection);
      when(selection.getAttributeSet()).thenReturn(attrs);
      when(selection.getComponents()).thenReturn(Set.of(splitter));
      final var model = new AttrTableSelectionModel(fixture.project, frame);
      final var count = model.getRowCount();
      final var row = model.getRow(count - 1);
      assertTrue(row instanceof SplitterDistributionRow);
      final var button = (JButton) row.getEditor(null);
      final var pin = pinWithLabel("", 40);
      for (final var components : List.of(Set.<Component>of(),
          Set.of(pin), Set.<Component>of(splitter, pin),
          Set.<Component>of(splitter, new Splitter(
              Location.create(80, 0, true), (AttributeSet) attrs.clone())))) {
        when(selection.getComponents()).thenReturn(components);
        model.selectionChanged(null);
        assertEquals(count - 1, model.getRowCount());
        assertFalse(row.isValueEditable());
        button.doClick();
        assertTrue(splitter.distributionChanges(1));
      }
    });
  }

  private static AttributeSet attributeSet(String label) {
    return AttributeSets.fixedSet(new Attribute<?>[] {StdAttr.LABEL}, new Object[] {label});
  }

  @SuppressWarnings("unchecked")
  private static Attribute<Object> labelAttribute() {
    return (Attribute<Object>) (Attribute<?>) StdAttr.LABEL;
  }

  private static AttrTableSelectionModel modelFor(
      Project project, Circuit circuit, Component component) {
    final var frame = mock(Frame.class);
    final var canvas = mock(Canvas.class);
    final var selection = mock(Selection.class);

    when(frame.getCanvas()).thenReturn(canvas);
    when(canvas.getCircuit()).thenReturn(circuit);
    when(canvas.getSelection()).thenReturn(selection);
    when(selection.isEmpty()).thenReturn(false);
    when(selection.getAttributeSet()).thenReturn(component.getAttributeSet());
    when(selection.getComponents()).thenReturn(Set.of(component));

    return new AttrTableSelectionModel(project, frame);
  }

  private static Component pinWithLabel(String label, int x) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, label);
    return Pin.FACTORY.createComponent(Location.create(x, 0, true), attrs);
  }

  private static void add(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
  }

  private static final class Fixture {
    private final Project project;
    private final Circuit circuit;

    private Fixture() {
      final var file = LogisimFile.createNew(new Loader(null), null);
      project = new Project(file);
      circuit = file.getMainCircuit();
      circuit.setProject(project);
      project.setCurrentCircuit(circuit);
    }
  }
}
