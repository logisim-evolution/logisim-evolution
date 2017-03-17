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

package com.cburch.logisim.gui.start;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SplashScreen extends JWindow implements ActionListener {

	private static class Marker {
		int count;
		String message;

		Marker(int count, String message) {
			this.count = count;
			this.message = message;
		}
	}

	final static Logger logger = LoggerFactory.getLogger(SplashScreen.class);

	private static final long serialVersionUID = 1L;
	public static final int LIBRARIES = 0;
	public static final int TEMPLATE_CREATE = 1;
	public static final int TEMPLATE_OPEN = 2;
	public static final int TEMPLATE_LOAD = 3;
	public static final int TEMPLATE_CLOSE = 4;
	public static final int GUI_INIT = 5;
	public static final int FILE_CREATE = 6;
	public static final int FILE_LOAD = 7;
	public static final int PROJECT_CREATE = 8;

	public static final int FRAME_CREATE = 9;
	private static final int PROGRESS_MAX = 3568;

	private static final boolean PRINT_TIMES = false;

	Marker[] markers = new Marker[] {
			new Marker(377, Strings.get("progressLibraries")),
			new Marker(990, Strings.get("progressTemplateCreate")),
			new Marker(1002, Strings.get("progressTemplateOpen")),
			new Marker(1002, Strings.get("progressTemplateLoad")),
			new Marker(1470, Strings.get("progressTemplateClose")),
			new Marker(1478, Strings.get("progressGuiInitialize")),
			new Marker(2114, Strings.get("progressFileCreate")),
			new Marker(2114, Strings.get("progressFileLoad")),
			new Marker(2383, Strings.get("progressProjectCreate")),
			new Marker(2519, Strings.get("progressFrameCreate")), };
	boolean inClose = false; // for avoiding mutual recursion
	JProgressBar progress = new JProgressBar(0, PROGRESS_MAX);
	JButton close = new JButton(Strings.get("startupCloseButton"));
	JButton cancel = new JButton(Strings.get("startupQuitButton"));
	long startTime = System.currentTimeMillis();

	public SplashScreen() {
		JPanel imagePanel = About.getImagePanel();
		imagePanel.setBorder(null);

		progress.setStringPainted(true);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(close);
		close.addActionListener(this);
		buttonPanel.add(cancel);
		cancel.addActionListener(this);

		JPanel contents = new JPanel(new BorderLayout());
		contents.add(imagePanel, BorderLayout.NORTH);
		contents.add(progress, BorderLayout.CENTER);
		contents.add(buttonPanel, BorderLayout.SOUTH);
		contents.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

		Color bg = imagePanel.getBackground();
		contents.setBackground(bg);
		buttonPanel.setBackground(bg);
		setBackground(bg);
		setContentPane(contents);
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == cancel) {
			System.exit(0);
		} else if (src == close) {
			close();
		}
	}

	public void close() {
		if (inClose)
			return;
		inClose = true;
		setVisible(false);
		inClose = false;
		if (PRINT_TIMES) {
			logger.info("{} closed", System.currentTimeMillis() - startTime);
		}
		markers = null;
	}

	public void setProgress(int markerId) {
		final Marker marker = markers == null ? null : markers[markerId];
		if (marker != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progress.setString(marker.message);
					progress.setValue(marker.count);
				}
			});
			if (PRINT_TIMES) {
				logger.info("{} {}", System.currentTimeMillis() - startTime,
						marker.message);
			}
		} else {
			if (PRINT_TIMES) {
				logger.info("{} ??", System.currentTimeMillis() - startTime);
			}
		}
	}

	@Override
	public void setVisible(boolean value) {
		if (value) {
			pack();
			Dimension dim = getToolkit().getScreenSize();
			int x = (int) (dim.getWidth() - getWidth()) / 2;
			int y = (int) (dim.getHeight() - getHeight()) / 2;
			setLocation(x, y);
		}
		super.setVisible(value);
	}
}
