/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.sim;

import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.hdl.VhdlEntityComponent;
import com.cburch.logisim.util.SocketClient;
import com.cburch.logisim.util.Softwares;
import com.cburch.logisim.vhdl.base.VhdlContent;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import com.cburch.logisim.vhdl.base.VhdlParser;
import com.cburch.logisim.vhdl.base.VhdlSimConstants;
import com.cburch.logisim.vhdl.base.VhdlSimConstants.State;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import javax.help.UnsupportedOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VHDL simulator allows Logisim to simulate the behavior of VHDL architectures. It delegate this
 * task to Questasim. Communication between Logisim and Questasim is done by a TCL socket
 * (TclBinder). Path to Questasim has to be specified in Logisim's preferences.
 *
 * @author christian.mueller@heig-vd.ch
 * @since 2.12.0
 */
public class VhdlSimulatorTop implements CircuitListener {

  private final VhdlSimulatorVhdlTop vhdlTop = new VhdlSimulatorVhdlTop();
  private final VhdlSimulatorTclComp tclRun = new VhdlSimulatorTclComp();
  private VhdlSimulatorTclBinder tclBinder;
  private final SocketClient socketClient = new SocketClient();

  public static final Logger logger = LoggerFactory.getLogger(VhdlSimulatorTop.class);

  private final Project project;

  private static final ArrayList<VhdlSimulatorListener> listeners =
      new ArrayList<>();

  private State state = State.DISABLED;

  public VhdlSimulatorTop(Project circuitState) {
    this.project = circuitState;
  }

  public void addVhdlSimStateListener(VhdlSimulatorListener l) {
    listeners.add(l);
  }

  /**
   * Circuit listener Each time the circuit changes, test if there is a VHDL component If yes, start
   * the VHDL simulator, and if not, stop it
   */
  @Override
  public void circuitChanged(CircuitEvent event) {

    if (hasVhdlComponent(getProject())) start();
    else stop();
  }

  /** Disable the simulator. If it is running, stops it first. */
  public void disable() {

    switch (state) {
      case RUNNING:
        stop();
        break;

      case ENABLED:
        break;

      case DISABLED:
        return;

      default:
        throw new UnsupportedOperationException(
            "Cannot disable VHDL simulator from " + state + " state");
    }

    setState(State.DISABLED);

    /* Hide and empty console log */
    if (getProject().getFrame() != null) {
      getProject().getFrame().setVhdlSimulatorConsoleStatusInvisible();
      getProject().getFrame().getVhdlSimulatorConsole().clear();
    }
  }

  /**
   * Enable the simulator. Here we also decide to start it directly as it is what the user generally
   * wants.
   */
  public void enable() {

    /*
     * Init binder with qsim, test qsim path and ask for a valid one if it
     * fails
     */
    if (tclBinder == null) {
      tclBinder = new VhdlSimulatorTclBinder(this);
    }

    switch (state) {
      case RUNNING:
        stop();
        break;

      case ENABLED:
        return;

      case DISABLED:
        break;

      default:
        throw new UnsupportedOperationException(
            "Cannot enable VHDL simulator from " + state + " state");
    }

    int i = 0;
    /* Wait in case starting simulation to early*/
    while (getProject().getFrame() == null) {
      try {
        Thread.sleep(100);
        if (i == 10) {
          break;
        }
        i++;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    if (i != 10) {
      getProject().getFrame().setVhdlSimulatorConsoleStatusVisible();
    }

    setState(State.ENABLED);

    start();
  }

  /* At least one of the VHDL entity changed */
  public void fireInvalidated() {

    // File dir = new File(SIM_SRC_PATH);
    // for(File file: dir.listFiles()) file.delete();

    // vhdlTop.fireInvalidated();
    // tclRun.fireInvalidated();
  }

  private void fireVhdlSimStateChanged() {
    for (VhdlSimulatorListener l : listeners) {
      l.stateChanged();
    }
  }

  public void generateFiles() {

    vhdlTop.fireInvalidated();
    tclRun.fireInvalidated();

    new File(VhdlSimConstants.SIM_PATH).mkdirs();
    new File(VhdlSimConstants.SIM_SRC_PATH).mkdirs();
    new File(VhdlSimConstants.SIM_COMP_PATH).mkdirs();

    try {
      Files.copy(
          this.getClass()
              .getResourceAsStream(VhdlSimConstants.SIM_RESOURCES_PATH + "questasim_binder.tcl"),
          Paths.get(VhdlSimConstants.SIM_PATH + "questasim_binder.tcl"),
          StandardCopyOption.REPLACE_EXISTING);
      Files.copy(
          this.getClass().getResourceAsStream(VhdlSimConstants.SIM_RESOURCES_PATH + "run.tcl"),
          Paths.get(VhdlSimConstants.SIM_PATH + "run.tcl"),
          StandardCopyOption.REPLACE_EXISTING);
      Softwares.copyQuestaModelsimConfig(Paths.get(VhdlSimConstants.SIM_COMP_PATH + "modelsim.ini"));
    } catch (IOException e) {
      VhdlSimulatorTop.logger.error("Cannot copy simulation files: {}", e.getMessage());
      e.printStackTrace();
    }

    final var vhdlComponents =
        VhdlSimConstants.getVhdlComponents(project.getCircuitState(), true);
    final var simulationComponents = configureSimulationComponents(vhdlComponents);
    final var projectSources =
        collectVhdlSources(project.getLogisimFile().getVhdlContents(), vhdlComponents);

    vhdlTop.generate(simulationComponents);

    final var sourceNames = new ArrayList<String>();
    for (final var source : projectSources) {
      saveProjectSource(source);
      sourceNames.add(source.getName());
    }

    /* Preserve the legacy embedded component path, which still uses a per-instance entity name. */
    for (final var comp : vhdlComponents) {
      if (comp.getFactory() instanceof VhdlEntityComponent entity) {
        entity.saveFile(comp.getAttributeSet());
        sourceNames.add(entity.getSimName(comp.getAttributeSet()));
      }
    }

    tclRun.generate(sourceNames);
  }

  static List<VhdlSimulatorComponent> configureSimulationComponents(
      List<Component> components) {
    final var result = new ArrayList<VhdlSimulatorComponent>(components.size());
    for (var index = 0; index < components.size(); index++) {
      final var component = components.get(index);
      final var attrs = component.getAttributeSet();
      final ComponentFactory factory = component.getFactory();

      if (factory instanceof VhdlEntity entity) {
        final var entityName = entity.getContent().getName();
        final var simulationName = createSimulationName(entityName, index);
        entity.setSimName(attrs, simulationName);
        result.add(
            new VhdlSimulatorComponent(entityName, simulationName, entity.getContent().getPorts()));
      } else if (factory instanceof VhdlEntityComponent entity) {
        entity.setSimName(attrs, VhdlSimConstants.VHDL_COMPONENT_SIM_NAME + index);
        final var simulationName = entity.getSimName(attrs);
        final var ports = new ArrayList<VhdlParser.PortDescription>();
        for (final var port : attrs.getValue(VhdlEntityComponent.CONTENT_ATTR).getPorts()) {
          ports.add(
              new VhdlParser.PortDescription(
                  port.getName(), port.getType(), port.getWidthInt()));
        }
        result.add(new VhdlSimulatorComponent(simulationName, simulationName, ports));
      }
    }
    return result;
  }

  static List<VhdlContent> collectVhdlSources(
      List<VhdlContent> projectSources, List<Component> placedComponents) {
    final var sources = new LinkedHashMap<String, VhdlContent>();
    for (final var source : projectSources) {
      sources.putIfAbsent(source.getName().toLowerCase(Locale.ROOT), source);
    }
    for (final var component : placedComponents) {
      if (component.getFactory() instanceof VhdlEntity entity) {
        final var source = entity.getContent();
        sources.putIfAbsent(source.getName().toLowerCase(Locale.ROOT), source);
      }
    }
    return List.copyOf(sources.values());
  }

  static String createSimulationName(String entityName, int index) {
    return VhdlSimConstants.VHDL_COMPONENT_SIM_NAME + entityName + "_" + index;
  }

  private static void saveProjectSource(VhdlContent source) {
    try (final var writer =
        new java.io.PrintWriter(
            VhdlSimConstants.SIM_SRC_PATH + source.getName() + ".vhdl",
            VhdlSimConstants.ENCODING)) {
      writer.print(source.getContent());
    } catch (IOException e) {
      logger.error("Could not create VHDL file: {}", e.getMessage());
    }
  }

  public Project getProject() {
    return project;
  }

  public SocketClient getSocketClient() {
    return socketClient;
  }

  public State getState() {
    return state;
  }

  private boolean hasVhdlComponent(CircuitState s) {

    /* Test current circuit */
    for (Component comp : s.getCircuit().getNonWires()) {
      if (comp.getFactory().getClass().equals(VhdlEntity.class)) {
        return true;
      }
      if (comp.getFactory().getClass().equals(VhdlEntityComponent.class)) return true;
    }

    /* Test sub-circuits */
    for (CircuitState sub : s.getSubstates()) {
      if (hasVhdlComponent(sub)) return true;
    }

    return false;
  }

  /**
   * Test if a project has a VHDL component
   *
   * @param p
   * @return
   */
  private boolean hasVhdlComponent(Project p) {
    return hasVhdlComponent(p.getCircuitState());
  }

  public boolean isEnabled() {
    return state != State.DISABLED;
  }

  public boolean isRunning() {
    return state == State.RUNNING;
  }

  /**
   * Receive a message from the VHDL simulator
   *
   * @return
   */
  public String receive() {
    if (!isRunning()) throw new UnsupportedOperationException();

    return socketClient.receive();
  }

  public void removeVhdlSimStateListener(VhdlSimulatorListener l) {
    listeners.remove(l);
  }

  public void reset() {
    if (isEnabled()) socketClient.send("restart");
  }

  /** Stop and restart. If not running, just start */
  public void restart() {
    switch (state) {
      case DISABLED, STARTING, ENABLED -> {
        start();
        return;
      }
      case RUNNING -> {
        stop();
        start();
      }
      default -> throw new UnsupportedOperationException("Cannot restart VHDL simulator from " + state + " state");
    }
  }

  /**
   * Send a message to the VHDL simulator
   *
   * @param message
   */
  public void send(String message) {
    if (!isRunning()) throw new UnsupportedOperationException();

    socketClient.send(message);
  }

  public void setEnabled(boolean enable) {
    if (enable) enable();
    else disable();
  }

  public void setState(State newState) {
    state = newState;
    fireVhdlSimStateChanged();
  }

  /** Start both TCL binder and socket server */
  public void start() {

    if (!hasVhdlComponent(getProject())) return;

    switch (state) {
      case DISABLED:
      case STARTING:
      case RUNNING:
        return;

      case ENABLED:
        setState(State.STARTING);

        generateFiles();
        tclBinder.start();
        break;

      default:
        throw new UnsupportedOperationException(
            "Cannot start VHDL simulator from " + state + " state");
    }
  }

  /** Stop the simulator, both TCL binder and socket server */
  public void stop() {

    switch (state) {
      case DISABLED:
      case ENABLED:
        return;

      case RUNNING:
        break;

      case STARTING:
        // FIXME : start a thread that waits for the simulator to finish
        // starting and then stop it
        break;

      default:
        throw new UnsupportedOperationException(
            "Cannot stop VHDL simulator from " + state + " state");
    }

    tclBinder.stop();
    socketClient.stop();

    setState(State.ENABLED);
  }

  public void tclStartCallback() {
    socketClient.start();
    setState(State.RUNNING);
  }
}
