package com.cburch.logisim.statemachine.parser

import com.cburch.logisim.statemachine.FSMDSLStandaloneSetup
import com.cburch.logisim.statemachine.fSMDSL.FSM
import com.cburch.logisim.statemachine.fSMDSL.FSMDSLPackage
import com.google.inject.Injector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.HashMap
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.resource.IResourceFactory
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.resource.XtextResourceSet
import org.w3c.dom.Entity
import java.nio.charset.Charset
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import javax.swing.JOptionPane
import org.eclipse.emf.common.util.BasicEList
import com.cburch.logisim.statemachine.PrettyPrinter
import java.io.PrintStream

/** 
  */ 
class FSMSerializer {
	
	def static String saveAsString(FSM fsm) {
			val bos = new ByteArrayOutputStream()
			reorderInputPorts(fsm);
			reorderOutputPorts(fsm);
			save(fsm,bos);
			return new String(bos.toByteArray,Charset.defaultCharset())
	}
	
	def static reorderOutputPorts(FSM fsm) {
		var ips= new BasicEList(fsm.in);
		println("before : "+ips.map[p|p.name])
		fsm.in.clear
		var sips= ips.sortBy[p|p.layout.y];
		fsm.in+=sips
	}
	
	def static reorderInputPorts(FSM fsm) {
		var ops= new BasicEList(fsm.out);
		fsm.out.clear
		var sops= ops.sortBy[p|p.layout.y];
		fsm.out+=sops
	}

	def static void saveToFile(FSM fsm, File f) {
		try {
			val bos = new FileOutputStream(f);
			save(fsm,bos);
		} catch (Exception e) {
			e.printStackTrace
			JOptionPane.showMessageDialog(null, e.message,"Error during FSM serialization", JOptionPane.ERROR_MESSAGE);
			throw new RuntimeException("Could not serialize current Model to string :"+e.message);
		}
	}
	
	def static void emfSave(FSM fsm, OutputStream os) {
			var FSMDSLStandaloneSetup instance = new FSMDSLStandaloneSetup()
			var Injector injector = instance.createInjectorAndDoEMFRegistration()
			FSMDSLStandaloneSetup.doSetup()
			var IResourceFactory factory = injector.getInstance(IResourceFactory)
			var XtextResource resource = factory.createResource(URI.createURI("internal.test")) as XtextResource
			resource.getContents().add(fsm)
			var HashMap saveOptions = new HashMap()
			saveOptions.put(XtextResource.OPTION_FORMAT, Boolean.TRUE)
			resource.save(os, saveOptions)
	}
	def static void save(FSM fsm, OutputStream os) {
		val ps = new PrintStream(os);
		ps.append(FSMTextSave.pp(fsm));
		ps.close
	}

	def static parseConstantList(String input) {
		var InputStream fis = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))
		return parse(fis)
	
	}

	def static EObject parsePredicate(FSM fsm, String in) throws IOException {
		var String input = 
			fsm.getConstants().map[c|PrettyPrinter.pp(c)].toList.toString+
			fsm.getIn().map[p|p.name+'['+p.width+']'].toList.toString+ in +';'
		var InputStream fis = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))
		try {
			return parse(fis)
		} catch (Exception e) {
			throw new IOException('''«e.getMessage()» in "«input»"''')
		}	
	}

	def static EObject parseCommandList(FSM fsm, String in) throws IOException {
		
		var String input = 
			fsm.getConstants().map[c|PrettyPrinter.pp(c)].toList.toString+
			fsm.getIn().map[p|p.name+'['+p.width+']'].toList.toString+
			fsm.getOut().map[p|p.name+'['+p.width+']'].toList.toString +in +';'
		var InputStream fis = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))
		try {
			return parse(fis)
		} catch (Exception e) {
			throw new IOException('''«e.getMessage()» in "«input»"''')
		}	
	}

	def public static FSM load(String in) throws IOException {
		var InputStream fis = new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8))
		try {
			var FSM fsm = parse(fis) as FSM
			return fsm
		} catch (Exception e) {
			throw new IOException('''«e.getMessage()» in "«in»"''')
		}	
	} 

	def static FSM load(File in) throws IOException {
		var InputStream fis = new FileInputStream(in)
		var FSM fsm = parse(fis) as FSM
		return fsm
	}

	def public static EObject parse(InputStream in) throws IOException {
		var FSMDSLStandaloneSetup instance = new FSMDSLStandaloneSetup()
		var Injector injector = instance.createInjectorAndDoEMFRegistration()
		FSMDSLStandaloneSetup.doSetup()
		var XtextResourceSet rs = injector.getInstance(XtextResourceSet)
		var IResourceFactory factory = injector.getInstance(IResourceFactory)
		var XtextResource r = factory.createResource(URI.createURI("internal.test")) as XtextResource
		EPackage.Registry.INSTANCE.put(FSMDSLPackage.eNS_URI, FSMDSLPackage.eINSTANCE)
		rs.getResources().add(r)
		r.load(in, null)
		EcoreUtil.resolveAll(r)
		for (org.eclipse.emf.ecore.resource.Resource.Diagnostic error : r.getErrors()) {
			System.err.println(error)
			throw new IOException('''«error.getMessage()» «error.getLine()»''')
		}
		r.getParseResult().getRootNode()
		var EObject root = r.getParseResult().getRootASTElement()
		return root
	}

}
