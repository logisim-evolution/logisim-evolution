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

  private static int parseTtyFormat(String fmt) {
    return switch (fmt) {
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
  }

  public static final String CMD_HELP = "h";
  public static final String CMD_HELP_LONG = "help";
  public static final String CMD_TTY = "tty";
  public static final String CMD_TEST_FGPA_IMPL = "tfi";
  public static final String CMD_TEST_FGPA_IMPL_LONG = "test-fpga-implementation";
  // NOTE: changed from clearprefs
  public static final String CMD_CLEAR_PREFS = "cprefs";
  public static final String CMD_CLEAR_PREFS_LONG = "clear-prefs";
  // NOTE: changed from clearprops
  public static final String CMD_CLEAR_PROPS = "cprops";
  public static final String CMD_CLEAR_PROPS_LONG = "clear-props";
  public static final String CMD_SUB = "sub";
  public static final String CMD_LOAD = "load";
  public static final String CMD_EMPTY = "empty";
  public static final String CMD_PLAIN = "plain";
  public static final String CMD_VERSION = "version";
  public static final String CMD_GATES = "gates";
  // FIXME: change to "geometry"
  public static final String CMD_GEOMETRY = "geom";
  public static final String CMD_LOCALE = "locale";
  public static final String CMD_ACCENTS = "accents";
  public static final String CMD_TEMPLATE = "template";
  // NOTE: changed from no-splash
  public static final String CMD_NO_SPLASH = "ns";
  public static final String CMD_NO_SPLASH_LONG = "no-splash";
  // NOTE: changed from testvector
  public static final String CMD_TEST_VECTOR = "tv";
  public static final String CMD_TEST_VECTOR_LONG = "test-vector";
  public static final String CMD_TEST_CIRCUIT = "tc";
  public static final String CMD_TEST_CIRCUIT_LONG = "test-circuit";
  public static final String CMD_TEST_CIRC_GEN = "tcgen";
  public static final String CMD_TEST_CIRC_GEN_LONG = "test-circ-gen";
  public static final String CMD_CIRCUIT = "circuit";
  public static final String CMD_ANALYZE = "analyze";
  public static final String CMD_QUESTA = "questa";

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
    if (flag.equals("yes")) {
      return true;
    } else if (flag.equals("no")) {
      return false;
    }

    throw new IllegalArgumentException("Invalid boolean flag. Use 'yes' or 'no'.");
  }


  /**
   * Prints available command line options.
   * @param opts Configured CLI options.
   */
  protected static void printHelp(Options opts) {
    final var header = Main.APP_DISPLAY_NAME;
    final var footer = Main.APP_URL;
    (new HelpFormatter()).printHelp(Main.APP_NAME, header, opts, footer, true);
  }

  /**
   * Parses CLI arguments
   *
   * @param args CLI arguments
   *
   * @return Instance of Startup class.
   */
  public static Startup parseArgs(String[] args) {
    final var opts = (new Options()).addOption(Option.builder(CMD_HELP).longOpt(CMD_HELP_LONG).build());
    opts.addOption(Option.builder(CMD_TTY).longOpt(CMD_TEST_FGPA_IMPL_LONG).numberOfArgs(1).desc(S.get("argTtyOption")).build());
    opts.addOption(Option.builder(CMD_TEST_FGPA_IMPL).hasArgs().desc(S.get("argTestImplement")).build());
    opts.addOption(Option.builder(CMD_CLEAR_PREFS).longOpt(CMD_CLEAR_PREFS_LONG).desc(S.get("argClearOption")).build());
    opts.addOption(Option.builder(CMD_CLEAR_PROPS).longOpt(Startup.CMD_CLEAR_PROPS_LONG).desc(S.get("argClearProps")).build());  // FIXME: NO LANG STR FOR IT!
    opts.addOption(Option.builder(CMD_SUB).numberOfArgs(2).desc(S.get("argSubOption")).build());
    opts.addOption(Option.builder(CMD_LOAD).numberOfArgs(1).desc(S.get("argLoadOption")).build());
    opts.addOption(Option.builder(CMD_EMPTY).desc(S.get("argEmptyOption")).build());
    opts.addOption(Option.builder(CMD_PLAIN).desc(S.get("argPlainOption")).build());
    opts.addOption(Option.builder(CMD_VERSION).desc(S.get("argVersionOption")).build());
    opts.addOption(Option.builder(CMD_GATES).numberOfArgs(1).desc(S.get("argGatesOption")).build());
    opts.addOption(Option.builder(CMD_GEOMETRY).numberOfArgs(1).desc(S.get("argGeometryOption")).build());
    opts.addOption(Option.builder(CMD_ACCENTS).numberOfArgs(1).desc(S.get("argAccentsOption")).build());
    opts.addOption(Option.builder(CMD_TEMPLATE).numberOfArgs(1).desc(S.get("argTemplateOption")).build());
    opts.addOption(Option.builder(CMD_NO_SPLASH).longOpt(CMD_NO_SPLASH_LONG).desc(S.get("argNoSplashOption")).build());
    opts.addOption(Option.builder(CMD_TEST_VECTOR).longOpt(CMD_TEST_VECTOR_LONG).desc(S.get("argTestVectorOption")).build());   // FIXME: NO LANG STR FOR IT!

    // FIXME definition for multiple args is wrong here!
    opts.addOption(Option.builder(CMD_TEST_CIRCUIT).numberOfArgs(1).desc(S.get("argTestCircuit")).build());  // FIXME add "Option" suffix to key name

    opts.addOption(Option.builder(CMD_TEST_CIRC_GEN).longOpt(CMD_TEST_CIRC_GEN_LONG).numberOfArgs(2).desc(S.get("argTestCircGen")).build());  // FIXME add "Option" suffix to key name
    opts.addOption(Option.builder(CMD_TEST_CIRCUIT).longOpt(CMD_TEST_CIRCUIT_LONG).numberOfArgs(1).desc(S.get("argCircuitOption")).build());
    opts.addOption(Option.builder(CMD_ANALYZE).numberOfArgs(1).desc(S.get("argAnalyzeOption")).build());
    opts.addOption(Option.builder(CMD_QUESTA).numberOfArgs(1).desc(S.get("argQuestaOption")).build());

    CommandLine cmd;
    try {
      final var parser = new DefaultParser();
      cmd = parser.parse(opts, args);
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
      // FIXME why we have two switches for the same? Gonna remove one. Which?
      shallClearPreferences = (cmd.hasOption(CMD_CLEAR_PREFS) || cmd.hasOption(CMD_CLEAR_PROPS));
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

    // CMD_HELP
    if (cmd.hasOption(CMD_HELP)) {
      printHelp(opts);
      return null;
    } // End of CMD_HELP

    // VERSION
    if (cmd.hasOption(CMD_VERSION)) {
      System.out.println(Main.APP_DISPLAY_NAME);
      return null;
    }  // End of VERSION

    // TTY format parsing
    if (cmd.hasOption(CMD_TTY)) {
      final var ttyVal = cmd.getOptionValue(CMD_TTY);

      final var fmts = ttyVal.split(",");
      if (fmts.length > 0) {
        for (final var singleFmt : fmts) {
          final var val = parseTtyFormat(singleFmt.trim());
          if (val == 0) {
            logger.error(S.get("ttyFormatError"));
            // FIXME: Shouldn't we exit here -> return null;
            continue;
          }
          ret.ttyFormat |= val;
        }
      } else {
        logger.error(S.get("ttyFormatError"));
        // FIXME: Shouldn't we exit here -> return null;
      }
    } // end of TTY format parsing

    // SUB command args parsing
    if (cmd.hasOption(CMD_SUB)) {
      final var fileA = cmd.getOptionValues(CMD_SUB)[0];
      final var fileB = cmd.getOptionValues(CMD_SUB)[1];
      final var a = new File(fileA);
      final var b = new File(fileB);
      if (ret.substitutions.containsKey(a)) {
        logger.error(S.get("argDuplicateSubstitutionError"));
        return null;
      } else {
        ret.substitutions.put(a, b);
      }
      // in case less than 2 args are given? but that's handled by cli
//      logger.error(S.get("argTwoSubstitutionError"));
//      return null;
    } // End of SUB command args parsing

    // LOAD file
    if (cmd.hasOption(CMD_LOAD)) {
      final var fileName = cmd.getOptionValue(CMD_LOAD);
      if (ret.loadFile != null) {
        logger.error(S.get("loadMultipleError"));
        // FIXME: shouldn't we quit here? -> return null;
      }
      ret.loadFile = new File(fileName);

      // TODO: remove this string as it should not be handled by us any more?
//      logger.error(S.get("loadNeedsFileError"));
//      return null;

    } // End of LOAD file

    // EMPTY
    if (cmd.hasOption(CMD_EMPTY)) {
      if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
        logger.error(S.get("argOneTemplateError"));
        return null;
      }
      ret.templEmpty = true;
    } // End of EMPTY

    // PLAIN
    if (cmd.hasOption(CMD_PLAIN)) {
      if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
        logger.error(S.get("argOneTemplateError"));
        return null;
      }
      ret.templPlain = true;
    } // End of PLAIN

    // GATES
    if (cmd.hasOption(CMD_GATES)) {
      final var gateShape = cmd.getOptionValue(CMD_GATES);
      if (gateShape.equals("shaped")) {
        AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_SHAPED);
      } else if (gateShape.equals("rectangular")) {
        AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_RECTANGULAR);
      } else {
        logger.error(S.get("argGatesOptionError"));
        return null;
      }
    } // End of GATES

    // GEOMETRY
    if (cmd.hasOption(CMD_GEOMETRY)) {
      final var geometry = cmd.getOptionValue(CMD_GEOMETRY);
      final var wxh = geometry.split("[xX]");
      if (wxh.length != 2 || wxh[0].length() < 1 || wxh[1].length() < 1) {
        logger.error(S.get("argGeometryError"));
        return null;
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
          return null;
        }
        try {
          x = Integer.parseInt(xy[0]);
          y = Integer.parseInt(xy[1]);
        } catch (NumberFormatException e) {
          logger.error(S.get("argGeometryError"));
          return null;
        }
      }
      var w = 0;
      var h = 0;
      try {
        w = Integer.parseInt(wxh[0]);
        h = Integer.parseInt(wxh[1]);
      } catch (NumberFormatException e) {
        logger.error(S.get("argGeometryError"));
        return null;
      }
      if (w <= 0 || h <= 0) {
        logger.error(S.get("argGeometryError"));
        return null;
      }
      AppPreferences.WINDOW_WIDTH.set(w);
      AppPreferences.WINDOW_HEIGHT.set(h);
      if (loc != null) AppPreferences.WINDOW_LOCATION.set(x + "," + y);
    } // End of GEOMETRY

    // LOCALE
    if (cmd.hasOption(CMD_LOCALE)) {
      final var locale = cmd.getOptionValue(CMD_LOCALE);
      setLocale(locale);
    } // End of LOCALE

    // ACCENTS
    if (cmd.hasOption(CMD_ACCENTS)) {
      final var flag = cmd.getOptionValue(CMD_ACCENTS).toLowerCase();
      try {
        AppPreferences.ACCENTS_REPLACE.setBoolean(!parseBool(flag));
      } catch (IllegalArgumentException ex) {
        logger.error(S.get("argAccentsOptionError"));
        return null;
      }
    } // End of ACCENTS

    // TEMPLATE
    if (cmd.hasOption(CMD_TEMPLATE)) {
      if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
        logger.error(S.get("argOneTemplateError"));
        return null;
      }
      final var file = cmd.getOptionValue(CMD_TEMPLATE);
      ret.templFile = new File(file);
      if (!ret.templFile.exists()) {
        logger.error(S.get("templateMissingError", file));
      } else if (!ret.templFile.canRead()) {
        logger.error(S.get("templateCannotReadError", file));
      }
    } // End of TEMPLATE

    // NO_SPLASH
    if (cmd.hasOption(CMD_NO_SPLASH)) {
      ret.showSplash = false;
    } // End of NO_SPLASH

    // TEST_VECTOR
    if (cmd.hasOption(CMD_TEST_VECTOR)) {
      ret.circuitToTest = cmd.getOptionValues(CMD_TEST_VECTOR)[0];
      ret.testVector = cmd.getOptionValues(CMD_TEST_VECTOR)[1];
      ret.showSplash = false;
      ret.exitAfterStartup = true;
      // This is to test a test bench. It will return 0 or 1 depending on if the tests pass or not.
    } // End of TEST_VECTOR

    // TEST_FPGA_IMPL
    if (cmd.hasOption(CMD_TEST_FGPA_IMPL)) {
      final var optArgs = cmd.getOptionValues(CMD_TEST_FGPA_IMPL);

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
    } // End of TEST_FPGA_IMPL

    // TEST_CIRCUIT
    if (cmd.hasOption(CMD_TEST_CIRCUIT)) {
      ret.testCircuitPathInput = cmd.getOptionValue(CMD_TEST_CIRCUIT);
      ret.filesToOpen.add(new File(ret.testCircuitPathInput));
      ret.showSplash = false;
      ret.exitAfterStartup = true;
    } // End of TEST_CIRCUIT

    // TEST_CIRC_GEN
    if (cmd.hasOption(CMD_TEST_CIRC_GEN)) {
      final var optArgs = cmd.getOptionValues(CMD_TEST_CIRC_GEN);
      // This is to test the XML consistency over different version of the Logisim
      // This is the input path of the file to open
      ret.testCircPathInput = optArgs[0];
      // This is the output file's path. The comparaison shall be done between the  testCircPathInput and the testCircPathOutput
      ret.testCircPathOutput = optArgs[1];
      ret.filesToOpen.add(new File(ret.testCircPathInput));
      ret.showSplash = false;
      ret.exitAfterStartup = true;
    } // End of TEST_CIRC_GEN

    // CMD_CIRCUIT
    if (cmd.hasOption(CMD_CIRCUIT)) {
      ret.circuitToTest = cmd.getOptionValue(CMD_CIRCUIT);
    } // End of CMD_CIRCUIT

    // CMD_ANALYZE
    Main.ANALYZE = cmd.hasOption(CMD_ANALYZE);

    // CMD_QUESTA
    if (cmd.hasOption(CMD_QUESTA)) {
      try {
        final var flag = cmd.getOptionValue(CMD_QUESTA).toLowerCase();
        AppPreferences.QUESTA_VALIDATION.setBoolean(parseBool(flag));
      } catch (IllegalArgumentException ex) {
        logger.error(S.get("argQuestaOptionError"));
        return null;
      }
    } // End of CMD_QUESTA

    // FIXME: not implemented yet
    // positional argument being files to load
    // ret.filesToOpen.add(new File(arg));


    if (ret.exitAfterStartup && ret.filesToOpen.isEmpty()) {
      // FIXME: use CLI's method
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
}
