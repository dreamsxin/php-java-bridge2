sinclude(tests.m4/function_checks.m4)
sinclude(tests.m4/threads.m4)
sinclude(tests.m4/java_check_broken_stdio_buffering.m4)

PHP_ARG_WITH(java, for java support,
[  --with-java[=JAVA_HOME]        Include java support])


if test "$PHP_JAVA" != "no"; then

       JAVA_FUNCTION_CHECKS
       PTHREADS_CHECK
       PTHREADS_ASSIGN_VARS
       PTHREADS_FLAGS
       JAVA_CHECK_BROKEN_STDIO_BUFFERING

# the JAVA_HOME directory
	PHP_SUBST(PHP_JAVA)
# find includes eg. -I/opt/jdk1.4/include -I/opt/jdk1.4/include/linux
	PHP_EVAL_INCLINE(`for i in \`find $PHP_JAVA/include -type d -print\`; do echo -n "-I$i "; done`)
# create java.so
	PHP_NEW_EXTENSION(java, java.c java_bridge.c client.c proxyenv.c bind.c init_cfg.o ,shared)
	PHP_ADD_MAKEFILE_FRAGMENT
# create libnatcJavaBridge.so
	PHP_NEW_EXTENSION(libnatcJavaBridge,server/natcJavaBridge.c ,shared)
	PHP_ADD_BUILD_DIR($ext_builddir/server)
fi

