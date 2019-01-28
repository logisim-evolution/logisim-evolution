package com.cburch.logisim.statemachine.editor.view

import com.cburch.logisim.statemachine.PrettyPrinter

import com.cburch.logisim.statemachine.fSMDSL.*
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.geom.QuadCurve2D
import java.awt.geom.RoundRectangle2D
import org.eclipse.emf.ecore.EObject
import java.awt.BasicStroke
import com.cburch.logisim.statemachine.fSMDSL.LayoutInfo
import com.cburch.logisim.statemachine.fSMDSL.State
import com.cburch.logisim.statemachine.fSMDSL.FSMElement
import java.util.List
import java.awt.geom.CubicCurve2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.util.HashMap

class FSMLayout {

	static final public int RADIUS = 30

	static final public int CHAR_HEIGHT = 15

	static final public int PORT_HEIGHT = 15

	static final public int INPUT_X = 100
	static final public int OUTPUT_X = 100
	
	
	final public static int FSM_BORDER_X=30;
	final public static int FSM_BORDER_Y=30;
	final public static int FSM_TITLE_HEIGHT=30;
	
	final static boolean DEBUG=false;

	HashMap<State, Integer> map = new HashMap<State, Integer>();
	
	def layout(FSM e) {
		
	}
}