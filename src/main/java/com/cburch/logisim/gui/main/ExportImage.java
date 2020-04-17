/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.generic.TikZWriter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GifEncoder;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.UniquelyNamedThread;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ProgressMonitor;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportImage {

  private static class ExportThread extends UniquelyNamedThread {
    Frame frame;
    Canvas canvas;
    File dest;
    ImageFileFilter filter;
    List<Circuit> circuits;
    double scale;
    boolean printerView;
    ProgressMonitor monitor;

    ExportThread(
        Frame frame,
        Canvas canvas,
        File dest,
        ImageFileFilter f,
        List<Circuit> circuits,
        double scale,
        boolean printerView,
        ProgressMonitor monitor) {
      super("ExportThread");
      this.frame = frame;
      this.canvas = canvas;
      this.dest = dest;
      this.filter = f;
      this.circuits = circuits;
      this.scale = scale;
      this.printerView = printerView;
      this.monitor = monitor;
    }

    private void export(Circuit circuit) {
      Bounds bds = circuit.getBounds(canvas.getGraphics()).expand(BORDER_SIZE);
      int width = (int) Math.round(bds.getWidth() * scale);
      int height = (int) Math.round(bds.getHeight() * scale);
      Graphics g;
      Graphics base;
      BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      if (filter.type == FORMAT_TIKZ || filter.type == FORMAT_SVG) {
        base = new TikZWriter();   
        g = base.create();
      } else {
        base = img.getGraphics();
        g = base.create();
        g.setColor(Color.white);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.black);
      }
      if (g instanceof Graphics2D) {
        ((Graphics2D) g).scale(scale, scale);
        ((Graphics2D) g).translate(-bds.getX(), -bds.getY());
      } else {
        OptionPane.showMessageDialog(frame, S.get("couldNotCreateImage"));
        monitor.close();
      }

      CircuitState circuitState = canvas.getProject().getCircuitState(circuit);
      ComponentDrawContext context =
          new ComponentDrawContext(canvas, circuit, circuitState, base, g, printerView);
      circuit.draw(context, null);

      File where;
      if (dest.isDirectory()) {
        where = new File(dest, circuit.getName() + filter.extensions[0]);
      } else if (filter.accept(dest)) {
        where = dest;
      } else {
        String newName = dest.getName() + filter.extensions[0];
        where = new File(dest.getParentFile(), newName);
      }
      try {
        switch (filter.type) {
          case FORMAT_GIF:
            GifEncoder.toFile(img, where, monitor);
            break;
          case FORMAT_PNG:
            ImageIO.write(img, "PNG", where);
            break;
          case FORMAT_JPG:
            ImageIO.write(img, "JPEG", where);
            break;
          case FORMAT_TIKZ:
            ((TikZWriter)g).WriteFile(where);
            break;
          case FORMAT_SVG:
            ((TikZWriter)g).WriteSvg(width, height,where);
            break;
        }
      } catch (Exception e) {
        OptionPane.showMessageDialog(frame, S.get("couldNotCreateFile"));
        e.printStackTrace();
        monitor.close();
        return;
      }
      g.dispose();
      monitor.close();
    }

    @Override
    public void run() {
      for (Circuit circ : circuits) {
        export(circ);
      }
    }
  }

  public static class ImageFileFilter extends FileFilter {
    private int type;
    private String[] extensions;
    private StringGetter desc;

    public ImageFileFilter(int type, StringGetter desc, String[] exts) {
      this.type = type;
      this.desc = desc;
      extensions = new String[exts.length];
      for (int i = 0; i < exts.length; i++) {
        extensions[i] = "." + exts[i].toLowerCase();
      }
    }

    @Override
    public boolean accept(File f) {
      String name = f.getName().toLowerCase();
      for (int i = 0; i < extensions.length; i++) {
        if (name.endsWith(extensions[i])) return true;
      }
      return f.isDirectory();
    }

    @Override
    public String getDescription() {
      return desc.toString();
    }
  }

  private static class OptionsPanel extends JPanel implements ChangeListener {
    private static final long serialVersionUID = 1L;
    JSlider slider;
    JLabel curScale;
    JCheckBox printerView;
    JRadioButton formatPng;
    JRadioButton formatGif;
    JRadioButton formatJpg;
    JRadioButton formatTikZ;
    JRadioButton formatSvg;
    GridBagLayout gridbag;
    GridBagConstraints gbc;
    Dimension curJim;

    @SuppressWarnings("rawtypes")
    OptionsPanel(JList list) {
      // set up components
      formatPng = new JRadioButton("PNG");
      formatGif = new JRadioButton("GIF");
      formatJpg = new JRadioButton("JPEG");
      formatTikZ = new JRadioButton("TikZ");
      formatSvg = new JRadioButton("SVG");
      ButtonGroup bgroup = new ButtonGroup();
      bgroup.add(formatPng);
      bgroup.add(formatGif);
      bgroup.add(formatJpg);
      bgroup.add(formatTikZ);
      bgroup.add(formatSvg);
      formatTikZ.addChangeListener(this);
      formatSvg.addChangeListener(this);
      formatPng.setSelected(true);

      slider = new JSlider(JSlider.HORIZONTAL, -3 * SLIDER_DIVISIONS, 3 * SLIDER_DIVISIONS, 0);
      slider.setMajorTickSpacing(10);
      slider.addChangeListener(this);
      curScale = new JLabel("222%");
      curScale.setHorizontalAlignment(SwingConstants.RIGHT);
      curScale.setVerticalAlignment(SwingConstants.CENTER);
      Dimension d = curScale.getPreferredSize();
      curJim = new Dimension(AppPreferences.getScaled(d.width+(d.width>>1)),AppPreferences.getScaled(d.height));
      curJim.height = Math.max(curJim.height, slider.getPreferredSize().height);
      stateChanged(null);

      printerView = new JCheckBox();
      printerView.setSelected(true);

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
      addGb(new JLabel(S.get("labelImageFormat") + " "));
      Box formatsPanel = new Box(BoxLayout.Y_AXIS);
      formatsPanel.add(formatPng);
      formatsPanel.add(formatGif);
      formatsPanel.add(formatJpg);
      formatsPanel.add(formatTikZ);
      formatsPanel.add(formatSvg);
      addGb(formatsPanel);

      gbc.gridy++;
      addGb(new JLabel(S.get("labelScale") + " "));
      addGb(slider);
      addGb(curScale);

      gbc.gridy++;
      addGb(new JLabel(S.get("labelPrinterView") + " "));
      addGb(printerView);
    }

    private void addGb(JComponent comp) {
      gridbag.setConstraints(comp, gbc);
      add(comp);
    }

    int getImageFormat() {
      if (formatGif.isSelected()) return FORMAT_GIF;
      if (formatJpg.isSelected()) return FORMAT_JPG;
      if (formatTikZ.isSelected()) return FORMAT_TIKZ;
      if (formatSvg.isSelected()) return FORMAT_SVG;
      return FORMAT_PNG;
    }

    boolean getPrinterView() {
      return printerView.isSelected();
    }

    double getScale() {
      return Math.pow(2.0, (double) slider.getValue() / SLIDER_DIVISIONS);
    }

    public void stateChanged(ChangeEvent e) {
      double scale = getScale();
      curScale.setText((int) Math.round(100.0 * scale) + "%");
      if (curJim != null) curScale.setPreferredSize(curJim);
      if (e == null)
        return;
      if (e.getSource().equals(formatTikZ)||e.getSource().equals(formatSvg)) {
        if (formatTikZ.isSelected() || formatSvg.isSelected()) {
          curScale.setEnabled(false);
          slider.setEnabled(false);
          slider.setValue(0);
          curScale.setText(100+ "%");
          if (curJim != null) curScale.setPreferredSize(curJim);
        } else {
          curScale.setEnabled(true);
          slider.setEnabled(true);
        }
      }
    }
  }
  
  public static ImageFileFilter getFilter(int fmt) {
    switch (fmt) {
    case FORMAT_GIF:
      return new ImageFileFilter(fmt,
          S.getter("exportGifFilter"), new String[] { "gif" });
    case FORMAT_PNG:
      return new ImageFileFilter(fmt,
          S.getter("exportPngFilter"), new String[] { "png" });
    case FORMAT_JPG:
      return new ImageFileFilter(fmt,
          S.getter("exportJpgFilter"), new String[] { "jpg",
            "jpeg", "jpe", "jfi", "jfif", "jfi" });
    case FORMAT_TIKZ:
      return new ImageFileFilter(fmt,
          S.getter("exportTikZFilter"), new String[] { "tex" });
    case FORMAT_SVG:
      return new ImageFileFilter(fmt,
          S.getter("exportSvgFilter"), new String[] { "svg" });
    default:
      logger.error("Unexpected image format; aborted!");
      return null;
    }
  }



  public static void doExport(Project proj) {
    // First display circuit/parameter selection dialog
    Frame frame = proj.getFrame();
    CircuitJList list = new CircuitJList(proj, true);
    if (list.getModel().getSize() == 0) {
      OptionPane.showMessageDialog(
          proj.getFrame(),
          S.get("exportEmptyCircuitsMessage"),
          S.get("exportEmptyCircuitsTitle"),
          OptionPane.YES_NO_OPTION);
      return;
    }
    OptionsPanel options = new OptionsPanel(list);
    int action =
        OptionPane.showConfirmDialog(
            frame,
            options,
            S.get("exportImageSelect"),
            OptionPane.OK_CANCEL_OPTION,
            OptionPane.QUESTION_MESSAGE);
    if (action != OptionPane.OK_OPTION) return;
    List<Circuit> circuits = list.getSelectedCircuits();
    double scale = options.getScale();
    boolean printerView = options.getPrinterView();
    if (circuits.isEmpty()) return;

    int fmt = options.getImageFormat();
    ImageFileFilter filter = getFilter(fmt);
    if (filter == null)
        return;

    // Then display file chooser
    Loader loader = proj.getLogisimFile().getLoader();
    JFileChooser chooser = loader.createChooser();
    chooser.setAcceptAllFileFilterUsed(false);
    if (circuits.size() > 1) {
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setDialogTitle(S.get("exportImageDirectorySelect"));
    } else {
      chooser.setFileFilter(filter);
      chooser.setDialogTitle(S.get("exportImageFileSelect"));
    }
    int returnVal = chooser.showDialog(frame, S.get("exportImageButton"));
    if (returnVal != JFileChooser.APPROVE_OPTION) return;

    // Determine whether destination is valid
    File dest = chooser.getSelectedFile();
    chooser.setCurrentDirectory(dest.isDirectory() ? dest : dest.getParentFile());
    if (dest.exists()) {
      if (!dest.isDirectory()) {
        int confirm =
            OptionPane.showConfirmDialog(
                proj.getFrame(),
                S.get("confirmOverwriteMessage"),
                S.get("confirmOverwriteTitle"),
                OptionPane.YES_NO_OPTION);
        if (confirm != OptionPane.YES_OPTION) return;
      }
    } else {
      if (circuits.size() > 1) {
        boolean created = dest.mkdir();
        if (!created) {
          OptionPane.showMessageDialog(
              proj.getFrame(),
              S.get("exportNewDirectoryErrorMessage"),
              S.get("exportNewDirectoryErrorTitle"),
              OptionPane.YES_NO_OPTION);
          return;
        }
      }
    }

    // Create the progress monitor
    ProgressMonitor monitor =
        new ProgressMonitor(frame, S.get("exportImageProgress"), null, 0, 10000);
    monitor.setMillisToDecideToPopup(100);
    monitor.setMillisToPopup(200);
    monitor.setProgress(0);

    // And start a thread to actually perform the operation
    // (This is run in a thread so that Swing will update the
    // monitor.)
    new ExportThread(frame, frame.getCanvas(), dest, filter, circuits, scale, printerView, monitor)
        .start();
  }

  static final Logger logger = LoggerFactory.getLogger(ExportImage.class);

  private static final int SLIDER_DIVISIONS = 6;

  public static final int FORMAT_GIF = 0;

  public static final int FORMAT_PNG = 1;

  public static final int FORMAT_JPG = 2;

  public static final int FORMAT_TIKZ = 3;
  
  public static final int FORMAT_SVG = 4;

  private static final int BORDER_SIZE = 5;

  private ExportImage() {}
}
