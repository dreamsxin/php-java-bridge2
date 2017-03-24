package php.java.bridge.generated;
public class LauncherUnix {
    private static final String data = "#!/bin/sh\n"+
"# php fcgi launcher\n"+
"set -x\n"+
"\n"+
"PHP_FCGI_CHILDREN=\"$PHP_JAVA_BRIDGE_FCGI_CHILDREN\"\n"+
"export PHP_FCGI_CHILDREN\n"+
"\n"+
"strace -s 1024 -ff \"$@\" 1>&2 &\n"+
"trap \"kill $! && exit 0;\" 1 2 15\n"+
"read result 1>&2\n"+
"kill $!\n"+
"";
    public static final byte[] bytes = data.getBytes(); 
}