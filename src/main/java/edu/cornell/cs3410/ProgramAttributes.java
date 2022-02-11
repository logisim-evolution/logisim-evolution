package edu.cornell.cs3410;

import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.proj.Project;

import edu.cornell.cs3410.ProgramAssembler.Listing;

// one per chip: attributes for the chip (including the listing), launches the editor
public class ProgramAttributes extends AbstractAttributeSet {
    private static List<Attribute<?>> ATTRIBUTES = Arrays.asList(new Attribute<?>[]
        { Program32.CONTENTS_ATTR });

    private static WeakHashMap<Listing, ProgramFrame32> windowRegistry = new WeakHashMap<Listing, ProgramFrame32>();
    private Listing contents;

    static ProgramFrame32 getProgramFrame(Listing value, Project proj) {
        synchronized(windowRegistry) {
            ProgramFrame32 ret = windowRegistry.get(value);
            if (ret == null) {
                ret = new ProgramFrame32(value, proj);
                ret.setLocationRelativeTo(null);
                ret.setLocation(300, 200);
                windowRegistry.put(value, ret);
            }
            return ret;
        }
    }

    ProgramAttributes() { contents = new Listing(); }

    @Override
    protected void copyInto(AbstractAttributeSet dest) {
        ProgramAttributes d = (ProgramAttributes) dest;
        d.contents = contents.clone();
    }

    @Override
    public List<Attribute<?>> getAttributes() { return ATTRIBUTES; }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attr) {
        if (attr == Program32.CONTENTS_ATTR) return (V) contents;
        return null;
    }

    @Override
    public <V> void setValue(Attribute<V> attr, V value) {
        V oldValue = (V)contents;
        if(attr == Program32.CONTENTS_ATTR) {
            contents = (Listing) value;
        }
        fireAttributeValueChanged(attr, value, oldValue);
    }
}
