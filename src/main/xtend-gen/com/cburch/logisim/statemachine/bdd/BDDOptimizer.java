package com.cburch.logisim.statemachine.bdd;

import com.cburch.logisim.statemachine.PrettyPrinter;
import com.cburch.logisim.statemachine.bdd.BDDDotExport;
import com.cburch.logisim.statemachine.bdd.BDDVariableMapping;
import com.cburch.logisim.statemachine.bdd.CollectFlags;
import com.cburch.logisim.statemachine.bdd.RemoveBitVectors;
import com.cburch.logisim.statemachine.editor.view.FSMCustomFactory;
import com.cburch.logisim.statemachine.fSMDSL.AndExpr;
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.ConcatExpr;
import com.cburch.logisim.statemachine.fSMDSL.Constant;
import com.cburch.logisim.statemachine.fSMDSL.InputPort;
import com.cburch.logisim.statemachine.fSMDSL.NotExpr;
import com.cburch.logisim.statemachine.fSMDSL.OrExpr;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.fSMDSL.PortRef;
import com.cburch.logisim.statemachine.fSMDSL.Range;
import com.cburch.logisim.statemachine.fSMDSL.util.FSMDSLSwitch;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jdd.bdd.BDD;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;

/**
 * implement with switch visit ... !!!
 */
@SuppressWarnings("all")
public class BDDOptimizer extends FSMDSLSwitch<Integer> {
  private static final String ZERO = "\"0\"";
  
  private static final String ONE = "\"1\"";
  
  private static final boolean VERBOSE = true;
  
  EList<InputPort> in;
  
  private BDD bdd;
  
  private Integer root;
  
  private BDDVariableMapping map;
  
  List<BoolExpr> list;
  
  private void debug(final String str) {
    if (BDDOptimizer.VERBOSE) {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append(str);
      System.out.println(_builder);
    }
  }
  
  public BDDOptimizer(final BoolExpr bexp) {
    BoolExpr copy = EcoreUtil.<BoolExpr>copy(bexp);
    copy = new RemoveBitVectors().replace(copy);
    this.in = new CollectFlags().collect(copy);
    int size = 0;
    for (final InputPort ip : this.in) {
      int _size = size;
      int _width = ip.getWidth();
      int _multiply = (size * _width);
      size = (_size + _multiply);
    }
    int bddsize = (size * size);
    BDD _bDD = new BDD(bddsize);
    this.bdd = _bDD;
    BDDVariableMapping _bDDVariableMapping = new BDDVariableMapping(this.bdd);
    this.map = _bDDVariableMapping;
    for (final InputPort icp : this.in) {
      this.map.map(icp);
    }
    this.root = this.doSwitch(copy);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("Root is ");
    _builder.append(this.root);
    this.debug(_builder.toString());
  }
  
  @Override
  public Integer caseAndExpr(final AndExpr object) {
    int varBDDAnd = 0;
    boolean first = true;
    EList<BoolExpr> _args = object.getArgs();
    for (final BoolExpr bexp : _args) {
      {
        this.doSwitch(bexp);
        if (first) {
          first = false;
          varBDDAnd = this.map.getBDDVarFor(bexp);
        } else {
          int old = varBDDAnd;
          varBDDAnd = this.bdd.and(varBDDAnd, this.map.getBDDVarFor(bexp));
          StringConcatenation _builder = new StringConcatenation();
          _builder.append("New node id=");
          _builder.append(varBDDAnd);
          _builder.append("=and(");
          _builder.append(old);
          _builder.append(",");
          int _bDDVarFor = this.map.getBDDVarFor(bexp);
          _builder.append(_bDDVarFor);
          _builder.append(")");
          this.debug(_builder.toString());
        }
      }
    }
    this.map.map(object, varBDDAnd);
    return Integer.valueOf(varBDDAnd);
  }
  
  @Override
  public Integer caseConcatExpr(final ConcatExpr object) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("BDD analysis dos not support operator ");
    String _pp = PrettyPrinter.pp(object);
    _builder.append(_pp);
    throw new UnsupportedOperationException(_builder.toString());
  }
  
  @Override
  public Integer caseConstant(final Constant object) {
    String value = object.getValue();
    boolean _equals = value.equals(BDDOptimizer.ONE);
    if (_equals) {
      this.map.map(object, this.bdd.getOne());
      return Integer.valueOf(this.bdd.getOne());
    } else {
      boolean _equals_1 = value.equals(BDDOptimizer.ZERO);
      if (_equals_1) {
        this.map.map(object, this.bdd.getZero());
        return Integer.valueOf(this.bdd.getZero());
      } else {
        StringConcatenation _builder = new StringConcatenation();
        _builder.append("BDD analysis does not support unkown constant value ");
        _builder.append(value);
        throw new UnsupportedOperationException(_builder.toString());
      }
    }
  }
  
  @Override
  public Integer casePortRef(final PortRef pref) {
    Port _port = pref.getPort();
    InputPort icp = ((InputPort) _port);
    boolean _contains = this.in.contains(icp);
    boolean _not = (!_contains);
    if (_not) {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Inconsistency in ");
      String _pp = PrettyPrinter.pp(pref);
      _builder.append(_pp);
      throw new RuntimeException(_builder.toString());
    } else {
      int width = icp.getWidth();
      if ((width > 1)) {
        Range range = pref.getRange();
        if ((range != null)) {
          int lb = range.getLb();
          int ub = range.getUb();
          if ((((lb == ub) || (ub == (-1))) && (lb < width))) {
            int varProduct = this.map.getBDDVarFor(icp, lb);
            this.map.map(pref, varProduct);
            return Integer.valueOf(varProduct);
          }
        }
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("BDD analysis dos not support bitvector port references ");
        String _pp_1 = PrettyPrinter.pp(pref);
        _builder_1.append(_pp_1);
        throw new RuntimeException(_builder_1.toString());
      } else {
        int varProduct_1 = this.map.getBDDVarFor(icp, 0);
        this.map.map(pref, varProduct_1);
        return Integer.valueOf(varProduct_1);
      }
    }
  }
  
  private boolean isBitSetAt(final int value, final int i) {
    return (((value >> i) & 0x1) == 0x1);
  }
  
  @Override
  public Integer caseOrExpr(final OrExpr object) {
    int orBDDExression = 0;
    boolean first = true;
    EList<BoolExpr> _args = object.getArgs();
    for (final BoolExpr bexp : _args) {
      {
        this.doSwitch(bexp);
        if (first) {
          first = false;
          orBDDExression = this.map.getBDDVarFor(bexp);
        } else {
          int old = orBDDExression;
          orBDDExression = this.bdd.or(orBDDExression, this.map.getBDDVarFor(bexp));
          StringConcatenation _builder = new StringConcatenation();
          _builder.append("New node id=");
          _builder.append(orBDDExression);
          _builder.append("=or(");
          _builder.append(old);
          _builder.append(",");
          int _bDDVarFor = this.map.getBDDVarFor(bexp);
          _builder.append(_bDDVarFor);
          _builder.append(")");
          this.debug(_builder.toString());
        }
      }
    }
    this.map.map(object, orBDDExression);
    return Integer.valueOf(orBDDExression);
  }
  
  @Override
  public Integer caseNotExpr(final NotExpr object) {
    int notBDDExpr0 = 0;
    BoolExpr bexp = object.getArgs().get(0);
    this.doSwitch(bexp);
    notBDDExpr0 = this.bdd.not(this.map.getBDDVarFor(bexp));
    this.map.map(object, notBDDExpr0);
    return Integer.valueOf(notBDDExpr0);
  }
  
  public BoolExpr simplify() {
    ArrayList<BoolExpr> _arrayList = new ArrayList<BoolExpr>();
    this.list = _arrayList;
    this.rebuildPredicateFromSimplifiedBDD(this.bdd, (this.root).intValue(), null);
    int _size = this.list.size();
    boolean _tripleEquals = (_size == 0);
    if (_tripleEquals) {
      this.debug("No solution -> always false");
      return FSMCustomFactory.cst(false);
    }
    OrExpr orExp = FSMCustomFactory.or(this.list);
    return orExp;
  }
  
  public boolean isAlwaysTrue() {
    return ((this.root).intValue() == 1);
  }
  
  public boolean isAlwaysFalse() {
    return ((this.root).intValue() == 0);
  }
  
  /**
   * Translates the simplified BDD boolean expression into a FSM predicates
   */
  private void rebuildPredicateFromSimplifiedBDD(final BDD bdd, final int root, final BoolExpr current_finalParam_) {
    BoolExpr current = current_finalParam_;
    BDDDotExport dot = null;
    try {
      BDDDotExport _bDDDotExport = new BDDDotExport("bdd.dot", bdd, this.map);
      dot = _bDDDotExport;
      dot.save(root);
    } catch (final Throwable _t) {
      if (_t instanceof FileNotFoundException) {
        final FileNotFoundException e = (FileNotFoundException)_t;
        e.printStackTrace();
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    if ((root >= 2)) {
      int bddInputVar = bdd.getVar(root);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Root=");
      _builder.append(root);
      _builder.append(" var=");
      _builder.append(bddInputVar);
      _builder.append(" high=");
      int _high = bdd.getHigh(root);
      _builder.append(_high);
      _builder.append(" low=");
      int _low = bdd.getLow(root);
      _builder.append(_low);
      this.debug(_builder.toString());
      Map.Entry<InputPort, Integer> entry = this.map.getICPortForBDDvar(bddInputVar);
      InputPort port = entry.getKey();
      PortRef t = null;
      BoolExpr nT = null;
      int _width = port.getWidth();
      boolean _tripleEquals = (_width == 1);
      if (_tripleEquals) {
        t = FSMCustomFactory.pref(port);
        nT = FSMCustomFactory.not(FSMCustomFactory.pref(port));
      } else {
        int _width_1 = port.getWidth();
        boolean _greaterThan = (_width_1 > 1);
        if (_greaterThan) {
          t = FSMCustomFactory.pref(port, (entry.getValue()).intValue(), (entry.getValue()).intValue());
          nT = FSMCustomFactory.not(FSMCustomFactory.pref(port, (entry.getValue()).intValue(), (entry.getValue()).intValue()));
        } else {
          StringConcatenation _builder_1 = new StringConcatenation();
          _builder_1.append("Illegal port width ");
          int _width_2 = port.getWidth();
          _builder_1.append(_width_2);
          _builder_1.append(" for port ");
          String _name = port.getName();
          _builder_1.append(_name);
          throw new RuntimeException(_builder_1.toString());
        }
      }
      if ((current != null)) {
        BoolExpr _copy = EcoreUtil.<BoolExpr>copy(current);
        BoolExpr ResHigh = ((BoolExpr) _copy);
        this.rebuildPredicateFromSimplifiedBDD(bdd, bdd.getHigh(root), FSMCustomFactory.and(t, ResHigh));
      } else {
        this.rebuildPredicateFromSimplifiedBDD(bdd, bdd.getHigh(root), t);
      }
      if ((current != null)) {
        BoolExpr _copy_1 = EcoreUtil.<BoolExpr>copy(current);
        BoolExpr ResLow = ((BoolExpr) _copy_1);
        this.rebuildPredicateFromSimplifiedBDD(bdd, bdd.getLow(root), FSMCustomFactory.and(nT, ResLow));
      } else {
        this.rebuildPredicateFromSimplifiedBDD(bdd, bdd.getLow(root), nT);
      }
    } else {
      if ((root == 1)) {
        if ((current == null)) {
          current = FSMCustomFactory.cst(true);
        }
        this.list.add(current);
        StringConcatenation _builder_2 = new StringConcatenation();
        _builder_2.append("Adding ");
        _builder_2.append(current);
        _builder_2.append(" to predicate");
        this.debug(_builder_2.toString());
      } else {
        if ((root == 0)) {
          StringConcatenation _builder_3 = new StringConcatenation();
          _builder_3.append("Adding ");
          _builder_3.append(current);
          _builder_3.append(" to predicate");
          this.debug(_builder_3.toString());
        }
      }
    }
  }
}
