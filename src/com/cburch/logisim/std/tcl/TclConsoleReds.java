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
package com.cburch.logisim.std.tcl;

import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.instance.Port;

/**
 * This is a static TCL component. It onlyy defines the interface as all other
 * things are defined by the parent class.
 *
 * You can use this as an example to create other static TCL components.
 *
 * You may notice that this class is dynamically loaded. You have to define it
 * int Tcl.java library. If you change the name, older circuits will not be able
 * to load the rightful component, so please don't. But if you need to change
 * the display name, you can do this in the resource files (std.properties).
 */
public class TclConsoleReds extends TclComponent {

	public TclConsoleReds() {
		super("TclConsoleReds", Strings.getter("tclConsoleReds"));

		List<PortDescription> inputsDesc = new ArrayList<PortDescription>();
		List<PortDescription> outputsDesc = new ArrayList<PortDescription>();

		outputsDesc.add(new PortDescription("S0_sti", "output", 1));
		outputsDesc.add(new PortDescription("S1_sti", "output", 1));
		outputsDesc.add(new PortDescription("S2_sti", "output", 1));
		outputsDesc.add(new PortDescription("S3_sti", "output", 1));
		outputsDesc.add(new PortDescription("S4_sti", "output", 1));
		outputsDesc.add(new PortDescription("S5_sti", "output", 1));
		outputsDesc.add(new PortDescription("S6_sti", "output", 1));
		outputsDesc.add(new PortDescription("S7_sti", "output", 1));
		outputsDesc.add(new PortDescription("S8_sti", "output", 1));
		outputsDesc.add(new PortDescription("S9_sti", "output", 1));
		outputsDesc.add(new PortDescription("S10_sti", "output", 1));
		outputsDesc.add(new PortDescription("S11_sti", "output", 1));
		outputsDesc.add(new PortDescription("S12_sti", "output", 1));
		outputsDesc.add(new PortDescription("S13_sti", "output", 1));
		outputsDesc.add(new PortDescription("S14_sti", "output", 1));
		outputsDesc.add(new PortDescription("S15_sti", "output", 1));
		outputsDesc.add(new PortDescription("Val_A_sti", "output", 16));
		outputsDesc.add(new PortDescription("Val_B_sti", "output", 16));
		outputsDesc.add(new PortDescription("rst_o", "output", 1));

		inputsDesc.add(new PortDescription("Hex0_obs", "input", 4));
		inputsDesc.add(new PortDescription("Hex1_obs", "input", 4));
		inputsDesc.add(new PortDescription("L0_obs", "input", 1));
		inputsDesc.add(new PortDescription("L1_obs", "input", 1));
		inputsDesc.add(new PortDescription("L2_obs", "input", 1));
		inputsDesc.add(new PortDescription("L3_obs", "input", 1));
		inputsDesc.add(new PortDescription("L4_obs", "input", 1));
		inputsDesc.add(new PortDescription("L5_obs", "input", 1));
		inputsDesc.add(new PortDescription("L6_obs", "input", 1));
		inputsDesc.add(new PortDescription("L7_obs", "input", 1));
		inputsDesc.add(new PortDescription("L8_obs", "input", 1));
		inputsDesc.add(new PortDescription("L9_obs", "input", 1));
		inputsDesc.add(new PortDescription("L10_obs", "input", 1));
		inputsDesc.add(new PortDescription("L11_obs", "input", 1));
		inputsDesc.add(new PortDescription("L12_obs", "input", 1));
		inputsDesc.add(new PortDescription("L13_obs", "input", 1));
		inputsDesc.add(new PortDescription("L14_obs", "input", 1));
		inputsDesc.add(new PortDescription("L15_obs", "input", 1));
		inputsDesc.add(new PortDescription("Result_A_obs", "input", 16));
		inputsDesc.add(new PortDescription("Result_B_obs", "input", 16));
		inputsDesc.add(new PortDescription("seg7_obs", "input", 8));

		inputsDesc.add(new PortDescription("sysclk_i", "input", 1));
		inputsDesc.add(new PortDescription("rst_in", "input", 1));

		Port[] inputs = new Port[inputsDesc.size()];
		Port[] outputs = new Port[outputsDesc.size()];

		for (int i = 0; i < inputsDesc.size(); i++) {
			PortDescription desc = inputsDesc.get(i);
			inputs[i] = new Port(0, (i * PORT_GAP) + HEIGHT, desc.getType(),
					desc.getWidth());
			inputs[i].setToolTip(Strings.getter(desc.getName()));
		}

		for (int i = 0; i < outputsDesc.size(); i++) {
			PortDescription desc = outputsDesc.get(i);
			outputs[i] = new Port(WIDTH, (i * PORT_GAP) + HEIGHT,
					desc.getType(), desc.getWidth());
			outputs[i].setToolTip(Strings.getter(desc.getName()));
		}

		setPorts(inputs, outputs);
	}

	@Override
	public String getDisplayName() {
		return Strings.get("tclConsoleReds");
	}
}
