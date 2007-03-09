#!/usr/bin/php -nq

# To run the following example gtk-sharp ver. 2.0.0.0, (key# 35e10195dab3c99f)
# must be installed.

<?php
if (!extension_loaded('mono')) {
  if (!(PHP_SHLIB_SUFFIX=="so" && dl('mono.so'))&&!(PHP_SHLIB_SUFFIX=="dll" && dl('php_mono.dll'))) {
    echo "mono extension not installed.";
    exit(2);
  }
}
ini_set("max_execution_time", 0);

class GtkFileSelectorDemo {

  var $filew;

  function GtkFileSelectorDemo () {
    // mono_require("gtk-sharp", "2.0.0.0", "35e10195dab3c99f"); 

    // The following is equivalent to the above mono_require
    // statement. It shows how to load a library from the GAC.
    $Assembly=new MonoClass("System.Reflection.Assembly");
    $assemblyName = new Mono("System.Reflection.AssemblyName");

    // Name is a property of AssemblyName, set_Name(...) calls the
    // setter, get_Name() calls the getter
    $assemblyName->set_Name("gtk-sharp");
    $assemblyName->set_Version(new Mono("System.Version", "2.0.0.0"));

    // pack converts the hex string into a byte array
    $assemblyName->setPublicKeyToken(pack("H16", "35e10195dab3c99f"));
    // load gtk-sharp 2.0.0.0 (35e10195dab3c99f)
    $Assembly->Load($assemblyName);
  }

  function ok($obj, $args) {
    echo "ok called\n";
    echo $this->filew->get_Filename() . "\n";
  }

  function quit($obj, $args) {
    echo "quit called\n";
    $this->Application->Quit();
  }

  function init() {
    $Application = $this->Application = new MonoClass("Gtk.Application");
    $Application->Init();

    $filew = $this->filew = new Mono("Gtk.FileSelection", "Open a file ...");
    $filew->add_DeleteEvent (new Mono("Gtk.DeleteEventHandler", mono_closure($this, "quit")));
    $b=$filew->get_OkButton();
    $b->add_Clicked (new Mono("System.EventHandler", mono_closure($this, "ok")));
    $b=$filew->get_CancelButton();
    $b->add_Clicked (new Mono("System.EventHandler", mono_closure($this, "quit")));
    $filew->set_Filename ("penguin.png");
    $filew->Show();
  }

  function run() {
    $this->init();
    $this->Application->Run();
  }
}
$demo=new GtkFileSelectorDemo();
$demo->run();

?>