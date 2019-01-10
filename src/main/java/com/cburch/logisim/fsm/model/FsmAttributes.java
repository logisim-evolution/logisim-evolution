/* This file is part of logisim-evolution.
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

 package com.cburch.logisim.fsm.model;

import java.awt.Window;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.StringGetter;

public class FsmAttributes extends AbstractAttributeSet {
	
	static class ContentAttribute extends Attribute<FsmDataStructure> {
		
		FsmDataStructure info;
		
		public ContentAttribute() {
			super("content", Strings.getter("FsmContentAttr"));
		}

		public ContentAttribute(String Value, StringGetter getter) {
			super(Value, getter);
		}

		@Override
		public java.awt.Component getCellEditor(Window source, FsmDataStructure value) {
			if (value == null)
				return new JLabel("");
			info = value;
			return new JTextField("bla");
		}
		
		@Override
		public FsmDataStructure parse(String value) {
			if (info != null)
				info.RemoveState();
			return info;
		}
		
		@Override
		public String toDisplayString(FsmDataStructure value) {
			info = null;
			return Strings.get("FsmContentValue");
		}
		
	}
	
	static class ResetStateAttribute extends Attribute<FsmDataStructure> {
		
		private FsmDataStructure info;
		
		public ResetStateAttribute() {
			super("resetstate", Strings.getter("FsmResetState"));
		}

		@Override
		public java.awt.Component getCellEditor(Window source, FsmDataStructure value) {
			if (value == null)
				return new JLabel("");
			JComboBox<String> StateList = new JComboBox<String>();
			for (String item : value.GetStateNames())
				StateList.addItem(item);
			StateList.setSelectedIndex(value.GetResetStateIndex()+1);
			info = value;
			return StateList;
		}
		
		@Override
		public FsmDataStructure parse(String value) {
			if (info != null) {
				info.SetResetState(value);
				return info;
			}
			return null;
		}
		
		@Override
		public String toDisplayString(FsmDataStructure value) {
			info = null;
			return value.getResetStateName();
		}
	}
	
	/* Fsm type options */
	public static final AttributeOption FSM_MEDVEDEV = new AttributeOption(
			"Medvedev", Strings.getter("FsmMedvedev"));
	public static final AttributeOption FSM_MOORE = new AttributeOption(
			"Moore", Strings.getter("FsmMoore"));
	public static final AttributeOption FSM_MEALY = new AttributeOption(
			"Moore", Strings.getter("FsmMealy"));
	public static final Attribute<AttributeOption> FSM_TYPE = Attributes.forOption("type", 
			Strings.getter("FsmType"), new AttributeOption[] { FSM_MEDVEDEV, FSM_MOORE,FSM_MEALY});
	
	/* Fsm coding options */
	public static final AttributeOption FSM_ONEHOTCODED = new AttributeOption(
			"OneHotCoded", Strings.getter("FsmOneHotCoded"));
	public static final AttributeOption FSM_BINARYCODED = new AttributeOption(
			"BinaryCoded", Strings.getter("FsmBinaryCoded"));
	public static final AttributeOption FSM_CUSTOMCODED = new AttributeOption(
			"CustomCoded", Strings.getter("FsmCustomCoded"));
	public static final Attribute<AttributeOption> FSM_CODING = Attributes.forOption("coding",
			Strings.getter("FsmCoding"), new AttributeOption[] {FSM_BINARYCODED,FSM_ONEHOTCODED,FSM_CUSTOMCODED});
	
	/* Fsm Reset options */
	public static final AttributeOption FSM_RESETSYNCHRON = new AttributeOption(
			"ResetSynchron", Strings.getter("FsmResetSynchron"));
	public static final AttributeOption FSM_RESETASYNCHRON = new AttributeOption(
			"ResetAsynchron", Strings.getter("FsmResetAsynchron"));
	public static final Attribute<AttributeOption> FSM_RESET = Attributes.forOption("reset", 
			Strings.getter("FsmResetBehavior"),new AttributeOption[] {FSM_RESETSYNCHRON,FSM_RESETASYNCHRON});
	
	/* Fsm Reset State option */
	static final Attribute<FsmDataStructure> FSMRESETSTATE_ATTR = new ResetStateAttribute();

	/* Fsm contents */
	static final Attribute<FsmDataStructure> FSMCONTENT_ATTR = new ContentAttribute();

	

	
	
	
	private static List<Attribute<?>> attributes = Arrays.asList(FSMCONTENT_ATTR,FSM_TYPE,FSM_CODING,FSM_RESET,
			FSMRESETSTATE_ATTR,StdAttr.LABEL, StdAttr.LABEL_VISIBILITY);

	private String label = "";
	private Boolean labelVisible = true;
	private FsmDataStructure data;
	private AttributeOption type = FSM_MEDVEDEV;
	
	public FsmAttributes() {
	}

	public FsmAttributes(AttributeOption type) {
        this.type = type;
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		FsmAttributes attrs = (FsmAttributes) dest;
		if (attrs.getValue(FSMRESETSTATE_ATTR) == null) {
			attrs.define();
		} else {
			if (data != null)
				attrs.SetState(data.clone((FsmAttributes)dest));
		}
	}
	
	public void SetState(FsmDataStructure val) {
		data = val;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return attributes;
	}
	
	public void define() {
		if (data == null) {
			data = new FsmDataStructure(this,type);
			SampleFsm(data);
		}
	}

	private void SampleFsm(FsmDataStructure info) {
		/* Just For Testing */
		FsmState Red = new FsmState("Red",Location.create(200,100));
		FsmState RedYellow = new FsmState("RedYellow",Location.create(300,200));
		FsmState Green = new FsmState("Green",Location.create(200,300));
		FsmState Yellow = new FsmState("Yellow",Location.create(100,200));
		info.AddState(Red);
		info.AddState(RedYellow);
		info.AddState(Green);
		info.AddState(Yellow);
		info.AddUnconditionalTransition(Red, RedYellow);
		info.AddUnconditionalTransition(RedYellow,Yellow);
		info.AddUnconditionalTransition(Yellow,Green);
		info.AddUnconditionalTransition(Green,Green);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> V getValue(Attribute<V> attr) {
		if (attr == StdAttr.LABEL) {
			return (V) label;
		} else if (attr == StdAttr.LABEL_VISIBILITY) {
			return (V) labelVisible;
		} else if ((attr == FSMCONTENT_ATTR)|(attr == FSMRESETSTATE_ATTR)) {
			return (V) data;
		} else if (attr == FSM_TYPE) {
			if (data == null)
				return (V) type;
			else return (V) data.GetType();
		} else if (attr == FSM_CODING) {
			if (data == null)
				return (V) FSM_BINARYCODED;
			else return (V) data.GetCoding();
		} else if (attr == FSM_RESET) {
			if (data == null)
				return(V) FSM_RESETSYNCHRON;
				else return (V) data.GetResetBehavior();
		} else return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (attr == StdAttr.LABEL && value instanceof String) {
			String newLabel = (String) value;
			String oldlabel = label;
			if (label.equals(newLabel))
				return;
			label = newLabel;
			fireAttributeValueChanged(attr, value, (V) oldlabel);
		} else if (attr == StdAttr.LABEL_VISIBILITY && value instanceof Boolean) {
			Boolean newvis = (Boolean) value;
			if (labelVisible.equals(newvis))
				return;
			labelVisible=newvis;
			fireAttributeValueChanged(attr, value,null);
		} else if (attr == FSMCONTENT_ATTR && value instanceof FsmDataStructure) {
			FsmDataStructure NewVal = (FsmDataStructure) value;
			data = NewVal;
			fireAttributeValueChanged(attr, value,null);
		} else if (attr == FSM_TYPE) {
			if (data == null)
				return;
			AttributeOption NewVal = (AttributeOption) value;
			if (data.GetType().equals(NewVal))
				return;
			data.SetType(NewVal);
			fireAttributeValueChanged(attr, value,null);
		} else if (attr == FSM_CODING) {
			if (data == null)
				return;
			AttributeOption NewVal = (AttributeOption) value;
			if (data.GetCoding().equals(NewVal))
				return;
			data.SetCoding(NewVal);
			fireAttributeValueChanged(attr, value,null);
		} else if (attr == FSM_RESET) {
			if (data == null)
				return;
			AttributeOption NewVal = (AttributeOption) value;
			if (data.GetResetBehavior().equals(NewVal))
				return;
			data.SetResetBehavior(NewVal);
			fireAttributeValueChanged(attr, value,null);
		}
	}

}
