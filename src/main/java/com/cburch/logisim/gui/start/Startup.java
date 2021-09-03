/*
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

package com.cburch.logisim.gui.start;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.Main;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.fpga.download.Download;
import com.cburch.logisim.fpga.file.BoardReaderClass;
import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.gui.icons.InfoIcon;
import com.cburch.logisim.gui.icons.QuestionIcon;
import com.cburch.logisim.gui.icons.WarningIcon;
import com.cburch.logisim.gui.main.Print;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.WindowManagers;
import com.cburch.logisim.gui.test.TestBench;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.std.gates.GatesLibrary;
import com.cburch.logisim.util.LineBuffer;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.help.JHelp;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.drjekyll.fontchooser.FontChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Startup implements AWTEventListener {

  static final Logger logger = LoggerFactory.getLogger(Startup.class);
  private static Startup startupTemp = null;
  private final ArrayList<File> filesToOpen = new ArrayList<>();
  private final HashMap<File, File> substitutions = new HashMap<>();
  private final ArrayList<File> filesToPrint = new ArrayList<>();
  // based on command line
  final boolean isTty;
  private File templFile = null;
  private boolean templEmpty = false;
  private boolean templPlain = false;
  private String testVector = null;
  private String circuitToTest = null;
  private boolean exitAfterStartup = false;
  private boolean showSplash;
  private File loadFile;
  private int ttyFormat = 0;
  // from other sources
  private boolean initialized = false;
  private SplashScreen monitor = null;
  /* Testing Circuit Variable */
  private String testCircuitPathInput = null;
  /* Test implementation */
  private String testCircuitImpPath = null;
  private boolean doFpgaDownload = false;
  private double testTickFrequency = 1;
  /* Name of the circuit withing logisim */
  private String testCircuitImpName = null;
  /* Name of the board to run on i.e Reptar, MAXV ...*/
  private String testCircuitImpBoard = null;
  /* Path folder containing Map file */
  private String testCircuitImpMapFile = null;
  /* Indicate if only the HDL should be generated */
  private Boolean testCircuitHdlOnly = false;
  /* Testing Xml (circ file) Variable */
  private String testCircPathInput = null;
  private String testCircPathOutput = null;

  private Startup(boolean isTty) {
    this.isTty = isTty;
    this.showSplash = !isTty;
  }

  static void doOpen(File file) {
    if (startupTemp != null) {
      startupTemp.doOpenFile(file);
    }
  }

  static void doPrint(File file) {
    if (startupTemp != null) {
      startupTemp.doPrintFile(file);
    }
  }

  // Non-used args short keys
  // --------------------------
  //    d    i k         u w t
  public static final String ARG_HELP = "h";
  public static final String ARG_HELP_LONG = "help";
  public static final String ARG_VERSION = "v";
  public static final String ARG_VERSION_LONG = "version";

  public static final String ARG_TTY = "t";
  public static final String ARG_TTY_LONG = "tty";
  public static final String ARG_TEST_FGPA = "f";
  public static final String ARG_TEST_FGPA_LONG = "test-fpga";
  public static final String ARG_CLEAR_PREFS = "r";
  public static final String ARG_CLEAR_PREFS_LONG = "clear-prefs";
  public static final String ARG_SUBSTITUTE = "s";
  public static final String ARG_SUBSTITUTE_LONG = "substitute";
  public static final String ARG_LOAD = "l";
  public static final String ARG_LOAD_LONG = "load";
  public static final String ARG_EMPTY = "e";
  public static final String ARG_EMPTY_LONG = "empty";
  public static final String ARG_PLAIN = "p";
  public static final String ARG_PLAIN_LONG = "plain";
  public static final String ARG_GATES = "g";
  public static final String ARG_GATES_LONG = "gates";
  public static final String ARG_GEOMETRY = "m";
  public static final String ARG_GEOMETRY_LONG = "geometry";
  public static final String ARG_LOCALE = "o";
  public static final String ARG_LOCALE_LONG = "locale";
  public static final String ARG_ACCENTS = "x";
  public static final String ARG_ACCENTS_LONG = "accents";
  public static final String ARG_TEMPLATE = "z";
  public static final String ARG_TEMPLATE_LONG = "template";
  public static final String ARG_NO_SPLASH = "n";
  public static final String ARG_NO_SPLASH_LONG = "no-splash";
  public static final String ARG_TEST_VECTOR = "w";
  public static final String ARG_TEST_VECTOR_LONG = "test-vector";
  public static final String ARG_TEST_CIRCUIT = "b";
  public static final String ARG_TEST_CIRCUIT_LONG = "test-circuit";
  public static final String ARG_TEST_CIRC_GEN = "j";
  public static final String ARG_TEST_CIRC_GEN_LONG = "test-circ-gen";
  public static final String ARG_CIRCUIT = "c";
  public static final String ARG_CIRCUIT_LONG = "circuit";

  /**
   * Parses provided string expecting it represent boolean option. Accepted values
   * are 'yes' (true) and 'no' (false). In case of unsupported value exception is
   * thrown.
   *
   * @param option String that represents boolean value.
   *
   * @return Value converted to boolean.
   *
   * @throws IllegalArgumentException
   */
  protected static boolean parseBool(String option) throws IllegalArgumentException {
    final var flag = option.toLowerCase();
    if (flag.equals("yes") || flag.equals("1") || flag.equals("true")) return true;
    if (flag.equals("no") || flag.equals("0") || flag.equals("false")) return false;
    throw new IllegalArgumentException("Invalid boolean flag. Use 'yes'/'true'/'1' or 'no'/'false'/'0'.");
  }

  /**
   * Prints available command line options.
   *
   * @param opts Configured CLI options.
   *
   * @return Handler return code enum (RC.xxx)
   */
  protected static RC printHelp(Options opts) {
    printVersion();
    System.out.println();
    (new HelpFormatter()).printHelp(Main.APP_NAME, null, opts, null, true);
    return RC.QUIT;
  }

  /**
   * Prints program version, build Id, compilation date and more.
   *
   * @return Handler return code enum (RC.xxx)
   */
  protected static RC printVersion() {
    System.out.println(Main.APP_DISPLAY_NAME);
    System.out.println(Main.APP_URL);
    System.out.println(LineBuffer.format("{{1}} ({{2}})", BuildInfo.buildId, BuildInfo.dateIso8601));
    System.out.println(LineBuffer.format("{{1}} ({{2}})", Main.JVM_VERSION, Main.JVM_VENDOR));
    return RC.QUIT;
  }

  /**
   * Helper class that simplifies setup of parser argument option.
   * Note: it assumes that if option have arguments, then there's
   * localization string named after option string base key with "ArgName"
   * suffix (i.e. for "fooBar" expecting arguments there must be "fooBarArgName"
   * string describing (short as possible, best in single word) type
   * of arguments (used to print CLI help page).
   *
   * @param opts Instance of {@link Options}.
   * @param stringBaseKey String localization base key.
   * @param shortKey Argument short key (i.e. "c" for "-c").
   * @param longKey Argument ling key (i.e. "foo" for "--foo").
   * @param expectedArgsCount Number of required option arguments.
   */
  protected static void addOption(Options opts, String stringBaseKey, String shortKey, String longKey, int expectedArgsCount) {
    final var builder = Option.builder(shortKey).longOpt(longKey).desc(S.get(stringBaseKey));
    if (expectedArgsCount > 0) {
      final var argNameKey = LineBuffer.format("{{1}}ArgName", stringBaseKey);
      builder.argName(S.get(argNameKey));
      builder.numberOfArgs(expectedArgsCount);
    }
    opts.addOption(builder.build());
  }

  /**
   * Add argumentless Option to CLI parser options.
   *
   * @param opts Instance of {@link Options}.
   * @param stringBaseKey String localization base key.
   * @param shortKey Argument short key (i.e. "c" for "-c").
   * @param longKey Argument ling key (i.e. "foo" for "--foo").
   * @param expectedArgsCount Number of required option arguments.
   */
  protected static void addOption(Options opts, String stringBaseKey, String shortKey, String longKey) {
    addOption(opts, stringBaseKey, shortKey, longKey, 0);
  }

  /**
   * Return code of last run argument handler.
   */
  private static RC lastHandlerRc;

  /**
   * Returns {@true} if last argument handler called requested app termination (w/o error).
   */
  public static boolean shallQuit() {
    return lastHandlerRc == RC.QUIT;
  }

  /**
   * Parses CLI arguments
   *
   * @param args CLI arguments
   *
   * @return Instance of Startup class.
   */
  public static Startup parseArgs(String[] args) {
    final var opts = new Options();
    addOption(opts, "argHelpOption", ARG_HELP, ARG_HELP_LONG);
    addOption(opts, "argVersionOption", ARG_VERSION, ARG_VERSION_LONG);

    // Set up supported arguments for the arg parser to look for.
    // Note: you need to create handler for each option. See handler loop below.
    addOption(opts, "argTtyOption", ARG_TTY, ARG_TTY_LONG, 1);
    addOption(opts, "argTestImplement", ARG_TEST_FGPA, ARG_TEST_FGPA_LONG, Option.UNLIMITED_VALUES);  // We can have 3, 4 or 5 arguments here
    addOption(opts, "argClearOption", ARG_CLEAR_PREFS, ARG_CLEAR_PREFS_LONG);
    addOption(opts, "argSubOption", ARG_SUBSTITUTE, ARG_SUBSTITUTE_LONG, 2);
    addOption(opts, "argLoadOption", ARG_LOAD, ARG_LOAD_LONG, 1);
    addOption(opts, "argEmptyOption", ARG_EMPTY, ARG_EMPTY_LONG);
    addOption(opts, "argPlainOption", ARG_PLAIN, ARG_PLAIN_LONG);
    addOption(opts, "argGatesOption", ARG_GATES, ARG_GATES_LONG, 1);
    addOption(opts, "argGeometryOption", ARG_GEOMETRY, ARG_GEOMETRY_LONG, 1);
    addOption(opts, "argLocaleOption", ARG_LOCALE, ARG_LOCALE_LONG, 1);
    addOption(opts, "argAccentsOption", ARG_ACCENTS, ARG_ACCENTS_LONG, 1);
    addOption(opts, "argTemplateOption", ARG_TEMPLATE, ARG_TEMPLATE_LONG, 1);
    addOption(opts, "argNoSplashOption", ARG_NO_SPLASH, ARG_NO_SPLASH_LONG);
    addOption(opts, "argTestVectorOption", ARG_TEST_VECTOR, ARG_TEST_VECTOR_LONG, 2);    // FIXME: NO LANG STR FOR IT!
    addOption(opts, "argTestCircuit", ARG_TEST_CIRCUIT, ARG_TEST_CIRCUIT_LONG, 1);   // FIXME add "Option" suffix to key name
    addOption(opts, "argTestCircGen", ARG_TEST_CIRC_GEN, ARG_TEST_CIRC_GEN_LONG, 2);   // FIXME add "Option" suffix to key name

    CommandLine cmd;
    try {
      cmd = (new DefaultParser()).parse(opts, args);
    } catch (UnrecognizedOptionException ex) {
      // FIXME: hardcoded string
      logger.error("Unrecognized option: '" + ex + ".);");
      logger.error("Use --help for more info.");
      return null;
    } catch (ParseException ex) {
      // FIXME: hardcoded string
      logger.error("Failed processing command line arguments.");
      return null;
    }

    // see whether we'll be using any graphics
    var isTty = false;
    var shallClearPreferences = false;
    if (cmd.hasOption(ARG_TTY) || cmd.hasOption(ARG_TEST_FGPA)) {
      isTty = true;
      Main.headless = true;
    } else {
      shallClearPreferences = cmd.hasOption(ARG_CLEAR_PREFS);
    }

    if (!isTty) {
      // we're using the GUI: Set up the Look&Feel to match the platform
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      LocaleManager.setReplaceAccents(false);
      // Initialize graphics acceleration if appropriate
      AppPreferences.handleGraphicsAcceleration();
    }

    // Initialize startup object.
    final var startup = new Startup(isTty);
    startupTemp = startup;
    if (!isTty) {
      MacOsAdapter.addListeners();
    }

    if (shallClearPreferences) {
      AppPreferences.clear();
    }

    // Iterate over parsed arguments and invoke option handler
    // for each detected argument.
    final var optionIter = cmd.iterator();
    while (optionIter.hasNext()) {
      final var opt = optionIter.next();
      // Note: you should have handler for each option. So number of `case`s
      // here should equal number of calls to `addOption()` above.
      final var optHandlerRc = switch (opt.getOpt()) {
        case ARG_HELP -> printHelp(opts);
        case ARG_VERSION -> printVersion();
        case ARG_TTY -> handleArgTty(startup, opt);
        case ARG_SUBSTITUTE -> handleArgSubstitute(startup, opt);
        case ARG_LOAD -> handleArgLoad(startup, opt);
        case ARG_EMPTY -> handleArgEmpty(startup, opt);
        case ARG_PLAIN -> handleArgPlain(startup, opt);
        case ARG_GATES -> handleArgGates(startup, opt);
        case ARG_GEOMETRY -> handleArgGeometry(startup, opt);
        case ARG_LOCALE -> handleArgLocale(startup, opt);
        case ARG_ACCENTS -> handleArgAccents(startup, opt);
        case ARG_TEMPLATE -> handleArgTemplate(startup, opt);
        case ARG_NO_SPLASH -> handleArgNoSplash(startup, opt);
        case ARG_TEST_VECTOR -> handleArgTestVector(startup, opt);
        case ARG_TEST_FGPA -> handleArgTestFpga(startup, opt);
        case ARG_TEST_CIRCUIT -> handleArgTestCircuit(startup, opt);
        case ARG_TEST_CIRC_GEN -> handleArgTestCircGen(startup, opt);
        case ARG_CIRCUIT -> handleArgCircuit(startup, opt);
        default -> RC.OK; // should not really happen IRL.
      };
      lastHandlerRc = optHandlerRc;
      switch (optHandlerRc) {
        case QUIT:
          return startup;
        default:
          continue;
      }
    }

    // positional argument being files to load
    for (final var arg : cmd.getArgs()) {
      startup.filesToOpen.add(new File(arg));
    }

    if (startup.exitAfterStartup && startup.filesToOpen.isEmpty()) {
      printHelp(opts);
      return null;
    }
    if (startup.isTty && startup.filesToOpen.isEmpty()) {
      logger.error(S.get("ttyNeedsFileError"));
      return null;
    }
    if (startup.loadFile != null && !startup.isTty) {
      logger.error(S.get("loadNeedsTtyError"));
      return null;
    }

    return startup;
  }

  /* ********************************************************************************************* */

  /**
   * Supported return codes from command handlers;
   */
  public static enum RC {
    /**
     * Handler completed succesfuly. We can proceed with another argument.
     */
    OK,
    /**
     * Handler had some minor propblems, but it is recoverable, so parsing should keep going.
     */
    WARN,
    /**
     * Unrecoverable error occured while handling option. No fall back, must quit.
     */
    QUIT
  }

  private static RC handleArgTty(Startup startup, Option opt) {
    // TTY format parsing
    final var ttyVal = opt.getValue();
    final var fmts = ttyVal.split(",");
    if (fmts.length > 0) {
      // FIXME: why we support multiple TTY types in one invocation? fallback?
      for (final var singleFmt : fmts) {
        final var val = switch (singleFmt.trim()) {
          case "table" -> TtyInterface.FORMAT_TABLE;
          case "speed" -> TtyInterface.FORMAT_SPEED;
          case "tty" -> TtyInterface.FORMAT_TTY;
          case "halt" -> TtyInterface.FORMAT_HALT;
          case "stats" -> TtyInterface.FORMAT_STATISTICS;
          case "binary" -> TtyInterface.FORMAT_TABLE_BIN;
          case "hex" -> TtyInterface.FORMAT_TABLE_HEX;
          case "csv" -> TtyInterface.FORMAT_TABLE_CSV;
          case "tabs" -> TtyInterface.FORMAT_TABLE_TABBED;
          default -> 0;
        };

        if (val == 0) {
          logger.error(S.get("ttyFormatError"));
          // FIXME: Shouldn't we exit here -> RC.QUIT;
          continue;
        }
        startup.ttyFormat |= val;
        return RC.OK;
      }
    }
    logger.error(S.get("ttyFormatError"));
    // FIXME: Shouldn't we exit here; -> RC.QUIT
    return RC.WARN;
  }

  private static RC handleArgSubstitute(Startup startup, Option opt) {
    final var fileA = new File(opt.getValues()[0]);
    final var fileB = new File(opt.getValues()[1]);
    if (!startup.substitutions.containsKey(fileA)) {
      startup.substitutions.put(fileA, fileB);
      return RC.OK;
    }

    // FIXME: warning should be sufficient here maybe?
    logger.error(S.get("argDuplicateSubstitutionError"));
    return RC.QUIT;
  }

  private static RC handleArgLoad(Startup startup, Option opt) {
    if (startup.loadFile != null) {
      logger.error(S.get("loadMultipleError"));
      // FIXME: shouldn't we quit here? -> RC.QUIT;
      return RC.WARN;
    }
    final var fileName = opt.getValue();
    startup.loadFile = new File(fileName);
    return RC.OK;
  }

  private static RC handleArgEmpty(Startup startup, Option opt) {
    if (startup.templFile != null || startup.templEmpty || startup.templPlain) {
      logger.error(S.get("argOneTemplateError"));
      return RC.QUIT;
    }
    startup.templEmpty = true;
    return RC.OK;
  }

  private static RC handleArgPlain(Startup startup, Option opt) {
    if (startup.templFile != null || startup.templEmpty || startup.templPlain) {
      logger.error(S.get("argOneTemplateError"));
      return RC.QUIT;
    }
    startup.templPlain = true;
    return RC.OK;
  }

  private static RC handleArgGates(Startup startup, Option opt) {
    final var gateShape = opt.getValue();
    if ("shaped".equals(gateShape)) {
      AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_SHAPED);
      return RC.OK;
    } else if ("rectangular".equals(gateShape)) {
      AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_RECTANGULAR);
      return RC.OK;
    }

    logger.error(S.get("argGatesOptionError"));
    return RC.QUIT;
  }

  private static RC handleArgGeometry(Startup startup, Option opt) {
    final var geometry = opt.getValue();
    final var wxh = geometry.split("[xX]");

    if (wxh.length != 2 || wxh[0].length() < 1 || wxh[1].length() < 1) {
      logger.error(S.get("argGeometryError"));
      return RC.QUIT;
    }

    final var p = wxh[1].indexOf('+', 1);
    String loc = null;
    var x = 0;
    var y = 0;
    if (p >= 0) {
      loc = wxh[1].substring(p + 1);
      wxh[1] = wxh[1].substring(0, p);
      final var xy = loc.split("\\+");
      if (xy.length != 2 || xy[0].length() < 1 || xy[1].length() < 1) {
        logger.error(S.get("argGeometryError"));
        return RC.QUIT;
      }
      try {
        x = Integer.parseInt(xy[0]);
        y = Integer.parseInt(xy[1]);
      } catch (NumberFormatException e) {
        logger.error(S.get("argGeometryError"));
        return RC.QUIT;
      }
    }

    var w = 0;
    var h = 0;
    try {
      w = Integer.parseInt(wxh[0]);
      h = Integer.parseInt(wxh[1]);
    } catch (NumberFormatException e) {
      logger.error(S.get("argGeometryError"));
      return RC.QUIT;
    }
    if (w <= 0 || h <= 0) {
      logger.error(S.get("argGeometryError"));
      return RC.QUIT;
    }
    AppPreferences.WINDOW_WIDTH.set(w);
    AppPreferences.WINDOW_HEIGHT.set(h);
    if (loc != null) {
      AppPreferences.WINDOW_LOCATION.set(x + "," + y);
    }
    return RC.OK;
  }

  private static RC handleArgLocale(Startup startup, Option opt) {
    setLocale(opt.getValue());
    return RC.OK;
  }

  private static RC handleArgAccents(Startup startup, Option opt) {
    final var flag = opt.getValue().toLowerCase();
    try {
      AppPreferences.ACCENTS_REPLACE.setBoolean(!parseBool(flag));
    } catch (IllegalArgumentException ex) {
      logger.error(S.get("argAccentsOptionError"));
      return RC.QUIT;
    }
    return RC.OK;
  }

  private static RC handleArgTemplate(Startup startup, Option opt) {
    if (startup.templFile != null || startup.templEmpty || startup.templPlain) {
      logger.error(S.get("argOneTemplateError"));
      return RC.QUIT;
    }
    final var file = opt.getValue();
    startup.templFile = new File(file);
    String errMsg = null;
    if (!startup.templFile.exists()) errMsg = S.get("templateMissingError", file);
    if (errMsg == null && !startup.templFile.canRead()) errMsg = S.get("templateCannotReadError", file);
    if (errMsg != null) return RC.WARN;  // FIXME: shouldn't we quit in such case?
    return RC.OK;
  }

  private static RC handleArgNoSplash(Startup startup, Option opt) {
    startup.showSplash = false;
    return RC.OK;
  }

  private static RC handleArgTestVector(Startup startup, Option opt) {
    startup.circuitToTest = opt.getValues()[0];
    startup.testVector = opt.getValues()[1];
    startup.showSplash = false;
    startup.exitAfterStartup = true;
    // This is to test a test bench. It will return 0 or 1 depending on if the tests pass or not.
    return RC.OK;
  }


  /**
   * Handles 4th argument of `--test-fpga` argument which can be either string literal
   * or tick frequency.
   *
   * Supported argument formats for `--test-fpga`:
   * - `circ_input circuit_name board`
   * - `circ_input circuit_name board [HDLONLY]`
   * - `circ_input circuit_name board [HDLONLY] [tick frequency]`
   * - `circ_input circuit_name board [tick frequency]`
   * - `circ_input circuit_name board [tick frequency] [HDLONLY]`
   *
   */
  private static RC handleArgTestFpgaParseArg(Startup startup, String argVal) {
    if ("HDLONLY".equals(argVal)) {
      startup.testCircuitHdlOnly = true;
      return RC.OK;
    }
    try {
      startup.testTickFrequency = Integer.parseUnsignedInt(argVal);
      return RC.OK;
    } catch (NumberFormatException ex) {
      logger.error(S.get("argTestInvalidArguments"));
    }
    return RC.QUIT;
  }

  private static RC handleArgTestFpga(Startup startup, Option opt) {
    final var optArgs = opt.getValues();
    final var argsCnt = optArgs.length;

    if (argsCnt < 3 || argsCnt > 5) {
      logger.error(S.get("argTestInvalidArguments"));
      return RC.QUIT;
    }

    // already handled above
    startup.testCircuitImpPath = optArgs[0];
    startup.testCircuitImpName = optArgs[1];
    startup.testCircuitImpBoard = optArgs[2];

    if (argsCnt >= 4) handleArgTestFpgaParseArg(startup, optArgs[3]);
    if (argsCnt >= 5) handleArgTestFpgaParseArg(startup, optArgs[4]);

    startup.doFpgaDownload = true;
    startup.showSplash = false;
    startup.filesToOpen.add(new File(startup.testCircuitImpPath));
    return RC.OK;
  }

  private static RC handleArgTestCircuit(Startup startup, Option opt) {
    final var fileName = opt.getValue();
    startup.testCircuitPathInput = fileName;
    startup.filesToOpen.add(new File(fileName));
    startup.showSplash = false;
    startup.exitAfterStartup = true;
    return RC.OK;
  }

  private static RC handleArgTestCircGen(Startup startup, Option opt) {
    final var optArgs = opt.getValues();
    // This is to test the XML consistency over different version of the Logisim
    // This is the input path of the file to open
    startup.testCircPathInput = optArgs[0];
    startup.filesToOpen.add(new File(startup.testCircPathInput));
    // This is the output file's path. The comparaison shall be done between the  testCircPathInput and the testCircPathOutput
    startup.testCircPathOutput = optArgs[1];
    startup.showSplash = false;
    startup.exitAfterStartup = true;
    return RC.OK;
  }

  private static RC handleArgCircuit(Startup startup, Option opt) {
    startup.circuitToTest = opt.getValue();
    return RC.OK;
  }

  /* ********************************************************************************************* */

  private static void setLocale(String lang) {
    final var opts = S.getLocaleOptions();
    for (final var locale : opts) {
      if (lang.equals(locale.toString())) {
        LocaleManager.setLocale(locale);
        return;
      }
    }
    logger.warn(S.get("invalidLocaleError"));
    logger.warn(S.get("invalidLocaleOptionsHeader"));

    for (final var opt : opts) {
      logger.warn("   {}", opt.toString());
    }
    System.exit(-1);
  }

  private void doOpenFile(File file) {
    if (initialized) {
      ProjectActions.doOpen(null, null, file);
    } else {
      filesToOpen.add(file);
    }
  }

  private void doPrintFile(File file) {
    if (initialized) {
      final var toPrint = ProjectActions.doOpen(null, null, file);
      Print.doPrint(toPrint);
      toPrint.getFrame().dispose();
    } else {
      filesToPrint.add(file);
    }
  }

  List<File> getFilesToOpen() {
    return filesToOpen;
  }

  File getLoadFile() {
    return loadFile;
  }

  String getCircuitToTest() {
    return circuitToTest;
  }

  Map<File, File> getSubstitutions() {
    return Collections.unmodifiableMap(substitutions);
  }

  int getTtyFormat() {
    return ttyFormat;
  }

  boolean isFpgaDownload() {
    return doFpgaDownload;
  }

  boolean FpgaDownload(Project proj) {
    /* Testing synthesis */
    Download Downloader =
        new Download(
            proj,
            testCircuitImpName,
            testTickFrequency,
            new BoardReaderClass(AppPreferences.Boards.getBoardFilePath(testCircuitImpBoard))
                .GetBoardInformation(),
            testCircuitImpMapFile,
            false,
            false,
            testCircuitHdlOnly);
    return Downloader.runtty();
  }

  private void loadTemplate(Loader loader, File templFile, boolean templEmpty) {
    if (showSplash) {
      monitor.setProgress(SplashScreen.TEMPLATE_OPEN);
    }
    if (templFile != null) {
      AppPreferences.setTemplateFile(templFile);
      AppPreferences.setTemplateType(AppPreferences.TEMPLATE_CUSTOM);
    } else if (templEmpty) {
      AppPreferences.setTemplateType(AppPreferences.TEMPLATE_EMPTY);
    } else if (templPlain) {
      AppPreferences.setTemplateType(AppPreferences.TEMPLATE_PLAIN);
    }
  }

  public void run() {
    if (isTty) {
      try {
        TtyInterface.run(this);
        System.exit(0);
      } catch (Exception t) {
        t.printStackTrace();
        System.exit(-1);
      }
    }

    // kick off the progress monitor
    // (The values used for progress values are based on a single run where
    // I loaded a large file.)
    if (showSplash) {
      try {
        monitor = new SplashScreen();
        monitor.setVisible(true);
      } catch (Exception t) {
        monitor = null;
        showSplash = false;
      }
    }

    Toolkit.getDefaultToolkit()
        .addAWTEventListener(this, AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);
    // pre-load the two basic component libraries, just so that the time
    // taken is shown separately in the progress bar.
    if (showSplash) {
      monitor.setProgress(SplashScreen.LIBRARIES);
    }
    final var templLoader = new Loader(monitor);
    final var count =
        templLoader.getBuiltin().getLibrary(BaseLibrary._ID).getTools().size()
            + templLoader.getBuiltin().getLibrary(GatesLibrary._ID).getTools().size();
    if (count < 0) {
      // this will never happen, but the optimizer doesn't know that...
      // FIXME: hardcoded string
      logger.error("FATAL ERROR - no components");
      System.exit(-1);
    }

    // load in template
    loadTemplate(templLoader, templFile, templEmpty);

    // now that the splash screen is almost gone, we do some last-minute
    // interface initialization
    if (showSplash) {
      monitor.setProgress(SplashScreen.GUI_INIT);
    }
    WindowManagers.initialize();
    if (MacCompatibility.isSwingUsingScreenMenuBar()) {
      MacCompatibility.setFramelessJMenuBar(new LogisimMenuBar(null, null, null, null));
    } else {
      new LogisimMenuBar(null, null, null, null);
      // most of the time occupied here will be in loading menus, which
      // will occur eventually anyway; we might as well do it when the
      // monitor says we are
    }

    // Make ENTER and SPACE have the same effect for focused buttons.
    UIManager.getDefaults()
        .put(
            "Button.focusInputMap",
            new UIDefaults.LazyInputMap(
                new Object[] {
                  "ENTER", "pressed",
                  "released ENTER", "released",
                  "SPACE", "pressed",
                  "released SPACE", "released"
                }));

    // if user has double-clicked a file to open, we'll
    // use that as the file to open now.
    initialized = true;

    // load file
    if (filesToOpen.isEmpty()) {
      final var proj = ProjectActions.doNew(monitor);
      proj.setStartupScreen(true);
      if (showSplash) {
        monitor.close();
      }
    } else {
      var numOpened = 0;
      var first = true;
      Project proj;
      for (final var fileToOpen : filesToOpen) {
        try {
          if (testVector != null) {
            proj = ProjectActions.doOpenNoWindow(monitor, fileToOpen);
            proj.doTestVector(testVector, circuitToTest);
          } else if (testCircPathInput != null && testCircPathOutput != null) {
            /* This part of the function will create a new circuit file (
             * XML) which will be open and saved again using the  */
            proj = ProjectActions.doOpen(monitor, fileToOpen, substitutions);

            ProjectActions.doSave(proj, new File(testCircPathOutput));
          } else if (testCircuitPathInput != null) {
            /* Testing test bench*/
            final var testB = new TestBench(testCircuitPathInput, monitor, substitutions);

            if (testB.startTestBench()) {
              // FIXME: hardcoded string
              System.out.println("Test bench pass\n");
              System.exit(0);
            } else {
              // FIXME: hardcoded string
              // FIXME: I'd capitalize FAIL to make it stand out.
              System.out.println("Test bench fail\n");
              System.exit(-1);
            }
          } else {
            ProjectActions.doOpen(monitor, fileToOpen, substitutions);
          }
          numOpened++;
        } catch (LoadFailedException ex) {
          logger.error("{} : {}", fileToOpen.getName(), ex.getMessage());
        }
        if (first) {
          first = false;
          if (showSplash) {
            monitor.close();
          }
          monitor = null;
        }
      }
      if (numOpened == 0) System.exit(-1);
    }

    for (final var fileToPrint : filesToPrint) {
      doPrintFile(fileToPrint);
    }

    if (exitAfterStartup) {
      System.exit(0);
    }
  }

  private boolean HasIcon(Component comp) {
    var result = false;
    if (comp instanceof JOptionPane) {
      for (Component comp1 : ((JOptionPane) comp).getComponents()) result |= HasIcon(comp1);
    } else if (comp instanceof JPanel) {
      for (Component comp1 : ((JPanel) comp).getComponents()) result |= HasIcon(comp1);
    } else if (comp instanceof JLabel) {
      return ((JLabel) comp).getIcon() != null;
    }
    return result;
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    if (event instanceof ContainerEvent) {
      final var containerEvent = (ContainerEvent) event;
      if (containerEvent.getID() == ContainerEvent.COMPONENT_ADDED) {
        final var container = containerEvent.getChild();
        if ((container instanceof JButton)
            || (container instanceof JCheckBox)
            || (container instanceof JComboBox)
            || (container instanceof JToolTip)
            || (container instanceof JLabel)
            || (container instanceof JFrame)
            || (container instanceof JMenuItem)
            || (container instanceof JRadioButton)
            || (container instanceof JRadioButtonMenuItem)
            || (container instanceof JProgressBar)
            || (container instanceof JSpinner)
            || (container instanceof JTabbedPane)
            || (container instanceof JTextField)
            || (container instanceof JTextArea)
            || (container instanceof JHelp)
            || (container instanceof JFileChooser)
            || ((container instanceof JScrollPane) && (!(container instanceof CanvasPane)))
            || (container instanceof FontChooser)
            || (container instanceof JCheckBoxMenuItem)) {
          AppPreferences.setScaledFonts(((JComponent) container).getComponents());
          try {
            container.setFont(AppPreferences.getScaledFont(containerEvent.getChild().getFont()));
            container.revalidate();
            container.repaint();
          } catch (Exception ignored) {
          }
        }
        if (container instanceof JOptionPane) {
          final var pane = (JOptionPane) container;
          if (HasIcon(pane)) {
            switch (pane.getMessageType()) {
              case OptionPane.ERROR_MESSAGE:
                pane.setIcon(new ErrorIcon());
                break;
              case OptionPane.QUESTION_MESSAGE:
                pane.setIcon(new QuestionIcon());
                break;
              case OptionPane.INFORMATION_MESSAGE:
                pane.setIcon(new InfoIcon());
                break;
              case OptionPane.WARNING_MESSAGE:
                pane.setIcon(new WarningIcon());
                break;
            }
          }
        }
      }
    }
  }

} // Startup
