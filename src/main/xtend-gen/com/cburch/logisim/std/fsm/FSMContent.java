/**
 * This file is part of logisim-evolution.
 * logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * logisim-evolution is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by :
 * + Haute École Spécialisée Bernoise
 * http://www.bfh.ch
 * + Haute École du paysage, d'ingénierie et d'architecture de Genève
 * http://hepia.hesge.ch/
 * + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 * http://www.heig-vd.ch/
 * The project is currently maintained by :
 * + REDS Institute - HEIG-VD
 * Yverdon-les-Bains, Switzerland
 * http://reds.heig-vd.ch
 */
package com.cburch.logisim.std.fsm;

import com.cburch.logisim.instance.Port;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.InputPort;
import com.cburch.logisim.statemachine.fSMDSL.OutputPort;
import com.cburch.logisim.statemachine.parser.FSMSerializer;
import com.cburch.logisim.std.fsm.FSMEntity;
import com.cburch.logisim.std.fsm.FSMModelListener;
import com.cburch.logisim.std.fsm.Strings;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;

@SuppressWarnings("all")
public class FSMContent implements Cloneable {
  protected static <T extends Object> T[] concat(final T[] first, final T[] second) {
    int _length = first.length;
    int _length_1 = second.length;
    int _plus = (_length + _length_1);
    T[] result = Arrays.<T>copyOf(first, _plus);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }
  
  protected EventSourceWeakSupport<FSMModelListener> listeners;
  
  private void init() {
    this.listeners = null;
  }
  
  public FSMContent(final String text) {
    HashMap<Port, InputPort> _hashMap = new HashMap<Port, InputPort>();
    this.inMap = _hashMap;
    HashMap<Port, OutputPort> _hashMap_1 = new HashMap<Port, OutputPort>();
    this.outMap = _hashMap_1;
    this.updateContent(text);
  }
  
  public void updateContent(final String txt) {
    this.init();
    this.parseContent(txt);
  }
  
  public FSM getFsm() {
    return this.fsm;
  }
  
  public void setFsm(final FSM fsm) {
    this.fsm = fsm;
  }
  
  @Override
  public FSMContent clone() {
    try {
      Object _clone = super.clone();
      FSMContent ret = ((FSMContent) _clone);
      ret.fsm = EcoreUtil.<FSM>copy(this.fsm);
      ret.listeners = null;
      return ret;
    } catch (final Throwable _t) {
      if (_t instanceof CloneNotSupportedException) {
        return this;
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  public void addFSMModelListener(final FSMModelListener l) {
    if ((this.listeners == null)) {
      EventSourceWeakSupport<FSMModelListener> _eventSourceWeakSupport = new EventSourceWeakSupport<FSMModelListener>();
      this.listeners = _eventSourceWeakSupport;
    }
    this.listeners.add(l);
  }
  
  protected void fireContentSet() {
    if ((this.listeners == null)) {
      return;
    }
    boolean found = false;
    for (final FSMModelListener l : this.listeners) {
      {
        found = true;
        l.contentSet(this);
      }
    }
    if ((!found)) {
      this.listeners = null;
    }
  }
  
  public void removeFSMModelListener(final FSMModelListener l) {
    if ((this.listeners == null)) {
      return;
    }
    this.listeners.remove(l);
    boolean _isEmpty = this.listeners.isEmpty();
    if (_isEmpty) {
      this.listeners = null;
    }
  }
  
  public static final String TEMPLATE = new Function0<String>() {
    public String apply() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("fsm example @[ 50 , 50 , 800 , 500 ] { ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("in A [ 3 ] @[ 50 , 100 , 44 , 15 ] ; ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("out X [ 4 ] @[ 807 , 140 , 43 , 15 ] ; ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("codeWidth = 2 ; ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("reset = S0 ; ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("state S0 = \"01\" @[ 297 , 181 , 30 , 30 ] { ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("commands @[ 246 , 173 , 50 , 40 ] { X = \"0001\" ; }");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("transitions { ");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("S0 -> S1 when default @[ 432 , 151 , 50 , 21 ] ; ");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("S0 -> S3 when A== \"000\" @[ 346 , 269 , 68 , 21 ] ; ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("} ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("state S1 = \"10\" @[ 470 , 186 , 30 , 30] { ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("commands @[ 522 , 190 , 40 , 40 ] { X = \"0010\" ; } ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("transitions { ");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("S1 -> S2 when default @[ 533 , 276 , 50 , 21 ] ; ");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("S1 -> S0 when A == \"000\" @[ 399 , 230 ,68 , 21 ] ; ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("} ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("state S2 = \"00\" @[ 471 , 339 , 30 , 30 ] { ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("commands @[ 524 ,353 , 60 , 40 ] { ");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("X = { \"00\" , A [ 1 ] , \"1\" } ;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("} ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("transitions { ");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("S2 -> S3 when default @[ 392 , 398 , 50 , 21 ] ; ");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("S2 -> S1 when A [ 2 : 1 ] == \"11\" @[ 557 ,250 , 90 , 21 ] ; ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("} ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("} ");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("state S3 = \"11\" @[ 287 , 325 , 30 , 30 ] { ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("commands @[244 , 341 , 60 , 40 ] { X = \"1000\" ; } ");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("transitions { ");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("S3 -> S0 when default @[248 , 278 , 50 , 21 ] ; ");
      _builder.newLine();
      _builder.append("\t\t\t");
      _builder.append("S3 -> S2 when A == \"000\" @[ 388 , 313 , 68 , 21 ] ;");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      return _builder.toString();
    }
  }.apply();
  
  static final int CLK = 0;
  
  static final int RST = 1;
  
  static final int EN = 2;
  
  protected Port[] inputs;
  
  protected Port[] outputs;
  
  protected Map<Port, InputPort> inMap;
  
  protected Map<Port, OutputPort> outMap;
  
  protected String name;
  
  private FSM fsm;
  
  private Port[] ctrl;
  
  public String getStringContent() {
    return FSMSerializer.saveAsString(this.fsm);
  }
  
  public Port[] getInputs() {
    if ((this.inputs == null)) {
      return new Port[0];
    }
    return this.inputs;
  }
  
  public int getInputsNumber() {
    if ((this.inputs == null)) {
      return 0;
    }
    return this.inputs.length;
  }
  
  public String getName() {
    if ((this.fsm == null)) {
      return "";
    }
    return this.fsm.getName();
  }
  
  public Port[] getOutputs() {
    if ((this.outputs == null)) {
      return new Port[0];
    }
    return this.outputs;
  }
  
  public int getOutputsNumber() {
    if ((this.outputs == null)) {
      return 0;
    }
    return this.outputs.length;
  }
  
  public Port[] getPorts() {
    return FSMContent.<Port>concat(this.ctrl, FSMContent.<Port>concat(this.inputs, this.outputs));
  }
  
  public Port[] getAllInPorts() {
    return FSMContent.<Port>concat(this.ctrl, this.inputs);
  }
  
  public int getPortsNumber() {
    if (((this.inputs == null) || (this.outputs == null))) {
      return 0;
    }
    int _length = this.ctrl.length;
    int _length_1 = this.inputs.length;
    int _plus = (_length + _length_1);
    int _length_2 = this.outputs.length;
    return (_plus + _length_2);
  }
  
  private boolean parseContent(final String content) {
    FSMSerializer parser = new FSMSerializer();
    try {
      FSM _load = FSMSerializer.load(content.toString());
      this.fsm = ((FSM) _load);
      this.name = this.fsm.getName();
      EList<com.cburch.logisim.statemachine.fSMDSL.Port> _in = this.fsm.getIn();
      List<InputPort> inputsDesc = ((List) _in);
      EList<com.cburch.logisim.statemachine.fSMDSL.Port> _out = this.fsm.getOut();
      List<OutputPort> outputsDesc = ((List) _out);
      this.ctrl = new Port[3];
      this.inputs = new Port[inputsDesc.size()];
      this.outputs = new Port[outputsDesc.size()];
      Port _port = new Port(0, FSMEntity.HEIGHT, Port.INPUT, 1);
      this.ctrl[FSMContent.CLK] = _port;
      Port _port_1 = new Port(0, (FSMEntity.HEIGHT + FSMEntity.PORT_GAP), Port.INPUT, 1);
      this.ctrl[FSMContent.RST] = _port_1;
      Port _port_2 = new Port(0, (FSMEntity.HEIGHT + (2 * FSMEntity.PORT_GAP)), Port.INPUT, 1);
      this.ctrl[FSMContent.EN] = _port_2;
      this.ctrl[FSMContent.CLK].setToolTip(Strings.getter("registerClkTip"));
      this.ctrl[FSMContent.RST].setToolTip(Strings.getter("registerRstTip"));
      this.ctrl[FSMContent.EN].setToolTip(Strings.getter("registerEnableTip"));
      this.inMap.clear();
      for (int i = 0; (i < inputsDesc.size()); i++) {
        {
          InputPort desc = inputsDesc.get(i);
          int _length = this.ctrl.length;
          int _plus = (i + _length);
          int _multiply = (_plus * FSMEntity.PORT_GAP);
          int _plus_1 = (_multiply + FSMEntity.HEIGHT);
          int _width = desc.getWidth();
          Port _port_3 = new Port(0, _plus_1, Port.INPUT, _width);
          this.inputs[i] = _port_3;
          this.inputs[i].setToolTip(Strings.getter(desc.getName()));
          this.inMap.put(this.inputs[i], desc);
        }
      }
      this.outMap.clear();
      for (int i = 0; (i < outputsDesc.size()); i++) {
        {
          OutputPort desc = outputsDesc.get(i);
          {
            final Port[] _wrVal_outputs = this.outputs;
            final int _wrIndx_outputs = i;
            int _length = this.ctrl.length;
            int _plus = (i + _length);
            int _multiply = (_plus * FSMEntity.PORT_GAP);
            int _plus_1 = (_multiply + FSMEntity.HEIGHT);
            int _width = desc.getWidth();
            Port _port_3 = new Port(FSMEntity.WIDTH, _plus_1, 
              Port.OUTPUT, _width);
            _wrVal_outputs[_wrIndx_outputs] = _port_3;
          }
          Port _xblockexpression = null;
          {
            final int _rdIndx_outputs = i;
            _xblockexpression = this.outputs[_rdIndx_outputs];
          }
          _xblockexpression.setToolTip(Strings.getter(desc.getName()));
          Port _xblockexpression_1 = null;
          {
            final int _rdIndx_outputs = i;
            _xblockexpression_1 = this.outputs[_rdIndx_outputs];
          }
          this.outMap.put(_xblockexpression_1, desc);
        }
      }
      this.fireContentSet();
      return true;
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception ex = (Exception)_t;
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, ex.getMessage(), Strings.get("validationParseError"), 
          JOptionPane.ERROR_MESSAGE);
        return false;
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  public Port[] getControls() {
    return this.ctrl;
  }
}
