#!/usr/bin/php

<?php
if(!extension_loaded('java')) {
  dl('java.' . PHP_SHLIB_SUFFIX);
}

$here=trim(`pwd`);
java_set_library_path("$here/arrayArray.jar");
$ReflectArray = new java_class("java.lang.reflect.Array");
$Array = new java_class("ArrayArray");
$arrayArray=$Array->create(10);

$String=new java_class("java.lang.String");
for($i=0; $i<10; $i++) {
	$ar = $arrayArray[$i]->array;
	echo $ar . " " .$ar[0] . "\n"; 
}

echo "\n";

foreach($arrayArray as $value) {
	$ar = $value->array;
	echo $ar . " " .$ar[0] ."\n";
}


?>