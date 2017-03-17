/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.bfh.logisim.fpgaboardeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import com.cburch.logisim.prefs.AppPreferences;

@SuppressWarnings("serial")
public class BoardPanel extends JPanel implements MouseListener,
		MouseMotionListener {
	private static class PNGFileFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(PNG_EXTENSION);
		}

		@Override
		public String getDescription() {
			return Strings.get("PNG File Filter"); // TODO: language adaptation
		}
	}

	/**
	 * 
	 */
	private int image_width = 740;
	private int image_height = 400;
	private BufferedImage image;
	private int xs, ys, w, h;
	private Boolean EditMode;
	public static final String PNG_EXTENSION = ".png";
	public static final FileFilter PNG_FILTER = new PNGFileFilter();

	private BoardDialog edit_parent;
	private float scale = (float) 1.0;

	public BoardPanel(BoardDialog parent) {
		xs = ys = w = h = 0;
		image = null;
		EditMode = true;
		Dimension thedim = new Dimension();
		thedim.width = getWidth();
		thedim.height = getHeight();
		edit_parent = parent;
		super.setPreferredSize(thedim);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
	}

	public BoardPanel(URL filename) {
		xs = ys = w = h = 0;
		try {
			image = ImageIO.read(filename);
		} catch (IOException ex) {
			image = null;
		}
		EditMode = false;
		Dimension thedim = new Dimension();
		thedim.width = getWidth();
		thedim.height = getHeight();
		super.setPreferredSize(thedim);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
	}
	
	public void SetScale(float ScaleFactor) {
		this.scale = ScaleFactor;
		Dimension preferredSize = new Dimension();
		preferredSize.width=getWidth();
		preferredSize.height=getHeight();
		setPreferredSize(preferredSize);
		setSize(preferredSize);
	}
	
	public void clear() {
		image = null;
	}
	
	public int getImageWidth() {
		return (image.getWidth()<3*image_width) ? image.getWidth() : 3*image_width;
	}
	
	public int getImageHeight() {
		return (image.getHeight()<3*image_height) ? image.getHeight() : 3*image_height;
	}

	public int getHeight() {
		return AppPreferences.getScaled(image_height,scale);
	}

	public Image getScaledImage(int width, int height) {
		return image.getScaledInstance(width, height, 4);
	}

	public int getWidth() {
		return AppPreferences.getScaled(image_width,scale);
	}

	public Boolean ImageLoaded() {
		return image != null;
	}

	public void mouseClicked(MouseEvent e) {
		if (EditMode && !this.ImageLoaded()) {
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setDialogTitle("Choose FPGA board picture to use");
			fc.setFileFilter(PNG_FILTER);
			fc.setAcceptAllFileFilterUsed(false);
			int retval = fc.showOpenDialog(null);
			if (retval == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				try {
					image = ImageIO.read(file);
				} catch (IOException ex) {
					image = null;
				}
				this.repaint();
				edit_parent.SetBoardName(file.getName());
			}
		}
	}

	public void mouseDragged(MouseEvent e) {
		if (EditMode && this.ImageLoaded()) {
			this.set_drag(AppPreferences.getDownScaled(e.getX(),scale), 
					AppPreferences.getDownScaled(e.getY(),scale));
			this.repaint();
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {

	}

	public void mousePressed(MouseEvent e) {
		if (e.getClickCount() > 1) {
			this.set_start(0, 0);
			edit_parent.EditDialog(AppPreferences.getDownScaled(e.getX(),scale), 
					AppPreferences.getDownScaled(e.getY(),scale));
		} else
			this.set_start(AppPreferences.getDownScaled(e.getX(),scale), 
					AppPreferences.getDownScaled(e.getY(),scale));
	}

	public void mouseReleased(MouseEvent e) {
		if (EditMode && this.ImageLoaded() && (h != 0) && (w != 0)) {
			BoardRectangle rect = new BoardRectangle(xs, ys, w, h);
			edit_parent.SelectDialog(rect);
			this.set_start(0, 0);
			this.repaint();
		}
	}

	public void paint(Graphics g) {
		super.paint(g);
		if (image != null) {
			g.drawImage(getScaledImage(getWidth(), getHeight()), 0, 0, null);
			g.setColor(Color.red);
			if ((w != 0) || (h != 0)) {
				int xr, yr, wr, hr;
				xr = (w < 0) ? xs + w : xs;
				yr = (h < 0) ? ys + h : ys;
				wr = (w < 0) ? -w : w;
				hr = (h < 0) ? -h : h;
				g.drawRect(AppPreferences.getScaled(xr,scale), AppPreferences.getScaled(yr,scale), 
						AppPreferences.getScaled(wr,scale), AppPreferences.getScaled(hr,scale));
			}
			if (EditMode && (edit_parent.defined_components != null)) {
				LinkedList<BoardRectangle> comps = edit_parent.defined_components;
				Iterator<BoardRectangle> iter = comps.iterator();
				g.setColor(Color.red);
				while (iter.hasNext()) {
					BoardRectangle thisone = iter.next();
					g.fillRect(AppPreferences.getScaled(thisone.getXpos(),scale), 
							AppPreferences.getScaled(thisone.getYpos(),scale),
							AppPreferences.getScaled(thisone.getWidth(),scale), 
							AppPreferences.getScaled(thisone.getHeight(),scale));
				}
			}
		} else {
			g.setColor(Color.gray);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (EditMode) {
				String message;
				int xpos;
				Font curfont = AppPreferences.getScaledFont(new Font(g.getFont().getFontName(), Font.BOLD,
						20));
				g.setColor(Color.black);
				g.setFont(curfont);
				FontMetrics fm = g.getFontMetrics();
				message = "Click here to add a board picture.";
				xpos = (this.getWidth() - fm.stringWidth(message)) / 2;
				g.drawString(message, xpos, 100);
				message = "The board picture should have at least a resolution of";
				xpos = (this.getWidth() - fm.stringWidth(message)) / 2;
				g.drawString(message, xpos, 200);
				message = image_width + "x" + image_height
						+ " pixels (width x height)";
				xpos = (this.getWidth() - fm.stringWidth(message)) / 2;
				g.drawString(message, xpos, (200 + (int) (1.5 * fm.getAscent())) );
				message = "for best graphical display.";
				xpos = (this.getWidth() - fm.stringWidth(message)) / 2;
				g.drawString(message, xpos, (200 + (int) (3 * fm.getAscent())) );
				message = "The board picture formate must be PNG";
				xpos = (this.getWidth() - fm.stringWidth(message)) / 2;
				g.drawString(message, xpos, (200 + (int) (4.5 * fm.getAscent())) );
				message = "Current resolution: "+getWidth()+"x"+getHeight();
				xpos = (this.getWidth() - fm.stringWidth(message)) / 2;
				g.drawString(message, xpos, (200 + (int) (6 * fm.getAscent())) );
			}
		}
	}

	public void set_drag(int x, int y) {
		if (EditMode && this.ImageLoaded()) {
			w = x - xs;
			h = y - ys;
		}
	}

	public void set_start(int x, int y) {
		if (EditMode && this.ImageLoaded()) {
			xs = x;
			ys = y;
			w = 0;
			h = 0;
		}
	}

	public void SetImage(BufferedImage pic) {
		image = pic;
		this.repaint();
	}
}