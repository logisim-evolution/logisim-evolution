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

package com.cburch.logisim.gui.scale;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.cburch.logisim.prefs.AppPreferences;

public class ScaledOptionPane {
	
	@SuppressWarnings("serial")
	private static class MessageDialog extends JDialog implements ActionListener,ComponentListener,KeyListener {
		
		private JButton OKButton;
		
		MessageDialog(Component parentComponent,Object message, String title, Icon icon) {
			super();
			super.addKeyListener(this);
			if (parentComponent!=null)
				parentComponent.addComponentListener(this);
			setModal(true);
			setResizable(false);
			GridBagLayout thisLayout = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			setLayout(thisLayout);
			setTitle(title);
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth=1;
			if (message instanceof JScrollPane)
				add((JScrollPane)message,gbc);
			else if (message instanceof JPanel)
				add((JPanel)message,gbc);
			else
				add(new ScaledLabel(message.toString(),icon,JLabel.LEFT),gbc);
			gbc.gridy = 1;
			add(new ScaledLabel(" "),gbc);
			gbc.gridy = 2;
			OKButton = new ScaledButton("OK");
			OKButton.setActionCommand("OK");
			OKButton.addActionListener(this);
			OKButton.addKeyListener(this);
			add(OKButton,gbc);
			gbc.gridy = 3;
			add(new ScaledLabel(" "),gbc);
			pack();
			setPreferredSize(new Dimension(
					getWidth()+2*AppPreferences.getScaled(getFont().getSize()),
					getHeight()));
			pack();
			if (parentComponent!=null)
				this.setLocationRelativeTo(parentComponent);
			else {
				PointerInfo mouseloc = MouseInfo.getPointerInfo();
				Point mlocation = mouseloc.getLocation();
				int xpos = mlocation.x;
				int ypos = mlocation.y;
				xpos -= getWidth()>>1;
				ypos -= getHeight()>>1;
				if (xpos < 0)
					xpos = 0;
				if (ypos < 0)
					ypos = 0;
				setLocation(xpos,ypos);
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("OK")) {
				this.dispose();
			}
		}

		@Override
		public void componentResized(ComponentEvent e) {
			pack();			
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			pack();			
		}

		@Override
		public void componentShown(ComponentEvent e) {
			pack();			
		}

		@Override
		public void componentHidden(ComponentEvent e) {
			pack();			
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if ((e.getKeyCode()==KeyEvent.VK_ENTER)||
					(e.getKeyCode()==KeyEvent.VK_ESCAPE)) {
					this.dispose();
				}
		}
	}
	
	
	@SuppressWarnings("serial")
	private static class ConfirmDialog extends JDialog implements ActionListener,ComponentListener,KeyListener {
		
		private JButton YesButton;
		private JButton NoButton;
		private JButton CancelButton;
		private Boolean IsOkButton;
		private JPanel MyComp;
		
		ConfirmDialog(Component parentComponent,Object message, String title, int optionType, Icon icon) {
			super();
			super.addKeyListener(this);
			int NrOfButtons;
			MyComp = null;
			String YesText="Yes";
			String NoText="No";
			String CancelText="Cancel";
			Boolean YesButtonV=false;
			Boolean NoButtonV=false;
			Boolean CancelButtonV=false;
			IsOkButton = false;
			switch (optionType) {
				case JOptionPane.OK_CANCEL_OPTION : YesText = "OK";
				                                    IsOkButton = true;
				                                    YesButtonV = true;
				                                    CancelButtonV = true;
				                                    NrOfButtons = 2;
				                                    break;
				case JOptionPane.YES_NO_OPTION: YesButtonV = true;
                								NoButtonV = true;
					                            NrOfButtons = 2;
				                                break;
				case JOptionPane.YES_NO_CANCEL_OPTION : YesButtonV = true;
														NoButtonV = true;
					                                    CancelButtonV = true;
														NrOfButtons = 3;
				default : NrOfButtons = 1;
				          break;
			}
			if (parentComponent!=null) {
				parentComponent.addComponentListener(this);
				parentComponent.addKeyListener(this);
			}
			setModal(true);
			setResizable(false);
			GridBagLayout thisLayout = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();
			setLayout(thisLayout);
			setTitle(title);
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth=NrOfButtons;
			if (message instanceof JScrollPane)
				add((JScrollPane)message,gbc);
			else if (message instanceof JPanel) {
				MyComp = (JPanel)message;
				MyComp.addComponentListener(this);
				for (Component comp : MyComp.getComponents())
					comp.addKeyListener(this);
				add((JPanel)message,gbc);
			} else
				add(new ScaledLabel(message.toString(),icon,JLabel.LEFT),gbc);
			gbc.gridy = 1;
			add(new ScaledLabel(" "),gbc);
			gbc.gridy = 2;
			gbc.gridx = 0;
			gbc.gridwidth=1;
			gbc.insets=new Insets(0,AppPreferences.getScaled(5),0,AppPreferences.getScaled(5));
			gbc.fill=GridBagConstraints.HORIZONTAL;
			YesButton = new ScaledButton(YesText);
			YesButton.setActionCommand("Yes");
			YesButton.addActionListener(this);
			YesButton.addKeyListener(this);
			NoButton = new ScaledButton(NoText);
			NoButton.setActionCommand("No");
			NoButton.addActionListener(this);
			NoButton.addKeyListener(this);
			CancelButton = new ScaledButton(CancelText);
			CancelButton.setActionCommand("No");
			CancelButton.addActionListener(this);
			CancelButton.addKeyListener(this);
			if(YesButtonV) {
				add(YesButton,gbc);
				gbc.gridx++;
			}
			if(NoButtonV) {
				add(NoButton,gbc);
				gbc.gridx++;
			}
			if(CancelButtonV) {
				add(CancelButton,gbc);
				gbc.gridx++;
			}
			gbc.gridwidth=NrOfButtons;
			gbc.gridy = 3;
			add(new ScaledLabel(" "),gbc);
			pack();
			setPreferredSize(new Dimension(
					getWidth()+2*AppPreferences.getScaled(getFont().getSize()),
					getHeight()));
			if (parentComponent!=null)
				this.setLocationRelativeTo(parentComponent);
			else {
				PointerInfo mouseloc = MouseInfo.getPointerInfo();
				Point mlocation = mouseloc.getLocation();
				int xpos = mlocation.x;
				int ypos = mlocation.y;
				xpos -= getWidth()>>1;
				ypos -= getHeight()>>1;
				if (xpos < 0)
					xpos = 0;
				if (ypos < 0)
					ypos = 0;
				setLocation(xpos,ypos);
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("Yes")) {
				returnValue = (IsOkButton) ? JOptionPane.OK_OPTION : JOptionPane.YES_OPTION;
				this.dispose();
			}
			if (e.getActionCommand().equals("No")) {
				returnValue = JOptionPane.NO_OPTION;
				this.dispose();
			}
			if (e.getActionCommand().equals("Cancel")) {
				returnValue = JOptionPane.CANCEL_OPTION;
				this.dispose();
			}
		}

		@Override
		public void componentResized(ComponentEvent e) {
			pack();
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			pack();			
		}

		@Override
		public void componentShown(ComponentEvent e) {
			pack();
		}

		@Override
		public void componentHidden(ComponentEvent e) {
			pack();			
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode()==KeyEvent.VK_ENTER) {
				if (e.getSource().equals(NoButton)) {
					returnValue = JOptionPane.NO_OPTION;
				} else
				if (e.getSource().equals(CancelButton)) {
					returnValue = JOptionPane.CANCEL_OPTION;
				} else {
					returnValue = (IsOkButton) ? JOptionPane.OK_OPTION : JOptionPane.YES_OPTION;
				}
				this.dispose();
			}
			if (e.getKeyCode()==KeyEvent.VK_ESCAPE) {
				returnValue = JOptionPane.CANCEL_OPTION;
				this.dispose();
			}
		}
	}
	
	
	private static ImageIcon GetIcon(Component parentComponent, int messageType) {
		ImageIcon icon = null;
		Component bundle = (parentComponent == null) ?  new JFrame() : parentComponent; // Dirty hack
		if (bundle!=null) {
		   switch(messageType) {
		      case JOptionPane.INFORMATION_MESSAGE : icon = AppPreferences.getScaledImageIcon(new ImageIcon(bundle.getClass().getResource("/resources/logisim/info.png")),3);
		                                             break;
		      case JOptionPane.WARNING_MESSAGE : icon = AppPreferences.getScaledImageIcon(new ImageIcon(bundle.getClass().getResource("/resources/logisim/warning.png")),3);
		                                         break;
		      case JOptionPane.ERROR_MESSAGE : icon =  AppPreferences.getScaledImageIcon(new ImageIcon(bundle.getClass().getResource("/resources/logisim/error.png")),3);
		                                       break;
 		      case JOptionPane.QUESTION_MESSAGE : icon =  AppPreferences.getScaledImageIcon(new ImageIcon(bundle.getClass().getResource("/resources/logisim/question.png")),3);
 		                                          break;
 		      default : icon = AppPreferences.getScaledImageIcon(new ImageIcon(bundle.getClass().getResource("/resources/logisim/plain.png")),4);
 		                break;
		   }
		}
		return icon;
	}
	
	private static int returnValue;
	
	public static int showConfirmDialog(Component parentComponent, Object message) {
		return showConfirmDialog(parentComponent,message,"",JOptionPane.DEFAULT_OPTION);
	}
	
	public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
		return showConfirmDialog(parentComponent,message,title,optionType,JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType, int messageType) {
		return showConfirmDialog(parentComponent,message,title,optionType,messageType,GetIcon(parentComponent,messageType));
	}
	
	public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType, int messageType, Icon icon) {
		returnValue = JOptionPane.CLOSED_OPTION;
    	ConfirmDialog dialog = new ConfirmDialog(parentComponent,message,title,
   			(optionType==JOptionPane.DEFAULT_OPTION)?JOptionPane.YES_NO_CANCEL_OPTION : optionType,
  					icon);
        dialog.setVisible(true);
		return returnValue;
	}

	
	public static void showMessageDialog(Component parentComponent, Object message) {
		showMessageDialog(parentComponent,message,"",JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static void showMessageDialog(Component parentComponent, Object message, int messageType) {
		showMessageDialog(parentComponent,message,"",JOptionPane.INFORMATION_MESSAGE);
	}
	
	public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
		ImageIcon icon = GetIcon(parentComponent,messageType);
		
		showMessageDialog(parentComponent, message, title, messageType, icon);
	}
	
	public static void showMessageDialog(Component parentComponent, Object message, String title, int messageType, Icon icon) {
		MessageDialog dialog = new MessageDialog(parentComponent,message,title,icon);
		dialog.setVisible(true);
	}

}
