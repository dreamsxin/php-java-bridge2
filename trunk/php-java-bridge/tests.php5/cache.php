#!/usr/bin/php

<?php
if (!extension_loaded('java')) {
  if (!(PHP_SHLIB_SUFFIX=="so" && dl('java.so'))&&!(PHP_SHLIB_SUFFIX=="dll" && dl('php_java.dll'))) {
    echo "java extension not installed.";
    exit(2);
  }
}

$here=getcwd();
java_set_library_path("$here/cache.jar");
$Cache = new JavaClass("Cache");
$instance= $Cache->getInstance();
echo $instance->hashCode();
?>

