package com.cburch.logisim.statemachine.editor.view

import java.awt.Point
import com.cburch.logisim.statemachine.fSMDSL.LayoutInfo

class Zone {

	Point x0;
	Point x1;
	
	new(Point a,Point b) {
		x0 = new Point(Math.min(a.x,b.x),Math.min(a.y,b.y));
		x1 = new Point(Math.max(a.x,b.x), Math.max(a.y,b.y));
	}

	new(Point a) {
		x0 = new Point(a);
		x1 = new Point(a);	
	}

	def resize(Point b) {
		
	}
	def isSinglePoint() {
		x1==x0 && x0!=null
	}
	new(int xa, int ya, int xb, int yb) {
		x0 = new Point(xa,ya);
		x1 = new Point(xb,yb);
	}

	new(LayoutInfo l) {
		x0 = new Point(l.x,l.y);
		x1 = new Point(l.x+l.width,l.y+l.height);
	}

	def contains(Point p) {
		if(isSinglePoint) {
			false
		} else {
			(p.x >= x0.x) && (p.x <= x1.x) &&
			(p.y >= x0.y) && (p.y <= x1.y) 
		}
	}	

	def contains(Zone p) {
		if(isSinglePoint) {
			false
		} else {
			contains(p.x0) && contains(p.x1)  
		}
	}	

	def getX0() {
		x0
	}

	def getX1() {
		x1
	}

	override public def String toString() {
		'''(«x0.x»,«x0.y»)->(«x1.x»,«x1.y»))'''
	}
}