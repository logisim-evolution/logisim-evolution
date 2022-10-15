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

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.std.base.BaseLibrary;
import com.cburch.logisim.std.gates.GatesLibrary;
import com.cburch.logisim.util.LineBuffer;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.MacCompatibility;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Startup {

  static final Logger logger = LoggerFactory.getLogger(Startup.class);

  public enum UI { NONE, TTY, GUI };

  // shared options (TODO should these be shared?)
  public final ArrayList<File> filesToOpen = new ArrayList<>();
  public final HashMap<File, File> substitutions = new HashMap<>();
  public UI ui = UI.NONE;
  public int exitCode = 0;

  // Gui only options
  public String gateShape = null;
  public Point windowSize = null;
  public Point windowLocation = null;
  public final ArrayList<File> filesToPrint = new ArrayList<>();
  public File templFile = null;
  public boolean templEmpty = false;
  public boolean templPlain = false;
  public boolean showSplash = true;
  public boolean clearPreferences = false;
  public boolean initialized = false; // TODO remove this
  
  // Tty only options
  public String testVector = null;
  public String circuitToTest = null;
  public File loadFile;
  public File saveFile;
  public int ttyFormat = 0;
  public String testCircuitPathInput = null;
  public String testCircuitImpPath = null;
  public String testCircPathInput = null;
  public String testCircPathOutput = null;
  public boolean doFpgaDownload = false;
  public double testTickFrequency = -1;
  /* Name of the circuit within logisim */
  public String testCircuitImpName = null;
  /* Name of the board to run on i.e Reptar, MAXV ...*/
  public String testCircuitImpBoard = null;
  /* Path folder containing Map file */
  public final String testCircuitImpMapFile = null;
  /* Indicate if only the HDL should be generated */
  public boolean testCircuitHdlOnly = false;
  // Indicates if handleArgTestFpgaParseArg() successfuly parsed and set tick freq.
  public boolean testFpgaFlagTickFreqSet = false;

  /**
   * Parses CLI arguments, report any errors, and fill public members for use by 
   * TtyInterface or GuiInterface.
   * 
   * If .ui == UI.NONE, exit w/ .exitCode.
   * If .ui == UI.TTY, TtyInterface.run(startup).
   * If .ui == UI.GUI, GuiInterface.run(startup).
   *
   * @param args CLI arguments
   */
  public Startup(String[] args) {

    final var opts = new Options();

    // Set up supported arguments for the arg parser to look for.
    // Note: you need to create handler for each option. See handler loop below.
    // It is assumed that evey option always has long-form switch. Short forms are optional.
    addOption(opts, "argHelpOption", ARG_HELP_LONG, ARG_HELP_SHORT);
    addOption(opts, "argVersionOption", ARG_VERSION_LONG, ARG_VERSION_SHORT);
    addOption(opts, "argTtyOption", ARG_TTY_LONG, ARG_TTY_SHORT, 1);
    addOption(opts, "argTestImplement", ARG_TEST_FGPA_LONG, ARG_TEST_FGPA_SHORT, Option.UNLIMITED_VALUES);  // We can have 3, 4 or 5 arguments here
    addOption(opts, "argClearOption", ARG_CLEAR_PREFS_LONG);
    addOption(opts, "argSubOption", ARG_SUBSTITUTE_LONG, ARG_SUBSTITUTE_SHORT, 2);
    addOption(opts, "argLoadOption", ARG_LOAD_LONG, ARG_LOAD_SHORT, 1);
    addOption(opts, "argSaveOption", ARG_SAVE_LONG, 1);
    addOption(opts, "argGatesOption", ARG_GATES_LONG, ARG_GATES_SHORT, 1);
    addOption(opts, "argGeometryOption", ARG_GEOMETRY_LONG, ARG_GEOMETRY_SHORT, 1);
    addOption(opts, "argLocaleOption", ARG_LOCALE_LONG, ARG_LOCALE_SHORT, 1);
    addOption(opts, "argTemplateOption", ARG_TEMPLATE_LONG, ARG_TEMPLATE_SHORT, 1);
    addOption(opts, "argNoSplashOption", ARG_NO_SPLASH_LONG);
    addOption(opts, "argMainCircuitOption", ARG_MAIN_CIRCUIT, 1);
    addOption(opts, "argTestVectorOption", ARG_TEST_VECTOR_LONG, ARG_TEST_VECTOR_SHORT, 2);
    addOption(opts, "argTestCircuitOption", ARG_TEST_CIRCUIT_LONG, ARG_TEST_CIRCUIT_SHORT, 1);
    addOption(opts, "argTestCircGenOption", ARG_TEST_CIRC_GEN_LONG, ARG_TEST_CIRC_GEN_SHORT, 2);

    CommandLine cmd;
    try {
      cmd = (new DefaultParser()).parse(opts, args);
    } catch (UnrecognizedOptionException ex) {
      // FIXME: hardcoded string
      logger.error("Unrecognized option: '" + ex + ".);");
      logger.error("Use --help for more info.");
      cmd = null;
    } catch (ParseException ex) {
      // FIXME: hardcoded string
      logger.error("Failed processing command line arguments.");
      cmd = null;
    }

    if (cmd == null) {
      exitCode = 1;
      return;
    }

    // Iterate over parsed arguments and invoke option handler
    // for each detected argument.
    for (var opt : cmd.getOptions()) {
      // Note: you should have handler for each option. So number of `case`s
      // here should equal number of calls to `addOption()` above.
      final var optHandlerRc = switch (opt.getLongOpt()) {
        case ARG_HELP_LONG -> printHelp(opts);
        case ARG_VERSION_LONG -> printVersion();
        case ARG_TTY_LONG -> handleArgTty(opt);
        case ARG_SUBSTITUTE_LONG -> handleArgSubstitute(opt);
        case ARG_LOAD_LONG -> handleArgLoad(opt);
        case ARG_SAVE_LONG -> handleArgSave(opt);
        case ARG_GATES_LONG -> handleArgGates(opt);
        case ARG_GEOMETRY_LONG -> handleArgGeometry(opt);
        case ARG_LOCALE_LONG -> handleArgLocale(opt);
        case ARG_TEMPLATE_LONG -> handleArgTemplate(opt);
        case ARG_NO_SPLASH_LONG -> handleArgNoSplash();
        case ARG_TEST_VECTOR_LONG -> handleArgTestVector(opt);
        case ARG_TEST_FGPA_LONG -> handleArgTestFpga(opt);
        case ARG_TEST_CIRCUIT_LONG -> handleArgTestCircuit(opt);
        case ARG_TEST_CIRC_GEN_LONG -> handleArgTestCircGen(opt);
        case ARG_MAIN_CIRCUIT -> handleArgMainCircuit(opt);
        case ARG_CLEAR_PREFS_LONG -> handleArgClearPreferences();
        default -> RC.EXIT; // should never happen
      };
      switch (optHandlerRc) {
        case EXIT:
          exitCode = 0;
          return;
        case IO_ERROR: 
          exitCode = 2;
          return;
        case CLI_ERROR: 
          exitCode = 1;
          return;
        case OK: 
          break;
      }
    }

    ui = cmd.hasOption(ARG_TTY_SHORT) 
      || cmd.hasOption(ARG_TEST_FGPA_SHORT) 
      || cmd.hasOption(ARG_TEST_VECTOR_SHORT) 
      || cmd.hasOption(ARG_TEST_CIRCUIT_SHORT) 
      || cmd.hasOption(ARG_TEST_CIRC_GEN_LONG) ? UI.TTY : UI.GUI;

    // positional arguments are files to load
    for (final var arg : cmd.getArgs()) {
      filesToOpen.add(new File(arg));
    }

    // check for combinations of options that are not considered legal

    if (loadFile != null && ui != UI.TTY) {
      logger.error(S.get("loadNeedsTtyError"));
      ui = UI.NONE;
      exitCode = 1;
      return;
    }
    if (saveFile != null && ui != UI.TTY) {
      logger.error(S.get("saveNeedsTtyError"));
      ui = UI.NONE;
      exitCode = 1;
      return;
    }

    if (ui == UI.TTY && filesToOpen.size() != 1) {
      logger.error(S.get("ttyNeedsFileError"));
      ui = UI.NONE;
      exitCode = 1;
      return;
    }

    // TTY && --gates is meaningless
    // TTY && --geometry is meaningless
    // TTY && --user-template is meaningless
    // is GUI && --substitutions ok?
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
  protected RC printHelp(Options opts) {
    printVersion();
    System.out.println();
    final var formatter = new HelpFormatter();
    formatter.setWidth(100);  // Arbitrary chosen value.
    formatter.printHelp(BuildInfo.name, null, opts, null, true);
    return RC.EXIT;
  }

  /**
   * Prints program version, build Id, compilation date and more.
   *
   * @return Handler return code enum (RC.xxx)
   */
  protected RC printVersion() {
    System.out.println(BuildInfo.displayName);
    System.out.println(BuildInfo.url);
    System.out.println(LineBuffer.format("{{1}} ({{2}})", BuildInfo.buildId, BuildInfo.dateIso8601));
    System.out.println(LineBuffer.format("{{1}} ({{2}})", BuildInfo.jvm_version, BuildInfo.jvm_vendor));
    return RC.EXIT;
  }

  /**
   * Helper class that simplifies setup of parser argument option.
   *
   * @param opts Instance of {@link Options}.
   * @param stringBaseKey String localization base key.
   * @param longKey Argument ling key (i.e. "foo" for "--foo").
   */
  protected void addOption(Options opts, String stringBaseKey, String longKey) {
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
  protected void addOption(Options opts, String stringBaseKey, String longKey, int expectedArgsCount) {
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
  protected void addOption(Options opts, String stringBaseKey, String longKey, String shortKey, int expectedArgsCount) {
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
  protected void addOption(Options opts, String stringBaseKey, String shortKey, String longKey) {
    addOption(opts, stringBaseKey, shortKey, longKey, 0);
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
     * Unrecoverable I/O error occured.
     */
    IO_ERROR,
    /**
     * Unrecoverable error occured while handling option.
     */
    CLI_ERROR,
    /** 
     * Handler requests an immediate exit.
     */
    EXIT
  }

  private RC handleArgTty(Option opt) {
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
          return RC.CLI_ERROR;
        }
        ttyFormat |= val;
        return RC.OK;
      }
    }
    logger.error(S.get("ttyFormatError"));
    return RC.CLI_ERROR;
  }

  private RC handleArgSubstitute(Option opt) {
    final var fileA = new File(opt.getValues()[0]);
    final var fileB = new File(opt.getValues()[1]);
    if (!substitutions.containsKey(fileA)) {
      substitutions.put(fileA, fileB);
      return RC.OK;
    }

    logger.error(S.get("argDuplicateSubstitutionError"));
    return RC.CLI_ERROR;
  }

  private RC handleArgLoad(Option opt) {
    if (loadFile != null) {
      logger.error(S.get("loadMultipleError"));
      return RC.CLI_ERROR;
    }
    final var fileName = opt.getValue();
    loadFile = new File(fileName);
    return RC.OK;
  }

  private RC handleArgSave(Option opt) {
    if (saveFile != null) {
      logger.error(S.get("saveMultipleError"));
      return RC.CLI_ERROR;
    }
    final var fileName = opt.getValue();
    saveFile = new File(fileName);
    return RC.OK;
  }

  private RC handleArgGates(Option opt) {
    final var shape = opt.getValue().toLowerCase();
    if ("ansi".equals(shape)) {
      gateShape = AppPreferences.SHAPE_SHAPED;
      return RC.OK;
    } else if ("iec".equals(shape)) {
      gateShape = AppPreferences.SHAPE_RECTANGULAR;
      return RC.OK;
    }

    logger.error(S.get("argGatesOptionError"));
    return RC.CLI_ERROR;
  }

  private RC handleArgGeometry(Option opt) {
    final var geometry = opt.getValue();
    final var wxh = geometry.split("[xX]");

    if (wxh.length != 2 || wxh[0].length() < 1 || wxh[1].length() < 1) {
      logger.error(S.get("argGeometryError"));
      return RC.CLI_ERROR;
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
        return RC.CLI_ERROR;
      }
      try {
        x = Integer.parseInt(xy[0]);
        y = Integer.parseInt(xy[1]);
      } catch (NumberFormatException e) {
        logger.error(S.get("argGeometryError"));
        return RC.CLI_ERROR;
      }
    }

    var w = 0;
    var h = 0;
    try {
      w = Integer.parseInt(wxh[0]);
      h = Integer.parseInt(wxh[1]);
    } catch (NumberFormatException e) {
      logger.error(S.get("argGeometryError"));
      return RC.CLI_ERROR;
    }
    if (w <= 0 || h <= 0) {
      logger.error(S.get("argGeometryError"));
      return RC.CLI_ERROR;
    }
    windowSize = new Point(w,h);
    if (loc != null) windowLocation = new Point(x,y);
    return RC.OK;
  }

  private RC handleArgLocale(Option opt) {
    String lang = opt.getValue();
    final var opts = S.getLocaleOptions();
    for (final var locale : opts) {
      if (lang.equals(locale.toString())) {
        LocaleManager.setLocale(locale);
        return RC.OK;
      }
    }
    logger.error(S.get("invalidLocaleError"));
    logger.error(S.get("invalidLocaleOptionsHeader"));
    for (final var o : opts) {
      logger.error("   {}", o.toString());
    }
    return RC.CLI_ERROR;
  }

  private RC handleArgTemplate(Option opt) {
    // duplicates are not allowed
    if (templFile != null || templEmpty || templPlain) {
      logger.error(S.get("argOneTemplateError"));
      return RC.CLI_ERROR;
    }

    final var option = opt.getValue();
    // we look if it is a file
    final var file = new File(option);
    if (file.exists()) {
      templFile = file;
      if (!templFile.canRead()) {
        logger.error(S.get("templateCannotReadError", file));
        return RC.IO_ERROR;
      }
      return RC.OK;
    }
    // okay, not a file, let's look for empty and plain
    if (option.equalsIgnoreCase("empty")) {
      templEmpty = true;
      return RC.OK;
    }
    if (option.equalsIgnoreCase("plain")) {
      templPlain = true;
      return RC.OK;
    }

    logger.error(S.get("argOneTemplateError"));
    return RC.CLI_ERROR;
  }

  private RC handleArgNoSplash() {
    showSplash = false;
    return RC.OK;
  }

  private RC handleArgTestVector(Option opt) {
    // This is to test a test bench. It will return 0 or 1 depending on if the tests pass or not.
    circuitToTest = opt.getValues()[0];
    testVector = opt.getValues()[1];
    return RC.OK;
  }

  private RC handleArgMainCircuit(Option opt) {
    circuitToTest = opt.getValues()[0];
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
  private RC handleArgTestFpgaParseArg(String argVal) {
    if ("HDLONLY".equals(argVal)) {
      if (!testFpgaFlagTickFreqSet) {
        testCircuitHdlOnly = true;
      }
      return RC.OK;
    }

    int freq;
    try {
      freq = Integer.parseUnsignedInt(argVal);
      if (!testFpgaFlagTickFreqSet) {
        testTickFrequency = freq;
        testFpgaFlagTickFreqSet = true;
      }
      return RC.OK;
    } catch (NumberFormatException ex) {
      // Do nothing here, we fail later.
    }

    logger.error(S.get("argTestUnknownFlagOrValue", String.valueOf(argVal)));
    return RC.CLI_ERROR;
  }

  private RC handleArgTestFpga(Option opt) {
    final var optArgs = opt.getValues();

    if (optArgs == null) {
      logger.error(S.get("argTestInvalidArguments"));
      return RC.CLI_ERROR;
    }

    final var argsCnt = optArgs.length;
    if (argsCnt < 3 || argsCnt > 5) {
      logger.error(S.get("argTestInvalidArguments"));
      return RC.CLI_ERROR;
    }

    testCircuitImpPath = optArgs[0];
    testCircuitImpName = optArgs[1];
    testCircuitImpBoard = optArgs[2];

    if (argsCnt >= 4) {
      RC rc = handleArgTestFpgaParseArg(optArgs[3]);
      if (rc != RC.OK) return rc;
    }
    if (argsCnt >= 5) {
      RC rc = handleArgTestFpgaParseArg(optArgs[4]);
      if (rc != RC.OK) return rc;
    }

    doFpgaDownload = true;
    filesToOpen.add(new File(testCircuitImpPath));
    return RC.OK;
  }

  private RC handleArgTestCircuit(Option opt) {
    final var fileName = opt.getValue();
    testCircuitPathInput = fileName;
    filesToOpen.add(new File(fileName)); // TODO fix this, fileToOpen AND(!) testCircuitPathInput
    return RC.OK;
  }

  private RC handleArgTestCircGen(Option opt) {
    // This is to test the XML consistency over different versions of the Logisim.
    final var optArgs = opt.getValues();
    // This is the input path of the file to open
    testCircPathInput = optArgs[0];
    filesToOpen.add(new File(testCircPathInput)); // TODO fix this, fileToOpen AND(!) testCircPathInput
    // This is the output file's path. The comparaison shall be done between the testCircPathInput and the testCircPathOutput
    testCircPathOutput = optArgs[1];
    return RC.OK;
  }

  private RC handleArgClearPreferences() {
    clearPreferences = true;
    return RC.OK;
  }
}
