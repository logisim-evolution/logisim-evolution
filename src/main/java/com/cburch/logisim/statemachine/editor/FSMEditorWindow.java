package com.cburch.logisim.statemachine.editor;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.ScrollPane;

import javax.swing.BoxLayout;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.logisim.std.fsm.FSMContentVisualEditor;

public class FSMEditorWindow extends Panel implements  ChangeListener{

	// This is not a Command Design Pattern : must be fixed
	private FSMContentVisualEditor visualEditor;
	private FSMView view;
	private ScrollPane scroller;
	//private JSlider slider;
	

	public FSMEditorWindow(FSMContentVisualEditor visualEditor) {
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		System.out.println(this.toString()+":"+getWidth()+"x"+getHeight());		
		//this.content=content;
		setMinimumSize(new Dimension(500, 500));
		
		Panel shapePanel = new Panel(); // holds buttons for adding shapes
		shapePanel.setLayout(new FlowLayout());
		add(shapePanel);	

		view =new FSMView(visualEditor);
		scroller = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
		scroller.setMinimumSize(new Dimension(300, 300));
		scroller.setSize(new Dimension(500, 500));

		scroller.add(view);
		scroller.setEnabled(true);

		add(scroller);
	}
	
	
	@Override
	public void stateChanged(ChangeEvent e) {
		int value = ((JSlider)e.getSource()).getValue();
		view.setScale(value/100.0);
		repaint();
		revalidate();
	}

	@Override
	public void repaint() {
		super.repaint();
//		slider.repaint();
		view.repaint();
		
		scroller.repaint();
		revalidate();
	}

}
