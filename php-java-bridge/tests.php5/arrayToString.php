#!/usr/bin/php

<?php
if(!extension_loaded('java')) {
  dl('java.' . PHP_SHLIB_SUFFIX);
}

$here=trim(`pwd`);
java_set_library_path("$here/arrayToString.jar");
$ArrayToString = new java_class("ArrayToString");


// create long array ...
$length=10;
for($i=0; $i<$length; $i++) {
  $arr[$i]=$i;
}

// ... and post it to java.  Should print integers
print "integer array: ". $ArrayToString->arrayToString($arr) . "\n";


// double
for($i=0; $i<$length; $i++) {
  $arr[$i]=$i +1.23;
}

// should print doubles
print "double array: ". $ArrayToString->arrayToString($arr) . "\n";


// boolean
for($i=0; $i<$length; $i++) {
  $arr[$i]=$i%2?true:false;
}

// should print booleans
print "boolean array: ". $ArrayToString->arrayToString($arr) ."\n";

?>