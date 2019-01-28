package com.cburch.logisim.statemachine.editor.view;

import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.State;
import java.util.HashMap;

@SuppressWarnings("all")
public class FSMLayout {
  public static final int RADIUS = 30;
  
  public static final int CHAR_HEIGHT = 15;
  
  public static final int PORT_HEIGHT = 15;
  
  public static final int INPUT_X = 100;
  
  public static final int OUTPUT_X = 100;
  
  public static final int FSM_BORDER_X = 30;
  
  public static final int FSM_BORDER_Y = 30;
  
  public static final int FSM_TITLE_HEIGHT = 30;
  
  private static final boolean DEBUG = false;
  
  private HashMap<State, Integer> map = new HashMap<State, Integer>();
  
  public Object layout(final FSM e) {
    return null;
  }
}
