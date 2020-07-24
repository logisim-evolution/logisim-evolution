package com.cburch.logisim.statemachine.editor.view


import com.cburch.logisim.statemachine.fSMDSL.*
import java.awt.Point

import java.util.List
import com.cburch.logisim.statemachine.editor.FSMEditorController
import java.util.ArrayList
import com.cburch.logisim.statemachine.PrettyPrinter
import com.cburch.logisim.statemachine.editor.FSMView

class FSMSelectionZone {

	new() {
	}  
	
	def getSelectedElement(Point p,FSMElement e) {
		if (isWithinElement(p,e)) {
			e
		} else {
			null
		}
	}

	int xmin;
	int ymin;	
	int xmax;
	int ymax;
	
	static final boolean VERBOSE=true
	

	def updateBoundingBox(FSMElement e) {
		xmin= Math.min(xmin, e.layout.x);
		ymin= Math.min(ymin, e.layout.y);
		xmax= Math.max(xmax, e.layout.x+e.layout.width);
		ymax= Math.max(ymax, e.layout.y+e.layout.height);
	}

	def computeBoundingBox(FSM fsm) {
		xmin=Integer.MAX_VALUE
		ymin=Integer.MAX_VALUE;
		xmax=0;
		ymax=0;
		updateBoundingBox(fsm)
		for(s:fsm.states) {
			
			updateBoundingBox(s);
			for(t:s.transition) {
				updateBoundingBox(t)
			}
		}
	} 
	
	public enum AreaType {INPUT,  OUTPUT, STATE, TRANSITION, NONE}
	
	def void detectElement(Point p, FSMElement o, List<FSMElement> l) {
		val isWithinElement = isWithinElement(p, o)
		if (isWithinElement && o !== null && l !== null && l.size()==0) {
			l.add(o)
		}

	}
	def void detectElement(Zone z, FSMElement o, List<FSMElement> l) {
		val isWithinElement = isWithinZone(z, o)
		if (isWithinElement && o !== null && l !== null) {
			l.add(o)
		}

	}

	def List<FSMElement> getElementsInZone(FSM fsm,Zone z) {
		var List<FSMElement> candidates = new ArrayList<FSMElement>()
		
		
		detectElement(z, fsm, candidates)
		for (Port ip : fsm.getIn()) {
			detectElement(z, ip, candidates)
		}
		for (Port op : fsm.getOut()) {
			detectElement(z, op, candidates)
		}
		for (State s : fsm.getStates()) {
			detectElement(z, s, candidates)
			detectElement(z, s.getCommandList(), candidates)
			for (Transition t : s.getTransition()) {
				detectElement(z, t, candidates)
			}

		}
		println(candidates)
		return candidates			
	}
	
	def setSelectionZone(Zone z) {
		
	}
	
	
	def List<FSMElement> getSelectedElementsAt(Point p) {
	
	}
		
	def List<FSMElement> getSelectedElements(FSM fsm,Point p) {
		var List<FSMElement> candidates = new ArrayList<FSMElement>()
		
		detectElement(p, fsm, candidates)
		for (Port ip : fsm.getIn()) {
			detectElement(p, ip, candidates)
		}
		for (Port op : fsm.getOut()) {
			detectElement(p, op, candidates)
		}
		for (State s : fsm.getStates()) {
			detectElement(p, s, candidates)
			detectElement(p, s.getCommandList(), candidates)
			for (Transition t : s.getTransition()) {
				detectElement(p, t, candidates)
			}

		}
		
		for (c:candidates) println("\t"+PrettyPrinter.pp(c))
		return candidates	
	}
	
	def public AreaType getAreaType(FSM fsm,Point p) {
		val List<FSMElement> selection = getSelectedElements(fsm,p);
		if (selection.size()>0) {
			val first = selection.get(0)
			if(first instanceof State) {
				return AreaType.TRANSITION
			}
		}
		computeBoundingBox(fsm);
		if(p.y>ymin && p.y<ymax) {
			if(p.x<xmin) {
				return AreaType.INPUT
			} else if(p.x>xmax) {
				return AreaType.OUTPUT
			} else {
				return AreaType.STATE
			}
		} else {
			return AreaType.NONE
		}
	}

	def dispatch boolean isWithinElement(Point p,FSMElement e) {
		val l= e.layout	
		debug("check if ("+p.x+','+p.y+") within "+e.class+" ["+(l.x)+","+(l.y)+","+(l.x+l.width)+","+(l.y+l.height)+"]")
		if (inRectangle(p.x,p.y,e.layout)) {
			println("\tYES !")
			return true
		}
		false
	}
	
	def debug(String string) {
		if (VERBOSE) println(string)
	}

	public static def inRectangle(int x, int y, LayoutInfo l) {
		if (l.height>0)
		return (
			(x >= l.x) && (x <= (l.x+l.width)) &&
			(y >= l.y) && (y <= (l.y+l.height))
			)
		else
		return (
			(x >= l.x) && (x <= (l.x+l.width)) &&
			(y >= l.y+l.height) && (y <= (l.y))
			)
	}
	
	def dispatch isWithinElement(Point p,CommandList e) {
		val l= e.layout	
		debug("check if ("+p.x+','+p.y+") within CommandList["+(l.x)+","+(l.y)+","+(l.x+l.width)+","+(l.y+l.height)+"]")
		if (inRectangle(p.x,p.y,e.layout)) {
			debug("\tYES !")
			return true
		}
		false
	}
	
	def dispatch isWithinElement(Point p,FSM e) {
		val l= e.layout	
		debug("check if ("+p.x+','+p.y+") within FSM ["+(l.x)+","+(l.y+FSMDrawing.FSM_TITLE_HEIGHT)+","+(l.x+l.width)+","+((l.y+FSMDrawing.FSM_TITLE_HEIGHT))+"]")
		if (p.x>l.x && p.x <(l.x+l.width)) {
			if (p.y>l.y && p.y <(l.y+FSMDrawing.FSM_TITLE_HEIGHT)) {
				debug("\tYES !")
				return true
			}
		}
		false
	}

	def dispatch isWithinElement(Point p,State e) {
		val l = e.layout
		val radius = l.width
		val dx = (p.x - (l.x+radius)) 
		val dy = (p.y - (l.y+radius))
		val distance = Math.sqrt(dx*dx+dy*dy)
		debug("check if ("+p.x+','+p.y+") within circle["+(l.x+radius)+","+(l.y+radius)+","+radius+"] -> distance = "+distance+"  ")
		if (distance<radius) {
			debug("\tYES !")
			return true
		}
		false
	}

	def dispatch isWithinElement(Point p,Transition e) {
		val l = e.layout
		debug("check if ("+p.x+','+p.y+") within Transition["+(l.x)+","+(l.y)+","+(l.width)+","+(l.height)+",]   ")
		if (inRectangle(p.x,p.y,e.layout) && e.dst!=null) {
			debug("\tYES !")
			return true
		}
		false
	}

	def dispatch isWithinElement(Point p,InputPort e) {
		val l= e.layout	
		debug('('+p.x+','+p.y+") within InPort["+(l.x)+","+(l.y)+","+(l.x+l.width)+","+(l.y+l.height)+"]")
		if (inRectangle(p.x,p.y,e.layout)) {
			debug("\tYES !")
			return true
		}
		false
	}

	def dispatch isWithinElement(Point p,OutputPort e) {
		val l= e.layout	
		debug('('+p.x+','+p.y+") within OutPort["+(l.x)+","+(l.y)+","+(l.x+l.width)+","+(l.y+l.height)+"]")
		if (inRectangle(p.x,p.y,e.layout)) {
			debug("\tYES !")
			return true
		}
		false
	}
	
	
	/////////////////////////////////
	
	
	def dispatch isWithinZone(Zone p,FSMElement e) {
	 	p.contains(new Zone(e.layout)) 
	}
	
	def dispatch isWithinZone(Zone p,FSM e) {
	 	val l = e.layout
	 	p.contains(new Zone(l.x,l.y,l.x+l.width,l.y+FSMDrawing.FSM_TITLE_HEIGHT)) 
	}

	def dispatch isWithinZone(Zone p,State e) {
		val l = e.layout
	 	p.contains(new Zone(l.x,l.y,l.x+2*l.width,l.y+2*l.width)) 
	}
	
	def dispatch isWithinZone(Zone p,Transition e) {
		p.contains(new Zone(e.layout))  && e.dst!=null
	}

	
	
	
}