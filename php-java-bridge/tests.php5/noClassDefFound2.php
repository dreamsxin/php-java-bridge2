#!/usr/bin/php

<?php
if (!extension_loaded('java')) {
  if (!(include_once("java/Java.php"))&&!(PHP_SHLIB_SUFFIX=="so" && dl('java.so'))&&!(PHP_SHLIB_SUFFIX=="dll" && dl('php_java.dll'))) {
    echo "java extension not installed.";
    exit(2);
  }
}

$here=realpath(dirname($_SERVER["SCRIPT_FILENAME"]));
if(!$here) $here=getcwd();

// must succeed
echo "must succeed\n";
java_require("$here/noClassDefFound.jar;$here/doesNotExist.jar");
$v=new java("NoClassDefFound");
$v->call(null);

?>