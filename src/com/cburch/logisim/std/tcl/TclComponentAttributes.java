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

package com.cburch.logisim.std.tcl;

import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.JInputComponent;

public class TclComponentAttributes extends AbstractAttributeSet {

	private static class ContentFileAttribute extends Attribute<File> {

		ContentFileCell chooser;

		public ContentFileAttribute() {
			super("filePath", Strings.getter("tclConsoleContentFile"));
		}

		public java.awt.Component getCellEditor(Window source, File file) {

			if (chooser == null)
				chooser = new ContentFileCell(file);

			chooser.setFileFilter(Loader.TCL_FILTER);
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

			return chooser;
		}

		@Override
		public File parse(String path) {
			return new File(path);
		}

		@Override
		public String toDisplayString(File file) {
			if (file.isDirectory())
				return "...";
			else
				return file.getName();
		}

		@Override
		public String toStandardString(File file) {
			return file.getPath();
		}
	}

	private static class ContentFileCell extends JFileChooser implements
			JInputComponent {
		private static final long serialVersionUID = 1L;

		ContentFileCell(File initial) {
			super(initial);
		}

		public Object getValue() {
			return getSelectedFile();
		}

		public void setValue(Object value) {
			setSelectedFile((File) value);
		}
	}

	public static Attribute<File> CONTENT_FILE_ATTR = new ContentFileAttribute();

	private static List<Attribute<?>> attributes = Arrays
			.asList(new Attribute<?>[] { CONTENT_FILE_ATTR, StdAttr.LABEL,
					StdAttr.LABEL_FONT });

	private File contentFile;
	private String label = "";
	private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;

	TclComponentAttributes() {
		contentFile = new File(System.getProperty("user.home"));
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		TclComponentAttributes attr = (TclComponentAttributes) dest;
		attr.labelFont = labelFont;
		attr.contentFile = new File(contentFile.getAbsolutePath());
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return attributes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> V getValue(Attribute<V> attr) {
		if (attr == CONTENT_FILE_ATTR) {
			return (V) contentFile;
		}
		if (attr == StdAttr.LABEL) {
			return (V) label;
		}
		if (attr == StdAttr.LABEL_FONT) {
			return (V) labelFont;
		}
		return null;
	}

	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == CONTENT_FILE_ATTR) {
			File newFile = (File) value;
			if (!contentFile.equals(newFile))
				contentFile = newFile;
			fireAttributeValueChanged(attr, value,null);
		}
		if (attr == StdAttr.LABEL) {
			String newLabel = (String) value;
			if (label.equals(newLabel))
				return;
			@SuppressWarnings("unchecked")
			V Oldlabel = (V) label;
			label = newLabel;
			fireAttributeValueChanged(attr, value,Oldlabel);
		}
		if (attr == StdAttr.LABEL_FONT) {
			Font newFont = (Font) value;
			if (labelFont.equals(newFont))
				return;
			labelFont = newFont;
			fireAttributeValueChanged(attr, value, null);
		}
	}

}
