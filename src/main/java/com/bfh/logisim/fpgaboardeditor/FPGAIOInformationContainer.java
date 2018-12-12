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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.bfh.logisim.settings.VendorSoftware;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.std.io.DipSwitch;
import com.cburch.logisim.std.io.PortIO;
import com.cburch.logisim.std.io.RGBLed;
import com.cburch.logisim.std.io.ReptarLocalBus;
import com.cburch.logisim.std.io.SevenSegment;

public class FPGAIOInformationContainer {

	public static enum IOComponentTypes {

		LED, Button, Pin, SevenSegment, DIPSwitch, RGBLED, PortIO, LocalBus, Bus, Unknown;
		public static IOComponentTypes getEnumFromString(String str) {
			for (IOComponentTypes elem : KnownComponentSet) {
				if (elem.name().equalsIgnoreCase(str)) {
					return elem;
				}
			}
			return IOComponentTypes.Unknown;
		}

		/* AMX: Localbus / Port IO / Pin led buton information about the number of input pins.
		 * This is the wrong way to do it. It should be taken from the Xml file!! */
		public static final int GetFPGAInOutRequirement(IOComponentTypes comp) {
			switch (comp) {
			case PortIO:
				return nbSwitch;
			case LocalBus:
				return 16;
			default:
				return 0;
			}
		}

		/* AMX: Localbus / Port IO / Pin led buton information about the number of input pins.
		 * This is the wrong way to do it. It should be taken from the Xml file!! */
		public static final int GetFPGAInputRequirement(IOComponentTypes comp) {
			switch (comp) {
			case Button:
				return 1;
			case DIPSwitch:
				return nbSwitch;
			case LocalBus:
				return 13;
			default:
				return 0;
			}
		}

		/* AMX: Localbus / Port IO / Pin led buton information about the number of output pins.
		 * This is the wrong way to do it. It should be taken from the Xml file!! */
		public static final int GetFPGAOutputRequirement(IOComponentTypes comp) {
			switch (comp) {
			case LED:
				return 1;
			case SevenSegment:
				return 8;
			case RGBLED:
				return 3;
			case LocalBus:
				return 2;
			default:
				return 0;
			}
		}

		/* AMX: Localbus / Port IO / Pin led buton information about the total of pins pins.
		 * This is the wrong way to do it. It should be taken from the Xml file!! */
		public static final int GetNrOfFPGAPins(IOComponentTypes comp) {
			switch (comp) {
			case LED:
			case Button:
			case Pin:
				return 1;
			case DIPSwitch:
			case PortIO:
				return nbSwitch;
			case SevenSegment:
				return 8;
			case RGBLED:
				return 3;
			case LocalBus:
				return 31;
			default:
				return 0;
			}
		}

		public static final EnumSet<IOComponentTypes> KnownComponentSet = EnumSet
				.range(IOComponentTypes.LED, IOComponentTypes.LocalBus);

		public static final EnumSet<IOComponentTypes> SimpleInputSet = EnumSet
				.range(IOComponentTypes.LED, IOComponentTypes.LocalBus);

		public static final EnumSet<IOComponentTypes> InputComponentSet = EnumSet
				.of(IOComponentTypes.Button, IOComponentTypes.Pin,
						IOComponentTypes.DIPSwitch);

		public static final EnumSet<IOComponentTypes> OutputComponentSet = EnumSet
				.of(IOComponentTypes.LED, IOComponentTypes.Pin,
						IOComponentTypes.RGBLED, IOComponentTypes.SevenSegment);

		public static final EnumSet<IOComponentTypes> InOutComponentSet = EnumSet
				.of(IOComponentTypes.Pin, IOComponentTypes.PortIO);

		private static int nbSwitch = 8;

		private void setNbSwitch(int nb) {
			nbSwitch = nb;
		}
	}

	/*
	 * Bus is just a placeholder for a multi-bit pin. It should not be used for
	 * mappable components
	 */

	public static LinkedList<String> GetComponentTypes() {
		LinkedList<String> result = new LinkedList<String>();
		for (IOComponentTypes comp : IOComponentTypes.KnownComponentSet) {
			result.add(comp.toString());
		}
		return result;
	};

	final static Logger logger = LoggerFactory
			.getLogger(FPGAIOInformationContainer.class);

	private IOComponentTypes MyType;
	private long MyIdentifier;
	private BoardRectangle MyRectangle;
	private Map<Integer, String> MyPinLocations;
	private Integer NrOfPins;
	private char MyPullBehavior;
	private char MyActivityLevel;
	private char MyIOStandard;
	private char MyDriveStrength;

	private boolean abort = false;

	public FPGAIOInformationContainer() {
		MyType = IOComponentTypes.Unknown;
		MyIdentifier = -1;
		MyRectangle = null;
		MyPinLocations = new HashMap<Integer, String>();
		NrOfPins = 0;
		MyPullBehavior = PullBehaviors.Unknown;
		MyActivityLevel = PinActivity.Unknown;
		MyIOStandard = IoStandards.Unknown;
		MyDriveStrength = DriveStrength.Unknown;
	}

	public FPGAIOInformationContainer(IOComponentTypes Type,
			BoardRectangle rect, BoardDialog parent) {
		MyType = Type;
		MyIdentifier = -1;
		MyRectangle = rect;
		MyPinLocations = new HashMap<Integer, String>();
		NrOfPins = 0;
		MyPullBehavior = PullBehaviors.Unknown;
		MyActivityLevel = PinActivity.Unknown;
		MyIOStandard = IoStandards.Unknown;
		MyDriveStrength = DriveStrength.Unknown;
		if (IOComponentTypes.SimpleInputSet.contains(Type)) {
			if (MyType.equals(IOComponentTypes.DIPSwitch)
					|| MyType.equals(IOComponentTypes.PortIO)) {
				GetSizeInformationDialog(parent);
			}
			GetSimpleInformationDialog(parent);
			return;
		}

		MyType = IOComponentTypes.Unknown;
	}

	public FPGAIOInformationContainer(IOComponentTypes Type,
			BoardRectangle rect, String loc, String pull, String active,
			String standard, String drive) {
		this.Set(Type, rect, loc, pull, active, standard, drive);
	}

	public FPGAIOInformationContainer(Node DocumentInfo) {
		/*
		 * This constructor is used to create an element during the reading of a
		 * board information xml file
		 */
		MyType = IOComponentTypes.Unknown;
		MyIdentifier = -1;
		MyRectangle = null;
		MyPinLocations = new HashMap<Integer, String>();
		NrOfPins = 0;
		MyPullBehavior = PullBehaviors.Unknown;
		MyActivityLevel = PinActivity.Unknown;
		MyIOStandard = IoStandards.Unknown;
		MyDriveStrength = DriveStrength.Unknown;
		IOComponentTypes SetId = IOComponentTypes
				.getEnumFromString(DocumentInfo.getNodeName());
		if (IOComponentTypes.KnownComponentSet.contains(SetId)) {
			MyType = SetId;
		} else {
			return;
		}
		NamedNodeMap Attrs = DocumentInfo.getAttributes();
		int x = -1, y = -1, width = -1, height = -1;
		for (int i = 0; i < Attrs.getLength(); i++) {
			Node ThisAttr = Attrs.item(i);
			if (ThisAttr.getNodeName().equals(BoardWriterClass.LocationXString)) {
				x = Integer.parseInt(ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName().equals(BoardWriterClass.LocationYString)) {
				y = Integer.parseInt(ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName().equals(BoardWriterClass.WidthString)) {
				width = Integer.parseInt(ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName().equals(BoardWriterClass.HeightString)) {
				height = Integer.parseInt(ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName().equals(
					BoardWriterClass.PinLocationString)) {
				NrOfPins = 1;
				MyPinLocations.put(0, ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName().equals(
					BoardWriterClass.MultiPinInformationString)) {
				NrOfPins = Integer.parseInt(ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName().startsWith(
					BoardWriterClass.MultiPinPrefixString)) {
				String Id = ThisAttr.getNodeName().substring(
						BoardWriterClass.MultiPinPrefixString.length());
				MyPinLocations.put(Integer.parseInt(Id),
						ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName().equals(
					DriveStrength.DriveAttributeString)) {
				MyDriveStrength = DriveStrength.getId(ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName()
					.equals(PullBehaviors.PullAttributeString)) {
				MyPullBehavior = PullBehaviors.getId(ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName().equals(IoStandards.IOAttributeString)) {
				MyIOStandard = IoStandards.getId(ThisAttr.getNodeValue());
			}
			if (ThisAttr.getNodeName().equals(
					PinActivity.ActivityAttributeString)) {
				MyActivityLevel = PinActivity.getId(ThisAttr.getNodeValue());
			}
		}
		if ((x < 0) || (y < 0) || (width < 1) || (height < 1)) {
			MyType = IOComponentTypes.Unknown;
			return;
		}
		boolean PinsComplete = true;
		for (int i = 0; i < NrOfPins; i++) {
			if (!MyPinLocations.containsKey(i)) {
				logger.warn("Bizar missing pin {} of component!", i);
				PinsComplete = false;
			}
		}
		if (!PinsComplete) {
			MyType = IOComponentTypes.Unknown;
			return;
		}
		if (MyType.equals(IOComponentTypes.DIPSwitch)
				|| MyType.equals(IOComponentTypes.PortIO)) {
			MyType.setNbSwitch(NrOfPins);
		}
		if (MyType.equals(IOComponentTypes.Pin))
			MyActivityLevel = PinActivity.ActiveHigh;
		MyRectangle = new BoardRectangle(x, y, width, height);
	}

	public void edit(BoardDialog parent) {
		if (!defined())
			return;
		if (MyType.equals(IOComponentTypes.DIPSwitch)
				|| MyType.equals(IOComponentTypes.PortIO)) {
			GetSizeInformationDialog(parent);
		}
		GetSimpleInformationDialog(parent);
	}

	public Boolean defined() {
		return MyIdentifier != -1;
	}

	public char GetActivityLevel() {
		return MyActivityLevel;
	}

	private ArrayList<String> GetAlteraPinStrings(String direction, int StartId) {
		/*
		 * for the time being we ignore the InputPins variable. It has to be
		 * implemented for more complex components
		 */
		ArrayList<String> Contents = new ArrayList<String>();
		for (int i = 0; i < NrOfPins; i++) {
			String NetName = "";
			if (direction == "in") {
				NetName = HDLGeneratorFactory.FPGAInputPinName + "_"
						+ Integer.toString(StartId + i);
			} else if (direction == "inout") {
				NetName = HDLGeneratorFactory.FPGAInOutPinName + "_"
						+ Integer.toString(StartId + i);
			} else {
				NetName = HDLGeneratorFactory.FPGAOutputPinName + "_"
						+ Integer.toString(StartId + i);
			}
			// String NetName = (InputPins) ?
			// HDLGeneratorFactory.FPGAInputPinName + "_" +
			// Integer.toString(StartId + i)
			// : HDLGeneratorFactory.FPGAOutputPinName + "_" +
			// Integer.toString(StartId + i);
			Contents.add("    set_location_assignment " + MyPinLocations.get(i)
			+ " -to " + NetName);
			if (MyPullBehavior == PullBehaviors.PullUp) {
				Contents.add("    set_instance_assignment -name WEAK_PULL_UP_RESISTOR ON -to " + NetName);
			}
		}
		return Contents;
	}

	public Element GetDocumentElement(Document doc) {
		if (MyType.equals(IOComponentTypes.Unknown)) {
			return null;
		}
		try {
			Element result = doc.createElement(MyType.toString());
			result.setAttribute(BoardWriterClass.LocationXString,
					Integer.toString(MyRectangle.getXpos()));
			Attr ypos = doc.createAttribute(BoardWriterClass.LocationYString);
			ypos.setValue(Integer.toString(MyRectangle.getYpos()));
			result.setAttributeNode(ypos);
			Attr width = doc.createAttribute(BoardWriterClass.WidthString);
			width.setValue(Integer.toString(MyRectangle.getWidth()));
			result.setAttributeNode(width);
			Attr height = doc.createAttribute(BoardWriterClass.HeightString);
			height.setValue(Integer.toString(MyRectangle.getHeight()));
			result.setAttributeNode(height);
			if (NrOfPins == 1) {
				Attr loc = doc
						.createAttribute(BoardWriterClass.PinLocationString);
				loc.setValue(MyPinLocations.get(0));
				result.setAttributeNode(loc);
			} else {
				Attr NrPins = doc
						.createAttribute(BoardWriterClass.MultiPinInformationString);
				NrPins.setValue(NrOfPins.toString());
				result.setAttributeNode(NrPins);
				for (int i = 0; i < NrOfPins; i++) {
					String PinName = BoardWriterClass.MultiPinPrefixString
							+ Integer.toString(i);
					Attr PinX = doc.createAttribute(PinName);
					PinX.setValue(MyPinLocations.get(i));
					result.setAttributeNode(PinX);
				}
			}
			if (MyDriveStrength != DriveStrength.Unknown) {
				Attr drive = doc
						.createAttribute(DriveStrength.DriveAttributeString);
				drive.setValue(DriveStrength.Behavior_strings[MyDriveStrength]);
				result.setAttributeNode(drive);
			}
			if (MyPullBehavior != PullBehaviors.Unknown) {
				Attr pull = doc
						.createAttribute(PullBehaviors.PullAttributeString);
				pull.setValue(PullBehaviors.Behavior_strings[MyPullBehavior]);
				result.setAttributeNode(pull);
			}
			if (MyIOStandard != IoStandards.Unknown) {
				Attr stand = doc.createAttribute(IoStandards.IOAttributeString);
				stand.setValue(IoStandards.Behavior_strings[MyIOStandard]);
				result.setAttributeNode(stand);
			}
			if (MyActivityLevel != PinActivity.Unknown) {
				Attr act = doc
						.createAttribute(PinActivity.ActivityAttributeString);
				act.setValue(PinActivity.Behavior_strings[MyActivityLevel]);
				result.setAttributeNode(act);
			}
			return result;
		} catch (Exception e) {
			/* TODO: handle exceptions */
			logger.error(
					"Exceptions not handled yet in GetDocumentElement(), but got an exception: {}",
					e.getMessage());
		}
		return null;
	}

	public char GetDrive() {
		return MyDriveStrength;
	}

	public long GetId() {
		return MyIdentifier;
	}

	public char GetIOStandard() {
		return MyIOStandard;
	}

	public int getNrOfPins() {
		return NrOfPins;
	}

	public ArrayList<String> GetPinlocStrings(int Vendor, String direction,
			int StartId) {
		if (Vendor == VendorSoftware.VendorXilinx) {
			return GetXilinxUCFStrings(direction, StartId);
		}
		if (Vendor == VendorSoftware.VendorAltera) {
			return GetAlteraPinStrings(direction, StartId);
		}
		if (Vendor == VendorSoftware.VendorVivado) {
			return GetVivadoXDCStrings(direction, StartId);
		}
		return new ArrayList<String>();
	}

	public char GetPullBehavior() {
		return MyPullBehavior;
	}

	public BoardRectangle GetRectangle() {
		return MyRectangle;
	}

	private void GetSimpleInformationDialog(BoardDialog parent) {
		int NrOfDevicePins = IOComponentTypes.GetNrOfFPGAPins(MyType);
		final JDialog selWindow = new JDialog(parent.GetPanel(), MyType
				+ " properties");
		JComboBox<String> DriveInput = new JComboBox<>(
				DriveStrength.Behavior_strings);
		JComboBox<String> PullInput = new JComboBox<>(
				PullBehaviors.Behavior_strings);
		JComboBox<String> ActiveInput = new JComboBox<>(
				PinActivity.Behavior_strings);
		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("cancel")) {
					MyType = IOComponentTypes.Unknown;
					abort = true;
				}
				selWindow.setVisible(false);
			}
		};
		GridBagLayout dialogLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		selWindow.setLayout(dialogLayout);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = -1;
		ArrayList<JTextField> LocInputs = new ArrayList<JTextField>();
		ArrayList<String> PinLabels;
		switch (MyType) {
		case SevenSegment:
			PinLabels = SevenSegment.GetLabels();
			break;
		case RGBLED:
			PinLabels = RGBLed.GetLabels();
			break;
		case DIPSwitch:
			PinLabels = DipSwitch.GetLabels(NrOfDevicePins);
			break;
		case PortIO:
			PinLabels = PortIO.GetLabels(NrOfDevicePins);
			break;
		case LocalBus:
			PinLabels = ReptarLocalBus.GetLabels();
			break;
		default:
			PinLabels = new ArrayList<String>();
			if (NrOfDevicePins == 1) {
				PinLabels.add("FPGA pin");
			} else {
				for (int i = 0; i < NrOfDevicePins; i++) {
					PinLabels.add("pin " + i);
				}
			}
		}
		int offset = 0;
		int oldY = c.gridy;
		int maxY = -1;
		for (int i = 0; i < NrOfDevicePins; i++) {
			if (i % 32 == 0) {
				offset = (i / 32) * 2;
				c.gridy = oldY;
			}
			JLabel LocText = new JLabel("Specify " + PinLabels.get(i)
			+ " location:");
			c.gridx = 0 + offset;
			c.gridy++;
			selWindow.add(LocText, c);
			JTextField txt = new JTextField(6);
			if (defined()) {
				txt.setText(MyPinLocations.get(i));
			}
			LocInputs.add(txt);
			c.gridx = 1 + offset;
			selWindow.add(LocInputs.get(i), c);
			maxY = c.gridy > maxY ? c.gridy : maxY;
		}
		c.gridy = maxY;

		JLabel StandardText = new JLabel("Specify FPGA pin standard:");
		c.gridy++;
		c.gridx = 0;
		selWindow.add(StandardText, c);
		JComboBox<String> StandardInput = new JComboBox<>(
				IoStandards.Behavior_strings);
		if (defined())
			StandardInput.setSelectedIndex(MyIOStandard);
		else
			StandardInput.setSelectedIndex(parent.GetDefaultStandard());
		c.gridx = 1;
		selWindow.add(StandardInput, c);

		if (IOComponentTypes.OutputComponentSet.contains(MyType)) {
			JLabel DriveText = new JLabel("Specify FPGA pin drive strength:");
			c.gridy++;
			c.gridx = 0;
			selWindow.add(DriveText, c);
			if (defined())
				DriveInput.setSelectedIndex(MyDriveStrength);
			else
				DriveInput.setSelectedIndex(parent.GetDefaultDriveStrength());
			c.gridx = 1;
			selWindow.add(DriveInput, c);
		}

		if (IOComponentTypes.InputComponentSet.contains(MyType)) {
			JLabel PullText = new JLabel("Specify FPGA pin pull behavior:");
			c.gridy++;
			c.gridx = 0;
			selWindow.add(PullText, c);
			if (defined())
				PullInput.setSelectedIndex(MyPullBehavior);
			else
				PullInput.setSelectedIndex(parent.GetDefaultPullSelection());
			c.gridx = 1;
			selWindow.add(PullInput, c);
		}

		if (!IOComponentTypes.InOutComponentSet.contains(MyType)) {
			JLabel ActiveText = new JLabel("Specify " + MyType + " activity:");
			c.gridy++;
			c.gridx = 0;
			selWindow.add(ActiveText, c);
			if (defined())
				ActiveInput.setSelectedIndex(MyActivityLevel);
			else
				ActiveInput.setSelectedIndex(parent.GetDefaultActivity());
			c.gridx = 1;
			selWindow.add(ActiveInput, c);
		}

		JButton OkayButton = new JButton("Done and Store");
		OkayButton.setActionCommand("done");
		OkayButton.addActionListener(actionListener);
		c.gridx = 0;
		c.gridy++;
		selWindow.add(OkayButton, c);

		JButton CancelButton = new JButton("Cancel");
		CancelButton.setActionCommand("cancel");
		CancelButton.addActionListener(actionListener);
		c.gridx = 1;
		selWindow.add(CancelButton, c);
		selWindow.pack();
		selWindow.setLocation(Projects.getCenteredLoc(selWindow.getWidth(),
				selWindow.getHeight()));
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// selWindow.setLocation(mlocation.x, mlocation.y);
		selWindow.setModal(true);
		selWindow.setResizable(false);
		selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		selWindow.setAlwaysOnTop(true);
		abort = false;
		while (!abort) {
			selWindow.setVisible(true);
			if (!abort) {
				boolean correct = true;
				for (int i = 0; i < NrOfDevicePins; i++) {
					if (LocInputs.get(i).getText().isEmpty()) {
						correct = false;
						showDialogNotification(selWindow, "Error",
								"<html>You have to specify a location for "
										+ PinLabels.get(i) + "!</html>");
						continue;
					}
				}
				if (correct) {
					parent.SetDefaultStandard(StandardInput.getSelectedIndex());
					NrOfPins = NrOfDevicePins;
					for (int i = 0; i < NrOfDevicePins; i++) {
						MyPinLocations.put(i, LocInputs.get(i).getText());
					}
					MyIOStandard = IoStandards.getId(StandardInput
							.getSelectedItem().toString());
					if (IOComponentTypes.OutputComponentSet.contains(MyType)) {
						parent.SetDefaultDriveStrength(DriveInput
								.getSelectedIndex());
						MyDriveStrength = DriveStrength.getId(DriveInput
								.getSelectedItem().toString());
					}
					if (IOComponentTypes.InputComponentSet.contains(MyType)) {
						parent.SetDefaultPullSelection(PullInput
								.getSelectedIndex());
						MyPullBehavior = PullBehaviors.getId(PullInput
								.getSelectedItem().toString());
					}
					if (!IOComponentTypes.InOutComponentSet.contains(MyType)) {
						parent.SetDefaultActivity(ActiveInput
								.getSelectedIndex());
						MyActivityLevel = PinActivity.getId(ActiveInput
								.getSelectedItem().toString());
					}
					abort = true;
				}
			}
		}
		selWindow.dispose();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void GetSizeInformationDialog(BoardDialog parent) {
		int NrOfDevicePins = IOComponentTypes.GetNrOfFPGAPins(MyType);
		int min = 1;
		int max = 1;
		String text = "null";

		switch (MyType) {
		case DIPSwitch:
			min = DipSwitch.MIN_SWITCH;
			max = DipSwitch.MAX_SWITCH;
			text = "switch";
			break;
		case PortIO:
			min = PortIO.MIN_IO;
			max = PortIO.MAX_IO;
			text = "pins";
			break;
		default:
			break;
		}

		final JDialog selWindow = new JDialog(parent.GetPanel(), MyType
				+ " number of " + text);
		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("next")) {
					MyType.setNbSwitch(Integer.valueOf(((JComboBox) (selWindow
							.getContentPane().getComponents()[1]))
							.getSelectedItem().toString()));
					// setNrOfPins(Integer.valueOf(((JComboBox)(selWindow.getContentPane().getComponents()[1])).getSelectedItem().toString()));
					selWindow.dispose();
				}
				selWindow.setVisible(false);
			}
		};

		JComboBox size = new JComboBox<>();
		for (int i = min; i <= max; i++) {
			size.addItem(i);
		}
		size.setSelectedItem(NrOfDevicePins);
		GridBagLayout dialogLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		selWindow.setLayout(dialogLayout);
		c.fill = GridBagConstraints.HORIZONTAL;

		JLabel sizeText = new JLabel("Specify number of " + text + ": ");
		c.gridx = 0;
		c.gridy = 0;
		selWindow.add(sizeText, c);

		c.gridx = 1;
		selWindow.add(size, c);

		JButton nextButton = new JButton("Next");
		nextButton.setActionCommand("next");
		nextButton.addActionListener(actionListener);
		c.gridy++;
		selWindow.add(nextButton, c);
		selWindow.pack();
		selWindow.setLocation(Projects.getCenteredLoc(selWindow.getWidth(),
				selWindow.getHeight()));
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// selWindow.setLocation(mlocation.x, mlocation.y);
		selWindow.setModal(true);
		selWindow.setResizable(false);
		selWindow.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		selWindow.setAlwaysOnTop(true);
		selWindow.setVisible(true);
	}

	public IOComponentTypes GetType() {
		return MyType;
	}

	private ArrayList<String> GetXilinxUCFStrings(String direction, int StartId) {
		ArrayList<String> Contents = new ArrayList<String>();
		StringBuffer Temp = new StringBuffer();
		Integer start = 0;
		Integer end = NrOfPins;
		ArrayList<String> labels = null;
		if (MyType.equals(IOComponentTypes.PortIO)) {
			labels = PortIO.GetLabels(IOComponentTypes.GetNrOfFPGAPins(MyType));
		} else if (MyType.equals(IOComponentTypes.LocalBus)) {
			labels = ReptarLocalBus.GetLabels();
			if (direction.equals("in")) {
				end = IOComponentTypes.GetFPGAInputRequirement(MyType);
			} else if (direction.equals("out")) {
				// TODO: YSY
				Contents.add("NET \"FPGA_LB_OUT_0\" LOC = \"R24\" | IOSTANDARD = LVCMOS18 ; # SP6_LB_WAIT3_o");
				Contents.add("NET \"FPGA_LB_OUT_1\" LOC = \"AB30\" | IOSTANDARD = LVCMOS18 ; # IRQ_o");
				return Contents;
				// start = IOComponentTypes.GetFPGAInputRequirement(MyType);
				// end = start +
				// IOComponentTypes.GetFPGAOutputRequirement(MyType);
			} else if (direction.equals("inout")) {
				start = IOComponentTypes.GetFPGAInputRequirement(MyType)
						+ IOComponentTypes.GetFPGAOutputRequirement(MyType);
				end = start + IOComponentTypes.GetFPGAInOutRequirement(MyType);
			}
		} else if (MyType.equals(IOComponentTypes.DIPSwitch)) {
			labels = DipSwitch.GetLabels(IOComponentTypes
					.GetNrOfFPGAPins(MyType));
		} else if (MyType.equals(IOComponentTypes.SevenSegment)) {
			labels = SevenSegment.GetLabels();
		} else if (MyType.equals(IOComponentTypes.RGBLED)) {
			labels = RGBLed.GetLabels();
		}
		for (int i = start; i < end; i++) {
			Temp.setLength(0);
			Temp.append("LOC = \"" + MyPinLocations.get(i) + "\" ");
			if (MyPullBehavior != PullBehaviors.Unknown
					&& MyPullBehavior != PullBehaviors.Float) {
				Temp.append("| "
						+ PullBehaviors
						.getContraintedPullString(MyPullBehavior) + " ");
			}
			if (MyDriveStrength != DriveStrength.Unknown
					&& MyDriveStrength != DriveStrength.DefaulStength) {
				Temp.append("| DRIVE = "
						+ DriveStrength
						.GetContraintedDriveStrength(MyDriveStrength)
						+ " ");
			}
			if (MyIOStandard != IoStandards.Unknown
					&& MyIOStandard != IoStandards.DefaulStandard) {
				Temp.append("| IOSTANDARD = "
						+ IoStandards.GetConstraintedIoStandard(MyIOStandard)
						+ " ");
			}
			Temp.append(";");
			if (labels != null) {
				Temp.append(" # " + labels.get(i));
			}
			String NetName = "";
			if (direction == "in") {
				NetName = HDLGeneratorFactory.FPGAInputPinName + "_"
						+ Integer.toString(StartId + i - start);
			} else if (direction == "inout") {
				NetName = HDLGeneratorFactory.FPGAInOutPinName + "_"
						+ Integer.toString(StartId + i - start);
			} else {
				NetName = HDLGeneratorFactory.FPGAOutputPinName + "_"
						+ Integer.toString(StartId + i - start);
			}
			// String NetName = (InputPins) ?
			// HDLGeneratorFactory.FPGAInputPinName + "_" +
			// Integer.toString(StartId + i)
			// : HDLGeneratorFactory.FPGAOutputPinName + "_" +
			// Integer.toString(StartId + i);
			Contents.add("NET \"" + NetName + "\" " + Temp.toString());
		}
		return Contents;
	}

	private ArrayList<String> GetVivadoXDCStrings(String direction, int StartId) {
		ArrayList<String> contents = new ArrayList<String>();
		for (int i = 0; i < NrOfPins; i++) {
			String netName = "";
			if (direction.equals("in")) {
				netName = HDLGeneratorFactory.FPGAInputPinName + "_"
						+ Integer.toString(StartId + i);
			} else if (direction.equals("inout")) {
				netName = HDLGeneratorFactory.FPGAInOutPinName + "_"
						+ Integer.toString(StartId + i);
			} else {
				netName = HDLGeneratorFactory.FPGAOutputPinName + "_"
						+ Integer.toString(StartId + i);
			}
			contents.add("set_property PACKAGE_PIN " + MyPinLocations.get(i) +
					" [get_ports {" + netName + "}]");

			if (MyIOStandard != IoStandards.Unknown	&& MyIOStandard != IoStandards.DefaulStandard) {
				contents.add("    set_property IOSTANDARD " +
						IoStandards.GetConstraintedIoStandard(MyIOStandard) +" [get_ports {" + netName + "}]");
			}
			if (MyIOStandard != IoStandards.Unknown	&& MyIOStandard != IoStandards.DefaulStandard) {
				contents.add("    set_property IOSTANDARD " +
						IoStandards.GetConstraintedIoStandard(MyIOStandard) +" [get_ports {" + netName + "}]");
			}
		}
		return contents;
	}

	public boolean IsInput() {
		return IOComponentTypes.InputComponentSet.contains(MyType);
	}

	public boolean IsInputOutput() {
		return IOComponentTypes.InOutComponentSet.contains(MyType);
	}

	public boolean IsKnownComponent() {
		return IOComponentTypes.KnownComponentSet.contains(MyType);
	}

	public boolean IsOutput() {
		return IOComponentTypes.OutputComponentSet.contains(MyType);
	}

	public void Set(IOComponentTypes Type, BoardRectangle rect, String loc,
			String pull, String active, String standard, String drive) {
		MyType = Type;
		MyRectangle = rect;
		rect.SetActiveOnHigh(active
				.equals(PinActivity.Behavior_strings[PinActivity.ActiveHigh]));
		NrOfPins = 1;
		MyPinLocations.put(0, loc);
		MyPullBehavior = PullBehaviors.getId(pull);
		MyActivityLevel = PinActivity.getId(active);
		MyIOStandard = IoStandards.getId(standard);
		MyIdentifier = 0;
		MyDriveStrength = DriveStrength.getId(drive);
	}

	public void SetId(long id) {
		MyIdentifier = id;
	}

	public void setNrOfPins(int count) {
		if (GetType().equals(IOComponentTypes.DIPSwitch)
				|| GetType().equals(IOComponentTypes.PortIO)) {
			NrOfPins = count;
		}
	}

	private void showDialogNotification(JDialog parent, String type,
			String string) {
		final JDialog dialog = new JDialog(parent, type);
		JLabel pic = new JLabel();
		if (type.equals("Warning")) {
			pic.setIcon(new ImageIcon(getClass().getResource(
					BoardDialog.pictureWarning)));
		} else {
			pic.setIcon(new ImageIcon(getClass().getResource(
					BoardDialog.pictureError)));
		}
		GridBagLayout dialogLayout = new GridBagLayout();
		dialog.setLayout(dialogLayout);
		GridBagConstraints c = new GridBagConstraints();
		JLabel message = new JLabel(string);
		JButton close = new JButton("close");
		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// panel.setAlwaysOnTop(true);
				dialog.dispose();
			}
		};
		close.addActionListener(actionListener);

		c.gridx = 0;
		c.gridy = 0;
		c.ipadx = 20;
		dialog.add(pic, c);

		c.gridx = 1;
		c.gridy = 0;
		dialog.add(message, c);

		c.gridx = 1;
		c.gridy = 1;
		dialog.add(close, c);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);

	}
}
