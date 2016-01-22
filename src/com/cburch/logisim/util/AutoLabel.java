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
	
	private String LabelBase;
	private Integer CurrentIndex;
	private Circuit circ;
	private boolean UseLabelBaseOnly;
	private boolean UseUnderscore;
	private boolean active;
	
	public AutoLabel() {
		this("",null,false);
	}
	
	public AutoLabel(String Label,
			         Circuit circ) {
		this(Label,circ,true);
	}
	

	public AutoLabel(String Label,
    		 		 Circuit circ,
    		 		 boolean UseFirstLabel) {
		this.circ = circ;
		update(Label,UseFirstLabel);
		active = !LabelBase.isEmpty()&&!(circ==null);
	}
	
	public boolean hasNext() {
		return !LabelBase.isEmpty()&&circ!=null&&active;
	}
	
	public String GetNext() {
		if (UseLabelBaseOnly) {
			UseLabelBaseOnly = false;
			return LabelBase;
		}
		if (circ==null)
			return "";
		String NewLabel="";
		do {
			CurrentIndex++;
			NewLabel = LabelBase;
			if (UseUnderscore)
				NewLabel = NewLabel.concat("_");
			NewLabel = NewLabel.concat(Integer.toString(CurrentIndex));
		} while (!Circuit.IsCorrectLabel(NewLabel, circ.getNonWires(), null,false));
	    return NewLabel;
	}
	
	public void SetLabel(String Label) {
		update(Label,true);
		active = !LabelBase.isEmpty()&&!(circ==null);
	}
	
	public void SetCircuit(Circuit circ) {
		this.circ = circ;
		active = !LabelBase.isEmpty()&&!(circ==null);
	}
	
	public void Activate() {
		active = !LabelBase.isEmpty()&&!(circ==null);
	}
	
	public void Stop() {
		active = false;
	}
	
	public static boolean LabelEndsWithNumber(String Label) {
		return CorrectLabel.Numbers.contains(Label.substring(Label.length()-1));
	}
	
	private int GetLabelBaseEndIndex(String Label) {
		int index = Label.length();
		while ((index >1)&&
			   CorrectLabel.Numbers.contains(Label.substring(index-1,index))) 
			index--;
		return (index-1);
	}
	
	private void update(String Label , boolean UseFirstLabel) {
		if (Label.isEmpty()||
				!SyntaxChecker.isVariableNameAcceptable(Label,false)) {
				LabelBase = "";
				CurrentIndex = 0;
				UseLabelBaseOnly = false;
				return;
			}
			UseLabelBaseOnly = UseFirstLabel;
			if (LabelEndsWithNumber(Label)) {
				int Index = GetLabelBaseEndIndex(Label);
				CurrentIndex = Integer.valueOf(Label.substring(Index+1,Label.length()));
				LabelBase = Label.substring(0,Index+1);
				UseUnderscore = false;
				if (UseFirstLabel) 
					CurrentIndex--;
				UseLabelBaseOnly = false;
			} else {
				LabelBase = Label;
				CurrentIndex = 0;
				UseUnderscore = !Label.substring(Label.length()-1).equals("_");
			}
	}


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

	public static String AskAndSetLabel(String ComponentName,
            					        String OldLabel,
            					        Circuit circ,
            					        Component comp,
            					        AttributeSet attrs,
            					        SetAttributeAction act,
	                                    boolean CreateAction) {
		boolean correct = false;
		String NewLabel = OldLabel;
		while (!correct) {
			NewLabel = (String) JOptionPane.showInputDialog(null, 
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
			else {
				correct = true;
				NewLabel = OldLabel;
			}
		}
		return NewLabel;
	}
	
	public boolean LabelKeyboardHandler(int KeyCode,
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
					String NewLabel = AskAndSetLabel(ComponentName,OldLabel,circ,comp,attrs,act,CreateAction);
					if (!NewLabel.equals(OldLabel)) {
						if (LabelEndsWithNumber(NewLabel)) {
							this.circ = circ;
							update(NewLabel,true);
							active = true;
						} else  {
							active = false;
						}
					}
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