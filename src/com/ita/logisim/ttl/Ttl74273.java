package com.ita.logisim.ttl;

public class Ttl74273 extends AbstractOctalFlops {

	public Ttl74273() {
		super("74273", (byte) 20, new byte[] { 2,5,6,9,12,15,16,19 },
				new String[] { "nCLR", "Q1", "D1", "D2", "Q2", "Q3", "D3", "D4",
						       "Q4", "CLK", "Q5", "D5", "D6" , "Q6", "Q7" , "D7", "D8" , "Q8"});
		super.SetWe(false);
	}

}
