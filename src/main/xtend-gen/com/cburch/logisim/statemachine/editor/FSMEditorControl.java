package com.cburch.logisim.statemachine.editor;

import com.cburch.logisim.statemachine.editor.FSMEditorController;
import com.cburch.logisim.statemachine.editor.SelectionZone;
import com.cburch.logisim.statemachine.fSMDSL.FSMElement;
import com.google.common.base.Objects;
import java.awt.Point;
import java.io.Serializable;
import org.eclipse.xtext.xbase.lib.InputOutput;

@SuppressWarnings("all")
public class FSMEditorControl {
  public enum Event {
    LEFT_PRESS,
    
    LEFT_CLICK,
    
    LEFT_DCLICK,
    
    LEFT_DRAGGED,
    
    LEFT_RELEASE,
    
    RIGHT_PRESS,
    
    MOUSE_MOVE,
    
    RIGHT_CLICK,
    
    RIGHT_RELEASE;
  }
  
  public enum CtrlState {
    IDLE,
    
    SELECT_ZONE,
    
    EXTEND_SEL_ZONE,
    
    SELECT_ELT,
    
    MOVE_ELT,
    
    SELECT_DST;
  }
  
  public FSMEditorControl.CtrlState state = FSMEditorControl.CtrlState.IDLE;
  
  public static final boolean DEBUG = true;
  
  private SelectionZone zone = new SelectionZone();
  
  public FSMEditorControl(final FSMEditorController ctrl) {
  }
  
  public void configureElement(final FSMElement element) {
    throw new UnsupportedOperationException("TODO: auto-generated method stub");
  }
  
  public void updateNewTransitionDst(final Point point) {
    throw new UnsupportedOperationException("TODO: auto-generated method stub");
  }
  
  public void cancelNewTransition() {
    throw new UnsupportedOperationException("TODO: auto-generated method stub");
  }
  
  public void finalizeNewTransition(final Point point) {
    throw new UnsupportedOperationException("TODO: auto-generated method stub");
  }
  
  public FSMElement getObjectAt(final Point point) {
    throw new UnsupportedOperationException("TODO: auto-generated method stub");
  }
  
  public void moveSelection(final Point point) {
    throw new UnsupportedOperationException("TODO: auto-generated method stub");
  }
  
  public String debug(final FSMEditorControl.CtrlState state, final String string) {
    String _xifexpression = null;
    if (FSMEditorControl.DEBUG) {
      String _string = state.toString();
      String _plus = (_string + string);
      _xifexpression = InputOutput.<String>println(_plus);
    }
    return _xifexpression;
  }
  
  public void showContextMenu() {
    throw new UnsupportedOperationException("TODO: auto-generated method stub");
  }
  
  public Serializable handleEvent(final FSMEditorControl.Event e, final Point p) {
    Serializable _switchResult = null;
    final FSMEditorControl.CtrlState state = this.state;
    if (state != null) {
      switch (state) {
        case IDLE:
          FSMEditorControl.CtrlState _switchResult_1 = null;
          if (e != null) {
            switch (e) {
              case LEFT_DCLICK:
                final FSMElement target = this.getObjectAt(p);
                if ((target != null)) {
                  this.configureElement(target);
                }
                break;
              case RIGHT_CLICK:
                this.debug(this.state, "show context menu");
                this.showContextMenu();
                break;
              case RIGHT_PRESS:
                FSMEditorControl.CtrlState _xblockexpression = null;
                {
                  this.zone.start(p);
                  FSMEditorControl.CtrlState _xifexpression = null;
                  FSMElement _objectAt = this.getObjectAt(p);
                  boolean _equals = Objects.equal(_objectAt, null);
                  if (_equals) {
                    _xifexpression = this.state = FSMEditorControl.CtrlState.MOVE_ELT;
                  } else {
                    _xifexpression = this.state = FSMEditorControl.CtrlState.SELECT_ZONE;
                  }
                  _xblockexpression = _xifexpression;
                }
                _switchResult_1 = _xblockexpression;
                break;
              default:
                break;
            }
          }
          _switchResult = _switchResult_1;
          break;
        case SELECT_ZONE:
          Serializable _switchResult_2 = null;
          if (e != null) {
            switch (e) {
              case RIGHT_CLICK:
                _switchResult_2 = this.state = FSMEditorControl.CtrlState.IDLE;
                break;
              case LEFT_RELEASE:
                _switchResult_2 = this.state = FSMEditorControl.CtrlState.IDLE;
                break;
              case LEFT_DRAGGED:
                _switchResult_2 = this.zone.extend(p);
                break;
              default:
                break;
            }
          }
          _switchResult = _switchResult_2;
          break;
        case MOVE_ELT:
          FSMEditorControl.CtrlState _switchResult_3 = null;
          if (e != null) {
            switch (e) {
              case LEFT_DRAGGED:
                this.moveSelection(p);
                break;
              default:
                _switchResult_3 = this.state = FSMEditorControl.CtrlState.IDLE;
                break;
            }
          } else {
            _switchResult_3 = this.state = FSMEditorControl.CtrlState.IDLE;
          }
          _switchResult = _switchResult_3;
          break;
        case SELECT_DST:
          FSMEditorControl.CtrlState _switchResult_4 = null;
          if (e != null) {
            switch (e) {
              case LEFT_RELEASE:
                FSMEditorControl.CtrlState _xblockexpression_1 = null;
                {
                  this.finalizeNewTransition(p);
                  _xblockexpression_1 = this.state = FSMEditorControl.CtrlState.IDLE;
                }
                _switchResult_4 = _xblockexpression_1;
                break;
              case MOUSE_MOVE:
                this.updateNewTransitionDst(p);
                break;
              default:
                FSMEditorControl.CtrlState _xblockexpression_2 = null;
                {
                  this.cancelNewTransition();
                  _xblockexpression_2 = this.state = FSMEditorControl.CtrlState.IDLE;
                }
                _switchResult_4 = _xblockexpression_2;
                break;
            }
          } else {
            FSMEditorControl.CtrlState _xblockexpression_2 = null;
            {
              this.cancelNewTransition();
              _xblockexpression_2 = this.state = FSMEditorControl.CtrlState.IDLE;
            }
            _switchResult_4 = _xblockexpression_2;
          }
          _switchResult = _switchResult_4;
          break;
        default:
          throw new UnsupportedOperationException("Unsupported case");
      }
    } else {
      throw new UnsupportedOperationException("Unsupported case");
    }
    return _switchResult;
  }
}
