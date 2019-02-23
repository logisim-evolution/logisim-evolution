package com.cburch.logisim.vhdl.file;

import static com.cburch.hdl.Strings.S;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.cburch.logisim.vhdl.gui.HdlContentEditor;

public class HdlFile {

	public static String load(File file) throws IOException {
		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(file));

			StringBuilder content = new StringBuilder();
			String l;

			while ((l = in.readLine()) != null) {
				content.append(l);
				content.append(System.getProperty("line.separator"));
			}
			return content.toString();
		} catch (IOException ex) {
			throw new IOException(S.get("hdlFileReaderError"));
		} finally {
			if (in != null)
				in.close();
		}
	}

	public static void save(File file, HdlContentEditor editor)
			throws IOException {
		BufferedWriter out = null;

		try {
			out = new BufferedWriter(new FileWriter(file));
			String data = editor.getText();
			out.write(data, 0, data.length());
		} catch (IOException ex) {
			throw new IOException(S.get("hdlFileWriterError"));
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

}
