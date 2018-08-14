/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.gui.start;

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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bfh.logisim.fpgagui.FPGACommanderTests;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.Main;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.gui.generic.CanvasPane;
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
import com.connectina.swing.fontchooser.JFontChooser;

public class Startup implements AWTEventListener {

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

	public static Startup parseArgs(String[] args) {
		// see whether we'll be using any graphics
		boolean isTty = false;
		boolean isClearPreferences = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-tty")) {
				isTty = true;
			} else if (args[i].equals("-clearprefs")
					|| args[i].equals("-clearprops")) {
				isClearPreferences = true;
			}
		}

		if (!isTty) {
			// we're using the GUI: Set up the Look&Feel to match the platform
			System.setProperty(
					"com.apple.mrj.application.apple.menu.about.name",
					"Logisim-evolution");
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

		// parse arguments
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-tty")) {
				if (i + 1 < args.length) {
					i++;
					String[] fmts = args[i].split(",");
					if (fmts.length == 0) {
						logger.error("{}", Strings.get("ttyFormatError"));
					}
					for (int j = 0; j < fmts.length; j++) {
						String fmt = fmts[j].trim();
						if (fmt.equals("table")) {
							ret.ttyFormat |= TtyInterface.FORMAT_TABLE;
						} else if (fmt.equals("speed")) {
							ret.ttyFormat |= TtyInterface.FORMAT_SPEED;
						} else if (fmt.equals("tty")) {
							ret.ttyFormat |= TtyInterface.FORMAT_TTY;
						} else if (fmt.equals("halt")) {
							ret.ttyFormat |= TtyInterface.FORMAT_HALT;
						} else if (fmt.equals("stats")) {
							ret.ttyFormat |= TtyInterface.FORMAT_STATISTICS;
						} else {
							logger.error("{}", Strings.get("ttyFormatError"));
						}
					}
				} else {
					logger.error("{}", Strings.get("ttyFormatError"));
					return null;
				}
			} else if (arg.equals("-sub")) {
				if (i + 2 < args.length) {
					File a = new File(args[i + 1]);
					File b = new File(args[i + 2]);
					if (ret.substitutions.containsKey(a)) {
						logger.error("{}",
								Strings.get("argDuplicateSubstitutionError"));
						return null;
					} else {
						ret.substitutions.put(a, b);
						i += 2;
					}
				} else {
					logger.error("{}", Strings.get("argTwoSubstitutionError"));
					return null;
				}
			} else if (arg.equals("-load")) {
				if (i + 1 < args.length) {
					i++;
					if (ret.loadFile != null) {
						logger.error("{}", Strings.get("loadMultipleError"));
					}
					File f = new File(args[i]);
					ret.loadFile = f;
				} else {
					logger.error("{}", Strings.get("loadNeedsFileError"));
					return null;
				}
			} else if (arg.equals("-empty")) {
				if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
					logger.error("{}", Strings.get("argOneTemplateError"));
					return null;
				}
				ret.templEmpty = true;
			} else if (arg.equals("-plain")) {
				if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
					logger.error("{}", Strings.get("argOneTemplateError"));
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
					AppPreferences.GATE_SHAPE
					.set(AppPreferences.SHAPE_RECTANGULAR);
				} else {
					logger.error("{}", Strings.get("argGatesOptionError"));
					System.exit(-1);
				}
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
					logger.error("{}", Strings.get("argAccentsOptionError"));
					System.exit(-1);
				}
			} else if (arg.equals("-template")) {
				if (ret.templFile != null || ret.templEmpty || ret.templPlain) {
					logger.error("{}", Strings.get("argOneTemplateError"));
					return null;
				}
				i++;
				if (i >= args.length) {
					printUsage();
				}
				ret.templFile = new File(args[i]);
				if (!ret.templFile.exists()) {
					logger.error("{}", StringUtil.format(
							Strings.get("templateMissingError"), args[i]));
				} else if (!ret.templFile.canRead()) {
					logger.error("{}", StringUtil.format(
							Strings.get("templateCannotReadError"), args[i]));
				}
			} else if (arg.equals("-nosplash")) {
				ret.showSplash = false;
			} else if (arg.equals("-testvector")) {
				i++;

				if (i >= args.length)
					printUsage();

				ret.circuitToTest = args[i];
				i++;

				if (i >= args.length)
					printUsage();

				ret.testVector = args[i];
				ret.showSplash = false;
				ret.exitAfterStartup = true;
				/* This is to test a test bench. It will return 0 or 1 depending on if
				 * the tests pass or not
				 */
			} else if (arg.equals("-test-fpga-implementation")) {
				// already handled above
				i++;
				if (i >= args.length)
					printUsage();

				ret.testCircuitImpPath = args[i];
				i++;
				if (i >= args.length)
					printUsage();

				ret.testCircuitImpMapFile = args[i];
				i++;
				if (i >= args.length)
					printUsage();

				ret.testCircuitImpName = args[i];
				i++;

				if (i >= args.length)
					printUsage();

				ret.testCircuitImpBoard = args[i];


				ret.filesToOpen.add(new File(ret.testCircuitImpPath));
				ret.showSplash = false;
				ret.exitAfterStartup = true;
			} else if (arg.equals("-test-circuit")) {
				// already handled above
				i++;
				if (i >= args.length)
					printUsage();

				ret.testCircuitPathInput= args[i];
				ret.filesToOpen.add(new File(ret.testCircuitPathInput));
				ret.showSplash = false;
				ret.exitAfterStartup = true;
			} else if (arg.equals("-test-circ-gen")) {
				/* This is to test the XML consistency over different version of
				 * the Logisim */
				i++;

				if (i >= args.length)
					printUsage();

				/* This is the input path of the file to open */
				ret.testCircPathInput = args[i];
				i++;
				if (i >= args.length)
					printUsage();

				/* This is the output file's path. The comparaison shall be
				 * done between the  testCircPathInput and the testCircPathOutput*/
				ret.testCircPathOutput = args[i];
				ret.filesToOpen.add(new File(ret.testCircPathInput));
				ret.showSplash = false;
				ret.exitAfterStartup = true;
			}
			else if (arg.equals("-clearprefs")) {
				// already handled above
			} else if (arg.equals("-analyze")) {
				Main.ANALYZE = true;
			} else if (arg.equals("-noupdates")) {
				Main.UPDATE = false;
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
					logger.error("{}", Strings.get("argQuestaOptionError"));
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
			logger.error("{}", Strings.get("ttyNeedsFileError"));
			return null;
		}
		if (ret.loadFile != null && !ret.isTty) {
			logger.error("{}", Strings.get("loadNeedsTtyError"));
			return null;
		}

		return ret;
	}

	private static void printUsage() {
		System.err.println(StringUtil.format(Strings.get("argUsage"),
				Startup.class.getName())); // OK
		System.err.println(); // OK
		System.err.println(Strings.get("argOptionHeader")); // OK
		System.err.println("   " + Strings.get("argAccentsOption")); // OK
		System.err.println("   " + Strings.get("argClearOption")); // OK
		System.err.println("   " + Strings.get("argEmptyOption")); // OK
		System.err.println("   " + Strings.get("argTestOption")); // OK
		System.err.println("   " + Strings.get("argGatesOption")); // OK
		System.err.println("   " + Strings.get("argHelpOption")); // OK
		System.err.println("   " + Strings.get("argLoadOption")); // OK
		System.err.println("   " + Strings.get("argLocaleOption")); // OK
		System.err.println("   " + Strings.get("argNoSplashOption")); // OK
		System.err.println("   " + Strings.get("argPlainOption")); // OK
		System.err.println("   " + Strings.get("argSubOption")); // OK
		System.err.println("   " + Strings.get("argTemplateOption")); // OK
		System.err.println("   " + Strings.get("argTtyOption")); // OK
		System.err.println("   " + Strings.get("argQuestaOption")); // OK
		System.err.println("   " + Strings.get("argVersionOption")); // OK
		System.err.println("   " + Strings.get("argTestCircGen")); // OK
		System.err.println("   " + Strings.get("argTestCircuit")); // OK
		System.err.println("   " + Strings.get("argTestImplement")); // OK

		System.exit(-1);
	}

	private static void registerHandler() {
		try {
			Class<?> needed1 = Class.forName("com.apple.eawt.Application");
			if (needed1 == null) {
				return;
			}
			Class<?> needed2 = Class
					.forName("com.apple.eawt.ApplicationAdapter");
			if (needed2 == null) {
				return;
			}
			MacOsAdapter.register();
			MacOsAdapter.addListeners(true);
		} catch (ClassNotFoundException e) {
			return;
		} catch (Exception t) {
			try {
				MacOsAdapter.addListeners(false);
			} catch (Exception t2) {
			}
		}
	}

	private static void setLocale(String lang) {
		Locale[] opts = Strings.getLocaleOptions();
		for (int i = 0; i < opts.length; i++) {
			if (lang.equals(opts[i].toString())) {
				LocaleManager.setLocale(opts[i]);
				return;
			}
		}
		logger.warn("{}", Strings.get("invalidLocaleError"));
		logger.warn("{}", Strings.get("invalidLocaleOptionsHeader"));

		for (int i = 0; i < opts.length; i++) {
			logger.warn("   {}", opts[i].toString());
		}
		System.exit(-1);
	}

	final static Logger logger = LoggerFactory.getLogger(Startup.class);

	private static Startup startupTemp = null;
	// based on command line
	boolean isTty;
	private File templFile = null;
	private boolean templEmpty = false;
	private boolean templPlain = false;
	private ArrayList<File> filesToOpen = new ArrayList<File>();
	private String testVector = null;
	private String circuitToTest = null;
	private boolean exitAfterStartup = false;
	private boolean showSplash;
	private File loadFile;
	private HashMap<File, File> substitutions = new HashMap<File, File>();
	private int ttyFormat = 0;
	// from other sources
	private boolean initialized = false;
	private SplashScreen monitor = null;
	/* Testing Circuit Variable */
	private String testCircuitPathInput = null;

	/* Test implementation */
	private String testCircuitImpPath = null;
	/* Name of the circuit withing logisim */
	private String testCircuitImpName = null;
	/* Name of the board to run on i.e Reptar, MAXV ...*/
	private String testCircuitImpBoard = null;
	/* Path folder containing Map file */
	private String testCircuitImpMapFile = null;

	/* Testing Xml (circ file) Variable */
	private String testCircPathInput = null;
	private String testCircPathOutput = null;
	private ArrayList<File> filesToPrint = new ArrayList<File>();

	private Startup(boolean isTty) {
		this.isTty = isTty;
		this.showSplash = !isTty;
	}

	/**
	 * Auto-update Logisim-evolution if a new version is available
	 *
	 * Original idea taken from Jupar:
	 * http://masterex.github.io/archive/2011/12/25/jupar.html by Periklis
	 * Master_ex Ntanasis <pntanasis@gmail.com>
	 *
	 * @return true if the code has been updated, and therefore the execution
	 *         has to be stopped, false otherwise
	 */
	public boolean autoUpdate() {
		if (!Main.UPDATE || !networkConnectionAvailable()) {
			// Auto-update disabled from command line, or network connection not
			// available
			return (false);
		}

		// Get the remote XML file containing the current version
		URL xmlURL;
		try {
			xmlURL = new URL(Main.UPDATE_URL);
		} catch (MalformedURLException e) {
			logger.error("The URL of the XML file for the auto-updater is malformed.\nPlease report this error to the software maintainer\n-- AUTO-UPDATE ABORTED --");
			return (false);
		}
		URLConnection conn;
		try {
			conn = xmlURL.openConnection();
		} catch (IOException e) {
			logger.error("Although an Internet connection should be available, the system couldn't connect to the URL requested by the auto-updater\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
			return (false);
		}
		InputStream in;
		try {
			in = conn.getInputStream();
		} catch (IOException e) {
			logger.error("Although an Internet connection should be available, the system couldn't retrieve the data requested by the auto-updater.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
			return (false);
		}
		ArgonXML logisimData = new ArgonXML(in, "logisim-evolution");

		// Get the appropriate remote version number
		LogisimVersion remoteVersion = LogisimVersion.parse(Main.VERSION
				.hasTracker() ? logisimData.child("tracked_version").content()
						: logisimData.child("untracked_version").content());

		// If the remote version is newer, perform the update
		if (remoteVersion.compareTo(Main.VERSION) > 0) {
			int answer = JOptionPane.showConfirmDialog(null,
					"A new Logisim-evolution version (" + remoteVersion
					+ ") is available!\nWould you like to update?",
					"Update", JOptionPane.YES_NO_OPTION,
					JOptionPane.INFORMATION_MESSAGE);

			if (answer == 1) {
				// User refused to update -- we just hope he gets sufficiently
				// annoyed by the message that he finally updates!
				return (false);
			}

			// Obtain the base directory of the jar archive
			CodeSource codeSource = Startup.class.getProtectionDomain()
					.getCodeSource();
			File jarFile = null;
			try {
				jarFile = new File(codeSource.getLocation().toURI().getPath());
			} catch (URISyntaxException e) {
				logger.error("Error in the syntax of the URI for the path of the executed Logisim-evolution JAR file!");
				e.printStackTrace();
				JOptionPane
				.showMessageDialog(
						null,
						"An error occurred while updating to the new Logisim-evolution version.\nPlease check the console for log information.",
						"Update failed", JOptionPane.ERROR_MESSAGE);
				return (false);
			}

			// Get the appropriate remote filename to download
			String remoteJar = Main.VERSION.hasTracker() ? logisimData.child(
					"tracked_file").content() : logisimData.child(
							"untracked_file").content();

					boolean updateOk = downloadInstallUpdatedVersion(remoteJar,
							jarFile.getAbsolutePath());

					if (updateOk) {
						JOptionPane
						.showMessageDialog(
								null,
								"The new Logisim-evolution version ("
										+ remoteVersion
										+ ") has been correctly installed.\nPlease restart Logisim-evolution for the changes to take effect.",
										"Update succeeded",
										JOptionPane.INFORMATION_MESSAGE);
						return (true);
					} else {
						JOptionPane
						.showMessageDialog(
								null,
								"An error occurred while updating to the new Logisim-evolution version.\nPlease check the console for log information.",
								"Update failed", JOptionPane.ERROR_MESSAGE);
						return (false);
					}
		}
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
	 * Download a new version of Logisim, according to the instructions received
	 * from autoUpdate(), and install it at the specified location
	 *
	 * Original idea taken from:
	 * http://baptiste-wicht.developpez.com/tutoriels/java/update/ by Baptiste
	 * Wicht
	 *
	 * @param filePath
	 *            remote file URL
	 * @param destination
	 *            local destination for the updated Jar file
	 * @return true if the new version has been downloaded and installed, false
	 *         otherwise
	 * @throws IOException
	 */
	private boolean downloadInstallUpdatedVersion(String filePath,
			String destination) {
		URL fileURL;
		try {
			fileURL = new URL(filePath);
		} catch (MalformedURLException e) {
			logger.error("The URL of the requested update file is malformed.\nPlease report this error to the software maintainer.\n-- AUTO-UPDATE ABORTED --");
			return (false);
		}
		URLConnection conn;
		try {
			conn = fileURL.openConnection();
		} catch (IOException e) {
			logger.error("Although an Internet connection should be available, the system couldn't connect to the URL of the updated file requested by the auto-updater.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
			return (false);
		}

		// Get remote file size
		int length = conn.getContentLength();
		if (length == -1) {
			logger.error("Cannot retrieve the file containing the updated version.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
			return (false);
		}

		// Get remote file stream
		InputStream is;
		try {
			is = new BufferedInputStream(conn.getInputStream());
		} catch (IOException e) {
			logger.error("Cannot get remote file stream.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
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
				currentBit = is.read(data, deplacement, data.length
						- deplacement);

				if (currentBit == -1) {
					// Reached EOF
					break;
				}
				deplacement += currentBit;
			}
		} catch (IOException e) {
			logger.error("An error occured while retrieving remote file (remote peer hung up).\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
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
			logger.error("An error occured while retrieving remote file (local size != remote size), download corrupted.\nIf the error persist, please contact the software maintainer\n-- AUTO-UPDATE ABORTED --");
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
			logger.error("An error occured while writing to the local Jar file.\n-- AUTO-UPDATE ABORTED --\nThe local file might be corrupted. If this is the case, please download a new copy of Logisim.");
		} finally {
			try {
				destinationFile.close();
			} catch (IOException e) {
				logger.error("Error encountered while closing the local destination file!\nThe local file might be corrupted. If this is the case, please download a new copy of Logisim.");
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

	Map<File, File> getSubstitutions() {
		return Collections.unmodifiableMap(substitutions);
	}

	int getTtyFormat() {
		return ttyFormat;
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
	 * This function tries to connect to google in order to test the
	 * availability of a network connection. This step is needed before
	 * attempting to perform an auto-update. It assumes that google is
	 * accessible -- usually this is the case, and it should also provide a
	 * quick reply to the connection attempt, reducing the lag.
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
				return;
			} catch (Exception t) {
				t.printStackTrace();
				System.exit(-1);
				return;
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

		Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);
		// pre-load the two basic component libraries, just so that the time
		// taken is shown separately in the progress bar.
		if (showSplash) {
			monitor.setProgress(SplashScreen.LIBRARIES);
		}
		Loader templLoader = new Loader(monitor);
		int count = templLoader.getBuiltin().getLibrary("Base").getTools()
				.size()
				+ templLoader.getBuiltin().getLibrary("Gates").getTools()
				.size();
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
			MacCompatibility
			.setFramelessJMenuBar(new LogisimMenuBar(null, null));
		} else {
			new LogisimMenuBar(null, null);
			// most of the time occupied here will be in loading menus, which
			// will occur eventually anyway; we might as well do it when the
			// monitor says we are
		}

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
						proj = ProjectActions.doOpenNoWindow(monitor,
								fileToOpen);
						proj.doTestVector(testVector, circuitToTest);
					} else if (testCircPathInput != null &&
							testCircPathOutput != null) {
						/* This part of the function will create a new circuit file (
						 * XML) which will be open and saved again using the  */
						proj = ProjectActions.doOpen(monitor,
								fileToOpen, substitutions);

						ProjectActions.doSave(proj, new File(testCircPathOutput));
					} else if (testCircuitPathInput != null)  {
						/* Testing test bench*/
						TestBench testB = new TestBench(testCircuitPathInput, monitor, substitutions);

						if (testB.startTestBench()) {
							System.out.println("Test bench pass\n");
							System.exit(0);
						} else {
							System.out.println("Test bench fail\n");
							System.exit(-1);
						}
					} else if (testCircuitImpPath != null) {
						/* Testing synthesis */
						proj = ProjectActions.doOpenNoWindow(monitor, fileToOpen);
						FPGACommanderTests testImpFpga = new FPGACommanderTests(proj,
								testCircuitImpMapFile,
								testCircuitImpName,
								testCircuitImpBoard);

						if (testImpFpga.StartTests()) {
							System.exit(0);
						} else {
							System.exit(-1);
						}
					} else {
						ProjectActions.doOpen(monitor, fileToOpen, substitutions);
					}
					numOpened++;
				} catch (LoadFailedException ex) {
					logger.error("{} : {}", fileToOpen.getName(),
							ex.getMessage());
				}
				if (first) {
					first = false;
					if (showSplash) {
						monitor.close();
					}
					monitor = null;
				}
			}
			if (numOpened == 0)
				System.exit(-1);
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
			for (Component comp1 : ((JOptionPane) comp).getComponents())
				result |= HasIcon(comp1);
		} else if (comp instanceof JPanel) {
			for (Component comp1 : ((JPanel) comp).getComponents())
				result |= HasIcon(comp1);
		} else if (comp instanceof JLabel) {
			return ((JLabel) comp).getIcon()!=null;
		}
		return result;
	}

	@Override
	public void eventDispatched(AWTEvent event) {
		if (event instanceof ContainerEvent) {
			ContainerEvent containerEvent = (ContainerEvent)event;
			if (containerEvent.getID() == ContainerEvent.COMPONENT_ADDED){
				Component container = containerEvent.getChild();
				if ((container instanceof JButton)||
						(container instanceof JCheckBox)||
						(container instanceof JComboBox)||
						(container instanceof JLabel)||
						(container instanceof JMenu)||
						(container instanceof JMenuItem)||
						(container instanceof JRadioButton)||
						(container instanceof JRadioButtonMenuItem)||
						(container instanceof JSpinner)||
						(container instanceof JTabbedPane)||
						(container instanceof JTextField)||
						(container instanceof JHelp)||
						(container instanceof JFileChooser)||
						((container instanceof JScrollPane)&&(!(container instanceof CanvasPane)))||
						(container instanceof JFontChooser)||
						(container instanceof JCheckBoxMenuItem)) {
					AppPreferences.setScaledFonts(((JComponent)container).getComponents());
					try{container.setFont(AppPreferences.getScaledFont(containerEvent.getChild().getFont()));
					container.revalidate();
					container.repaint();}
					catch(Exception e){}
				}
				if (container instanceof JOptionPane) {
					JOptionPane pane = (JOptionPane) container;
					if (HasIcon(pane)) {
						ImageIcon icon;
						switch (pane.getMessageType()) {
						case JOptionPane.ERROR_MESSAGE :
							icon = new ImageIcon(getClass().getClassLoader().getResource("resources/logisim/error.png"));
							pane.setIcon(AppPreferences.getScaledImageIcon(icon,3));
							break;
						case JOptionPane.QUESTION_MESSAGE :
							icon = new ImageIcon(getClass().getClassLoader().getResource("resources/logisim/question.png"));
							pane.setIcon(AppPreferences.getScaledImageIcon(icon,3));
							break;
						case JOptionPane.PLAIN_MESSAGE :
							icon = new ImageIcon(getClass().getClassLoader().getResource("resources/logisim/plain.png"));
							pane.setIcon(AppPreferences.getScaledImageIcon(icon,3));
							break;
						case JOptionPane.INFORMATION_MESSAGE :
							icon = new ImageIcon(getClass().getClassLoader().getResource("resources/logisim/info.png"));
							pane.setIcon(AppPreferences.getScaledImageIcon(icon,3));
							break;
						case JOptionPane.WARNING_MESSAGE :
							icon = new ImageIcon(getClass().getClassLoader().getResource("resources/logisim/warning.png"));
							pane.setIcon(AppPreferences.getScaledImageIcon(icon,3));
							break;
						}
					}
				}
			}

		}
		// TODO Auto-generated method stub
	}
}
