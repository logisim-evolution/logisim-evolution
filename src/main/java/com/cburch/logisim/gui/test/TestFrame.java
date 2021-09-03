/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.test;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.TestException;
import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenuItemManager;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TestFrame extends LFrame.SubWindowWithSimulation {

  private static final long serialVersionUID = 1L;
  private final Map<Circuit, Model> modelMap = new HashMap<>();
  private final MyListener myListener = new MyListener();
  private final WindowMenuManager windowManager;
  private final JFileChooser chooser = new JFileChooser();
  private final TestPanel panel;
  private final JButton load = new JButton();
  private final JButton run = new JButton();
  private final JButton stop = new JButton();
  private final JButton reset = new JButton();
  private final JButton close = new JButton();
  private final JLabel pass = new JLabel();
  private final JLabel fail = new JLabel();
  private Simulator curSimulator = null;
  private Model curModel;
  private int finished;
  private int count;
  private File curFile;

  public TestFrame(Project project) {
    super(project);
    this.windowManager = new WindowMenuManager();
    project.addProjectListener(myListener);
    setSimulator(project.getSimulator(), project.getCircuitState().getCircuit());

    chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
    chooser.addChoosableFileFilter(TestVector.FILE_FILTER);
    chooser.setFileFilter(TestVector.FILE_FILTER);

    panel = new TestPanel(this);

    JPanel statusPanel = new JPanel();
    statusPanel.add(pass);
    statusPanel.add(fail);

    JPanel buttonPanel = new JPanel();
    buttonPanel.add(load);
    buttonPanel.add(run);
    buttonPanel.add(stop);
    buttonPanel.add(reset);
    buttonPanel.add(close);
    load.addActionListener(myListener);
    run.addActionListener(myListener);
    stop.addActionListener(myListener);
    reset.addActionListener(myListener);
    close.addActionListener(myListener);

    run.setEnabled(false);
    stop.setEnabled(false);
    reset.setEnabled(false);

    Container contents = getContentPane();
    panel.setPreferredSize(new Dimension(450, 300));
    contents.add(statusPanel, BorderLayout.NORTH);
    contents.add(panel, BorderLayout.CENTER);
    contents.add(buttonPanel, BorderLayout.SOUTH);

    LocaleManager.addLocaleListener(myListener);
    myListener.localeChanged();
    pack();
  }

  private static String computeTitle(Model data, Project proj) {
    var name = data == null ? "???" : data.getCircuit().getName();
    return S.get("testFrameTitle", name, proj.getLogisimFile().getDisplayName());
  }

  Model getModel() {
    return curModel;
  }

  private void setSimulator(Simulator value, Circuit circuit) {
    if ((value == null) == (curModel == null)) {
      if (value == null || value.getCircuitState().getCircuit() == curModel.getCircuit()) return;
    }

    menubar.setCircuitState(value, value.getCircuitState());

    if (curSimulator != null) curSimulator.removeSimulatorListener(myListener);
    if (curModel != null) curModel.setSelected(false);
    if (curModel != null) curModel.removeModelListener(myListener);

    Model oldModel = curModel;
    Model data = null;
    if (value != null) {
      data = modelMap.get(value.getCircuitState().getCircuit());
      if (data == null) {
        data = new Model(project, value.getCircuitState().getCircuit());
        modelMap.put(data.getCircuit(), data);
      }
    }
    curSimulator = value;
    curModel = data;

    if (curSimulator != null) curSimulator.addSimulatorListener(myListener);
    if (curModel != null) curModel.setSelected(true);
    if (curModel != null) curModel.addModelListener(myListener);
    setTitle(computeTitle(curModel, project));
    if (panel != null) panel.modelChanged(oldModel, curModel);
  }

  @Override
  public void setVisible(boolean value) {
    if (value) windowManager.frameOpened(this);
    super.setVisible(value);
  }

  private class MyListener
      implements ActionListener,
          ProjectListener,
          Simulator.Listener,
          LocaleListener,
          ModelListener {

    @Override
    public void actionPerformed(ActionEvent event) {
      Object src = event.getSource();
      if (src == close) {
        requestClose();
      } else if (src == load) {
        int result = chooser.showOpenDialog(TestFrame.this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.exists() || !file.canRead() || file.isDirectory()) {
          OptionPane.showMessageDialog(
              TestFrame.this,
              S.get("fileCannotReadMessage", file.getName()),
              S.get("fileCannotReadTitle"),
              OptionPane.OK_OPTION);
          return;
        }
        try {
          TestVector vec = new TestVector(file);
          finished = 0;
          count = vec.data.size();
          getModel().setVector(vec);
          curFile = file;
          getModel().setPaused(true);
          getModel().start();
        } catch (IOException e) {
          OptionPane.showMessageDialog(
              TestFrame.this,
              S.get("fileCannotParseMessage", file.getName(), e.getMessage()),
              S.get("fileCannotReadTitle"),
              OptionPane.OK_OPTION);
        } catch (TestException e) {
          OptionPane.showMessageDialog(
              TestFrame.this,
              S.get("fileWrongPinsMessage", file.getName(), e.getMessage()),
              S.get("fileWrongPinsTitle"),
              OptionPane.OK_OPTION);
        }
      } else if (src == run) {
        try {
          getModel().start();
        } catch (TestException e) {
          OptionPane.showMessageDialog(
              TestFrame.this,
              S.get("fileWrongPinsMessage", curFile.getName(), e.getMessage()),
              S.get("fileWrongPinsTitle"),
              OptionPane.OK_OPTION);
        }
      } else if (src == stop) {
        getModel().setPaused(true);
      } else if (src == reset) {
        getModel().clearResults();
        testingChanged();
      }
    }

    @Override
    public void localeChanged() {
      setTitle(computeTitle(curModel, project));
      panel.localeChanged();
      load.setText(S.get("loadButton"));
      run.setText(S.get("runButton"));
      stop.setText(S.get("stopButton"));
      reset.setText(S.get("resetButton"));
      close.setText(S.get("closeButton"));
      myListener.testResultsChanged(getModel().getPass(), getModel().getFail());
      windowManager.localeChanged();
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      int action = event.getAction();
      if (action == ProjectEvent.ACTION_SET_STATE) {
        setSimulator(
            event.getProject().getSimulator(), event.getProject().getCircuitState().getCircuit());
      } else if (action == ProjectEvent.ACTION_SET_FILE) {
        setTitle(computeTitle(curModel, project));
      }
    }

    @Override
    public void testingChanged() {
      if (getModel().isRunning() && !getModel().isPaused()) {
        run.setEnabled(false);
        stop.setEnabled(true);
      } else if (getModel().getVector() != null && finished != count) {
        run.setEnabled(true);
        stop.setEnabled(false);
      } else {
        run.setEnabled(false);
        stop.setEnabled(false);
      }
      reset.setEnabled(getModel().getVector() != null && finished > 0);
    }

    @Override
    public void testResultsChanged(int numPass, int numFail) {
      pass.setText(S.get("passMessage", Integer.toString(numPass)));
      fail.setText(S.get("failMessage", Integer.toString(numFail)));
      finished = numPass + numFail;
    }

    @Override
    public void vectorChanged() {
      // do nothing
    }

    // simulator
    @Override
    public void simulatorReset(Simulator.Event e) {
      // FIXME: is no-op the right implementation here?
      // ? curModel.propagationCompleted();
    }

    @Override
    public void propagationCompleted(Simulator.Event e) {
      // FIXME: is no-op the right implementation here?
      // curMoedl.propagationCompleted();
    }

    @Override
    public void simulatorStateChanged(Simulator.Event e) {
      // do nothing
    }

  }

  private class WindowMenuManager extends WindowMenuItemManager
      implements LocaleListener, ProjectListener {

    WindowMenuManager() {
      super(S.get("logFrameMenuItem"), false);
      project.addProjectListener(this);
    }

    @Override
    public JFrame getJFrame(boolean create, java.awt.Component parent) {
      return TestFrame.this;
    }

    @Override
    public void localeChanged() {
      String title = project.getLogisimFile().getDisplayName();
      setText(S.get("testFrameMenuItem", title));
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      if (event.getAction() == ProjectEvent.ACTION_SET_FILE) {
        localeChanged();
      }
    }
  }
}
