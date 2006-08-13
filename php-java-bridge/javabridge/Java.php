<?php /*-*- mode: php; tab-width:4 -*-*/

/* javabridge_Java.php -- provides backward compatibility.

   Copyright (C) 2006 Jost Boekemeier

This file is part of the PHP/Java Bridge.

This file ("the library") is free software; you can redistribute it
and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 2, or (at
your option) any later version.

The library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with the PHP/Java Bridge; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this file statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

require_once("javabridge/JavaProxy.php");

class Java extends javabridge_JavaProxy {}
class JavaClass extends javabridge_JavaProxyClass {}
class JavaException { function JavaException() {die("not implemented");} }

function java_closure() {return javabridge_closure(func_get_args()); }
function java_begin_document() { javabridge_begin_document(); }
function java_end_document() { javabridge_end_document(); }
function java_values($arg) { return javabridge_values($arg); }
function java_require($arg) { return javabridge_require($arg); }
function java_inspect($arg) { return javabridge_inspect($arg); }
function java_set_file_encoding($e) {return javabridge_set_file_encoding($e);}
function java_instanceof($o, $c) {return javabridge_instanceof($o, $c); }
function java_session() {return javabridge_session(func_get_args()); }
function java_context() {return javabridge_context(); }
function java_server_name() { return javabridge_servername(); }
function java_get_server_name() { return javabridge_servername(); }

//TODO:
function java_last_exception_get() { return null; }
function java_last_exception_clear() {}
?>
