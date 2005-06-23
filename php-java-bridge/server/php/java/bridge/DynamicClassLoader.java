package php.java.bridge;

import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.lang.ref.*;
import java.io.*;
/**
 * <p>Title: php-java-bridge</p>
 * <p>
 * This class implements a class loader, which keeps track of a dynamic list of other delegate URLClassLoaders
 * It is possible to change the list of these Classloaders during execution. The classloaders themselves, as well as
 * their corresponding classes are globally cached.
 * </p>
 * <p>
 * In case of file:// URLs, this classloader also handles reloading of Jar-Files once they are modified.
 * This is possible <b>without restarting the JVM</b>
 * It keeps track of the latest file modification times, and reloads Jar files if neccessary.
 * It is also possible to add an URL pointing to a directory of simple class files.
 * This is slow, though, and only recommended for quick and dirty development use since
 * it will *always* reload
 * </p>
 * <p>Copyright: PHP-License</p>
 * <p>http://sourceforge.net/projects/php-java-bridge</p>
 * @author Kai Londenberg
 * @version 2.06
 */

public class DynamicClassLoader extends SecureClassLoader {

  protected static HashMap classLoaderCache = new HashMap(); // Global Cache Map of Classpath=>Soft Reference=>URLClassLoaderEntry
  protected static WeakHashMap parentCacheMap = new WeakHashMap(); // Holds global caches for parent Classloaders
  public static long defaultCacheTimeout = 2000;  // By default minumum file modification check interval is 2 seconds, that should be fast enough :)
  public static boolean defaultLazy = true;  // By default lazy classpath addition
  private static final String nf = "not found"; // Dummy entry for cache maps if a class or resource can't be found.
  private static int instanceCount = 0;
  private static long debugStart = System.currentTimeMillis();

  protected class URLClassLoaderEntry {
    URLClassLoader cl;
    long lastModified;
    HashMap cache = new HashMap(); // Cache for this URLClassLoader

    protected URLClassLoaderEntry (URLClassLoader cl, long lastModified) {
      this.cl = cl;
      this.lastModified = lastModified;
    }
  }

  public static void debugMsg(String str) {
    Util.logStream.println((System.currentTimeMillis()-debugStart)+"::"+str);
    Util.logStream.flush();
  }

  public static void clearCache() {
    classLoaderCache.clear();
  }

  /**
   * Invalidates a given classpath, so that the corresponding classloader gets reloaded.
   * @param classpath
   */
  public static void invalidate(URL urls[]) {
      invalidate(getStringFromURLArray(urls));
  }

  /**
   * Invalidates a given classpath, so that the corresponding classloader gets reloaded.
   * This method should be called from PHP to signal that a given codebase has been modified.
   * @param classpath
   */
  public static void invalidate(String classpath) {
      classLoaderCache.remove(classpath);
  }

  public final static String getStringFromURLArray(URL urls[]) {
      if (urls.length==0) return "";
      StringBuffer cp = new StringBuffer(urls[0].toExternalForm());
      for (int i=1;i<urls.length;i++) {
        cp.append(';');
        cp.append(urls[i].toExternalForm());
      }
      return cp.toString();
  }

  public final static URL[] getURLArrayFromString(String cp) throws MalformedURLException {
      StringTokenizer st = new StringTokenizer(cp, ";", false);
      ArrayList urls = new ArrayList();
      while (st.hasMoreTokens()) {
        String urlStr = st.nextToken();
        URL u = new URL(urlStr);
        urls.add(urls);
      }
      URL u[] = new URL[urls.size()];
      urls.toArray(u);
      return u;
  }

  protected int instanceIndex = 0;
  protected HashMap classLoaders = new HashMap(); // Map of Classpath=>URLClassLoaderEntries of this DynamicClassLoader (Hard References)
  protected LinkedList classPaths = new LinkedList(); // List of Classpaths (corresponding to URLClassLoaderEntries) of this DynamicClassLoader
  protected LinkedList urlsToAdd = new LinkedList(); // List of URLs to add (lazy evaluation)
  protected long cacheTimeout = 5000; // Minimum interval to check for file modification dates
  protected boolean lazy = true; // Lazy Classloader Creation ?
  protected HashMap parentCache = null; // Fetched globally from parentCacheMap

  public DynamicClassLoader(ClassLoader parent) {
    super(parent);
    this.cacheTimeout = defaultCacheTimeout;
    this.lazy = defaultLazy;
    this.instanceIndex = instanceCount++;
    // Load global cache for the parent
    parentCache = (HashMap)parentCacheMap.get(parent);
    if (parentCache==null) {
      parentCache = new HashMap();
      parentCacheMap.put(parent, parentCache);
    }
  }

  public DynamicClassLoader() {
    super();
    ClassLoader parent = ClassLoader.getSystemClassLoader();
    this.cacheTimeout = defaultCacheTimeout;
    this.lazy = defaultLazy;
    // Load global cache for the parent
    parentCache = (HashMap)parentCacheMap.get(parent);
    if (parentCache==null) {
      parentCache = new HashMap();
      parentCacheMap.put(parent, parentCache);
    }
  }

  public void clear() {
    classLoaders.clear();
    classPaths.clear();
    urlsToAdd.clear();
  }

  public void setLazy(boolean lazy) {
    this.lazy = lazy;
  }

  public void setCacheTimeout(long cacheTimeoutMilliseconds) {
    this.cacheTimeout = cacheTimeoutMilliseconds;
  }

   public void addURLs(URL urls[]) {
      addURLs(getStringFromURLArray(urls), urls, lazy);
  }

  public void addURLs(URL urls[], boolean lazy) {
      addURLs(getStringFromURLArray(urls), urls, lazy);
  }

  public void addURLs(String urlClassPath) throws MalformedURLException {
       addURLs(urlClassPath, getURLArrayFromString(urlClassPath), lazy);
  }

  public void addURLs(String urlClassPath, boolean lazy) throws MalformedURLException {
      addURLs(urlClassPath, getURLArrayFromString(urlClassPath), lazy);
  }

  public void addURL(URL url, boolean lazy) {
      URL u[] = new URL[] {url};
      addURLs(u, lazy);
  }

  public void addURL(URL url) {
      URL u[] = new URL[] {url};
      addURLs(u, lazy);
  }

  protected void addURLs(String classPath, URL urls[], boolean lazy) {
      if (lazy) {
        lazyAddURLs(classPath, urls);
      } else {
        realAddURLs(classPath, urls);
      }
  }

  protected URLClassLoaderEntry realAddURLs(String classPath, URL urls[]) {
      URLClassLoaderEntry entry = getClassPathFromCache(classPath);
      if (entry==null) {
        entry = createURLClassLoader(classPath, urls);
      } else {
        long time = System.currentTimeMillis();
        if (entry.lastModified+cacheTimeout<time) {
          long urlsLastModified = getLastModified(urls);
          if (urlsLastModified>entry.lastModified) {
            entry = createURLClassLoader(classPath, urls);
          }
        }
      }
      return entry;
  }

  protected void lazyAddURLs(String classPath, URL urls[]) {
      Object params[] = new Object[] {classPath, urls};
      urlsToAdd.add(params);
  }

  protected URLClassLoaderEntry addDelayedURLs() {
      if (urlsToAdd.isEmpty()) return null;
      Object params[] = (Object[]) urlsToAdd.getFirst();
      urlsToAdd.removeFirst();
      return realAddURLs((String)params[0], (URL[])params[1]);
  }

  protected long getLastModified(URL urls[]) { // Returns the highest modification date from all URLs
      long lastModified = 0;
      for (int i=0;i<urls.length;i++) {
        URL u = urls[i];
        long lm = 0;
        try {
          if (u.getProtocol().equals("file")) {
              File f = new File(u.getPath());
              if (f.isFile()) {
                lm = f.lastModified();
              } else if (f.isDirectory()) {
                return 0; // Directories and non-file URLs are considered to be never modified, except through a static call to "invalidate"
              }
          } else {
              URLConnection conn = u.openConnection();
              lm = conn.getLastModified();
          }
          if (lm>lastModified) lastModified = lm;
        } catch (IOException ioe) {
        }
      }
      return lastModified;
  }

  protected URLClassLoaderEntry createURLClassLoader(String classPath, URL urls[]) {
    URLClassLoader cl = new URLClassLoader(urls, this.getParent());
    URLClassLoaderEntry entry = new URLClassLoaderEntry(cl, System.currentTimeMillis());
    classLoaders.put(classPath, entry);
    SoftReference cacheEntry = new SoftReference(entry);
    classLoaderCache.put(classPath, cacheEntry);
    return entry;
  }

  protected URLClassLoaderEntry getClassPathFromCache(String classPath) {
      Object o = classLoaders.get(classPath);
      if (o==null) {
        o = classLoaderCache.get(classPath);
        if (o!=null) {
          o = ((SoftReference)o).get(); // Caching with SoftReferences to avoid OutOfMemoryExceptions
        }
      }
      return (URLClassLoaderEntry)o;
  }


  protected void addURLClassLoader(String loaderClasspath, URLClassLoader cl, long lastModified) {
    URLClassLoaderEntry entry = (URLClassLoaderEntry)classLoaders.get(loaderClasspath);
    if (entry==null) { // Check for duplicate entry
        entry = new URLClassLoaderEntry(cl, lastModified);
        classLoaders.put(loaderClasspath, entry);
    } else { // If neccessary, update
      if (entry.lastModified<lastModified) {
        entry.cl = cl;
        entry.lastModified = lastModified;
      }
    }
  }

  /**
   *
   * I have decided to override loadClass instead of findClass,
   * so that this method will actually get to re-load
   * classes if neccessary. Otherwise, the Java system would call
   * the final method "getLoadedClass(name)", (i.e. use it's own caching) without
   * dynamically re-loading classes if neccessary.
   */
  public Class loadClass(String name) throws ClassNotFoundException {
      Class result = null;
      Object c = parentCache.get(name);
      if (c!=nf) {
        if (c!=null) return (Class)c;
        try {
          result = super.loadClass(name);
          parentCache.put(name, result);
          return result;
        } catch (ClassNotFoundException cnfe) {
          parentCache.put(name, nf);
        }
      }
      Iterator iter = classPaths.iterator();
      URLClassLoaderEntry e = null;
      while (iter.hasNext()) {
        e = (URLClassLoaderEntry) classLoaders.get(iter.next());
        c = e.cache.get(name);
        if (c!=nf) {
          if (c!=null) return (Class)c;
          try {
            result = e.cl.loadClass(name);
            e.cache.put(name, result);
            return result;
          } catch (ClassNotFoundException cnfe) {
            e.cache.put(name, nf);
          }
        }
      }
      e = addDelayedURLs();
      while (e!=null) {
        c = e.cache.get(name);
        if (c!=nf) {
          if (c!=null) return (Class)c;
          try {
            result = e.cl.loadClass(name);
            e.cache.put(name, result);
            return result;
          } catch (ClassNotFoundException cnfe) {
            e.cache.put(name, nf);
          }
        }
        e = addDelayedURLs();
      }
      if (result==null) {
        throw new ClassNotFoundException("Class "+name+" not found");
      }
      return result;
  }

  // Not cached
  public Enumeration findResources(String name) throws java.io.IOException {
      Vector result = null;
      Enumeration enum = super.findResources(name);
      while (enum.hasMoreElements()) {
         result.add(enum.nextElement());
      }
      Iterator iter = classPaths.iterator();
      URLClassLoaderEntry e = null;
      while (iter.hasNext()) {
        e = (URLClassLoaderEntry) classLoaders.get(iter.next());
        enum = e.cl.findResources(name);
        while (enum.hasMoreElements()) {
          result.add(enum.nextElement());
        }
      }
      e = addDelayedURLs();
      while (e!=null) {
        enum = e.cl.findResources(name);
        while (enum.hasMoreElements()) {
          result.add(enum.nextElement());
        }
        e = addDelayedURLs();
      }
      return result.elements();
  }

  public URL findResource(String name)  {
      String cacheName = "@"+name; // definitely different from class-names
      Object c = parentCache.get(cacheName);
      if ((c!=nf) && (c!=null)) return (URL)c;
      c = super.findResource(name);
      if (c!=null) {
        parentCache.put(cacheName, c);
        return (URL)c;
      } else {
        parentCache.put(cacheName, nf);
      }
      Iterator iter = classPaths.iterator();
      URLClassLoaderEntry e = null;
      while (iter.hasNext()) {
        e = (URLClassLoaderEntry) classLoaders.get(iter.next());
        c = e.cache.get(cacheName);
        if ((c!=nf) && (c!=null)) return (URL)c;
        c = e.cl.findResource(name);
        if (c!=null) {
          e.cache.put(cacheName, c);
          return (URL)c;
        } else {
          e.cache.put(cacheName, nf);
        }
      }
      e = addDelayedURLs();
      while (e!=null) {
        c = e.cache.get(cacheName);
        if ((c!=nf) && (c!=null)) return (URL)c;
        c = e.cl.findResource(name);
        if (c!=null) {
          e.cache.put(cacheName, c);
          return (URL)c;
        } else {
          e.cache.put(cacheName, nf);
        }
        e = addDelayedURLs();
      }
      return null;
  }

}