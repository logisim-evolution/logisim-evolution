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

package com.cburch.logisim.soc.data;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.gui.CpuDrawSupport;
import com.cburch.logisim.util.GraphicsUtil;

public class TraceInfo {
    private int pc;
    private int instruction;
    private String asm;
    private boolean error;
    
    public TraceInfo(int pc , int instruction, String asm, boolean error) {
      this.pc = pc;
      this.instruction = instruction;
      this.asm = asm;
      this.error = error;
    }
    
    public void setError() {
      error = true;
    }
    
    public void paint(Graphics2D g , int yOffset , boolean scale) {
      int blockWidth = CpuDrawSupport.getBlockWidth(g,scale);
      if (scale)
        blockWidth = AppPreferences.getDownScaled(blockWidth);
      int xOff = 5;
      paintBox(g,xOff,yOffset,pc, scale, blockWidth);
      xOff += blockWidth+5;
      paintBox(g,xOff,yOffset,instruction, scale, blockWidth);
      xOff += blockWidth+5;
      g.setColor(error ? Color.RED : Color.BLACK);
      Font f = g.getFont();
      Font myFont = scale ? AppPreferences.getScaledFont(new Font( "Monospaced", Font.PLAIN, 12 ).deriveFont(Font.BOLD)) :
                            new Font( "Monospaced", Font.PLAIN, 12 ).deriveFont(Font.BOLD);
      g.setFont(myFont);
      Bounds bds = CpuDrawSupport.getBounds(xOff,yOffset+15,0,0,scale);
      g.drawString(asm, bds.getX(), bds.getY());
      g.setFont(f);
    }
    
    private void paintBox(Graphics2D g, int x , int y , int value , boolean scale , int blockWidth) {
      g.setColor(Color.WHITE);
      Bounds bds;
      bds = CpuDrawSupport.getBounds(x, y+1, blockWidth, CpuDrawSupport.TRACEHEIGHT-2,scale);
      g.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g.setColor(Color.BLACK);
      g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g.setColor(error ? Color.RED : Color.DARK_GRAY);
      bds = CpuDrawSupport.getBounds(x+blockWidth/2, y+CpuDrawSupport.TRACEHEIGHT/2,0,0,scale);
      GraphicsUtil.drawCenteredText(g, String.format("0x%08X", value), bds.getX(), bds.getY());
      g.setColor(Color.BLACK);
    }
}
