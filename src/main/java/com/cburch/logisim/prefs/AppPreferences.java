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

package com.cburch.logisim.prefs;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.UIManager;

import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.PropertyChangeWeakSupport;

public class AppPreferences {
	//
	// LocalePreference
	//
	private static class LocalePreference extends PrefMonitorString {
		private static Locale findLocale(String lang) {
			Locale[] check;
			for (int set = 0; set < 2; set++) {
				if (set == 0)
					check = new Locale[] { Locale.getDefault(), Locale.ENGLISH };
				else
					check = Locale.getAvailableLocales();
				for (int i = 0; i < check.length; i++) {
					Locale loc = check[i];
					if (loc != null && loc.getLanguage().equals(lang)) {
						return loc;
					}
				}
			}
			return null;
		}

		public LocalePreference() {
			super("locale", "");

			String localeStr = this.get();
			if (localeStr != null && !localeStr.equals("")) {
				LocaleManager.setLocale(new Locale(localeStr));
			}
			LocaleManager.addLocaleListener(myListener);
			myListener.localeChanged();
		}

		@Override
		public void set(String value) {
			if (findLocale(value) != null) {
				super.set(value);
			}
		}
	}

	//
	// methods for accessing preferences
	//
	private static class MyListener implements PreferenceChangeListener,
			LocaleListener {
		public void localeChanged() {
			Locale loc = LocaleManager.getLocale();
			String lang = loc.getLanguage();
			if (LOCALE != null) {
				LOCALE.set(lang);
			}
		}

		public void preferenceChange(PreferenceChangeEvent event) {
			Preferences prefs = event.getNode();
			String prop = event.getKey();
			if (ACCENTS_REPLACE.getIdentifier().equals(prop)) {
				getPrefs();
				LocaleManager.setReplaceAccents(ACCENTS_REPLACE.getBoolean());
			} else if (prop.equals(TEMPLATE_TYPE)) {
				int oldValue = templateType;
				int value = prefs.getInt(TEMPLATE_TYPE, TEMPLATE_UNKNOWN);
				if (value != oldValue) {
					templateType = value;
					propertySupport.firePropertyChange(TEMPLATE, oldValue,
							value);
					propertySupport.firePropertyChange(TEMPLATE_TYPE, oldValue,
							value);
				}
			} else if (prop.equals(TEMPLATE_FILE)) {
				File oldValue = templateFile;
				File value = convertFile(prefs.get(TEMPLATE_FILE, null));
				if (value == null ? oldValue != null : !value.equals(oldValue)) {
					templateFile = value;
					if (templateType == TEMPLATE_CUSTOM) {
						customTemplate = null;
						propertySupport.firePropertyChange(TEMPLATE, oldValue,
								value);
					}
					propertySupport.firePropertyChange(TEMPLATE_FILE, oldValue,
							value);
				}
			}
		}
	}

	//
	// PropertyChangeSource methods
	//
	public static void addPropertyChangeListener(PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(listener);
	}

	public static void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertySupport.addPropertyChangeListener(propertyName, listener);
	}

	public static void clear() {
		Preferences p = getPrefs(true);
		try {
			p.clear();
		} catch (BackingStoreException e) {
		}
	}

	private static File convertFile(String fileName) {
		if (fileName == null || fileName.equals("")) {
			return null;
		} else {
			File file = new File(fileName);
			return file.canRead() ? file : null;
		}
	}

	private static <E> PrefMonitor<E> create(PrefMonitor<E> monitor) {
		return monitor;
	}

	static void firePropertyChange(String property, boolean oldVal,
			boolean newVal) {
		propertySupport.firePropertyChange(property, oldVal, newVal);
	}

	static void firePropertyChange(String property, Object oldVal, Object newVal) {
		propertySupport.firePropertyChange(property, oldVal, newVal);
	}

	private static Template getCustomTemplate() {
		File toRead = templateFile;
		if (customTemplateFile == null || !(customTemplateFile.equals(toRead))) {
			if (toRead == null) {
				customTemplate = null;
				customTemplateFile = null;
			} else {
				FileInputStream reader = null;
				try {
					reader = new FileInputStream(toRead);
					customTemplate = Template.create(reader);
					customTemplateFile = templateFile;
				} catch (Exception t) {
					setTemplateFile(null);
					customTemplate = null;
					customTemplateFile = null;
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException e) {
						}
					}
				}
			}
		}
		return customTemplate == null ? getPlainTemplate() : customTemplate;
	}

	public static Template getEmptyTemplate() {
		if (emptyTemplate == null)
			emptyTemplate = Template.createEmpty();
		return emptyTemplate;
	}

	private static Template getPlainTemplate() {
		if (plainTemplate == null) {
			ClassLoader ld = Startup.class.getClassLoader();
			InputStream in = ld
					.getResourceAsStream("resources/logisim/default.templ");
			if (in == null) {
				plainTemplate = getEmptyTemplate();
			} else {
				try {
					try {
						plainTemplate = Template.create(in);
					} finally {
						in.close();
					}
				} catch (Exception e) {
					plainTemplate = getEmptyTemplate();
				}
			}
		}
		return plainTemplate;
	}

	public static Preferences getPrefs() {
		return getPrefs(false);
	}

	private static Preferences getPrefs(boolean shouldClear) {
		if (prefs == null) {
			synchronized (AppPreferences.class) {
				if (prefs == null) {
					Preferences p = Preferences.userNodeForPackage(Main.class);
					if (shouldClear) {
						try {
							p.clear();
						} catch (BackingStoreException e) {
						}
					}
					myListener = new MyListener();
					p.addPreferenceChangeListener(myListener);
					prefs = p;

					setTemplateFile(convertFile(p.get(TEMPLATE_FILE, null)));
					setTemplateType(p.getInt(TEMPLATE_TYPE, TEMPLATE_PLAIN));
				}
			}
		}
		return prefs;
	}

	//
	// recent projects
	//
	public static List<File> getRecentFiles() {
		return recentProjects.getRecentFiles();
	}

	//
	// template methods
	//
	public static Template getTemplate() {
		getPrefs();
		switch (templateType) {
		case TEMPLATE_PLAIN:
			return getPlainTemplate();
		case TEMPLATE_EMPTY:
			return getEmptyTemplate();
		case TEMPLATE_CUSTOM:
			return getCustomTemplate();
		default:
			return getPlainTemplate();
		}
	}

	public static File getTemplateFile() {
		getPrefs();
		return templateFile;
	}

	//
	// accessor methods
	//
	public static int getTemplateType() {
		getPrefs();
		int ret = templateType;
		if (ret == TEMPLATE_CUSTOM && templateFile == null) {
			ret = TEMPLATE_UNKNOWN;
		}
		return ret;
	}

	public static void handleGraphicsAcceleration() {
		String accel = GRAPHICS_ACCELERATION.get();
		try {
			if (accel == ACCEL_NONE) {
				System.setProperty("sun.java2d.opengl", "False");
				System.setProperty("sun.java2d.d3d", "False");
			} else if (accel == ACCEL_OPENGL) {
				System.setProperty("sun.java2d.opengl", "True");
				System.setProperty("sun.java2d.d3d", "False");
			} else if (accel == ACCEL_D3D) {
				System.setProperty("sun.java2d.opengl", "False");
				System.setProperty("sun.java2d.d3d", "True");
			}
		} catch (Exception t) {
		}
	}

	public static void removePropertyChangeListener(
			PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(listener);
	}

	public static void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertySupport.removePropertyChangeListener(propertyName, listener);
	}

	public static void setTemplateFile(File value) {
		getPrefs();
		setTemplateFile(value, null);
	}

	public static void setTemplateFile(File value, Template template) {
		getPrefs();
		if (value != null && !value.canRead())
			value = null;
		if (value == null ? templateFile != null : !value.equals(templateFile)) {
			try {
				customTemplateFile = template == null ? null : value;
				customTemplate = template;
				getPrefs().put(TEMPLATE_FILE,
						value == null ? "" : value.getCanonicalPath());
			} catch (IOException ex) {
			}
		}
	}

	public static void setTemplateType(int value) {
		getPrefs();
		if (value != TEMPLATE_PLAIN && value != TEMPLATE_EMPTY
				&& value != TEMPLATE_CUSTOM) {
			value = TEMPLATE_UNKNOWN;
		}
		if (value != TEMPLATE_UNKNOWN && templateType != value) {
			getPrefs().putInt(TEMPLATE_TYPE, value);
		}
	}

	public static void setScaledFonts(Component[] comp) {
		for (int x = 0 ; x < comp.length; x++) {
			if (comp[x] instanceof Container)
				setScaledFonts(((Container) comp[x]).getComponents());
			try{comp[x].setFont(getScaledFont(comp[x].getFont()));
			    comp[x].revalidate();
			    comp[x].repaint();}
			catch(Exception e){}
		}
	}
	public static int getDownScaled(int value, float ExtScale) {
		getPrefs();
		float scale = ((float)((int)(SCALE_FACTOR.get()*10)))/(float)10.0;
		scale *= ExtScale;
		return (int)((float)value/scale);
	}
	
	public static int getDownScaled(int value) {
		getPrefs();
		float scale = ((float)((int)(SCALE_FACTOR.get()*10)))/(float)10.0;
		return (int)((float)value/scale);
	}
	
	public static int getScaled(int value, float ExtScale) {
		getPrefs();
		float scale = ((float)((int)(SCALE_FACTOR.get()*10)))/(float)10.0;
		scale *= ExtScale;
		return (int)((float)value*scale);
	}

	public static int getScaled(int value) {
		getPrefs();
		float scale = ((float)((int)(SCALE_FACTOR.get()*10)))/(float)10.0;
		return (int)((float)value*scale);
	}

	public static float getScaled(float value) {
		getPrefs();
		float scale = ((float)((int)(SCALE_FACTOR.get()*10)))/(float)10.0;
		return value*scale;
	}
	
	public static double getScaled(double value) {
		getPrefs();
		double scale = ((double)((int)(SCALE_FACTOR.get()*10)))/(double)10.0;
		return value*scale;
	}
	
	public static Font getScaledFont(Font myfont) {
		if (myfont != null)
			return myfont.deriveFont(getScaled((float)FontSize));
		else
			return null;
	}
	
	public static ImageIcon getScaledImageIcon(ImageIcon icon) {
		Image IcImage = icon.getImage();
		return new ImageIcon(IcImage.getScaledInstance(getScaled(IconSize), getScaled(IconSize), Image.SCALE_SMOOTH));
	}

	public static ImageIcon getScaledImageIcon(ImageIcon icon,float scale) {
		Image IcImage = icon.getImage();
		return new ImageIcon(IcImage.getScaledInstance(getScaled(IconSize,scale), getScaled(IconSize,scale), Image.SCALE_SMOOTH));
	}

	public static void updateRecentFile(File file) {
		recentProjects.updateRecent(file);
	}

	// class variables for maintaining consistency between properties,
	// internal variables, and other classes
	private static Preferences prefs = null;

	private static MyListener myListener = null;
	private static PropertyChangeWeakSupport propertySupport = new PropertyChangeWeakSupport(
			AppPreferences.class);

	// Template preferences
	public static final int IconSize = 16;
	public static final int FontSize = 14;
	public static final int IconBorder = 2;
	public static final int BoxSize = IconSize+2*IconBorder;
	public static final int TEMPLATE_UNKNOWN = -1;
	public static final int TEMPLATE_EMPTY = 0;
	public static final int TEMPLATE_PLAIN = 1;
	public static final int TEMPLATE_CUSTOM = 2;
	public static final String TEMPLATE = "template";
	public static final String TEMPLATE_TYPE = "templateType";
	public static final String TEMPLATE_FILE = "templateFile";
	private static int templateType = TEMPLATE_PLAIN;

	private static File templateFile = null;

	private static Template plainTemplate = null;
	private static Template emptyTemplate = null;
	private static Template customTemplate = null;
	private static File customTemplateFile = null;

	// International preferences
	public static final String SHAPE_SHAPED = "shaped";

	public static final String SHAPE_RECTANGULAR = "rectangular";
//	public static final String SHAPE_DIN40700 = "din40700";

	public static final PrefMonitor<String> GATE_SHAPE = create(new PrefMonitorStringOpts(
			"gateShape", new String[] { SHAPE_SHAPED, SHAPE_RECTANGULAR }, SHAPE_SHAPED));
	public static final PrefMonitor<String> LOCALE = create(new LocalePreference());
	public static final PrefMonitor<Boolean> ACCENTS_REPLACE = create(new PrefMonitorBoolean(
			"accentsReplace", false));
	
	// FPGA Commander Preferences
	public static final PrefMonitor<String> FPGA_Workspace=create(new PrefMonitorString(
			"FPGAWorkspace", System.getProperty("user.home")+"/logisim_evolution_workspace"));
	public static final PrefMonitor<String> HDL_Type = create(new PrefMonitorStringOpts(
			"afterAdd", new String[] { HDLGeneratorFactory.VHDL, HDLGeneratorFactory.VERILOG },
			HDLGeneratorFactory.VHDL));
	public static final PrefMonitor<Boolean> DownloadToBoard=create(new PrefMonitorBoolean(
			"DownloadToBoard",true));
	public static final PrefMonitor<String> SelectedBoard=create(new PrefMonitorString(
			"SelectedBoard",null));
	
	public static final String External_Boards = "ExternalBoards";
	public static final FPGABoards Boards = new FPGABoards();


	
	// Window preferences
	public static final String TOOLBAR_HIDDEN = "hidden";
	public static final String TOOLBAR_DOWN_MIDDLE = "downMiddle";
	public static final PrefMonitor<Boolean> SHOW_TICK_RATE = create(new PrefMonitorBoolean(
			"showTickRate", false));
	public static final PrefMonitor<String> TOOLBAR_PLACEMENT = create(new PrefMonitorStringOpts(
			"toolbarPlacement", new String[] { Direction.NORTH.toString(),
					Direction.SOUTH.toString(), Direction.EAST.toString(),
					Direction.WEST.toString(), TOOLBAR_DOWN_MIDDLE,
					TOOLBAR_HIDDEN }, Direction.NORTH.toString()));
	public static final PrefMonitor<String> LookAndFeel = create(new PrefMonitorString(
			"LookAndFeel",UIManager.getCrossPlatformLookAndFeelClassName()));
	
	// Layout preferences
	public static final String ADD_AFTER_UNCHANGED = "unchanged";
	public static final String ADD_AFTER_EDIT = "edit";
	public static final PrefMonitor<Boolean> PRINTER_VIEW = create(new PrefMonitorBoolean(
			"printerView", false));
	public static final PrefMonitor<Boolean> ATTRIBUTE_HALO = create(new PrefMonitorBoolean(
			"attributeHalo", true));
	public static final PrefMonitor<Boolean> COMPONENT_TIPS = create(new PrefMonitorBoolean(
			"componentTips", true));
	public static final PrefMonitor<Boolean> MOVE_KEEP_CONNECT = create(new PrefMonitorBoolean(
			"keepConnected", true));
	public static final PrefMonitor<Boolean> ADD_SHOW_GHOSTS = create(new PrefMonitorBoolean(
			"showGhosts", true));
	public static final PrefMonitor<Boolean> NAMED_CIRCUIT_BOXES = create(new PrefMonitorBoolean(
			"namedBoxes", true));
	public static final PrefMonitor<Boolean> NAMED_CIRCUIT_BOXES_FIXED_SIZE = create(new PrefMonitorBoolean(
			"namedBoxesFixed", true));
	public static final PrefMonitor<Boolean> NEW_INPUT_OUTPUT_SHAPES = create(new PrefMonitorBoolean(
			"oldIO", true));
	public static final PrefMonitor<Double> SCALE_FACTOR = create(new PrefMonitorDouble(
			"Scale", (((!GraphicsEnvironment.isHeadless()) ? Toolkit.getDefaultToolkit().getScreenSize().getHeight() : 0)/1000) < 1.0 ? 1.0 :
				((!GraphicsEnvironment.isHeadless()) ? Toolkit.getDefaultToolkit().getScreenSize().getHeight() : 0)/1000));
	
	public static final PrefMonitor<String> ADD_AFTER = create(new PrefMonitorStringOpts(
			"afterAdd", new String[] { ADD_AFTER_EDIT, ADD_AFTER_UNCHANGED },
			ADD_AFTER_EDIT));

	public static PrefMonitor<String> POKE_WIRE_RADIX1;

	public static PrefMonitor<String> POKE_WIRE_RADIX2;

	static {
		RadixOption[] radixOptions = RadixOption.OPTIONS;
		String[] radixStrings = new String[radixOptions.length];
		for (int i = 0; i < radixOptions.length; i++) {
			radixStrings[i] = radixOptions[i].getSaveString();
		}
		POKE_WIRE_RADIX1 = create(new PrefMonitorStringOpts("pokeRadix1",
				radixStrings, RadixOption.RADIX_2.getSaveString()));
		POKE_WIRE_RADIX2 = create(new PrefMonitorStringOpts("pokeRadix2",
				radixStrings, RadixOption.RADIX_10_SIGNED.getSaveString()));
	}
	public static final PrefMonitor<Boolean> Memory_Startup_Unknown = create(new PrefMonitorBoolean(
			"MemStartUnknown", false));

	// Experimental preferences
	public static final String ACCEL_DEFAULT = "default";

	public static final String ACCEL_NONE = "none";

	public static final String ACCEL_OPENGL = "opengl";

	public static final String ACCEL_D3D = "d3d";

	public static final PrefMonitor<String> GRAPHICS_ACCELERATION = create(new PrefMonitorStringOpts(
			"graphicsAcceleration", new String[] { ACCEL_DEFAULT, ACCEL_NONE,
					ACCEL_OPENGL, ACCEL_D3D }, ACCEL_DEFAULT));
	public static final PrefMonitor<Boolean> AntiAliassing = create(new PrefMonitorBoolean("AntiAliassing",true));

	// Third party softwares preferences
	public static final PrefMonitor<String> QUESTA_PATH = create(new PrefMonitorString(
			"questaPath", ""));

	public static final PrefMonitor<Boolean> QUESTA_VALIDATION = create(new PrefMonitorBoolean(
			"questaValidation", false));
	public static final PrefMonitor<String> QuartusToolPath=create(new PrefMonitorString(
			"QuartusToolPath",""));
	public static final PrefMonitor<String> ISEToolPath=create(new PrefMonitorString(
			"ISEToolPath",""));
	public static final PrefMonitor<String> VivadoToolPath=create(new PrefMonitorString(
			"VivadoToolPath",""));

	// hidden window preferences - not part of the preferences dialog, changes
	// to preference does not affect current windows, and the values are not
	// saved until the application is closed
	public static final String RECENT_PROJECTS = "recentProjects";

	private static final RecentProjects recentProjects = new RecentProjects();

	public static final PrefMonitor<Double> TICK_FREQUENCY = create(new PrefMonitorDouble(
			"tickFrequency", 1.0));

	public static final PrefMonitor<Boolean> LAYOUT_SHOW_GRID = create(new PrefMonitorBoolean(
			"layoutGrid", true));

	public static final PrefMonitor<Double> LAYOUT_ZOOM = create(new PrefMonitorDouble(
			"layoutZoom", 1.0));

	public static final PrefMonitor<Boolean> APPEARANCE_SHOW_GRID = create(new PrefMonitorBoolean(
			"appearanceGrid", true));

	public static final PrefMonitor<Double> APPEARANCE_ZOOM = create(new PrefMonitorDouble(
			"appearanceZoom", 1.0));

	public static final PrefMonitor<Integer> WINDOW_STATE = create(new PrefMonitorInt(
			"windowState", JFrame.NORMAL));

	public static final PrefMonitor<Integer> WINDOW_WIDTH = create(new PrefMonitorInt(
			"windowWidth", ((!GraphicsEnvironment.isHeadless()) ? Toolkit.getDefaultToolkit().getScreenSize().width : 0)/2));

	public static final PrefMonitor<Integer> WINDOW_HEIGHT = create(new PrefMonitorInt(
			"windowHeight", ((!GraphicsEnvironment.isHeadless()) ? Toolkit.getDefaultToolkit().getScreenSize().height : 0)));

	public static final PrefMonitor<String> WINDOW_LOCATION = create(new PrefMonitorString(
			"windowLocation", "0,0"));

	public static final PrefMonitor<Double> WINDOW_MAIN_SPLIT = create(new PrefMonitorDouble(
			"windowMainSplit", 0.25));

	public static final PrefMonitor<Double> WINDOW_LEFT_SPLIT = create(new PrefMonitorDouble(
			"windowLeftSplit", 0.5));

	public static final PrefMonitor<Double> WINDOW_RIGHT_SPLIT = create(new PrefMonitorDouble(
			"windowRightSplit", 0.75));

	public static final PrefMonitor<String> DIALOG_DIRECTORY = create(new PrefMonitorString(
			"dialogDirectory", ""));
}
