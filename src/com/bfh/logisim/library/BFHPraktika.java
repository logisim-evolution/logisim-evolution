package com.bfh.logisim.library;

import java.util.List;

import com.cburch.logisim.std.arith.Strings;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class BFHPraktika extends Library {
	private static FactoryDescription[] DESCRIPTIONS = {
		new FactoryDescription("Binairy_to_BCD_converter",
		         Strings.getter("Bin2BCD"), "",
		         "bin2bcd"), 
		new FactoryDescription("BCD_to_7_Segment_decoder",
		         Strings.getter("BCD2SevenSegment"), "",
		         "bcd2sevenseg"),};

private List<Tool> tools = null;

public BFHPraktika() {
}

@Override
public String getDisplayName() {
	return Strings.get("BFH mega functions");
}

@Override
public String getName() {
	return "BFH-Praktika";
}

@Override
public List<Tool> getTools() {
	if (tools == null) {
		tools = FactoryDescription.getTools(BFHPraktika.class, DESCRIPTIONS);
	}
	return tools;
}

public boolean removeLibrary(String Name) {
	return false;
}
}
