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
  private double testTickFrequency = -1;
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
    switch (fmt) {
      case "table":
        return TtyInterface.FORMAT_TABLE;
      case "speed":
        return TtyInterface.FORMAT_SPEED;
      case "tty":
        return TtyInterface.FORMAT_TTY;
      case "halt":
        return TtyInterface.FORMAT_HALT;
      case "stats":
        return TtyInterface.FORMAT_STATISTICS;
      case "binary":
        return TtyInterface.FORMAT_TABLE_BIN;
      case "hex":
        return TtyInterface.FORMAT_TABLE_HEX;
      case "csv":
        return TtyInterface.FORMAT_TABLE_CSV;
      case "tabs":
        return TtyInterface.FORMAT_TABLE_TABBED;
      default:
        return 0;
    }
  }

  public static Startup parseArgs(String[] args) {
    // see whether we'll be using any graphics
    var isTty = false;
    var isClearPreferences = false;
    for (final var value : args) {
      if (value.equals("-tty") || value.equals("-test-fpga-implementation")) {
        isTty = true;
        Main.headless = true;
      } else if (value.equals("-clearprefs") || value.equals("-clearprops")) {
        isClearPreferences = true;
      }
    }

    if (!isTty) {
      // we're using the GUI: Set up the Look&Feel to match the platform
      System.setProperty("apple.laf.useScreenMenuBar", "true");

      LocaleManager.setReplaceAccents(false);

      // Initialize graphics acceleration if appropriate
      AppPreferences.handleGraphicsAcceleration();
    }

    final var ret = new Startup(isTty);
    startupTemp = ret;
    if (!isTty) {
      registerHandler();
    }

    if (isClearPreferences) {
      AppPreferences.clear();
    }

    // parse arguments
    for (var i = 0; i < args.length; i++) {
      final var arg = args[i];
      if (arg.equals("-tty")) {
        if (i + 1 < args.length) {
          i++;
          final var fmts = args[i].split(",");
          if (fmts.length == 0) {
            logger.error("{}", S.get("ttyFormatError"));
          }
          for (final var s : fmts) {
            final var fmt = s.trim();
            final var val = parseTtyFormat(fmt);
            if (val == 0)
              logger.error("{}", S.get("ttyFormatError"));
            else
              ret.ttyFormat |= val;
          }
        } else {
          logger.error("{}", S.get("ttyFormatError"));
          return null;
        }
      } else if (arg.equals("-sub")) {
        if (i + 2 < args.length) {
          final var a = new File(args[i + 1]);
          final var b = new File(args[i + 2]);
          if (ret.substitutions.containsKey(a)) {
            logger.error("{}", S.get("argDuplicateSubstitutionError"));
            return null;
          } else {
            ret.substitutions.put(a, b);
            i += 2;
          }
        } else {
          logger.error("{}", S.get("argTwoSubstitutionError"));
          return null;
        }
      } else if (arg.equals("-load")) {
        if (i + 1 < args.length) {
          i++;
          if (ret.loadFile != null) {
            logger.error("{}", S.get("loadMultipleError"));
          }
          ret.loadFile = new File(args[i]);
        } else {
          logger.error("{}", S.get("loadNeedsFileError"));
          return null;
        }
      } else if (arg.equals("-empty")) {
        if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
          logger.error("{}", S.get("argOneTemplateError"));
          return null;
        }
        ret.templEmpty = true;
      } else if (arg.equals("-plain")) {
        if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
          logger.error("{}", S.get("argOneTemplateError"));
          return null;
        }
        ret.templPlain = true;
      } else if (arg.equals("-version")) {
        System.out.println(Main.VERSION);
        return null;
      } else if (arg.equals("-gates")) {
        i++;
        if (i >= args.length) {
          printUsage();
        }
        final var a = args[i];
        if (a.equals("shaped")) {
          AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_SHAPED);
        } else if (a.equals("rectangular")) {
          AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_RECTANGULAR);
        } else {
          logger.error("{}", S.get("argGatesOptionError"));
          System.exit(-1);
        }
      } else if (arg.equals("-geom")) {
        i++;
        if (i >= args.length) {
          printUsage();
        }
        final var wxh = args[i].split("[xX]");
        if (wxh.length != 2 || wxh[0].length() < 1 || wxh[1].length() < 1) {
          logger.error("{}", S.get("argGeometryError"));
          System.exit(1);
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
            logger.error("{}", S.get("argGeometryError"));
            System.exit(1);
          }
          try {
            x = Integer.parseInt(xy[0]);
            y = Integer.parseInt(xy[1]);
          } catch (NumberFormatException e) {
            logger.error("{}", S.get("argGeometryError"));
            System.exit(1);
          }
        }
        var w = 0;
        var h = 0;
        try {
          w = Integer.parseInt(wxh[0]);
          h = Integer.parseInt(wxh[1]);
        } catch (NumberFormatException e) {
          logger.error("{}", S.get("argGeometryError"));
          System.exit(1);
        }
        if (w <= 0 || h <= 0) {
          logger.error("{}", S.get("argGeometryError"));
          System.exit(1);
        }
        AppPreferences.WINDOW_WIDTH.set(w);
        AppPreferences.WINDOW_HEIGHT.set(h);
        if (loc != null) AppPreferences.WINDOW_LOCATION.set(x + "," + y);
      } else if (arg.equals("-locale")) {
        i++;
        if (i >= args.length) {
          printUsage();
        }
        setLocale(args[i]);
      } else if (arg.equals("-accents")) {
        i++;
        if (i >= args.length) {
          printUsage();
        }
        final var a = args[i];
        if (a.equals("yes")) {
          AppPreferences.ACCENTS_REPLACE.setBoolean(false);
        } else if (a.equals("no")) {
          AppPreferences.ACCENTS_REPLACE.setBoolean(true);
        } else {
          logger.error("{}", S.get("argAccentsOptionError"));
          System.exit(-1);
        }
      } else if (arg.equals("-template")) {
        if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
          logger.error("{}", S.get("argOneTemplateError"));
          return null;
        }
        i++;
        if (i >= args.length) {
          printUsage();
        }
        ret.templFile = new File(args[i]);
        if (!ret.templFile.exists()) {
          logger.error("{}", S.get("templateMissingError", args[i]));
        } else if (!ret.templFile.canRead()) {
          logger.error("{}", S.get("templateCannotReadError", args[i]));
        }
      } else if (arg.equals("-nosplash")) {
        ret.showSplash = false;
      } else if (arg.equals("-testvector")) {
        i++;

        if (i >= args.length) printUsage();

        ret.circuitToTest = args[i];
        i++;

        if (i >= args.length) printUsage();

        ret.testVector = args[i];
        ret.showSplash = false;
        ret.exitAfterStartup = true;
        /* This is to test a test bench. It will return 0 or 1 depending on if
         * the tests pass or not
         */
      } else if (arg.equals("-test-fpga-implementation")) {
        // already handled above
        i++;
        if (i >= args.length) printUsage();

        ret.testCircuitImpPath = args[i];
        i++;
        if (i >= args.length) printUsage();

        if (args[i].toUpperCase().endsWith("MAP.XML")) {
          ret.testCircuitImpMapFile = args[i];
          i++;
          if (i >= args.length) printUsage();
        }

        ret.testCircuitImpName = args[i];
        i++;

        if (i >= args.length) printUsage();

        ret.testCircuitImpBoard = args[i];
        i++;
        if (i < args.length) {
          if (!args[i].startsWith("-")) {
            try {
              ret.testTickFrequency = Integer.parseUnsignedInt(args[i]);
              i++;
            } catch (NumberFormatException ignored) {
              // do nothing
            }
            if (i < args.length) {
              if (!args[i].startsWith("-")) {
                if (args[i].equalsIgnoreCase("HDLONLY")) ret.testCircuitHdlOnly = true;
                else printUsage();
              } else i--;
            }
          } else i--;
        }
        ret.doFpgaDownload = true;
        ret.showSplash = false;
        ret.filesToOpen.add(new File(ret.testCircuitImpPath));
      } else if (arg.equals("-test-circuit")) {
        // already handled above
        i++;
        if (i >= args.length) printUsage();

        ret.testCircuitPathInput = args[i];
        ret.filesToOpen.add(new File(ret.testCircuitPathInput));
        ret.showSplash = false;
        ret.exitAfterStartup = true;
      } else if (arg.equals("-test-circ-gen")) {
        /* This is to test the XML consistency over different version of
         * the Logisim */
        i++;

        if (i >= args.length) printUsage();

        /* This is the input path of the file to open */
        ret.testCircPathInput = args[i];
        i++;
        if (i >= args.length) printUsage();

        /* This is the output file's path. The comparaison shall be
         * done between the  testCircPathInput and the testCircPathOutput*/
        ret.testCircPathOutput = args[i];
        ret.filesToOpen.add(new File(ret.testCircPathInput));
        ret.showSplash = false;
        ret.exitAfterStartup = true;
      } else if (arg.equals("-circuit")) {
        i++;
        if (i >= args.length) printUsage();
        ret.circuitToTest = args[i];
      } else if (arg.equals("-clearprefs") || arg.equals("-clearprops")) {
        // already handled above
      } else if (arg.equals("-analyze")) {
        Main.ANALYZE = true;
      } else if (arg.equals("-questa")) {
        i++;
        if (i >= args.length) {
          printUsage();
        }
        String a = args[i];
        if (a.equals("yes")) {
          AppPreferences.QUESTA_VALIDATION.setBoolean(true);
        } else if (a.equals("no")) {
          AppPreferences.QUESTA_VALIDATION.setBoolean(false);
        } else {
          logger.error("{}", S.get("argQuestaOptionError"));
          System.exit(-1);
        }
      } else if (arg.charAt(0) == '-') {
        printUsage();
        return null;
      } else {
        ret.filesToOpen.add(new File(arg));
      }
    }

    if (ret.exitAfterStartup && ret.filesToOpen.isEmpty()) {
      printUsage();
    }
    if (ret.isTty && ret.filesToOpen.isEmpty()) {
      logger.error("{}", S.get("ttyNeedsFileError"));
      return null;
    }
    if (ret.loadFile != null && !ret.isTty) {
      logger.error("{}", S.get("loadNeedsTtyError"));
      return null;
    }

    return ret;
  }

  private static void printUsage() {
    System.err.println(S.get("argUsage", Startup.class.getName()));
    System.err.println();
    System.err.println(S.get("argOptionHeader"));
    String[] opts = {
      "argGeometryOption",
      "argAccentsOption",
      "argClearOption",
      "argEmptyOption",
      "argAnalyzeOption",
      "argTestOption",
      "argGatesOption",
      "argHelpOption",
      "argLoadOption",
      "argLocaleOption",
      "argNoSplashOption",
      "argPlainOption",
      "argSubOption",
      "argTemplateOption",
      "argTtyOption",
      "argQuestaOption",
      "argVersionOption",
      "argTestCircGen",
      "argTestCircuit",
      "argTestImplement",
      "argCircuitOption",
    };
    for (final var opt : opts) {
      System.err.println("   " + S.get(opt));
    }

    System.exit(0);
  }

  private static void registerHandler() {
    MacOsAdapter.addListeners();
  }

  private static void setLocale(String lang) {
    final var opts = S.getLocaleOptions();
    for (final var locale : opts) {
      if (lang.equals(locale.toString())) {
        LocaleManager.setLocale(locale);
        return;
      }
    }
    logger.warn("{}", S.get("invalidLocaleError"));
    logger.warn("{}", S.get("invalidLocaleOptionsHeader"));

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
    final var mainCircuit = proj.getLogisimFile().getCircuit(testCircuitImpName);
    if (mainCircuit == null) return false;
    final var simTickFreq = mainCircuit.getTickFrequency();
    final var downTickFreq = mainCircuit.getDownloadFrequency();
    final var usedFrequency = (testTickFrequency > 0) ? testTickFrequency : 
        (downTickFreq > 0) ? downTickFreq : simTickFreq;
    Download Downloader =
        new Download(
            proj,
            testCircuitImpName,
            usedFrequency,
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
              System.out.println("Test bench pass\n");
              System.exit(0);
            } else {
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
