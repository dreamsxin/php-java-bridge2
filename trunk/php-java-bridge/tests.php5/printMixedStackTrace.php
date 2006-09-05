#!/usr/bin/php

<?php

if (!extension_loaded('java')) {
  if (!(include_once("java/Java.php"))&&!(PHP_SHLIB_SUFFIX=="so" && dl('java.so'))&&!(PHP_SHLIB_SUFFIX=="dll" && dl('php_java.dll'))) {
    echo "java extension not installed.";
    exit(2);
  }
}

try {
  try {
    new java("java.lang.String", null);
  } catch(JavaException $ex) {
    if(!($ex instanceof java_exception)) {
      echo "TEST FAILED: The exception is not a java exception!\n";
      return 2;
    }
  }

  try {
    new java("java.lang.String", null);
  } catch(java_exception $ex) {
    // print the stack trace to $trace
    echo "Exception occured: {$ex->__toString()} \n";
    return 0;
  }
} catch (exception $err) {
  print "An error occured: $err\n";
  return 1;
}