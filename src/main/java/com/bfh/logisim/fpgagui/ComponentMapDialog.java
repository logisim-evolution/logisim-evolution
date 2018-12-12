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

package com.bfh.logisim.fpgagui;

import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.BoardRectangle;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.fpgaboardeditor.Strings;
import com.bfh.logisim.fpgaboardeditor.ZoomSlider;
import com.cburch.logisim.prefs.AppPreferences;

public class ComponentMapDialog implements ActionListener,
ListSelectionListener {

	private class MappedComponentIdContainer {

		private String key;
		private BoardRectangle rect;

		public MappedComponentIdContainer(String key,
				BoardRectangle SelectedItem) {
			this.key = key;
			rect = SelectedItem;
		}

		public BoardRectangle getRectangle() {
			return rect;
		}

		public void Paint(Graphics g) {
			g.setFont(AppPreferences.getScaledFont(g.getFont()));
			FontMetrics metrics = g.getFontMetrics(g.getFont());
			Color Yellow = new Color(250, 250, 0, 180);
			Color Blue = new Color(0, 0, 250, 180);
			int hgt = metrics.getHeight();
			int adv = metrics.stringWidth(key);
			int real_xpos = AppPreferences.getScaled(rect.getXpos(),scale) - adv / 2 - 2;
			if (real_xpos < 0) {
				real_xpos = 0;
			} else if (real_xpos + adv + 4 > AppPreferences.getScaled(image_width,scale)) {
				real_xpos = AppPreferences.getScaled(image_width,scale) - adv - 4;
			}
			int real_ypos = AppPreferences.getScaled(rect.getYpos(),scale);
			if (real_ypos - hgt - 4 < 0) {
				real_ypos = hgt + 4;
			}
			g.setColor(Yellow);
			g.fillRect(real_xpos, real_ypos - hgt - 4, adv + 4, hgt + 4);
			g.drawRect(AppPreferences.getScaled(rect.getXpos(),scale) + 1,
					AppPreferences.getScaled(rect.getYpos(),scale),
					AppPreferences.getScaled(rect.getWidth(),scale) - 2,
					AppPreferences.getScaled(rect.getHeight(),scale) - 1);
			g.setColor(Blue);
			g.drawString(key, real_xpos + 2,
					real_ypos - 2 - metrics.getMaxDescent());
		}
	}

	@SuppressWarnings("serial")
	private class SelectionWindow extends JPanel implements MouseListener,
	MouseMotionListener {

		public SelectionWindow() {
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
		}

		@Override
		public int getHeight() {
			return AppPreferences.getScaled(image_height,scale);
		}

		@Override
		public int getWidth() {
			return AppPreferences.getScaled(image_width,scale);
		}

		private void HandleSelect(MouseEvent e) {
			if (!SelectableItems.isEmpty()) {
				if (HighlightItem != null) {
					if (HighlightItem.PointInside(
							AppPreferences.getDownScaled(e.getX(),scale),
							AppPreferences.getDownScaled(e.getY(),scale))) {
						return;
					}
				}
				BoardRectangle NewItem = null;
				/* TODO: Optimize, SLOW! */
				for (BoardRectangle Item : SelectableItems) {
					if (Item.PointInside(
							AppPreferences.getDownScaled(e.getX(),scale),
							AppPreferences.getDownScaled(e.getY(),scale))) {
						NewItem = Item;
						break;
					}
				}
				if ((NewItem == null && HighlightItem != null)
						|| (NewItem != HighlightItem)) {
					HighlightItem = NewItem;
					paintImmediately(0,0,this.getWidth(),this.getHeight());
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseDragged(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
			Note = null;
			paintImmediately(0,0,this.getWidth(),this.getHeight());
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			HandleSelect(e);
			if (MappableComponents.hasMappedComponents()) {
				if (Note != null) {
					if (Note.getRectangle().PointInside(
							AppPreferences.getDownScaled(e.getX(),scale),
							AppPreferences.getDownScaled(e.getY(),scale))) {
						return;
					}
				}
				BoardRectangle NewItem = null;
				String newKey = "";
				/* TODO: This makes the things very slow optimize! */
				for (BoardRectangle ThisItem : MappableComponents
						.GetMappedRectangles()) {
					if (ThisItem.PointInside(
							AppPreferences.getDownScaled(e.getX(),scale),
							AppPreferences.getDownScaled(e.getY(),scale))) {
						NewItem = ThisItem;
						newKey = MappableComponents.GetDisplayName(ThisItem);
						break;
					}
				}
				if (Note == null) {
					if (NewItem != null) {
						Note = new MappedComponentIdContainer(newKey, NewItem);
						this.paintImmediately(0,0,this.getWidth(),this.getHeight());
					}
				} else {
					if (!Note.getRectangle().equals(NewItem)) {
						if (NewItem != null) {
							Note = new MappedComponentIdContainer(newKey,
									NewItem);
							this.paintImmediately(0,0,this.getWidth(),this.getHeight());
						} else {
							Note = null;
							this.paintImmediately(0,0,this.getWidth(),this.getHeight());
						}
					}
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (HighlightItem != null) {
				MapOne();
			}
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			Color Black = new Color(0, 0, 0, 150);
			Image image = BoardInfo.GetImage().getScaledInstance(
					AppPreferences.getScaled(image_width,scale),
					AppPreferences.getScaled(image_height,scale),
					Image.SCALE_SMOOTH);
			if (image != null) {
				g.drawImage(image, 0, 0, null);
			}
			for (BoardRectangle rect : MappableComponents.GetMappedRectangles()) {
				boolean cadre = false;
				if (MappedHighlightItem != null) {
					if (MappedHighlightItem.equals(rect)) {
						g.setColor(Color.RED);
						cadre = true;
					} else {
						g.setColor(Black);
					}
				} else {
					g.setColor(Black);
				}
				g.fillRect(AppPreferences.getScaled(rect.getXpos(),scale),
						AppPreferences.getScaled(rect.getYpos(),scale),
						AppPreferences.getScaled(rect.getWidth(),scale),
						AppPreferences.getScaled(rect.getHeight(),scale));
				if (cadre) {
					g.setColor(Black);
					g.drawRect(AppPreferences.getScaled(rect.getXpos(),scale),
							AppPreferences.getScaled(rect.getYpos(),scale),
							AppPreferences.getScaled(rect.getWidth(),scale),
							AppPreferences.getScaled(rect.getHeight(),scale));
					if ((rect.getWidth() >= 4) && (rect.getHeight() >= 4)) {
						g.drawRect(AppPreferences.getScaled(rect.getXpos(),scale) + 1,
								AppPreferences.getScaled(rect.getYpos(),scale) + 1,
								AppPreferences.getScaled(rect.getWidth(),scale) - 2,
								AppPreferences.getScaled(rect.getHeight(),scale) - 2);
					}
				}
			}
			Color test = new Color(255, 0, 0, 100);
			for (BoardRectangle rect : SelectableItems) {
				g.setColor(test);
				g.fillRect(AppPreferences.getScaled(rect.getXpos(),scale),
						AppPreferences.getScaled(rect.getYpos(),scale),
						AppPreferences.getScaled(rect.getWidth(),scale),
						AppPreferences.getScaled(rect.getHeight(),scale));
			}
			if (HighlightItem != null && ComponentSelectionMode) {
				g.setColor(Color.RED);
				g.fillRect(AppPreferences.getScaled(HighlightItem.getXpos(),scale),
						AppPreferences.getScaled(HighlightItem.getYpos(),scale),
						AppPreferences.getScaled(HighlightItem.getWidth(),scale),
						AppPreferences.getScaled(HighlightItem.getHeight(),scale));
			}
			if (Note != null) {
				Note.Paint(g);
			}
		}
	}

	private static class XMLFileFilter extends FileFilter {

		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(".xml")
					|| f.getName().endsWith(".XML");
		}

		@Override
		public String getDescription() {
			return Strings.get("XMLFileFilter"); // TODO: language adaptation
		}
	}

	final static Logger logger = LoggerFactory
			.getLogger(ComponentMapDialog.class);

	private class ZoomChange implements ChangeListener {

		private ComponentMapDialog parent;

		public ZoomChange(ComponentMapDialog parent) {
			this.parent = parent;
		}
		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider)e.getSource();
			if (!source.getValueIsAdjusting()) {
				int value = source.getValue();
				if (value > MaxZoom) {
					source.setValue(MaxZoom);
					value = MaxZoom;
				}
				parent.SetScale(value/(float)100.0);
			}
		}

	}

	private JDialog panel;
	private boolean doneAssignment = false;
	private JButton UnMapButton = new JButton();
	private JButton UnMapAllButton = new JButton();
	private JButton DoneButton = new JButton();
	private JButton SaveButton = new JButton();
	private JButton CancelButton = new JButton();
	private JButton LoadButton = new JButton();
	private ZoomSlider ScaleButton = new ZoomSlider();
	private JLabel MessageLine = new JLabel();
	private JScrollPane UnMappedPane;
	private JScrollPane MappedPane;
	@SuppressWarnings("rawtypes")
	private JList UnmappedList;
	@SuppressWarnings("rawtypes")
	private JList MappedList;
	private SelectionWindow BoardPic;
	private boolean ComponentSelectionMode;
	private BoardRectangle HighlightItem = null;
	private BoardRectangle MappedHighlightItem = null;
	private int image_width = 740;
	private int image_height = 400;
	private float scale = 1;
	private int MaxZoom;
	private BoardInformation BoardInfo;
	private ArrayList<BoardRectangle> SelectableItems = new ArrayList<BoardRectangle>();
	private String OldDirectory = "";
	private String[] MapSectionStrings = { "Key", "LocationX", "LocationY",
			"Width", "Height" };

	private MappedComponentIdContainer Note;

	private MappableResourcesContainer MappableComponents;

	private MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseClicked(MouseEvent e) {
			if ((e.getClickCount() == 2)
					&& (e.getButton() == MouseEvent.BUTTON1)
					&& (UnmappedList.getSelectedValue() != null)) {
				int idx = UnmappedList.getSelectedIndex();
				String item = UnmappedList.getSelectedValue().toString();
				MappableComponents.ToggleAlternateMapping(item);
				RebuildSelectionLists();
				UnmappedList.setSelectedIndex(idx);
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}
	};

	public void SetScale(float scale) {
		this.scale = scale;
		BoardPic.setPreferredSize(new Dimension(BoardPic.getWidth(), BoardPic
				.getHeight()));
		BoardPic.setSize(new Dimension(BoardPic.getWidth(), BoardPic.getHeight()));
		UnMappedPane.setPreferredSize(new Dimension(
				BoardPic.getWidth()/3, 6*DoneButton.getHeight()+ScaleButton.getHeight()));
		MappedPane.setPreferredSize(new Dimension(
				BoardPic.getWidth()/3, 6*DoneButton.getHeight()+ScaleButton.getHeight()));
		panel.pack();
	}

	@SuppressWarnings("rawtypes")
	public ComponentMapDialog(JFrame parrentFrame, String projectPath) {

		OldDirectory = projectPath;

		panel = new JDialog(parrentFrame, ModalityType.APPLICATION_MODAL);
		panel.setTitle("Component to FPGA board mapping");
		panel.setResizable(false);
		panel.setAlwaysOnTop(true);
		panel.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

		GridBagLayout thisLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(thisLayout);
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// panel.setLocation(mlocation.x, mlocation.y);

		/* Add the board Picture */
		BoardPic = new SelectionWindow();
		BoardPic.setPreferredSize(new Dimension(BoardPic.getWidth(), BoardPic
				.getHeight()));
		c.gridx = 0;

		/* Add some text */
		JLabel UnmappedText = new JLabel();
		UnmappedText.setText("Unmapped List:");
		UnmappedText.setHorizontalTextPosition(JLabel.CENTER);
		UnmappedText.setPreferredSize(new Dimension(
				BoardPic.getWidth()/3, AppPreferences.getScaled(25)));
		UnmappedText
		.setToolTipText("<html>Select component and place it on the board.<br>"
				+ "To expand component (Port, DIP, ...) or change type (Button<->Pin),<br>"
				+ "double clic on it.</html>");
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		panel.add(UnmappedText, c);
		JLabel MappedText = new JLabel();
		MappedText.setText("Mapped List:");
		MappedText.setHorizontalTextPosition(JLabel.CENTER);
		MappedText.setPreferredSize(new Dimension(
				BoardPic.getWidth()/3, AppPreferences.getScaled(25)));
		c.gridx = 1;
		panel.add(MappedText, c);
		JLabel CommandText = new JLabel();
		CommandText.setText("Command:");
		CommandText.setHorizontalTextPosition(JLabel.CENTER);
		CommandText.setPreferredSize(new Dimension(
				BoardPic.getWidth()/3, AppPreferences.getScaled(25)));
		c.gridx = 2;
		panel.add(CommandText, c);

		/* Add the Zoom button */
		c.gridx = 2;
		c.gridy = 8;
		panel.add(ScaleButton, c);
		ScaleButton.addChangeListener(new ZoomChange(this));

		UnMapButton.setText("Release component");
		UnMapButton.setActionCommand("UnMap");
		UnMapButton.addActionListener(this);
		UnMapButton.setEnabled(false);
		c.gridx = 2;
		c.gridy = 2;
		panel.add(UnMapButton, c);

		/* Add the UnMapAll button */
		UnMapAllButton.setText("Release all components");
		UnMapAllButton.setActionCommand("UnMapAll");
		UnMapAllButton.addActionListener(this);
		UnMapAllButton.setEnabled(false);
		c.gridy = 3;
		panel.add(UnMapAllButton, c);

		/* Add the Load button */
		LoadButton.setText("Load Map");
		LoadButton.setActionCommand("Load");
		LoadButton.addActionListener(this);
		LoadButton.setEnabled(true);
		c.gridy = 4;
		panel.add(LoadButton, c);

		/* Add the Save button */
		SaveButton.setText("Save Map");
		SaveButton.setActionCommand("Save");
		SaveButton.addActionListener(this);
		SaveButton.setEnabled(false);
		c.gridy = 5;
		panel.add(SaveButton, c);

		/* Add the Cancel button */
		CancelButton.setText("Cancel");
		CancelButton.setActionCommand("Cancel");
		CancelButton.addActionListener(this);
		CancelButton.setEnabled(true);
		c.gridy = 6;
		panel.add(CancelButton, c);

		/* Add the Done button */
		DoneButton.setText("Done");
		DoneButton.setActionCommand("Done");
		DoneButton.addActionListener(this);
		DoneButton.setEnabled(false);
		c.gridy = 7;
		panel.add(DoneButton, c);

		/* Add the unmapped list */
		UnmappedList = new JList();
		UnmappedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		UnmappedList.addListSelectionListener(this);
		UnmappedList.addMouseListener(mouseListener);
		UnMappedPane = new JScrollPane(UnmappedList);
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 8;
		panel.add(UnMappedPane, c);
		ComponentSelectionMode = false;

		/* Add the mapped list */
		MappedList = new JList();
		MappedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		MappedList.addListSelectionListener(this);
		MappedPane = new JScrollPane(MappedList);
		c.gridx = 1;
		c.gridy = 1;
		c.gridheight = 8;
		panel.add(MappedPane, c);

		/* Add the message line */
		MessageLine.setForeground(Color.BLUE);
		MessageLine.setText("No messages");
		MessageLine.setEnabled(true);
		c.gridx = 0;
		c.gridy = 9;
		c.gridwidth = 3;
		c.gridheight = 1;
		panel.add(MessageLine, c);

		c.gridy = 10;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.CENTER;
		panel.add(BoardPic, c);

		panel.pack();
		/*
		 * panel.setLocation(Projects.getCenteredLoc(panel.getWidth(),
		 * panel.getHeight()));
		 */
		panel.setLocationRelativeTo(null);
		panel.setVisible(false);
		UnMappedPane.setPreferredSize(new Dimension(
				BoardPic.getWidth()/3, 6*DoneButton.getHeight()+ScaleButton.getHeight()));
		MappedPane.setPreferredSize(new Dimension(
				BoardPic.getWidth()/3, 6*DoneButton.getHeight()+ScaleButton.getHeight()));
		panel.pack();
		int ScreenWidth = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		int ScreenHeight = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		int ImageWidth = BoardPic.getWidth();
		int ImageHeight = BoardPic.getHeight();
		int ImageXBorder = panel.getWidth()-ImageWidth;
		int ImageYBorder = panel.getHeight()-ImageHeight;
		ScreenWidth -= ImageXBorder;
		ScreenHeight -= (ImageYBorder+(ImageYBorder>>2));
		int zoomX = (ScreenWidth*100)/ImageWidth;
		int zoomY = (ScreenHeight*100)/ImageHeight;
		MaxZoom = (zoomY > zoomX) ? zoomX : zoomY;
		if (MaxZoom < 100)
			MaxZoom = 100;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Done")) {
			doneAssignment = true;
			panel.setVisible(false);
		} else if (e.getActionCommand().equals("UnMapAll")) {
			doneAssignment = false;
			UnMapAll();
		} else if (e.getActionCommand().equals("UnMap")) {
			doneAssignment = false;
			UnMapOne();
		} else if (e.getActionCommand().equals("Save")) {
			Save();
		} else if (e.getActionCommand().equals("Load")) {
			Load();
		} else if (e.getActionCommand().equals("Cancel")) {
			doneAssignment = false;
			panel.dispose();
		}
	}

	private void ClearSelections() {
		MappedHighlightItem = null;
		HighlightItem = null;
		UnMapButton.setEnabled(false);
		MappedList.clearSelection();
		UnmappedList.clearSelection();
		ComponentSelectionMode = false;
		SelectableItems.clear();
	}

	private String getDirName(String window_name) {
		JFileChooser fc = new JFileChooser(OldDirectory);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setDialogTitle(window_name);
		if (!OldDirectory.isEmpty()) {
			File SelFile = new File(OldDirectory);
			fc.setSelectedFile(SelFile);
		}
		FileFilter ff = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}

			@Override
			public String getDescription() {
				return "Select Directory";
			}
		};
		fc.setFileFilter(ff);
		fc.setAcceptAllFileFilterUsed(false);
		int retval = fc.showOpenDialog(null);
		if (retval == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			OldDirectory = file.getPath();
			if (!OldDirectory.endsWith(File.separator)) {
				OldDirectory += File.separator;
			}
			return OldDirectory;
		} else {
			return "";
		}
	}

	public boolean isDoneAssignment() {
		return doneAssignment;
	}

	private void Load() {
		JFileChooser fc = new JFileChooser(OldDirectory);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.setDialogTitle("Choose XML board description file to use");
		FileFilter XML_FILTER = new XMLFileFilter();
		fc.setFileFilter(XML_FILTER);
		fc.setAcceptAllFileFilterUsed(false);
		panel.setVisible(false);
		int retval = fc.showOpenDialog(null);
		if (retval == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			String FileName = file.getName();
			String AbsoluteFileName = file.getPath();
			OldDirectory = AbsoluteFileName.substring(0,
					AbsoluteFileName.length() - FileName.length());
			try {
				// Create instance of DocumentBuilderFactory
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				// Get the DocumentBuilder
				DocumentBuilder parser = factory.newDocumentBuilder();
				// Create blank DOM Document
				File xml = new File(AbsoluteFileName);
				Document MapDoc = parser.parse(xml);
				NodeList Elements = MapDoc
						.getElementsByTagName("LogisimGoesFPGABoardMapInformation");
				Node CircuitInfo = Elements.item(0);
				NodeList CircuitInfoDetails = CircuitInfo.getChildNodes();
				for (int i = 0; i < CircuitInfoDetails.getLength(); i++) {
					if (CircuitInfoDetails.item(i).getNodeName()
							.equals("GlobalMapInformation")) {
						NamedNodeMap Attrs = CircuitInfoDetails.item(i)
								.getAttributes();
						for (int j = 0; j < Attrs.getLength(); j++) {
							if (Attrs.item(j).getNodeName().equals("BoardName")) {
								if (!BoardInfo.getBoardName().equals(
										Attrs.item(j).getNodeValue())) {
									MessageLine.setForeground(Color.RED);
									MessageLine
									.setText("LOAD ERROR: The selected Map file is not for the selected target board!");
									panel.setVisible(true);
									return;
								}
							} else if (Attrs.item(j).getNodeName()
									.equals("ToplevelCircuitName")) {
								if (!MappableComponents.GetToplevelName()
										.equals(Attrs.item(j).getNodeValue())) {
									MessageLine.setForeground(Color.RED);
									MessageLine
									.setText("LOAD ERROR: The selected Map file is not for the selected toplevel circuit!");
									panel.setVisible(true);
									return;
								}
							}
						}
						break;
					}
				}
				/* cleanup the current map */
				UnMapAll();
				for (int i = 0; i < CircuitInfoDetails.getLength(); i++) {
					if (CircuitInfoDetails.item(i).getNodeName()
							.startsWith("MAPPEDCOMPONENT")) {
						int x = -1, y = -1, width = -1, height = -1;
						String key = "";
						NamedNodeMap Attrs = CircuitInfoDetails.item(i)
								.getAttributes();
						for (int j = 0; j < Attrs.getLength(); j++) {
							if (Attrs.item(j).getNodeName()
									.equals(MapSectionStrings[0])) {
								key = Attrs.item(j).getNodeValue();
							}
							if (Attrs.item(j).getNodeName()
									.equals(MapSectionStrings[1])) {
								x = Integer.parseInt(Attrs.item(j)
										.getNodeValue());
							}
							if (Attrs.item(j).getNodeName()
									.equals(MapSectionStrings[2])) {
								y = Integer.parseInt(Attrs.item(j)
										.getNodeValue());
							}
							if (Attrs.item(j).getNodeName()
									.equals(MapSectionStrings[3])) {
								width = Integer.parseInt(Attrs.item(j)
										.getNodeValue());
							}
							if (Attrs.item(j).getNodeName()
									.equals(MapSectionStrings[4])) {
								height = Integer.parseInt(Attrs.item(j)
										.getNodeValue());
							}
						}
						if (!key.isEmpty() && (x > 0) && (y > 0) && (width > 0)
								&& (height > 0)) {
							BoardRectangle rect = null;
							for (FPGAIOInformationContainer comp : BoardInfo
									.GetAllComponents()) {
								if ((comp.GetRectangle().getXpos() == x)
										&& (comp.GetRectangle().getYpos() == y)
										&& (comp.GetRectangle().getWidth() == width)
										&& (comp.GetRectangle().getHeight() == height)) {
									rect = comp.GetRectangle();
									break;
								}
							}
							if (rect != null) {
								MappableComponents.TryMap(key, rect,
										BoardInfo.GetComponentType(rect));
							}
						}
					}
				}
				ClearSelections();
				RebuildSelectionLists();
				BoardPic.paintImmediately(0,0,BoardPic.getWidth(),BoardPic.getHeight());
			} catch (Exception e) {
				/* TODO: handle exceptions */
				logger.error(
						"Exceptions not handled yet in Load(), but got an exception: {}",
						e.getMessage());
			}
		}
		panel.setVisible(true);
	}

	private void MapOne() {
		if (UnmappedList.getSelectedIndex() >= 0) {
			String key = UnmappedList.getSelectedValue().toString();
			if (HighlightItem != null) {
				MappableComponents.Map(key, HighlightItem,
						BoardInfo.GetComponentType(HighlightItem));
				RebuildSelectionLists();
			}
		} else if (MappedList.getSelectedIndex() >= 0) {
			String key = MappedList.getSelectedValue().toString();
			if (HighlightItem != null) {
				MappableComponents.Map(key, HighlightItem,
						BoardInfo.GetComponentType(HighlightItem));
			}
		}
		ClearSelections();
		BoardPic.paintImmediately(0,0,BoardPic.getWidth(),BoardPic.getHeight());
		UnmappedList.setSelectedIndex(0);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void RebuildSelectionLists() {
		UnmappedList.clearSelection();
		MappedList.clearSelection();
		Set<String> Unmapped = MappableComponents.UnmappedList();
		Set<String> Mapped = MappableComponents.MappedList();
		JList unmapped = new JList(Unmapped.toArray());
		UnmappedList.setModel(unmapped.getModel());
		JList mapped = new JList(Mapped.toArray());
		MappedList.setModel(mapped.getModel());
		UnmappedList.paintImmediately(0, 0, UnmappedList.getBounds().width,
				UnmappedList.getBounds().height);
		MappedList.paintImmediately(0, 0, MappedList.getBounds().width,
				MappedList.getBounds().height);
		UnMapAllButton.setEnabled(!Mapped.isEmpty());
		CancelButton.setEnabled(true);
		DoneButton.setEnabled(Unmapped.isEmpty());
		SaveButton.setEnabled(!Mapped.isEmpty());
	}

	private void Save() {
		panel.setVisible(false);
		String SelectedDir = getDirName("Select Directory to save the current map");
		if (!SelectedDir.isEmpty()) {
			String SaveFileName = SelectedDir
					+ CorrectLabel.getCorrectLabel(MappableComponents
							.GetToplevelName()) + "-"
							+ BoardInfo.getBoardName() + "-MAP.xml";
			try {
				// Create instance of DocumentBuilderFactory
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				// Get the DocumentBuilder
				DocumentBuilder parser = factory.newDocumentBuilder();
				// Create blank DOM Document
				Document MapInfo = parser.newDocument();

				Element root = MapInfo
						.createElement("LogisimGoesFPGABoardMapInformation");
				MapInfo.appendChild(root);
				Element CircuitInfo = MapInfo
						.createElement("GlobalMapInformation");
				CircuitInfo.setAttribute("BoardName", BoardInfo.getBoardName());
				Attr circ = MapInfo.createAttribute("ToplevelCircuitName");
				circ.setNodeValue(MappableComponents.GetToplevelName());
				CircuitInfo.setAttributeNode(circ);
				root.appendChild(CircuitInfo);
				int count = 1;
				for (String key : MappableComponents.MappedList()) {
					Element Map = MapInfo.createElement("MAPPEDCOMPONENT_"
							+ Integer.toHexString(count++));
					BoardRectangle rect = MappableComponents.GetMap(key);
					Map.setAttribute(MapSectionStrings[0], key);
					Attr xpos = MapInfo.createAttribute(MapSectionStrings[1]);
					xpos.setValue(Integer.toString(rect.getXpos()));
					Map.setAttributeNode(xpos);
					Attr ypos = MapInfo.createAttribute(MapSectionStrings[2]);
					ypos.setValue(Integer.toString(rect.getYpos()));
					Map.setAttributeNode(ypos);
					Attr width = MapInfo.createAttribute(MapSectionStrings[3]);
					width.setValue(Integer.toString(rect.getWidth()));
					Map.setAttributeNode(width);
					Attr height = MapInfo.createAttribute(MapSectionStrings[4]);
					height.setValue(Integer.toString(rect.getHeight()));
					Map.setAttributeNode(height);
					root.appendChild(Map);
				}
				TransformerFactory tranFactory = TransformerFactory
						.newInstance();
				tranFactory.setAttribute("indent-number", 3);
				Transformer aTransformer = tranFactory.newTransformer();
				aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
				Source src = new DOMSource(MapInfo);
				File file = new File(SaveFileName);
				Result dest = new StreamResult(file);
				aTransformer.transform(src, dest);
			} catch (Exception e) {
				/* TODO: handle exceptions */
				logger.error(
						"Exceptions not handled yet in Save(), but got an exception: {}",
						e.getMessage());
			}
		}
		panel.setVisible(true);
	}

	public void SetBoardInformation(BoardInformation Board) {
		BoardInfo = Board;
	}

	public void SetMappebleComponents(MappableResourcesContainer mappable) {
		MappableComponents = mappable;
		RebuildSelectionLists();
		ClearSelections();
	}

	public void SetVisible(boolean selection) {
		MessageLine.setForeground(Color.BLUE);
		MessageLine.setText("No messages");
		panel.setVisible(selection);
	}

	private void UnMapAll() {
		ClearSelections();
		MappableComponents.UnmapAll();
		MappableComponents.rebuildMappedLists();
		BoardPic.paintImmediately(0,0,BoardPic.getWidth(),BoardPic.getHeight());
		RebuildSelectionLists();
	}

	private void UnMapOne() {
		if (MappedList.getSelectedIndex() >= 0) {
			String key = MappedList.getSelectedValue().toString();
			MappableComponents.UnMap(key);
			ClearSelections();
			RebuildSelectionLists();
			BoardPic.paintImmediately(0,0,BoardPic.getWidth(),BoardPic.getHeight());
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() == MappedList) {
			if (MappedList.getSelectedIndex() >= 0) {
				UnmappedList.clearSelection();
				UnMapButton.setEnabled(true);
				MappedHighlightItem = MappableComponents.GetMap(MappedList
						.getSelectedValue().toString());
				BoardPic.paintImmediately(0,0,BoardPic.getWidth(),BoardPic.getHeight());
				ComponentSelectionMode = true;
				SelectableItems.clear();
				String DisplayName = MappedList.getSelectedValue().toString();
				SelectableItems = MappableComponents.GetSelectableItemsList(
						DisplayName, BoardInfo);
				BoardPic.paintImmediately(0,0,BoardPic.getWidth(),BoardPic.getHeight());
			} else {
				ComponentSelectionMode = false;
				SelectableItems.clear();
				Note = null;
				MappedHighlightItem = null;
				HighlightItem = null;
			}
		} else if (e.getSource() == UnmappedList) {
			if (UnmappedList.getSelectedIndex() < 0) {
				ComponentSelectionMode = false;
				SelectableItems.clear();
				Note = null;
				MappedHighlightItem = null;
				HighlightItem = null;
			} else {
				MappedList.clearSelection();
				ComponentSelectionMode = true;
				SelectableItems.clear();
				String DisplayName = UnmappedList.getSelectedValue().toString();
				SelectableItems = MappableComponents.GetSelectableItemsList(
						DisplayName, BoardInfo);
			}
			MappedHighlightItem = null;
			UnMapButton.setEnabled(false);
			CancelButton.setEnabled(true);
			BoardPic.paintImmediately(0,0,BoardPic.getWidth(),BoardPic.getHeight());
		}
	}

}
