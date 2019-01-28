package com.cburch.logisim.statemachine.analysis

import com.cburch.logisim.data.BitWidth
import com.cburch.logisim.data.Value
import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.InputPort
import com.cburch.logisim.statemachine.fSMDSL.Port
import java.util.ArrayList
import java.util.List
import org.eclipse.emf.common.util.BasicEList
import org.eclipse.emf.common.util.EList

class InputVectorGenerator {
	
	var List<Integer> current = new ArrayList<Integer>;
	var List<Integer> widths = new ArrayList<Integer>;
	
	static final boolean QUOTE = false;
	
	
	
	new(FSM fsm) {
		var wl = new ArrayList(fsm.in.map[p|p.width])
		wl+=fsm.width
		for (w:wl) {
			current+=0
			widths+=w
		}
	}

	def getSize() {
		current.size
	}

	def getValue(int i) {
		current.get(i)
	}

	def getQuotedBinaryValue(int i) {
		'''"«getBinaryValue(i)»"'''.toString
	}

	def getBinaryValue(int i) {
		val v = current.get(i)
		val w = widths.get(i)
		val str= Integer.toBinaryString(v)
		String.format("%"+w+"s", str).replace(" ", "0")
	}



	def public boolean inc() {
		inc(current.size-1)
	}
	
	def private boolean inc(int pos) {
		if(pos>=0) {
			var newVal = current.get(pos)+1 
			newVal = newVal % ((1<<(widths.get(pos))))
			current.set(pos,newVal);
			if(newVal==0) {
				return inc(pos-1)
			}
			true
		} else {
			false
		}
	}
	
	override String toString() {
		var res = ""
		for (i:0..(current.size-1)) {
			val v = Value.createKnown(BitWidth.create(widths.get(i)),current.get(i))
			if(QUOTE) {
				if (i>0) res+= ";"
				res += "\""+v.toBinaryString+"\""
			} else {
				res += v.toBinaryString
			} 
		}
		res
	}
	
}