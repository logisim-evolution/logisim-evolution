package com.cburch.logisim.gui.generic;

import java.awt.Component;
import java.awt.Container;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.logisim.prefs.AppPreferences;

public class ZoomFontControl extends JPanel implements ChangeListener {

	private static final long serialVersionUID = 1L;

	private JSlider slider;
	private JLabel lblValue;
	private final int min=50;
	private final int max=250;
	private final UIDefaults startDefaults;
	private final JFrame parent;
	private final JMenuBar mbar;
	
	public ZoomFontControl(JFrame parent,JMenuBar mbar) {
		super();
		startDefaults=(UIDefaults) UIManager.getLookAndFeelDefaults().clone();

		this.parent=parent;
		this.mbar=mbar;
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setBorder(BorderFactory.createEmptyBorder(0,20,0,0));

		int scale=AppPreferences.LAYOUT_FONT_ZOOM.get().intValue();
		if(scale>max) scale=max;
		if(scale<min) scale=min;
		

		slider=new JSlider(min,max,scale);
		slider.addChangeListener(this);

		lblValue=new JLabel();
		setLabel();

		this.add(slider);
		this.add(lblValue);
		
		UpdateFonts();
	}
	
	public void setLabel(){
		lblValue.setText(String.valueOf((slider.getValue()/100.0)));
		AppPreferences.LAYOUT_FONT_ZOOM.set(slider.getValue());
	}


	@Override
	public void stateChanged(ChangeEvent arg0) {
		
		UpdateFonts();
	}
	
	public void UpdateFonts(){
		for (Entry<Object, Object> entry : UIManager.getLookAndFeelDefaults().entrySet()) {
			Object key = entry.getKey();
			Object value = javax.swing.UIManager.get(key);
			if (value != null && value instanceof javax.swing.plaf.FontUIResource) {
				Object defValue=startDefaults.get(key);
				if (defValue != null && defValue instanceof javax.swing.plaf.FontUIResource) {
					javax.swing.plaf.FontUIResource fr=(javax.swing.plaf.FontUIResource)defValue;
					UIManager.put(key, new javax.swing.plaf.FontUIResource(fr.deriveFont((float) (fr.getSize2D() * slider.getValue()/100.0))));		    		
				}  
			}
		}

		iterateComponents(parent);
		iterateComponents(mbar);
		setLabel();	
		lblValue.updateUI();		
	}

	public void iterateComponents(Container c) {
		Component[] components = c.getComponents();
		for(Component com : components) {

			if(com instanceof JComponent){
				((JComponent)com).updateUI();
			}

			if(com instanceof Container){
				if(com!=this)
					iterateComponents((Container)com);
			}
		}
	}	
}
