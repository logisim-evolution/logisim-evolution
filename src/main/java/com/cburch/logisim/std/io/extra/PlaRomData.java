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

package com.cburch.logisim.std.io.extra;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.util.LocaleManager;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Arrays;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class PlaRomData implements InstanceData {
  private byte inputs, outputs, and;
  private String SavedData = "";
  private boolean[][] InputAnd;
  private boolean[][] AndOutput;
  public int rowhovered = -1, columnhovered = 0;
  private Value[] InputValue;
  private Value[] AndValue;
  private Value[] OutputValue;
  private String[] options =
      new String[] {
        new LocaleManager("resources/logisim", "gui").get("saveOption"), S.get("ramClearMenuItem")
      };
  private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
  private JScrollPane panel;
  private PlaRomPanel drawing;

  public PlaRomData(byte inputs, byte outputs, byte and) {
    this.inputs = inputs;
    this.outputs = outputs;
    this.and = and;
    InputAnd = new boolean[getAnd()][getInputs() * 2];
    AndOutput = new boolean[getAnd()][getOutputs()];
    InputValue = new Value[getInputs()];
    AndValue = new Value[getAnd()];
    OutputValue = new Value[getOutputs()];
    InitializeInputValue();
    setAndValue();
    setOutputValue();
  }

  public void ClearMatrixValues() {
    for (byte i = 0; i < getAnd(); i++) {
      for (byte j = 0; j < getOutputs(); j++) {
        setAndOutputValue(i, j, false);
      }
      for (byte k = 0; k < getInputs() * 2; k++) {
        setInputAndValue(i, k, false);
      }
    }
    this.SavedData = "";
  }

  @Override
  public PlaRomData clone() {
    try {
      PlaRomData ret = (PlaRomData) super.clone();
      return ret;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public void decodeSavedData(String s) {
    // if empty, all to false so don't do anything
    if (s == null || s == "") return;
    // split the attribute content string in an array of strings with a single
    // information each one
    String[] datas = s.split(" "), tmp;
    byte value;
    int cnt = 0;
    for (int i = 0; i < datas.length; i++) {
      // if contains a '*' it has to fill the array with the first value for x (second
      // number) cycles
      if (datas[i].contains("*")) {
        tmp = datas[i].split("\\*");
        for (int j = 0; j < Integer.parseInt(tmp[1]); j++) {
          value = (byte) Integer.parseInt(tmp[0]);
          writeData(value, cnt);
          cnt++;
        }
      } else {
        value = (byte) Integer.parseInt(datas[i]);
        writeData(value, cnt);
        cnt++;
      }
    }
  }

  public int editWindow() {
    this.drawing = new PlaRomPanel(this);
    panel =
        new JScrollPane(
            this.drawing,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    panel.setBorder(null);
    panel.getVerticalScrollBar().setUnitIncrement(10);
    if (this.drawing.getPreferredSize().getWidth() >= (int) (screenSize.width * 0.75))
      panel.setPreferredSize(
          new Dimension(
              (int) (screenSize.width * 0.75), (int) panel.getPreferredSize().getHeight()));
    if (this.drawing.getPreferredSize().getHeight() >= (int) (screenSize.height * 0.75))
      panel.setPreferredSize(
          new Dimension(
              (int) panel.getPreferredSize().getWidth(), (int) (screenSize.height * 0.75)));
    int ret =
        OptionPane.showOptionDialog(
            null,
            panel,
            S.fmt("PlaEditWindowTitel",getSizeString()),
            OptionPane.OK_CANCEL_OPTION,
            OptionPane.PLAIN_MESSAGE,
            null,
            this.options,
            null);
    SaveData();
    return ret;
  }

  public byte getAnd() {
    return this.and;
  }

  public boolean getAndOutputValue(int row, int column) {
    return this.AndOutput[row][column];
  }

  public Value getAndValue(byte i) {
    return AndValue[i];
  }

  public boolean getInputAndValue(int row, int column) {
    return this.InputAnd[row][column];
  }

  public byte getInputs() {
    return this.inputs;
  }

  public Value getInputValue(byte i) {
    return this.InputValue[i];
  }

  public byte getOutputs() {
    return this.outputs;
  }

  public Value getOutputValue(byte i) {
    return OutputValue[i];
  }

  public Value[] getOutputValues() {
    Value[] OutputValuecopy = new Value[getOutputs()];
    for (byte i = (byte) (getOutputs() - 1); i >= 0; i--) // reverse array
    OutputValuecopy[i] = OutputValue[OutputValue.length - i - 1];
    return OutputValuecopy;
  }

  public String getSavedData() {
    // return the string to save in the .circ
    return SavedData;
  }

  public String getSizeString() {
    return this.getInputs() + 'x' + this.getAnd() + "x" + this.getOutputs();
  }

  private void InitializeInputValue() {
    for (byte i = 0; i < getInputs(); i++) InputValue[i] = Value.UNKNOWN;
  }

  private void SaveData() {
    // string to write inside the .circ to not lose data
    int row, column, size1 = getInputs() * getAnd(), size2 = getOutputs() * getAnd(), count = 0;
    char val, last = 'x';
    boolean dirty = false;
    String data = "";
    // input-and matrix
    for (int i = 0; i < size1; i++) {
      row = i / getInputs();
      column = i - row * getInputs();
      // 1= not line selected, 2 = input line selected, 0 = nothing selected in that
      // input line
      if (InputAnd[row][column * 2]) {
        val = '1';
        dirty = true;
      } else if (InputAnd[row][column * 2 + 1]) {
        val = '2';
        dirty = true;
      } else val = '0';
      if (val == last) count++;
      else if (last == 'x') {
        last = val;
        count++;
      }
      if (val != last || i == size1 - 1) {
        if (count >= 3) data += last + "*" + count + ' ';
        else for (int j = 0; j < count; j++) data += last + " ";
        if (val != last && i == size1 - 1) data += val + " ";
        count = 1;
        last = val;
      }
    }
    last = 'x';
    count = 0;
    // and-or matrix
    for (int i = 0; i < size2; i++) {
      row = i / getOutputs();
      column = i - row * getOutputs();
      // 0 = nothing selected, 1 = node selected
      if (AndOutput[row][column]) {
        val = '1';
        dirty = true;
      } else val = '0';
      if (val == last) count++;
      else if (last == 'x') {
        last = val;
        count++;
      }
      if (val != last || i == size2 - 1) {
        if (count >= 3) data += last + "*" + count + ' ';
        else for (int j = 0; j < count; j++) data += last + " ";
        if (val != last && i == size2 - 1) data += val + " ";
        count = 1;
        last = val;
      }
    }
    if (!dirty) data = "";
    SavedData = data;
  }

  public void setAndOutputValue(int row, int column, boolean b) {
    this.AndOutput[row][column] = b;
    // update all values
    setAndValue();
    setOutputValue();
  }

  private void setAndValue() {
    boolean thereisadot = false;
    for (byte i = 0; i < getAnd(); i++) {
      AndValue[i] = Value.TRUE;
      for (byte j = 0; j < getInputs() * 2; j++) {
        if (getInputAndValue(i, j)) {
          thereisadot = true;
          if (j % 2 == 0) { // not
            if (!getInputValue((byte) (j / 2)).isFullyDefined()) AndValue[i] = Value.ERROR;
            else if (getInputValue((byte) (j / 2)) == Value.TRUE) {
              AndValue[i] = Value.FALSE;
              break;
            }
          } else if (j % 2 == 1) {
            if (!getInputValue((byte) ((j - 1) / 2)).isFullyDefined()) AndValue[i] = Value.ERROR;
            else if (getInputValue((byte) ((j - 1) / 2)) == Value.FALSE) {
              AndValue[i] = Value.FALSE;
              break;
            }
          }
        }
      }
      if (!thereisadot) AndValue[i] = Value.ERROR;
      thereisadot = false;
    }
  }

  public void setHovered(int row, int column) {
    rowhovered = row;
    columnhovered = column;
  }

  public void setInputAndValue(int row, int column, boolean b) {
    this.InputAnd[row][column] = b;
    // update all values
    setAndValue();
    setOutputValue();
  }

  public void setInputsValue(Value[] inputs) {
    int mininputs = getInputs() < inputs.length ? getInputs() : inputs.length;
    for (byte i = 0; i < mininputs; i++)
      this.InputValue[i + getInputs() - mininputs] = inputs[i + inputs.length - mininputs];
    setAndValue();
    setOutputValue();
  }

  private void setOutputValue() {
    boolean thereisadot = false;
    for (byte i = 0; i < getOutputs(); i++) {
      OutputValue[i] = Value.FALSE;
      for (byte j = 0; j < getAnd(); j++) {
        if (getAndOutputValue(j, i)) {
          OutputValue[i] = OutputValue[i].or(getAndValue(j));
          thereisadot = true;
        }
      }
      if (!thereisadot) OutputValue[i] = Value.ERROR;
      thereisadot = false;
    }
  }

  public boolean updateSize(byte inputs, byte outputs, byte and) {
    if (this.inputs != inputs || this.outputs != outputs || this.and != and) {
      byte mininputs = getInputs() < inputs ? getInputs() : inputs;
      byte minoutputs = getOutputs() < outputs ? getOutputs() : outputs;
      byte minand = getAnd() < and ? getAnd() : and;
      this.inputs = inputs;
      this.outputs = outputs;
      this.and = and;
      boolean oldInputAnd[][] = Arrays.copyOf(InputAnd, InputAnd.length);
      boolean oldAndOutput[][] = Arrays.copyOf(AndOutput, AndOutput.length);
      InputAnd = new boolean[getAnd()][getInputs() * 2];
      AndOutput = new boolean[getAnd()][getOutputs()];
      InputValue = new Value[getInputs()];
      AndValue = new Value[getAnd()];
      OutputValue = new Value[getOutputs()];
      for (byte i = 0; i < minand; i++) {
        for (byte j = 0; j < mininputs * 2; j++) {
          InputAnd[i][j] = oldInputAnd[i][j];
        }
        for (byte k = 0; k < minoutputs; k++) {
          AndOutput[i][k] = oldAndOutput[i][k];
        }
      }
      InitializeInputValue();
      setAndValue();
      setOutputValue();
      // data to save in the .circ
      SaveData();
      return true;
    }
    return false;
  }

  private void writeData(byte value, int node) {
    int row, column;
    // first matrix
    if (node < getInputs() * getAnd()) {
      row = node / getInputs();
      column = node - row * getInputs();
      switch (value) {
        case 0:
          // none selected
          InputAnd[row][column * 2] = false;
          InputAnd[row][column * 2 + 1] = false;
          break;
        case 1:
          // not selected
          InputAnd[row][column * 2] = true;
          InputAnd[row][column * 2 + 1] = false;
          break;
        case 2:
          // normal input selected
          InputAnd[row][column * 2] = false;
          InputAnd[row][column * 2 + 1] = true;
          break;
        default:
          System.err.println("PlaRom: Error in saved data ");
          return;
      }
    } // second matrix
    else if (node < getInputs() * getAnd() + getOutputs() * getAnd()) {
      node -= getInputs() * getAnd();
      row = node / getOutputs();
      column = node - row * getOutputs();
      switch (value) {
        case 0:
          // not selected
          AndOutput[row][column] = false;
          break;
        case 1:
          AndOutput[row][column] = true;
          break;
        default:
          System.err.println("PlaRom: Error in saved data 2");
          return;
      }
    }
  }
}
