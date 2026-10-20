/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.draw.actions.ModelTranslateAction;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.circuit.appear.AppearanceElement;
import com.cburch.logisim.circuit.appear.AppearancePort;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.WiringLibrary;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AppearancePinPersistenceTest {
  @ParameterizedTest
  @CsvSource({
    "classic, false, pin",
    "evolution, false, pin",
    "logisim_evolution, false, pin",
    "classic, true, pin",
    "evolution, true, pin",
    "logisim_evolution, true, pin",
    "classic, false, anchor",
    "evolution, false, anchor",
    "logisim_evolution, false, anchor",
    "classic, true, anchor",
    "evolution, true, anchor",
    "logisim_evolution, true, anchor"
  })
  void movingPinOrAnchorAutomaticallySelectsCustomAndSurvivesReload(
      String mode, boolean instantiated, String target)
      throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          try {
            final var loader = new Loader(null);
            final var file = LogisimFile.createNew(loader, null);
            file.addLibrary(loader.getBuiltin().getLibrary(WiringLibrary._ID));
            final var project = new Project(file);
            final var child = new Circuit("child", file, project);
            file.addCircuit(child);
            child.getStaticAttributes().setValue(
                CircuitAttributes.APPEARANCE_ATTR, CircuitAttributes.APPEARANCE_ATTR.parse(mode));
            final var pinAttributes = Pin.FACTORY.createAttributeSet();
            pinAttributes.setValue(StdAttr.LABEL, "A");
            final var pin =
                Pin.FACTORY.createComponent(Location.create(100, 100, true), pinAttributes);
            final var addPin = new CircuitMutation(child);
            addPin.add(pin);
            addPin.execute();
            if (instantiated) {
              final var factory = child.getSubcircuitFactory();
              final var addInstance = new CircuitMutation(file.getMainCircuit());
              addInstance.add(factory.createComponent(
                  Location.create(200, 200, true), factory.createAttributeSet()));
              addInstance.execute();
            }
            project.setCurrentCircuit(child);
            final var view = new AppearanceView();
            view.setCircuit(project, project.getCircuitState());
            final var element = appearanceElement(child, target);
            final var expected = element.getLocation().translate(70, 130);

            ((AppearanceCanvas) view.getCanvas()).doAction(
                new ModelTranslateAction(
                    child.getAppearance().getCustomAppearanceDrawing(), List.of(element), 70, 130));

            assertEquals(CircuitAttributes.APPEAR_CUSTOM,
                child.getStaticAttributes().getValue(CircuitAttributes.APPEARANCE_ATTR));
            assertEquals(expected, element.getLocation());
            assertTrue(file.isDirty());
            final var output = new ByteArrayOutputStream();
            file.write(output, loader);
            final var reloaded = LogisimFile.load(
                new ByteArrayInputStream(output.toByteArray()), new Loader(null));
            final var loadedChild = reloaded.getCircuit("child");
            assertEquals(CircuitAttributes.APPEAR_CUSTOM,
                loadedChild.getStaticAttributes().getValue(CircuitAttributes.APPEARANCE_ATTR));
            assertEquals(expected, appearanceElement(loadedChild, target).getLocation());
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        });
  }

  private static AppearanceElement appearanceElement(Circuit circuit, String target) {
    if ("anchor".equals(target)) {
      return circuit.getAppearance().getCustomObjectsFromBottom().stream()
          .filter(AppearanceAnchor.class::isInstance)
          .map(AppearanceAnchor.class::cast)
          .findFirst().orElseThrow();
    }
    return circuit.getAppearance().getCustomObjectsFromBottom().stream()
        .filter(AppearancePort.class::isInstance)
        .map(AppearancePort.class::cast)
        .filter(port -> "A".equals(port.getPin().getAttributeValue(StdAttr.LABEL)))
        .findFirst().orElseThrow();
  }
}
