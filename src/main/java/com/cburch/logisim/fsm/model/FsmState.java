/* This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.fsm.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class FsmState {
	private ArrayList<FsmStateListener> listeners = new ArrayList<FsmStateListener>();
	private String StateName = "";
	private long StateCoding = -1;
	private Location place = null;
	private boolean IsCurrentState = false;
	
	public FsmState(String Name) {
		StateName = Name;
	}
	
	public FsmState(String Name, Location loc) {
		place = loc;
		StateName = Name;
	}
	
	public FsmState clone() {
		FsmState Clone = new FsmState(StateName,place);
		Clone.SetCode(StateCoding);
		if (IsCurrentState)
			Clone.SetAsCurrentState();
		return Clone;
	}
	
	//
	// listener methods
	//
	
	public void addFsmStateListener(FsmStateListener l) {
		listeners.add(l);
	}
	
	public void removeFsmStateListener(FsmStateListener l) {
		if (listeners.contains(l))
			listeners.remove(l);
	}
	
	public void fireEvent(int event) {
		fireEvent (event,null);
	}
	
	public void fireEvent(int event, Object data) {
		if (listeners.size() == 0)
			return;
		FsmStateEvent newevent = new FsmStateEvent(this,event,data);
		for (FsmStateListener cur : listeners)
			cur.StateChanged(newevent);
	}
	
	//
	// Other methods
	//
	
	public boolean IsCurrentState() {
		return IsCurrentState;
	}
	
	public void SetAsCurrentState() {
		IsCurrentState = true;
		fireEvent(FsmStateEvent.StateIsCurrentState);
	}
	
	public void RemoveAsCurrentState() {
		IsCurrentState = false;
		fireEvent(FsmStateEvent.StateIsNoLongerCurrentState);
	}
	
	public void SetCode(long value) {
		if (value >= 0)
			StateCoding = value;
	}
	
	public void ClearCode() {
		StateCoding = -1;
	}
	
	public String GetName() {
		return StateName;
	}
	
	public Location GetLocation() {
		return place;
	}
	
	public long GetCode() {
		return StateCoding;
	}
	
	public Bounds getSize( FontMetrics metrics ) {
		int xleft,ytop;
		int width = (metrics == null) ? StateName.length()*10 : metrics.stringWidth(StateName);
		if (width < 40)
			width=40;
		width += 20;
		width /= 20;
		width *= 20;
		xleft = (place==null) ? 0 : place.getX()-width/2;
		int height = (metrics == null) ? 20 : (metrics.getHeight() < 20) ? 20 : metrics.getHeight();
		height += 20;
		height /= 20;
		height *= 20;
		ytop  = (place==null) ? 0 : place.getY()-height/2;
		return Bounds.create(xleft, ytop, width, height);
	}
	
	public void DrawState( Graphics g , int x , int y , boolean grayout) {
		Font font = g.getFont();
		Color col = g.getColor();
		g.setFont(StdAttr.DEFAULT_LABEL_FONT);
		FontMetrics metric = g.getFontMetrics();
		Bounds bds = getSize(metric);
		Color NameCol = (IsCurrentState) ? Color.WHITE : (grayout) ? Color.GRAY : Color.BLACK;
		Color BoxCol = (grayout) ? Color.GRAY : Color.black;
		int xpos = (place==null) ? 0 : place.getX()+x;
		int ypos = (place==null) ? 0 : place.getY()+y;
		g.setColor(BoxCol);
		GraphicsUtil.switchToWidth(g, 2);
		if (IsCurrentState)
			g.fillRoundRect(xpos-bds.getWidth()/2, ypos-bds.getHeight()/2, bds.getWidth(), bds.getHeight(), 20, 20);
		else
			g.drawRoundRect(xpos-bds.getWidth()/2, ypos-bds.getHeight()/2, bds.getWidth(), bds.getHeight(), 20, 20);
		GraphicsUtil.switchToWidth(g, 1);
		g.setColor(NameCol);
		GraphicsUtil.drawText(g, StateName, xpos, ypos, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
		g.setColor(col);
		g.setFont(font);
	}

}
