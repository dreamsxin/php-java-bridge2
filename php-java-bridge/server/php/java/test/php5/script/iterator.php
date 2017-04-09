<?php

$hash = java("HashSetFactory")->getSet();
$hash->add(1);
$hash->add(3);

foreach($hash as $key=>$val) {
  echo "$key=>$val\n";
}

?>
