#!/usr/bin/php

<?php
if (!extension_loaded('java')) {
  if (!(PHP_SHLIB_SUFFIX=="so" && dl('java.so'))&&!(PHP_SHLIB_SUFFIX=="dll" && dl('php_java.dll'))) {
    echo "java extension not installed.";
    exit(2);
  }
}

if (version_compare("5.0.0", phpversion(), "<=")) {
    echo "Run the php5 callback test in tests.php5 instead.\n";
    exit(0);
 }

class c {
  function c1($s1, $o1) {
    echo "c1: $s1, $o1\n";
    // must not display a warning
    $var=new java("java.lang.String", null); if(java_last_exception_get()) return;

    // not reached 
    echo "ERROR.\n"; 
    exit(3); 
  }
  function c2($b) {
    echo "c2: $b\n";
    return $b;
  }
  function c3 ($e) {
    echo "c3: $e\n";
    return 2;
  }
}
$here=getcwd();
java_require("$here/../tests.php5/callback.jar");
$c=new c();
$closure=java_closure($c);
$callbackTest=new java('Callback$Test', $closure);

if($callbackTest->test()) {
  echo "test okay\n";
  exit(0);
} else {
  echo "test failed\n";
  exit(1);
}
?>
