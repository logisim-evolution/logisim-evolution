package com.cburch.logisim.statemachine.editor.commands;

import com.cburch.logisim.statemachine.editor.commands.AbstractCommand;
import com.cburch.logisim.statemachine.fSMDSL.FSMElement;
import java.util.List;

@SuppressWarnings("all")
public class CopyCommand extends AbstractCommand {
  private List<FSMElement> copyList;
  
  public CopyCommand() {
  }
}
