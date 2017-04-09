<?php
include("../../../../../../php_java_lib/JspTag.php");
$session = java_session ();
$here=realpath(dirname($_SERVER["SCRIPT_FILENAME"]));
if(!$here) $here=getcwd();

$attributes = array("att1"=>"98.5", "att2"=>"92.3","att3"=>"107.7");
$pc = new java_PageContext($session);
$tag = new java_Tag($pc, "FooTag", $attributes);
if($tag->start()) {
  do {
    $pc->getPageContext()->getOut()->print("member:: ");
    $pc->getPageContext()->getOut()->println($pc->getPageContext()->getAttribute("member"));
  } while($tag->repeat());
}
$tag->end();
?>
