package com.cburch.logisim.statemachine;

import com.cburch.logisim.statemachine.fSMDSL.CommandList;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.FSMElement;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import org.eclipse.emf.common.util.EList;

@SuppressWarnings("all")
public class FSMUtils {
  public void getAllFSMElements(final FSM fsm) {
    ArrayList<FSMElement> l = new ArrayList<FSMElement>();
    l.add(fsm);
    EList<Port> _in = fsm.getIn();
    Iterables.<FSMElement>addAll(l, _in);
    EList<Port> _out = fsm.getOut();
    Iterables.<FSMElement>addAll(l, _out);
    EList<State> _states = fsm.getStates();
    Iterables.<FSMElement>addAll(l, _states);
    EList<State> _states_1 = fsm.getStates();
    for (final State s : _states_1) {
      {
        CommandList _commandList = s.getCommandList();
        l.add(_commandList);
        EList<Transition> _transition = s.getTransition();
        for (final Transition t : _transition) {
          l.add(t);
        }
      }
    }
  }
}
