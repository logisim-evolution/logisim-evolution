package com.cburch.logisim.statemachine.editor.view;

import com.cburch.logisim.statemachine.fSMDSL.AndExpr;
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.CommandList;
import com.cburch.logisim.statemachine.fSMDSL.Constant;
import com.cburch.logisim.statemachine.fSMDSL.DefaultPredicate;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.FSMDSLFactory;
import com.cburch.logisim.statemachine.fSMDSL.InputPort;
import com.cburch.logisim.statemachine.fSMDSL.LayoutInfo;
import com.cburch.logisim.statemachine.fSMDSL.NotExpr;
import com.cburch.logisim.statemachine.fSMDSL.OrExpr;
import com.cburch.logisim.statemachine.fSMDSL.OutputPort;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.fSMDSL.PortRef;
import com.cburch.logisim.statemachine.fSMDSL.Range;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import com.google.common.base.Objects;
import java.util.List;
import org.eclipse.emf.common.util.EList;

@SuppressWarnings("all")
public class FSMCustomFactory {
  public static FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
  
  public static int CMD_OFFSETX = 45;
  
  public static int CMD_OFFSETY = 15;
  
  public static int CMD_WIDTH = 30;
  
  public static int CMD_HEIGHT = 20;
  
  public static int PRED_WIDTH = 20;
  
  public static int PRED_HEIGHT = 15;
  
  public static int PORT_HEIGHT = 30;
  
  public static int PORT_WIDTH = 20;
  
  public static int FSM_HEIGHT = 500;
  
  public static int FSM_WIDTH = 500;
  
  public static int STATE_RADIUS = 30;
  
  public static State state(final String label, final String code, final int x, final int y) {
    State _xblockexpression = null;
    {
      final State s = FSMCustomFactory.factory.createState();
      s.setName(label);
      s.setCode(code);
      s.setCommandList(FSMCustomFactory.factory.createCommandList());
      CommandList _commandList = s.getCommandList();
      _commandList.setLayout(FSMCustomFactory.factory.createLayoutInfo());
      LayoutInfo _layout = s.getCommandList().getLayout();
      _layout.setX((x + FSMCustomFactory.CMD_OFFSETX));
      LayoutInfo _layout_1 = s.getCommandList().getLayout();
      _layout_1.setY((y + FSMCustomFactory.CMD_OFFSETY));
      LayoutInfo _layout_2 = s.getCommandList().getLayout();
      _layout_2.setWidth(FSMCustomFactory.CMD_WIDTH);
      LayoutInfo _layout_3 = s.getCommandList().getLayout();
      _layout_3.setHeight(FSMCustomFactory.CMD_HEIGHT);
      s.setLayout(FSMCustomFactory.factory.createLayoutInfo());
      LayoutInfo _layout_4 = s.getLayout();
      _layout_4.setX(x);
      LayoutInfo _layout_5 = s.getLayout();
      _layout_5.setY(y);
      LayoutInfo _layout_6 = s.getLayout();
      _layout_6.setWidth(FSMCustomFactory.STATE_RADIUS);
      LayoutInfo _layout_7 = s.getLayout();
      _layout_7.setHeight(FSMCustomFactory.STATE_RADIUS);
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static FSM fsm(final String label) {
    FSM _xblockexpression = null;
    {
      final FSM s = FSMCustomFactory.factory.createFSM();
      s.setName(label);
      s.setLayout(FSMCustomFactory.factory.createLayoutInfo());
      LayoutInfo _layout = s.getLayout();
      _layout.setX(15);
      LayoutInfo _layout_1 = s.getLayout();
      _layout_1.setY(15);
      LayoutInfo _layout_2 = s.getLayout();
      _layout_2.setWidth(FSMCustomFactory.FSM_WIDTH);
      LayoutInfo _layout_3 = s.getLayout();
      _layout_3.setHeight(FSMCustomFactory.FSM_WIDTH);
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static Transition transition(final State src, final State dst, final int x, final int y) {
    Transition _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final Transition t = factory.createTransition();
      t.setDst(dst);
      t.setSrc(src);
      t.setLayout(factory.createLayoutInfo());
      LayoutInfo _layout = t.getLayout();
      _layout.setX(x);
      LayoutInfo _layout_1 = t.getLayout();
      _layout_1.setY(y);
      LayoutInfo _layout_2 = t.getLayout();
      _layout_2.setWidth(FSMCustomFactory.PRED_WIDTH);
      LayoutInfo _layout_3 = t.getLayout();
      _layout_3.setHeight(FSMCustomFactory.PRED_HEIGHT);
      src.getTransition().add(t);
      t.setPredicate(FSMCustomFactory.defaultPred());
      _xblockexpression = t;
    }
    return _xblockexpression;
  }
  
  public static DefaultPredicate defaultPred() {
    DefaultPredicate _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final DefaultPredicate t = factory.createDefaultPredicate();
      _xblockexpression = t;
    }
    return _xblockexpression;
  }
  
  public static InputPort inport(final String label, final int width, final int x, final int y) {
    InputPort _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final InputPort s = factory.createInputPort();
      s.setName(label);
      s.setWidth(width);
      s.setLayout(factory.createLayoutInfo());
      LayoutInfo _layout = s.getLayout();
      _layout.setX(x);
      LayoutInfo _layout_1 = s.getLayout();
      _layout_1.setY(y);
      LayoutInfo _layout_2 = s.getLayout();
      _layout_2.setWidth(FSMCustomFactory.PORT_WIDTH);
      LayoutInfo _layout_3 = s.getLayout();
      _layout_3.setHeight(FSMCustomFactory.PORT_HEIGHT);
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static OutputPort outport(final String label, final int width, final int x, final int y) {
    OutputPort _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final OutputPort s = factory.createOutputPort();
      s.setName(label);
      s.setWidth(width);
      s.setLayout(factory.createLayoutInfo());
      LayoutInfo _layout = s.getLayout();
      _layout.setWidth(FSMCustomFactory.PORT_WIDTH);
      LayoutInfo _layout_1 = s.getLayout();
      _layout_1.setHeight(FSMCustomFactory.PORT_HEIGHT);
      LayoutInfo _layout_2 = s.getLayout();
      _layout_2.setX(x);
      LayoutInfo _layout_3 = s.getLayout();
      _layout_3.setY(y);
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static PortRef pref(final Port p) {
    PortRef _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final PortRef s = factory.createPortRef();
      s.setPort(p);
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static PortRef pref(final Port p, final int lb, final int ub) {
    PortRef _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final PortRef s = factory.createPortRef();
      s.setRange(factory.createRange());
      Range _range = s.getRange();
      _range.setLb(lb);
      Range _range_1 = s.getRange();
      _range_1.setUb(ub);
      s.setPort(p);
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static NotExpr negpref(final Port p) {
    return FSMCustomFactory.not(FSMCustomFactory.pref(p));
  }
  
  public static AndExpr and(final BoolExpr a, final BoolExpr b) {
    AndExpr _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final AndExpr s = factory.createAndExpr();
      EList<BoolExpr> _args = s.getArgs();
      _args.add(a);
      EList<BoolExpr> _args_1 = s.getArgs();
      _args_1.add(b);
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static AndExpr and(final List<BoolExpr> list) {
    AndExpr _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final AndExpr s = factory.createAndExpr();
      for (final BoolExpr bexp : list) {
        boolean _notEquals = (!Objects.equal(bexp, null));
        if (_notEquals) {
          s.getArgs().add(bexp);
        }
      }
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static AndExpr and(final BoolExpr[] list) {
    AndExpr _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final AndExpr s = factory.createAndExpr();
      for (final BoolExpr bexp : list) {
        boolean _notEquals = (!Objects.equal(bexp, null));
        if (_notEquals) {
          s.getArgs().add(bexp);
        }
      }
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static OrExpr or(final BoolExpr a, final BoolExpr b) {
    OrExpr _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final OrExpr s = factory.createOrExpr();
      EList<BoolExpr> _args = s.getArgs();
      _args.add(a);
      EList<BoolExpr> _args_1 = s.getArgs();
      _args_1.add(b);
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static OrExpr or(final List<BoolExpr> list) {
    OrExpr _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final OrExpr s = factory.createOrExpr();
      for (final BoolExpr bexp : list) {
        boolean _notEquals = (!Objects.equal(bexp, null));
        if (_notEquals) {
          s.getArgs().add(bexp);
        }
      }
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static OrExpr or(final BoolExpr[] list) {
    OrExpr _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final OrExpr s = factory.createOrExpr();
      for (final BoolExpr bexp : list) {
        boolean _notEquals = (!Objects.equal(bexp, null));
        if (_notEquals) {
          s.getArgs().add(bexp);
        }
      }
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static NotExpr not(final BoolExpr args) {
    NotExpr _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final NotExpr s = factory.createNotExpr();
      EList<BoolExpr> _args = s.getArgs();
      _args.add(args);
      _xblockexpression = s;
    }
    return _xblockexpression;
  }
  
  public static Constant cst(final String v) {
    Constant _xblockexpression = null;
    {
      final FSMDSLFactory factory = FSMDSLFactory.eINSTANCE;
      final Constant p = factory.createConstant();
      p.setValue(v);
      _xblockexpression = p;
    }
    return _xblockexpression;
  }
  
  public static Constant cst(final boolean b) {
    Constant _xifexpression = null;
    if (b) {
      _xifexpression = FSMCustomFactory.cst("\"1\"");
    } else {
      _xifexpression = FSMCustomFactory.cst("\"0\"");
    }
    return _xifexpression;
  }
}
