package com.bfh.logisim.fpgagui;


import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

@SuppressWarnings("serial")
public class FPGACommanderTextWindow extends JFrame implements KeyListener,WindowListener {

	private int FontSize = 14;
	private String Title;
	private int LineCount;
	private JTextArea textArea = new JTextArea(50, 80);
	private boolean IsActive = false;
	private boolean count;
	
	public FPGACommanderTextWindow(String Title,Color fg, boolean count) {
		super((count)?Title+" (0)":Title);
		this.Title = Title;
		setResizable(true);
		setAlwaysOnTop(false);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		Color bg = Color.black;

		textArea.setForeground(fg);
		textArea.setBackground(bg);
		textArea.setFont(new Font("monospaced", Font.PLAIN, FontSize));
		textArea.setEditable(false);
		clear();

		JScrollPane textMessages = new JScrollPane(textArea);
		textMessages
			.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		textMessages
			.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(textMessages);
		setLocationRelativeTo(null);
		textArea.addKeyListener(this);
		pack();
		addWindowListener(this);
		LineCount = 0;
		this.count = count;
	}
	
	public boolean IsActivated() {
		return IsActive;
	}
	
	public void clear() {
		textArea.setText(null);
		LineCount = 0;
		if (count)
			setTitle(Title+" (0)");
	}
	
	public void add(String line) {
		LineCount++;
		textArea.setText(line);
		if (count)
			setTitle(Title+" ("+LineCount+")");
		Rectangle rect = textArea.getBounds();
		rect.x = 0;
		rect.y = 0;
		textArea.paintImmediately(rect);
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		Rectangle rect;
		switch (e.getKeyCode()) {
			case KeyEvent.VK_EQUALS:
			case KeyEvent.VK_PLUS:
			case KeyEvent.VK_ADD:
				FontSize++;
				textArea.setFont(new Font("monospaced", Font.PLAIN, FontSize));
				rect = textArea.getBounds();
				rect.x = 0;
				rect.y = 0;
				textArea.paintImmediately(rect);
				break;
			case KeyEvent.VK_MINUS:
			case KeyEvent.VK_SUBTRACT:
				if (FontSize > 8) {
					FontSize--;
					textArea.setFont(new Font("monospaced", Font.PLAIN, FontSize));
					rect = textArea.getBounds();
					rect.x = 0;
					rect.y = 0;
					textArea.paintImmediately(rect);
				}
				break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		IsActive = false;
		setVisible(false);
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
		IsActive = true;
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
