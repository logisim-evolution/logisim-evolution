package com.ita.logisim.ttl;

import com.cburch.logisim.data.AttributeSet;

public class Ttl7411 extends Ttl7410 {
	
	public class Ttl7411HDLGenerator extends Ttl7410HDLGenerator {
		
		public Ttl7411HDLGenerator() {
		   super(false);
		}
	}
	
	public Ttl7411() {
		super("7411",false);
	}

	@Override
	public boolean HDLSupportedComponent(String HDLIdentifier,
			AttributeSet attrs) {
		if (MyHDLGenerator == null)
			MyHDLGenerator = new Ttl7411HDLGenerator();
		return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
	}
}
