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

  public static final String CMD_HELP = "h";
  public static final String CMD_HELP_LONG = "help";
  public static final String CMD_VERSION = "v";
  public static final String CMD_VERSION_LONG = "version";

  public static final String CMD_TTY = "t";
  public static final String CMD_TTY_LONG = "tty";
  public static final String CMD_TEST_FGPA_IMPL = "f";
  public static final String CMD_TEST_FGPA_IMPL_LONG = "test-fpga";
  public static final String CMD_CLEAR_PREFS = "r";
  public static final String CMD_CLEAR_PREFS_LONG = "clear-prefs";
  public static final String CMD_SUBSTITUTE = "s";
  public static final String CMD_SUBSTITUTE_LONG = "substitute";
  public static final String CMD_LOAD = "l";
  public static final String CMD_LOAD_LONG = "load";
  public static final String CMD_EMPTY = "e";
  public static final String CMD_EMPTY_LONG = "empty";
  public static final String CMD_PLAIN = "p";
  public static final String CMD_PLAIN_LONG = "plain";
  public static final String CMD_GATES = "g";
  public static final String CMD_GATES_LONG = "gates";
  public static final String CMD_GEOMETRY = "m";
  public static final String CMD_GEOMETRY_LONG = "geometry";
  public static final String CMD_LOCALE = "o";
  public static final String CMD_LOCALE_LONG = "locale";
  public static final String CMD_ACCENTS = "x";
  public static final String CMD_ACCENTS_LONG = "accents";
  public static final String CMD_TEMPLATE = "z";
  public static final String CMD_TEMPLATE_LONG = "template";
  public static final String CMD_NO_SPLASH = "n";
  public static final String CMD_NO_SPLASH_LONG = "no-splash";
  public static final String CMD_TEST_VECTOR = "tv";
  public static final String CMD_TEST_VECTOR_LONG = "test-vector";
  public static final String CMD_TEST_CIRCUIT = "tc";
  public static final String CMD_TEST_CIRCUIT_LONG = "test-circuit";
  public static final String CMD_TEST_CIRC_GEN = "tg";
  public static final String CMD_TEST_CIRC_GEN_LONG = "test-circ-gen";
  public static final String CMD_CIRCUIT = "c";
  public static final String CMD_CIRCUIT_LONG = "circuit";
  public static final String CMD_ANALYZE = "a";
  public static final String CMD_ANALYZE_LONG = "analyze";
  public static final String CMD_QUESTA = "q";
  public static final String CMD_QUESTA_LONG = "questa";

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
   */
  protected static RC printHelp(Options opts) {
    final var header = Main.APP_DISPLAY_NAME;
    printVersion();
    final var fmt = new HelpFormatter();
    fmt.printHelp(Main.APP_NAME, null, opts, null, true);
    return RC.QUIT;
  }

  protected static RC printVersion() {
    System.out.println(S.get(Main.APP_DISPLAY_NAME));
    System.out.println(S.get("appVersionBuildDate", BuildInfo.dateIso8601));
    System.out.println(S.get("appVersionBuildId", BuildInfo.buildId));
    System.out.println(S.get("appVersionUrl", Main.APP_URL));
    System.out.println();
    return RC.QUIT;
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
    opts.addOption(Option.builder(CMD_HELP).longOpt(CMD_HELP_LONG).desc(S.get("argHelpOption")).build());
    opts.addOption(Option.builder(CMD_VERSION).longOpt(CMD_VERSION_LONG).desc(S.get("argVersionOption")).build());

    opts.addOption(Option.builder(CMD_TTY).longOpt(CMD_TTY_LONG).numberOfArgs(1).desc(S.get("argTtyOption")).build());
    opts.addOption(Option.builder(CMD_TEST_FGPA_IMPL).longOpt(CMD_TEST_FGPA_IMPL_LONG).hasArgs().desc(S.get("argTestImplement")).build());
    opts.addOption(Option.builder(CMD_CLEAR_PREFS).longOpt(CMD_CLEAR_PREFS_LONG).desc(S.get("argClearOption")).build());
    opts.addOption(Option.builder(CMD_SUBSTITUTE).longOpt(CMD_SUBSTITUTE_LONG).numberOfArgs(2).desc(S.get("argSubOption")).build());
    opts.addOption(Option.builder(CMD_LOAD).longOpt(CMD_LOAD_LONG).numberOfArgs(1).desc(S.get("argLoadOption")).build());
    opts.addOption(Option.builder(CMD_EMPTY).longOpt(CMD_EMPTY_LONG).desc(S.get("argEmptyOption")).build());
    opts.addOption(Option.builder(CMD_PLAIN).longOpt(CMD_PLAIN_LONG).desc(S.get("argPlainOption")).build());
    opts.addOption(Option.builder(CMD_GATES).longOpt(CMD_GATES_LONG).numberOfArgs(1).desc(S.get("argGatesOption")).build());
    opts.addOption(Option.builder(CMD_GEOMETRY).longOpt(CMD_GEOMETRY_LONG).numberOfArgs(1).desc(S.get("argGeometryOption")).build());
    opts.addOption(Option.builder(CMD_LOCALE).longOpt(CMD_LOCALE_LONG).numberOfArgs(1).desc(S.get("argLocaleOption")).build());
    opts.addOption(Option.builder(CMD_ACCENTS).longOpt(CMD_ACCENTS_LONG).numberOfArgs(1).desc(S.get("argAccentsOption")).build());
    opts.addOption(Option.builder(CMD_TEMPLATE).longOpt(CMD_TEMPLATE_LONG).numberOfArgs(1).desc(S.get("argTemplateOption")).build());
    opts.addOption(Option.builder(CMD_NO_SPLASH).longOpt(CMD_NO_SPLASH_LONG).desc(S.get("argNoSplashOption")).build());
    opts.addOption(Option.builder(CMD_TEST_VECTOR).longOpt(CMD_TEST_VECTOR_LONG).desc(S.get("argTestVectorOption")).build());   // FIXME: NO LANG STR FOR IT!
    opts.addOption(Option.builder(CMD_TEST_CIRCUIT).longOpt(CMD_TEST_CIRCUIT_LONG).numberOfArgs(1).desc(S.get("argTestCircuit")).build());  // FIXME add "Option" suffix to key name
    opts.addOption(Option.builder(CMD_TEST_CIRC_GEN).longOpt(CMD_TEST_CIRC_GEN_LONG).numberOfArgs(2).desc(S.get("argTestCircGen")).build());  // FIXME add "Option" suffix to key name
    opts.addOption(Option.builder(CMD_ANALYZE).longOpt(CMD_ANALYZE_LONG).numberOfArgs(1).desc(S.get("argAnalyzeOption")).build());
    opts.addOption(Option.builder(CMD_QUESTA).longOpt(CMD_QUESTA_LONG).numberOfArgs(1).desc(S.get("argQuestaOption")).build());

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
    if (cmd.hasOption(CMD_TTY) || cmd.hasOption(CMD_TEST_FGPA_IMPL)) {
      isTty = true;
      Main.headless = true;
    } else {
      shallClearPreferences = cmd.hasOption(CMD_CLEAR_PREFS);
    }

    if (!isTty) {
      // we're using the GUI: Set up the Look&Feel to match the platform
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      LocaleManager.setReplaceAccents(false);
      // Initialize graphics acceleration if appropriate
      AppPreferences.handleGraphicsAcceleration();
    }

    // Initialize startup object.
    final var ret = new Startup(isTty);
    startupTemp = ret;
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
      System.out.println(opt.getLongOpt());
      final var optHandlerRc = switch (opt.getOpt()) {
        case CMD_HELP -> printHelp(opts);
        case CMD_VERSION -> printVersion();
        case CMD_TTY -> cmdTty(ret, opt);
        case CMD_SUBSTITUTE -> cmdSubstitute(ret, opt);
        case CMD_LOAD -> cmdLoad(ret, opt);
        case CMD_EMPTY -> cmdEmpty(ret, opt);
        case CMD_PLAIN -> cmdPlain(ret, opt);
        case CMD_GATES -> cmdGates(ret, opt);
        case CMD_GEOMETRY -> cmdGeometry(ret, opt);
        case CMD_LOCALE -> cmdLocale(ret, opt);
        case CMD_ACCENTS -> cmdAccents(ret, opt);
        case CMD_TEMPLATE -> cmdTemplate(ret, opt);
        case CMD_NO_SPLASH -> cmdNoSplash(ret, opt);
        case CMD_TEST_VECTOR -> cmdTestVector(ret, opt);
        case CMD_TEST_FGPA_IMPL -> cmdTestFpgaImpl(ret, opt);
        case CMD_TEST_CIRCUIT -> cmdTestCircuit(ret, opt);
        case CMD_TEST_CIRC_GEN -> cmdTestCircGen(ret, opt);
        case CMD_CIRCUIT -> cmdCircuit(ret, opt);
        case CMD_ANALYZE -> cmdAnalyze(ret, opt);
        case CMD_QUESTA -> cmdQuesta(ret, opt);
        default -> RC.OK; // should not really happen IRL.
      };
      if (optHandlerRc == RC.QUIT) return null;
    }

    // FIXME: not implemented yet
    // positional argument being files to load
    // ret.filesToOpen.add(new File(arg));

    if (ret.exitAfterStartup && ret.filesToOpen.isEmpty()) {
      printHelp(opts);
      return null;
    }
    if (ret.isTty && ret.filesToOpen.isEmpty()) {
      logger.error(S.get("ttyNeedsFileError"));
      return null;
    }
    if (ret.loadFile != null && !ret.isTty) {
      logger.error(S.get("loadNeedsTtyError"));
      return null;
    }

    return ret;
  }

  /* ********************************************************************************************* */

  /**
   * Supported return codes from command handlers;
   */
  public static enum RC {
    /**
     * Handler completed succesfuly.
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

  private static RC cmdTty(Startup startup, Option opt) {
    // TTY format parsing
    final var ttyVal = opt.getValue();
    final var fmts = ttyVal.split(",");
    if (fmts.length > 0) {
      // FIXME: why we support multiple TTY typesw?
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

  private static RC cmdSubstitute(Startup startup, Option opt) {
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

  private static RC cmdLoad(Startup startup, Option opt) {
    if (startup.loadFile != null) {
      logger.error(S.get("loadMultipleError"));
      // FIXME: shouldn't we quit here? -> RC.QUIT;
      return RC.WARN;
    }
    final var fileName = opt.getValue();
    startup.loadFile = new File(fileName);
    return RC.OK;
  }

  private static RC cmdEmpty(Startup startup, Option opt) {
    if (startup.templFile != null || startup.templEmpty || startup.templPlain) {
      logger.error(S.get("argOneTemplateError"));
      return RC.QUIT;
    }
    startup.templEmpty = true;
    return RC.OK;
  }

  private static RC cmdPlain(Startup ret, Option opt) {
    if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
      logger.error(S.get("argOneTemplateError"));
      return RC.QUIT;
    }
    ret.templPlain = true;
    return RC.OK;
  }

  private static RC cmdGates(Startup ret, Option opt) {
    final var gateShape = opt.getValue();
    if (gateShape.equals("shaped")) {
      AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_SHAPED);
    } else if (gateShape.equals("rectangular")) {
      AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_RECTANGULAR);
    } else {
      logger.error(S.get("argGatesOptionError"));
      return RC.QUIT;
    }
    return RC.OK;
  }

  private static RC cmdGeometry(Startup ret, Option opt) {
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
    if (loc != null) AppPreferences.WINDOW_LOCATION.set(x + "," + y);

    return RC.OK;
  }

  private static RC cmdLocale(Startup ret, Option opt) {
    final var locale = opt.getValue();
    setLocale(locale);
    return RC.OK;
  }

  private static RC cmdAccents(Startup ret, Option opt) {
    final var flag = opt.getValue().toLowerCase();
    try {
      AppPreferences.ACCENTS_REPLACE.setBoolean(!parseBool(flag));
    } catch (IllegalArgumentException ex) {
      logger.error(S.get("argAccentsOptionError"));
      return RC.QUIT;
    }
    return RC.OK;
  }

  private static RC cmdTemplate(Startup ret, Option opt) {
    if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
      logger.error(S.get("argOneTemplateError"));
      return RC.QUIT;
    }
    final var file = opt.getValue();
    ret.templFile = new File(file);
    String errMsg = null;
    if (!ret.templFile.exists()) errMsg = S.get("templateMissingError", file);
    if (!ret.templFile.canRead()) errMsg = S.get("templateCannotReadError", file);

    if (errMsg == null) {
      return RC.OK;
    }
    // FIXME: shouldn't we quit in such case?
    return RC.WARN;
  }

  private static RC cmdNoSplash(Startup ret, Option opt) {
    ret.showSplash = false;
    return RC.OK;
  }

  private static RC cmdTestVector(Startup ret, Option opt) {
    ret.circuitToTest = opt.getValues()[0];
    ret.testVector = opt.getValues()[1];
    ret.showSplash = false;
    ret.exitAfterStartup = true;
    // This is to test a test bench. It will return 0 or 1 depending on if the tests pass or not.
    return RC.OK;
  }

  private static RC cmdTestFpgaImpl(Startup ret, Option opt) {
    final var optArgs = opt.getValues();

    // already handled above
    ret.testCircuitImpPath = optArgs[0];
    ret.testCircuitImpMapFile = optArgs[1];
    ret.testCircuitImpName = optArgs[2];
    ret.testCircuitImpBoard = optArgs[3];

    if (optArgs.length > 4) {
      try {
        ret.testTickFrequency = Integer.parseUnsignedInt(optArgs[4]);
      } catch (NumberFormatException ignored) {
        // FIXME: do nothing, but that's not the best error handlong
      }
      if (optArgs.length > 5) {
        ret.testCircuitHdlOnly = optArgs[5].equalsIgnoreCase("HDLONLY");
      }
    }

    ret.doFpgaDownload = true;
    ret.showSplash = false;
    ret.filesToOpen.add(new File(ret.testCircuitImpPath));
    return RC.OK;
  }

  private static RC cmdTestCircuit(Startup ret, Option opt) {
    final var fileName = opt.getValue();
    ret.testCircuitPathInput = fileName;
    ret.filesToOpen.add(new File(fileName));
    ret.showSplash = false;
    ret.exitAfterStartup = true;
    return RC.OK;
  }

  private static RC cmdTestCircGen(Startup ret, Option opt) {
    final var optArgs = opt.getValues();
    // This is to test the XML consistency over different version of the Logisim
    // This is the input path of the file to open
    ret.testCircPathInput = optArgs[0];
    ret.filesToOpen.add(new File(ret.testCircPathInput));
    // This is the output file's path. The comparaison shall be done between the  testCircPathInput and the testCircPathOutput
    ret.testCircPathOutput = optArgs[1];
    ret.showSplash = false;
    ret.exitAfterStartup = true;
    return RC.OK;
  }

  private static RC cmdCircuit(Startup ret, Option opt) {
    ret.circuitToTest = opt.getValue();
    return RC.OK;
  }

  private static RC cmdAnalyze(Startup ret, Option opt) {
    Main.ANALYZE = true;
    return RC.OK;
  }

  private static RC cmdQuesta(Startup ret, Option opt) {
    try {
      final var flag = opt.getValue().toLowerCase();
      AppPreferences.QUESTA_VALIDATION.setBoolean(parseBool(flag));
      return RC.OK;
    } catch (IllegalArgumentException ex) {
      logger.error(S.get("argQuestaOptionError"));
    }
    return RC.QUIT;
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
