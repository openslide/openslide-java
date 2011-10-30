ACLOCAL_AMFLAGS = -I m4

AM_CFLAGS = -Wall $(JNI_CFLAGS) $(OPENSLIDE_CFLAGS)
AM_CFLAGS += -Wno-pointer-to-int-cast -Wno-int-to-pointer-cast
AM_LDFLAGS = -avoid-version -no-undefined -module $(OPENSLIDE_LIBS)

if DLL
# JNI uses stdcall without @
AM_LDFLAGS += -Wl,--kill-at
JNI_LA = openslide-jni.la
JNI_SO = openslide-jni.dll
else
JNI_LA = libopenslide-jni.la
JNI_SO = libopenslide-jni.so
endif

if EMBED_PATH
JNI_EMBED_PATH = $(pkglibdir)/$(JNI_SO)
endif

export JAVA_HOME
export ANT_HOME

pkglib_LTLIBRARIES = $(JNI_LA)
libopenslide_jni_la_SOURCES = openslide-jni.c

pkglib_DATA = openslide.jar
JAVA_SRC = build.xml \
	src/edu/cmu/cs/openslide/AssociatedImageMap.java \
	src/edu/cmu/cs/openslide/OpenSlideDisposedException.java \
	src/edu/cmu/cs/openslide/OpenSlide.java \
	src/edu/cmu/cs/openslide/OpenSlideJNI.java \
	src/edu/cmu/cs/openslide/TestCLI.java \
	src/edu/cmu/cs/openslide/gui/Annotation.java \
	src/edu/cmu/cs/openslide/gui/DefaultAnnotation.java \
	src/edu/cmu/cs/openslide/gui/DefaultSelectionListModel.java \
	src/edu/cmu/cs/openslide/gui/Demo.java \
	src/edu/cmu/cs/openslide/gui/OpenSlideView.java \
	src/edu/cmu/cs/openslide/gui/SelectionListModel.java \
	src/org/openslide/dzi/DeepZoomGenerator.java

openslide.jar: $(JAVA_SRC) config.h
	@$(ANT) -f $(srcdir)/build.xml -Dbuilddir=$(abs_builddir) \
		-Dopenslide.jni.path=$(JNI_EMBED_PATH)

mostlyclean-local:
	@$(ANT) -f $(srcdir)/build.xml -Dbuilddir=$(abs_builddir) clean

install-exec-hook:
	chmod -x $(DESTDIR)$(pkglibdir)/$(JNI_SO)
	rm -f $(DESTDIR)$(pkglibdir)/$(JNI_LA) \
		$(DESTDIR)$(pkglibdir)/$(JNI_SO).a

EXTRA_DIST = \
	CHANGELOG.txt \
	LICENSE.txt \
	README.txt \
	TODO.txt \
	lgpl-2.1.txt \
	$(JAVA_SRC)
