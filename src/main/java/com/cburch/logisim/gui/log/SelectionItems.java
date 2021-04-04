/*
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

package com.cburch.logisim.gui.log;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;


public class SelectionItems extends ArrayList<SelectionItem> implements Transferable
{
  private static final long serialVersionUID = 1L;
  public static final DataFlavor dataFlavor;
  static {
    DataFlavor f = null;
    try {
        f = new DataFlavor(
            String.format("%s;class=\"%s\"",
              DataFlavor.javaJVMLocalObjectMimeType,
              SelectionItems.class.getName()));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    dataFlavor = f;
  }
  public static final DataFlavor[] dataFlavors = new DataFlavor[] { dataFlavor };

  @Override
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
    if(!isDataFlavorSupported(flavor))
      throw new UnsupportedFlavorException(flavor);
    return this;
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    return dataFlavors;
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return dataFlavor.equals(flavor);
  }
}
