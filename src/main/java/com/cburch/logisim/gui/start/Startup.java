/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
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

  /* File contains the data that should be loaded into a RAM/ROM with a label matching the String
     (if no label is provided, File is loaded into every RAM/ROM) */
  private List<Pair<File, String>> memoriesToLoad = new ArrayList<>();

  private File saveFile;
  private int ttyFormat = 0;
  // from other sources
  private boolean initialized = false;
  private SplashScreen monitor = null;
  /* Testing Circuit Variable */
  private String testCircuitPathInput = null;
  /* Test implementation */
  private String testCircuitImpPath = null;
  private boolean doFpgaDownload = false;
  private double testTickFrequency = -1;
  /* Name of the circuit withing logisim */
  private String testCircuitImpName = null;
  /* Name of the board to run on i.e Reptar, MAXV ...*/
  private String testCircuitImpBoard = null;
  /* Path folder containing Map file */
  private final String testCircuitImpMapFile = null;
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

  private static final String ARG_TEST_CIRCUIT_SHORT = "b";
  private static final String ARG_TEST_CIRCUIT_LONG = "test-circuit";
  private static final String ARG_TEST_FGPA_SHORT = "f";
  private static final String ARG_TEST_FGPA_LONG = "test-fpga";
  private static final String ARG_GATES_SHORT = "g";
  private static final String ARG_GATES_LONG = "gates";
  private static final String ARG_HELP_SHORT = "h";
  private static final String ARG_HELP_LONG = "help";
  private static final String ARG_LOAD_SHORT = "l";
  private static final String ARG_LOAD_LONG = "load";
  private static final String ARG_SAVE_LONG = "save";
  private static final String ARG_GEOMETRY_SHORT = "m";
  private static final String ARG_GEOMETRY_LONG = "geometry";
  private static final String ARG_TEST_CIRC_GEN_SHORT = "n";
  private static final String ARG_TEST_CIRC_GEN_LONG = "new-file-format";
  private static final String ARG_LOCALE_SHORT = "o";
  private static final String ARG_LOCALE_LONG = "locale";
  private static final String ARG_CLEAR_PREFS_LONG = "clear-prefs";
  private static final String ARG_SUBSTITUTE_SHORT = "s";
  private static final String ARG_SUBSTITUTE_LONG = "substitute";
  private static final String ARG_TTY_SHORT = "t";
  private static final String ARG_TTY_LONG = "tty";
  private static final String ARG_TEMPLATE_SHORT = "u";
  private static final String ARG_TEMPLATE_LONG = "user-template";
  private static final String ARG_VERSION_SHORT = "v";
  private static final String ARG_VERSION_LONG = "version";
  private static final String ARG_TEST_VECTOR_SHORT = "w";
  private static final String ARG_TEST_VECTOR_LONG = "test-vector";
  private static final String ARG_NO_SPLASH_LONG = "no-splash";
  private static final String ARG_MAIN_CIRCUIT = "toplevel-circuit";

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
    final var positives = List.of("yes", "y", "1", "true", "t");
    final var negatives = List.of("no", "n", "0", "false", "f");
    final var flag = option.toLowerCase();
    if (positives.contains(flag)) return true;
    if (negatives.contains(flag)) return false;
    // FIXME: hardcoded string
    throw new IllegalArgumentException(
            LineBuffer.format("Invalid boolean flag value. Use '{{1}}' for positives and '{{2}}' for negatives.",
                    String.join(", ", positives), String.join(", ", negatives)));
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
    final var formatter = new HelpFormatter();
    formatter.setWidth(100);  // Arbitrary chosen value.
    formatter.printHelp(BuildInfo.name, null, opts, null, true);
    return RC.QUIT;
  }

  /**
   * Prints program version, build Id, compilation date and more.
   *
   * @return Handler return code enum (RC.xxx)
   */
  protected static RC printVersion() {
    System.out.println(BuildInfo.displayName);
    System.out.println(BuildInfo.url);
    System.out.println(LineBuffer.format("{{1}} ({{2}})", BuildInfo.buildId, BuildInfo.dateIso8601));
    System.out.println(LineBuffer.format("{{1}} ({{2}})", BuildInfo.jvm_version, BuildInfo.jvm_vendor));
    return RC.QUIT;
  }

  /**
   * Helper class that simplifies setup of parser argument option.
   *
   * @param opts Instance of {@link Options}.
   * @param stringBaseKey String localization base key.
   * @param longKey Argument ling key (i.e. "foo" for "--foo").
   */
  protected static void addOption(Options opts, String stringBaseKey, String longKey) {
    addOption(opts, stringBaseKey, longKey, null, 0);
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
   * @param longKey Argument ling key (i.e. "foo" for "--foo").
   * @param expectedArgsCount Number of required option arguments.
   */
  protected static void addOption(Options opts, String stringBaseKey, String longKey, int expectedArgsCount) {
    addOption(opts, stringBaseKey, longKey, null, expectedArgsCount);
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
   * @param longKey Argument ling key (i.e. "foo" for "--foo").
   * @param shortKey Argument short key (i.e. "c" for "-c") or null if none.
   * @param expectedArgsCount Number of required option arguments.
   */
  protected static void addOption(Options opts, String stringBaseKey, String longKey, String shortKey, int expectedArgsCount) {
    final var builder = Option.builder(shortKey).longOpt(longKey).desc(S.get(stringBaseKey));
    if (expectedArgsCount == Option.UNLIMITED_VALUES || expectedArgsCount > 0) {
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
  public boolean shallQuit() {
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
    addOption(opts, "argHelpOption", ARG_HELP_LONG, ARG_HELP_SHORT);
    addOption(opts, "argVersionOption", ARG_VERSION_LONG, ARG_VERSION_SHORT);

    // Set up supported arguments for the arg parser to look for.
    // Note: you need to create handler for each option. See handler loop below.
    // It is assumed that evey option always has long-form switch. Short forms are optional.
    addOption(opts, "argTtyOption", ARG_TTY_LONG, ARG_TTY_SHORT, 1);
    addOption(opts, "argTestImplement", ARG_TEST_FGPA_LONG, ARG_TEST_FGPA_SHORT, Option.UNLIMITED_VALUES);  // We can have 3, 4 or 5 arguments here
    addOption(opts, "argClearOption", ARG_CLEAR_PREFS_LONG);
    addOption(opts, "argSubOption", ARG_SUBSTITUTE_LONG, ARG_SUBSTITUTE_SHORT, 2);
    addOption(opts, " ", ARG_LOAD_LONG, ARG_LOAD_SHORT, Option.UNLIMITED_VALUES); // We can have 1 or 2 arguments here
    addOption(opts, "argSaveOption", ARG_SAVE_LONG, 1);
    addOption(opts, "argGatesOption", ARG_GATES_LONG, ARG_GATES_SHORT, 1);
    addOption(opts, "argGeometryOption", ARG_GEOMETRY_LONG, ARG_GEOMETRY_SHORT, 1);
    addOption(opts, "argLocaleOption", ARG_LOCALE_LONG, ARG_LOCALE_SHORT, 1);
    addOption(opts, "argTemplateOption", ARG_TEMPLATE_LONG, ARG_TEMPLATE_SHORT, 1);
    addOption(opts, "argNoSplashOption", ARG_NO_SPLASH_LONG);
    addOption(opts, "argMainCircuitOption", ARG_MAIN_CIRCUIT, 1);
    addOption(opts, "argTestVectorOption", ARG_TEST_VECTOR_LONG, ARG_TEST_VECTOR_SHORT, 2);
    addOption(opts, "argTestCircuitOption", ARG_TEST_CIRCUIT_LONG, ARG_TEST_CIRCUIT_SHORT, 1);     // FIXME add "Option" suffix to key name
    addOption(opts, "argTestCircGenOption", ARG_TEST_CIRC_GEN_LONG, ARG_TEST_CIRC_GEN_SHORT, 2);   // FIXME add "Option" suffix to key name

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
    if (cmd.hasOption(ARG_TTY_SHORT) || cmd.hasOption(ARG_TEST_FGPA_SHORT) || cmd.hasOption(ARG_TEST_FGPA_LONG)) {
      isTty = true;
      Main.headless = true;
    } else {
      shallClearPreferences = cmd.hasOption(ARG_CLEAR_PREFS_LONG);
    }

    if (!isTty) {
      // we're using the GUI: Set up the Look&Feel to match the platform
      System.setProperty("apple.laf.useScreenMenuBar", "true");
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
      final var optHandlerRc = switch (opt.getLongOpt()) {
        case ARG_HELP_LONG -> printHelp(opts);
        case ARG_VERSION_LONG -> printVersion();
        case ARG_TTY_LONG -> handleArgTty(startup, opt);
        case ARG_SUBSTITUTE_LONG -> handleArgSubstitute(startup, opt);
        case ARG_LOAD_LONG -> handleArgLoad(startup, opt);
        case ARG_SAVE_LONG -> handleArgSave(startup, opt);
        case ARG_GATES_LONG -> handleArgGates(startup, opt);
        case ARG_GEOMETRY_LONG -> handleArgGeometry(startup, opt);
        case ARG_LOCALE_LONG -> handleArgLocale(startup, opt);
        case ARG_TEMPLATE_LONG -> handleArgTemplate(startup, opt);
        case ARG_NO_SPLASH_LONG -> handleArgNoSplash(startup, opt);
        case ARG_TEST_VECTOR_LONG -> handleArgTestVector(startup, opt);
        case ARG_TEST_FGPA_LONG -> handleArgTestFpga(startup, opt);
        case ARG_TEST_CIRCUIT_LONG -> handleArgTestCircuit(startup, opt);
        case ARG_TEST_CIRC_GEN_LONG -> handleArgTestCircGen(startup, opt);
        case ARG_MAIN_CIRCUIT -> handleArgMainCircuit(startup, opt);
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
    if (!startup.memoriesToLoad.isEmpty() && !startup.isTty) {
      logger.error(S.get("loadNeedsTtyError"));
      return null;
    }
    if (startup.saveFile != null && !startup.isTty) {
      logger.error(S.get("saveNeedsTtyError"));
      return null;
    }

    return startup;
  }

  /* ********************************************************************************************* */

  /**
   * Supported return codes from command handlers;
   */
  public enum RC {
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
          return RC.QUIT;
        }
        startup.ttyFormat |= val;
        return RC.OK;
      }
    }
    logger.error(S.get("ttyFormatError"));
    return RC.QUIT;
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

    final var optArgs = opt.getValues();

    if (optArgs == null) {
      logger.error(S.get("argLoadInvalidArguments"));
      return RC.QUIT;
    }

    final var argsCnt = optArgs.length;
    if (argsCnt < 1 || argsCnt > 2) {
      logger.error(S.get("argLoadInvalidArguments"));
      return RC.QUIT;
    }

    final var pair = new MutablePair<File, String>();
    pair.left = new File(optArgs[0]);
    if (argsCnt == 2)
      pair.right = optArgs[1];

    startup.memoriesToLoad.add(pair);
    return RC.OK;
  }

  private static RC handleArgSave(Startup startup, Option opt) {
    if (startup.saveFile != null) {
      logger.error(S.get("saveMultipleError"));
      return RC.WARN;
    }
    final var fileName = opt.getValue();
    startup.saveFile = new File(fileName);
    return RC.OK;
  }

  private static RC handleArgGates(Startup startup, Option opt) {
    final var gateShape = opt.getValue().toLowerCase();
    if ("ansi".equals(gateShape)) {
      AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_SHAPED);
      return RC.OK;
    } else if ("iec".equals(gateShape)) {
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

  private static RC handleArgTemplate(Startup startup, Option opt) {
    if (startup.templFile != null || startup.templEmpty || startup.templPlain) {
      logger.error(S.get("argOneTemplateError"));
      return RC.QUIT;
    }
    // first we get the option
    final var option = opt.getValue();
    // we look if it is a file
    final var file = new File(option);
    if (file.exists()) {
      startup.templFile = file;
      if (!startup.templFile.canRead()) {
        logger.error(S.get("templateCannotReadError", file));
        return RC.QUIT;
      }
      return RC.OK;
    }
    // okay, not a file, let's look for empty and plain
    if (option.equalsIgnoreCase("empty")) {
      startup.templEmpty = true;
      return RC.OK;
    }
    if (option.equalsIgnoreCase("plain")) {
      startup.templPlain = true;
      return RC.OK;
    }
    logger.error(S.get("argOneTemplateError"));
    return RC.QUIT;
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

  private static RC handleArgMainCircuit(Startup startup, Option opt) {
    startup.circuitToTest = opt.getValues()[0];
    return RC.OK;
  }


  /**
   * Handles 4th argument of `--test-fpga` argument which can be either string literal
   * or tick frequency.
   *
   * Supported argument formats for `--test-fpga`:<br /><br />
   * * circ_file name board<br />
   * * circ_file name board [HDLONLY]<br />
   * * circ_file name board [HDLONLY] [tick frequency]<br />
   * * circ_file name board [tick_freq]<br />
   * * circ_file name board [tick_freq] [HDLONLY]<br />
   * <br />
   * where:
   * <br /><br />
   * * `circ_file` is *.circ project file to load.<br />
   * * `name` is circuit name present in loaded project file.<br />
   * * `board` is connected FPGA board name.<br />
   * * `tick_freq` is optional tick frequency.<br />
   * * `HDLONLY` (literal), uses HDL only.<br />
   */
  private static RC handleArgTestFpgaParseArg(Startup startup, String argVal) {
    if ("HDLONLY".equals(argVal)) {
      if (!testFpgaFlagTickFreqSet) {
        startup.testCircuitHdlOnly = true;
      }
      return RC.OK;
    }

    int freq;
    try {
      freq = Integer.parseUnsignedInt(argVal);
      if (!testFpgaFlagTickFreqSet) {
        startup.testTickFrequency = freq;
        testFpgaFlagTickFreqSet = true;
      }
      return RC.OK;
    } catch (NumberFormatException ex) {
      // Do nothing here, we fail later.
    }

    logger.error(S.get("argTestUnknownFlagOrValue", String.valueOf(argVal)));
    return RC.QUIT;
  }

  // Indicates if handleArgTestFpgaParseArg() successfuly parsed and set tick freq.
  private static boolean testFpgaFlagTickFreqSet = false;

  private static RC handleArgTestFpga(Startup startup, Option opt) {
    final var optArgs = opt.getValues();

    if (optArgs == null) {
      logger.error(S.get("argTestInvalidArguments"));
      return RC.QUIT;
    }

    final var argsCnt = optArgs.length;
    if (argsCnt < 3 || argsCnt > 5) {
      logger.error(S.get("argTestInvalidArguments"));
      return RC.QUIT;
    }

    // already handled above
    startup.testCircuitImpPath = optArgs[0];
    startup.testCircuitImpName = optArgs[1];
    startup.testCircuitImpBoard = optArgs[2];

    var handlerRc = RC.OK;
    if (argsCnt >= 4) {
      handlerRc = handleArgTestFpgaParseArg(startup, optArgs[3]);
      if (handlerRc == RC.QUIT) return handlerRc;
    }
    if (argsCnt >= 5) {
      handlerRc = handleArgTestFpgaParseArg(startup, optArgs[4]);
      if (handlerRc == RC.QUIT) return handlerRc;
    }

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



  List<Pair<File, String>> getMemoriesToLoad() {
    return memoriesToLoad;
  }


  File getSaveFile() {
    return saveFile;
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

  boolean fpgaDownload(Project proj) {
    /* Testing synthesis */
    final var mainCircuit = proj.getLogisimFile().getCircuit(testCircuitImpName);
    if (mainCircuit == null) return false;
    final var simTickFreq = mainCircuit.getTickFrequency();
    final var downTickFreq = mainCircuit.getDownloadFrequency();
    final var usedFrequency = (testTickFrequency > 0) ? testTickFrequency :
        (downTickFreq > 0) ? downTickFreq : simTickFreq;
    Download downloader =
        new Download(
            proj,
            testCircuitImpName,
            usedFrequency,
            new BoardReaderClass(AppPreferences.Boards.getBoardFilePath(testCircuitImpBoard))
                .getBoardInformation(),
            testCircuitImpMapFile,
            false,
            false,
            testCircuitHdlOnly,
            1.0,
            1.0);
    return downloader.runTty();
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

  private boolean hasIcon(Component comp) {
    var result = false;
    if (comp instanceof JOptionPane pane) {
      for (final var comp1 : pane.getComponents()) result |= hasIcon(comp1);
    } else if (comp instanceof JPanel panel) {
      for (final var comp1 : panel.getComponents()) result |= hasIcon(comp1);
    } else if (comp instanceof JLabel label) {
      return label.getIcon() != null;
    }
    return result;
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    if (event instanceof ContainerEvent containerEvent) {
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
            || (container instanceof JCheckBoxMenuItem)) {
          AppPreferences.setScaledFonts(((JComponent) container).getComponents());
          try {
            container.setFont(AppPreferences.getScaledFont(containerEvent.getChild().getFont()));
            container.revalidate();
            container.repaint();
          } catch (Exception ignored) {
          }
        }
        if (container instanceof final JOptionPane pane) {
          if (hasIcon(pane)) {
            switch (pane.getMessageType()) {
              case OptionPane.ERROR_MESSAGE -> pane.setIcon(new ErrorIcon());
              case OptionPane.QUESTION_MESSAGE -> pane.setIcon(new QuestionIcon());
              case OptionPane.INFORMATION_MESSAGE -> pane.setIcon(new InfoIcon());
              case OptionPane.WARNING_MESSAGE -> pane.setIcon(new WarningIcon());
            }
          }
        }
      }
    }
  }

} // Startup
