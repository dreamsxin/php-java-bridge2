#!/usr/bin/php

<?php

if (!extension_loaded('java')) {
  if (!(include_once("java/Java.php"))&&!(PHP_SHLIB_SUFFIX=="so" && dl('java.so'))&&!(PHP_SHLIB_SUFFIX=="dll" && dl('php_java.dll'))) {
    echo "java extension not installed.";
    exit(2);
  }
}

$v = new java("java.util.Vector");
$ar=(array("one"=>1, "two"=>2, "three"=>3, array("a",array(1,2,3,array("a","b"),5,"c","d"), "five"=>5, "six"=>6)));
$v->add($ar);

$result=(array_diff_assoc(java_values($v),$ar));
if(sizeof($result)==0) {
  echo "test okay\n";
  exit(0);
} else {
  echo "ERROR\n";
  exit(1);
}
?>
