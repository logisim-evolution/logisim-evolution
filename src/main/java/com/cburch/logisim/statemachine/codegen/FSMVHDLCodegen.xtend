package com.cburch.logisim.statemachine.codegen

// Drawing.java
// By Matthew McNierney for CS5 Lab Assignment #3
// Holds various methods for manipulating and storing the shapes in the drawing.

import com.cburch.logisim.statemachine.fSMDSL.AndExpr
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr
import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.NotExpr
import com.cburch.logisim.statemachine.fSMDSL.OrExpr
import com.cburch.logisim.statemachine.fSMDSL.Port
import com.cburch.logisim.statemachine.fSMDSL.PortRef
import com.cburch.logisim.statemachine.fSMDSL.State
import com.cburch.logisim.statemachine.fSMDSL.Constant
import com.cburch.logisim.statemachine.fSMDSL.Command
import com.cburch.logisim.statemachine.fSMDSL.DefaultPredicate

import java.io.PrintStream
import java.io.File
import java.util.ArrayList
import com.cburch.logisim.statemachine.fSMDSL.CmpExpr
import java.io.FileNotFoundException
import com.cburch.logisim.statemachine.fSMDSL.InputPort
import com.cburch.logisim.statemachine.fSMDSL.OutputPort
import com.cburch.logisim.statemachine.fSMDSL.ConcatExpr

class FSMVHDLCodeGen{


	/** 
	 * Constructor. Initialize the color to the default color and create the
	 * ArrayList to hold the shapes.
	 * @param defaultColor
	 */
	new() {
	}

	def export(FSM fsm, File f) throws FileNotFoundException{
		val ps  = new PrintStream(f);
		ps.append(generate(fsm));
		ps.close
	}

	def dispatch generate(FSM e) {
		var ios = new ArrayList<Port>();
		ios.addAll(e.in)
		ios.addAll(e.out)
		'''
«««		library IEEE;  
«««		use IEEE.std_logic_1164.all; 
«««		
«««		entity «e.name» is port (
«««			clk : in std_logic;
«««			rst : in std_logic;
«««			en  : in std_logic;
«««			«FOR i:ios SEPARATOR ";"»
«««				«genPort(i)»
«««			«ENDFOR»
«««		);
«««		end entity;
		
		architecture RTL of «e.name» is --«e.hashCode»
			
			function BOOL_TO_SL(X : boolean)
              return std_logic is
			begin
			  if X then
			    return '1';
			  else
			    return '0';
			  end if;
			end BOOL_TO_SL;
			
			type state_type is («e.states.map[s|"S_"+s.name].reduce[s1,s2|s1+','+s2]»);  
			signal symCS : state_type ;  
			signal CS : std_logic_vector(«e.width-1» downto 0) ;  
			
			constant ONE : std_logic:='1';
			constant ZERO : std_logic:='0';

			«FOR s:e.states»
			constant «s.name» : std_logic_vector(«e.width-1» downto 0):=«s.code»;
			«ENDFOR»
			
		begin
			UPDATE: process(clk,rst)
			begin
				if rst='1' then
					CS <= «e.start.name»;
					symCS <= S_«e.start.name»;
				elsif rising_edge(clk) then
					if en='1'then
						case (CS) is
							«FOR s:e.states»«genTransition(s)»«ENDFOR» 
							when others => null;
						end case; 
					end if;
				end if;
			end process;
			
			OUTPUT : process(CS«IF e.in.size>0»,«FOR i:e.in SEPARATOR ","»«i.name»«ENDFOR»«ENDIF»)
			begin
				«FOR o:e.out»«genDefaultValue(o)»«ENDFOR» 
				case (CS) is
					«FOR s:e.states»«genCommand(s)»«ENDFOR» 
					when others => null;
				end case; 
			end process;
			
		end RTL;
		'''
	}
	
	def genDefaultValue(Port port) {
		val value= if(port.width==1) {
			"'0'"
		} else {
			'''"«Integer.toBinaryString((1<<port.width)-1).replace('1','0')»"'''
		}
		'''«port.name» <= «value»;'''
	}
	
	def dispatch genPort(Port port) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	def dispatch genPort(InputPort port) {
		if(port.width >1)
		'''«port.name» : in std_logic_vector(«port.width-1» downto 0)'''
		else
		'''«port.name» : in std_logic'''
		
	}
	def dispatch genPort(OutputPort port) {
		if(port.width >1)
		'''«port.name» : out std_logic_vector(«port.width-1» downto 0)'''
		else
		'''«port.name» : out std_logic'''
	}
	
	def getDefault(State s) {
		s.transition.findFirst[p|(p.predicate instanceof DefaultPredicate)]
	}
	def genTransition(State state) {
		'''when «state.name» =>
				«IF getDefault(state)!=null»
				-- default transition
				CS <= «getDefault(state).dst.name»;
				symCS <= S_«getDefault(state).dst.name»;
				«ENDIF»
				«FOR t:state.transition.filter[t|!(t.predicate instanceof DefaultPredicate)]»
				if «genPred(t.predicate)»='1'  then
					CS <= «t.dst.name»;
					symCS <= S_«t.dst.name»;
				end if;
				«ENDFOR»
		'''
	}
	

	def static dispatch genPred(CmpExpr b) {
		switch(b.op) {
			case "==" : {'''BOOL_TO_SL(«genPred(b.args.get(0))»=«genPred(b.args.get(1))»)'''}
			case "!=" : {'''BOOL_TO_SL(«genPred(b.args.get(0))»/=«genPred(b.args.get(1))»)'''}
			case "/=" : {'''BOOL_TO_SL(«genPred(b.args.get(0))»/=«genPred(b.args.get(1))»)'''}
			default : throw new UnsupportedOperationException("Not implemented")			
		}
	}
	def static dispatch genPred(ConcatExpr b) {
		'''(«FOR i:b.args SEPARATOR " & "»«genPred(i)»«ENDFOR»)'''.toString
	}
	def static dispatch genPred(OrExpr b) {
		'''(«FOR i:b.args SEPARATOR " or "»«genPred(i)»«ENDFOR»)'''.toString
	}
	def static dispatch genPred(AndExpr b) {
		'''(«FOR i:b.args SEPARATOR " and "»«genPred(i)»«ENDFOR»)'''.toString
	}
	def static dispatch genPred(NotExpr b) {
		"(not("+genPred(b.args.get(0))+"))";
		
	}
	def static dispatch genPred(PortRef b) {
		if(b.range!=null) {
			if (b.range.ub!=-1)
		  		'''«b.port.name»(«b.range.ub» downto «b.range.lb»)'''.toString
		  	else
		  		'''«b.port.name»(«b.range.lb»)'''.toString
		} else {
			b.port.name
		}
	}
	def static dispatch genPred(Constant b) {
		if(b.value.length>3) 
			b.value
		else {
			switch(b.value) {
				case "\"0\"" : return "ZERO"
 
				case "\"1\"" : return "ONE"
				 
				default :{
					throw new UnsupportedOperationException("Invalid one-bit value "+b.value);					
				}
			}
		}
		
		//b.value.replace('"','\'') 
	}


	def genCommand(State s) {
		
		if (s.commandList.commands.size==0) {
			'''
			when «s.name» => null;
			'''
		} else {
			'''
			when «s.name» => 
			«FOR c:s.commandList.commands»
				«genCommand(c)»
			«ENDFOR»
			'''
		}
	}
	
	def genCommand(Command c) {
		c.name.name+"<="+genPred(c.value)+";";		
	}
	def gen(Port port) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}




}
