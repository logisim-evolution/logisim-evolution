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

package com.cburch.draw.shapes;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;

public class SvgReader {
	private static AbstractCanvasObject createLine(Element elt) {
		int x0 = Integer.parseInt(elt.getAttribute("x1"));
		int y0 = Integer.parseInt(elt.getAttribute("y1"));
		int x1 = Integer.parseInt(elt.getAttribute("x2"));
		int y1 = Integer.parseInt(elt.getAttribute("y2"));
		return new Line(x0, y0, x1, y1);
	}

	private static AbstractCanvasObject createOval(Element elt) {
		double cx = Double.parseDouble(elt.getAttribute("cx"));
		double cy = Double.parseDouble(elt.getAttribute("cy"));
		double rx = Double.parseDouble(elt.getAttribute("rx"));
		double ry = Double.parseDouble(elt.getAttribute("ry"));
		int x = (int) Math.round(cx - rx);
		int y = (int) Math.round(cy - ry);
		int w = (int) Math.round(rx * 2);
		int h = (int) Math.round(ry * 2);
		return new Oval(x, y, w, h);
	}

	private static AbstractCanvasObject createPath(Element elt) {
		Matcher patt = PATH_REGEX.matcher(elt.getAttribute("d"));
		List<String> tokens = new ArrayList<String>();
		int type = -1; // -1 error, 0 start, 1 curve, 2 polyline
		while (patt.find()) {
			String token = patt.group();
			tokens.add(token);
			if (Character.isLetter(token.charAt(0))) {
				switch (token.charAt(0)) {
				case 'M':
					if (type == -1)
						type = 0;
					else
						type = -1;
					break;
				case 'Q':
				case 'q':
					if (type == 0)
						type = 1;
					else
						type = -1;
					break;
				/*
				 * not supported case 'L': case 'l': case 'H': case 'h': case
				 * 'V': case 'v': if (type == 0 || type == 2) type = 2; else
				 * type = -1; break;
				 */
				default:
					type = -1;
				}
				if (type == -1) {
					throw new NumberFormatException(
							"Unrecognized path command '" + token.charAt(0)
									+ "'");
				}
			}
		}

		if (type == 1) {
			if (tokens.size() == 8 && tokens.get(0).equals("M")
					&& tokens.get(3).toUpperCase().equals("Q")) {
				int x0 = Integer.parseInt(tokens.get(1));
				int y0 = Integer.parseInt(tokens.get(2));
				int x1 = Integer.parseInt(tokens.get(4));
				int y1 = Integer.parseInt(tokens.get(5));
				int x2 = Integer.parseInt(tokens.get(6));
				int y2 = Integer.parseInt(tokens.get(7));
				if (tokens.get(3).equals("q")) {
					x1 += x0;
					y1 += y0;
					x2 += x0;
					y2 += y0;
				}
				Location e0 = Location.create(x0, y0);
				Location e1 = Location.create(x2, y2);
				Location ct = Location.create(x1, y1);
				return new Curve(e0, e1, ct);
			} else {
				throw new NumberFormatException("Unexpected format for curve");
			}
		} else {
			throw new NumberFormatException("Unrecognized path");
		}
	}

	private static AbstractCanvasObject createPolygon(Element elt) {
		return new Poly(true, parsePoints(elt.getAttribute("points")));
	}

	private static AbstractCanvasObject createPolyline(Element elt) {
		return new Poly(false, parsePoints(elt.getAttribute("points")));
	}

	private static AbstractCanvasObject createRectangle(Element elt) {
		int x = Integer.parseInt(elt.getAttribute("x"));
		int y = Integer.parseInt(elt.getAttribute("y"));
		int w = Integer.parseInt(elt.getAttribute("width"));
		int h = Integer.parseInt(elt.getAttribute("height"));
		if (elt.hasAttribute("rx")) {
			AbstractCanvasObject ret = new RoundRectangle(x, y, w, h);
			int rx = Integer.parseInt(elt.getAttribute("rx"));
			ret.setValue(DrawAttr.CORNER_RADIUS, Integer.valueOf(rx));
			return ret;
		} else {
			return new Rectangle(x, y, w, h);
		}
	}

	public static AbstractCanvasObject createShape(Element elt) {
		String name = elt.getTagName();
		AbstractCanvasObject ret;
		if (name.equals("ellipse")) {
			ret = createOval(elt);
		} else if (name.equals("line")) {
			ret = createLine(elt);
		} else if (name.equals("path")) {
			ret = createPath(elt);
		} else if (name.equals("polyline")) {
			ret = createPolyline(elt);
		} else if (name.equals("polygon")) {
			ret = createPolygon(elt);
		} else if (name.equals("rect")) {
			ret = createRectangle(elt);
		} else if (name.equals("text")) {
			ret = createText(elt);
		} else {
			return null;
		}
		List<Attribute<?>> attrs = ret.getAttributes();
		if (attrs.contains(DrawAttr.PAINT_TYPE)) {
			String stroke = elt.getAttribute("stroke");
			String fill = elt.getAttribute("fill");
			if (stroke.equals("") || stroke.equals("none")) {
				ret.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
			} else if (fill.equals("none")) {
				ret.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_STROKE);
			} else {
				ret.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_STROKE_FILL);
			}
		}
		attrs = ret.getAttributes(); // since changing paintType could change it
		if (attrs.contains(DrawAttr.STROKE_WIDTH)
				&& elt.hasAttribute("stroke-width")) {
			Integer width = Integer.valueOf(elt.getAttribute("stroke-width"));
			ret.setValue(DrawAttr.STROKE_WIDTH, width);
		}
		if (attrs.contains(DrawAttr.STROKE_COLOR)) {
			String color = elt.getAttribute("stroke");
			String opacity = elt.getAttribute("stroke-opacity");
			if (!color.equals("none")) {
				ret.setValue(DrawAttr.STROKE_COLOR, getColor(color, opacity));
			}
		}
		if (attrs.contains(DrawAttr.FILL_COLOR)) {
			String color = elt.getAttribute("fill");
			if (color.equals(""))
				color = "#000000";
			String opacity = elt.getAttribute("fill-opacity");
			if (!color.equals("none")) {
				ret.setValue(DrawAttr.FILL_COLOR, getColor(color, opacity));
			}
		}
		return ret;
	}

	private static AbstractCanvasObject createText(Element elt) {
		int x = Integer.parseInt(elt.getAttribute("x"));
		int y = Integer.parseInt(elt.getAttribute("y"));
		String text = elt.getTextContent();
		Text ret = new Text(x, y, text);

		String fontFamily = elt.getAttribute("font-family");
		String fontStyle = elt.getAttribute("font-style");
		String fontWeight = elt.getAttribute("font-weight");
		String fontSize = elt.getAttribute("font-size");
		int styleFlags = 0;
		if (fontStyle.equals("italic"))
			styleFlags |= Font.ITALIC;
		if (fontWeight.equals("bold"))
			styleFlags |= Font.BOLD;
		int size = Integer.parseInt(fontSize);
		ret.setValue(DrawAttr.FONT, new Font(fontFamily, styleFlags, size));

		String alignStr = elt.getAttribute("text-anchor");
		AttributeOption halign;
		if (alignStr.equals("start")) {
			halign = DrawAttr.ALIGN_LEFT;
		} else if (alignStr.equals("end")) {
			halign = DrawAttr.ALIGN_RIGHT;
		} else {
			halign = DrawAttr.ALIGN_CENTER;
		}
		ret.setValue(DrawAttr.ALIGNMENT, halign);

		// fill color is handled after we return
		return ret;
	}

	private static Color getColor(String hue, String opacity) {
		int r;
		int g;
		int b;
		if (hue == null || hue.equals("")) {
			r = 0;
			g = 0;
			b = 0;
		} else {
			r = Integer.parseInt(hue.substring(1, 3), 16);
			g = Integer.parseInt(hue.substring(3, 5), 16);
			b = Integer.parseInt(hue.substring(5, 7), 16);
		}
		int a;
		if (opacity == null || opacity.equals("")) {
			a = 255;
		} else {
			/*
			 * Patch taken from Cornell's version of Logisim:
			 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
			 */
			double x;
			try {
				x = Double.parseDouble(opacity);
			} catch (NumberFormatException e) {
				// some localizations use commas for decimal points
				int comma = opacity.lastIndexOf(',');
				if (comma >= 0) {
					try {
						String repl = opacity.substring(0, comma) + "."
								+ opacity.substring(comma + 1);
						x = Double.parseDouble(repl);
					} catch (Throwable t) {
						throw e;
					}
				} else {
					throw e;
				}
			}
			a = (int) Math.round(x * 255);
		}
		return new Color(r, g, b, a);
	}

	private static List<Location> parsePoints(String points) {
		Pattern patt = Pattern.compile("[ ,\n\r\t]+");
		String[] toks = patt.split(points);
		Location[] ret = new Location[toks.length / 2];
		for (int i = 0; i < ret.length; i++) {
			int x = Integer.parseInt(toks[2 * i]);
			int y = Integer.parseInt(toks[2 * i + 1]);
			ret[i] = Location.create(x, y);
		}
		return UnmodifiableList.create(ret);
	}

	private static final Pattern PATH_REGEX = Pattern
			.compile("[a-zA-Z]|[-0-9.]+");

	private SvgReader() {
	}
}
