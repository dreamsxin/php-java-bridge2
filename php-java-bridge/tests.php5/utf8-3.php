#!/usr/bin/php

<?php
if (!extension_loaded('java')) {
  if (!(include_once("java/Java.php"))&&!(PHP_SHLIB_SUFFIX=="so" && dl('java.so'))&&!(PHP_SHLIB_SUFFIX=="dll" && dl('php_java.dll'))) {
    echo "java extension not installed.";
    exit(2);
  }
}

// Start server with:
// java -Dfile.encoding=ASCII -jar JavaBridge.jar INET:0 4 ""

// test UTF8 and ISO-8859-1 input and convert output to UTF-8.
// WARNING: Open and save this file with a HEX editor only!

java_set_file_encoding("ASCII");
$string = new Java("java.lang.String", "Cześć! -- שלום -- Grüß Gott -- Dobrý deň -- Dobrý den -- こんにちは, ｺﾝﾆﾁﾊ", "UTF-8");
print java_values($string->getBytes("UTF-8")) . "\n";

$string = new Java("java.lang.String", "Gr�� Gott", "ISO-8859-1");
print java_values($string->getBytes("UTF-8")) . "\n";

?>