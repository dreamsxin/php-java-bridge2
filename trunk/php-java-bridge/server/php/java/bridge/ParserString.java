/*-*- mode: Java; tab-width:8 -*-*/


package php.java.bridge;

import java.io.UnsupportedEncodingException;

/**
 * This class holds the parser string.
 * @author jostb
 *
 */
public class ParserString {
    byte[] string;
    int off;
    int length;

    /**
     * Returns the UTF8 string representation. Useful for debugging only
     * @return The UTF-8 encoded string.
     */
    public String getUTF8StringValue() {
        try { 
	    return new String(string, off, length, Util.UTF8);
        } catch (java.io.UnsupportedEncodingException e) { 
	    Util.printStackTrace(e);
	    return new String(string, off, length);
	}
    }
    /**
     * Returns the string encoded via java_set_file_encoding().
     * @param options The current request options.
     * @return The encoded string value.
     * @see php.java.bridge.Options#getEncoding()
     */
    public String getStringValue(Options options) {
        return options.newString(string, off, length);
    }
    /**
     * Returns the ASCII string representation. Useful for serialized objects, float, long.
     * @return The ASCII encoded string.
     */
    public String getASCIIStringValue() {
        try {
            return new String(string, off, length, Util.ASCII);
        } catch (UnsupportedEncodingException e) {
            return new String(string, off, length);
        }
    }
    /**
     * Returns the int value.
     * @return The int value.
     */
    public int getIntValue() {
        int sign;
        if(length==0) return 0;
        int off = this.off;
        int length = this.length;
        int val = 0;
        
        if(string[off]=='-') { 
            off++; length--; sign=-1;
        }
        else if(string[off]=='+') { 
            off++; length--; sign=1; 
        }
        else sign=1;
        
        int pos=1;
        while(length-->0) {
            val+=((int)(string[off+length]-(byte)'0')) * pos;
            pos*=10;
        }
        return val*sign;
    }
    /**
     * Returns the long value.
     * @return The long value.
     */
    public long getLongValue() {
        long sign;
        if(length==0) return 0;
        int off = this.off;
        int length = this.length;
        long val = 0;
        
        if(string[off]=='-') { 
            off++; length--; sign=-1;
        }
        else if(string[off]=='+') { 
            off++; length--; sign=1; 
        } 
        else sign=1;
        
        long pos=1;
        while(length-->0) {
            val+=((long)(string[off+length]-(byte)'0')) * pos;
            pos*=10;
        }
        return val*sign;
    }
    
    /**
     * Returns the double value.
     * @return The double value.
     */
    public double getDoubleValue() {
        return(Double.parseDouble(getASCIIStringValue()));
    }
    /**
     * Returns the UTF8 string representation. Useful for debugging only
     * @return The description of the string.
     */
    public String toString() {
    	return "{" + getUTF8StringValue() + " @:" + String.valueOf(off) + " l:" + String.valueOf(length) + "}";
    }
}
