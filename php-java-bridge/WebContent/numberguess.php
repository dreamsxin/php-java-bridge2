<html>
<head/>
<body>
<?php 

var_dump($_POST);

if($_POST['guess']) {
  echo "POST!!!";
}
?>


  <form method=post>
  What's your guess? <input type=text name=guess>
  <input type=submit value="Submit">
  </form>


 

</font>
</body>
</html>
