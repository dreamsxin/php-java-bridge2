<?php

/**
 * The JPersistenceAdapter makes it possible to serialize java values.
 *
 * Example:
 * $v=new JPersistenceAdapter(new java("java.lang.StringBuffer", "hello"));
 * $id=serialize($v);
 * $file=fopen("file.out","w");
 * fwrite($file, $id);
 * fclose($file);
 */


class JPersistenceProxy {
  var $java;
  var $serialID;

  function __construct($java){ 
    $this->java=$java; 
    $this->serialID; 
  }
  function __sleep() {
    $buf = new java("java.io.ByteArrayOutputStream");
    $out = new java("java.io.ObjectOutputStream", $buf);
    $out->writeObject($this->java);
    $out->close();
    $this->serialID = base64_encode((string)$buf->toByteArray());
    return array("serialID");
  }
  function __wakeup() {
    $buf = new java("java.io.ByteArrayInputStream", base64_decode($this->serialID));
    $in = new java("java.io.ObjectInputStream", $buf);
    $this->java = $in->readObject();
    $in->close();
  }
  function getJava() {
    return $this->java;
  }
  function __destruct() { 
    if($this->java) return $this->java->__destruct(); 
  }
}

class JPersistenceAdapter extends JPersistenceProxy {
  function __get($arg)       { if($this->java) return $this->java->__get($arg); }
  function __put($key, $val) { if($this->java) return $this->java->__put($key, $val); }
  function __call($m, $a)    { if($this->java) return $this->java->__call($m,$a); }
  function __toString()      { if($this->java) return $this->java->__toString(); }
}

?>
