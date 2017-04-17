#-*- mode: rpm-spec; tab-width:4 -*-
%define version 7.1.3
%define release 1
%define PHP_MAJOR_VERSION %(((LANG=C rpm -q --queryformat "%{VERSION}" php) || echo "4.0.0") | tail -1 | sed 's/\\\..*$//')
%define PHP_MINOR_VERSION %(((LANG=C rpm -q --queryformat "%{VERSION}" php) || echo "4.0.0") | tail -1 | LANG=C cut -d. -f2)
%define PHP_RELEASE_VERSION %(((LANG=C rpm -q --queryformat "%{VERSION}" php) || echo "4.0.0") | tail -1 | LANG=C cut -d. -f3)
%define have_j2 %((rpm -q --whatprovides j2sdk) >/dev/null && echo 1 || echo 0)
%define have_j3 %((rpm -q --whatprovides jdk) >/dev/null && echo 1 || echo 0)
%define have_policy_modules %(if test -f /etc/selinux/config && ( test -d /etc/selinux/%{__policy_tree}/modules || test 2 -lt `echo %{_selinux_policy_version} | awk -F. '{print $1}'` ); then echo 1; else echo 0; fi)
%define have_policy_devel %(if test -f %{_datadir}/selinux/devel/Makefile; then echo 1; else echo 0; fi)

%define tomcat_name			tomcat
%define tomcat_webapps		%{_localstatedir}/lib/%{tomcat_name}/webapps
%define shared_java			%{_datadir}/java
%define shared_pear			%{_datadir}/pear
%global debug_package %{nil}

Name: php-java-bridge
Summary: PHP Hypertext Preprocessor to Java Bridge
Group: Development/Languages
Version: %{version}
Release: %{release}
License: LGPL
URL: http://www.sourceforge.net/projects/php-java-bridge
Source0: https://downloads.sourceforge.net/project/php-java-bridge/src/php-java-bridge_%{version}/php-java-bridge_%{version}.tar.gz

BuildRequires: httpd
BuildRequires: phpdoc >= 1.4.2
BuildRequires: ant >= 1.7.1
BuildRequires: ed

%if %{have_j3} == 1
BuildRequires: jdk >= 1.4.2
%else
%if %{have_j2} == 1
BuildRequires: j2sdk >= 1.4.2
%else
BuildRequires: java-devel >= 1.4.2
%endif
%endif
%if %{have_policy_modules} == 1
BuildRequires: selinux-policy
BuildRequires: policycoreutils checkpolicy coreutils
%if %{have_policy_devel} == 0
BuildRequires: selinux-policy-devel
%endif
%endif

# PHP 4 or PHP 5 or PHP 5.1
%if %{PHP_MAJOR_VERSION} == 4
Requires: php >= 4.3.2
Requires: php < 5.0.0
%else
%if %{PHP_MAJOR_VERSION} == 5
Requires: php >= 5.1.1
Requires: php < 6.0.0
%if (%{PHP_MINOR_VERSION} == 2 && %{PHP_RELEASE_VERSION} > 6) || (%{PHP_MINOR_VERSION} > 2)
%endif
%else
Requires: php >= 5.2.0
%endif
%endif
Requires: httpd 
Requires: %{tomcat_name}
%if %{have_policy_modules} == 1
Requires: policycoreutils coreutils
%endif


BuildRoot: %{_tmppath}/%{name}-root

%description 
Java module/extension for the PHP script language.  Contains the basic
files: java extension for PHP/Apache HTTP server and a simple back-end
which automatically starts and stops when the HTTP server
starts/stops. The bridge log appears in the http server error log.


%package devel
Group: Development/Libraries
Summary: PHP/Java Bridge development files and documentation
Requires: php-java-bridge = %{version}
%description devel
Contains the development documentation
and the development files needed to create java applications with
embedded PHP scripts.


%prep
echo Building for PHP %{PHP_MAJOR_VERSION}.

%setup -q

%build
set -x
PATH=/bin:%{_bindir}
LD_LIBRARY_PATH=/lib:%{_libdir}

# calculate java dir
%if %{have_j3} == 1
pkgid=`rpm -q --whatprovides jdk --queryformat "%{PKGID} %{VERSION}\n" | sed 's/\./0/g;s/_/./' |sort -r -k 2,2 -n | head -1 | awk '{print $1}'`
%else
%if %{have_j2} == 1
pkgid=`rpm -q --whatprovides j2sdk --queryformat "%{PKGID} %{VERSION}\n" | sed 's/\./0/g;s/_/./' |sort -r -k 2,2 -n | head -1 | awk '{print $1}'`
%else
pkgid=`rpm -q --whatprovides java-devel --queryformat "%{PKGID} %{VERSION}\n" | sed 's/\./0/g;s/_/./' |sort -r -k 2,2 -n | head -1 | awk '{print $1}'`
%endif
%endif
jdk=`rpm  -q --pkgid $pkgid`
java=`rpm -ql $jdk | grep 'bin/java$' | head -1`
java_dir=`dirname $java`
java_dir=`dirname $java_dir`
if test X`basename $java_dir` = Xjre; then
  java_dir=`dirname $java_dir`;
fi
echo "using java_dir: $java_dir"
if test X$java_dir = X; then echo "ERROR: java not installed" >2; exit 1; fi

ant clean &&
ant PhpDoc 2>/dev/null >/dev/null && 
ant && 
ant SrcZip

%if %{have_policy_modules} == 1
(cd security/module; make -f %{_datadir}/selinux/devel/Makefile; rm -rf tmp;)
%endif

%install
#rm -rf $RPM_BUILD_ROOT
echo >filelist
echo >filelist-devel

mod_dir=`pwd`/dist
mkdir -p $RPM_BUILD_ROOT/%{shared_java}
cp $mod_dir/JavaBridge.jar $RPM_BUILD_ROOT/%{shared_java}/JavaBridge-%{version}.jar; 
(cd $RPM_BUILD_ROOT/%{shared_java}; ln -fs JavaBridge-%{version}.jar JavaBridge.jar); 
echo %{shared_java}/JavaBridge.jar >>filelist-devel

files="PHPDebugger.php Client.inc GlobalRef.inc Java.inc JavaBridge.inc JavaProxy.inc NativeParser.inc Options.inc Parser.inc Protocol.inc SimpleParser.inc"
mkdir -p $RPM_BUILD_ROOT/%{shared_pear}/java
for i in $files; 
  do cp WebContent/META-INF/java/$i $RPM_BUILD_ROOT/%{shared_pear}/java/$i; 
  echo %{shared_pear}/java/$i >>filelist
done

files=JavaBridge.war
mkdir -p $RPM_BUILD_ROOT/%{tomcat_webapps}
for i in $files; 
  do cp $mod_dir/$i $RPM_BUILD_ROOT/%{tomcat_webapps}
  rm -f $mod_dir/$i; 
done

# server also contains the server documentation
mv server server.backup
mkdir server
(cd server.backup; find php -name "*.java" -print | cpio -dp ../server)
cp dist/src.zip server

%clean
rm -rf $RPM_BUILD_ROOT

%post
if test -f /etc/selinux/config; then
  if test %{have_policy_modules} -eq 1; then 
	/sbin/service httpd stop > /dev/null 2>&1
	/sbin/service %{tomcat_name} stop > /dev/null 2>&1
	%{_sbindir}/semodule -i %{_docdir}/%{name}/security/module/php-java-bridge.pp
	%{_sbindir}/semodule -i %{_docdir}/%{name}/security/module/php-java-bridge-tomcat.pp
	/sbin/service httpd start > /dev/null 2>&1
	/sbin/service %{tomcat_name} start > /dev/null 2>&1
  else
	te=/etc/selinux/%{__policy_tree}/src/policy/domains/program/php-java-bridge.te
	fc=/etc/selinux/%{__policy_tree}/src/policy/file_contexts/program/php-java-bridge.fc
	echo "SECURITY ENHANCED LINUX"
	echo "-----------------------"
	echo "You are running a SELinx system. Please install the policy sources:"
	echo "rpm -i selinux-policy-%{__policy_tree}-sources-*.rpm"
	echo "sh %{_docdir}/%{name}-%{version}/security/update_policy.sh \\"
	echo "					/etc/selinux/%{__policy_tree}/src/policy"
	echo "Please see README document for more information."
	echo
	echo "Or type: setenforce 0  to disable SEL temporarily."
	echo
  fi
fi
if test -d /var/www/html &&  ! test -e /var/www/html/JavaBridge; then
  cp -r %{tomcat_webapps}/JavaBridge /var/www/html/;
  ed /var/www/html/JavaBridge/java/Java.inc <<\EOF
1,11s/# define ("JAVA_HOSTS", "127.0.0.1:8787");/define ("JAVA_HOSTS", "127.0.0.1:8080");/
w
q
EOF
fi
echo "PHP/Java Bridge installed. Start with:"
echo "service %{tomcat_name} restart"
echo "service httpd restart"
echo "Then visit: http://localhost/JavaBridge or http://localhost:8080/JavaBridge"
echo
exit 0

%post devel
exit 0

%preun
if [ $1 = 0 ]; then
	/sbin/service httpd stop > /dev/null 2>&1
	if test %{have_policy_modules} -eq 1; then 
		%{_sbindir}/semodule -r php-java-bridge
		%{_sbindir}/semodule -r php-java-bridge-tomcat
	fi
	if test -e /var/www/html/JavaBridge && test -e %{tomcat_webapps}/JavaBridge && test %{tomcat_webapps}/JavaBridge -ef /var/www/html/JavaBridge; then
		rm -f /var/www/html/JavaBridge;
	fi
	rm -rf %{tomcat_webapps}/JavaBridge %{tomcat_webapps}/work/*
	/sbin/service httpd start > /dev/null 2>&1
fi

%preun devel
if [ $1 = 0 ]; then
	/bin/true
fi


%files -f filelist
%defattr(-,root,root)
%attr(-,tomcat,tomcat) %{tomcat_webapps}/JavaBridge.war
%doc README FAQ.html COPYING CREDITS NEWS test.php INSTALL.J2EE security 

%files devel -f filelist-devel
%defattr(-,root,root)
%attr(755,root,root) %{shared_java}/JavaBridge-%{version}.jar 
%doc FAQ.html CREDITS ChangeLog README PROTOCOL.TXT COPYING server documentation examples php_java_lib NEWS
