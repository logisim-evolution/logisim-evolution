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

import java.awt.Font;
import java.awt.Graphics;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class FsmAbstractFactory  extends InstanceFactory {
	
	public final static int PinDistance = 20;
	
	protected FsmAbstractFactory(String name) {
		super(name,Strings.getter("FsmComponent"));
		this.setIconName("fsm.gif");
	}
	
	private void computePorts(Instance instance) {
		AttributeSet attrs = instance.getAttributeSet();
		FsmDataStructure info = attrs.getValue(FsmAttributes.FSMCONTENT_ATTR);
		if (info != null) {
			int NrOfInputs = info.NrOfInputs();
			int NrOfOutputs = info.NrOfOutputs();
			Bounds bds = instance.getBounds();
			Port[] ports = new Port[NrOfInputs+NrOfOutputs];
			for (int i = 0 ; i < NrOfInputs ; i++) {
				ports[i] = new Port(0,10+i*PinDistance,Port.INPUT,1);
				ports[i].setToolTip(info.getInputName(i));
			}
			for (int i = 0 ; i < NrOfOutputs ; i++) {
				ports[NrOfInputs+i] = new Port(bds.getWidth(),10+i*PinDistance,Port.OUTPUT,info.getOutputBitWidth(i));
				ports[NrOfInputs+i].setToolTip(info.getOutputName(i));
			}
			instance.setPorts(ports);
			instance.recomputeBounds();
		}
		/* add correct label location */
		Bounds bds = instance.getBounds();
		instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, bds.getX()
				+ bds.getWidth() / 2, bds.getY() - 3, GraphicsUtil.H_CENTER,
				GraphicsUtil.V_BASELINE);
	}
	
	@Override
	public Bounds getOffsetBounds(AttributeSet attrs) {
		FsmDataStructure info = attrs.getValue(FsmAttributes.FSMCONTENT_ATTR);
		if (info != null) {
			int nin = info.NrOfInputs();
			int nout = info.NrOfOutputs();
			int height = (nin>nout) ? nin*PinDistance : nout*PinDistance;
			if (height < info.GetBounds().getHeight())
				height = info.GetBounds().getHeight();
			int width =  info.GetBounds().getWidth();
			if (width < 60)
				width = 60;
			return Bounds.create(0,0,width,height);
		} else return Bounds.create(0, 0, 160, 60);
	}

	@Override
	public void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
		instance.recomputeBounds();
	}

	@Override
	protected void configureNewInstance(Instance instance) {
		instance.addAttributeListener();
		computePorts(instance);
	}
	
	@Override
	public AttributeSet createAttributeSet() {
		return new FsmAttributes();
	}
	@Override
	public void paintInstance(InstancePainter painter) {
		Graphics g = painter.getGraphics();
		AttributeSet attrs = painter.getAttributeSet();
		FsmDataStructure info = attrs.getValue(FsmAttributes.FSMCONTENT_ATTR);
		if ((info != null)&&(!info.HasFontMetrics()))
			info.SetFontMetrics(g.getFontMetrics(StdAttr.DEFAULT_LABEL_FONT));			
		Bounds bds = painter.getBounds();
		GraphicsUtil.switchToWidth(g, 2);
		g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 10, 10);
		GraphicsUtil.switchToWidth(g, 1);
		painter.drawPorts();
		painter.drawLabel();
		if (info != null) {
			int NrOfInputs = info.NrOfInputs();
			int yoff = 10+(NrOfInputs-1)*PinDistance;
			Font font = g.getFont();
			g.setFont(StdAttr.DEFAULT_LABEL_FONT);
			for (int i = 0 ; i < NrOfInputs-1 ; i++)
				GraphicsUtil.drawText(g, info.getInputName(i).toString(), bds.getX()+5, 
						bds.getY()+10+i*PinDistance, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
			for (int i = 0 ; i < info.NrOfOutputs() ; i++) 
				GraphicsUtil.drawText(g, info.getOutputName(i).toString(), bds.getX()+bds.getWidth()-5, 
						bds.getY()+10+i*PinDistance, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
			g.setFont(font);
			/* Draw Clock symbol */
			g.drawLine(bds.getX(), bds.getY()+yoff-5, bds.getX()+10, bds.getY()+yoff);
			g.drawLine(bds.getX(), bds.getY()+yoff+5, bds.getX()+10, bds.getY()+yoff);
			if (!info.HasStates()) {
				GraphicsUtil.drawCenteredText(g, Strings.get("FsmEmpty"), bds.getCenterX(),bds.getCenterY());
			} else {
				info.DrawStateDiagram(g, bds.getX()+5, bds.getY()+5);
			}
		}
	}

	@Override
	public void propagate(InstanceState state) {
		/* TODO Add outputs */
		FsmDataStructure info = state.getAttributeValue(FsmAttributes.FSMCONTENT_ATTR);
		for (int i = 0 ; i < info.NrOfInputs() ; i++)
			info.setInputValue(i, state.getPortValue(i));
		info.propagate();
	}

	@Override
	public boolean RequiresNonZeroLabel() {
		return true;
	}

}
