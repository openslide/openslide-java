#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.63])
AC_INIT([OpenSlide Java], [0.12.4], [openslide-users@lists.andrew.cmu.edu])
AC_CONFIG_AUX_DIR([build-aux])
AC_CONFIG_MACRO_DIR([m4])
AC_CONFIG_SRCDIR([openslide-jni.c])
AC_CONFIG_HEADERS([config.h])
AM_INIT_AUTOMAKE([1.11 foreign dist-xz])
AM_SILENT_RULES([yes])
LT_INIT([win32-dll disable-static])

# Checks for programs.

PKG_PROG_PKG_CONFIG()
AC_PROG_CC

# For distribution-packaged Ant, $ANT may not be $ANT_HOME/bin/ant.
# Use and pass through ANT_HOME if we receive it; otherwise, focus on ANT.
AC_ARG_VAR([ANT], [path to ant program])
AC_ARG_VAR([ANT_HOME], [path to ant installation directory])
if test "z$ANT" = z && test "z$ANT_HOME" != z ; then
    ANT="$ANT_HOME/bin/ant"
fi
AC_PATH_PROG([ANT], [ant])
if test "z$ANT" = z ; then
    AC_MSG_ERROR([ant not found])
fi

# On Cygwin the JVM uses Windows paths.  The ant wrapper script converts
# almost everything for us, but not the argument to -Dbuilddir.
AC_MSG_CHECKING([Ant build path])
AS_CASE([$build],
    [*-*-cygwin], [ANT_BUILDDIR="$(cygpath -w "$(pwd)")"],
    [ANT_BUILDDIR="$(pwd)"]
)
AC_SUBST([ANT_BUILDDIR])
AC_MSG_RESULT([$ANT_BUILDDIR])

# Checks for libraries.

PKG_CHECK_MODULES([OPENSLIDE], [openslide >= 3.4.0])

# Checks for header files.

AC_ARG_VAR([JAVA_HOME], [path to JDK])
# Dynamic default for Mac OS X
if test -x /usr/libexec/java_home ; then
    DEFAULT_JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null)
fi
AC_ARG_WITH([java], [AS_HELP_STRING([--with-java], [path to JDK])], [],
        [with_java="$JAVA_HOME:$DEFAULT_JAVA_HOME:/usr/lib/jvm/java"])
AC_ARG_WITH([jni-headers], [AS_HELP_STRING([--with-jni-headers=PLATFORM],
        [use JNI headers for PLATFORM.  Specify "cross" to use bundled
        Classpath headers.])], [], [with_jni_headers=guess])
if test z$with_jni_headers = zguess ; then
    if test $host = $build ; then
        AS_CASE([$host],
            [*-*-linux-gnu], [with_jni_headers=linux],
            [*-*-mingw32], [with_jni_headers=win32],
            [*-*-darwin*], [with_jni_headers=darwin],
            [with_jni_headers=]
        )
    else
        with_jni_headers=cross
        # If building with MinGW inside Cygwin, the native headers are
        # available, so use them.
        AS_CASE([$build], [*-*-cygwin],
            AS_CASE([$host], [*-*-mingw32], [with_jni_headers=win32]))
    fi
fi
if test z$with_jni_headers != zcross ; then
    # Find jni.h
    JOIN_EACH([include_paths], [$with_java], [include])
    FIND_FILE([jni_h_dir], [jni.h], [$include_paths])
    JAVA_HOME=$(dirname "$jni_h_dir")
    # Find jni_md.h
    JOIN_EACH([platform_include_paths], [$include_paths], [$with_jni_headers])
    FIND_FILE([jni_md_h_dir], [jni_md.h],
            [$platform_include_paths:$include_paths])
    JNI_CFLAGS="\"-I$jni_h_dir\" \"-I$jni_md_h_dir\""
else
    # For cross builds, we don't have the OpenJDK jni_md.h for the target
    # platform, so use our own copy of the GNU Classpath JNI headers instead.
    #
    # Don't set JAVA_HOME unless it was specified on the command line.
    AC_MSG_CHECKING([whether to use bundled JNI headers])
    AC_MSG_RESULT([yes])
    JNI_CFLAGS='-I$(top_srcdir)/cross'
fi
AC_SUBST([JNI_CFLAGS])
AM_CONDITIONAL([USE_CROSS_HEADERS], [test x$with_jni_headers = xcross])

# Checks for typedefs, structures, and compiler characteristics.

AC_TYPE_INT64_T
AC_TYPE_UINT32_T

# Get the filename extension for dynamic libraries.
# $module matters on Mac OS X.
module=no
eval dynlib_ext=$shrext_cmds
if test x$dynlib_ext = x.dylib ; then
    # Mac OS X has a special file extension for JNI libraries
    dynlib_ext=.jnilib
fi
AM_CONDITIONAL([RENAME_JNI], [test x$dynlib_ext = x.jnilib])

AC_MSG_CHECKING([JNI library name])
if test x$dynlib_ext = x.dll ; then
    AC_SUBST([JNI_SO], [openslide-jni.dll])
    # JNI uses stdcall without @
    AC_SUBST([JNI_LDFLAGS], [-Wl,--kill-at])
else
    AC_SUBST([JNI_SO], [libopenslide-jni$dynlib_ext])
fi
AC_MSG_RESULT([$JNI_SO])
AM_CONDITIONAL([DLL], [test x$dynlib_ext = x.dll])

AC_MSG_CHECKING([whether to embed library path in JAR])
if test x$dynlib_ext != x.dll ; then
    AC_SUBST([JNI_EMBED_PATH], [$libdir/$PACKAGE/$JNI_SO])
    AC_MSG_RESULT([yes])
else
    AC_MSG_RESULT([no])
fi

# Checks for library functions.

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
