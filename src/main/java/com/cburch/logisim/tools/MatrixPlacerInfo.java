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

package com.cburch.logisim.tools;

import com.cburch.logisim.data.Bounds;

public class MatrixPlacerInfo {

	private String OldLabel;
	private String SharedLabel;
	private int NrOfXCopies = 1;
	private int NrOfYCopies = 1;
	private int XDisplacement = 1;
	private int YDisplacement = 1;
	private int XDmin = 1;
	private int YDmin = 1;
	
	public MatrixPlacerInfo( String Label ) {
		SharedLabel = Label;
		OldLabel = Label;
	}
	
	void SetBounds(Bounds bds) {
		XDisplacement = XDmin = (bds.getWidth()+9)/10;
		YDisplacement = YDmin = (bds.getHeight()+9)/10;
	}
	
	int getMinimalXDisplacement() {
		return XDmin;
	}
	
	int getMinimalYDisplacement() {
		return YDmin;
	}
	
	String GetLabel() {
		return SharedLabel;
	}
	
	void UndoLabel() {
		SharedLabel = OldLabel;
	}
	
	void SetLabel( String Lab ) {
		SharedLabel = Lab;
	}
	
	int getNrOfXCopies() {
		return NrOfXCopies;
	}
	
	void setNrOfXCopies( int val ) {
		NrOfXCopies = val;
	}
	
	int getNrOfYCopies() {
		return NrOfYCopies;
	}
	
	void setNrOfYCopies( int val ) {
		NrOfYCopies = val;
	}
	
	int GetDeltaX() {
		return XDisplacement*10;
	}
	
	void SetDeltaX( int value ) {
		if (value > 0)
			XDisplacement = (value+9) / 10;
	}
	
	void setXDisplacement( int value ) {
		if (value > 0)
			XDisplacement = value;
	}

	int getXDisplacement() {
		return XDisplacement;
	}

	int GetDeltaY() {
		return YDisplacement*10;
	}
	
	void SetDeltaY( int value ) {
		if (value > 0)
			YDisplacement = (value+9) / 10;
	}
	
	void setYisplacement( int value ) {
		if (value > 0)
			YDisplacement = value;
	}

	int getYDisplacement() {
		return YDisplacement;
	}

}
