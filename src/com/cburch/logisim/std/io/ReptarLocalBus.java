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
package com.cburch.logisim.std.io;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer;
import com.bfh.logisim.fpgaboardeditor.FPGAIOInformationContainer.IOComponentTypes;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.IOComponentInformationContainer;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;


public class ReptarLocalBus extends InstanceFactory {

	public static ArrayList<String> GetLabels() {
		ArrayList<String> LabelNames = new ArrayList<String>();
		for (int i = 0; i < IOComponentTypes
				.GetNrOfFPGAPins(IOComponentTypes.LocalBus); i++) {
			LabelNames.add("");
		}
		LabelNames.set(0, "SP6_LB_nCS3_i");
		LabelNames.set(1, "SP6_LB_nADV_ALE_i");
		LabelNames.set(2, "SP6_LB_RE_nOE_i");
		LabelNames.set(3, "SP6_LB_nWE_i");
		for (int i = 0; i < 9; i++) {
			LabelNames.set(4 + i, "Addr_LB_i_" + (i + 16));
		}
		LabelNames.set(13, "SP6_LB_WAIT3_o");
		LabelNames.set(14, "IRQ_o");
		// LabelNames.set(Addr_Data_LB_o , "Addr_Data_LB_i_0" );
		// LabelNames.set(Addr_Data_LB_i , "Addr_Data_LB_o_0" );
		// LabelNames.set(5, "Addr_Data_LB_tris_o");
		// LabelNames.set(6, "Addr_Data_LB_io_0");
		// LabelNames.set(7, "Addr_Data_LB_io_0");
		for (int i = 0; i < 16; i++) {
			// LabelNames.set(7+i,"Addr_Data_LB_i_"+i);
			// LabelNames.set(15+7+i,"Addr_Data_LB_o_"+i);
			// LabelNames.set(15+15+7+i,"Addr_Data_LB_io_"+i);
			LabelNames.set(15 + i, "Addr_Data_LB_io_" + i);
		}
		return LabelNames;
	}

	public static final int SP6_LB_nCS3_o = 0;
	public static final int SP6_LB_nADV_ALE_o = 1;
	public static final int SP6_LB_RE_nOE_o = 2;
	public static final int SP6_LB_nWE_o = 3;
	public static final int SP6_LB_WAIT3_i = 4;
	public static final int Addr_Data_LB_o = 5;
	public static final int Addr_Data_LB_i = 6;
	public static final int Addr_Data_LB_tris_i = 7;
	public static final int Addr_LB_o = 8;
	public static final int IRQ_i = 9;

	// private static final int Addr_Data_LB_io = 9;
	private MappableResourcesContainer mapInfo;
	/* Default Name. Very important for the genration of the VDHL Code */
	private String defaultLocalBusName = "LocalBus";

	public ReptarLocalBus() {
		super("ReptarLB", Strings.getter("repLBComponent"));

		setAttributes(new Attribute[] {StdAttr.LABEL},
				new Object[] {defaultLocalBusName});

		// setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
		setOffsetBounds(Bounds.create(-110, -10, 110, 110));
		setIconName("localbus.gif");

		Port[] ps = new Port[10];
		ps[SP6_LB_nCS3_o] = new Port(0, 0, Port.OUTPUT, 1);
		ps[SP6_LB_nADV_ALE_o] = new Port(0, 10, Port.OUTPUT, 1);
		ps[SP6_LB_RE_nOE_o] = new Port(0, 20, Port.OUTPUT, 1);
		ps[SP6_LB_nWE_o] = new Port(0, 30, Port.OUTPUT, 1);
		ps[SP6_LB_WAIT3_i] = new Port(0, 40, Port.INPUT, 1);
		ps[Addr_Data_LB_o] = new Port(0, 50, Port.OUTPUT, 16);
		ps[Addr_Data_LB_i] = new Port(0, 60, Port.INPUT, 16);
		ps[Addr_Data_LB_tris_i] = new Port(0, 70, Port.INPUT, 1);
		ps[Addr_LB_o] = new Port(0, 80, Port.OUTPUT, 9);
		ps[IRQ_i] = new Port(0, 90, Port.INPUT, 1);
		// ps[Addr_Data_LB_io ] = new Port(0,80, Port.INOUT,16);
		ps[SP6_LB_nCS3_o].setToolTip(Strings.getter("repLBTip"));
		ps[SP6_LB_nADV_ALE_o].setToolTip(Strings.getter("repLBTip"));
		ps[SP6_LB_RE_nOE_o].setToolTip(Strings.getter("repLBTip"));
		ps[SP6_LB_nWE_o].setToolTip(Strings.getter("repLBTip"));
		ps[SP6_LB_WAIT3_i].setToolTip(Strings.getter("repLBTip"));
		ps[Addr_Data_LB_o].setToolTip(Strings.getter("repLBTip"));
		ps[Addr_Data_LB_i].setToolTip(Strings.getter("repLBTip"));
		ps[Addr_Data_LB_tris_i].setToolTip(Strings.getter("repLBTip"));
		ps[Addr_LB_o].setToolTip(Strings.getter("repLBTip"));
		ps[IRQ_i].setToolTip(Strings.getter("repLBTip"));
		// ps[Addr_Data_LB_io ].setToolTip(Strings.getter("repLBTip"));
		setPorts(ps);

		// From FPGA pin view
		MyIOInformation = new IOComponentInformationContainer(13, 2, 16,
				FPGAIOInformationContainer.IOComponentTypes.LocalBus);

	}

	@Override
	public String getHDLName(AttributeSet attrs) {
		/* Force the name of the localBus*/
		return attrs.getValue(StdAttr.LABEL);
	}

	public MappableResourcesContainer getMapInfo() {
		return mapInfo;
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		// return false;
		if (MyHDLGenerator == null) {
			MyHDLGenerator = new ReptarLocalBusHDLGeneratorFactory();
		}
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}

	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		painter.drawBounds();

		g.setColor(Color.BLACK);
		g.setFont(g.getFont().deriveFont(g.getFont().getSize2D() - 2));
		painter.drawPort(SP6_LB_nCS3_o, "SP6_LB_nCS3_o", Direction.WEST);
		painter.drawPort(SP6_LB_nADV_ALE_o, "SP6_LB_nADV_ALE_o", Direction.WEST);
		painter.drawPort(SP6_LB_RE_nOE_o, "SP6_LB_RE_nOE_o", Direction.WEST);
		painter.drawPort(SP6_LB_nWE_o, "SP6_LB_nWE_o", Direction.WEST);
		painter.drawPort(SP6_LB_WAIT3_i, "SP6_LB_WAIT3_i", Direction.WEST);
		painter.drawPort(Addr_Data_LB_o, "Addr_Data_LB_o", Direction.WEST);
		painter.drawPort(Addr_Data_LB_i, "Addr_Data_LB_i", Direction.WEST);
		painter.drawPort(Addr_Data_LB_tris_i, "Addr_Data_LB_tris_i",
				Direction.WEST);
		painter.drawPort(Addr_LB_o, "Addr_LB_o", Direction.WEST);
		painter.drawPort(IRQ_i, "IRQ_i", Direction.WEST);
		// painter.drawPort(Addr_Data_LB_io ,"Addr_Data_LB_io",Direction.WEST);

		// Location loc = painter.getLocation();
		// int x = loc.getX();
		// int y = loc.getY();
		// GraphicsUtil.switchToWidth(g, 2);
		// g.setColor(Color.BLACK);
		// g.drawLine(x - 15, y, x - 5, y);
		// g.drawLine(x - 10, y - 5, x - 10, y + 5);
		// GraphicsUtil.switchToWidth(g, 1);
	}

	@Override
	public void propagate(InstanceState state) {
		throw new UnsupportedOperationException(
				"Reptar Local Bus simulation not implemented");
		// // get attributes
		// BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
		//
		// // compute outputs
		// Value a = state.getPortValue(IN0);
		// Value b = state.getPortValue(IN1);
		// Value c_in = state.getPortValue(C_IN);
		// Value[] outs = ReptarLocalBus.computeSum(dataWidth, a, b, c_in);
		//
		// // propagate them
		// int delay = (dataWidth.getWidth() + 2) * PER_DELAY;
		// state.setPort(OUT, outs[0], delay);
		// state.setPort(C_OUT, outs[1], delay);
	}

	@Override
	public boolean RequiresGlobalClock() {
		return true;
	}

	public void setMapInfo(MappableResourcesContainer mapInfo) {
		this.mapInfo = mapInfo;
	}
}
