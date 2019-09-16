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
import java.awt.Font;
import java.awt.Graphics2D;

import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;

public class SocBusTransaction {

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
  private String MasterName;
  private String SlaveName;
  private int Error;
  private boolean hidden;
   
  public SocBusTransaction(int type , int addr , int value, int access, String master) {
     this.type = type;
     this.address = addr;
     this.writeData = value;
     this.access = access;
     MasterName = master;
     SlaveName = null;
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
  
  public String transactionInitiator() {
    return MasterName;
  }
  
  public String transactionResponder() {
    return SlaveName;
  }
  
  public void setTransactionResponder( String name ) {
    SlaveName = name;
  }
  
  private void paintTraceInfo(Graphics2D g, boolean isRequest) {
    g.setColor(Color.BLACK);
    g.drawLine(0, 0, 0, SocBusStateInfo.TraceHeight-2);
    if (hasError()&&!isRequest) {
      Font f = g.getFont();
      g.setColor(Color.RED);
      g.setFont(StdAttr.DEFAULT_LABEL_FONT);
      GraphicsUtil.drawCenteredText(g, getShortErrorMessage(), 117, (SocBusStateInfo.TraceHeight-2)>>1);
      g.setFont(f);
      g.setColor(Color.BLACK);
      return;
    }
    String title = isRequest ? S.get("SocBusStateMaster")+transactionInitiator() : 
    S.get("SocBusStateSlave")+transactionResponder();
    GraphicsUtil.drawCenteredText(g, title, 118, (SocBusStateInfo.TraceHeight-2)/4);
    g.drawRect(2, (SocBusStateInfo.TraceHeight-2)>>1, 92, (SocBusStateInfo.TraceHeight-2)>>1);
    g.drawLine(14, (SocBusStateInfo.TraceHeight-2)>>1, 14, (SocBusStateInfo.TraceHeight-2));
    GraphicsUtil.drawCenteredText(g, "A", 8, (3*(SocBusStateInfo.TraceHeight-2))/4);
    String Str = String.format("0x%08X", getAddress());
    GraphicsUtil.drawCenteredText(g, Str, 53, (3*(SocBusStateInfo.TraceHeight-2))/4);
    g.drawRect(98, (SocBusStateInfo.TraceHeight-2)>>1, 92, (SocBusStateInfo.TraceHeight-2)>>1);
    g.drawLine(110, (SocBusStateInfo.TraceHeight-2)>>1, 110, (SocBusStateInfo.TraceHeight-2));
    GraphicsUtil.drawCenteredText(g, "D", 104, (3*(SocBusStateInfo.TraceHeight-2))/4);
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
    GraphicsUtil.drawCenteredText(g, Str, 148, (3*(SocBusStateInfo.TraceHeight-2))/4);
    if (!isRequest)
      return;
    if (isAtomicTransaction()) {
      g.setColor(Color.yellow);
      g.fillRect(203, (SocBusStateInfo.TraceHeight-2)>>1 , 10, (SocBusStateInfo.TraceHeight-2)>>1);
      g.setColor(Color.BLUE);
      GraphicsUtil.drawCenteredText(g, "A", 208, (3*(SocBusStateInfo.TraceHeight-2))/4);
      g.setColor(Color.BLACK);
    }
    if (isWriteTransaction()) {
      g.fillRect(214, (SocBusStateInfo.TraceHeight-2)>>1 , 10, (SocBusStateInfo.TraceHeight-2)>>1);
      g.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(g, "W", 219, (3*(SocBusStateInfo.TraceHeight-2))/4);
      g.setColor(Color.BLACK);
    }
    if (isReadTransaction()) {
      g.fillRect(225, (SocBusStateInfo.TraceHeight-2)>>1 , 10, (SocBusStateInfo.TraceHeight-2)>>1);
      g.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(g, "R", 230, (3*(SocBusStateInfo.TraceHeight-2))/4);
      g.setColor(Color.BLACK);
    }
  }
    
  public void paint(int x , int y, Graphics2D g2, Long index) {
    Graphics2D g = (Graphics2D)g2.create();
    g.translate(x, y);
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, SocBusStateInfo.TraceWidth-2, SocBusStateInfo.TraceHeight-2);
    g.setColor(Color.BLACK);
    g.drawRect(0, 0, SocBusStateInfo.TraceWidth-2, SocBusStateInfo.TraceHeight-2);
    GraphicsUtil.drawCenteredText(g, S.get("SocBusStateTraceIndex"), 79, (SocBusStateInfo.TraceHeight-2)/4);
    GraphicsUtil.drawCenteredText(g, index.toString(), 79, (3*(SocBusStateInfo.TraceHeight-2)/4));
    g.translate(158, 0);
    paintTraceInfo(g,true);
    g.translate(235, 0);
    paintTraceInfo(g,false);
    g.dispose();
  }

}
