use java\lang\StringBuilder as JString;
use java\util\ArrayList as JList;

class PString extends JString {
  function toString () {
    return "hello " . parent::toString();
  }
}
$str = new PString("Java");

$list = new JList();
$list->add (java_closure($str));
$list->add ("from PHP");
$ar = java_values ($list->toArray());

foreach($ar as $entry) echo "$entry<br>\n";
