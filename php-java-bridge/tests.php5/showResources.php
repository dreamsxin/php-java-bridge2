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
java_set_library_path("$here/showResources.jar");
$sr=new java("ShowResources");
$sr->main(array());
echo "\n\n";

$sr->main(array("showResources.jar"));
?>