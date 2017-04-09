<?php
$here=realpath(dirname($_SERVER["SCRIPT_FILENAME"]));
if(!$here) $here=getcwd();
$binaryData = new Java("BinaryData");

echo java_inspect($binaryData)

?>
