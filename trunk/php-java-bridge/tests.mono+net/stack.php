#!/usr/bin/php

<?php

if(!extension_loaded('mono')) {
  dl('mono.' . PHP_SHLIB_SUFFIX);
}

$s = new Mono('System.Collections.Stack');
$s->Push('Hello World');
$s->Push('Mono');

if ($s->Contains('Mono')) {
	print "Works like a perfect darling\n";
}

print $s->Pop() . ' ' . $s->Pop() . "\n";
?>