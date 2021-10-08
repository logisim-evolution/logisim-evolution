/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it "+AND+"/or modify
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
 * with logisim-evolution. If "+NOT+", see <http://www.gnu.org/licenses/>.
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

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class Ttl7442HdlGenerator extends AbstractHdlGeneratorFactory {

  private final boolean IsExes3;
  private final boolean IsGray;

  public Ttl7442HdlGenerator() {
    this(false, false);
  }

  public Ttl7442HdlGenerator(boolean Exess3, boolean Gray) {
    super();
    IsExes3 = Exess3;
    IsGray = Gray;
    myPorts
        .add(Port.INPUT, "A", 1, 13)
        .add(Port.INPUT, "B", 1, 12)
        .add(Port.INPUT, "C", 1, 11)
        .add(Port.INPUT, "D", 1, 10)
        .add(Port.OUTPUT, "O0", 1, 0)
        .add(Port.OUTPUT, "O1", 1, 1)
        .add(Port.OUTPUT, "O2", 1, 2)
        .add(Port.OUTPUT, "O3", 1, 3)
        .add(Port.OUTPUT, "O4", 1, 4)
        .add(Port.OUTPUT, "O5", 1, 5)
        .add(Port.OUTPUT, "O6", 1, 6)
        .add(Port.OUTPUT, "O7", 1, 7)
        .add(Port.OUTPUT, "O8", 1, 8)
        .add(Port.OUTPUT, "O9", 1, 9);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist nets, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer();
    if (IsExes3) {
      contents.add("""
          {{assign}} O0 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} B {{and}} A );
          {{assign}} O1 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} {{not}}(A) );
          {{assign}} O2 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} A );
          {{assign}} O3 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} {{not}}(A) );
          {{assign}} O4 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} A );
          {{assign}} O5 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} {{not}}(A) );
          {{assign}} O6 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} A );
          {{assign}} O7 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} B {{and}} {{not}}(A) );
          {{assign}} O8 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} B {{and}} A );
          {{assign}} O9 {{=}} {{not}}( D {{and}} C {{and}} {{not}}(B) {{and}} {{not}}(A) );");
          """);
    } else if (IsGray) {
      contents.add("""
          {{assign}} O0 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} B {{and}} {{not}}(A) );
          {{assign}} O1 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} {{not}}(A) );
          {{assign}} O2 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} A );
          {{assign}} O3 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} A );
          {{assign}} O4 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} {{not}(A) );
          {{assign}} O5 {{=}} {{not}}( D {{and}} C {{and}} {{not}}(B) {{and}} {{not}}(A) );
          {{assign}} O6 {{=}} {{not}}( D {{and}} C {{and}} {{not}}(B) {{and}} A );
          {{assign}} O7 {{=}} {{not}}( D {{and}} C {{and}} B {{and}} A );
          {{assign}} O8 {{=}} {{not}}( D {{and}} C {{and}} B {{and}} {{not}}(A) );
          {{assign}} O9 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} B {{and}} {not}}(A) );");
          """);
    } else {
      contents.add("""
          {{assign}} O0 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} {{not}}(A) );
          {{assign}} O1 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} A );
          {{assign}} O2 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} B {{and}} {{not}}(A) );
          {{assign}} O3 {{=}} {{not}}( {{not}}(D) {{and}} {{not}}(C) {{and}} B {{and}} A );
          {{assign}} O4 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} {{not}}(A) );
          {{assign}} O5 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} {{not}}(B) {{and}} A );
          {{assign}} O6 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} {{not}}(A) );
          {{assign}} O7 {{=}} {{not}}( {{not}}(D) {{and}} C {{and}} B {{and}} A );
          {{assign}} O8 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} {{not}}(A) );
          {{assign}} O9 {{=}} {{not}}( D {{and}} {{not}}(C) {{and}} {{not}}(B) {{and}} A );"
          """);
    }
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND));
  }
}
