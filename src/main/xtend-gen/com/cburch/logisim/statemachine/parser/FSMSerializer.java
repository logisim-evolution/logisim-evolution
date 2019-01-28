package com.cburch.logisim.statemachine.parser;

import com.cburch.logisim.statemachine.FSMDSLStandaloneSetup;
import com.cburch.logisim.statemachine.PrettyPrinter;
import com.cburch.logisim.statemachine.fSMDSL.ConstantDef;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.FSMDSLPackage;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.parser.FSMTextSave;
import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import javax.swing.JOptionPane;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;

@SuppressWarnings("all")
public class FSMSerializer {
  public static String saveAsString(final FSM fsm) {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    FSMSerializer.reorderInputPorts(fsm);
    FSMSerializer.reorderOutputPorts(fsm);
    FSMSerializer.save(fsm, bos);
    byte[] _byteArray = bos.toByteArray();
    Charset _defaultCharset = Charset.defaultCharset();
    return new String(_byteArray, _defaultCharset);
  }
  
  public static boolean reorderOutputPorts(final FSM fsm) {
    boolean _xblockexpression = false;
    {
      EList<Port> _in = fsm.getIn();
      BasicEList<Port> ips = new BasicEList<Port>(_in);
      final Function1<Port, String> _function = (Port p) -> {
        return p.getName();
      };
      List<String> _map = ListExtensions.<Port, String>map(ips, _function);
      String _plus = ("before : " + _map);
      InputOutput.<String>println(_plus);
      fsm.getIn().clear();
      final Function1<Port, Integer> _function_1 = (Port p) -> {
        return Integer.valueOf(p.getLayout().getY());
      };
      List<Port> sips = IterableExtensions.<Port, Integer>sortBy(ips, _function_1);
      EList<Port> _in_1 = fsm.getIn();
      _xblockexpression = Iterables.<Port>addAll(_in_1, sips);
    }
    return _xblockexpression;
  }
  
  public static boolean reorderInputPorts(final FSM fsm) {
    boolean _xblockexpression = false;
    {
      EList<Port> _out = fsm.getOut();
      BasicEList<Port> ops = new BasicEList<Port>(_out);
      fsm.getOut().clear();
      final Function1<Port, Integer> _function = (Port p) -> {
        return Integer.valueOf(p.getLayout().getY());
      };
      List<Port> sops = IterableExtensions.<Port, Integer>sortBy(ops, _function);
      EList<Port> _out_1 = fsm.getOut();
      _xblockexpression = Iterables.<Port>addAll(_out_1, sops);
    }
    return _xblockexpression;
  }
  
  public static void saveToFile(final FSM fsm, final File f) {
    try {
      final FileOutputStream bos = new FileOutputStream(f);
      FSMSerializer.save(fsm, bos);
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, e.getMessage(), "Error during FSM serialization", JOptionPane.ERROR_MESSAGE);
        String _message = e.getMessage();
        String _plus = ("Could not serialize current Model to string :" + _message);
        throw new RuntimeException(_plus);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  public static void emfSave(final FSM fsm, final OutputStream os) {
    try {
      FSMDSLStandaloneSetup instance = new FSMDSLStandaloneSetup();
      Injector injector = instance.createInjectorAndDoEMFRegistration();
      FSMDSLStandaloneSetup.doSetup();
      IResourceFactory factory = injector.<IResourceFactory>getInstance(IResourceFactory.class);
      Resource _createResource = factory.createResource(URI.createURI("internal.test"));
      XtextResource resource = ((XtextResource) _createResource);
      resource.getContents().add(fsm);
      HashMap saveOptions = new HashMap<Object, Object>();
      saveOptions.put(XtextResource.OPTION_FORMAT, Boolean.TRUE);
      resource.save(os, saveOptions);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public static void save(final FSM fsm, final OutputStream os) {
    final PrintStream ps = new PrintStream(os);
    ps.append(FSMTextSave.pp(fsm));
    ps.close();
  }
  
  public static EObject parseConstantList(final String input) {
    try {
      byte[] _bytes = input.getBytes(StandardCharsets.UTF_8);
      InputStream fis = new ByteArrayInputStream(_bytes);
      return FSMSerializer.parse(fis);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public static EObject parsePredicate(final FSM fsm, final String in) throws IOException {
    final Function1<ConstantDef, String> _function = (ConstantDef c) -> {
      return PrettyPrinter.pp(c);
    };
    String _string = IterableExtensions.<String>toList(ListExtensions.<ConstantDef, String>map(fsm.getConstants(), _function)).toString();
    final Function1<Port, String> _function_1 = (Port p) -> {
      String _name = p.getName();
      String _plus = (_name + "[");
      int _width = p.getWidth();
      String _plus_1 = (_plus + Integer.valueOf(_width));
      return (_plus_1 + "]");
    };
    String _string_1 = IterableExtensions.<String>toList(ListExtensions.<Port, String>map(fsm.getIn(), _function_1)).toString();
    String _plus = (_string + _string_1);
    String _plus_1 = (_plus + in);
    String input = (_plus_1 + ";");
    byte[] _bytes = input.getBytes(StandardCharsets.UTF_8);
    InputStream fis = new ByteArrayInputStream(_bytes);
    try {
      return FSMSerializer.parse(fis);
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
        StringConcatenation _builder = new StringConcatenation();
        String _message = e.getMessage();
        _builder.append(_message);
        _builder.append(" in \"");
        _builder.append(input);
        _builder.append("\"");
        throw new IOException(_builder.toString());
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  public static EObject parseCommandList(final FSM fsm, final String in) throws IOException {
    final Function1<ConstantDef, String> _function = (ConstantDef c) -> {
      return PrettyPrinter.pp(c);
    };
    String _string = IterableExtensions.<String>toList(ListExtensions.<ConstantDef, String>map(fsm.getConstants(), _function)).toString();
    final Function1<Port, String> _function_1 = (Port p) -> {
      String _name = p.getName();
      String _plus = (_name + "[");
      int _width = p.getWidth();
      String _plus_1 = (_plus + Integer.valueOf(_width));
      return (_plus_1 + "]");
    };
    String _string_1 = IterableExtensions.<String>toList(ListExtensions.<Port, String>map(fsm.getIn(), _function_1)).toString();
    String _plus = (_string + _string_1);
    final Function1<Port, String> _function_2 = (Port p) -> {
      String _name = p.getName();
      String _plus_1 = (_name + "[");
      int _width = p.getWidth();
      String _plus_2 = (_plus_1 + Integer.valueOf(_width));
      return (_plus_2 + "]");
    };
    String _string_2 = IterableExtensions.<String>toList(ListExtensions.<Port, String>map(fsm.getOut(), _function_2)).toString();
    String _plus_1 = (_plus + _string_2);
    String _plus_2 = (_plus_1 + in);
    String input = (_plus_2 + ";");
    byte[] _bytes = input.getBytes(StandardCharsets.UTF_8);
    InputStream fis = new ByteArrayInputStream(_bytes);
    try {
      return FSMSerializer.parse(fis);
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
        StringConcatenation _builder = new StringConcatenation();
        String _message = e.getMessage();
        _builder.append(_message);
        _builder.append(" in \"");
        _builder.append(input);
        _builder.append("\"");
        throw new IOException(_builder.toString());
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  public static FSM load(final String in) throws IOException {
    byte[] _bytes = in.getBytes(StandardCharsets.UTF_8);
    InputStream fis = new ByteArrayInputStream(_bytes);
    try {
      EObject _parse = FSMSerializer.parse(fis);
      FSM fsm = ((FSM) _parse);
      return fsm;
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
        StringConcatenation _builder = new StringConcatenation();
        String _message = e.getMessage();
        _builder.append(_message);
        _builder.append(" in \"");
        _builder.append(in);
        _builder.append("\"");
        throw new IOException(_builder.toString());
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  public static FSM load(final File in) throws IOException {
    InputStream fis = new FileInputStream(in);
    EObject _parse = FSMSerializer.parse(fis);
    FSM fsm = ((FSM) _parse);
    return fsm;
  }
  
  public static EObject parse(final InputStream in) throws IOException {
    FSMDSLStandaloneSetup instance = new FSMDSLStandaloneSetup();
    Injector injector = instance.createInjectorAndDoEMFRegistration();
    FSMDSLStandaloneSetup.doSetup();
    XtextResourceSet rs = injector.<XtextResourceSet>getInstance(XtextResourceSet.class);
    IResourceFactory factory = injector.<IResourceFactory>getInstance(IResourceFactory.class);
    Resource _createResource = factory.createResource(URI.createURI("internal.test"));
    XtextResource r = ((XtextResource) _createResource);
    EPackage.Registry.INSTANCE.put(FSMDSLPackage.eNS_URI, FSMDSLPackage.eINSTANCE);
    rs.getResources().add(r);
    r.load(in, null);
    EcoreUtil.resolveAll(r);
    EList<Resource.Diagnostic> _errors = r.getErrors();
    for (final Resource.Diagnostic error : _errors) {
      {
        System.err.println(error);
        StringConcatenation _builder = new StringConcatenation();
        String _message = error.getMessage();
        _builder.append(_message);
        _builder.append(" ");
        int _line = error.getLine();
        _builder.append(_line);
        throw new IOException(_builder.toString());
      }
    }
    r.getParseResult().getRootNode();
    EObject root = r.getParseResult().getRootASTElement();
    return root;
  }
}
