package com.cburch.logisim.statemachine.bdd;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BidirHashMap<T1,T2> {

	private Map<T2,T1> mapleft;
	private Map<T1,T2> mapright;
	
	public BidirHashMap() {
		mapleft = new HashMap<T2, T1>();
		mapright = new HashMap<T1, T2>();
	}
	
	public void link(T1 object,T2 object2) {
		mapleft.put(object2, object);
		mapright.put(object, object2);
	}
	
	public T2 get(T1 object) {
		return mapright.get(object);
	}

	public T1 getInverse(T2 object) {
		return mapleft.get(object);
	}
	
	public boolean containsKey(T1 key) {
		return mapright.containsKey(key);
	}

	public boolean containsValue(T2 value) {
		return mapleft.containsKey(value);
	}

	public Set<T1> keySet() {
		return mapright.keySet();
	}

	public Set<T2> valueSet() {
		return mapleft.keySet();
	}

	@Override
	public String toString() {
		return "BidirHashMap [mapleft=" + mapleft + "]";
	}
	
	
}
