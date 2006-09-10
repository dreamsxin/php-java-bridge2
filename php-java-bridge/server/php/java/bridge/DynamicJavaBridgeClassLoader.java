/*-*- mode: Java; tab-width:8 -*-*/

/*
 * Copyright (C) 2005, 2006 Kai Londenberg, Jost Boekemeier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER(S) OR AUTHOR(S) BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package php.java.bridge;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The bridge class loader which uses the DynamicClassLoader when possible.
 * @author jostb
 *
 */
/*  
 * Instances of this class are shared. 
 * Use the JavaBridgeClassLoader if you need to access the current request-handling bridge instance. 
 * */
public class DynamicJavaBridgeClassLoader extends DynamicClassLoader {
    // maps rawPath -> URL[]
    private static Map urlCache = Collections.synchronizedMap(new HashMap()); //TODO: keep only recent entries
	    
    protected DynamicJavaBridgeClassLoader(DynamicJavaBridgeClassLoader other) {
    	super(other);
    }
    protected DynamicJavaBridgeClassLoader(ClassLoader parent) {
    	super(parent);
    }
    protected DynamicJavaBridgeClassLoader() {
    	super();
    }
    private static final URL[] EMPTY_URL = new URL[0];
    private static final JarLibraryPath EMPTY_PATH = new JarLibraryPath() {public URL[] getURLs() { return EMPTY_URL; } };
    /** Holds a checked JarLibraryPath entry */
    public static class JarLibraryPath {
	private String path;

	private boolean isCached;
	private String rawPath, rawContextDir;
	private URL[] urls;
	/** create an invalid entry */
	protected JarLibraryPath() { isCached = true; }
	/** Create a checked JarLibraryPath entry
	 * @param rawPath The path or file in the local file system or url
	 * @param rawContextDir The context directory, for example c:\php
	 * @throws IOException, if the local path or file does not exist or cannot be accessed
	 */
        public JarLibraryPath(String rawPath, String rawContextDir) throws IOException {
            if(rawContextDir == null) throw new NullPointerException("rawContextDir");
            this.rawPath = rawPath;
            // How to check that rawContextDir is really a symbol?
            this.rawContextDir = rawContextDir;
    	    this.path = makePath(rawPath);
    	    
    	    this.urls = checkURLs();
        }
        private boolean hasResult;
        private int result = 1;
        public int hashCode() {
	    if(hasResult) return result;
	    result = result * 31 + rawPath.hashCode(); 
	    result = result * 31 + rawContextDir.hashCode();
	    hasResult = true;
	    return result;
	}
	public boolean equals(Object o) {
	    if(o==null) return false;
	    JarLibraryPath that = (JarLibraryPath) o;
	    if(rawContextDir != that.rawContextDir) return false;
	    if(!rawPath.equals(that.rawPath)) return false;
	    return true;
	}
        
        private String makePath(String rawPath) {
          /*
           * rawPath always starts with a token separator, e.g. ";" 
  	     */
  	    // add a token separator if first char is alnum
  	    char c=rawPath.charAt(0);
  	    if((c>='A' && c<='Z') || (c>='a' && c<='z') ||
  		(c>='0' && c<='9') || (c!='.' || c!='/'))
  	      rawPath = ";" + rawPath;

  	    return rawPath;
        }
        private String makeContextDir(String rawContextDir) {
            rawContextDir = new File(rawContextDir, "lib").getAbsolutePath();
            return rawContextDir;
        }
	/**
	 * Return the urls associated with this entry
	 * @return The url value
	 * @throws IOException 
	 */
        public URL[] getURLs() {
            return urls;
        }
        private URL[] checkURLs() throws IOException {
            /*
             * Check the cache
             */
  	    URL[] urls = (URL[]) urlCache.get(this);
  	    if(urls != null) { this.urls = urls; isCached = true; return urls; } 

  	    isCached = false;
  	    return createUrls();
        }
        private URL[] createUrls() throws IOException {
          /*
           * Parse the path.
           */
      	List toAdd = new LinkedList();
  	String currentPath = path.substring(1);
  	StringTokenizer st = new StringTokenizer(currentPath, path.substring(0, 1));
        String contextDir = makeContextDir(rawContextDir);
  	while (st.hasMoreTokens()) {
  	    URL url;
  	    String s;
  	    s = st.nextToken();

  	    try {
  		url = new URL(s);
  		checkUrl(url);
  	    } catch (MalformedURLException e) {
  		try {
  		    File f=null;
  		    File file=null;
  		    StringBuffer buf= new StringBuffer();
  		    if((f=new File(s)).isFile() || f.isAbsolute()) {
  		    	buf.append(s); file = f;
  		    } else if ((f=new File(contextDir, s)).isFile()) {
  		    	buf.append(f.getAbsolutePath()); file = f;
  		    } else if ((f=new File("/usr/share/java/"+ s)).isFile()) {
  			buf.append(f.getAbsolutePath()); file = f;
  		    } else {
  			buf.append(s); file = new File(s);
  		    }
  		    /* From URLClassLoader:
  		    ** This class loader is used to load classes and resources from a search
  		    ** path of URLs referring to both JAR files and directories. Any URL that
  		    ** ends with a '/' is assumed to refer to a directory. Otherwise, the URL
  		    *
  		    * So we must replace the last backslash with a slash or append a slash
  		    * if necessary.
  		    */
  		    if(file!=null && file.isDirectory()) {
                          addJars(toAdd, f);
  		    	int l = buf.length();
  		    	if(l>0) {
  			    if(buf.charAt(l-1) == File.separatorChar) {
  				buf.setCharAt(l-1, '/');
  			    } else if(buf.charAt(l-1)!= '/') {
  				buf.append('/');
  			    }
  			}
  		    } 
  		    if(!file.isDirectory()) checkJarFile(file);
  		    url = new URL("file", null, buf.toString());
  		}  catch (MalformedURLException e1) {
  		    Util.printStackTrace(e1);
  		    continue;
  		}
  	    }
  	    toAdd.add(url);
  	}
	URL[] urls = new URL[toAdd.size()];
        toAdd.toArray(urls);
        return urls;
	}
        /** Return the path
         * @return the key
         */
	public String getPath() {
            return path;
        }
	/**
	 * Adds this entry to the cache
	 */
	public void addToCache() {
	    if(!isCached) { urlCache.put(this, urls); urls=null; }
	}
    }
    /** Set the library path for the bridge instance. Examples:
     * setJarLibPath(";file:///tmp/test.jar;file:///tmp/my.jar");<br>
     * setJarLibPath("|file:c:/t.jar|http://.../a.jar|jar:file:///tmp/x.jar!/");<br>
     * The first char must be the token separator.
     * @param rawPath The path
     * @param rawContextDir The context dir, e.g. /usr/lib/php/extensions
     * @throws IOException 
     */
    public static JarLibraryPath checkJarLibraryPath(String rawPath, String rawContextDir) throws IOException {
        if(rawPath==null || rawPath.length()<1) return EMPTY_PATH;
    	return new JarLibraryPath(rawPath, rawContextDir);
    }
    /** Update the library path for the bridge instance. Examples:
     * setJarLibPath(";file:///tmp/test.jar;file:///tmp/my.jar");<br>
     * setJarLibPath("|file:c:/t.jar|http://.../a.jar|jar:file:///tmp/x.jar!/");<br>
     * The first char must be the token separator.
     * @param rawPath The path
     * @param rawContextDir The context dir, e.g. /usr/lib/php/extensions
     * @throws IOException 
     */
    public void updateJarLibraryPath(String rawPath, String rawContextDir) throws IOException {
        updateJarLibraryPath(checkJarLibraryPath(rawPath, rawContextDir));
    }
    /** Update the library path for the bridge instance. 
     * @param path the checked JarLibraryPath
     * @throws IOException 
     * @see #checkJarLibraryPath(String, String)
     */
    public void updateJarLibraryPath(JarLibraryPath path) {
        String key = path.getPath();
        URL[] urls = path.getURLs();
        if(urls.length>0)
            addURLs(key, urls, false); // Uses protected method to explicitly set the classpath entry that is added.
        path.addToCache();
    }
    private static void checkUrl(URL url) {
        url.getProtocol();
    }
    private static void checkJarFile(File f) throws IOException {
        try {
            doCheckJarFile(f);
        } catch (IOException e) {
            IOException ex = new IOException("Could not open jar file " + f + ", reason: " +(String.valueOf(e.getMessage())));
            ex.initCause(e);
            throw ex;
        }
    }
    private static void doCheckJarFile(File f) throws IOException {
        JarFile jar = new JarFile(f);
        Manifest mf = jar.getManifest();
        if(Util.logLevel>4) {
            if(mf!=null) {
                Set main = mf.getMainAttributes().entrySet();
                if(Util.logLevel>5) Util.logDebug("ClassLoader: loaded file: " + f + ", main attributes: " + main);
            }
        }
        jar.close();
        
    }
    /*
     * Add all .jar files in a directory
     */
    static void addJars(List list, File dir) {
	File files[] = dir.listFiles();
	if(files==null) return;
	for(int i=0; i<files.length; i++) {
	    File f = files[i];
	    if(f.getName().endsWith(".jar")) {
		try {
		    list.add(new URL("file", null, f.getAbsolutePath()));
		} catch (MalformedURLException e) {
		    Util.printStackTrace(e);
		}
	    }
	}
    }
    /**@deprecated*/
    public static synchronized void initClassLoader(String phpConfigDir) {
    }
    /**
     * The VM associates a map with each loader to speed up Class.forName(). Since our
     * loader can shrink, we discard the VM cache when clear() or reset() is called.
     * @return A new instance which should be used instead of the current instance.
     */
    public DynamicJavaBridgeClassLoader clearVMLoader() {
	DynamicJavaBridgeClassLoader that = new DynamicJavaBridgeClassLoader(this);
	copyInto(that);
	return that;
    }
    /**
     * Reset to initial state.
     */
    public void reset() {
	synchronized(getClass()) {
	    clearLoader();
	    clearCache();
	}
    }
    /**
     * Clear all loader caches. 
     */
    public void clearCaches() {
	clearLoaderCaches();
    }
    /**
     * Clear the loader so that it can be used in new requests.
     */
    public void clear() {
	clearLoader();
    }
    private static final boolean checkVM() {
	try {
	    return "libgcj".equals(System.getProperty("gnu.classpath.vm.shortname"));
	} catch (Throwable t) {
	    return false;
	}
    }
    private static final String getLD_LIBRARY_PATH() {
	try {
	    return System.getProperty("java.library.path");
	} catch (Throwable t) {
	    return "[error: no java.library.path set]";
	}
    }
    private static final String getCLASSPATH() {
	try {
	    return System.getProperty("java.ext.dirs");
	} catch (Throwable t) {
	    return "[error: no java.ext.dirs set]";
	}
    }
    static final boolean IS_GNU_JAVA = checkVM();
    static final String LD_LIBRARY_PATH = getLD_LIBRARY_PATH();
    static final String CLASSPATH = getCLASSPATH();

    /**
     * Searches for a library name in our classpath
     * @param name the library name, e.g. natcJavaBridge.so
     * @return never returns.  It throws a UnsatisfiedLinkError.
     * @throws UnsatisfiedLinkError
     */
    protected String resolveLibraryName(String name) {
	URL url =  findResource("lib"+name+".so");
	if(url==null) url = findResource(name+".dll");
	if(url!=null) return new File(url.getPath()).getAbsolutePath();
	throw new UnsatisfiedLinkError("Native library " + name + " could not be found in java_require() path.");
    }
    protected URLClassLoaderFactory getUrlClassLoaderFactory() {
    	return new URLClassLoaderFactory() {
    	        public URLClassLoader createUrlClassLoader(String classPath, URL urls[], ClassLoader parent) {
		    URLClassLoader loader = new URLClassLoader(urls, parent) {
		      	    public String toString() {
		      	        return String.valueOf(arrayToString(this.getURLs()));
		      	    }
		      	    private Map cache = new HashMap();
    	                    public Class loadClass(String name) throws ClassNotFoundException {                      
    	                        Class result = null;
    	                  	Object c = null;
    	                    	if(Util.logLevel>4) Util.logDebug("trying to load class: " +name + " from: "+ "LOADER-ID"+System.identityHashCode(this));
				c = cache.get(name);
				if (c != nf) {
				    if (c != null)
					return (Class) c;
				    try {
					result = doLoadClass(name);
					cache.put(name, result);
					return result;
				    } catch (ClassNotFoundException cnfe) {
					cache.put(name, nf);
				    }
    	                    	}
				throw new ClassNotFoundException(name);
			    }
			    private Class doLoadClass(String name) throws ClassNotFoundException {
  	                    	ClassLoader parent;
  	                    	Class c = null;
  	                    	try {
  	                    	    if((parent=getParent())!=null) 
  	                    	    	try {
  	                    	    	    c = parent.loadClass(name);
  	                    	    	} catch (ClassNotFoundException ex) {/*ignore*/}
  	                    	    if(c==null) c = super.findClass(name);
  	                    	    if(c!=null) return c;
  	                    	} catch (ClassNotFoundException e) {
  	                    	    throw e;
				} catch (Exception ex) {
				    throw new ClassNotFoundException("Class " + name + " not found due to exception: " + ex + ".", ex);
				}
				throw new ClassNotFoundException(name);
			    }
			    public URL findResource(String name)  {
				return super.findResource(name);
    	                	
			    }
			    protected String findLibrary(String name) {
				if(Util.logLevel>4) Util.logDebug("trying to load library: " +name + " from: "+"LOADER-ID"+System.identityHashCode(this));
				if(!IS_GNU_JAVA) throw new UnsatisfiedLinkError("This java VM can only load pure java libraries. Either use GNU java instead or move the java library to " + CLASSPATH + " and the shared library "+ name +" to "+ LD_LIBRARY_PATH);
				String s = super.findLibrary(name);
				if(s!=null) return s;
				return resolveLibraryName(name);
			    }
			};
			if(Util.logLevel>4) Util.logDebug("Added LOADER-ID"+System.identityHashCode(loader)+"\nOrigPath: " + classPath + "\nTranslated: "+ arrayToString(urls));
			return loader;
    	        }
	    };
    }
    public String toString() {
        StringBuffer buf = new StringBuffer();

        Iterator iter = classPaths.iterator();
	URLClassLoaderEntry e = null;
	while (iter.hasNext()) {
	    e = (URLClassLoaderEntry) classLoaders.get(iter.next());
	    buf.append(e.toString());
	    buf.append(";");
	}
        ClassLoader parent = getParent();
        if(parent!=null && parent instanceof URLClassLoader) {
            buf.append(String.valueOf(arrayToString(((URLClassLoader)parent).getURLs())));
            buf.append(";");
        }
	return buf.toString();
    }
    /*
     *  (non-Javadoc)
     * @see php.java.bridge.DynamicClassLoader#loadClass(String)
     */
    public Class loadClass(String name) throws ClassNotFoundException {
	try {
	    return super.loadClass(name); 
	} catch (ClassNotFoundException e) {
	    throw new ClassNotFoundException(("Could not find " + name + " in java_require() path. Please check the path and the SEL and File permissions."), e);    
	}
    }
    /**
     * Create an instance of the dynamic java bridge classloader
     * It may return null due to security restrictions on certain systems, so don't
     * use this method directly but call: 
     * new JavaBridgeClassLoader(bridge, DynamicJavaBridgeClassLoader.newInstance()) instead.
     */
    public static synchronized DynamicJavaBridgeClassLoader newInstance(ClassLoader parent) {
	try {
	    DynamicJavaBridgeClassLoader cl = new DynamicJavaBridgeClassLoader(parent);
	    cl.setUrlClassLoaderFactory(cl.getUrlClassLoaderFactory());
	    return cl;
	} catch (java.security.AccessControlException e) {
	    return null;
	}
    }
}
