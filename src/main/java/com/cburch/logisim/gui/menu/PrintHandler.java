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

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

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

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.generic.TikZWriter;
import com.cburch.logisim.gui.main.ExportImage;
import com.cburch.logisim.gui.main.ExportImage.ImageFileFilter;
import com.cburch.logisim.util.GifEncoder;
import com.cburch.logisim.util.JFileChoosers;

public abstract class PrintHandler implements Printable {

  static File lastExportedFile;

  public static File getLastExported() {
    return lastExportedFile;
  }

  public static void setLastExported(File f) {
    lastExportedFile = f;
  }

  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src == LogisimMenuBar.PRINT)
      print();
    else if (src == LogisimMenuBar.EXPORT_IMAGE)
      exportImage();
  }

  public void print() {
    PageFormat format = new PageFormat();
    PrinterJob job = PrinterJob.getPrinterJob();
    job.setPrintable(this, format);
    if (!job.printDialog())
      return;
    try {
      job.print();
    } catch (PrinterException e) {
      OptionPane.showMessageDialog(
          KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
          S.fmt("printError", e.toString()),
          S.get("printErrorTitle"), OptionPane.ERROR_MESSAGE);
    }
  }

  public void exportImage() {
    ImageFileFilter[] filters = {
      ExportImage.getFilter(ExportImage.FORMAT_PNG),
      ExportImage.getFilter(ExportImage.FORMAT_GIF),
      ExportImage.getFilter(ExportImage.FORMAT_JPG),
      ExportImage.getFilter(ExportImage.FORMAT_TIKZ),
      ExportImage.getFilter(ExportImage.FORMAT_SVG)
    };
    JFileChooser chooser = JFileChoosers.createSelected(getLastExported());
    chooser.setAcceptAllFileFilterUsed(false);
    for (ImageFileFilter ff : filters)
      chooser.addChoosableFileFilter(ff);
    chooser.setFileFilter(filters[0]);
    chooser.setDialogTitle(S.get("exportImageFileSelect"));

    int returnVal = chooser.showDialog(
        KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
        S.get("exportImageButton"));
    if (returnVal != JFileChooser.APPROVE_OPTION)
      return;
    File dest = chooser.getSelectedFile();
    FileFilter ff = null;
    for (int i = 0; i < filters.length ; i++) {
      if (filters[i].accept(dest))
        ff = filters[i];
    }
    if (ff == null)
      ff = chooser.getFileFilter();
    if (!ff.accept(dest)) {
      if (ff == filters[0]) dest = new File(dest + ".png");
      else if (ff == filters[1]) dest = new File(dest + ".gif");
      else if (ff == filters[2]) dest = new File(dest + ".jpg");
      else if (ff == filters[3]) dest = new File(dest+".tex");
      else dest = new File(dest+".svg");
    }
    setLastExported(dest);
    if (dest.exists()) {
      int confirm = OptionPane.showConfirmDialog(
          KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
          S.get("confirmOverwriteMessage"),
          S.get("confirmOverwriteTitle"),
          OptionPane.YES_NO_OPTION);
      if (confirm != OptionPane.YES_OPTION)
        return;
    }
    int fmt = (ff == filters[0] ? ExportImage.FORMAT_PNG
        : ff == filters[1] ? ExportImage.FORMAT_GIF
        : ff == filters[2] ? ExportImage.FORMAT_JPG
        : ff == filters[2] ? ExportImage.FORMAT_TIKZ
        : ExportImage.FORMAT_SVG);
    exportImage(dest, fmt);
  }

  @Override
  public int print(Graphics pg, PageFormat pf, int pageNum) {
    double imWidth = pf.getImageableWidth();
    double imHeight = pf.getImageableHeight();
    Graphics2D g = (Graphics2D) pg;
    g.translate(pf.getImageableX(), pf.getImageableY());
    return print(g, pf, pageNum, imWidth, imHeight);
  }

  public abstract int print(Graphics2D g, PageFormat pf, int pageNum, double w, double h);

  public abstract Dimension getExportImageSize();

  public abstract void paintExportImage(BufferedImage img, Graphics2D g);

  private boolean showErr(String key) {
    Component parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    String msg = S.get("couldNotCreateImage");
    OptionPane.showMessageDialog(parent, msg);
    return true;
  }

  public void exportImage(File dest, int fmt) {
    Dimension d = getExportImageSize();
    if (d == null && showErr("couldNotCreateImage"))
      return;

    Graphics base;
    Graphics gr;
    BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
    if (fmt == ExportImage.FORMAT_TIKZ || fmt == ExportImage.FORMAT_SVG)
      base = new TikZWriter();
    else
      base = img.getGraphics();
    gr = base.create();
    try {
      if (!(gr instanceof Graphics2D) && showErr("couldNotCreateImage"))
        return;
      Graphics2D g = (Graphics2D)gr;
      g.setColor(Color.white);
      g.fillRect(0, 0, d.width, d.height);
      g.setColor(Color.black);

      try {
        paintExportImage(img, g);
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
          ((TikZWriter)g).WriteFile(dest);
          break;
        case ExportImage.FORMAT_SVG:
          ((TikZWriter)g).WriteSvg(d.width, d.height,dest);
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
}
