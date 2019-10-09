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

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

public class SocBusTransaction {

  public static final int BLOCK_SKIP = 2;
  public static final int BLOCK_MARKER = 14;
  public static final int BLOCK_HEX = 78;

  public static int READTransaction = 1;
  public static int WRITETransaction = 2;
  public static int ATOMICTransaction = 4;
  
  public static final int NoError = 0;
  public static final int NoResponsError = 1;
  public static final int NoSlavesError = 2;
  public static final int MultipleSlavesError = 3;
  public static final int NoneAtomicReadWriteError = 4;
  public static final int NoSocBusConnectedError = 5;
  public static final int MisallignedAddressError = 6;
  public static final int AccessTypeNotSupportedError = 7;
  public static final int ReadOnlyAccessError = 8;
  public static final int WriteOnlyAccessError = 9;
  public static final int RegisterDoesNotExistError = 10;
  
  
  public static final int ByteAccess = 1;
  public static final int HalfWordAccess = 2;
  public static final int WordAccess = 3;
   
  private int address,writeData,readData,type,access;
  private Object master;
  private Component slave;
  private int Error;
  private boolean hidden;
   
  public SocBusTransaction(int type , int addr , int value, int access, Object master) {
     this.type = type;
     this.address = addr;
     this.writeData = value;
     this.access = access;
     this.master = master;
     slave = null;
     readData = 0;
     Error = NoError;
     hidden = false;
  }
  
  public void setAsHiddenTransaction() {
    hidden = true;
  }
  
  public boolean isHidden() {
    return hidden;
  }
  
  public int getAccessType() {
    return access;
  }
  
  public String getErrorMessage() {
    switch (Error) {
      case NoError : return S.get("SocTransactionSuccessfull");
      case NoResponsError : return S.get("SocTransactionNoRespons");
      case NoSlavesError : return S.get("SocTransactionNoSlavesAttached");
      case MultipleSlavesError : return S.get("SocTransactionMultipleSlaveAnswers");
      case NoneAtomicReadWriteError : return S.get("SocTransactionNoneAtomicRW");
      case NoSocBusConnectedError : return S.get("SocTransactionNoBusConnected");
      case MisallignedAddressError: return S.get("SocTransactionMisallignedAddress");
      case AccessTypeNotSupportedError : 
        switch (access) {
          case ByteAccess     : return S.get("SocTransactionByteAccesNoSupport");
          case HalfWordAccess : return S.get("SocTransactionHalfWordAccesNoSupport");
          default             : return S.get("SocTransactionWordAccesNoSupport");
        }
      case ReadOnlyAccessError : return S.get("SocTransactionReadOnlyAccessError");
      case WriteOnlyAccessError: return S.get("SocTransactionWriteOnlyAccessError");
      case RegisterDoesNotExistError : return S.get("SocTransactionRegisterDoesNotExist");
    }
    return S.get("SocTransactionUnknownError");
  }
  
  public String getShortErrorMessage() {
    switch (Error) {
      case NoError : return S.get("SocTransactionSuccessfullShort");
      case NoResponsError : return S.get("SocTransactionNoResponsShort");
      case NoSlavesError : return S.get("SocTransactionNoSlavesAttachedShort");
      case MultipleSlavesError : return S.get("SocTransactionMultipleSlaveAnswersShort");
      case NoneAtomicReadWriteError : return S.get("SocTransactionNoneAtomicRWShort");
      case NoSocBusConnectedError : return S.get("SocTransactionNoBusConnectedShort");
      case MisallignedAddressError: return S.get("SocTransactionMisallignedAddressShort");
      case AccessTypeNotSupportedError : 
        switch (access) {
          case ByteAccess     : return S.get("SocTransactionByteAccesNoSupportShort");
          case HalfWordAccess : return S.get("SocTransactionHalfWordAccesNoSupportShort");
          default             : return S.get("SocTransactionWordAccesNoSupportShort");
        }
      case ReadOnlyAccessError : return S.get("SocTransactionReadOnlyAccessErrorShort");
      case WriteOnlyAccessError: return S.get("SocTransactionWriteOnlyAccessErrorShort");
      case RegisterDoesNotExistError : return S.get("SocTransactionRegisterDoesNotExistShort");
    }
    return S.get("SocTransactionUnknownErrorShort");
  }
  
  public int getType() {
    return type;
  }
  
  public void setError(int value) {
    Error = value;
  }
  
  public boolean hasError() {
    return Error != NoError;
  }
  
  public boolean isReadTransaction() {
    return (type&READTransaction) != 0;
  }
  
  public boolean isWriteTransaction() {
    return (type&WRITETransaction) != 0;
  }
  
  public boolean isAtomicTransaction() {
    return (type&ATOMICTransaction) != 0;
  }
  
  public int getAddress() {
    return address;
  }
  
  public int getReadData() {
    return readData;
  }
  
  public int getWriteData() {
    return writeData;
  }
	  
  public void setReadData(int value) {
    readData = value;
  }
  
  private String getTransactionInitiatorName() {
    if (master instanceof String)
      return (String)master;
    if (master instanceof Component)
      return SocSupport.getComponentName((Component) master);
    return "BUG";
  }
  
  public Object getTransactionInitiator() { return master; }
  
  private String getTransactionResponderName() { return SocSupport.getComponentName(slave); }
  public Component getTransactionResponder() { return slave; }
  
  public void setTransactionResponder( Component comp ) { slave = comp; }
  
  private void paintTraceInfo(Graphics2D g2, BoxInfo bi, boolean isRequest,boolean scale) {
	Graphics2D g = (Graphics2D)g2.create();
    Bounds bds;
	if (!scale) {
      g.setColor(Color.BLACK);
      g.drawLine(0, 0, 0, SocBusStateInfo.TraceHeight-2);
	}
    if (hasError()&&!isRequest) {
      g.setColor(Color.RED);
      g.setFont(StdAttr.DEFAULT_LABEL_FONT);
      bds = getScaled(bi.blockWidth/2,(SocBusStateInfo.TraceHeight-2)>>1,0,0,scale);
      GraphicsUtil.drawCenteredText(g, getShortErrorMessage(), bds.getX(), bds.getY());
      g.dispose();
      return;
    }
    String title = isRequest ? S.get("SocBusStateMaster")+getTransactionInitiatorName() : 
            S.get("SocBusStateSlave")+getTransactionResponderName();
    bds = getScaled(bi.blockWidth/2,(SocBusStateInfo.TraceHeight-2)>>2,0,0,scale);
    GraphicsUtil.drawCenteredText(g, title, bds.getX(), bds.getY());
    bds = getScaled(bi.skip, ((SocBusStateInfo.TraceHeight-2)>>1)+1, bi.mark+bi.hex, 
    		(SocBusStateInfo.TraceHeight-2)>>1,scale);
    g.drawRect(bds.getX(),bds.getY(),bds.getWidth(),bds.getHeight());
    bds = getScaled(bi.skip+bi.mark, ((SocBusStateInfo.TraceHeight-2)>>1)+1,0,SocBusStateInfo.TraceHeight-2,scale);
    g.drawLine(bds.getX(),bds.getY(),bds.getX(),bds.getHeight());
    bds = getScaled(bi.skip+bi.mark/2, (3*(SocBusStateInfo.TraceHeight-2))/4,0,0,scale);
    GraphicsUtil.drawCenteredText(g, "A", bds.getX(),bds.getY());
    String Str = String.format("0x%08X", getAddress());
    bds = getScaled(bi.skip+bi.mark+bi.hex/2, (3*(SocBusStateInfo.TraceHeight-2))/4,0,0,scale);
    GraphicsUtil.drawCenteredText(g, Str, bds.getX(), bds.getY());
    bds = getScaled(bi.skip+bi.mark+bi.hex,0,0,0,scale);
    g.translate(bds.getX(), 0);
    
    bds = getScaled(bi.skip, ((SocBusStateInfo.TraceHeight-2)>>1)+1, bi.mark+bi.hex, (SocBusStateInfo.TraceHeight-2)>>1,scale);
    g.drawRect(bds.getX(),bds.getY(),bds.getWidth(),bds.getHeight());
    bds = getScaled(bi.skip+bi.mark, ((SocBusStateInfo.TraceHeight-2)>>1)+1, 0, (SocBusStateInfo.TraceHeight-2),scale);
    g.drawLine(bds.getX(),bds.getY(),bds.getX(),bds.getHeight());
    bds = getScaled(bi.skip+bi.mark/2, (3*(SocBusStateInfo.TraceHeight-2))/4,0,0,scale);
    GraphicsUtil.drawCenteredText(g, "D", bds.getX(), bds.getY());
    if ((isRequest && isWriteTransaction())||
        (!isRequest && isReadTransaction())) {
      String format = "0x%08X";
      if (getAccessType() == SocBusTransaction.HalfWordAccess)
        format = "0x%04X";
      if (getAccessType() == SocBusTransaction.ByteAccess)
        format = "0x%02X";
      Str = String.format(format, isRequest ? getWriteData() : getReadData());
    }
    else
      Str = S.get("SocBusStateNoDataMax10chars");
    bds = getScaled(bi.skip+bi.mark+bi.hex/2, (3*(SocBusStateInfo.TraceHeight-2))/4,0,0,scale);
    GraphicsUtil.drawCenteredText(g, Str, bds.getX(), bds.getY());
    if (!isRequest) { g.dispose(); return; }
    bds = getScaled(bi.skip+bi.mark+bi.hex,0,0,0,scale);
    g.translate(bds.getX(), 0);
    if (isAtomicTransaction()) {
      g.setColor(Color.yellow);
      bds = getScaled(0, ((SocBusStateInfo.TraceHeight-2)>>1)+1 , bi.mark, (SocBusStateInfo.TraceHeight-2)>>1,scale);
      g.fillRect(bds.getX(),bds.getY(),bds.getWidth(),bds.getHeight());
      g.setColor(Color.BLUE);
      bds = getScaled(bi.mark/2, (3*(SocBusStateInfo.TraceHeight-2))/4,0,0,scale);
      GraphicsUtil.drawCenteredText(g, "A", bds.getX(), bds.getY());
      g.setColor(Color.BLACK);
    }
    bds = getScaled(bi.skip+bi.mark,0,0,0,scale);
    g.translate(bds.getX(), 0);
    if (isWriteTransaction()) {
      bds = getScaled(0, ((SocBusStateInfo.TraceHeight-2)>>1)+1 , bi.mark, (SocBusStateInfo.TraceHeight-2)>>1,scale);
      g.setColor(Color.MAGENTA);
      g.fillRect(bds.getX(),bds.getY(),bds.getWidth(),bds.getHeight());
      g.setColor(Color.BLACK);
      bds = getScaled(bi.mark/2, (3*(SocBusStateInfo.TraceHeight-2))/4,0,0,scale);
      GraphicsUtil.drawCenteredText(g, "W", bds.getX(), bds.getY());
    }
    bds = getScaled(bi.skip+bi.mark,0,0,0,scale);
    g.translate(bds.getX(), 0);
    if (isReadTransaction()) {
      bds = getScaled(0, ((SocBusStateInfo.TraceHeight-2)>>1)+1 , bi.mark, (SocBusStateInfo.TraceHeight-2)>>1,scale);
      g.setColor(Color.CYAN);
      g.fillRect(bds.getX(),bds.getY(),bds.getWidth(),bds.getHeight());
      bds = getScaled(bi.mark/2, (3*(SocBusStateInfo.TraceHeight-2))/4, 0, 0, scale);
      g.setColor(Color.BLACK);
      GraphicsUtil.drawCenteredText(g, "R", bds.getX(), bds.getY());
    }
    g.dispose();
  }
    
  public void paint(int x , int y, Graphics2D g2, Long index) {
	BoxInfo realWidth = getRealBlockWidth(g2,false);
    Graphics2D g = (Graphics2D)g2.create();
    g.translate(x, y);
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, SocBusStateInfo.TraceWidth-2, SocBusStateInfo.TraceHeight-1);
    g.setColor(Color.BLACK);
    g.drawRect(0, 0, SocBusStateInfo.TraceWidth-2, SocBusStateInfo.TraceHeight-1);
    GraphicsUtil.drawCenteredText(g, S.get("SocBusStateTraceIndex"), 79, (SocBusStateInfo.TraceHeight-2)/4);
    GraphicsUtil.drawCenteredText(g, index.toString(), 79, (3*(SocBusStateInfo.TraceHeight-2)/4));
    g.translate(158, 0);
    paintTraceInfo(g,realWidth,true,false);
    g.translate(235, 0);
    paintTraceInfo(g,realWidth,false,false);
    g.dispose();
  }
  
  private BoxInfo getRealBlockWidth(Graphics2D g, boolean scale) {
	BoxInfo i = new BoxInfo();
	if (scale) {
	  i.skip = AppPreferences.getScaled(BLOCK_SKIP);
      double prefferedMark = AppPreferences.getScaled(BLOCK_MARKER);
      double prefferedHex = AppPreferences.getScaled(BLOCK_HEX);
      FontMetrics t = g.getFontMetrics();
      double realHex = t.getStringBounds("0x00000000", g).getWidth();
      double corFactor = realHex <= prefferedHex ? 1.0 : realHex/prefferedHex;
      i.mark = AppPreferences.getDownScaled((int) Math.round(corFactor*prefferedMark));
      i.hex = AppPreferences.getDownScaled((int) Math.round(corFactor*prefferedHex));
      i.blockWidth = 6*i.skip+5*i.mark+2*i.hex;
	} else {
	  i.skip = BLOCK_SKIP;
	  i.mark = BLOCK_MARKER;
	  i.hex = BLOCK_HEX;
	  i.blockWidth = SocBusStateInfo.BlockWidth;
	}
	return i;
  }
  
  private class BoxInfo {
    private int skip;
    private int mark;
    private int hex;
    private int blockWidth;
  }
  
  private Bounds getScaled(int x , int y , int width, int height, boolean scale) {
	if (scale)
      return Bounds.create(AppPreferences.getScaled(x), AppPreferences.getScaled(y), 
            AppPreferences.getScaled(width), AppPreferences.getScaled(height));
	return Bounds.create(x, y, width, height);
  }
  
  public int paint(Graphics2D g2, Long index, int width) {
	BoxInfo realWidth = getRealBlockWidth(g2,true);
	int usedWidth = realWidth.blockWidth <= width ? width : realWidth.blockWidth; 
    Bounds bds = getScaled(usedWidth/2,(SocBusStateInfo.TraceHeight-2)/4,usedWidth,SocBusStateInfo.TraceHeight>>1,true);
    g2.setColor(Color.LIGHT_GRAY);
    g2.fillRect(0, 0, bds.getWidth(), bds.getHeight()-1);
    g2.setColor(Color.black);
    g2.drawRect(0, 0, bds.getWidth(), bds.getHeight()-1);
    GraphicsUtil.drawCenteredText(g2, S.get("SocBusStateTraceIndex")+" "+index.toString(), bds.getX(), bds.getY());
    g2.translate(0, bds.getHeight());
    bds = getScaled(0,0,usedWidth,SocBusStateInfo.TraceHeight,true);
    g2.drawLine(0, -1, bds.getWidth(), -1);
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 1, bds.getWidth()-2, bds.getHeight()-1);
    g2.setColor(Color.BLACK);
    g2.drawRect(0, 1, bds.getWidth()-2, bds.getHeight()-1);
    paintTraceInfo(g2,realWidth,true,true);
    bds = getScaled(0,0,usedWidth,SocBusStateInfo.TraceHeight,true);
    g2.translate(0, bds.getHeight());
    g2.setColor(Color.YELLOW);
    g2.fillRect(0, 0, bds.getWidth(), bds.getHeight());
    g2.setColor(Color.BLACK);
    g2.drawLine(0, 0, bds.getWidth(), 0);
    paintTraceInfo(g2,realWidth,false,true);
    return usedWidth;
  }

}
