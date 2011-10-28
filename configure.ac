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
LT_INIT([win32-dll disable-static])

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
if test $host = $build; then
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
else
    # For cross builds, we don't have the OpenJDK jni_md.h for the target
    # platform, so use the GNU Classpath JNI headers in the build system's
    # gcj include directory instead.  Autoconf doesn't give us a variable
    # for the build compiler, which is okay because anything other than
    # GCC probably won't support this syntax.
    #
    # This is terrible.
    JNI_CFLAGS=
    AC_MSG_CHECKING([for cross jni.h and jni_md.h])
    for word in $(gcc - -v -E < /dev/null 2>&1)
    do
        if test -r "$word/jni.h" -a -r "$word/jni_md.h" ; then
            AC_MSG_RESULT([$word])
            JNI_CFLAGS="-I$word"
            break
        fi
    done
    if test x$JNI_CFLAGS = x ; then
        AC_MSG_RESULT([not found])
        AC_MSG_ERROR([cannot find JNI headers])
    fi
fi
AC_SUBST([JNI_CFLAGS])

# Checks for typedefs, structures, and compiler characteristics.

AC_TYPE_INT64_T
AC_TYPE_UINT32_T

AC_MSG_CHECKING([JNI library name])
if test x$shrext_cmds = x.dll ; then
    AC_MSG_RESULT([openslide-jni.dll])
else
    AC_MSG_RESULT([libopenslide-jni.so])
fi
AM_CONDITIONAL([DLL], [test x$shrext_cmds = x.dll])

# Checks for library functions.

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
