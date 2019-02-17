package com.cburch.logisim.prefs;

import static com.cburch.logisim.gui.Strings.S;

import java.util.ArrayList;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.ProbeAttributes;

public class PrefMonitorBooleanConvert extends PrefMonitorBoolean {
	
	private ArrayList<ConvertEventListener> MyListeners = new ArrayList<ConvertEventListener>();

	PrefMonitorBooleanConvert(String name, boolean dflt) {
		super(name,dflt);
	}
	
	public void addConvertListener(ConvertEventListener l) {
		if (!MyListeners.contains(l))
			MyListeners.add(l);
	}
	
	public void removeConvertListener(ConvertEventListener l) {
		if (MyListeners.contains(l))
			MyListeners.remove(l);
	}
	
	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		Preferences prefs = event.getNode();
		String prop = event.getKey();
		String name = getIdentifier();
		if (prop.equals(name)) {
			boolean oldValue = value;
			boolean newValue = prefs.getBoolean(name, dflt);
			if (newValue != oldValue) {
				value = newValue;
				AppPreferences.firePropertyChange(name, oldValue, newValue);
				if (!MyListeners.isEmpty()) {
					ConvertEvent e = new ConvertEvent(newValue?ProbeAttributes.APPEAR_EVOLUTION_NEW:StdAttr.APPEAR_CLASSIC);
					Object[] options = {S.get("OptionYes"),S.get("OptionNo")};
					int ret = JOptionPane.showOptionDialog(null, 
							S.fmt("OptionConvertAllPinsProbes", e.GetValue().getDisplayGetter().toString()), 
							S.get("OptionConvertAll"), JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					if (ret == JOptionPane.YES_OPTION) {
						fireConvertAction(e);
					}
				}
			}
		}
	}

	private void fireConvertAction(ConvertEvent e) {
		for (ConvertEventListener l : MyListeners)
			l.AttributeValueChanged(e);
	}
}
