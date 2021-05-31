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

import com.cburch.logisim.LogisimVersion;
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
import com.cburch.logisim.util.ArgonXML;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.MacCompatibility;
import com.cburch.logisim.util.StringUtil;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import javax.swing.ProgressMonitor;
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
  
  private static int parseTtyFormat(String fmt)
  {
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
    boolean isTty = false;
    boolean isClearPreferences = false;
    for (String value : args) {
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

    Startup ret = new Startup(isTty);
    startupTemp = ret;
    if (!isTty) {
      registerHandler();
    }

    if (isClearPreferences) {
      AppPreferences.clear();
    }

    if (AppPreferences.FirstTimeStartup.getBoolean() & !isTty) {
      System.out.println("First time startup");
      int Result =
          OptionPane.showConfirmDialog(
              null,
              "Logisim can automatically check for new updates and versions.\n"
                  + "Would you like to enable this feature?\n"
                  + "(This feature can be disabled in Window -> Preferences -> Software)\n",
              "Autoupdate",
              OptionPane.YES_NO_OPTION);
      if (Result == OptionPane.YES_OPTION) AppPreferences.AutomaticUpdateCheck.setBoolean(true);
      AppPreferences.FirstTimeStartup.set(false);
    }

    // parse arguments
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.equals("-tty")) {
        if (i + 1 < args.length) {
          i++;
          String[] fmts = args[i].split(",");
          if (fmts.length == 0) {
            logger.error("{}", S.get("ttyFormatError"));
          }
          for (String s : fmts) {
            String fmt = s.trim();
            int val = parseTtyFormat(fmt);
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
          File a = new File(args[i + 1]);
          File b = new File(args[i + 2]);
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
        System.out.println(Main.VERSION_NAME); // OK
        return null;
      } else if (arg.equals("-gates")) {
        i++;
        if (i >= args.length) {
          printUsage();
        }
        String a = args[i];
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
        String[] wxh = args[i].split("[xX]");
        if (wxh.length != 2 || wxh[0].length() < 1 || wxh[1].length() < 1) {
          logger.error("{}", S.get("argGeometryError"));
          System.exit(1);
        }
        int p = wxh[1].indexOf('+', 1);
        String loc = null;
        int x = 0, y = 0;
        if (p >= 0) {
          loc = wxh[1].substring(p + 1);
          wxh[1] = wxh[1].substring(0, p);
          String[] xy = loc.split("\\+");
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
        int w = 0, h = 0;
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
        String a = args[i];
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
          logger.error("{}", StringUtil.format(S.get("templateMissingError"), args[i]));
        } else if (!ret.templFile.canRead()) {
          logger.error("{}", StringUtil.format(S.get("templateCannotReadError"), args[i]));
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
      } else if (arg.equals("-noupdates")) {
        AppPreferences.AutomaticUpdateCheck.setBoolean(false);
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
    System.err.println(StringUtil.format(S.get("argUsage"), Startup.class.getName())); // OK
    System.err.println(); // OK
    System.err.println(S.get("argOptionHeader")); // OK
    System.err.println("   " + S.get("argNoUpdatesOption")); // OK
    System.err.println("   " + S.get("argGeometryOption")); // OK
    System.err.println("   " + S.get("argAccentsOption")); // OK
    System.err.println("   " + S.get("argClearOption")); // OK
    System.err.println("   " + S.get("argEmptyOption")); // OK
    System.err.println("   " + S.get("argAnalyzeOption")); // OK
    System.err.println("   " + S.get("argTestOption")); // OK
    System.err.println("   " + S.get("argGatesOption")); // OK
    System.err.println("   " + S.get("argHelpOption")); // OK
    System.err.println("   " + S.get("argLoadOption")); // OK
    System.err.println("   " + S.get("argLocaleOption")); // OK
    System.err.println("   " + S.get("argNoSplashOption")); // OK
    System.err.println("   " + S.get("argPlainOption")); // OK
    System.err.println("   " + S.get("argSubOption")); // OK
    System.err.println("   " + S.get("argTemplateOption")); // OK
    System.err.println("   " + S.get("argTtyOption")); // OK
    System.err.println("   " + S.get("argQuestaOption")); // OK
    System.err.println("   " + S.get("argVersionOption")); // OK
    System.err.println("   " + S.get("argTestCircGen")); // OK
    System.err.println("   " + S.get("argTestCircuit")); // OK
    System.err.println("   " + S.get("argTestImplement")); // OK
    System.err.println("   " + S.get("argCircuitOption")); // OK

    System.exit(-1);
  }

  private static void registerHandler() {
    MacOsAdapter.addListeners();
  }

  private static void setLocale(String lang) {
    Locale[] opts = S.getLocaleOptions();
    for (Locale locale : opts) {
      if (lang.equals(locale.toString())) {
        LocaleManager.setLocale(locale);
        return;
      }
    }
    logger.warn("{}", S.get("invalidLocaleError"));
    logger.warn("{}", S.get("invalidLocaleOptionsHeader"));

    for (Locale opt : opts) {
      logger.warn("   {}", opt.toString());
    }
    System.exit(-1);
  }

  /**
   * Auto-update Logisim-evolution if a new version is available
   *
   * <p>Original idea taken from Jupar: http://masterex.github.io/archive/2011/12/25/jupar.html by
   * Periklis Master_ex Ntanasis <pntanasis@gmail.com>
   *
   * @return true if the code has been updated, and therefore the execution has to be stopped, false
   *     otherwise
   */
  public boolean autoUpdate() {
    if (!AppPreferences.AutomaticUpdateCheck.getBoolean()) return false;
    ProgressMonitor Monitor =
        new ProgressMonitor(null, "Checking for new logisim version", "Autoupdate", 0, 4);
    Monitor.setProgress(0);
    Monitor.setMillisToPopup(0);
    Monitor.setMillisToDecideToPopup(0);
    if (!networkConnectionAvailable()) {
      Monitor.close();
      return false;
    }
    Monitor.setProgress(1);
    // Get the remote XML file containing the current version
    URL xmlURL;
    try {
      xmlURL = new URL(Main.UPDATE_URL);
    } catch (MalformedURLException e) {
      logger.error(
          "The URL of the XML file for the auto-updater is malformed.\nPlease report this error to the software maintainer\n-- AUTO-UPDATE ABORTED --");
      Monitor.close();
      return (false);
    }
    URLConnection conn;
    try {
      conn = xmlURL.openConnection();
    } catch (IOException e) {
      logger.error(
          "Although an Internet connection should be available, the system couldn't connect to the URL requested by the auto-updater\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
      Monitor.close();
      return (false);
    }
    InputStream in;
    try {
      in = conn.getInputStream();
    } catch (IOException e) {
      logger.error(
          "Although an Internet connection should be available, the system couldn't retrieve the data requested by the auto-updater.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
      Monitor.close();
      return (false);
    }
    ArgonXML logisimData = new ArgonXML(in, "logisim-evolution");
    Monitor.setProgress(2);

    // Get the appropriate remote version number
    LogisimVersion remoteVersion = LogisimVersion.parse(logisimData.child("version").content());

    // If the remote version is newer, perform the update
    Monitor.setProgress(3);
    if (remoteVersion.compareTo(Main.VERSION) > 0) {
      int answer =
          OptionPane.showConfirmDialog(
              null,
              "A new Logisim-evolution version ("
                  + remoteVersion
                  + ") is available!\nWould you like to update?",
              "Update",
              OptionPane.YES_NO_OPTION,
              OptionPane.INFORMATION_MESSAGE);

      if (answer == 1) {
        // User refused to update -- we just hope he gets sufficiently
        // annoyed by the message that he finally updates!
        Monitor.close();
        return (false);
      }

      // Obtain the base directory of the jar archive
      CodeSource codeSource = Startup.class.getProtectionDomain().getCodeSource();
      File jarFile = null;
      try {
        jarFile = new File(codeSource.getLocation().toURI().getPath());
      } catch (URISyntaxException e) {
        logger.error(
            "Error in the syntax of the URI for the path of the executed Logisim-evolution JAR file!");
        e.printStackTrace();
        OptionPane.showMessageDialog(
            null,
            "An error occurred while updating to the new Logisim-evolution version.\nPlease check the console for log information.",
            "Update failed",
            OptionPane.ERROR_MESSAGE);
        Monitor.close();
        return (false);
      }

      // Get the appropriate remote filename to download
      String remoteJar = logisimData.child("file").content();

      boolean updateOk = downloadInstallUpdatedVersion(remoteJar, jarFile.getAbsolutePath());

      if (updateOk) {
        OptionPane.showMessageDialog(
            null,
            "The new Logisim-evolution version ("
                + remoteVersion
                + ") has been correctly installed.\nPlease restart Logisim-evolution for the changes to take effect.",
            "Update succeeded",
            OptionPane.INFORMATION_MESSAGE);
        Monitor.close();
        return (true);
      } else {
        OptionPane.showMessageDialog(
            null,
            "An error occurred while updating to the new Logisim-evolution version.\nPlease check the console for log information.",
            "Update failed",
            OptionPane.ERROR_MESSAGE);
        Monitor.close();
        return (false);
      }
    }
    Monitor.close();
    return (false);
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
      Project toPrint = ProjectActions.doOpen(null, null, file);
      Print.doPrint(toPrint);
      toPrint.getFrame().dispose();
    } else {
      filesToPrint.add(file);
    }
  }

  /**
   * Download a new version of Logisim, according to the instructions received from autoUpdate(),
   * and install it at the specified location
   *
   * <p>Original idea taken from: http://baptiste-wicht.developpez.com/tutoriels/java/update/ by
   * Baptiste Wicht
   *
   * @param filePath remote file URL
   * @param destination local destination for the updated Jar file
   * @return true if the new version has been downloaded and installed, false otherwise
   * @throws IOException
   */
  private boolean downloadInstallUpdatedVersion(String filePath, String destination) {
    URL fileURL;
    try {
      fileURL = new URL(filePath);
    } catch (MalformedURLException e) {
      logger.error(
          "The URL of the requested update file is malformed.\nPlease report this error to the software maintainer.\n-- AUTO-UPDATE ABORTED --");
      return (false);
    }
    URLConnection conn;
    try {
      conn = fileURL.openConnection();
    } catch (IOException e) {
      logger.error(
          "Although an Internet connection should be available, the system couldn't connect to the URL of the updated file requested by the auto-updater.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
      return (false);
    }

    // Get remote file size
    int length = conn.getContentLength();
    if (length == -1) {
      logger.error(
          "Cannot retrieve the file containing the updated version.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
      return (false);
    }

    // Get remote file stream
    InputStream is;
    try {
      is = new BufferedInputStream(conn.getInputStream());
    } catch (IOException e) {
      logger.error(
          "Cannot get remote file stream.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
      return (false);
    }

    // Local file buffer
    byte[] data = new byte[length];

    // Helper variables for marking the current position in the downloaded
    // file
    int currentBit = 0;
    int deplacement = 0;

    // Download remote content
    try {
      while (deplacement < length) {
        currentBit = is.read(data, deplacement, data.length - deplacement);

        if (currentBit == -1) {
          // Reached EOF
          break;
        }
        deplacement += currentBit;
      }
    } catch (IOException e) {
      logger.error(
          "An error occured while retrieving remote file (remote peer hung up).\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
      return (false);
    }
    // Close remote stream
    try {
      is.close();
    } catch (IOException e) {
      logger.error("Error encountered while closing the remote stream!");
      e.printStackTrace();
    }

    // If not all the bytes have been retrieved, abort update
    if (deplacement != length) {
      logger.error(
          "An error occured while retrieving remote file (local size != remote size), download corrupted.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
      return (false);
    }

    // Open stream for local Jar and write data
    FileOutputStream destinationFile;
    try {
      destinationFile = new FileOutputStream(destination);
    } catch (FileNotFoundException e) {
      logger.error("An error occured while opening the local Jar file.\n-- AUTO-UPDATE ABORTED --");
      return (false);
    }
    try {
      destinationFile.write(data);
      destinationFile.flush();
    } catch (IOException e) {
      logger.error(
          "An error occured while writing to the local Jar file.\n-- AUTO-UPDATE ABORTED --\nThe local file might be corrupted. If this is the case, please download a new copy of Logisim.");
    } finally {
      try {
        destinationFile.close();
      } catch (IOException e) {
        logger.error(
            "Error encountered while closing the local destination file!\nThe local file might be corrupted. If this is the case, please download a new copy of Logisim.");
        return (false);
      }
    }

    return (true);
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
            new BoardReaderClass(AppPreferences.Boards.GetBoardFilePath(testCircuitImpBoard))
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

  /**
   * Check if network connection is available.
   *
   * <p>This function tries to connect to google in order to test the availability of a network
   * connection. This step is needed before attempting to perform an auto-update. It assumes that
   * google is accessible -- usually this is the case, and it should also provide a quick reply to
   * the connection attempt, reducing the lag.
   *
   * @return true if the connection is available, false otherwise
   */
  private boolean networkConnectionAvailable() {
    try {
      URL url = new URL("http://www.google.com");
      URLConnection uC = url.openConnection();
      uC.connect();
      return (true);
    } catch (MalformedURLException e) {
      logger.error("The URL used to check the connectivity is malformed -- no Google?");
      e.printStackTrace();
    } catch (IOException e) {
      // If we get here, the connection somehow failed
      return (false);
    }
    return (false);
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
    Loader templLoader = new Loader(monitor);
    int count =
        templLoader.getBuiltin().getLibrary("Base").getTools().size()
            + templLoader.getBuiltin().getLibrary("Gates").getTools().size();
    if (count < 0) {
      // this will never happen, but the optimizer doesn't know that...
      logger.error("FATAL ERROR - no components"); // OK
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
      Project proj = ProjectActions.doNew(monitor);
      proj.setStartupScreen(true);
      if (showSplash) {
        monitor.close();
      }
    } else {
      int numOpened = 0;
      boolean first = true;
      Project proj;
      for (File fileToOpen : filesToOpen) {
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
            TestBench testB = new TestBench(testCircuitPathInput, monitor, substitutions);

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

    for (File fileToPrint : filesToPrint) {
      doPrintFile(fileToPrint);
    }

    if (exitAfterStartup) {
      System.exit(0);
    }
  }

  private boolean HasIcon(Component comp) {
    boolean result = false;
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
      ContainerEvent containerEvent = (ContainerEvent) event;
      if (containerEvent.getID() == ContainerEvent.COMPONENT_ADDED) {
        Component container = containerEvent.getChild();
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
          JOptionPane pane = (JOptionPane) container;
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
