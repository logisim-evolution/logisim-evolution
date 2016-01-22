package com.cburch.logisim.util;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.SetAttributeAction;

public class AutoLabel {
	
	static final Integer[] UsedKeyStrokes = new Integer[] {KeyEvent.VK_L,KeyEvent.VK_T,KeyEvent.VK_V,KeyEvent.VK_H};
	public static Set<Integer> KeyStrokes = new HashSet<Integer>(Arrays.asList(UsedKeyStrokes));

	private static class ComponentSorter implements Comparator<Component> {

		@Override
		public int compare(Component o1, Component o2) {
			if (o1==o2)
				return 0;
			Location l1 = o1.getLocation();
			Location l2 = o2.getLocation();
			if (l2.getY() != l1.getY())
				return l1.getY()-l2.getY();
			if (l2.getX() != l1.getX())
			    return l1.getX()-l2.getX();
			return -1;
		}
		
	}
	
	public static SortedSet<Component> Sort(Set<Component> comps) {
		SortedSet<Component> sorted = new TreeSet<Component>(new ComponentSorter());
		sorted.addAll(comps);
		return sorted;
	}

	public static void AskAndSetLabel(String ComponentName,
            					      String OldLabel,
            					      Circuit circ,
            					      Component comp,
            					      AttributeSet attrs,
            					      SetAttributeAction act,
	                                  boolean CreateAction) {
		boolean correct = false;
		while (!correct) {
			String NewLabel = (String) JOptionPane.showInputDialog(null, 
					Strings.get("editLabelQuestion")+" "+ComponentName,
					Strings.get("editLabelDialog"),
					JOptionPane.QUESTION_MESSAGE,null,null,
					OldLabel);
			if (NewLabel!=null) {
				if (Circuit.IsCorrectLabel(NewLabel, circ.getNonWires(), attrs,true)&&
					SyntaxChecker.isVariableNameAcceptable(NewLabel,true)&&
					!CorrectLabel.IsKeyword(NewLabel,true)) {
					if (CreateAction)
						act.set(comp, StdAttr.LABEL, NewLabel);
					else
					  attrs.setValue(StdAttr.LABEL, NewLabel);
					correct = true;
				}
			}
			else
				correct = true;
		}
	}
	
	public static boolean LabelKeyboardHandler(int KeyCode,
			                                   AttributeSet attrs,
			                                   String ComponentName,
			                                   Component comp,
			                                   Circuit circ,
			                                   SetAttributeAction act,
				                               boolean CreateAction) {
		switch (KeyCode) {
			case KeyEvent.VK_L:
				if (attrs.containsAttribute(StdAttr.LABEL)) {
					String OldLabel = attrs.getValue(StdAttr.LABEL);
					AskAndSetLabel(ComponentName,OldLabel,circ,comp,attrs,act,CreateAction);
				}
				return true;
			case KeyEvent.VK_T:
				if (attrs.containsAttribute(StdAttr.LABEL_VISABILITY)) {
					if (CreateAction)
						act.set(comp, StdAttr.LABEL_VISABILITY, !attrs.getValue(StdAttr.LABEL_VISABILITY));
					else
					  attrs.setValue(StdAttr.LABEL_VISABILITY, !attrs.getValue(StdAttr.LABEL_VISABILITY));
				}
				return true;
			case KeyEvent.VK_V:
				if (attrs.containsAttribute(StdAttr.LABEL_VISABILITY)&&!attrs.getValue(StdAttr.LABEL_VISABILITY)) {
					if (CreateAction)
						act.set(comp, StdAttr.LABEL_VISABILITY, true);
					else
					  attrs.setValue(StdAttr.LABEL_VISABILITY, true);
				}
				return true;
			case KeyEvent.VK_H:
				if (attrs.containsAttribute(StdAttr.LABEL_VISABILITY)&&attrs.getValue(StdAttr.LABEL_VISABILITY)) {
					if (CreateAction)
						act.set(comp, StdAttr.LABEL_VISABILITY, false);
					else
					  attrs.setValue(StdAttr.LABEL_VISABILITY, false);
				}
				return true;
		}
		return false;
	}

}