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

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import java.util.ArrayList;

public class PortHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }
  
  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
  
  @Override
  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      String CircuitName) {
    ArrayList<String> Contents = new ArrayList<>();
    AttributeOption dir = ComponentInfo.GetComponent().getAttributeSet().getValue(PortIO.ATTR_DIR);
    int size = ComponentInfo.GetComponent().getAttributeSet().getValue(PortIO.ATTR_SIZE).getWidth();
    int nBus = (((size - 1) / BitWidth.MAXWIDTH) + 1);
    if (dir == PortIO.INPUT) {
      for (int i = 0 ; i < nBus ; i++) {
        int start = ComponentInfo.GetLocalBubbleInputStartId()+i*BitWidth.MAXWIDTH;
        int end = start-1;
        end += Math.min(size, BitWidth.MAXWIDTH);
        size -= BitWidth.MAXWIDTH;
        Contents.add("   "+HDL.assignPreamble()+GetBusName(ComponentInfo, i, Nets)+
                     HDL.assignOperator()+HDLGeneratorFactory.LocalInputBubbleBusname+
                     HDL.BracketOpen()+end+HDL.vectorLoopId()+"0"+HDL.BracketClose()+";");
      }
    } else if (dir == PortIO.OUTPUT) {
      for (int i = 0 ; i < nBus ; i++) {
        int start = ComponentInfo.GetLocalBubbleOutputStartId()+i*BitWidth.MAXWIDTH;
        int end = start-1;
        end += Math.min(size, BitWidth.MAXWIDTH);
        size -= BitWidth.MAXWIDTH;
        Contents.add("   "+HDL.assignPreamble()+HDLGeneratorFactory.LocalOutputBubbleBusname+
                     HDL.assignOperator()+GetBusName(ComponentInfo, i, Nets)+
                     HDL.BracketOpen()+end+HDL.vectorLoopId()+"0"+HDL.BracketClose()+";");
      }
    } else {
      for (int i = 0 ; i < nBus ; i++) {
        int start = ComponentInfo.GetLocalBubbleInOutStartId()+i*BitWidth.MAXWIDTH;
        int nbits = Math.min(size, BitWidth.MAXWIDTH);
        int end = start-1+nbits;
        size -= nbits;
        int enableIndex = (dir == PortIO.INOUTSE) ? 0 : i*2;
        int inputIndex = (dir == PortIO.INOUTSE) ? i+1 : i*2+1;
        int outputIndex = (dir == PortIO.INOUTSE) ? 1+nBus+i : 2*nBus+i;
        String InputName = GetBusName(ComponentInfo, inputIndex, Nets);
        String OutputName = GetBusName(ComponentInfo, outputIndex, Nets);
        String EnableName = (dir == PortIO.INOUTSE) ? GetNetName(ComponentInfo, enableIndex, true, Nets) :
                                                      GetBusName(ComponentInfo, enableIndex, Nets);
        Contents.add("   "+HDL.assignPreamble()+OutputName+HDL.assignOperator()+HDLGeneratorFactory.LocalInOutBubbleBusname+
                     HDL.BracketOpen()+end+HDL.vectorLoopId()+start+HDL.BracketClose()+";");
        if (dir == PortIO.INOUTSE) {
          if (HDL.isVHDL()) {
            Contents.add("   "+HDLGeneratorFactory.LocalInOutBubbleBusname+
                         HDL.BracketOpen()+end+HDL.vectorLoopId()+start+HDL.BracketClose()+" <= "+
            		     InputName+" WHEN "+EnableName+" = '1' ELSE (OTHERS => 'Z');"); 
          } else {
            Contents.add("   "+HDL.assignPreamble()+HDLGeneratorFactory.LocalInOutBubbleBusname+
                         HDL.BracketOpen()+end+HDL.vectorLoopId()+start+HDL.BracketClose()+" = ("+EnableName+
                         ") ? "+InputName+" : "+nbits+"'bZ;");
          }
        } else {
          for (int bit = 0 ; bit < nbits ; bit++) {
            if (HDL.isVHDL()) {
              Contents.add("   "+HDLGeneratorFactory.LocalInOutBubbleBusname+
                           HDL.BracketOpen()+(start+bit)+HDL.BracketClose()+" <= "+
              		       InputName+"("+bit+") WHEN "+EnableName+"("+bit+") = '1' ELSE 'Z';"); 
            } else {
              Contents.add("   "+HDL.assignPreamble()+HDLGeneratorFactory.LocalInOutBubbleBusname+
                           HDL.BracketOpen()+(start+bit)+HDL.BracketClose()+" = ("+EnableName+
                           "["+bit+"]) ? "+InputName+"["+bit+"] : 1'bZ;");
            }
          }
        }
      }
    }
    return Contents;
  }  
}
