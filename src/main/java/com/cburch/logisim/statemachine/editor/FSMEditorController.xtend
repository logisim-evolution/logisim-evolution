package com.cburch.logisim.statemachine.editor

// Drawing.java
// By Matthew McNierney for CS5 Lab Assignment #3
// Holds various methods for manipulating and storing the shapes in the drawing.

import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.FSMElement
import com.cburch.logisim.statemachine.fSMDSL.InputPort
import com.cburch.logisim.statemachine.fSMDSL.Port
import com.cburch.logisim.statemachine.fSMDSL.State
import com.cburch.logisim.statemachine.fSMDSL.Transition
import java.awt.Graphics2D
import java.awt.Point
import java.util.ArrayList
import java.util.List
import javax.swing.JOptionPane
import org.eclipse.emf.ecore.EObject
import com.cburch.logisim.statemachine.editor.FSMView
import java.awt.event.MouseEvent
import com.cburch.logisim.statemachine.editor.view.*
import com.cburch.logisim.statemachine.fSMDSL.LayoutInfo
import com.cburch.logisim.statemachine.fSMDSL.CommandList
import org.eclipse.emf.ecore.util.EcoreUtil
import com.cburch.logisim.statemachine.fSMDSL.OutputPort
import java.awt.Stroke
import java.awt.BasicStroke
import com.cburch.logisim.statemachine.editor.view.Zone
import com.cburch.logisim.statemachine.editor.view.FSMSelectionZone
import com.cburch.logisim.statemachine.editor.view.FSMSelectionZone.AreaType
import java.util.HashMap
import java.util.Map
import com.cburch.logisim.statemachine.PrettyPrinter

class FSMEditorController {
	
	// ArrayList holding all of the shapes
	private FSM fsm
	private FSMDrawing drawing
	private FSMEdit edit
	private FSMView view;
	private FSMRemoveElement remover
	
	
	List<FSMElement> activeSelection = new ArrayList<FSMElement>
	
	Transition newTransition
	
	List<FSMElement> clipboard= new ArrayList<FSMElement>
	
	

	Point zoneStart
	Point zoneEnd
	Point lastPos
	
	Zone selectionZone

	public enum CtrlState {IDLE, SELECT_ZONE,  MOVE_ZONE, SELECT_ELT, MOVE_ELT, SELECT_DST, ERROR_STATE}

	public CtrlState state = CtrlState.IDLE
	
	public static final boolean DEBUG = true;
	
	Point copyStart
	
	Map<State,State> copyMap = new HashMap<State,State>;
	
	FSMSelectionZone zones
	
	Point contextSelection
	
	/** 
	 * Constructor. Initialize the color to the default color and create the
	 * ArrayList to hold the shapes.
	 * @param defaultColor
	 */
	new(FSMView view, FSM fsm ) {
		this.fsm = fsm
		this.view = view
		
		drawing = new FSMDrawing()
		
		edit = new FSMEdit()
		remover = new FSMRemoveElement(fsm)
		zones = new FSMSelectionZone()
		
	}

	def int getNbState() {
		return fsm.getStates().size()
	}

	def List<FSMElement> getActiveSelection() {
		return activeSelection
	}

	def List<FSMElement> getClipboard() {
		return clipboard
	}

	def void showContextMenu() {
		contextSelection = view.scaledPosition
		view.showContextMenu(zones.getAreaType(fsm,view.scaledPosition))
	}

	def List<FSMElement> getCurrentSelection() {
		return zones.getSelectedElements(fsm,view.scaledPosition)
	}

	def getContextSelection() {
		// Handles some bugs in W7 
		return zones.getSelectedElements(fsm,contextSelection);
	}


	def List<FSMElement> getElementsWithin(Zone z) {
		return zones.getElementsInZone(fsm, z)
	}

    def findUnassignedStateCode() {
		var map = new HashMap<String,State>(); 
		for (s:fsm.states) map.put(s.code,s)
		val ub = ((1<<fsm.width)-1);
		for(n:0..ub) {
			var code = Integer.toBinaryString(n);
			while (code.length() < fsm.width) {    //pad with 16 0's
        		code = "0" + code;
  			}
			code = '"' + code+'"'
			if (!map.containsKey(code)) {
				return code
			}
		}
		null
    }

	def addNewState(int x, int y) {
		val code = findUnassignedStateCode;
		if(code!==null)
			fsm.states.add(FSMCustomFactory.state("S"+fsm.states.size,code,x,y))
		
	}

	def addNewTransition(State src,int x, int y) {
		val t= FSMCustomFactory.transition(src,null,x,y)
		if(t.src===null && src!==null) {
			(t.src=src)
		}
		t
	}

	def addInputPort(InputPort ip) {
		fsm.in.add(ip)
	}

	
	def addNewInputPort(int x, int y) {
		fsm.in.add(FSMCustomFactory.inport("I"+fsm.in.size,1,x,y))
	}

	def addNewOutputPort(int x, int y) {
		fsm.out.add(FSMCustomFactory.outport("O"+fsm.out.size,1,x,y))
	}
	/** 
	 * Draw all of the shapes.
	 * @param g
	 */
	def void draw(Graphics2D g) {
				
		if (fsm!=null) {
			drawing.drawElement(fsm,g,activeSelection)
			g.drawString(state.toString,20,20);
			if(selectionZone!=null) {
				val float[] f = #[3.0 as float]
				g.stroke = new BasicStroke(0.3 as float, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,f, 0);
	        	val old = g.stroke
	        	val p0= selectionZone.x0 
	        	val p1= selectionZone.x1
				g.drawRect(p0.x,p0.y,Math.abs(p1.x-p0.x),Math.abs(p1.y-p0.y))
				g.stroke = (old);
			}
		}
	}

	/** 
	 * Get the front most shape at a given point. Creates a temp array with all
	 * of the shapes at a given point, and returns the last shape in the array.
	 * @param p
	 * @return frontmost shape
	 */


	private def setLayout(int x, int y, FSMElement e) {
		var LayoutInfo layout = e.getLayout()
		layout.setX(x)
		layout.setY(y)
		
	} 
	public def dispatch move(int dx, int dy, FSMElement e) {
		setLayout(e.layout.x+dx,e.layout.y+dy,e)
	}

	def double distance(int xa,int ya,int xb,int yb) {
		val dx= xa-xb
		val dy = ya - yb
		Math.sqrt(dx*dx+dy*dy) 
	}
	
	public def dispatch move(Point last, Point current, List<FSMElement> list) {
		// FIXME 
		list.forEach[e|move(current.x-last.x,current.y-last.y,e)]
		view.repaint
	}


	public def dispatch move(int dx, int dy, CommandList cl) {
		val state = cl.eContainer as State
		val layout = state.layout
		val p =new Point(cl.layout.x+dx,cl.layout.y+dy);
		if(distance(p.x,p.y,layout.x, layout.y)<=400) {
			setLayout(p.x,p.y,cl);
		} else {
			setLayout(layout.x+45,layout.y+15, cl);		
		}
		view.repaint
	}

	public def dispatch move(int dx, int dy, State s) {
		setLayout(s.layout.x+dx,s.layout.y+dy,s);
		setLayout(s.layout.x+45,s.layout.y+15, s.commandList);		
		view.repaint
	}

	
	def executeEdit(Point p) {
		val List<FSMElement> selection = getCurrentSelection();
		if (selection.size()>0) {
			val first = selection.get(0)
			edit.edit(first);
			view.repaint
		}
	}

	def executeDelete(Point p) {
		if (DEBUG) println ("[Delete] command "+state+ " state");
		val List<FSMElement> selection = getContextSelection();
		if (selection.size()>0) {
			for (i:0..selection.size-1) {
				val first = selection.get(i)
				if (DEBUG) println ("[Delete] object "+PrettyPrinter.pp(first));
				remover.remove(first);
				view.repaint
			}
		}
	}
	


	def executeCopy(Point p) {
		copyStart =p
		clipboard= new ArrayList();
		copyMap.clear()
		for (e:activeSelection) copyToClipboard(e)

	    for (e: clipboard) {
	    	switch (e) {
	    		State : {
					for (t : e.transition) {
						if(copyMap.containsKey(t.dst)) {
							t.dst=copyMap.get(t.dst)
						}
					}
	    		}
	    		Transition : {
					if(copyMap.containsKey(e.dst)) {
						e.dst=copyMap.get(e.dst)
					}
					if(copyMap.containsKey(e.src)) {
						e.src=copyMap.get(e.src)
					}
	    		}
	    	}
	    }
	}  

	def execute(List<FSMElement> l) {
	}
	

	def dispatch copyToClipboard(State e) {
		val cp = EcoreUtil.copy(e) as State
		copyMap.put(e,cp)
		clipboard.add(cp);
	}

	def dispatch copyToClipboard(Port e) {
		clipboard.add(EcoreUtil.copy(e));
	}

	def dispatch copyToClipboard(FSMElement e) {
		
	}

	def executePaste(Point p) {
		val dx = p.x- copyStart.x ;
		val dy = p.y- copyStart.y ;
		
		for (e  :clipboard) {
			paste(e, dx,dy);
		}
		view.repaint
	
	}  

	def dispatch paste(FSMElement e, int x, int y) {
		if(activeSelection.size==1 && activeSelection.get(0) instanceof InputPort) {
			val ip = (activeSelection.get(0) as InputPort)
			fsm.in.add(ip);
			ip.layout.x=x
			ip.layout.y=y
		} else {
			throw new RuntimeException("Incompatible selection for paste "+activeSelection);
		}
	}
	
	
	def dispatch paste(InputPort e,int dx, int dy) {
		e.layout.y=dy+e.layout.y
		e.name="copy_of_"+e.name
		
		fsm.in.add(e);
	}

	def dispatch paste(OutputPort e,int dx, int dy) {
		e.layout.y=dy+e.layout.y
		e.name="copy_of_"+e.name
		fsm.out.add(e);
	}

	def dispatch paste(State e,int dx, int dy) {
		e.layout.x=e.layout.x+dx
		e.layout.y=e.layout.y+dy
		e.name="copy_of_"+e.name
		fsm.states.add(e);
		for(t:e.transition) {
			paste(t,dx,dy)
		}
	} 

	
	def dispatch paste(Transition e,int dx, int dy) {
		e.layout.x=e.layout.x+dx
		e.layout.y=e.layout.y+dy
	} 

	def executeCreate(Point p, AreaType type) {
		switch(type) {
			case AreaType.INPUT :{
				addNewInputPort(p.x,p.y);
			}
			case AreaType.OUTPUT :{
				addNewOutputPort(p.x,p.y);
			}
			case AreaType.STATE:{
				addNewState(p.x,p.y);
			}
			case AreaType.TRANSITION :{
				if(activeSelection.size==1) {
					state=CtrlState.SELECT_DST
					val state = activeSelection.get(0) as State
					newTransition = addNewTransition(state,p.x,p.y); 
					println("Create Transition from "+state.name)
				} else {
					throw new RuntimeException("Unsupported case");
				}
			}
			default: {
				return
			}
		}
		view.repaint  
	
	}  
	
	
	def executeDoubleClick(Point p) {
		switch(state) {
			case IDLE :{
				executeEdit(p)
			}
			default: {
				if (DEBUG) println ("[RightClick] state "+state+ "-> ERROR !!!!");
				state=CtrlState.ERROR_STATE
						
			}
		}
	}
	
	
	def executeRightClick() {
		switch(state) {
			case IDLE :{
				if (DEBUG) println ("[RightClick] show context menu (state="+state+")");
				view.repaint
				showContextMenu
			}
			
			/// MOVE_ZONE, SELECT_ELT, MOVE_ELT, SELECT_DST, ERROR_STATE
			default: {
				if (DEBUG) println ("[RightClick] going back to IDLE!!!!");
				if (newTransition!=null) {
					newTransition.src=null;
					newTransition=null;
				}
				state=CtrlState.IDLE
			}
			
		}
		view.repaint
		
	}
	
	  
	def executePress(Point p) {
		val localSelection  = currentSelection;
		switch(state) {
			case IDLE :{
				if(localSelection.size>0) {
					if (!activeSelection.contains(localSelection.get(0))) {
						// If the newly selected object is not part of the active objects
						// we remove it
						activeSelection =localSelection
					}
					state=CtrlState.MOVE_ELT
					zoneStart=new Point(p);
					lastPos= new Point(p)
					if (DEBUG) println ("[Press] state IDLE->"+state+ " selection ="+activeSelection+", zone="+selectionZone);
				} else {
					state=CtrlState.SELECT_ZONE	
					zoneStart=p;
					selectionZone= new Zone(p,p);
					if (DEBUG) println ("[Press] state IDLE->"+state+ " no selection");
				}	
			}
			case SELECT_DST :{
				if (DEBUG) println ("[Press] state "+state+ "-> "+state+ "!!!!");
			}
			/// MOVE_ZONE, SELECT_ELT, MOVE_ELT, SELECT_DST, ERROR_STATE
			
			case ERROR_STATE :{
				state=CtrlState.IDLE
			}
			
		}
		view.repaint
	}
	
		
	def executeDragged(Point p) {	
		switch(state) {
			case MOVE_ELT:{
				move(lastPos,p,activeSelection);
				lastPos=p
			}
			case SELECT_ZONE:{
				zoneEnd=p;
				selectionZone= new Zone(zoneStart,zoneEnd);
				if (DEBUG) println ("[Dragged] state SELECT_ZONE->"+state+ " : extending selection zone");
			}
			default: {
				if (DEBUG) println ("[Dragged] state "+state+ "-> ERROR !!!!");
				state=CtrlState.ERROR_STATE
			}
			
		}
		view.repaint
	}

	def executeRelease(Point p) {
		switch(state) {
			case SELECT_DST: {
				if (newTransition != null) {
					if (DEBUG) println ("[Move] state "+state+ "-> choosing transition destination state");
					val selection = currentSelection;
					if (selection.size()==1) {
						val first = selection.get(0)
						if (first instanceof State) {
							// TODO : make a function for the following code
							val LayoutInfo layout = newTransition.getLayout();
							val LayoutInfo srcLayout = newTransition.getSrc().getLayout();
							layout.setX((p.x+srcLayout.getX())/2);
							layout.setY((p.y+srcLayout.getY())/2);
							if(first!=newTransition.getSrc()) {
								newTransition.setDst(first);
							} else {
								deleteElement(newTransition);
							}
						} else if(newTransition!=null) {
							deleteElement(newTransition);
						}
						newTransition=null;
					} else {
						deleteElement(newTransition);
						newTransition=null;
					}
				}
				state=CtrlState.IDLE
			}
			case MOVE_ELT:{
				state=CtrlState.IDLE
				if (DEBUG) println ("[Release] state "+state+ "-> IDLE (end of move)");
				lastPos=null
			}
			case SELECT_ZONE:{
				state=CtrlState.IDLE
				if (DEBUG) println ("[Release] state "+state+ "-> IDLE (end of zone selection)");
				zoneStart=null;
				zoneEnd=null;
				activeSelection=getElementsWithin(selectionZone)
				selectionZone= null;
				
				
			}
			default: {
				if (DEBUG) println ("[Release] state "+state+ "-> ERROR !!!!");
				if (newTransition!=null) deleteElement(newTransition);
				state=CtrlState.ERROR_STATE
			}
			
		}
		view.repaint
	}

	def executeMove(Point p) {
		switch(state) {
			case SELECT_DST:{
				if (newTransition != null) {
					if (DEBUG) println ("[Move] state "+state+ "-> choosing transition destination state");
					val layout = newTransition.getLayout();
					layout.setX(p.x);
					layout.setY(p.y);
				} else {
					if (DEBUG) println ("[Release] state "+state+ "-> ERROR !!!!");
					state=CtrlState.ERROR_STATE
				}
			}
			case IDLE:{
				if (DEBUG) println ("[Move] state "+state+ "-> IDLE");
				zoneEnd=p;
			}
			default: {
				if (DEBUG) println ("[Release] state "+state+ "-> ERROR !!!!");
				state=CtrlState.ERROR_STATE
			}
			
		}
		view.repaint
	}
	
	def executeLeftClick(Point scaledP) {
		if (DEBUG) println ("[LeftClick] state "+state );
		view.revalidate
		view.repaint
	}
	

	
	/** 
	 * Removes a shape from the drawing.
	 * @param shape
	 */
	def dispatch void deleteElement(FSM e) {
		// do not remove the FSM itself !
	}

	/** 
	 * Removes a shape from the drawing.
	 * @param shape
	 */
	def dispatch void deleteElement(FSMElement e) {
		remover.remove(e)
		view.repaint
		
	}

	/** 
	 * Moves a shape to the front of the drawing.
	 * @param shape
	 */
	def void editElement(FSMElement e) {
		edit.edit(e)
	}


	def getFSM() {
		fsm
	}
}
