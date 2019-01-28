package com.cburch.logisim.statemachine.editor

import java.awt.Point

class SelectionZone {
	
	Point start
	Point end

	new() {
		
	}
	
	def isSinglePoint() {
		 ((start!=null && end==null) ||(end.equals(start)))
	}
	
	def start(Point point) {
		start= new Point(point);
	}
	def extend(Point point) {
		end= new Point(point);
	}

	def clear() {
		start=null;
		end=null;
	}
	
}