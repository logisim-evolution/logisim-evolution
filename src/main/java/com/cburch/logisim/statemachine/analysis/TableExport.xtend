package com.cburch.logisim.statemachine.analysis

import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.Port
import java.util.ArrayList
import com.cburch.logisim.statemachine.simulator.FSMSimulator
import com.cburch.logisim.statemachine.fSMDSL.InputPort
import com.cburch.logisim.statemachine.parser.FSMSerializer
import java.io.PrintStream
import java.io.File

class TableExport {
	
	def genName(Port ip) {
		var il= new ArrayList()
		for (off: (ip.width-1)..0) {
				il+=ip.name+"_"+off
		}
		il
	}
	
	def genSIS(FSM fsm) {
		'''
		«
			FOR ip:fsm.in SEPARATOR ";"»«
				FOR n:genName(ip) SEPARATOR ";"»«n»«
				ENDFOR»«
			ENDFOR» «
			FOR i:0..fsm.width SEPARATOR ";"»cs_«i»«
			ENDFOR»;;«
			FOR op:fsm.out SEPARATOR ";"»«
				FOR n:genName(op) SEPARATOR ";"»«n»«
				ENDFOR»«
			ENDFOR» «
			FOR i:0..fsm.width SEPARATOR ";"»ns_«i»«
			ENDFOR»
		«FOR l: buildTruthTable(fsm)»
		«l»
		«ENDFOR»
		'''
	}
	
	
	def buildTruthTable(FSM fsm) {
		var buffer = new ArrayList<String>();
		var ic = new InputVectorGenerator(fsm); 
		val FSMSimulator sim = new FSMSimulator(fsm);
		sim.refreshInputPorts
		do {
			for (int i : 0..(fsm.in.size-1)) {
				val ip = fsm.in.get(i) as InputPort
				sim.updateInput(ip,ic.getQuotedBinaryValue(i));
			} 

			val currentCode=ic.getQuotedBinaryValue(fsm.in.size)
			sim.currentState =null
			for (s:fsm.states) {
				
				if (s.code==currentCode) {
					sim.currentState=s
				}
			}
			if (sim.currentState==null) {
				throw new RuntimeException("Error not matching state in FSM")
			}
		
			var String line ="";
			for (int i:0..(ic.size-1)) {
				line+=ic.getBinaryValue(i)
			}

			line+=";"
			sim.updateState	
			sim.updateCommands
			line+= sim.currentState.code.replace("\"","")
			for (i : 0..(fsm.out.size-1)) {
				line+=sim.getOutput(i).replace("\"","")
			}
			line= line.replace("0","0;")
			line= line.replace("1","1;")
			buffer+=line

		} while (ic.inc())
		buffer
		
	}
	
		static val ex= '''fsm example  { 

		in  keypad [ 4 ]; 
		in  A [ 1 ]; 
		out X [ 4 ] ; 
		codeWidth = 2 ; 
		reset = S0 ; 

		state S0 = "01" { 
			commands  { X = "0001" ; }
			transitions { 
				S0 -> S1 when A.keypad/="1100" ; 
			}
		} 
		state S1 = "10" { 
			commands { X = "0010" ; } 
			transitions { 
				S1 -> S2 when keypad/="1010"   ; 
			} 
		} 
		state S2 = "00"  { 
			commands { X = { "0000" } ; } 
			transitions { 
				S2 -> S0 when default   ; 
			} 
		}
		state S3 = "11"  { 
			commands { X = { "0000" } ; } 
			transitions { 
				S2 -> S0 when default   ; 
			} 
		}
	}
	''' 

    def static void main(String[] args) {
		val fsm = FSMSerializer.load(ex)
		val tt = new TableExport()
		val ps=  new PrintStream(new File(fsm.name+".csv"));
		ps.append(tt.genSIS(fsm));
		ps.close
    }
}