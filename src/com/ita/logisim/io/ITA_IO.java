package com.ita.logisim.io;

import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class ITA_IO extends Library {

	private static FactoryDescription[] DESCRIPTIONS = {
			new FactoryDescription("Switch", Strings.getter("switchComponent"), "switch.gif", "Switch"),
			new FactoryDescription("Buzzer", Strings.getter("buzzerComponent"), "buzzer.gif", "Buzzer"),
			new FactoryDescription("DipSwitch", Strings.getter("DipSwitchComponent"), "dipswitch.gif", "DipSwitch"),
			new FactoryDescription("Slider", Strings.getter("Slider"), "slider.gif", "Slider"),
			new FactoryDescription("Digital Oscilloscope", Strings.getter("DigitalOscilloscopeComponent"),
					"digitaloscilloscope.gif", "DigitalOscilloscope"),
			new FactoryDescription("PlaRom", Strings.getter("PlaRomComponent"), "plarom.gif", "PlaRom"),
	};
	
	private List<Tool> tools = null;
	private Tool[] ADD_TOOLS = {new AddTool(ProgrammableGenerator.FACTORY),};

	@Override
	public String getName() {
		return "Logisim ITA components";
	}

	@Override
	public boolean removeLibrary(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<? extends Tool> getTools() {
		if (tools == null) {
			List<Tool> ret = new ArrayList<Tool>(ADD_TOOLS.length+DESCRIPTIONS.length);
			for (Tool a : ADD_TOOLS)
				ret.add(a);
			ret.addAll(FactoryDescription.getTools(ITA_IO.class, DESCRIPTIONS));
			tools = ret;
		}
		return tools;
	}

}
