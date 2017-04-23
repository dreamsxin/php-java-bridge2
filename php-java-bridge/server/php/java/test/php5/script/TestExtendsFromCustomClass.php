use php\java\test\php5\TestExtends$TestClass as TestClass;

class PhpTestClass extends TestClass {

  function func ($s, $val) {
    $res = parent::func($s, val);
    $res->append($val+10);
    return $res;
  }
  function func2 ($s, $val) {
    $res = func3($s, val);
    $res->append($val+20);
    return $res;
  }
}

$te = new PhpTestClass();
echo $te->runTest();
exit(2);

