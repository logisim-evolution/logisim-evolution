package com.cburch.logisim.statemachine.editor.view;

import com.cburch.logisim.statemachine.editor.editpanels.FSMCommandListEditPanel;
import com.cburch.logisim.statemachine.editor.editpanels.FSMEditPanel;
import com.cburch.logisim.statemachine.editor.editpanels.FSMPortEditPanel;
import com.cburch.logisim.statemachine.editor.editpanels.FSMStateEditPanel;
import com.cburch.logisim.statemachine.editor.editpanels.FSMTransitionEditPanel;
import com.cburch.logisim.statemachine.fSMDSL.CommandList;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.InputPort;
import com.cburch.logisim.statemachine.fSMDSL.OutputPort;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import java.util.Arrays;
import org.eclipse.emf.ecore.EObject;

@SuppressWarnings("all")
public class FSMEdit {
  protected Object _edit(final EObject e) {
    return null;
  }
  
  protected Object _edit(final CommandList e) {
    final FSMCommandListEditPanel panel = new FSMCommandListEditPanel(e);
    panel.configure();
    System.out.println(e);
    return null;
  }
  
  protected Object _edit(final State e) {
    final FSMStateEditPanel panel = new FSMStateEditPanel(e);
    panel.configure();
    System.out.println(e);
    return null;
  }
  
  protected Object _edit(final FSM e) {
    final FSMEditPanel panel = new FSMEditPanel(e);
    panel.configure();
    System.out.println(e);
    return null;
  }
  
  protected Object _edit(final Transition e) {
    final FSMTransitionEditPanel panel = new FSMTransitionEditPanel(e);
    panel.configure();
    System.out.println(e);
    return null;
  }
  
  protected Object _edit(final InputPort e) {
    final FSMPortEditPanel panel = new FSMPortEditPanel(e);
    panel.configure();
    System.out.println(e);
    return null;
  }
  
  protected Object _edit(final OutputPort e) {
    final FSMPortEditPanel panel = new FSMPortEditPanel(e);
    panel.configure();
    System.out.println(e);
    return null;
  }
  
  public Object edit(final EObject e) {
    if (e instanceof InputPort) {
      return _edit((InputPort)e);
    } else if (e instanceof OutputPort) {
      return _edit((OutputPort)e);
    } else if (e instanceof CommandList) {
      return _edit((CommandList)e);
    } else if (e instanceof FSM) {
      return _edit((FSM)e);
    } else if (e instanceof State) {
      return _edit((State)e);
    } else if (e instanceof Transition) {
      return _edit((Transition)e);
    } else if (e != null) {
      return _edit(e);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(e).toString());
    }
  }
}
