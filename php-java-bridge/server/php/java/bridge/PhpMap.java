/*-*- mode: Java; tab-width:8 -*-*/


package php.java.bridge;

public abstract class PhpMap {
    Object value;
    Object keyType; //null: key is integer (array), !null: key is string (hash)
    public PhpMap(Object value, Object keyType) {
	this.value=value;
	this.keyType=keyType;
	init();
    }
    abstract void init();
    public abstract Object currentData();
    public abstract byte[] currentKey();
    public abstract Object moveForward();
    public abstract Object hasMore();
    public Object getType() {
	return keyType;
    }

    public abstract boolean offsetExists(Object pos);
    public abstract Object offsetGet(Object pos);
    public abstract void offsetSet(Object pos, Object val);
    public abstract void offsetUnset(Object pos); //remove
}
