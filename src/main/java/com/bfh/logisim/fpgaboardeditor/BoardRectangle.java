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

package com.bfh.logisim.fpgaboardeditor;

public class BoardRectangle {
	private int xPosition;
	private int yPosition;
	private int Width;
	private int Height;
	private boolean IsActiveHigh = true;

	public BoardRectangle(int x, int y, int w, int h) {
		this.set(x, y, w, h);
	}

	@Override
	public boolean equals(Object rect) {
		if (!(rect instanceof BoardRectangle))
			return false;
		BoardRectangle Rect = (BoardRectangle) rect;
		return ((Rect.getHeight() == Height) && (Rect.getWidth() == Width)
				&& (Rect.getXpos() == xPosition) && (Rect.getYpos() == yPosition));
	}

	public int getHeight() {
		return Height;
	}

	public int getWidth() {
		return Width;
	}

	public int getXpos() {
		return xPosition;
	}

	public int getYpos() {
		return yPosition;
	}

	public boolean IsActiveOnHigh() {
		return IsActiveHigh;
	}

	public Boolean Overlap(BoardRectangle rect) {
		Boolean result;
		int xl, xr, yt, yb;
		xl = rect.getXpos();
		xr = xl + rect.getWidth();
		yt = rect.getYpos();
		yb = yt + rect.getHeight();

		/* first check for the other corner points inside myself */
		result = this.PointInside(xl, yt);
		result |= this.PointInside(xl, yb);
		result |= this.PointInside(xr, yt);
		result |= this.PointInside(xr, yb);

		/* check for my corner points inside him */
		result |= rect.PointInside(xPosition, yPosition);
		result |= rect.PointInside(xPosition + Width, yPosition);
		result |= rect.PointInside(xPosition, yPosition + Height);
		result |= rect.PointInside(xPosition + Width, yPosition + Height);

		/*
		 * if result=false: for sure the corner points are not inside one of
		 * each other
		 */
		/* we now have to check for partial overlap */
		if (!result) {
			result |= ((xl >= xPosition) && (xl <= (xPosition + Width))
					&& (yt <= yPosition) && (yb >= (yPosition + Height)));
			result |= ((xr >= xPosition) && (xr <= (xPosition + Width))
					&& (yt <= yPosition) && (yb >= (yPosition + Height)));
			result |= ((xl <= xPosition) && (xr >= (xPosition + Width))
					&& (yt >= yPosition) && (yt <= (yPosition + Height)));
			result |= ((xl <= xPosition) && (xr >= (xPosition + Width))
					&& (yb >= yPosition) && (yb <= (yPosition + Height)));
		}
		if (!result) {
			result |= ((xPosition >= xl) && (xPosition <= xr)
					&& (yPosition <= yt) && ((yPosition + Height) >= yb));
			result |= (((xPosition + Width) >= xl)
					&& ((xPosition + Width) <= xr) && (yPosition <= yt) && ((yPosition + Height) >= yb));
			result |= ((xPosition <= xl) && ((xPosition + Width) >= xr)
					&& (yPosition >= yt) && (yPosition <= yb));
			result |= ((xPosition <= xl) && ((xPosition + Width) >= xr)
					&& ((yPosition + Height) >= yt) && ((yPosition + Height) <= yb));
		}

		return result;
	}

	public Boolean PointInside(int x, int y) {
		return ((x >= xPosition) && (x <= (xPosition + Width))
				&& (y >= yPosition) && (y <= (yPosition + Height)));
	}

	private void set(int x, int y, int w, int h) {
		if (w < 0) {
			xPosition = x + w;
			Width = -w;
		} else {
			xPosition = x;
			Width = w;
		}
		if (h < 0) {
			yPosition = y + h;
			Height = -h;
		} else {
			yPosition = y;
			Height = h;
		}
	}

	public void SetActiveOnHigh(boolean IsActiveHigh) {
		this.IsActiveHigh = IsActiveHigh;
	}

}