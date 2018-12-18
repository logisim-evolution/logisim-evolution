/*******************************************************************************
 * This file is part of logisim-evolution.
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

package com.cburch.logisim.circuit.appear;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cburch.draw.shapes.Curve;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.draw.shapes.Text;
import com.cburch.draw.util.EditableLabel;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;

class DefaultAppearance {
	private static class CompareLocations implements Comparator<Instance> {
		private boolean byX;

		CompareLocations(boolean byX) {
			this.byX = byX;
		}

		public int compare(Instance a, Instance b) {
			Location aloc = a.getLocation();
			Location bloc = b.getLocation();
			if (byX) {
				int ax = aloc.getX();
				int bx = bloc.getX();
				if (ax != bx) {
					return ax < bx ? -1 : 1;
				}
			} else {
				int ay = aloc.getY();
				int by = bloc.getY();
				if (ay != by) {
					return ay < by ? -1 : 1;
				}
			}
			return aloc.compareTo(bloc);
		}
	}

	public static List<CanvasObject> build(Collection<Instance> pins,
			                               boolean NamedBox,
			                               boolean Fixed,
			                               String CircuitName,
			                               Graphics g) {
		if (NamedBox) {
			return new_build(pins,CircuitName,g,Fixed);
		} else {
			return old_build(pins);
		}
	}

    private static List<CanvasObject> old_build(Collection<Instance> pins) {
        Map<Direction,List<Instance>> edge;
        edge = new HashMap<Direction,List<Instance>>();
        edge.put(Direction.NORTH, new ArrayList<Instance>());
        edge.put(Direction.SOUTH, new ArrayList<Instance>());
        edge.put(Direction.EAST, new ArrayList<Instance>());
        edge.put(Direction.WEST, new ArrayList<Instance>());
        for (Instance pin : pins) {
                Direction pinFacing = pin.getAttributeValue(StdAttr.FACING);
                Direction pinEdge = pinFacing.reverse();
                List<Instance> e = edge.get(pinEdge);
                e.add(pin);
        }
        
        for (Map.Entry<Direction, List<Instance>> entry : edge.entrySet()) {
                sortPinList(entry.getValue(), entry.getKey());
        }

        int numNorth = edge.get(Direction.NORTH).size();
        int numSouth = edge.get(Direction.SOUTH).size();
        int numEast = edge.get(Direction.EAST).size();
        int numWest = edge.get(Direction.WEST).size();
        int maxVert = Math.max(numNorth, numSouth);
        int maxHorz = Math.max(numEast, numWest);

        int offsNorth = computeOffset(numNorth, numSouth, maxHorz);
        int offsSouth = computeOffset(numSouth, numNorth, maxHorz);
        int offsEast = computeOffset(numEast, numWest, maxVert);
        int offsWest = computeOffset(numWest, numEast, maxVert);
        
        int width = computeDimension(maxVert, maxHorz);
        int height = computeDimension(maxHorz, maxVert);

        // compute position of anchor relative to top left corner of box
        int ax;
        int ay;
        if (numEast > 0) { // anchor is on east side
                ax = width;
                ay = offsEast;
        } else if (numNorth > 0) { // anchor is on north side
                ax = offsNorth;
                ay = 0;
        } else if (numWest > 0) { // anchor is on west side
                ax = 0;
                ay = offsWest;
        } else if (numSouth > 0) { // anchor is on south side
                ax = offsSouth;
                ay = height;
        } else { // anchor is top left corner
                ax = 0;
                ay = 0;
        }
        
        // place rectangle so anchor is on the grid
        int rx = OFFS + (9 - (ax + 9) % 10);
        int ry = OFFS + (9 - (ay + 9) % 10);
        
        Location e0 = Location.create(rx + (width - 8) / 2, ry + 1);
        Location e1 = Location.create(rx + (width + 8) / 2, ry + 1);
        Location ct = Location.create(rx + width / 2, ry + 11);
        Curve notch = new Curve(e0, e1, ct);
        notch.setValue(DrawAttr.STROKE_WIDTH, Integer.valueOf(2));
        notch.setValue(DrawAttr.STROKE_COLOR, Color.GRAY);
        Rectangle rect = new Rectangle(rx, ry, width, height);
        rect.setValue(DrawAttr.STROKE_WIDTH, Integer.valueOf(2));

        List<CanvasObject> ret = new ArrayList<CanvasObject>();
        ret.add(notch);
        ret.add(rect);
        placePins(ret, edge.get(Direction.WEST),
                        rx,             ry + offsWest,  0, 10);
        placePins(ret, edge.get(Direction.EAST),
                rx + width,     ry + offsEast,  0, 10);
        placePins(ret, edge.get(Direction.NORTH),
                rx + offsNorth, ry,            10,  0);
        placePins(ret, edge.get(Direction.SOUTH),
                rx + offsSouth, ry + height,   10,  0);
        ret.add(new AppearanceAnchor(Location.create(rx + ax, ry + ay)));
        return ret;
    }

    private static List<CanvasObject> new_build(Collection<Instance> pins, String CircuitName, Graphics g, boolean FixedSize) {
		Map<Direction, List<Instance>> edge;
		edge = new HashMap<Direction, List<Instance>>();
		edge.put(Direction.EAST, new ArrayList<Instance>());
		edge.put(Direction.WEST, new ArrayList<Instance>());
		int MaxLeftLabelLength = 0;
		int MaxRightLabelLength = 0;
		int TitleWidth = CircuitName.length()*DrawAttr.FixedFontCharWidth;

		if (!pins.isEmpty()) {
			for (Instance pin : pins) {
				Direction pinEdge;
				Text label = new Text(0,0,pin.getAttributeValue(StdAttr.LABEL));
				int LabelWidth = label.getText().length()*DrawAttr.FixedFontCharWidth;
				if (pin.getAttributeValue(Pin.ATTR_TYPE)) {
					pinEdge=Direction.EAST;
					if (LabelWidth>MaxRightLabelLength)
						MaxRightLabelLength = LabelWidth;
				}
				else {
					pinEdge=Direction.WEST;
					if (LabelWidth>MaxLeftLabelLength)
						MaxLeftLabelLength = LabelWidth;
				}
				List<Instance> e = edge.get(pinEdge);
				e.add(pin);
			}
		}

		
		for (Map.Entry<Direction, List<Instance>> entry : edge.entrySet()) {
			sortPinList(entry.getValue(), entry.getKey());
		}

		int numEast = edge.get(Direction.EAST).size();
		int numWest = edge.get(Direction.WEST).size();
		int maxVert = Math.max(numEast, numWest);

		int dy = ((DrawAttr.FixedFontHeight+(DrawAttr.FixedFontHeight>>2)+5)/10)*10;
		int textWidth = (FixedSize) ? 25*DrawAttr.FixedFontCharWidth : 
			(MaxLeftLabelLength+MaxRightLabelLength+35) < (TitleWidth+15) ? TitleWidth+15 :
			(MaxLeftLabelLength+MaxRightLabelLength+35);
		int Thight = ((DrawAttr.FixedFontHeight+10)/10)*10;
		int width = (textWidth/10)*10+20;
		int height = (maxVert > 0) ? maxVert*dy+Thight : 10+Thight;
		int sdy = (DrawAttr.FixedFontAscent-DrawAttr.FixedFontDescent)>>1;

		// compute position of anchor relative to top left corner of box
		int ax;
		int ay;
		if (numEast > 0) { // anchor is on east side
			ax = width;
			ay = 10;
		} else if (numWest > 0) { // anchor is on west side
			ax = 0;
			ay = 10;
		} else { // anchor is top left corner
			ax = 0;
			ay = 0;
		}

		// place rectangle so anchor is on the grid
		int rx = OFFS + (9 - (ax + 9) % 10);
		int ry = OFFS + (9 - (ay + 9) % 10);

		List<CanvasObject> ret = new ArrayList<CanvasObject>();
		placePins(ret, edge.get(Direction.WEST), rx, ry + 10, 0, dy,true,
				sdy,FixedSize);
		placePins(ret, edge.get(Direction.EAST), rx + width, ry + 10, 0,
				dy,false,sdy,FixedSize);
		Rectangle rect = new Rectangle(rx+10,ry+height-Thight,width-20,Thight);
		rect.setValue(DrawAttr.STROKE_WIDTH, Integer.valueOf(1));
		rect.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
		rect.setValue(DrawAttr.FILL_COLOR, Color.black);
		ret.add(rect);
		rect = new Rectangle(rx+10, ry, width-20, height);
		rect.setValue(DrawAttr.STROKE_WIDTH, Integer.valueOf(2));
		ret.add(rect);
		String Label = CircuitName;
		if (FixedSize) {
			if (Label.length()>23) {
				Label = Label.substring(0, 20);
				Label = Label.concat("...");
			}
		}
		Text label = new Text(rx+(width>>1),ry+(height-DrawAttr.FixedFontDescent-5),Label);
		label.getLabel().setHorizontalAlignment(EditableLabel.CENTER);
		label.getLabel().setColor(Color.white);
		label.getLabel().setFont(DrawAttr.DEFAULT_NAME_FONT);
		ret.add(label);
		ret.add(new AppearanceAnchor(Location.create(rx + ax, ry + ay)));
		return ret;
    }

    private static int computeDimension(int maxThis, int maxOthers) {
        if (maxThis < 3) {
                return 30;
        } else if (maxOthers == 0) {
                return 10 * maxThis;
        } else {
                return 10 * maxThis + 10;
        }
    }

    private static int computeOffset(int numFacing, int numOpposite, int maxOthers) {
        int maxThis = Math.max(numFacing, numOpposite);
        int maxOffs;
        switch (maxThis) {
        case 0:
        case 1:
                maxOffs = (maxOthers == 0 ? 15 : 10);
                break;
        case 2:
                maxOffs = 10;
                break;
        default:
                maxOffs = (maxOthers == 0 ? 5 : 10);
        }
        return maxOffs + 10 * ((maxThis - numFacing) / 2);
    }

    private static void placePins(List<CanvasObject> dest, List<Instance> pins,
                int x, int y, int dx, int dy) {
        for (Instance pin : pins) {
                dest.add(new AppearancePort(Location.create(x, y), pin));
                x += dx;
                y += dy;
        }
    }
    
    private static void placePins(List<CanvasObject> dest, List<Instance> pins,
			int x, int y, int dx, int dy, boolean LeftSide, int ldy, boolean FixedSize) {
		int halign;
		Color color = Color.DARK_GRAY;
		int ldx;
		for (Instance pin : pins) {
			int offset = (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1) ? Wire.WIDTH_BUS>>1:Wire.WIDTH>>1;
			int height = (pin.getAttributeValue(StdAttr.WIDTH).getWidth() > 1) ? Wire.WIDTH_BUS:Wire.WIDTH;
			Rectangle rect;
			if (LeftSide) {
				ldx=15;
				halign=EditableLabel.LEFT;
				rect = new Rectangle(x,y-offset,10,height);
			} else {
				ldx=-15;
				halign=EditableLabel.RIGHT;
				rect = new Rectangle(x-10,y-offset,10,height);
			}
			rect.setValue(DrawAttr.STROKE_WIDTH, Integer.valueOf(1));
			rect.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
			rect.setValue(DrawAttr.FILL_COLOR, Color.BLACK);
			dest.add(rect);
			dest.add(new AppearancePort(Location.create(x, y), pin));
			if (pin.getAttributeSet().containsAttribute(StdAttr.LABEL)) {
				String Label = pin.getAttributeValue(StdAttr.LABEL);
				if (FixedSize) {
					if (Label.length()>12) {
						Label = Label.substring(0, 9);
						Label = Label.concat("..");
					}
				}
				Text label = new Text(x+ldx,y+ldy,Label);
				label.getLabel().setHorizontalAlignment(halign);
				label.getLabel().setColor(color);
				label.getLabel().setFont(DrawAttr.DEFAULT_FIXED_PICH_FONT);
				dest.add(label);
			}
			x += dx;
			y += dy;
		}
	}

	static void sortPinList(List<Instance> pins, Direction facing) {
		if (facing == Direction.NORTH || facing == Direction.SOUTH) {
			Comparator<Instance> sortHorizontal = new CompareLocations(true);
			Collections.sort(pins, sortHorizontal);
		} else {
			Comparator<Instance> sortVertical = new CompareLocations(false);
			Collections.sort(pins, sortVertical);
		}
	}

	private static final int OFFS = 50;

	private DefaultAppearance() {
	}
}
