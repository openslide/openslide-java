ACLOCAL_AMFLAGS = -I m4

pkglib_LTLIBRARIES = libopenslide-jni.la
libopenslide_jni_la_SOURCES = openslide-jni.c
libopenslide_jni_la_CFLAGS = -Wall $(JNI_CFLAGS) $(OPENSLIDE_CFLAGS)
libopenslide_jni_la_CFLAGS += -Wno-pointer-to-int-cast -Wno-int-to-pointer-cast
libopenslide_jni_la_LDFLAGS = -avoid-version $(OPENSLIDE_LIBS)

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

openslide.jar: $(JAVA_SRC)
	@$(ANT) -f $(srcdir)/build.xml -Dbuilddir=$(abs_builddir)

mostlyclean-local:
	@$(ANT) -f $(srcdir)/build.xml -Dbuilddir=$(abs_builddir) clean

install-exec-hook:
	chmod -x $(DESTDIR)$(pkglibdir)/libopenslide-jni.so
	rm -f $(DESTDIR)$(pkglibdir)/libopenslide-jni.la

EXTRA_DIST = \
	CHANGELOG.txt \
	LICENSE.txt \
	TODO.txt \
	lgpl-2.1.txt \
	mingw32-cheatsheet.txt \
	$(JAVA_SRC)
