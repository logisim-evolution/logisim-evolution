/** 
 * This file is part of logisim-evolution.
 * logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * logisim-evolution is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by :
 * + Haute École Spécialisée Bernoise
 * http://www.bfh.ch
 * + Haute École du paysage, d'ingénierie et d'architecture de Genève
 * http://hepia.hesge.ch/
 * + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 * http://www.heig-vd.ch/
 * The project is currently maintained by :
 * + REDS Institute - HEIG-VD
 * Yverdon-les-Bains, Switzerland
 * http://reds.heig-vd.ch
 */
package com.cburch.logisim.std.fsm

import java.util.Arrays
import java.util.HashMap
import java.util.List
import java.util.Map

import javax.swing.JOptionPane
import org.eclipse.emf.ecore.util.EcoreUtil

import com.cburch.logisim.instance.Port
import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.InputPort
import com.cburch.logisim.statemachine.fSMDSL.OutputPort
import com.cburch.logisim.statemachine.parser.FSMSerializer
import com.cburch.logisim.util.EventSourceWeakSupport
 
class FSMContent implements Cloneable {
	def protected static <T> T[] concat(T[] first, T[] second) {
		var T[] result = Arrays::copyOf(first, first.length + second.length)
		System::arraycopy(second, 0, result, first.length, second.length)
		return result
	}

	protected EventSourceWeakSupport<FSMModelListener> listeners

	def private void init() {
		this.listeners = null
	}

	new(String text) {
		inMap = new HashMap<Port, InputPort>()
		outMap = new HashMap<Port, OutputPort>()
		updateContent(text)
	}

	def void updateContent(String txt) {
		init()
		this.parseContent(txt)
	}

	def FSM getFsm() {
		return fsm
	}

	def void setFsm(FSM fsm) {
		this.fsm = fsm
	}

	override FSMContent clone() {
		try {
			var FSMContent ret = super.clone() as FSMContent
			ret.fsm = EcoreUtil::copy(fsm)
			ret.listeners = null
			return ret
		} catch (CloneNotSupportedException ex) {
			return this
		}

	}

	def void addFSMModelListener(FSMModelListener l) {
		if (listeners === null) {
			listeners = new EventSourceWeakSupport<FSMModelListener>()
		}
		listeners.add(l)
	}

	def protected void fireContentSet() {
		if (listeners === null) {
			return;
		}
		var boolean found = false
		for (FSMModelListener l : listeners) {
			found = true
			l.contentSet(this)
		}  
		if (!found) {
			listeners = null
		}

	}

	def void removeFSMModelListener(FSMModelListener l) {
		if (listeners === null) {
			return;
		}
		listeners.remove(l)
		if (listeners.isEmpty()) {
			listeners = null
		}

	}

	static public final String TEMPLATE = 
	'''
	fsm example @[ 50 , 50 , 800 , 500 ] { 
		in A [ 3 ] @[ 50 , 100 , 44 , 15 ] ; 
		out X [ 4 ] @[ 807 , 140 , 43 , 15 ] ; 
		codeWidth = 2 ; 
		reset = S0 ; 
		state S0 = "01" @[ 297 , 181 , 30 , 30 ] { 
			commands @[ 246 , 173 , 50 , 40 ] { X = "0001" ; }
			transitions { 
				S0 -> S1 when default @[ 432 , 151 , 50 , 21 ] ; 
				S0 -> S3 when A== "000" @[ 346 , 269 , 68 , 21 ] ; 
			}
		} 
		state S1 = "10" @[ 470 , 186 , 30 , 30] { 
			commands @[ 522 , 190 , 40 , 40 ] { X = "0010" ; } 
			transitions { 
				S1 -> S2 when default @[ 533 , 276 , 50 , 21 ] ; 
				S1 -> S0 when A == "000" @[ 399 , 230 ,68 , 21 ] ; 
			}
		} 
		state S2 = "00" @[ 471 , 339 , 30 , 30 ] { 
			commands @[ 524 ,353 , 60 , 40 ] { 
				X = { "00" , A [ 1 ] , "1" } ;
			} 
			transitions { 
				S2 -> S3 when default @[ 392 , 398 , 50 , 21 ] ; 
				S2 -> S1 when A [ 2 : 1 ] == "11" @[ 557 ,250 , 90 , 21 ] ; 
			} 
		} 
		state S3 = "11" @[ 287 , 325 , 30 , 30 ] { 
			commands @[244 , 341 , 60 , 40 ] { X = "1000" ; } 
			transitions { 
				S3 -> S0 when default @[248 , 278 , 50 , 21 ] ; 
				S3 -> S2 when A == "000" @[ 388 , 313 , 68 , 21 ] ;
			}
		}
	}
	''';
	static final package int CLK = 0
	static final package int RST = 1
	static final package int EN = 2
	protected Port[] inputs
	protected Port[] outputs
	protected Map<Port, InputPort> inMap
	protected Map<Port, OutputPort> outMap
	protected String name
	FSM fsm
	Port[] ctrl

	def String getStringContent() {
		return FSMSerializer::saveAsString(fsm)
	}

	def Port[] getInputs() {
		if(inputs === null) return newArrayOfSize(0)
		return inputs
	}

	def int getInputsNumber() {
		if(inputs === null) return 0
		return inputs.length
	}

	def String getName() {
		if(fsm === null) return ""
		return fsm.getName()
	}

	def Port[] getOutputs() {
		if(outputs === null) return newArrayOfSize(0)
		return outputs
	}

	def int getOutputsNumber() {
		if(outputs === null) return 0
		return outputs.length
	}

	def Port[] getPorts() {
		return concat(ctrl, concat(inputs, outputs))
	}

	def Port[] getAllInPorts() {
		return concat(ctrl, inputs)
	}

	def int getPortsNumber() {
		if(inputs === null || outputs === null) return 0
		return ctrl.length + inputs.length + outputs.length
	}

	def private boolean parseContent(String content) {
		var FSMSerializer parser = new FSMSerializer()
		try {
			fsm = FSMSerializer.load(content.toString()) as FSM
			name = fsm.getName()
			var List<InputPort> inputsDesc = fsm.getIn() as List
			var List<OutputPort> outputsDesc = fsm.getOut() as List
			ctrl = newArrayOfSize(3)
			inputs = newArrayOfSize(inputsDesc.size())
			outputs = newArrayOfSize(outputsDesc.size())

			ctrl.set(CLK, new Port(0, FSMEntity::HEIGHT, Port::INPUT, 1))
			ctrl.set(RST, new Port(0, FSMEntity::HEIGHT + FSMEntity::PORT_GAP, Port::INPUT, 1))
			ctrl.set(EN, new Port(0, FSMEntity::HEIGHT + 2 * FSMEntity::PORT_GAP, Port::INPUT, 1))
		
			ctrl.get(CLK).setToolTip(Strings::getter("registerClkTip"))
			ctrl.get(RST).setToolTip(Strings::getter("registerRstTip"))
			ctrl.get(EN).setToolTip(Strings::getter("registerEnableTip"))
			inMap.clear
			
			for (var int i = 0; i < inputsDesc.size(); i++) {
				var InputPort desc = inputsDesc.get(i)
				
					inputs.set(i,
						new Port(0, ((i + ctrl.length) * FSMEntity::PORT_GAP) + FSMEntity::HEIGHT, Port::INPUT,
							desc.getWidth()))
				
				inputs.get(i).setToolTip(Strings::getter(desc.getName()))
				inMap.put(inputs.get(i), desc)
			}

			outMap.clear
			for (var int i = 0; i < outputsDesc.size(); i++) {
				var OutputPort desc = outputsDesc.get(i)
				{
					val _wrVal_outputs = outputs
					val _wrIndx_outputs = i
					_wrVal_outputs.set(_wrIndx_outputs,
						new Port(FSMEntity::WIDTH, ((i + ctrl.length) * FSMEntity::PORT_GAP) + FSMEntity::HEIGHT,
							Port::OUTPUT, desc.getWidth()))
				}
				{
					val _rdIndx_outputs = i
					outputs.get(_rdIndx_outputs)
				}.setToolTip(Strings::getter(desc.getName()))
				outMap.put({
					val _rdIndx_outputs = i
					outputs.get(_rdIndx_outputs)
				}, desc)
			}
			fireContentSet()
			return true
		} catch (Exception ex) {
			ex.printStackTrace()
			
			JOptionPane::showMessageDialog(null, ex.getMessage(), Strings::get("validationParseError"),
				JOptionPane::ERROR_MESSAGE)
			return false
		}

	}

	def Port[] getControls() {
		return ctrl
	}

}
