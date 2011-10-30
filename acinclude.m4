# FIND_FILE([OUTPUT], [FILE_NAME], [SEARCH_PATHS])
# The paths in SEARCH_PATHS are searched to determine whether they contain
# the file FILE_NAME.  If so, OUTPUT is set to the directory containing
# the file.  Otherwise, an error is produced.
# ------------------------------------------------------------------------
AC_DEFUN([FIND_FILE], [
    AC_MSG_CHECKING([for $2])
    $1=
    for ac__path in $3
    do
        if test -r $ac__path/$2 ; then
            AC_MSG_RESULT([$ac__path])
            $1=$ac__path
            break
        fi
    done
    if test z$$1 = z ; then
        AC_MSG_RESULT([not found])
        AC_MSG_ERROR([cannot find $2 in $3])
    fi
])


# JOIN_EACH([OUTPUT], [PATHS], [SUBDIR])
# Append SUBDIR to each of the PATHS and put the result in OUTPUT.
# ----------------------------------------------------------------
AC_DEFUN([JOIN_EACH], [
    $1=
    for dir in $2
    do
        $1="$$1 $dir/$3"
    done
])
