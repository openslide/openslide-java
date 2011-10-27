#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ([2.66])
AC_INIT([OpenSlide Java], [0.9.2], [openslide-users@lists.andrew.cmu.edu])
AC_CONFIG_AUX_DIR([build-aux])
AC_CONFIG_MACRO_DIR([m4])
AC_CONFIG_SRCDIR([openslide-jni.c])
AC_CONFIG_HEADERS([config.h])
AM_INIT_AUTOMAKE([foreign dist-xz])
AM_SILENT_RULES([yes])
LT_INIT([disable-static])

# Checks for programs.

PKG_PROG_PKG_CONFIG()
AC_PROG_CC

AC_PATH_PROG([ANT], [ant])
AC_ARG_VAR([ANT], [path to ant])
if test z$ANT = z ; then
    AC_MSG_ERROR([ant not found])
fi

# Checks for libraries.

PKG_CHECK_MODULES([OPENSLIDE], [openslide])

# Checks for header files.

AC_ARG_VAR([JAVA_HOME], [path to JDK])
AC_ARG_WITH([java], [AS_HELP_STRING([--with-java], [path to JDK])], [],
        [with_java="$JAVA_HOME /usr/lib/jvm/java"])
# Find jni.h
JOIN_EACH([include_paths], [$with_java], [include])
FIND_FILE([jni_h_dir], [jni.h], [$include_paths])
# Find jni_md.h
AS_CASE([$host],
    [*-*-linux-gnu], [include_subdir=linux],
    [*-*-mingw32], [include_subdir=win32],
    [include_subdir=]
)
JOIN_EACH([platform_include_paths], [$include_paths], [$include_subdir])
FIND_FILE([jni_md_h_dir], [jni_md.h],
        [$platform_include_paths $include_paths])
JNI_CFLAGS="-I$jni_h_dir -I$jni_md_h_dir"
AC_SUBST([JNI_CFLAGS])

# Checks for typedefs, structures, and compiler characteristics.

AC_TYPE_INT64_T
AC_TYPE_UINT32_T

# Checks for library functions.

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
