/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */
package com.cburch.logisim.verilog;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.JFileChoosers;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Source selection and cancellable synthesis; project mutation occurs on the event thread. */
public final class VerilogImportDialog extends JDialog {
  private static final long serialVersionUID = 1L;
  private final Project project;
  private final JTextArea sources = new JTextArea(3, 45);
  private final JTextField top = new JTextField(30);
  private final JTextField executable = new JTextField(30);
  private final JTextField directory = new JTextField(30);
  private final JTextArea includes = new JTextArea(2, 45);
  private final JTextArea defines = new JTextArea(2, 45);
  private final JCheckBox systemVerilog = new JCheckBox(S.get("verilogSystemVerilog"));
  private final JComboBox<String> undefined = new JComboBox<>(new String[] {
      S.get("verilogUndefinedError"), S.get("verilogUndefinedZero"), S.get("verilogUndefinedOne")});
  private final JComboBox<String> memories = new JComboBox<>(new String[] {
      S.get("verilogMemoryAuto"), S.get("verilogMemoryNative"), S.get("verilogMemoryLogic")});
  private final JTextArea log = new JTextArea(12, 60);
  private final JProgressBar progress = new JProgressBar();
  private final JButton run = new JButton(S.get("verilogImport"));
  private final JButton cancel = new JButton(S.get("verilogClose"));
  private SwingWorker<VerilogImporter.Result, String> worker;

  private VerilogImportDialog(Project project) {
    super(project.getFrame(), S.get("verilogImportTitle"), true);
    this.project = project;
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    executable.setText(AppPreferences.getPrefs().get("verilogYosysExecutable", "yosys"));
    directory.setText(project.createChooser().getCurrentDirectory().getAbsolutePath());
    var fields = new JPanel(new GridBagLayout());
    field(fields, 0, "verilogSources", new JScrollPane(sources), browse(this::chooseSources));
    field(fields, 1, "verilogTop", top, null);
    field(fields, 2, "verilogExecutable", executable, browse(this::chooseExecutable));
    field(fields, 3, "verilogWorkingDirectory", directory, null);
    field(fields, 4, "verilogIncludes", new JScrollPane(includes), null);
    field(fields, 5, "verilogDefines", new JScrollPane(defines), null);
    field(fields, 6, "verilogUndefined", undefined, null);
    field(fields, 7, "verilogMemories", memories, null);
    field(fields, 8, "verilogLanguage", systemVerilog, null);
    log.setEditable(false);
    log.setLineWrap(true);
    log.setWrapStyleWord(true);
    var bottom = new JPanel(new BorderLayout(8, 8));
    bottom.add(progress, BorderLayout.CENTER);
    var buttons = new JPanel();
    buttons.add(run);
    buttons.add(cancel);
    bottom.add(buttons, BorderLayout.EAST);
    var content = new JPanel(new BorderLayout(8, 8));
    content.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
    content.add(fields, BorderLayout.NORTH);
    content.add(new JScrollPane(log), BorderLayout.CENTER);
    content.add(bottom, BorderLayout.SOUTH);
    setContentPane(content);
    run.addActionListener(event -> start());
    cancel.addActionListener(event -> closeOrCancel());
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent event) {
        closeOrCancel();
      }
    });
    getRootPane().setDefaultButton(run);
    pack();
    setMinimumSize(new Dimension(720, 640));
    setLocationRelativeTo(project.getFrame());
  }

  public static void show(Project project) {
    new VerilogImportDialog(project).setVisible(true);
  }

  private static JButton browse(Runnable action) {
    var button = new JButton(S.get("verilogBrowse"));
    button.addActionListener(event -> action.run());
    return button;
  }

  private static void field(
      JPanel panel, int row, String key, java.awt.Component field, JButton button) {
    var c = new GridBagConstraints();
    c.gridy = row;
    c.insets = new Insets(3, 3, 3, 3);
    c.anchor = GridBagConstraints.WEST;
    var label = new JLabel(S.get(key));
    label.setLabelFor(field instanceof JScrollPane pane ? pane.getViewport().getView() : field);
    panel.add(label, c);
    c.gridx = 1;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    panel.add(field, c);
    if (button != null) {
      c.gridx = 2;
      c.weightx = 0;
      panel.add(button, c);
    }
  }

  private void chooseSources() {
    if (worker != null) {
      return;
    }
    var chooser = project.createChooser();
    chooser.setMultiSelectionEnabled(true);
    chooser.setFileFilter(new FileNameExtensionFilter(S.get("verilogSourceFilter"), "v", "sv"));
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      var files = chooser.getSelectedFiles();
      sources.setText(
          String.join("\n", Arrays.stream(files).map(java.io.File::getAbsolutePath).toList()));
      if (files.length > 0) {
        directory.setText(files[0].getParent());
      }
    }
  }

  private void chooseExecutable() {
    if (worker != null) {
      return;
    }
    var chooser = JFileChoosers.create();
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      executable.setText(chooser.getSelectedFile().getAbsolutePath());
    }
  }

  private static List<String> lines(String value) {
    return value.lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
  }

  private void appendLog(String message) {
    log.append(message + "\n");
    if (log.getDocument().getLength() > 65536) {
      log.replaceRange("", 0, log.getDocument().getLength() - 65536);
    }
    log.setCaretPosition(log.getDocument().getLength());
  }

  private void start() {
    if (worker != null) {
      return;
    }
    final YosysRunner.Options options;
    try {
      options = new YosysRunner.Options(executable.getText().strip(),
          lines(sources.getText()).stream().map(Path::of).toList(), top.getText().strip(),
          lines(includes.getText()).stream().map(Path::of).toList(), lines(defines.getText()),
          Path.of(directory.getText().strip()), systemVerilog.isSelected(),
          new String[] {"error", "0", "1"}[undefined.getSelectedIndex()],
          YosysRunner.Memories.values()[memories.getSelectedIndex()]);
    } catch (RuntimeException exception) {
      appendLog(exception.getMessage());
      return;
    }
    var destination = project.getLogisimFile();
    var reserved = VerilogImporter.reservedNames(destination);
    AppPreferences.getPrefs().put("verilogYosysExecutable", options.executable());
    run.setEnabled(false);
    cancel.setText(S.get("verilogCancel"));
    progress.setIndeterminate(true);
    log.setText("");
    worker = new SwingWorker<>() {
      @Override
      protected VerilogImporter.Result doInBackground() throws Exception {
        return VerilogImporter.synthesize(options, destination, reserved, this::publish);
      }

      @Override
      protected void process(List<String> messages) {
        messages.forEach(VerilogImportDialog.this::appendLog);
      }

      @Override
      protected void done() {
        try {
          var result = get();
          project.doAction(result.action(S.get("verilogImportAction")));
          appendLog(S.get("verilogSuccess", result.circuits().size(), result.componentCount()));
        } catch (CancellationException exception) {
          appendLog(S.get("verilogCancelled"));
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          appendLog(S.get("verilogCancelled"));
        } catch (ExecutionException exception) {
          appendLog(S.get("verilogFailed", exception.getCause().getMessage()));
        } catch (RuntimeException exception) {
          appendLog(S.get("verilogFailed", exception.getMessage()));
        } finally {
          worker = null;
          run.setEnabled(true);
          cancel.setText(S.get("verilogClose"));
          progress.setIndeterminate(false);
        }
      }
    };
    worker.execute();
  }

  private void closeOrCancel() {
    if (worker != null) {
      worker.cancel(true);
    } else {
      dispose();
    }
  }
}
