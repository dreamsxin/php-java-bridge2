<?php require_once("mono/Mono.inc");

phpinfo();
print "\n\n";

$v = new Mono("java.lang.System");
$arr=$v->getProperties();

foreach ($arr as $key => $value) {
  print $key . " -> " .  $value . "<br>\n";
}
?>

