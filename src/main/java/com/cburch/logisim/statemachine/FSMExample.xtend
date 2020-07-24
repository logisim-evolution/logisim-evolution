package com.cburch.logisim.statemachine

import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.parser.FSMSerializer

class FSMExample {
	def static FSM getEx0() {
		return FSMSerializer::load(
		'''
		fsm baseFSM {
			in A;
			in B;
			out X;
			out Y;
			
			start=S0;
			
			state S0 {
				code = "0000";
				commands {
					X=A;
				}
				
				transitions {
					S0 -> S0 when A+B;
				}
			}
		}
		
		''') as FSM;
	}
	
	def static void main(String[] args) {
		print(ex0);
	}
	

}
