package com.cburch.logisim.statemachine.editor

// Drawing.java
// By Matthew McNierney for CS5 Lab Assignment #3
// Holds various methods for manipulating and storing the shapes in the drawing.


import com.cburch.logisim.statemachine.editor.FSMView
import com.cburch.logisim.statemachine.fSMDSL.*
import java.awt.Point

class FSMEditorControl{
	

	public enum Event {
		LEFT_PRESS, 
		LEFT_CLICK, 
		LEFT_DCLICK, 
		LEFT_DRAGGED, 
		LEFT_RELEASE, 
		RIGHT_PRESS, 
		MOUSE_MOVE, 
		RIGHT_CLICK, 
		RIGHT_RELEASE 
	}

	public enum CtrlState {
		IDLE,
		SELECT_ZONE,
		EXTEND_SEL_ZONE,
		SELECT_ELT,
		MOVE_ELT,
		SELECT_DST
	}

	public CtrlState state = CtrlState.IDLE
	
	public static final boolean DEBUG = true;
	
	SelectionZone zone = new SelectionZone();
//	FSMEditorController ctrl;
//	
//	FSMView view
	
	new(FSMEditorController ctrl) {
//		this.ctrl=ctrl;
	}

	def configureElement(FSMElement element) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	
	def updateNewTransitionDst(Point point) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	def cancelNewTransition() {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	def finalizeNewTransition(Point point) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	def FSMElement getObjectAt(Point point) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	def moveSelection(Point point) {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	def debug(CtrlState state, String string) {
		if(DEBUG) println(state.toString+string)
	}	

	def showContextMenu() {
		throw new UnsupportedOperationException("TODO: auto-generated method stub")
	}
	
	def handleEvent(Event e, Point p) {
		switch (state) {
			case IDLE: {
				switch(e) {
					case LEFT_DCLICK :{
						val target = getObjectAt(p);
						if (target!==null) {
							configureElement(target)
						}
					}
					case RIGHT_CLICK :{
						debug(state, "show context menu");
						showContextMenu();
					}
					case RIGHT_PRESS:{
						zone.start(p);
						if (getObjectAt(p)==null) {
							state= CtrlState.MOVE_ELT
						} else {
							state= CtrlState.SELECT_ZONE
						}
					}
				}
			}
			case SELECT_ZONE:{
				switch(e) {
					case RIGHT_CLICK :{
						state=CtrlState.IDLE;
					}
					case LEFT_RELEASE :{
						state=CtrlState.IDLE;
					}
					case LEFT_DRAGGED :{
						zone.extend(p);
					}
				}
			}
			case MOVE_ELT:{
				switch(e) {
					case LEFT_DRAGGED :{
						moveSelection(p);
					}
					default :{
						state=CtrlState.IDLE;
					}
				}
				
			}
			case SELECT_DST:{
				switch(e) {
					case LEFT_RELEASE:{
						finalizeNewTransition(p);
						state=CtrlState.IDLE;
					}
					case MOUSE_MOVE :{
						updateNewTransitionDst(p);
					}
					default :{
						cancelNewTransition();
						state=CtrlState.IDLE;
					}
				}
			}
			default: {
				throw new UnsupportedOperationException("Unsupported case")
			}
		}
	}
	
	


}
