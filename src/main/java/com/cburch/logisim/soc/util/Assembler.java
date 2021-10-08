/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.util;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

public class Assembler extends AbstractParser implements LocaleListener {

  private final AssemblerInterface assembler;
  private final HashMap<GutterIconInfo, StringGetter> errorMarkers;
  private final RTextScrollPane pane;
  private AssemblerInfo assemblerInfo;
  private long entryPoint;

  public Assembler(AssemblerInterface assembler, RTextScrollPane pane) {
    this.assembler = assembler;
    errorMarkers = new HashMap<>();
    this.pane = pane;
    LocaleManager.addLocaleListener(this);
    reset();
  }

  public void reset() {
    errorMarkers.clear();
    pane.getGutter().removeAllTrackingIcons();
    entryPoint = -1;
  }

  public List<Integer> getErrorPositions() {
    final var positions = new ArrayList<Integer>();
    for (final var info : errorMarkers.keySet()) {
      final var pos = info.getMarkedOffset();
      if (positions.isEmpty()) {
        positions.add(pos);
      } else {
        var found = false;
        for (int i = 0; i < positions.size() && !found; i++) {
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
    final var assemblerTokens = new LinkedList<AssemblerToken>();
    assemblerInfo = new AssemblerInfo(assembler);
    /* first pass: we build a list of AssemblerTokens from the token
     * list provided by the AssemblerHighlighter */
    final var text = pane.getTextArea();
    for (var i = 0; i < text.getLineCount(); i++) {
      assemblerTokens.addAll(checkAndBuildTokens(i));
    }
    /* second pass, we are going to collect all labels */
    final var labels = new HashMap<String, Long>();
    final var labelToken = new HashMap<String, AssemblerToken>();
    for (final var asm : assemblerTokens) {
      if (asm.getType() == AssemblerToken.LABEL)
        if (labels.containsKey(asm.getValue())) {
          addError(
              asm.getoffset(),
              S.getter("AssemblerDuplicatedLabelNotSupported"),
              errorMarkers.keySet());
          addError(
              labelToken.get(asm.getValue()).getoffset(),
              S.getter("AssemblerDuplicatedLabelNotSupported"),
              errorMarkers.keySet());
        } else {
          labels.put(asm.getValue(), -1L);
          labelToken.put(asm.getValue(), asm);
        }
    }
    labelToken.clear();
    /* Third pass, we are going to mark all known labels and references to the pc*/
    for (final var asm : assemblerTokens) {
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
    final var toBeRemoved = new ArrayList<AssemblerToken>();
    for (var i = 0; i < assemblerTokens.size(); i++) {
      final var asm = assemblerTokens.get(i);
      if (AssemblerToken.MATH_OPERATORS.contains(asm.getType())) {
        if ((i + 1) >= assemblerTokens.size()) {
          addError(asm.getoffset(), S.getter("AssemblerReguiresNumberAfterMath"), errorMarkers.keySet());
          continue;
        }
        var before = (i == 0) ? null : assemblerTokens.get(i - 1);
        final var after = assemblerTokens.get(i + 1);
        if (before == null
            || (!before.isNumber() && before.getType() != AssemblerToken.PROGRAM_COUNTER))
          before = null;
        if (!after.isNumber() && after.getType() != AssemblerToken.PROGRAM_COUNTER) {
          addError(asm.getoffset(), S.getter("AssemblerReguiresNumberAfterMath"), errorMarkers.keySet());
          continue;
        }
        final var beforeValue = before == null ? 0 : before.getNumberValue();
        if (after.getType() == AssemblerToken.PROGRAM_COUNTER
            || (before != null && before.getType() == AssemblerToken.PROGRAM_COUNTER)) {
          i++;
        } else switch (asm.getType()) {
            case AssemblerToken.MATH_ADD:
              after.setValue(beforeValue + after.getNumberValue());
              if (before != null) toBeRemoved.add(before);
              toBeRemoved.add(asm);
              i++;
              break;
            case AssemblerToken.MATH_SHIFT_LEFT:
              after.setValue(beforeValue << after.getNumberValue());
              if (before != null) toBeRemoved.add(before);
              toBeRemoved.add(asm);
              i++;
              break;
            case AssemblerToken.MATH_SHIFT_RIGHT:
              after.setValue(beforeValue >> after.getNumberValue());
              if (before != null) toBeRemoved.add(before);
              toBeRemoved.add(asm);
              i++;
              break;
            case AssemblerToken.MATH_SUBTRACT:
              after.setValue(beforeValue - after.getNumberValue());
              if (before != null) toBeRemoved.add(before);
              toBeRemoved.add(asm);
              i++;
              break;
            case AssemblerToken.MATH_MUL:
              after.setValue(beforeValue * after.getNumberValue());
              if (before != null) toBeRemoved.add(before);
              toBeRemoved.add(asm);
              i++;
              break;
            case AssemblerToken.MATH_DIV:
              if (after.getNumberValue() == 0) {
                addError(after.getoffset(), S.getter("AssemblerDivZero"), errorMarkers.keySet());
                i++;
                break;
              }
              after.setValue(beforeValue / after.getNumberValue());
              if (before != null) toBeRemoved.add(before);
              toBeRemoved.add(asm);
              i++;
              break;
            case AssemblerToken.MATH_REM:
              if (after.getNumberValue() == 0) {
                addError(after.getoffset(), S.getter("AssemblerDivZero"), errorMarkers.keySet());
                i++;
                break;
              }
              after.setValue(beforeValue % after.getNumberValue());
              if (before != null) toBeRemoved.add(before);
              toBeRemoved.add(asm);
              i++;
              break;
          }
      } else if (asm.getType() == AssemblerToken.STRING && (i + 1) < assemblerTokens.size()) {
        AssemblerToken next;
        do {
          next = assemblerTokens.get(i + 1);
          if (next.getType() == AssemblerToken.STRING) {
            i++;
            toBeRemoved.add(next);
            asm.setValue(asm.getValue().concat(next.getValue()));
          }
        } while (next.getType() == AssemblerToken.STRING && (i + 1) < assemblerTokens.size());
      } else if (asm.getType() == AssemblerToken.BRACKET_OPEN && (i + 2) < assemblerTokens.size()) {
        final var second = assemblerTokens.get(i + 1);
        final var third = assemblerTokens.get(i + 2);
        if (second.getType() == AssemblerToken.REGISTER && third.getType() == AssemblerToken.BRACKET_CLOSE) {
          second.setType(AssemblerToken.BRACKETED_REGISTER);
          toBeRemoved.add(asm);
          toBeRemoved.add(third);
          i += 2;
        }
      }
    }
    assemblerTokens.removeAll(toBeRemoved);
    for (final var error : assemblerInfo.getErrors().keySet()) {
      addError(error.getoffset(), assemblerInfo.getErrors().get(error), errorMarkers.keySet());
    }
    /* fifth pass: perform cpu specific operations */
    assembler.performUpSpecificOperationsOnTokens(assemblerTokens);
    /* sixth pass: We are going to detect and remove the macros */
    toBeRemoved.clear();
    var errors = false;
    final var iter = assemblerTokens.iterator();
    final var macros = new HashMap<String, AssemblerMacro>();
    while (iter.hasNext()) {
      final var asm = iter.next();
      if (asm.getType() == AssemblerToken.ASM_INSTRUCTION && asm.getValue().equals(".macro")) {
        toBeRemoved.add(asm);
        if (!iter.hasNext()) {
          addError(asm.getoffset(), S.getter("AssemblerExpectedMacroName"), errorMarkers.keySet());
          break;
        }
        final var name = iter.next();
        toBeRemoved.add(name);
        if (name.getType() != AssemblerToken.MAYBE_LABEL) {
          addError(asm.getoffset(), S.getter("AssemblerExpectedMacroName"), errorMarkers.keySet());
          break;
        }
        if (!iter.hasNext()) {
          addError(
              asm.getoffset(),
              S.getter("AssemblerExpectedMacroNrOfParameters"),
              errorMarkers.keySet());
          break;
        }
        final var nrParameters = iter.next();
        toBeRemoved.add(nrParameters);
        if (!nrParameters.isNumber()) {
          addError(
              asm.getoffset(),
              S.getter("AssemblerExpectedMacroNrOfParameters"),
              errorMarkers.keySet());
          break;
        }
        final var macro = new AssemblerMacro(name.getValue(), nrParameters.getNumberValue());
        var endOfMacro = false;
        while (!endOfMacro && iter.hasNext()) {
          final var macroAsm = iter.next();
          if (macroAsm.getType() == AssemblerToken.ASM_INSTRUCTION) {
            if (macroAsm.getValue().equals(".endm"))
              endOfMacro = true;
            else {
              addError(macroAsm.getoffset(), S.getter("AssemblerCannotUseInsideMacro"), errorMarkers.keySet());
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
          addError(asm.getoffset(), S.getter("AssemblerEndOfMacroNotFound"), errorMarkers.keySet());
          errors = true;
        } else {
          final var markers = new HashMap<AssemblerToken, StringGetter>();
          if (macro.checkParameters(markers))
            macros.put(macro.getName(), macro);
          else {
            for (final var marker : markers.keySet()) {
              addError(marker.getoffset(), markers.get(marker), errorMarkers.keySet());
            }
            errors = true;
          }
        }
      }
    }
    if (errors) return errorMarkers.isEmpty();
    assemblerTokens.removeAll(toBeRemoved);
    /* go trough the remaining tokens and the macros and mark the macros */
    for (final var asm : assemblerTokens)
      if (asm.getType() == AssemblerToken.MAYBE_LABEL)
        if (macros.containsKey(asm.getValue())) {
          asm.setType(AssemblerToken.MACRO);
        }
    final var markers = new HashMap<AssemblerToken, StringGetter>();
    for (final var name : macros.keySet()) {
      macros.get(name).checkForMacros(markers, macros.keySet());
    }
    // finally replace the local labels; this has to be done at this point as before the macros
    // inside a macro were not yet marked as being macro
    for (final var name : macros.keySet()) {
      macros.get(name).replaceLabels(labels, markers, assembler, macros);
    }
    if (!markers.isEmpty()) {
      for (final var marker : markers.keySet()) {
        addError(marker.getoffset(), markers.get(marker), errorMarkers.keySet());
      }
      return errorMarkers.isEmpty();
    }
    /* here the real work starts */
    assemblerInfo.assemble(assemblerTokens, labels, macros);
    for (final var error : assemblerInfo.getErrors().keySet()) {
      addError(error.getoffset(), assemblerInfo.getErrors().get(error), errorMarkers.keySet());
    }
    if (labels.containsKey("_start")) entryPoint = labels.get("_start");
    return errorMarkers.isEmpty();
  }

  public void addError(int location, StringGetter sg, Set<GutterIconInfo> known) {
    /* first search for the known icons */
    for (final var knownError : known) {
      if (knownError.getMarkedOffset() == location) {
        return;
      }
    }
    /* okay a new error, add it to the marker set */
    GutterIconInfo newError;
    try {
      newError = pane.getGutter().addOffsetTrackingIcon(location, new ErrorIcon(12), sg.toString());
    } catch (BadLocationException e) {
      newError = null;
    }
    if (newError != null) errorMarkers.put(newError, sg);
    /* inform of the new errors */
    ((RSyntaxTextArea) pane.getTextArea()).forceReparsing(this);
  }

  public LinkedList<AssemblerToken> checkAndBuildTokens(int lineNumber) {
    final var lineTokens = new LinkedList<AssemblerToken>();
    int startOffset;
    int endOffset;
    final var text = (RSyntaxTextArea) pane.getTextArea();
    try {
      startOffset = text.getLineStartOffset(lineNumber);
    } catch (BadLocationException e1) {
      return null;
    }
    try {
      endOffset = text.getLineEndOffset(lineNumber);
    } catch (BadLocationException e1) {
      return null;
    }
    var first = text.getTokenListForLine(lineNumber);
    /* search for all error markers on this line */
    final var lineErrorMarkers = new HashSet<GutterIconInfo>();
    for (final var error : errorMarkers.keySet()) {
      if (error.getMarkedOffset() >= startOffset && error.getMarkedOffset() <= endOffset)
        lineErrorMarkers.add(error);
    }
    /* first pass: check all highlighted tokens and convert them to assembler tokens */
    while (first != null) {
      if (first.getType() != Token.NULL
          && first.getType() != Token.COMMENT_EOL
          && first.getType() != Token.WHITESPACE) {
        final var name = first.getLexeme();
        final var type = first.getType();
        final var offset = first.getOffset();
        if (type == Token.LITERAL_CHAR) {
          switch (name) {
            case ",":
              lineTokens.add(new AssemblerToken(AssemblerToken.SEPERATOR, null, offset));
              break;
            case "(":
              if (!assembler.usesRoundedBrackets()) {
                addError(offset, S.getter("AssemblerWrongOpeningBracket"), lineErrorMarkers);
              } else
                lineTokens.add(new AssemblerToken(AssemblerToken.BRACKET_OPEN, null, offset));
              break;
            case ")":
              if (!assembler.usesRoundedBrackets()) {
                addError(offset, S.getter("AssemblerWrongClosingBracket"), lineErrorMarkers);
              } else
                lineTokens.add(new AssemblerToken(AssemblerToken.BRACKET_CLOSE, null, offset));
              break;
            case "[":
              if (assembler.usesRoundedBrackets()) {
                addError(offset, S.getter("AssemblerWrongOpeningBracket"), lineErrorMarkers);
              } else
                lineTokens.add(new AssemblerToken(AssemblerToken.BRACKET_OPEN, null, offset));
              break;
            case "]":
              if (assembler.usesRoundedBrackets()) {
                addError(offset, S.getter("AssemblerWrongClosingBracket"), lineErrorMarkers);
              } else
                lineTokens.add(new AssemblerToken(AssemblerToken.BRACKET_CLOSE, null, offset));
              break;
            case "{":
              addError(offset, S.getter("AssemblerWrongOpeningBracket"), lineErrorMarkers);
              break;
            case "}":
              addError(offset, S.getter("AssemblerWrongClosingBracket"), lineErrorMarkers);
              break;
            case ":":
              lineTokens.add(new AssemblerToken(AssemblerToken.LABEL_IDENTIFIER, null, offset));
              break;
            case "-":
              lineTokens.add(new AssemblerToken(AssemblerToken.MATH_SUBTRACT, null, offset));
              break;
            case "+":
              lineTokens.add(new AssemblerToken(AssemblerToken.MATH_ADD, null, offset));
              break;
            case "*":
              lineTokens.add(new AssemblerToken(AssemblerToken.MATH_MUL, null, offset));
              break;
            case "%":
              lineTokens.add(new AssemblerToken(AssemblerToken.MATH_REM, null, offset));
              break;
            case "/":
              lineTokens.add(new AssemblerToken(AssemblerToken.MATH_DIV, null, offset));
              break;
            case "<<":
              lineTokens.add(new AssemblerToken(AssemblerToken.MATH_SHIFT_LEFT, null, offset));
              break;
            case ">>":
              lineTokens.add(new AssemblerToken(AssemblerToken.MATH_SHIFT_RIGHT, null, offset));
              break;
            default:
              addError(offset, S.getter("AssemblerUnknowCharacter"), lineErrorMarkers);
              break;
          }
        } else
          switch (type) {
            case Token.LITERAL_NUMBER_DECIMAL_INT:
              lineTokens.add(new AssemblerToken(AssemblerToken.DEC_NUMBER, name, offset));
              break;
            case Token.LITERAL_NUMBER_HEXADECIMAL:
              lineTokens.add(new AssemblerToken(AssemblerToken.HEX_NUMBER, name, offset));
              break;
            case Token.FUNCTION:
              lineTokens.add(new AssemblerToken(AssemblerToken.ASM_INSTRUCTION, name, offset));
              break;
            case Token.OPERATOR:
              lineTokens.add(
                  new AssemblerToken(
                      name.equals("pc") ? AssemblerToken.PROGRAM_COUNTER : AssemblerToken.REGISTER,
                      name,
                      offset));
              break;
            case Token.RESERVED_WORD:
              lineTokens.add(new AssemblerToken(AssemblerToken.INSTRUCTION, name, offset));
              break;
            case Token.LITERAL_STRING_DOUBLE_QUOTE:
              lineTokens.add(new AssemblerToken(AssemblerToken.STRING, name, offset));
              break;
            case Token.IDENTIFIER:
              lineTokens.add(new AssemblerToken(AssemblerToken.MAYBE_LABEL, name, offset));
              break;
            case Token.PREPROCESSOR:
              lineTokens.add(new AssemblerToken(AssemblerToken.MACRO_PARAMETER, name, offset));
              break;
          }
      }
      first = first.getNextToken();
    }
    /* second pass, detect the labels */
    final var toBeRemoved = new ArrayList<AssemblerToken>();
    for (var i = 0; i < lineTokens.size(); i++) {
      final var asm = lineTokens.get(i);
      if (asm.getType() == AssemblerToken.LABEL_IDENTIFIER) {
        if (i == 0)
          addError(asm.getoffset(), S.getter("AssemblerMissingLabelBefore"), lineErrorMarkers);
        else {
          final var before = lineTokens.get(i - 1);
          if (before.getType() == AssemblerToken.MAYBE_LABEL) {
            before.setType(AssemblerToken.LABEL);
          } else
            addError(
                before.getoffset(),
                S.getter("AssemblerExpectingLabelIdentifier"),
                lineErrorMarkers);
        }
        toBeRemoved.add(asm);
      }
    }
    for (final var del : toBeRemoved) lineTokens.remove(del);
    /* all errors left are old ones, so clean up */
    for (final var olderr : lineErrorMarkers) {
      pane.getGutter().removeTrackingIcon(olderr);
      errorMarkers.remove(olderr);
    }
    return lineTokens;
  }

  public long getEntryPoint() {
    long result = -1;
    if (entryPoint >= 0) return entryPoint;
    result = assemblerInfo.getEntryPoint();
    if (result < 0)
      OptionPane.showMessageDialog(
          pane,
          S.get("AssemblerNoExecutableSection"),
          S.get("AsmPanRun"),
          OptionPane.ERROR_MESSAGE);
    else
      OptionPane.showMessageDialog(
          pane,
          S.get("AssemblerAssumingEntryPoint"),
          S.get("AsmPanRun"),
          OptionPane.WARNING_MESSAGE);
    return result;
  }

  public boolean download(SocProcessorInterface cpu, CircuitState state) {
    return assemblerInfo.download(cpu, state);
  }

  public ElfSectionHeader getSectionHeader() {
    return assemblerInfo.getSectionHeader();
  }

  @Override
  public void localeChanged() {
    final var oldSet = new HashMap<GutterIconInfo, StringGetter>(errorMarkers);
    errorMarkers.clear();
    for (final var error : oldSet.keySet()) {
      pane.getGutter().removeTrackingIcon(error);
      GutterIconInfo newError;
      try {
        newError =
            pane.getGutter()
                .addOffsetTrackingIcon(
                    error.getMarkedOffset(), error.getIcon(), oldSet.get(error).toString());
      } catch (BadLocationException e) {
        newError = null;
      }
      if (newError != null) errorMarkers.put(newError, oldSet.get(error));
    }
    oldSet.clear();
    ((RSyntaxTextArea) pane.getTextArea()).forceReparsing(this);
  }

  @Override
  public ParseResult parse(RSyntaxDocument doc, String style) {
    final var result = new DefaultParseResult(this);
    final var offsets = new HashMap<Integer, String>();
    for (final var gutterIconInfo : errorMarkers.keySet()) {
      offsets.put(gutterIconInfo.getMarkedOffset(), errorMarkers.get(gutterIconInfo).toString());
    }
    for (final var token : doc) {
      int offs = token.getOffset();
      if (offsets.containsKey(offs)) {
        final var len = token.length();
        final var line = doc.getDefaultRootElement().getElementIndex(offs);
        result.addNotice(new DefaultParserNotice(this, offsets.get(offs), line, offs, len));
      }
    }
    return result;
  }

}
