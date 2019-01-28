package com.cburch.logisim.statemachine.analysis;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IntegerRange;
import org.eclipse.xtext.xbase.lib.ListExtensions;

@SuppressWarnings("all")
public class InputVectorGenerator {
  private List<Integer> current = new ArrayList<Integer>();
  
  private List<Integer> widths = new ArrayList<Integer>();
  
  private static final boolean QUOTE = false;
  
  public InputVectorGenerator(final FSM fsm) {
    final Function1<Port, Integer> _function = (Port p) -> {
      return Integer.valueOf(p.getWidth());
    };
    List<Integer> _map = ListExtensions.<Port, Integer>map(fsm.getIn(), _function);
    ArrayList<Integer> wl = new ArrayList<Integer>(_map);
    int _width = fsm.getWidth();
    wl.add(Integer.valueOf(_width));
    for (final Integer w : wl) {
      {
        this.current.add(Integer.valueOf(0));
        this.widths.add(w);
      }
    }
  }
  
  public int getSize() {
    return this.current.size();
  }
  
  public Integer getValue(final int i) {
    return this.current.get(i);
  }
  
  public String getQuotedBinaryValue(final int i) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("\"");
    String _binaryValue = this.getBinaryValue(i);
    _builder.append(_binaryValue);
    _builder.append("\"");
    return _builder.toString();
  }
  
  public String getBinaryValue(final int i) {
    String _xblockexpression = null;
    {
      final Integer v = this.current.get(i);
      final Integer w = this.widths.get(i);
      final String str = Integer.toBinaryString((v).intValue());
      _xblockexpression = String.format((("%" + w) + "s"), str).replace(" ", "0");
    }
    return _xblockexpression;
  }
  
  public boolean inc() {
    int _size = this.current.size();
    int _minus = (_size - 1);
    return this.inc(_minus);
  }
  
  private boolean inc(final int pos) {
    boolean _xifexpression = false;
    if ((pos >= 0)) {
      boolean _xblockexpression = false;
      {
        Integer _get = this.current.get(pos);
        int newVal = ((_get).intValue() + 1);
        Integer _get_1 = this.widths.get(pos);
        int _doubleLessThan = (1 << (_get_1).intValue());
        int _modulo = (newVal % _doubleLessThan);
        newVal = _modulo;
        this.current.set(pos, Integer.valueOf(newVal));
        if ((newVal == 0)) {
          return this.inc((pos - 1));
        }
        _xblockexpression = true;
      }
      _xifexpression = _xblockexpression;
    } else {
      _xifexpression = false;
    }
    return _xifexpression;
  }
  
  @Override
  public String toString() {
    String _xblockexpression = null;
    {
      String res = "";
      int _size = this.current.size();
      int _minus = (_size - 1);
      IntegerRange _upTo = new IntegerRange(0, _minus);
      for (final Integer i : _upTo) {
        {
          final Value v = Value.createKnown(BitWidth.create((this.widths.get((i).intValue())).intValue()), (this.current.get((i).intValue())).intValue());
          if (InputVectorGenerator.QUOTE) {
            if (((i).intValue() > 0)) {
              String _res = res;
              res = (_res + ";");
            }
            String _res_1 = res;
            String _binaryString = v.toBinaryString();
            String _plus = ("\"" + _binaryString);
            String _plus_1 = (_plus + "\"");
            res = (_res_1 + _plus_1);
          } else {
            String _res_2 = res;
            String _binaryString_1 = v.toBinaryString();
            res = (_res_2 + _binaryString_1);
          }
        }
      }
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
}
