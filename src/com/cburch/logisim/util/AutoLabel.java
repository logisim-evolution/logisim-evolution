package com.cburch.logisim.util;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
	
	static final Integer[] UsedKeyStrokes = new Integer[] {KeyEvent.VK_L,KeyEvent.VK_T,KeyEvent.VK_V,KeyEvent.VK_H,KeyEvent.VK_A};
	public static Set<Integer> KeyStrokes = new HashSet<Integer>(Arrays.asList(UsedKeyStrokes));
	
	private HashMap<Circuit,String> LabelBase = new HashMap<Circuit,String>();
	private HashMap<Circuit,Integer> CurrentIndex = new HashMap<Circuit,Integer>();
	private HashMap<Circuit,Boolean> UseLabelBaseOnly = new HashMap<Circuit,Boolean>();
	private HashMap<Circuit,Boolean> UseUnderscore = new HashMap<Circuit,Boolean>();
	private HashMap<Circuit,Boolean> active = new HashMap<Circuit,Boolean>();
	private HashMap<Circuit,String> CurrentLabel = new HashMap<Circuit,String>();
	
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
		update(circ,Label,UseFirstLabel);
		Activate(circ);
	}
	
	public boolean hasNext(Circuit circ) {
		if (circ==null||!active.containsKey(circ))
			return false;
		return active.get(circ);
	}
	
	public String GetCurrent(Circuit circ) {
		if (circ == null||!CurrentLabel.containsKey(circ)||CurrentLabel.get(circ).isEmpty())
		   return "";
		if (Circuit.IsCorrectLabel(CurrentLabel.get(circ), circ.getNonWires(), null,false))
			return CurrentLabel.get(circ);
		else if (hasNext(circ)) {
			return GetNext(circ);
		} else {
			SetLabel("",circ);
		}
		return "";
	}
	
	public String GetNext(Circuit circ) {
		if (circ==null)
			return "";
		if (UseLabelBaseOnly.get(circ)) {
			UseLabelBaseOnly.put(circ, false);
			return LabelBase.get(circ);
		}
		String NewLabel="";
		int CurIdx = CurrentIndex.get(circ);
		String BaseLab = LabelBase.get(circ);
		boolean Undescore = UseUnderscore.get(circ);
		do {CurIdx++;
			NewLabel = BaseLab;
			if (Undescore)
				NewLabel = NewLabel.concat("_");
			NewLabel = NewLabel.concat(Integer.toString(CurIdx));
		} while (!Circuit.IsCorrectLabel(NewLabel, circ.getNonWires(), null,false));
		CurrentIndex.put(circ, CurIdx);
		CurrentLabel.put(circ, NewLabel);
	    return NewLabel;
	}
	
	public boolean IsActive(Circuit circ) {
		if (circ==null)
			return false;
		if (!active.containsKey(circ))
			return false;
		return active.get(circ);
	}
	
	public void SetLabel(String Label,Circuit circ) {
		if (circ==null)
			return;
		update(circ,Label,true);
	}
	
	public void Activate(Circuit circ) {
		if (circ == null)
			return;
		if (LabelBase.containsKey(circ)&&
			CurrentIndex.containsKey(circ)&&
			UseLabelBaseOnly.containsKey(circ)&&
			UseUnderscore.containsKey(circ))
			active.put(circ, !LabelBase.get(circ).isEmpty());
	}
	
	public void Stop(Circuit circ) {
		if (circ == null)
			return;
		SetLabel("",circ);
		active.put(circ, false);
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
	
	private void update(Circuit circ,String Label , boolean UseFirstLabel) {
		if (circ == null)
			return;
		if (Label.isEmpty()||
			!SyntaxChecker.isVariableNameAcceptable(Label,false)) {
				LabelBase.put(circ, "");
				CurrentIndex.put(circ, 0);
				UseLabelBaseOnly.put(circ, false);
				CurrentLabel.put(circ, "");
				return;
		}
		UseLabelBaseOnly.put(circ, UseFirstLabel);
		if (LabelEndsWithNumber(Label)) {
			int Index = GetLabelBaseEndIndex(Label);
			CurrentIndex.put(circ, Integer.valueOf(Label.substring(Index+1,Label.length())));
			LabelBase.put(circ, Label.substring(0,Index+1));
			UseUnderscore.put(circ, false);
			UseLabelBaseOnly.put(circ, false);
		} else {
			LabelBase.put(circ, Label);
			CurrentIndex.put(circ, 0);
			UseUnderscore.put(circ, !Label.substring(Label.length()-1).equals("_"));
		}
		if (UseFirstLabel)
			CurrentLabel.put(circ, Label);
		else
			CurrentLabel.put(circ, GetNext(circ));
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

	public String AskAndSetLabel(String ComponentName,
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
						SetLabel(NewLabel,circ);
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
						if (!NewLabel.isEmpty()&&
							LabelEndsWithNumber(NewLabel)) {
							Activate(circ);
						} else  {
							active.put(circ, false);
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
			case KeyEvent.VK_A:
				Stop(circ);
				return true;
		}
		return false;
	}

}