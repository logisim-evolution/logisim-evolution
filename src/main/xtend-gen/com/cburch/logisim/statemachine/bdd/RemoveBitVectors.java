package com.cburch.logisim.statemachine.bdd;

import com.cburch.logisim.statemachine.PrettyPrinter;
import com.cburch.logisim.statemachine.bdd.BitWidthAnalyzer;
import com.cburch.logisim.statemachine.fSMDSL.AndExpr;
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.CmpExpr;
import com.cburch.logisim.statemachine.fSMDSL.ConcatExpr;
import com.cburch.logisim.statemachine.fSMDSL.ConstRef;
import com.cburch.logisim.statemachine.fSMDSL.Constant;
import com.cburch.logisim.statemachine.fSMDSL.FSMDSLFactory;
import com.cburch.logisim.statemachine.fSMDSL.NotExpr;
import com.cburch.logisim.statemachine.fSMDSL.OrExpr;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.fSMDSL.PortRef;
import com.cburch.logisim.statemachine.fSMDSL.Range;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.IteratorExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;

@SuppressWarnings("all")
public class RemoveBitVectors {
  private static final boolean VERBOSE = true;
  
  private BitWidthAnalyzer analyzer = new BitWidthAnalyzer();
  
  public RemoveBitVectors() {
  }
  
  protected BoolExpr _replace(final BoolExpr e) {
    BoolExpr _xblockexpression = null;
    {
      InputOutput.<String>print((("replace " + e) + " by "));
      final List<CmpExpr> list = IteratorExtensions.<CmpExpr>toList(Iterators.<CmpExpr>filter(EcoreUtil.<Object>getAllContents(e, false), CmpExpr.class));
      for (final CmpExpr n : list) {
        {
          InputOutput.<String>println("");
          EcoreUtil.replace(n, this.slice(n));
        }
      }
      String _pp = PrettyPrinter.pp(e);
      String _plus = (" " + _pp);
      InputOutput.<String>println(_plus);
      _xblockexpression = e;
    }
    return _xblockexpression;
  }
  
  protected BoolExpr _replace(final CmpExpr e) {
    BoolExpr _xblockexpression = null;
    {
      String _pp = PrettyPrinter.pp(e);
      String _plus = ("replace " + _pp);
      String _plus_1 = (_plus + " by ");
      InputOutput.<String>print(_plus_1);
      final List<CmpExpr> list = IteratorExtensions.<CmpExpr>toList(Iterators.<CmpExpr>filter(EcoreUtil.<Object>getAllContents(e, false), CmpExpr.class));
      for (final CmpExpr n : list) {
        {
          InputOutput.<String>println("Replace ");
          EcoreUtil.replace(n, this.slice(n));
        }
      }
      final BoolExpr res = this.slice(e);
      String _pp_1 = PrettyPrinter.pp(res);
      String _plus_2 = (" " + _pp_1);
      InputOutput.<String>println(_plus_2);
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
  
  protected BoolExpr _slice(final BoolExpr e, final int offset) {
    throw new RuntimeException(("Cannot bitslice expression " + e));
  }
  
  public BoolExpr and(final BoolExpr a, final BoolExpr b) {
    AndExpr _xblockexpression = null;
    {
      AndExpr and = FSMDSLFactory.eINSTANCE.createAndExpr();
      EList<BoolExpr> _args = and.getArgs();
      Iterables.<BoolExpr>addAll(_args, Collections.<BoolExpr>unmodifiableSet(CollectionLiterals.<BoolExpr>newHashSet(a, b)));
      _xblockexpression = and;
    }
    return _xblockexpression;
  }
  
  public BoolExpr or(final BoolExpr a, final BoolExpr b) {
    OrExpr _xblockexpression = null;
    {
      OrExpr or = FSMDSLFactory.eINSTANCE.createOrExpr();
      EList<BoolExpr> _args = or.getArgs();
      Iterables.<BoolExpr>addAll(_args, Collections.<BoolExpr>unmodifiableSet(CollectionLiterals.<BoolExpr>newHashSet(a, b)));
      _xblockexpression = or;
    }
    return _xblockexpression;
  }
  
  public BoolExpr not(final BoolExpr a) {
    NotExpr _xblockexpression = null;
    {
      NotExpr not = FSMDSLFactory.eINSTANCE.createNotExpr();
      EList<BoolExpr> _args = not.getArgs();
      _args.add(a);
      _xblockexpression = not;
    }
    return _xblockexpression;
  }
  
  public BoolExpr equ(final BoolExpr a, final BoolExpr b) {
    return this.or(this.and(EcoreUtil.<BoolExpr>copy(a), EcoreUtil.<BoolExpr>copy(b)), this.not(this.or(EcoreUtil.<BoolExpr>copy(a), EcoreUtil.<BoolExpr>copy(b))));
  }
  
  public BoolExpr nequ(final BoolExpr a, final BoolExpr b) {
    return this.or(this.and(this.not(EcoreUtil.<BoolExpr>copy(a)), EcoreUtil.<BoolExpr>copy(b)), this.and(EcoreUtil.<BoolExpr>copy(a), this.not(EcoreUtil.<BoolExpr>copy(b))));
  }
  
  protected BoolExpr _slice(final CmpExpr e) {
    AndExpr _xblockexpression = null;
    {
      AndExpr and = FSMDSLFactory.eINSTANCE.createAndExpr();
      boolean canDoIt = true;
      int i = 0;
      this.analyzer.computeBitwidth(e);
      final Integer bw = this.analyzer.getBitwidth(e);
      for (i = 0; (i < (bw).intValue()); i++) {
        {
          BoolExpr slice = null;
          final BoolExpr left = this.slice(e.getArgs().get(0), i);
          final BoolExpr right = this.slice(e.getArgs().get(1), i);
          String _op = e.getOp();
          if (_op != null) {
            switch (_op) {
              case "==":
                slice = this.equ(left, right);
                break;
              case "/=":
                slice = this.nequ(left, right);
                break;
              default:
                String _op_1 = e.getOp();
                String _plus = ("Invalid compare operator " + _op_1);
                String _plus_1 = (_plus + " only ==,/= allowed");
                throw new UnsupportedOperationException(_plus_1);
            }
          } else {
            String _op_1 = e.getOp();
            String _plus = ("Invalid compare operator " + _op_1);
            String _plus_1 = (_plus + " only ==,/= allowed");
            throw new UnsupportedOperationException(_plus_1);
          }
          boolean _notEquals = (!Objects.equal(slice, null));
          if (_notEquals) {
            and.getArgs().add(slice);
          }
        }
      }
      _xblockexpression = and;
    }
    return _xblockexpression;
  }
  
  protected BoolExpr _slice(final AndExpr e, final int offset) {
    AndExpr _xblockexpression = null;
    {
      AndExpr and = FSMDSLFactory.eINSTANCE.createAndExpr();
      final Function1<BoolExpr, BoolExpr> _function = (BoolExpr arg) -> {
        return this.slice(arg, offset);
      };
      and.getArgs().addAll(ListExtensions.<BoolExpr, BoolExpr>map(e.getArgs(), _function));
      _xblockexpression = and;
    }
    return _xblockexpression;
  }
  
  protected BoolExpr _slice(final ConcatExpr e, final int offset) {
    this.analyzer.computeBitwidth(e);
    int current = offset;
    EList<BoolExpr> _args = e.getArgs();
    for (final BoolExpr arg : _args) {
      {
        final Integer width = this.analyzer.getBitwidth(arg);
        if ((current < (width).intValue())) {
          return this.slice(arg, offset);
        }
        current = (current - (width).intValue());
      }
    }
    throw new IndexOutOfBoundsException(((("Offset " + Integer.valueOf(offset)) + " is out of bound w.r.t to expression ") + e));
  }
  
  protected BoolExpr _slice(final OrExpr e, final int offset) {
    OrExpr _xblockexpression = null;
    {
      OrExpr or = FSMDSLFactory.eINSTANCE.createOrExpr();
      final Function1<BoolExpr, BoolExpr> _function = (BoolExpr arg) -> {
        return this.slice(arg, offset);
      };
      or.getArgs().addAll(ListExtensions.<BoolExpr, BoolExpr>map(e.getArgs(), _function));
      _xblockexpression = or;
    }
    return _xblockexpression;
  }
  
  protected BoolExpr _slice(final NotExpr e, final int offset) {
    NotExpr _xblockexpression = null;
    {
      NotExpr not = FSMDSLFactory.eINSTANCE.createNotExpr();
      final Function1<BoolExpr, BoolExpr> _function = (BoolExpr arg) -> {
        return this.slice(arg, offset);
      };
      not.getArgs().addAll(ListExtensions.<BoolExpr, BoolExpr>map(e.getArgs(), _function));
      _xblockexpression = not;
    }
    return _xblockexpression;
  }
  
  protected BoolExpr _slice(final Constant e, final int offset) {
    Constant _xifexpression = null;
    int _length = e.getValue().length();
    int _minus = (_length - 2);
    boolean _lessEqualsThan = (offset <= _minus);
    if (_lessEqualsThan) {
      Constant _xblockexpression = null;
      {
        Constant c = FSMDSLFactory.eINSTANCE.createConstant();
        StringConcatenation _builder = new StringConcatenation();
        _builder.append("\"");
        char _charAt = e.getValue().charAt((offset + 1));
        _builder.append(_charAt);
        _builder.append("\"");
        c.setValue(_builder.toString());
        _xblockexpression = c;
      }
      _xifexpression = _xblockexpression;
    } else {
      throw new IndexOutOfBoundsException(((("Offset " + Integer.valueOf(offset)) + " is out of bound w.r.t to expression ") + e));
    }
    return _xifexpression;
  }
  
  protected BoolExpr _slice(final ConstRef e, final int offset) {
    return this.slice(e.getConst().getValue(), offset);
  }
  
  protected BoolExpr _slice(final PortRef e, final int offset) {
    PortRef _xifexpression = null;
    Range _range = e.getRange();
    boolean _notEquals = (!Objects.equal(_range, null));
    if (_notEquals) {
      PortRef _xblockexpression = null;
      {
        if (((e.getRange().getUb() != (-1)) && ((offset + e.getRange().getLb()) > e.getRange().getUb()))) {
          Port _port = e.getPort();
          String _plus = ((("Offset " + Integer.valueOf(offset)) + " is out of bound w.r.t to port ") + _port);
          throw new IndexOutOfBoundsException(_plus);
        }
        PortRef pref = FSMDSLFactory.eINSTANCE.createPortRef();
        pref.setPort(e.getPort());
        pref.setRange(FSMDSLFactory.eINSTANCE.createRange());
        Range _range_1 = pref.getRange();
        int _lb = e.getRange().getLb();
        int _plus_1 = (offset + _lb);
        _range_1.setLb(_plus_1);
        Range _range_2 = pref.getRange();
        int _lb_1 = e.getRange().getLb();
        int _plus_2 = (offset + _lb_1);
        _range_2.setUb(_plus_2);
        _xblockexpression = pref;
      }
      _xifexpression = _xblockexpression;
    } else {
      PortRef _xblockexpression_1 = null;
      {
        int _width = e.getPort().getWidth();
        boolean _greaterEqualsThan = (offset >= _width);
        if (_greaterEqualsThan) {
          Port _port = e.getPort();
          String _plus = ((("Offset " + Integer.valueOf(offset)) + " is out of bound w.r.t to port ") + _port);
          throw new IndexOutOfBoundsException(_plus);
        }
        PortRef pref = FSMDSLFactory.eINSTANCE.createPortRef();
        pref.setPort(e.getPort());
        pref.setRange(FSMDSLFactory.eINSTANCE.createRange());
        Range _range_1 = pref.getRange();
        _range_1.setLb(offset);
        Range _range_2 = pref.getRange();
        _range_2.setUb(offset);
        _xblockexpression_1 = pref;
      }
      _xifexpression = _xblockexpression_1;
    }
    return _xifexpression;
  }
  
  public BoolExpr replace(final BoolExpr e) {
    if (e instanceof CmpExpr) {
      return _replace((CmpExpr)e);
    } else if (e != null) {
      return _replace(e);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(e).toString());
    }
  }
  
  public BoolExpr slice(final BoolExpr e, final int offset) {
    if (e instanceof AndExpr) {
      return _slice((AndExpr)e, offset);
    } else if (e instanceof ConcatExpr) {
      return _slice((ConcatExpr)e, offset);
    } else if (e instanceof ConstRef) {
      return _slice((ConstRef)e, offset);
    } else if (e instanceof Constant) {
      return _slice((Constant)e, offset);
    } else if (e instanceof NotExpr) {
      return _slice((NotExpr)e, offset);
    } else if (e instanceof OrExpr) {
      return _slice((OrExpr)e, offset);
    } else if (e instanceof PortRef) {
      return _slice((PortRef)e, offset);
    } else if (e != null) {
      return _slice(e, offset);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(e, offset).toString());
    }
  }
  
  public BoolExpr slice(final CmpExpr e) {
    return _slice(e);
  }
}
