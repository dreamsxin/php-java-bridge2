# -*- mode: Makefile; -*-


$(phplibdir)/libnatcJavaBridge.la:
	cd $(srcdir)/server; sh autogen.sh && sh ./configure --with-java="$(PHP_JAVA_HOME)" --with-extdir="$(EXTENSION_DIR)" --libdir="$(phplibdir)" --datadir="$(phplibdir)" --bindir="$(phplibdir)" && make CFLAGS="$(CFLAGS_CLEAN)" GCJFLAGS="$(GCJFLAGS) $(CFLAGS_CLEAN)" install
