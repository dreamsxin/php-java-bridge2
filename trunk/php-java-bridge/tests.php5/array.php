#!/usr/bin/php

<?php
if(!extension_loaded('java')) {
  dl('java.' . PHP_SHLIB_SUFFIX);
}

$here=trim(`pwd`);
java_set_library_path("$here/array.jar");
$array = new java("Array");

$map = $array->getConversion();
$set = $map->entrySet();
$iterator = $set->iterator();
while ($iterator->hasNext()) {
  $next = $iterator->next();
  $key = $next->getKey();
  $value = $next->getValue();
  echo "$key => $value\n";
}

echo "\n";

$idx = $array->getIndex("Seelandschaft mit Pocahontas, Arno Schmidt, 1914--1979");
$entry = $array->getEntry($idx);
echo "$idx => $entry\n";
?>