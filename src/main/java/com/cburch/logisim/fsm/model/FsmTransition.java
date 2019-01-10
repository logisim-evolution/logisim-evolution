package com.cburch.logisim.fsm.model;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class FsmTransition implements FsmStateListener,FsmInputListListener {
	
	public static final int TransitionOffset = 10;
	public static final Color ActiveColor = new Color(51,153,255);
	public static final Color PassiveColor = new Color(0,0,200);
	public static final int ArrowWidth = 10;
	public static final int ArrowHeight = 6;

	private boolean active = false;
	private FsmState sourceState;
	private FsmState destState;
	private Location sourceConnect;
	private Location destConnect;
	private boolean unconditional = true;
	private boolean ToBeRemoved = false;

	public FsmTransition(FsmState source , FsmState dest) {
		sourceState = source;
		destState = dest;
	}
	
	/* Here the event listeners are defined */
	@Override
	public void FsmInputListChanged(FsmInputListEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void StateChanged(FsmStateEvent event) {
		if (event.getSource().equals(sourceState)) {
			if (event.getReason()==FsmStateEvent.StateIsNoLongerCurrentState)
				active=false;
			else if (event.getReason()==FsmStateEvent.StateRemoved)
				ToBeRemoved = true;
			else if (event.getReason()==FsmStateEvent.StateIsCurrentState) {
				if (unconditional)
					active = true;
				else {
					/* TODO: Determine if this event is active  and update data structure */
				}
			}
		}
		if (event.getSource().equals(destState)) {
			if (event.getReason()==FsmStateEvent.StateRemoved)
				ToBeRemoved = true;
		}
	}
	
	/* Here all other methods are defined */
	public boolean IsActive() {
		return active;
	}
	
	public FsmState getSourceState() {
		return sourceState;
	}
	
	public FsmState getNextState() {
		return destState;
	}
	
	public boolean ToBeRemoved() {
		return ToBeRemoved;
	}
	
	public boolean IsUnconditional() {
		return unconditional;
	}
	
	/* Here the gui related stuff is defined */
	private Bounds DetermineConnectionPoints(Bounds start, Bounds dest, FontMetrics metrics) {
		int deltaX = dest.getCenterX()-start.getCenterX();
		int deltaY = dest.getCenterY()-start.getCenterY();
		boolean SameY = deltaY == 0;
		boolean SameX = deltaX == 0;
		int SourceDeltaX = start.getWidth()/2;
		int SourceDeltaY = start.getHeight()/2;
		boolean InReagionA = SameY;
		if (SameX) {
			InReagionA = false;
		} else if (!SameY) {
			double SourceIncline = (double) SourceDeltaY / (double) SourceDeltaX;
			double RelY = (deltaX < 0) ? -SourceIncline*(double)deltaX : SourceIncline*(double)deltaX; 
			int RelDestLocation = (RelY > (double) Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) RelY;
			int AbsdeltaY = deltaY < 0 ? -deltaY : deltaY;
			InReagionA = AbsdeltaY <= RelDestLocation;
		}
		/* TODO: take into account the size of the condition string */
		int startX=0,startY=0,destX=0,destY=0;
		if (InReagionA) {
			if (deltaX > 0) {
				// We have the connection on the right side of the source and the left side of the destination 
				startX = start.getX()+start.getWidth();
				startY = start.getCenterY();
				destX = dest.getX();
				destY = (SameY) ? dest.getY()+TransitionOffset : 
					(deltaY > 0) ? dest.getY()+TransitionOffset: dest.getY()+dest.getHeight()-TransitionOffset;
			} else {
				// We have the connection on the left side of the source and the right side of the destination
				startX = start.getX();
				startY = start.getCenterY();
				destX = dest.getX()+dest.getWidth();
				destY =(SameY) ? dest.getY()+dest.getHeight()-TransitionOffset :
					(deltaY > 0) ? dest.getY()+TransitionOffset: dest.getY()+dest.getHeight()-TransitionOffset;
			}
		} else {
			if (deltaY < 0) {
				// We have the connection on the top side of the source and the bottom side of the destination
				startX = start.getCenterX();
				startY = start.getY();
				destX = (SameX) ? dest.getX()+TransitionOffset :
					(deltaX > 0) ? dest.getX()+TransitionOffset : dest.getX()+dest.getWidth() - TransitionOffset ;
				destY = dest.getY()+dest.getHeight();
			} else {
				// We have the connection on the bottom side of the source and the top side of the destination
				startX = start.getCenterX();
				startY = start.getY()+start.getHeight();
				destX = (SameX) ? dest.getX()+dest.getWidth()-TransitionOffset :
					(deltaX > 0) ? dest.getX()+TransitionOffset : dest.getX()+dest.getWidth()-TransitionOffset ;
				destY = dest.getY();
			}
		}
		sourceConnect = Location.create(startX, startY);
		destConnect = Location.create(destX, destY);
		return Bounds.create(startX>destX ? destX : startX,
				             startY>destY ? destY : startY, 
				             startX>destX ? startX-destX : destX - startX,
				             startY>destY ? startY-destY : destY - startY);
	}
		
	public Bounds getBounds(FontMetrics metrics) {
		if ((sourceState == null) || (destState==null))
			return Bounds.EMPTY_BOUNDS;
		Bounds sourceBounds = sourceState.getSize(metrics);
		Bounds destBounds = destState.getSize(metrics);
		if (sourceState.equals(destState)) {
			/* special case for transition into the same state */
			sourceConnect = Location.create(sourceBounds.getX()+TransitionOffset, sourceBounds.getY());
			destConnect = Location.create(sourceBounds.getX()+sourceBounds.getWidth()-TransitionOffset, sourceBounds.getY());
			int width = sourceBounds.getWidth()-2*TransitionOffset;
			int height = (unconditional) ? width : width+metrics.getHeight();
			/* TODO: take width of condition into account */
			return Bounds.create(sourceBounds.getX()+TransitionOffset, sourceBounds.getY()-height/2, width, height);
		}
		return DetermineConnectionPoints(sourceBounds,destBounds,metrics);
	}

	/**
	 * Draw an arrow line between two points.
	 * @param g the graphics component.
	 * @param x1 x-position of first point.
	 * @param y1 y-position of first point.
	 * @param x2 x-position of second point.
	 * @param y2 y-position of second point.
	 * @param d  the width of the arrow.
	 * @param h  the height of the arrow.
	 * (taken from : https://stackoverflow.com/questions/2027613/how-to-draw-a-directed-arrow-line-in-java)
	 */
	private void drawArrowLine(Graphics g, int x1, int y1, int x2, int y2, int d, int h) {
	    int dx = x2 - x1, dy = y2 - y1;
	    double D = Math.sqrt(dx*dx + dy*dy);
	    double xm = D - d, xn = xm, ym = h, yn = -h, x;
	    double sin = dy / D, cos = dx / D;

	    x = xm*cos - ym*sin + x1;
	    ym = xm*sin + ym*cos + y1;
	    xm = x;

	    x = xn*cos - yn*sin + x1;
	    yn = xn*sin + yn*cos + y1;
	    xn = x;

	    int[] xpoints = {x2, (int) xm, (int) xn};
	    int[] ypoints = {y2, (int) ym, (int) yn};

	    g.drawLine(x1, y1, x2, y2);
	    g.fillPolygon(xpoints, ypoints, 3);
	}
	
	public void DrawTransition( Graphics g , int x , int y , boolean grayout) {
		if ((sourceConnect==null)|(destConnect == null))
			return;
		/* TODO: draw transition string */
		Color col = g.getColor();
		FontMetrics metric = g.getFontMetrics(StdAttr.DEFAULT_LABEL_FONT);
		Bounds bds = this.getBounds(metric);
		g.setColor(grayout ? Color.GRAY : (active) ? ActiveColor : PassiveColor);
		GraphicsUtil.switchToWidth(g, 2);
		if (sourceState.equals(destState)) {
			g.drawArc(x+bds.getX(), y+bds.getY(), bds.getWidth(), bds.getWidth(), 0, 180);
			int[] xpoints = {x+bds.getX()+bds.getWidth(),x+bds.getX()+bds.getWidth()-ArrowHeight,x+bds.getX()+bds.getWidth()+ArrowHeight};
			int[] ypoints = {y+bds.getCenterY(),y+bds.getCenterY()-ArrowWidth,y+bds.getCenterY()-ArrowWidth};
			g.fillPolygon(xpoints, ypoints, 3);
		} else
			drawArrowLine(g,x+sourceConnect.getX(),y+sourceConnect.getY(),x+destConnect.getX(),y+destConnect.getY(),
					ArrowWidth,ArrowHeight);
		GraphicsUtil.switchToWidth(g, 1);
		g.setColor(col);
	}
}
