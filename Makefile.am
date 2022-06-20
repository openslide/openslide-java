ACLOCAL_AMFLAGS = -I m4

AM_CFLAGS = -Wall $(JNI_CFLAGS) $(OPENSLIDE_CFLAGS)
AM_CFLAGS += -Wno-pointer-to-int-cast -Wno-int-to-pointer-cast
AM_LDFLAGS = -avoid-version -no-undefined $(OPENSLIDE_LIBS) $(JNI_LDFLAGS)

if DLL
JNI_LA = openslide-jni.la
else
JNI_LA = libopenslide-jni.la
endif

if USE_CROSS_HEADERS
CROSS_H = cross/jni.h cross/jni_md.h
endif

export JAVA_HOME
export ANT_HOME

pkglib_LTLIBRARIES = $(JNI_LA)
libopenslide_jni_la_SOURCES = openslide-jni.c $(CROSS_H)
openslide_jni_la_SOURCES = $(libopenslide_jni_la_SOURCES)
# Building without lib prefix requires -module.
# On the other hand, the JNI ABI on Mac OS X precludes it.
openslide_jni_la_LDFLAGS = -module $(AM_LDFLAGS)

# Automake insists that DATA not go in pkglibdir
pkglibdatadir = $(pkglibdir)
pkglibdata_DATA = openslide.jar
JAVA_SRC = build.xml \
	src/org/openslide/AssociatedImage.java \
	src/org/openslide/OpenSlideDisposedException.java \
	src/org/openslide/OpenSlide.java \
	src/org/openslide/OpenSlideJNI.java \
	src/org/openslide/TestCLI.java \
	src/org/openslide/gui/Annotation.java \
	src/org/openslide/gui/DefaultAnnotation.java \
	src/org/openslide/gui/DefaultSelectionListModel.java \
	src/org/openslide/gui/Demo.java \
	src/org/openslide/gui/OpenSlideView.java \
	src/org/openslide/gui/SelectionListModel.java

openslide.jar: $(JAVA_SRC) config.h
	@"$(ANT)" -f "$(srcdir)/build.xml" -Dbuilddir="$(ANT_BUILDDIR)" \
		-Dopenslide.binding.version=$(PACKAGE_VERSION) \
		-Dopenslide.jni.path="$(JNI_EMBED_PATH)"

mostlyclean-local:
	@"$(ANT)" -f "$(srcdir)/build.xml" -Dbuilddir="$(ANT_BUILDDIR)" clean

install-exec-hook:
if RENAME_JNI
	source ./$(JNI_LA) && mv "$(DESTDIR)$(pkglibdir)/$$dlname" \
		"$(DESTDIR)$(pkglibdir)/$(JNI_SO)"
endif
	rm -f "$(DESTDIR)$(pkglibdir)/$(JNI_LA)" \
		"$(DESTDIR)$(pkglibdir)/$(JNI_SO).a"

uninstall-hook:
	rm -f "$(DESTDIR)$(pkglibdir)/$(JNI_SO)"

EXTRA_DIST = \
	CHANGELOG.md \
	README.md \
	TODO.txt \
	$(JAVA_SRC)
