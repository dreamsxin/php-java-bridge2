<?php
function x1() {
  echo("x1 called\n");
  return true;
}
$here=realpath(dirname($_SERVER["SCRIPT_FILENAME"]));
if(!$here) $here=getcwd();

$closure=java_closure(null, "x1", java("Callback"));
$callbackTest=new java('Callback$Test', $closure);

if($callbackTest->test()) {
  echo "test okay\n";
  exit(0);
} else {
  echo "test failed\n";
  exit(1);
}
?>
