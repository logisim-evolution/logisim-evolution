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

public class SocBusTransaction implements Cloneable {

  public static int READTransaction = 1;
  public static int WRITETransaction = 2;
  public static int ATOMICTransaction = 4;
  
  public static final int NoError = 0;
  public static final int NoResponsError = 1;
  public static final int NoSlavesError = 2;
  public static final int MultipleSlavesError = 3;
  public static final int NoneAtomicReadWriteError = 4;
  public static final int NoSocBusConnectedError = 5;
  public static final int ByteAccess = 1;
  public static final int HalfWordAccess = 2;
  public static final int WordAccess = 3;
   
  private int address,data,type,access;
  private String MasterName;
  private String SlaveName;
  private int Error;
  private boolean hidden;
   
  public SocBusTransaction(int type , int addr , int value, int access, String master) {
     this.type = type;
     this.address = addr;
     this.data = value;
     this.access = access;
     MasterName = master;
     SlaveName = null;
     Error = NoError;
     hidden = false;
  }
  
  public SocBusTransaction clone() {
	SocBusTransaction ret = new SocBusTransaction(type,address,data,access,MasterName);
	ret.SlaveName = SlaveName;
	ret.Error = Error;
	ret.hidden = hidden;
	return ret;
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
  
  public int getData() {
    return data;
  }
  
  public void setData(int value) {
    data = value;
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
}
