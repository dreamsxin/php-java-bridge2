/*-*- mode: Java; tab-width:8 -*-*/

package php.java.bridge;

import java.util.HashMap;
import java.util.Map;

public class Session{
	HashMap map;
	private String name;
	private static int sessionCount=0;
	boolean isNew=true;
	
	public Object get(Object ob) {
		return map.get(ob);
	}
	
	public void put(Object ob1, Object ob2) {
		map.put(ob1, ob2);
	}
	
	public Object remove(Object ob) {
		return map.remove(ob);
	}
	
	public Session(String name) {
		this.name=name;
		sessionCount++;
		this.map=new HashMap();
	}

	public void setTimeout(long timeout) {
		throw new NotImplementedException();
	}
	
	public long getTimeout() {
		throw new NotImplementedException();
	}
	
	public int getSessionCount() {
		return sessionCount;
	}
	
	public boolean isNew() {
		return isNew;
	}
	
	public void destroy() {
		sessionCount--;
		synchronized(JavaBridge.sessionHash) {
			if(JavaBridge.sessionHash!=null)
				JavaBridge.sessionHash.remove(name);
			}
	}
	
	public void destroySession() {
		destroy();
	}

	public void putAll(Map vars) {
		map.putAll(vars);
	}
}
