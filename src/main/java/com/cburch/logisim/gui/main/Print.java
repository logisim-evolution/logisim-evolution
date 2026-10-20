/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Project;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class Print {
  private Print() {}

  public static void doPrint(Project proj) {
    final var list = new CircuitJList(proj, true);
    final var frame = proj.getFrame();
    if (list.getModel().getSize() == 0) {
      OptionPane.showMessageDialog(
          proj.getFrame(),
          S.get("printEmptyCircuitsMessage"),
          S.get("printEmptyCircuitsTitle"),
          OptionPane.YES_NO_OPTION);
      return;
    }
    var parmsPanel = new ParmsPanel(list);
    int action =
        OptionPane.showConfirmDialog(
            frame,
            parmsPanel,
            S.get("printParmsTitle"),
            OptionPane.OK_CANCEL_OPTION,
            OptionPane.QUESTION_MESSAGE);
    if (action != OptionPane.OK_OPTION) return;
    List<Circuit> circuits = list.getSelectedCircuits();
    if (circuits.isEmpty()) return;

    final var format = new PageFormat();
    final var print = new MyPrintable(
            proj,
            circuits,
            parmsPanel.getHeader(),
            parmsPanel.getRotateToFit(),
            parmsPanel.getPrinterView());

    final var job = PrinterJob.getPrinterJob();
    job.setPrintable(print, format);
    if (!job.printDialog()) return;
    try {
      job.print();
    } catch (PrinterException e) {
      OptionPane.showMessageDialog(
          proj.getFrame(),
          S.get("printError", e.toString()),
          S.get("printErrorTitle"),
          OptionPane.ERROR_MESSAGE);
    }
  }

  private static String format(String header, int index, int max, String circName) {
    int mark = header.indexOf('%');
    if (mark < 0) return header;
    final var ret = new StringBuilder();
    int start = 0;
    for (;
        mark >= 0 && mark + 1 < header.length();
        start = mark + 2, mark = header.indexOf('%', start)) {
      ret.append(header, start, mark);
      switch (header.charAt(mark + 1)) {
        case 'n' -> ret.append(circName);
        case 'p' -> ret.append("").append(index);
        case 'P' -> ret.append("").append(max);
        case '%' -> ret.append("%");
        default -> ret.append("%").append(header.charAt(mark + 1));
      }
    }
    if (start < header.length()) {
      ret.append(header.substring(start));
    }
    return ret.toString();
  }

  private static class MyPrintable implements Printable {
    final Project proj;
    final List<Circuit> circuits;
    final String header;
    final boolean rotateToFit;
    final boolean printerView;

    MyPrintable(
        Project proj,
        List<Circuit> circuits,
        String header,
        boolean rotateToFit,
        boolean printerView) {
      this.proj = proj;
      this.circuits = circuits;
      this.header = header;
      this.rotateToFit = rotateToFit;
      this.printerView = printerView;
    }

    public int print(Graphics base, PageFormat format, int pageIndex) {
      if (pageIndex >= circuits.size()) return Printable.NO_SUCH_PAGE;

      var circ = circuits.get(pageIndex);
      final var circState = proj.getCircuitState(circ);
      final var g = base.create();
      final var g2 = g instanceof Graphics2D ? (Graphics2D) g : null;
      final var fm = g.getFontMetrics();
      final var head =
          (header != null && !header.isEmpty())
              ? format(header, pageIndex + 1, circuits.size(), circ.getName())
              : null;
      final var headHeight = (head == null ? 0 : fm.getHeight());

      // Compute image size
      var imWidth = format.getImageableWidth();
      var imHeight = format.getImageableHeight();

      // Correct coordinate system for page, including
      // translation and possible rotation.
      final var bds = circ.getBounds(g).expand(4);
      var scale = Math.min(imWidth / bds.getWidth(), (imHeight - headHeight) / bds.getHeight());
      if (g2 != null) {
        g2.translate(format.getImageableX(), format.getImageableY());
        if (rotateToFit && scale < 1.0 / 1.1) {
          double scale2 =
              Math.min(imHeight / bds.getWidth(), (imWidth - headHeight) / bds.getHeight());
          if (scale2 >= scale * 1.1) { // will rotate
            scale = scale2;
            if (imHeight > imWidth) { // portrait -> landscape
              g2.translate(0, imHeight);
              g2.rotate(-Math.PI / 2);
            } else { // landscape -> portrait
              g2.translate(imWidth, 0);
              g2.rotate(Math.PI / 2);
            }
            var t = imHeight;
            imHeight = imWidth;
            imWidth = t;
          }
        }
      }

      // Draw the header line if appropriate
      if (head != null) {
        g.drawString(head, (int) Math.round((imWidth - fm.stringWidth(head)) / 2), fm.getAscent());
        if (g2 != null) {
          imHeight -= headHeight;
          g2.translate(0, headHeight);
        }
      }

      // Now change coordinate system for circuit, including
      // translation and possible scaling
      if (g2 != null) {
        if (scale < 1.0) {
          g2.scale(scale, scale);
          imWidth /= scale;
          imHeight /= scale;
        }
        double dx = Math.max(0.0, (imWidth - bds.getWidth()) / 2);
        g2.translate(-bds.getX() + dx, -bds.getY());
      }

      // Ensure that the circuit is eligible to be drawn
      final var clip = g.getClipBounds();
      clip.add(bds.getX(), bds.getY());
      clip.add(bds.getX() + bds.getWidth(), bds.getY() + bds.getHeight());
      g.setClip(clip);

      // And finally draw the circuit onto the page
      final var context =
          new ComponentDrawContext(
              proj.getFrame().getCanvas(), circ, circState, base, g, printerView);
      Collection<Component> noComps = Collections.emptySet();
      circ.draw(context, noComps);
      g.dispose();
      return Printable.PAGE_EXISTS;
    }
  }

  private static class ParmsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    final JCheckBox rotateToFit;
    final JCheckBox printerView;
    final JTextField header;
    final GridBagLayout gridbag;
    final GridBagConstraints gbc;

    @SuppressWarnings("rawtypes")
    ParmsPanel(JList list) {
      // set up components
      rotateToFit = new JCheckBox();
      rotateToFit.setSelected(true);
      printerView = new JCheckBox();
      printerView.setSelected(true);
      header = new JTextField(20);
      header.setText("%n (%p of %P)");

      // set up panel
      gridbag = new GridBagLayout();
      gbc = new GridBagConstraints();
      setLayout(gridbag);

      // now add components into panel
      gbc.gridy = 0;
      gbc.gridx = GridBagConstraints.RELATIVE;
      gbc.anchor = GridBagConstraints.NORTHWEST;
      gbc.insets = new Insets(5, 0, 5, 0);
      gbc.fill = GridBagConstraints.NONE;
      addGb(new JLabel(S.get("labelCircuits") + " "));
      gbc.fill = GridBagConstraints.HORIZONTAL;
      addGb(new JScrollPane(list));
      gbc.fill = GridBagConstraints.NONE;

      gbc.gridy++;
      addGb(new JLabel(S.get("labelHeader") + " "));
      addGb(header);

      gbc.gridy++;
      addGb(new JLabel(S.get("labelRotateToFit") + " "));
      addGb(rotateToFit);

      gbc.gridy++;
      addGb(new JLabel(S.get("labelPrinterView") + " "));
      addGb(printerView);
    }

    private void addGb(JComponent comp) {
      gridbag.setConstraints(comp, gbc);
      add(comp);
    }

    String getHeader() {
      return header.getText();
    }

    boolean getPrinterView() {
      return printerView.isSelected();
    }

    boolean getRotateToFit() {
      return rotateToFit.isSelected();
    }
  }
}
