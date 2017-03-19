package php.java.bridge.util;

import java.io.PrintStream;


/**
 * Only for internal use. Use Logger.getLogger() instread.
 * 
 * A bridge which uses log4j or the default logger.
 *
 */
public class Logger {

    private static ILogger defaultLogger;
    public static ILogger getLogger() {
	if (defaultLogger==null){
	    defaultLogger = new FileLogger();
	}
	return defaultLogger;
    }
    public void setLogger(ILogger logger) {
	defaultLogger = logger;
    }

    /**
     * The logStream, defaults to System.err
     */
    static PrintStream logStream;
        
    /**
     * The loglevel:<br>
     * 0: log off <br>
     * 1: log fatal <br>
     * 2: log messages/exceptions <br>
     * 3: log verbose <br>
     * 4: log debug <br>
     * 5: log method invocations
     */
    public static int logLevel;


    /**
     * print a message on a given log level
     * @param level The log level
     * @param msg The message
     */
    public static void println(int level, String msg) {
	getLogger().log(level, msg);
    }
    
    /**
     * Display a warning if logLevel &gt;= 1
     * @param msg The warn message
     */
    public static void warn(String msg) {
	if(logLevel<=0) return;
	getLogger().warn(msg);
    }
    
    /**
     * Display a stack trace if logLevel >= 1
     * @param t The Throwable
     */
    public static void printStackTrace(Throwable t) {
        getLogger().printStackTrace(t);
    }
    /**
     * Display a debug message
     * @param msg The message
     */
    public static void logDebug(String msg) {
        if(logLevel>3) println(4, msg);
    }
    
    /**
     * Display a fatal error
     * @param msg The error
     */
    public static void logFatal(String msg) {
	if(logLevel>0) println(1, msg);
    }
    
    /**
     * Display an error or an exception
     * @param msg The error or the exception
     */
    public static void logError(String msg) {
	if(logLevel>1) println(2, msg);
    }
    
    /**
     * Display a message
     * @param msg The message
     */
    public static void logMessage(String msg) {
	if(logLevel>2) println(3, msg);
    }
    /**
     * Sets the fall back logger, used when no thread-local logger exists. The default logger is initialized with: <code>new Logger(new FileLogger())</code>. 
     * @param logger the logger
     * @see #logDebug
     */
    public static synchronized void setDefaultLogger(ILogger logger) {
	Logger.defaultLogger = logger;
    }
    /**
     * @return Returns the logger.
     */
    public static ILogger getDefaultLogger() {
	return defaultLogger;
    }

    /** Redirect System.out and System.err to the configured logFile or System.err.
     * System.out is always redirected, either to the logFile or to System.err.
     * This is because System.out is reserved to report the status back to the 
     * container (IIS, Apache, ...) running the JavaBridge back-end.
     * @param redirectOutput this flag is set, if natcJavaBridge has already redirected stdin, stdout, stderr
     * @param logFile the log file
     */
    public static void redirectOutput(String logFile) {
	redirectJavaOutput(logFile);
    }
    public static void redirectJavaOutput(String logFile) {
	Logger.logStream = System.err;
        if(logFile != null && logFile.length()>0) 
            try {
        	Logger.logStream=new java.io.PrintStream(new java.io.FileOutputStream(logFile));
            } catch (Exception e) {e.printStackTrace();}
            try { System.setErr(logStream); } catch (Exception e) {e.printStackTrace(); }
	try { System.setOut(logStream); } catch (Exception e) {e.printStackTrace(); System.exit(9); }
    }
    public static void setDefaultLogger(String logFile) {
	  setDefaultLogger(new FileLogger());
    }
}