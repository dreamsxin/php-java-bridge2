<?php

$ListClass=new java_class("java.util.ArrayList");
$list = new java("java.util.ArrayList");
$list->add(0);
$list->add("one");
$list->add(null);
$list->add(new java("java.lang.Object"));
$list->add(new java("java.lang.Long", 4));
$list->add($list); // the list now contains itself
$list->add(new java("java.lang.String", "last entry"));

foreach ($list as $key=>$value) {               
  echo "$key => ";
  if($value instanceof java) {
    if(java_instanceof($value, $ListClass)) {
      echo "[I have found myself!] ";
    } else {
      echo "[found java object: " .$value->toString() . "] ";
    }
  }
  echo "$value\n";
}
?>