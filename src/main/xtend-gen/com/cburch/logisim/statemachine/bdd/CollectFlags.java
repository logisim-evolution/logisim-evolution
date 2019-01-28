package com.cburch.logisim.statemachine.bdd;

import com.cburch.logisim.statemachine.fSMDSL.AndExpr;
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.InputPort;
import com.cburch.logisim.statemachine.fSMDSL.NotExpr;
import com.cburch.logisim.statemachine.fSMDSL.OrExpr;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.fSMDSL.PortRef;
import com.cburch.logisim.statemachine.fSMDSL.util.FSMDSLSwitch;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

@SuppressWarnings("all")
public class CollectFlags extends FSMDSLSwitch<Object> {
  private EList<InputPort> list;
  
  public EList<InputPort> collect(final BoolExpr bexp) {
    BasicEList<InputPort> _basicEList = new BasicEList<InputPort>();
    this.list = _basicEList;
    this.doSwitch(bexp);
    return this.list;
  }
  
  @Override
  public Object caseAndExpr(final AndExpr object) {
    EList<BoolExpr> _args = object.getArgs();
    for (final BoolExpr bexp : _args) {
      this.doSwitch(bexp);
    }
    return super.caseAndExpr(object);
  }
  
  @Override
  public Object caseOrExpr(final OrExpr object) {
    EList<BoolExpr> _args = object.getArgs();
    for (final BoolExpr bexp : _args) {
      this.doSwitch(bexp);
    }
    return super.caseOrExpr(object);
  }
  
  @Override
  public Object caseNotExpr(final NotExpr object) {
    this.doSwitch(object.getArgs().get(0));
    return super.caseNotExpr(object);
  }
  
  @Override
  public Object casePortRef(final PortRef object) {
    boolean _contains = this.list.contains(object.getPort());
    boolean _not = (!_contains);
    if (_not) {
      Port _port = object.getPort();
      this.list.add(((InputPort) _port));
    }
    return super.casePortRef(object);
  }
}
