<?php
$s = <<<EOF
<?php
/* wrapper for Java.inc and PHPDebugger.inc */

\$java_include_only=str_pad(javaproxy_getHeader("X_JAVABRIDGE_INCLUDE_ONLY", \$_SERVER), 2, "0", false);
if(\$java_include_only[1]=='1') { // include PHPDebugger.php
	require_once("PHPDebugger.php");
}
if(\$java_include_only[0]=='1') { // include Java.inc
	require_once("Java.inc");
}

if (\$java_script_orig = \$java_script = javaproxy_getHeader("X_JAVABRIDGE_INCLUDE", \$_SERVER)) {

	if (\$java_script!="@") {
		if ((\$_SERVER['REMOTE_ADDR']=='127.0.0.1') || ((\$java_script = realpath(\$java_script)) && (!strncmp(\$_SERVER['DOCUMENT_ROOT'], \$java_script, strlen(\$_SERVER['DOCUMENT_ROOT']))))) {
			chdir (dirname (\$java_script));
			if (     (\$java_include_only[0]=='1') || 
					((\$java_include_only[1]=='1') && !(isset(\$_SERVER["SCRIPT_FILENAME"]) && isset(\$_SERVER["QUERY_STRING"])&&!extension_loaded("Zend Debugger")))) {
					// if Java.inc is enabled or if PHPDebugger.php is enabled but not requested, require original script
					// otherwise the debugger will load the file, if necessary.
					require_once(\$java_script);
			}
		} else {
			trigger_error("illegal access: ".\$java_script_orig, E_USER_ERROR);
		}
	}

	if (\$java_include_only[0]=='1') { // Java.inc
		java_call_with_continuation();
	}
	if (\$java_include_only[1]=='1') { // PHPDebugger
		\$pdb_dbg->handleRequests();
	}
}
function javaproxy_getHeader(\$name,\$array) {
	if (array_key_exists(\$name,\$array)) return \$array[\$name];
	\$name="HTTP_\$name";
	if (array_key_exists(\$name,\$array)) return \$array[\$name];
	return null;
}

?>
EOF;

file_put_contents($argv[1], $s);
?>
