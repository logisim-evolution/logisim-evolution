package com.ita.logisim.io;

import java.util.List;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class ITA_IO extends Library {

	private static FactoryDescription[] DESCRIPTIONS = {
			new FactoryDescription("Switch", Strings.getter("switchComponent"), "switch.gif", "Switch"),
			new FactoryDescription("Buzzer", Strings.getter("buzzerComponent"), "buzzer.gif", "Buzzer"),
	};
	
	private List<Tool> tools = null;

	@Override
	public String getName() {
		return "Logisim ITA Input/Output";
	}

	@Override
	public boolean removeLibrary(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<? extends Tool> getTools() {
		if (tools == null) {
			tools = FactoryDescription.getTools(ITA_IO.class, DESCRIPTIONS);
		}
		return tools;
	}

}
