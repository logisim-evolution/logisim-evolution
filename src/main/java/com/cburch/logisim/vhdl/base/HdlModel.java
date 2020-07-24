/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.vhdl.base;

public interface HdlModel {

  /** Registers a listener for changes to the values. */
  public void addHdlModelListener(HdlModelListener l);

  /** Compares the model's content with another model. */
  public boolean compare(HdlModel model);

  /** Compares the model's content with a string. */
  public boolean compare(String value);

  /** Gets the content of the HDL-IP component. */
  public String getContent();

  /** Get the component's name */
  public String getName();

  /** Unregisters a listener for changes to the values. */
  public void removeHdlModelListener(HdlModelListener l);

  /** Sets the content of the component. */
  public boolean setContent(String content);

  /** Checks whether the content of the component is valid. */
  public boolean isValid();

  /** Displays errors, if any. */
  public void showErrors();

  /** Fire notification that the display has changed. */
  public void displayChanged();
}
