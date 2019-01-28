package com.cburch.logisim.statemachine;

import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.parser.FSMSerializer;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.InputOutput;

@SuppressWarnings("all")
public class FSMExample {
  public static FSM getEx0() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("fsm baseFSM {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("in A;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("in B;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("out X;");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("out Y;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("start=S0;");
      _builder.newLine();
      _builder.append("\t");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("state S0 {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("code = \"0000\";");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("commands {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("X=A;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("transitions {");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("S0 -> S0 when A+B;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      _builder.newLine();
      FSM _load = FSMSerializer.load(_builder.toString());
      return ((FSM) _load);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public static void main(final String[] args) {
    InputOutput.<FSM>print(FSMExample.getEx0());
  }
}
