/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.generic.TikZWriter;
import com.cburch.logisim.gui.main.ExportImage;
import com.cburch.logisim.gui.main.ExportImage.ImageFileFilter;
import com.cburch.logisim.util.GifEncoder;
import com.cburch.logisim.util.JFileChoosers;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public abstract class PrintHandler implements Printable {

  static File lastExportedFile;

  public static File getLastExported() {
    return lastExportedFile;
  }

  public static void setLastExported(File f) {
    lastExportedFile = f;
  }

  public void actionPerformed(ActionEvent e) {
    final var src = e.getSource();
    if (src == LogisimMenuBar.PRINT) print();
    else if (src == LogisimMenuBar.EXPORT_IMAGE) exportImage();
  }

  public void print() {
    final var format = new PageFormat();
    final var job = PrinterJob.getPrinterJob();
    job.setPrintable(this, format);
    if (!job.printDialog()) return;
    try {
      job.print();
    } catch (PrinterException e) {
      OptionPane.showMessageDialog(
          KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
          S.get("printError", e.toString()),
          S.get("printErrorTitle"),
          OptionPane.ERROR_MESSAGE);
    }
  }

  @Override
  public int print(Graphics pg, PageFormat pf, int pageNum) {
    final var imWidth = pf.getImageableWidth();
    final var imHeight = pf.getImageableHeight();
    final var g = (Graphics2D) pg;
    g.translate(pf.getImageableX(), pf.getImageableY());
    return print(g, pf, pageNum, imWidth, imHeight);
  }

  public abstract int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h);

  public void exportImage() {
    ImageFileFilter[] filters = {
      ExportImage.getFilter(ExportImage.FORMAT_PNG),
      ExportImage.getFilter(ExportImage.FORMAT_GIF),
      ExportImage.getFilter(ExportImage.FORMAT_JPG),
      ExportImage.getFilter(ExportImage.FORMAT_TIKZ),
      ExportImage.getFilter(ExportImage.FORMAT_SVG)
    };
    final var chooser = JFileChoosers.createSelected(getLastExported());
    chooser.setAcceptAllFileFilterUsed(false);
    for (final var ff : filters) {
      chooser.addChoosableFileFilter(ff);
    }
    chooser.setFileFilter(filters[0]);
    chooser.setDialogTitle(S.get("exportImageFileSelect"));

    final var returnVal =
        chooser.showDialog(
            KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
            S.get("exportImageButton"));
    if (returnVal != JFileChooser.APPROVE_OPTION) return;
    var dest = chooser.getSelectedFile();
    FileFilter ff = null;
    for (final var filter : filters) {
      if (filter.accept(dest)) ff = filter;
    }
    if (ff == null) ff = chooser.getFileFilter();
    if (!ff.accept(dest)) {
      if (ff == filters[0]) dest = new File(dest + ".png");
      else if (ff == filters[1]) dest = new File(dest + ".gif");
      else if (ff == filters[2]) dest = new File(dest + ".jpg");
      else if (ff == filters[3]) dest = new File(dest + ".tex");
      else dest = new File(dest + ".svg");
    }
    setLastExported(dest);
    if (dest.exists()) {
      final var confirm =
          OptionPane.showConfirmDialog(
              KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
              S.get("confirmOverwriteMessage"),
              S.get("confirmOverwriteTitle"),
              OptionPane.YES_NO_OPTION);
      if (confirm != OptionPane.YES_OPTION) return;
    }
    final var fmt =
        (ff == filters[0]
            ? ExportImage.FORMAT_PNG
            : ff == filters[1]
                ? ExportImage.FORMAT_GIF
                : ff == filters[2]
                    ? ExportImage.FORMAT_JPG
                    : ff == filters[2] ? ExportImage.FORMAT_TIKZ : ExportImage.FORMAT_SVG);
    exportImage(dest, fmt);
  }

  public void exportImage(File dest, int fmt) {
    final var d = getExportImageSize();
    if (d == null) {
      showErr("couldNotCreateImage");
      return;
    }

    final var img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
    final var base = (fmt == ExportImage.FORMAT_TIKZ || fmt == ExportImage.FORMAT_SVG) ? new TikZWriter() : img.getGraphics();
    final var gr = base.create();

    try {
      if (!(gr instanceof Graphics2D g2d)) {
        showErr("couldNotCreateImage");
        return;
      }
      g2d.setColor(Color.white);
      g2d.fillRect(0, 0, d.width, d.height);
      g2d.setColor(Color.black);

      try {
        paintExportImage(img, g2d);
      } catch (Exception e) {
        showErr("couldNotCreateImage");
        return;
      }

      try {
        switch (fmt) {
          case ExportImage.FORMAT_GIF:
            GifEncoder.toFile(img, dest, null);
            break;
          case ExportImage.FORMAT_PNG:
            ImageIO.write(img, "PNG", dest);
            break;
          case ExportImage.FORMAT_JPG:
            ImageIO.write(img, "JPEG", dest);
            break;
          case ExportImage.FORMAT_TIKZ:
            ((TikZWriter) g2d).writeFile(dest);
            break;
          case ExportImage.FORMAT_SVG:
            ((TikZWriter) g2d).writeSvg(d.width, d.height, dest);
            break;
        }
      } catch (Exception e) {
        showErr("couldNotCreateFile");
        return;
      }
    } finally {
      gr.dispose();
    }
  }

  public abstract Dimension getExportImageSize();

  public abstract void paintExportImage(BufferedImage img, Graphics2D g);

  private boolean showErr(String key) {
    final Component parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    OptionPane.showMessageDialog(parent, S.get("couldNotCreateImage"));
    return true;
  }
}
