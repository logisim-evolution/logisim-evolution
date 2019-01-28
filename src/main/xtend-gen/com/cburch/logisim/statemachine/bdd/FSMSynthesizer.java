package com.cburch.logisim.statemachine.bdd;

import com.cburch.logisim.statemachine.fSMDSL.FSM;
import org.eclipse.xtext.xbase.lib.IntegerRange;

@SuppressWarnings("all")
public class FSMSynthesizer {
  private FSM fsm;
  
  public FSMSynthesizer(final FSM fsm) {
    this.fsm = fsm;
  }
  
  public void run() {
    int _width = this.fsm.getWidth();
    int _minus = (_width - 1);
    IntegerRange _upTo = new IntegerRange(0, _minus);
    for (final Integer i : _upTo) {
    }
  }
}
