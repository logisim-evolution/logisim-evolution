package com.cburch.logisim.fsm.model;

import java.util.List;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Fsm extends Library {

	private static FactoryDescription[] DESCRIPTIONS = { 
			new FactoryDescription("FSMMedvedev", Strings.getter("FsmMedvedev"), "fsm.gif","FsmMedvedevFactory"), 
			new FactoryDescription("FSMMoore", Strings.getter("FsmMoore"), "fsm.gif","FsmMooreFactory"),
			new FactoryDescription("FSMMealy", Strings.getter("FsmMealy"), "fsm.gif","FsmMealyFactory"),
			};

	private List<Tool> tools = null;

	public Fsm() {
	}

	@Override
	public String getDisplayName() {
		return Strings.get("FsmLibrary");
	}

	@Override
	public String getName() {
		return "FSM_IP";
	}

	@Override
	public List<Tool> getTools() {
		if (tools == null) {
			tools = FactoryDescription.getTools(Fsm.class, DESCRIPTIONS);
		}
		return tools;
	}

	public boolean removeLibrary(String Name) {
		return false;
	}
}
