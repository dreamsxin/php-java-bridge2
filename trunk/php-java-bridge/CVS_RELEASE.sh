#!/bin/sh
set -x
LANG=C

echo "ssh -t jost_boekemeier,php-java-bridge@shell.sourceforge.net create -- port?"
read port

echo "host (example: shell4):"
read host

version=`cat VERSION`

rm -rf .??* *~
( cd ..; tar czf php-java-bridge_${version}.tar.gz php-java-bridge )
find . -print0 | xargs -0 touch
dirs=`ls -l | grep '^d' | fgrep -v CVS | awk '{print $9}'`
find $dirs -name "CVS" -print | xargs rm -rf

cp WebContent/test.php .
ln -s `pwd` php-java-bridge-${version}

# create archive
tar czhf php-java-bridge_${version}.tar.gz --exclude "php-java-bridge-${version}/php-java-bridge[_-]*" --exclude CVS --exclude ".??*" php-java-bridge-${version}

rpmbuild -tb php-java-bridge_${version}.tar.gz

ant clean
ant PhpDoc
ant 
ant SrcZip
ant JavaBridgeTemplateWar

cp dist/*.war dist/src.zip .
cp -r php_java_lib tests.php5 tests.jsr223 server

cp  src.zip README FAQ.html PROTOCOL.TXT INSTALL.STANDALONE INSTALL.J2EE INSTALL.J2SE NEWS documentation
mv examples documentation
mv server documentation
cp COPYING documentation/GPL
list="documentation/examples documentation/README documentation/API documentation/FAQ.html documentation/PROTOCOL.TXT documentation/INSTALL.J2EE documentation/INSTALL.J2SE documentation/INSTALL.STANDALONE documentation/src.zip documentation/NEWS JavaBridge.war documentation/server/php_java_lib documentation/server/php/java/test"
find $list -type d -name "CVS" -print | xargs rm -rf


chmod +x JavaBridge.war

# create j2ee download
zip -q -r php-java-bridge_${version}_documentation.zip $list
mv JavaBridgeTemplate.war "JavaBridgeTemplate`echo ${version}|sed 's/\.//g'`.war"

ssh -p $port "jost_boekemeier@${host}.sourceforge.net" mkdir -p "/home/frs/project/php-java-bridge/Binary\ package/php-java-bridge_`cat VERSION`/exploded/ /home/frs/project/php-java-bridge/src/php-java-bridge_`cat VERSION`/"

scp -P $port "php-java-bridge_`cat VERSION`_documentation.zip" "JavaBridgeTemplate`echo ${version}|sed 's/\.//g'`.war" "jost_boekemeier@${host}.sourceforge.net:/home/frs/project/php-java-bridge/Binary\ package/php-java-bridge_`cat VERSION`/"

scp -P $port  dist/Java.inc dist/JavaBridge.jar dist/php-servlet.jar "jost_boekemeier@${host}.sourceforge.net:/home/frs/project/php-java-bridge/Binary\ package/php-java-bridge_`cat VERSION`/exploded/"

scp "php-java-bridge_`cat VERSION`.tar.gz" jost_boekemeier,php-java-bridge@web.sf.net:"/home/frs/project/php-java-bridge/src/php-java-bridge_`cat VERSION`/"

echo "done. type return to restore modified contents"
read a
( cd ..; rm -rf php-java-bridge; tar xzf php-java-bridge_${version}.tar.gz )
