/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.soc.util;

import static com.cburch.logisim.soc.Strings.S;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;

public class Assembler extends AbstractParser implements LocaleListener {

  private AssemblerInterface assembler;
  private HashMap<GutterIconInfo,StringGetter> errorMarkers;
  private RTextScrollPane pane;
  private AssemblerInfo assemblerInfo;
  private long EntryPoint;
  
  public Assembler(AssemblerInterface assembler, RTextScrollPane pane) {
    this.assembler = assembler;
    errorMarkers = new HashMap<GutterIconInfo,StringGetter>();
    this.pane = pane;
    LocaleManager.addLocaleListener(this);
    reset();
  }
  
  public void reset() {
    errorMarkers.clear();
    pane.getGutter().removeAllTrackingIcons();
    EntryPoint = -1;
  }
  
  public ArrayList<Integer> getErrorPositions() {
    ArrayList<Integer> positions = new ArrayList<Integer>();
    for (GutterIconInfo info : errorMarkers.keySet()) {
      int pos = info.getMarkedOffset();
      if (positions.isEmpty()) positions.add(pos);
      else {
        boolean found = false;
        for (int i = 0 ; i < positions.size() && !found ; i++) {
          if (pos < positions.get(i)) {
            found = true;
            positions.add(i, pos);
          }
        }
        if (!found) positions.add(pos);
      }
    }
    return positions;
  }
  
  public boolean assemble() {
    reset();
    LinkedList<AssemblerToken> assemblerTokens = new LinkedList<AssemblerToken>();
    assemblerInfo = new AssemblerInfo(assembler);
    /* first pass: we build a list of AssemblerTokens from the token 
     * list provided by the AssemblerHighlighter */
    RTextArea text = (RTextArea) pane.getTextArea();
    for (int i = 0 ; i < text.getLineCount() ; i++) {
      assemblerTokens.addAll(checkAndBuildTokens(i));
    }
    /* second pass, we are going to collect all labels */
    HashMap<String,Long> labels = new HashMap<String,Long>();
    HashMap<String,AssemblerToken> labelToken = new HashMap<String,AssemblerToken>();
    for (AssemblerToken asm : assemblerTokens) {
      if (asm.getType() == AssemblerToken.LABEL)
    	if (labels.containsKey(asm.getValue())) {
    	  addError(asm.getoffset(),S.getter("AssemblerDuplicatedLabelNotSupported"),errorMarkers.keySet());
    	  addError(labelToken.get(asm.getValue()).getoffset(),S.getter("AssemblerDuplicatedLabelNotSupported"),errorMarkers.keySet());
    	} else {
          labels.put(asm.getValue(), -1L);
          labelToken.put(asm.getValue(), asm);
    	}
    }
    labelToken.clear();
    /* Third pass, we are going to mark all known labels and references to the pc*/
    for (AssemblerToken asm : assemblerTokens) {
        if (asm.getType() == AssemblerToken.MAYBE_LABEL) {
          if (labels.containsKey(asm.getValue())) 
            asm.setType(AssemblerToken.PARAMETER_LABEL);
      }
    }
    /* Fourth pass: calculate all resulting numbers, merge multi-line strings and determine
     *              the bracketed registers */
    
    /* IMPORTANT: the math functions are evaluated always left to right (can be improved), hence:
     * 5+10*2 => (5+10)*2 = 30
     * 10*2+5 => (10*2)+5 = 25
     */
    ArrayList<AssemblerToken> toBeRemoved = new ArrayList<AssemblerToken>();
    for (int i = 0 ; i < assemblerTokens.size() ; i++) {
      AssemblerToken asm = assemblerTokens.get(i);
      if (AssemblerToken.MATH_OPERATORS.contains(asm.getType())) {
        if ((i+1) >= assemblerTokens.size()) {
          addError(asm.getoffset(),S.getter("AssemblerReguiresNumberAfterMath"),errorMarkers.keySet());
          continue;
        }
        AssemblerToken before = (i==0) ? null :assemblerTokens.get(i-1);
        AssemblerToken after = assemblerTokens.get(i+1);
        if (before == null || (!before.isNumber() && before.getType()!=AssemblerToken.PROGRAM_COUNTER)) 
          before = null;
        if (!after.isNumber() && after.getType() != AssemblerToken.PROGRAM_COUNTER) {
          addError(asm.getoffset(),S.getter("AssemblerReguiresNumberAfterMath"),errorMarkers.keySet());
          continue;
        }
        int beforeValue = before == null ? 0 : before.getNumberValue();
        if (after.getType() == AssemblerToken.PROGRAM_COUNTER || (before != null && before.getType() == AssemblerToken.PROGRAM_COUNTER)) {
          i++;
        } else switch (asm.getType()) {
          case AssemblerToken.MATH_ADD        : after.setValue(beforeValue+after.getNumberValue());
                                                if (before != null) toBeRemoved.add(before);
                                                toBeRemoved.add(asm);
                                                i++;
                                                break;
          case AssemblerToken.MATH_SHIFT_LEFT : after.setValue(beforeValue<<after.getNumberValue());
                                                if (before != null) toBeRemoved.add(before);
                                                toBeRemoved.add(asm);
                                                i++;
                                                break;
          case AssemblerToken.MATH_SHIFT_RIGHT: after.setValue(beforeValue>>after.getNumberValue());
                                                if (before != null) toBeRemoved.add(before);
                                                toBeRemoved.add(asm);
                                                i++;
                                                break;
          case AssemblerToken.MATH_SUBTRACT   : after.setValue(beforeValue-after.getNumberValue());
                                                if (before != null) toBeRemoved.add(before);
                                                toBeRemoved.add(asm);
                                                i++;
                                                break;
          case AssemblerToken.MATH_MUL        : after.setValue(beforeValue*after.getNumberValue());
                                                if (before != null) toBeRemoved.add(before);
                                                toBeRemoved.add(asm);
                                                i++;
                                                break;
          case AssemblerToken.MATH_DIV        : if (after.getNumberValue() == 0) {
        	                                      addError(after.getoffset(),S.getter("AssemblerDivZero"), errorMarkers.keySet());
        	                                      i++;
        	                                      break;
                                                }
        	                                    after.setValue(beforeValue/after.getNumberValue());
                                                if (before != null) toBeRemoved.add(before);
                                                toBeRemoved.add(asm);
                                                i++;
                                                break;
          case AssemblerToken.MATH_REM        : if (after.getNumberValue() == 0) {
                                                  addError(after.getoffset(),S.getter("AssemblerDivZero"), errorMarkers.keySet());
                                                  i++;
                                                  break;
                                                }
                                                after.setValue(beforeValue%after.getNumberValue());
                                                if (before != null) toBeRemoved.add(before);
                                                toBeRemoved.add(asm);
                                                i++;
                                                break;
        }
    } else if (asm.getType() == AssemblerToken.STRING && (i+1) < assemblerTokens.size()) {
        AssemblerToken next;
        do {
          next = assemblerTokens.get(i+1);
          if (next.getType() == AssemblerToken.STRING) {
            i++;
            toBeRemoved.add(next);
            asm.setValue(asm.getValue().concat(next.getValue()));
          }
        } while (next.getType() == AssemblerToken.STRING && (i+1) < assemblerTokens.size());
      } else  if (asm.getType() == AssemblerToken.BRACKET_OPEN && (i+2) < assemblerTokens.size()) {
        AssemblerToken second = assemblerTokens.get(i+1);
        AssemblerToken third = assemblerTokens.get(i+2);
        if (second.getType() == AssemblerToken.REGISTER && third.getType() == AssemblerToken.BRACKET_CLOSE) {
          second.setType(AssemblerToken.BRACKETED_REGISTER);
          toBeRemoved.add(asm);
          toBeRemoved.add(third);
          i += 2;
        }
      }
    }
    assemblerTokens.removeAll(toBeRemoved);
    for (AssemblerToken error : assemblerInfo.getErrors().keySet()) 
        addError(error.getoffset(),assemblerInfo.getErrors().get(error),errorMarkers.keySet());
    /* fifth pass: perform cpu specific operations */
    assembler.performUpSpecificOperationsOnTokens(assemblerTokens);
    /* sixth pass: We are going to detect and remove the macros */
    toBeRemoved.clear();
    boolean errors = false;
    Iterator<AssemblerToken> iter = assemblerTokens.iterator();
    HashMap<String,AssemblerMacro> macros = new HashMap<String,AssemblerMacro>();
    while (iter.hasNext()) {
      AssemblerToken asm = iter.next();
      if (asm.getType() == AssemblerToken.ASM_INSTRUCTION && asm.getValue().equals(".macro")) {
        toBeRemoved.add(asm);
        if (!iter.hasNext()) {
          addError(asm.getoffset(),S.getter("AssemblerExpectedMacroName"),errorMarkers.keySet());
          break;
        }
        AssemblerToken name = iter.next();
        toBeRemoved.add(name);
        if (name.getType() != AssemblerToken.MAYBE_LABEL) {
          addError(asm.getoffset(),S.getter("AssemblerExpectedMacroName"),errorMarkers.keySet());
          break;
        }
        if (!iter.hasNext()) {
          addError(asm.getoffset(),S.getter("AssemblerExpectedMacroNrOfParameters"),errorMarkers.keySet());
          break;
        }
        AssemblerToken nrParameters = iter.next();
        toBeRemoved.add(nrParameters);
        if (!nrParameters.isNumber()) {
          addError(asm.getoffset(),S.getter("AssemblerExpectedMacroNrOfParameters"),errorMarkers.keySet());
          break;
        }
        AssemblerMacro macro = new AssemblerMacro(name.getValue(),nrParameters.getNumberValue());
        boolean endOfMacro = false;
        while (!endOfMacro && iter.hasNext()) {
          AssemblerToken macroAsm = iter.next();
          if (macroAsm.getType() == AssemblerToken.ASM_INSTRUCTION) {
            if (macroAsm.getValue().equals(".endm"))
              endOfMacro = true;
            else {
              addError(macroAsm.getoffset(),S.getter("AssemblerCannotUseInsideMacro"),errorMarkers.keySet());
              errors = true;
            }
          } else {
            macro.addToken(macroAsm);
            if (macroAsm.getType() == AssemblerToken.LABEL) {
              /* labels are local to the macro */
              labels.remove(macroAsm.getValue());
              macro.addLabel(macroAsm.getValue());
            }
          }
          toBeRemoved.add(macroAsm);
        }
        if (!endOfMacro) {
          addError(asm.getoffset(),S.getter("AssemblerEndOfMacroNotFound"),errorMarkers.keySet());
          errors = true;
        } else {
          HashMap<AssemblerToken,StringGetter> markers = new HashMap<AssemblerToken,StringGetter>(); 
          if (macro.checkParameters(markers))
            macros.put(macro.getName(), macro);
          else {
        	for (AssemblerToken marker : markers.keySet()) 
        	  addError(marker.getoffset(),markers.get(marker),errorMarkers.keySet());
            errors = true;
          };
        }
      }
    }
    if (errors) return errorMarkers.isEmpty();
    assemblerTokens.removeAll(toBeRemoved);
    /* go trough the remaining tokens and the macros and mark the macros */
    for (AssemblerToken asm : assemblerTokens)
      if (asm.getType() == AssemblerToken.MAYBE_LABEL) 
        if (macros.containsKey(asm.getValue())) {
          asm.setType(AssemblerToken.MACRO);
        }
    HashMap<AssemblerToken,StringGetter> markers = new HashMap<AssemblerToken,StringGetter>();
    for (String name : macros.keySet()) {
      macros.get(name).checkForMacros(markers, macros.keySet());
    }
    /* finally replace the local labels; this has to be done at this point as before the macros inside
     * a macro were not yet marked as being macro */
    for (String name : macros.keySet()) {
      macros.get(name).replaceLabels(labels, markers, assembler, macros);
    }
    if (!markers.isEmpty()) {
      for (AssemblerToken marker : markers.keySet())
        addError(marker.getoffset(),markers.get(marker),errorMarkers.keySet());
      return errorMarkers.isEmpty();
    }
    /* here the real work starts */
    assemblerInfo.assemble(assemblerTokens,labels,macros);
    for (AssemblerToken error : assemblerInfo.getErrors().keySet()) 
      addError(error.getoffset(),assemblerInfo.getErrors().get(error),errorMarkers.keySet());
    if (labels.containsKey("_start"))
      EntryPoint = labels.get("_start");
    return errorMarkers.isEmpty();
  }
  
  public void addError(int location, StringGetter sg, Set<GutterIconInfo> known) {
    /* first search for the known icons */
    for (GutterIconInfo knownError : known) {
      if (knownError.getMarkedOffset() == location) {
        return;
      }
    }
    /* okay a new error, add it to the marker set */
    GutterIconInfo newError;
    try { 
      newError = pane.getGutter().addOffsetTrackingIcon(location, new ErrorIcon(12), sg.toString());
    } catch (BadLocationException e) { newError = null; }
    if (newError != null) errorMarkers.put(newError,sg);
    /* inform of the new errors */
    ((RSyntaxTextArea)pane.getTextArea()).forceReparsing(this);
  }
  
  public LinkedList<AssemblerToken> checkAndBuildTokens(int lineNumber) {
    LinkedList<AssemblerToken> lineTokens = new LinkedList<AssemblerToken>();
    int startoffset,endoffset;
    RSyntaxTextArea text = (RSyntaxTextArea) pane.getTextArea();
    try { startoffset = text.getLineStartOffset(lineNumber); } catch (BadLocationException e1) { return null;}
    try { endoffset = text.getLineEndOffset(lineNumber); } catch (BadLocationException e1) { return null;}
    Token first = text.getTokenListForLine(lineNumber);
    /* search for all error markers on this line */
    HashSet<GutterIconInfo> lineErrorMarkers = new HashSet<GutterIconInfo>();
    for (GutterIconInfo error : errorMarkers.keySet()) {
      if (error.getMarkedOffset() >= startoffset && error.getMarkedOffset() <= endoffset)
        lineErrorMarkers.add(error);
    }
    /* first pass: check all highlighted tokens and convert them to assembler tokens */
    while (first != null) {
      if (first.getType() != Token.NULL && first.getType() != Token.COMMENT_EOL && 
          first.getType() != Token.WHITESPACE) {
        String name = first.getLexeme();
        int type = first.getType();
        int offset = first.getOffset();
        if (type == Token.LITERAL_CHAR) {
          if (name.equals(",")) 
            lineTokens.add(new AssemblerToken(AssemblerToken.SEPERATOR,null,offset));
          else if (name.equals("(")) {
            if (!assembler.usesRoundedBrackets()) {
              addError(offset,S.getter("AssemblerWrongOpeningBracket"),lineErrorMarkers);
            } else
              lineTokens.add(new AssemblerToken(AssemblerToken.BRACKET_OPEN,null,offset));
          } else if (name.equals(")")) {
            if (!assembler.usesRoundedBrackets()) {
              addError(offset,S.getter("AssemblerWrongClosingBracket"),lineErrorMarkers);
             } else
               lineTokens.add(new AssemblerToken(AssemblerToken.BRACKET_CLOSE,null,offset));
          } else if (name.equals("[")) {
            if (assembler.usesRoundedBrackets()) {
              addError(offset,S.getter("AssemblerWrongOpeningBracket"),lineErrorMarkers);
            } else
              lineTokens.add(new AssemblerToken(AssemblerToken.BRACKET_OPEN,null,offset));
          } else if (name.equals("]")) {
            if (assembler.usesRoundedBrackets()) {
              addError(offset,S.getter("AssemblerWrongClosingBracket"),lineErrorMarkers);
            } else
              lineTokens.add(new AssemblerToken(AssemblerToken.BRACKET_CLOSE,null,offset));
          } else if (name.equals("{"))
            addError(offset,S.getter("AssemblerWrongOpeningBracket"),lineErrorMarkers);
          else if (name.equals("}"))
            addError(offset,S.getter("AssemblerWrongClosingBracket"),lineErrorMarkers);
          else if (name.equals(":"))
            lineTokens.add(new AssemblerToken(AssemblerToken.LABEL_IDENTIFIER,null,offset));
          else if (name.equals("-"))
        	lineTokens.add(new AssemblerToken(AssemblerToken.MATH_SUBTRACT,null,offset));
          else if (name.equals("+"))
        	lineTokens.add(new AssemblerToken(AssemblerToken.MATH_ADD,null,offset));
          else if (name.equals("*"))
        	lineTokens.add(new AssemblerToken(AssemblerToken.MATH_MUL,null,offset));
          else if (name.equals("%"))
        	lineTokens.add(new AssemblerToken(AssemblerToken.MATH_REM,null,offset));
          else if (name.equals("/"))
        	lineTokens.add(new AssemblerToken(AssemblerToken.MATH_DIV,null,offset));
          else if (name.equals("<<"))
        	lineTokens.add(new AssemblerToken(AssemblerToken.MATH_SHIFT_LEFT,null,offset));
          else if (name.equals(">>"))
        	lineTokens.add(new AssemblerToken(AssemblerToken.MATH_SHIFT_RIGHT,null,offset));
          else addError(offset,S.getter("AssemblerUnknowCharacter"),lineErrorMarkers);
        } else
          switch (type) {
            case Token.LITERAL_NUMBER_DECIMAL_INT : 
                lineTokens.add(new AssemblerToken(AssemblerToken.DEC_NUMBER,name,offset));
                break;
            case Token.LITERAL_NUMBER_HEXADECIMAL :
                lineTokens.add(new AssemblerToken(AssemblerToken.HEX_NUMBER,name,offset));
                break;
            case Token.FUNCTION :
                lineTokens.add(new AssemblerToken(AssemblerToken.ASM_INSTRUCTION,name,offset));
                break;
            case Token.OPERATOR :
                lineTokens.add(new AssemblerToken(name.equals("pc") ? AssemblerToken.PROGRAM_COUNTER :
                                                  AssemblerToken.REGISTER,name,offset));
                break;
            case Token.RESERVED_WORD :
                lineTokens.add(new AssemblerToken(AssemblerToken.INSTRUCTION,name,offset));
                break;
            case Token.LITERAL_STRING_DOUBLE_QUOTE :
                lineTokens.add(new AssemblerToken(AssemblerToken.STRING,name,offset));
                break;
            case Token.IDENTIFIER :
                lineTokens.add(new AssemblerToken(AssemblerToken.MAYBE_LABEL,name,offset));
                break;
            case Token.PREPROCESSOR :
                lineTokens.add(new AssemblerToken(AssemblerToken.MACRO_PARAMETER,name,offset));
                break;
          }
      }
      first = first.getNextToken();
    }
    /* second pass, detect the labels */
    ArrayList<AssemblerToken> toBeRemoved = new ArrayList<AssemblerToken>();
    for (int i = 0 ; i < lineTokens.size() ; i++) {
      AssemblerToken asm = lineTokens.get(i);
      if (asm.getType() == AssemblerToken.LABEL_IDENTIFIER) {
        if (i==0) addError(asm.getoffset(),S.getter("AssemblerMissingLabelBefore"),lineErrorMarkers);
        else {
          AssemblerToken before = lineTokens.get(i-1);
          if (before.getType() == AssemblerToken.MAYBE_LABEL) {
            before.setType(AssemblerToken.LABEL);
          } else addError(before.getoffset(),S.getter("AssemblerExpectingLabelIdentifier"),lineErrorMarkers);
        }
        toBeRemoved.add(asm);
      }
    }
    for (AssemblerToken del : toBeRemoved) lineTokens.remove(del);
    /* all errors left are old ones, so clean up */
    for (GutterIconInfo olderr : lineErrorMarkers) {
      pane.getGutter().removeTrackingIcon(olderr);
      errorMarkers.remove(olderr);
    }
    return lineTokens;
  }
  
  public long getEntryPoint() {
    long result = -1;
    if (EntryPoint >= 0) return EntryPoint;
    result = assemblerInfo.getEntryPoint();
    if (result < 0) 
      OptionPane.showMessageDialog(pane, S.get("AssemblerNoExecutableSection"), S.get("AsmPanRun"), OptionPane.ERROR_MESSAGE);
    else
      OptionPane.showMessageDialog(pane, S.get("AssemblerAssumingEntryPoint"), S.get("AsmPanRun"), OptionPane.WARNING_MESSAGE);
    return result;
  }
  
  public boolean download(SocProcessorInterface cpu, CircuitState state) { 
    return assemblerInfo.download(cpu, state);
  }
  
  public ElfSectionHeader getSectionHeader() { return assemblerInfo.getSectionHeader(); }

  @Override
  public void localeChanged() { 
    HashMap<GutterIconInfo,StringGetter> oldSet = new HashMap<GutterIconInfo, StringGetter>();
    oldSet.putAll(errorMarkers);
    errorMarkers.clear();
    for (GutterIconInfo error : oldSet.keySet()) {
      pane.getGutter().removeTrackingIcon(error);
      GutterIconInfo newError;
      try { newError = pane.getGutter().addOffsetTrackingIcon(error.getMarkedOffset(), error.getIcon(), 
                         oldSet.get(error).toString());
      } catch (BadLocationException e) { newError = null; }
      if (newError != null) errorMarkers.put(newError, oldSet.get(error));
    }
    oldSet.clear();
    ((RSyntaxTextArea)pane.getTextArea()).forceReparsing(this);
  }

  @Override
  public ParseResult parse(RSyntaxDocument doc, String style) {
    DefaultParseResult result = new DefaultParseResult(this);
    HashMap<Integer,String> offsets = new HashMap<Integer,String>();
    for (GutterIconInfo x : errorMarkers.keySet()) offsets.put(x.getMarkedOffset(),errorMarkers.get(x).toString());
    for (Token t : doc) {
      int offs = t.getOffset();
      if (offsets.containsKey(offs)) {
    	int len = t.length();
    	int line = doc.getDefaultRootElement().getElementIndex(offs);
        result.addNotice(new DefaultParserNotice(this, offsets.get(offs), line, offs, len));
      }
    }
    return result;
  }

}
