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

import com.cburch.draw.model.Handle;
import com.cburch.logisim.data.Location;

public class PolyUtil {
	public static class ClosestResult {
		private double dist;
		private Location loc;
		private Handle prevHandle;
		private Handle nextHandle;

		public double getDistanceSq() {
			return dist;
		}

		public Location getLocation() {
			return loc;
		}

		public Handle getNextHandle() {
			return nextHandle;
		}

		public Handle getPreviousHandle() {
			return prevHandle;
		}
	}

	public static ClosestResult getClosestPoint(Location loc, boolean closed,
			Handle[] hs) {
		int xq = loc.getX();
		int yq = loc.getY();
		ClosestResult ret = new ClosestResult();
		ret.dist = Double.MAX_VALUE;
		if (hs.length > 0) {
			Handle h0 = hs[0];
			int x0 = h0.getX();
			int y0 = h0.getY();
			int stop = closed ? hs.length : (hs.length - 1);
			for (int i = 0; i < stop; i++) {
				Handle h1 = hs[(i + 1) % hs.length];
				int x1 = h1.getX();
				int y1 = h1.getY();
				double d = LineUtil.ptDistSqSegment(x0, y0, x1, y1, xq, yq);
				if (d < ret.dist) {
					ret.dist = d;
					ret.prevHandle = h0;
					ret.nextHandle = h1;
				}
				h0 = h1;
				x0 = x1;
				y0 = y1;
			}
		}
		if (ret.dist == Double.MAX_VALUE) {
			return null;
		} else {
			Handle h0 = ret.prevHandle;
			Handle h1 = ret.nextHandle;
			double[] p = LineUtil.nearestPointSegment(xq, yq, h0.getX(),
					h0.getY(), h1.getX(), h1.getY());
			ret.loc = Location.create((int) Math.round(p[0]),
					(int) Math.round(p[1]));
			return ret;
		}
	}

	private PolyUtil() {
	}
}
