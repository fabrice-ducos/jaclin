#------------------------------------------------------------------------
# AC_MSG_LOG( MSG, ?LOGONLY? )
#
#	Write the message out to the config.log file and the console.
#	If 1 is passed as the second argument, then write to the
#	config.log file only.
#
# Arguments:
#	1. The message to log
#	2. Optional boolean, if true then write to config.log only
#------------------------------------------------------------------------

AC_DEFUN([AC_MSG_LOG], [
    echo $1 >&AC_FD_LOG
    ifval([$2], , [echo $1])
])

#------------------------------------------------------------------------
# AC_GREP_FILE( PATTERN, FILE, ACTION-IF-FOUND, [ACTION-IF-NOT-FOUND])
#
#	Use grep to search for a pattern in a file. If the pattern
#	is not found then return a non zero exit status. No information
#	will be echoed to the screen by this macro.
#
# Arguments:
#	1. The pattern to search for
#	2. The name of the file to be grep'ed
#	3. The script to execute if PATTERN is found in FILE
#	4. The script to execute if PATTERN is not found in FILE (optional)
#------------------------------------------------------------------------

AC_DEFUN([AC_GREP_FILE], [
    AC_MSG_LOG([grep in $2 for pattern '"$1"'], 1)
    if (grep "$1" $2 > /dev/null 2>&1) ; then
        AC_MSG_LOG([grep result : yes], 1)
        $3
    else
        AC_MSG_LOG([grep result : no], 1)
        ifval([$4], [
            $4
        ])dnl
    fi
])


#------------------------------------------------------------------------
# TCLJAVA_VERSION_CHECK
#
#	Check the TCLJAVA_VERSION variable set at the top of configure.in.
#	If it has changed since we last ran configure, then we need to
#	update the hard coded tcljava version numbers in source files.
#
# Arguments:
#	NONE
#------------------------------------------------------------------------

AC_DEFUN([TCLJAVA_VERSION_CHECK], [
    TOOLS=$srcdir/unix/tools

    # The tools directory is not distributed in dist .tar files.
    if test -d "$TOOLS" ; then

        # Check that grep is working by looking for the match to the
        # current version number in the configure.in file.

	AC_GREP_FILE(TCLJAVA_VERSION=$TCLJAVA_VERSION, $srcdir/configure.in, , [
            AC_MSG_ERROR([TCLJAVA_VERSION grep failed.
            did you rerun autoconf after changing the version number in configure.in?])
        ])

        # Check that the files we need actually exist

        if test ! -d $TOOLS/vutil ; then
            AC_MSG_ERROR([$TOOLS/vutil does not exist])
        fi

        if test ! -f $TOOLS/vutil/vsub.sh ; then
            AC_MSG_ERROR([$TOOLS/vutil/vsub.sh does not exist])
        fi

        # Find out if we need to update the version numbers by checking to
        # see if the one we got differs from the one in the file "current".

        AC_GREP_FILE(TCLJAVA_VERSION=$TCLJAVA_VERSION, $TOOLS/vutil/current, , [
            AC_MSG_LOG([Updating version numbers in source files])
            TMP_CWD=`pwd`
            cd $TOOLS/vutil
            $SHELL vsub.sh $TCLJAVA_VERSION
            cd $TMP_CWD
        ])
    fi
])




#------------------------------------------------------------------------
# TCLJAVA_ENABLE_JACL_OR_TCLBLEND
#
#	Check to see is --enable-jacl or --enable-tclblend is given
#	at the command line. If one of them is given then configure
#	and build for that subsystem only, otherwise do an automated
#	check to see which one we should build. Configure to build
#	them both if possible (for instance, when checked out of the CVS)
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	TCLJAVA is set to "jacl" "tclblend" or "both".
#
#------------------------------------------------------------------------

AC_DEFUN([TCLJAVA_ENABLE_JACL_OR_TCLBLEND], [

    AC_ARG_ENABLE(jacl, [  --enable-jacl            build Jacl only, used with CVS version],
	[ok=$enableval], [ok=no])
    if test "$ok" = "yes"; then
	TCLJAVA=jacl
    fi

    AC_ARG_ENABLE(tclblend, [  --enable-tclblend        build Tcl Blend only, used with CVS version],
	[ok=$enableval], [ok=no])
    if test "$ok" = "yes"; then
	TCLJAVA=tclblend
    fi

    if test "x$TCLJAVA" = "x"; then
        if test -d $srcdir/src/tclblend && test -d $srcdir/src/jacl ; then
            AC_MSG_LOG(configuring for both jacl and tclblend)
            TCLJAVA=both
        elif test -d $srcdir/src/tclblend ; then
            TCLJAVA=tclblend
        elif  test -d $srcdir/src/jacl ; then
            TCLJAVA=jacl
        else
            AC_MSG_ERROR([Cannot find jacl or tclblend sources])
        fi
    fi
])




#------------------------------------------------------------------------
# AC_JAVA_WITH_JDK
#
#	Check to see if the --with-jdk command line option is given.
#	If it was, then set ac_java_with_jdk to the DIR argument.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	ac_java_with_jdk can be set to the directory where the jdk lives
#	ac_java_jvm_name can be set to "jdk"
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_WITH_JDK], [
    AC_ARG_WITH(jdk, [  --with-jdk=DIR          use Sun's JDK from DIR], ok=$withval, ok=no)
    if test "$ok" = "no" ; then
        NO=op
    elif test "$ok" = "yes" || test ! -d "$ok"; then
        AC_MSG_ERROR([--with-jdk=DIR option, must pass a valid DIR])
    elif test "$ok" != "no" ; then
        ac_java_jvm_dir=$ok
        ac_java_jvm_name=jdk
    fi
])

#------------------------------------------------------------------------
# AC_JAVA_WITH_KAFFE
#
#	Check to see if the --with-kaffe command line option is given.
#	If it was, then set ac_java_with_kaffe to the DIR argument.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	ac_java_jvm_dir can be set to the directory where the kaffe lives
#	ac_java_jvm_name cab be set to "kaffe"
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_WITH_KAFFE], [
    AC_ARG_WITH(kaffe, [  --with-kaffe=DIR        use Kaffe Open JVM], ok=$withval, ok=no)
    if test "$ok" = "no" ; then
        NO=op
    elif test "$ok" = "yes" || test ! -d "$ok"; then
        AC_MSG_ERROR([--with-kaffe=DIR option, must pass a valid DIR])
    elif test "$ok" != "no" ; then
        ac_java_jvm_dir=$ok
        ac_java_jvm_name=kaffe
    fi
])

#------------------------------------------------------------------------
# AC_JAVA_WITH_JIKES
#
#	Check to see if the --with-jikes command line option is given.
#	If it was, then set JAVAC to the jikes compiler. We default
#	to using jikes if it can be found event if --with-jikes is not given.
#
#	If you want to use jikes as a cross compiler, you will need to
#	use this macro before AC_JAVA_DETECT_JVM and set the CLASSPATH
#	env variable before running configure.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	JAVAC
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_WITH_JIKES], [
    AC_ARG_WITH(jikes, [  --with-jikes=PROG       use jikes compiler given by PROG, if PROG is not given look for jikes on the PATH.],
    ok=$withval, ok=yes)
    if test "$ok" = "no" ; then
        JIKES=
    else
        if test "$ok" = "yes"; then
            AC_PATH_PROG(JIKES, jikes, $JAVAC)
        else
            JIKES=$ok
        fi
        if test ! -f "$JIKES" ; then
            AC_MSG_ERROR([jikes executable '$JIKES' does not exist or is not executable.])
        fi
        AC_MSG_LOG([Using JIKES=$JIKES], 1)
    fi
])

#------------------------------------------------------------------------
# AC_PROG_JAVAC
#
#	If JAVAC is not already defined, then search for "javac" on
#	the path. If a java compiler is found, then test it to make
#	sure it actually works.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	JAVAC can be set to the path name of the java compiler
#	JAVAC_FLAGS can be set to compiler specific flags
#	ac_java_jvm_dir can be set to the jvm's root directory
#------------------------------------------------------------------------

AC_DEFUN([AC_PROG_JAVAC], [
    if test "x$JAVAC" = "x" ; then
        AC_PATH_PROG(JAVAC, javac)
        if test "x$JAVAC" = "x" ; then
            AC_MSG_ERROR([javac not found on PATH])
        fi
    fi
    if test ! -f "$JAVAC" ; then
        AC_MSG_ERROR([javac '$JAVAC' does not exist.
        Perhaps Java is not installed or you passed a bad dir to a --with option.])
    fi

    # If we were searching for javac, then set ac_java_jvm_dir
    if test "x$ac_java_jvm_dir" = "x"; then
        TMP=`dirname $JAVAC`
        TMP=`dirname $TMP`
        ac_java_jvm_dir=$TMP
    fi

    # If the user wanted to use jikes instead of javac, set that now
    if test "x$JIKES" != "x" ; then
        JAVAC=$JIKES
    fi

    # Look for a setting for the CLASSPATH, we might need one to run JAVAC
    AC_JAVA_CLASSPATH

    # FIXME : add detection of command line arguments for JAVAC

    JAVAC_FLAGS=-g
    JAVAC_D_FLAG=-d

    dnl Test out the Java compiler with an empty class
    AC_MSG_CHECKING([to see if the java compiler works])
    AC_JAVA_TRY_COMPILE(,,works=yes)
    if test "$works" = "yes" ; then
        AC_MSG_RESULT($works)
    else
        AC_MSG_ERROR([Could not compile simple Java program with '$JAVAC'])
    fi
])


#------------------------------------------------------------------------
# AC_JAVA_TRY_COMPILE(imports, main-body, action-if-worked, [action-if-failed])
#
#	Try to compile a Java program. This works a lot like AC_TRY_COMPILE
#	except is supports Java instead of C or C++. This macro will create
#	a file named Test.java and try to compile it.
#
# Arguments:
#	imports should contain Java import statements like [import java.util.*;]
#       main-body should contain the code to appear in the main() method
#	action-if-worked should contain the code to run if the compile worked
#	action-if-failed should contain the code to run if the compile failed (optional)
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_TRY_COMPILE], [
    cat << \EOF > conftest.java
// [#]line __oline__ "configure"
[$1]

public class conftest {
    public static void main(String[[]] argv) {
        [$2]
    }
}
EOF

    CLASSPATH=$ac_java_classpath
    export CLASSPATH
    cmd="$JAVAC ${JAVAC_FLAGS} conftest.java"
    if (echo $cmd >&AC_FD_LOG ; eval $cmd >&AC_FD_LOG 2>&AC_FD_LOG) ; then
        echo "yes" >&AC_FD_LOG
        $3
    else
        echo "configure: failed program was:" >&AC_FD_LOG
        cat conftest.java >&AC_FD_LOG
        echo "configure: CLASSPATH was $CLASSPATH" >&AC_FD_LOG
        ifval([$4],
        [  $4
        ])dnl
    fi
])

#------------------------------------------------------------------------
# AC_JAVA_DETECT_JVM
#
#	Figure out what JVM to build with. If no JVM was already defined
#	using a --with command line option then we search for one
#	by looking for the javac executable.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	JAVAC
#	ac_java_jvm_version can be set to 1.1, 1.2, or 1.3
#	ac_java_jvm_dir can be set to the jvm's root directory
#
# DEPENDS ON:
#	This macro can depend on the values set by the following macros:
#	AC_JAVA_WITH_JDK
#	AC_JAVA_WITH_KAFFE
#	AC_PROG_JAVAC
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_DETECT_JVM], [

    # if we do not know the jvm dir, javac will be found on the PATH
    if test "x$JAVAC" = "x" && test "x$ac_java_jvm_dir" != "x"; then
        ac_java_jvm_dir=`cd $ac_java_jvm_dir ; pwd`
        JAVAC=$ac_java_jvm_dir/bin/javac${EXEEXT}
    fi

    # Search for and test the javac compiler
    AC_PROG_JAVAC

    AC_MSG_LOG([Java found in $ac_java_jvm_dir])

    # Try to detect non JDK JVMs. If we can't, then just assume a jdk

    AC_MSG_CHECKING([type of jvm]) 

    if test "x$ac_java_jvm_name" = "x" ; then
        AC_JAVA_TRY_COMPILE([import kaffe.lang.Application;],,ac_java_jvm_name=kaffe)
    fi

    if test "x$ac_java_jvm_name" = "x" ; then
        AC_JAVA_TRY_COMPILE([import gnu.java.io.EncodingManager;],,ac_java_jvm_name=gcj)
    fi

    if test "x$ac_java_jvm_name" = "x" ; then
       ac_java_jvm_name=jdk
    fi

    AC_MSG_RESULT([$ac_java_jvm_name])

    case "$ac_java_jvm_name" in
        gcj) DO=nothing ;;
        jdk) DO=nothing ;;
        kaffe) DO=nothing ;;
        *) AC_MSG_ERROR(['$ac_java_jvm_name' is not a supported JVM]) ;;
    esac

    # Try to detect the version of java that is installed

    AC_MSG_CHECKING([java API version])

    # The class java.lang.StrictMath is new to 1.3

    AC_JAVA_TRY_COMPILE([import java.lang.StrictMath;], , ac_java_jvm_version=1.3)

    # The class java.lang.Package is new to 1.2

    if test "x$ac_java_jvm_version" = "x" ; then
        AC_JAVA_TRY_COMPILE([import java.lang.Package;], , ac_java_jvm_version=1.2)
    fi

    # The class java.lang.reflect.Method is new to 1.1

    if test "x$ac_java_jvm_version" = "x" ; then
        AC_JAVA_TRY_COMPILE([import java.lang.reflect.Method;], , ac_java_jvm_version=1.1)
    fi

    if test "x$ac_java_jvm_version" = "x" ; then
        AC_MSG_ERROR([Could not detect Java version 1.1 or newer])
    fi

    AC_MSG_RESULT([$ac_java_jvm_version])

])


#------------------------------------------------------------------------
# AC_JAVA_CLASSPATH
#
#	Find out which .zip or .jar files need to be included on
#	the CLASSPATH if we are setting it via an env variable.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	ac_java_classpath
#
# DEPENDS ON:
#	This macro is used by the AC_JAVA_DETECT_JVM macro.
#	It depends on the ac_java_jvm_dir variable.
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_CLASSPATH], [
    AC_MSG_CHECKING([for zip or jar files to include on CLASSPATH])

    if test "x$ac_java_jvm_dir" = "x" ; then
        AC_MSG_ERROR([jvm directory not set])
    fi

    # Use the CLASSPATH env variable is the user set it
    if test "x$CLASSPATH" != "x" ; then
        ac_java_classpath=$CLASSPATH
    fi

    # GNU gcj does not need to set the CLASSPATH.

    # Kaffe 1.X
    F=share/kaffe/Klasses.jar
    if test "x$ac_java_classpath" = "x" ; then
        AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
        if test -f $ac_java_jvm_dir/$F ; then
            AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
            ac_java_classpath=$ac_java_jvm_dir/$F
        fi
    fi

    # SGI IRIX 1.1
    F=lib/rt.jar
    if test "x$ac_java_classpath" = "x" ; then
        AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
        if test -f $ac_java_jvm_dir/$F ; then
            AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
            ac_java_classpath=$ac_java_jvm_dir/$F
        fi
    fi

    # Sun JDK 1.1
    F=lib/classes.zip
    if test "x$ac_java_classpath" = "x" ; then
        AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
        if test -f $ac_java_jvm_dir/$F ; then
            AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
            ac_java_classpath=$ac_java_jvm_dir/$F
        fi
    fi

    # Sun JDK 1.2
    F=jre/lib/rt.jar
    if test "x$ac_java_classpath" = "x" ; then
        AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
        if test -f $ac_java_jvm_dir/$F ; then
            AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
            ac_java_classpath=$ac_java_jvm_dir/$F
        fi
    fi

    AC_MSG_LOG([Using CLASSPATH=$ac_java_classpath], 1)
    AC_MSG_RESULT($ac_java_classpath)
])


#------------------------------------------------------------------------
# AC_JAVA_TOOLS
#
#	Figure out the paths of any Java tools we will need later on.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	JAVA
#	JAVA_G
#	JAVAC
#	JAVAH
#	JAR
#	JDB
#
# DEPENDS ON:
#	This macro must be run after the AC_JAVA_DETECT_JVM macro as
#	it depends on the ac_java_jvm_name, ac_java_jvm_version and
#	ac_java_jvm_dir variables
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_TOOLS], [
    AC_JAVA_TOOLS_CHECK(JAVA, java, $ac_java_jvm_dir/bin)

    # Don't error if java_g can not be found
    JAVA_G_FLAGS=-debug
    AC_JAVA_TOOLS_CHECK(JAVA_G, java_g, $ac_java_jvm_dir/bin, 1)

    TOOL=javah
    if test "$ac_java_jvm_name" = "kaffe" ; then
        TOOL=kaffeh
    fi
    AC_JAVA_TOOLS_CHECK(JAVAH, $TOOL, $ac_java_jvm_dir/bin)  


    AC_JAVA_TOOLS_CHECK(JAR, jar, $ac_java_jvm_dir/bin)

    # Don't error if jdb can not be found
    AC_JAVA_TOOLS_CHECK(JDB, jdb, $ac_java_jvm_dir/bin, 1)
])

#------------------------------------------------------------------------
# AC_JAVA_TOOLS_CHECK(VARIABLE, TOOL, PATH, NOERR)
#
#	Helper function that will look for the given tool on the
#	given PATH. If cross compiling and the tool can not
#	be found on the PATH, then search for the same tool
#	on the users PATH. If the tool still can not be found
#	then give up with an error unless NOERR is 1.
#
# Arguments:
#	1. The variable name we pass to AC_PATH_PROG
#	2. The name of the tool
#	3. The path to search on
#	4. Pass 1 if you do not want any error generated 
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_TOOLS_CHECK], [
    if test "$cross_compiling" = "yes" ; then
        AC_PATH_PROG($1, $2)
    else
        AC_PATH_PROG($1, $2, , $3)
    fi

    # Check to see if $1 could not be found

    ifval([$4], ,[
    if test "x[$]$1" = "x" ; then
        AC_MSG_ERROR([Cannot find $2 on $3])
    fi
    ])
])

#------------------------------------------------------------------------
# AC_JAVA_JNI_INCLUDE
#
#	Figure out where jni.h and jni_md.h include files are installed.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	ac_java_jvm_jni_include_flags : Flags that we pass to the compiler
#           so that it can locate JNI headers. (for example: -I/usr/jdk/include)
#
# DEPENDS ON:
#	This macro must be run after the AC_JAVA_DETECT_JVM macro as
#	it depends on the ac_java_jvm_dir variable.
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_JNI_INCLUDE], [

    # Look for jni.h in the subdirectory $ac_java_jvm_dir/include

    F=$ac_java_jvm_dir/include/jni.h
    if test -f "$F" ; then
         ac_java_jvm_jni_include_flags="-I`dirname $F`"
    else
         F=`ls $ac_java_jvm_dir/include/*/jni.h 2>/dev/null`
         if test -f "$F" ; then
             ac_java_jvm_jni_include_flags="-I`dirname $F`"
         else
             AC_MSG_ERROR([Could not locate Java's jni.h include file])
         fi
    fi

    # Look for jni_md.h in an arch specific subdirectory
    # we assume that there is only one arch subdirectory,
    # if that is not the case we would need to use $host

    # FIXME: check to make sure this works in case the above else
    # branch is taken, (a include subdir for an arch?)
    F=`ls $ac_java_jvm_dir/include/*/jni_md.h 2>/dev/null`
    if test -f "$F" ; then
        ac_java_jvm_jni_include_flags="$ac_java_jvm_jni_include_flags -I`dirname $F`"
    fi


    AC_MSG_LOG([Using the following JNI include flags $ac_java_jvm_jni_include_flags])

    # Make sure a simple #include <jni.h> will compile.

    AC_REQUIRE([AC_PROG_CC])

    AC_CACHE_CHECK(to see if jni.h can be included,
        ac_java_jvm_jni_working,
        AC_LANG_PUSH(C)
        ac_saved_cflags=$CFLAGS
        CFLAGS="$CFLAGS $ac_java_jvm_jni_include_flags"
        AC_TRY_COMPILE([
            #include <jni.h>
        ],[return 0;],
        ac_java_jvm_jni_working=yes,
        AC_MSG_ERROR([could not compile file that includes jni.h]))
        AC_LANG_POP()
        CFLAGS=$ac_saved_cflags
    )

    # FIXME: should we look for or require a include/native_threads dir?
])


#------------------------------------------------------------------------
# AC_JAVA_JNI_LIBS
#
#	Figure out where the native threads libraries for JNI live.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	ac_java_jvm_ld_preload : list of libraries to include in LD_PROLOAD
#	ac_java_jvm_ld_bind_now : if set to 1, then use LD_BIND_NOW=1
#	ac_java_jvm_jni_lib_flags : library flags that we will pass to the compiler.
#	    For instance, we might pass -L/usr/jdk/lib -ljava
#	ac_java_jvm_jni_lib_runtime_path : colon seperated path of directories
#	    that is typically passed to rld.
#
# DEPENDS ON:
#	This macro must be run after the AC_JAVA_DETECT_JVM macro as
#	it depends on the ac_java_jvm_dir variable.
#------------------------------------------------------------------------

AC_DEFUN([AC_JAVA_JNI_LIBS], [
    # Check for Kaffe install directory location. Thankfully, Kaffe
    # uses a rather standard install lib location across systems

    if test "$ac_java_jvm_name" = "kaffe" ; then
        # Kaffe JVM under Cygwin (untested, is -lpthread needed?)

        F=lib/kaffevm.dll
        if test "x$ac_java_jvm_jni_lib_flags" = "x" ; then
            AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
            if test -f $ac_java_jvm_dir/$F ; then
                AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
                D=`dirname $ac_java_jvm_dir/$F`
                ac_java_jvm_jni_lib_runtime_path=$D
                ac_java_jvm_jni_lib_flags="-lpthread -L$D -lkaffevm"
            fi
        fi

        # Kaffe JVM under Unix

        F=lib/libkaffevm.so
        if test "x$ac_java_jvm_jni_lib_flags" = "x" ; then
            AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
            if test -f $ac_java_jvm_dir/$F ; then
                AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
                D=`dirname $ac_java_jvm_dir/$F`
                ac_java_jvm_jni_lib_runtime_path=$D
                ac_java_jvm_jni_lib_flags="-lpthread -L$D -lkaffevm -ldl"
                # Kaffe requires lib/kaffe on the lib path or it fails to load
                D=$ac_java_jvm_dir/lib/kaffe
                ac_java_jvm_jni_lib_runtime_path="${ac_java_jvm_jni_lib_runtime_path}:$D"
            fi
        fi
    fi


    # Check for known JDK installation layouts

    if test "$ac_java_jvm_name" = "jdk"; then

        # IRIX 1.1 JDK (32 bit ABI)

        F=lib32/sgi/native_threads/libjava.so
        if test "x$ac_java_jvm_jni_lib_flags" = "x" ; then
            AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
            if test -f $ac_java_jvm_dir/$F ; then
                AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
                D=`dirname $ac_java_jvm_dir/$F`
                ac_java_jvm_jni_lib_runtime_path=$D
                ac_java_jvm_jni_lib_flags="-lpthread -L$D -ljava"
                ac_java_jvm_ld_bind_now=1
            fi
        fi

        # HP-UX 1.1 JDK on PA_RISC

        F=lib/PA_RISC/native_threads/libjava.sl
        if test "x$ac_java_jvm_jni_lib_flags" = "x" ; then
            AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
            if test -f $ac_java_jvm_dir/$F ; then
                AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
                D=`dirname $ac_java_jvm_dir/$F`
                ac_java_jvm_jni_lib_runtime_path=$D
                ac_java_jvm_jni_lib_flags="-lpthread -L$D -ljava"
            fi
        fi

        # IBM JDK 1.1 for Linux

        F=lib/linux/native_threads/libjava.so
        if test "x$ac_java_jvm_jni_lib_flags" = "x" ; then
            AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
            if test -f $ac_java_jvm_dir/$F ; then
                AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
                D=`dirname $ac_java_jvm_dir/$F`
                ac_java_jvm_jni_lib_runtime_path=$D
                ac_java_jvm_jni_lib_flags="-lpthread -L$D -ljava"
            fi
        fi

        # IBM JDK 1.3 for Linux

        F=jre/bin/libjava.so
        if test "x$ac_java_jvm_jni_lib_flags" = "x" ; then
            AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
            if test -f $ac_java_jvm_dir/$F ; then
                AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
                D=`dirname $ac_java_jvm_dir/$F`
                ac_java_jvm_jni_lib_runtime_path=$D
                ac_java_jvm_jni_lib_flags="-lpthread -L$D -ljava"
                D=$ac_java_jvm_dir/jre/bin/classic
                ac_java_jvm_jni_lib_runtime_path="${ac_java_jvm_jni_lib_runtime_path}:$D"
                ac_java_jvm_jni_lib_flags="$ac_java_jvm_jni_lib_flags -L$D -ljvm -lhpi"
            fi
        fi

        # JDK 1.1 for Solaris

        F=lib/sparc/native_threads/libjava.so
        if test "x$ac_java_jvm_jni_lib_flags" = "x" ; then
            AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
            if test -f $ac_java_jvm_dir/$F ; then
                AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
                D=`dirname $ac_java_jvm_dir/$F`
                ac_java_jvm_jni_lib_runtime_path=$D
                ac_java_jvm_jni_lib_flags="-lthread -L$D -ljava"
            fi
        fi

        # JDK 1.2 for Solaris (groan, handle regular and production layout)

        F=jre/lib/sparc/libjava.so
        if test "x$ac_java_jvm_jni_lib_flags" = "x" ; then
            AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
            if test -f $ac_java_jvm_dir/$F ; then
                AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
                D=`dirname $ac_java_jvm_dir/$F`
                ac_java_jvm_jni_lib_runtime_path=$D
                ac_java_jvm_jni_lib_flags="-lthread -L$D -ljava"
                D=$ac_java_jvm_dir/jre/lib/sparc/native_threads
                if test -d $D ; then
                    ac_java_jvm_jni_lib_flags="$ac_java_jvm_jni_lib_flags -L$D"
                    ac_java_jvm_jni_lib_runtime_path="${ac_java_jvm_jni_lib_runtime_path}:$D"
                fi
                D=$ac_java_jvm_dir/jre/lib/sparc/classic
                if test -d $D ; then
                    ac_java_jvm_jni_lib_flags="$ac_java_jvm_jni_lib_flags -L$D"
                    ac_java_jvm_jni_lib_runtime_path="${ac_java_jvm_jni_lib_runtime_path}:$D"
                fi

                ac_java_jvm_jni_lib_flags="$ac_java_jvm_jni_lib_flags -ljvm -lhpi"
            fi
        fi

        # Blackdown JDK 1.1 for Linux (this one can get a little wacky)

        F=README.linux
        if test "x$ac_java_jvm_jni_lib_flags" = "x" &&
            test -f $ac_java_jvm_dir/$F ; then
            # Figure out if it is 1.1.8 and not 1.1.7
            AC_GREP_FILE([JDK 1.1.8], $ac_java_jvm_dir/$F, IS118=1)

            F=lib/`uname --machine`/native_threads/libjava.so
            AC_MSG_LOG([Looking for $ac_java_jvm_dir/$F], 1)
            if test -f $ac_java_jvm_dir/$F ; then
                AC_MSG_LOG([Found $ac_java_jvm_dir/$F], 1)
                D=`dirname $ac_java_jvm_dir/$F`
                ac_java_jvm_jni_lib_runtime_path=$D
                ac_java_jvm_jni_lib_flags="-lpthread -L$D -ljava"

                # We only want to use this scary hack with Blackdown 1.1.7
                if test "x$IS118" = "x" ; then
                    AC_MSG_LOG([Using AWT GUI components under Tcl Blend with the Linux port of the JDK 1.1.7 from Blackdown requires a special modification to jtclsh and the Makefile. See known_issues.txt for more info.])
                    ac_java_jvm_ld_preload="libpthread.so libjava.so"
                    ac_java_jvm_ld_bind_now=1
                fi
            fi
        fi

        #FIXME : Blackdown JDK 1.2 for Linux

    fi

    AC_MSG_LOG([Using the following JNI library flags $ac_java_jvm_jni_lib_flags])
    AC_MSG_LOG([Using the following runtime library path $ac_java_jvm_jni_lib_runtime_path])

    AC_MSG_LOG([Using LD_PRELOAD=$ac_java_jvm_ld_preload], 1)
    AC_MSG_LOG([Using LD_BIND_NOW=$ac_java_jvm_ld_bind_now], 1)

    # Make sure we can compile and link a trivial JNI program

    AC_REQUIRE([AC_PROG_CC])

    AC_CACHE_CHECK(to see if we can link a JNI application,
        ac_java_jvm_working_jni_link,
        AC_LANG_PUSH(C)
        ac_saved_cflags=$CFLAGS
        CFLAGS="$CFLAGS $ac_java_jvm_jni_include_flags $ac_java_jvm_jni_lib_flags"
        AC_TRY_LINK([
            #include <jni.h>
        ],[JNI_GetCreatedJavaVMs(NULL,0,NULL);],
        ac_java_jvm_working_jni_link=yes,
        AC_MSG_ERROR([could not link file that includes jni.h
        It is likely that your JVM install is broken or corrupted.]))
        AC_LANG_POP()
        CFLAGS=$ac_saved_cflags
    )
])




#------------------------------------------------------------------------
# TCLJAVA_WITH_TCL
#
#	Check to see if the --with-tcl command line option is given.
#	If it was, then load Tcl configure info from tclConfig.sh
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	FIXME
#------------------------------------------------------------------------

AC_DEFUN([TCLJAVA_WITH_TCL], [

if test $TCLJAVA = "tclblend" || test $TCLJAVA = "both"; then

    #--------------------------------------------------------------------
    #	See if there was a command-line option for where Tcl is;  if
    #	not, assume that its top-level directory is a sibling of ours.
    #--------------------------------------------------------------------
    
    AC_ARG_WITH(tcl, [  --with-tcl=DIR          build directory for Tcl 8.3.1 (or newer) source release from DIR],
    	TCL_BIN_DIR=$withval, TCL_BIN_DIR="$srcdir/../tcl8.3.1/unix")

    if test ! -d "$TCL_BIN_DIR"; then
        AC_MSG_ERROR([Tcl directory $TCL_BIN_DIR could not be located.
Use the --with-tcl=<dirName> configure flag to specify the location.])
    else
	TCL_BIN_DIR=`cd $TCL_BIN_DIR; pwd`
    fi

    AC_MSG_LOG([checking for Tcl build in $TCL_BIN_DIR])

    if test ! -f $TCL_BIN_DIR/tclConfig.sh; then
        # provide shortcut if --with-tcl=$PATH/tcl8.X was given
        if test -f $TCL_BIN_DIR/unix/tclConfig.sh; then
            TCL_BIN_DIR=$TCL_BIN_DIR/unix
        else
	    AC_MSG_ERROR([Tcl was not configured in the directory $TCL_BIN_DIR.])
        fi
    fi

    #--------------------------------------------------------------------
    #	Read in configuration information generated by Tcl for shared
    #	libraries, and arrange for it to be substituted into our
    #	Makefile.
    #--------------------------------------------------------------------

    file=$TCL_BIN_DIR/tclConfig.sh
    . $file
    if test "$TCL_SHARED_BUILD" = "0" ; then
        AC_MSG_ERROR([Tcl was not built correctly.  
Make sure Tcl was configured with --enable-shared.])
    fi

    CC=$TCL_CC
    SHLIB_CFLAGS=$TCL_SHLIB_CFLAGS
    SHLIB_LD=$TCL_SHLIB_LD
    SHLIB_LD_LIBS=$TCL_SHLIB_LD_LIBS
    SHLIB_PREFIX=lib
    SHLIB_SUFFIX=$TCL_SHLIB_SUFFIX
    SHLIB_VERSION=$TCL_SHLIB_VERSION

    # Set debug extension for the Tcl Blend shared lib
    # as defined by the Java method System.loadLibrary()

    # FIXME : this is removed until System.loadLibrary() bugs are fixed.
    #if test "$TCL_DBGX" = "g"; then
    #    TCLJAVA_DBGX=_g
    #fi

#FIXME: replace this, does this happend in Tcl 8.3?
    # Fix up the TCL_LD_SEARCH_FLAGS (known problem fixed by TEA)

    case "`uname -s`" in
        SunOS*)
            TCL_LD_SEARCH_FLAGS=`echo ${TCL_LD_SEARCH_FLAGS} | sed -e 's/-Wl,-R,/-R /'`
            ;;
        IRIX)
            TCL_LD_SEARCH_FLAGS=`echo ${TCL_LD_SEARCH_FLAGS} | sed -e 's/-Wl,-rpath,/-rpath /'`
            ;;
    esac

fi
])


#------------------------------------------------------------------------
# TCLJAVA_CHECK_TCLSH
#
#	Check for the installed version of tclsh and wish. we need to use the
#	one we compiled against because you can not compile with one version
#	and then load into another. If you compiled Tcl Blend with Tcl 8.1 and
#	then load it into a Tcl 8.0 interp, it will segfault.
#
# Arguments:
#	NONE
#
# VARIABLES SET:
#	FIXME
#------------------------------------------------------------------------

AC_DEFUN([TCLJAVA_CHECK_TCLSH], [

if test $TCLJAVA = "tclblend" || test $TCLJAVA = "both"; then

  # Check to make sure that tclsh has been built by looking for the
  # tclsh executable in the TCL_BIN_DIR directory.

  if test ! -x $TCL_BIN_DIR/tclsh; then
    AC_MSG_ERROR([Tcl was configued in $TCL_BIN_DIR, but it has not been built, please build it and run configure again.])
  fi

  # Double check that tclsh works and that it is tcl 8.3.1 or better
  # We need to set LD_LIBRARY_PATH and SHLIB_PATH so that Tcl can find its
  # shared library in the build directory on a Unix or HP-UX system.

  LD_LIBRARY_PATH=$TCL_BIN_DIR:$LD_LIBRARY_PATH
  export LD_LIBRARY_PATH
  SHLIB_PATH=$TCL_BIN_DIR:$SHLIB_PATH
  export SHLIB_PATH

  TCLSH_LOC=$TCL_BIN_DIR/tclsh
  rm -f tcl_version.tcl

  echo 'puts HELLO' > tcl_version.tcl
  if test "`$TCLSH_LOC tcl_version.tcl 2>&AC_FD_LOG`" != "HELLO"; then
    AC_MSG_ERROR([$TCLSH_LOC is broken, I could not run a simple Tcl script with it])
  fi

  echo '
        if {[[catch {package require Tcl 8.3} err]]} {
          puts stderr $err
          exit -1
        }
        puts 1
        exit 0
       ' > tcl_version.tcl

  if test "`$TCLSH_LOC tcl_version.tcl 2>&AC_FD_LOG`" = "1"; then
      AC_MSG_RESULT([Tcl executable $TCLSH_LOC works])
      rm -f tcl_version.tcl
  else
      rm -f tcl_version.tcl
      AC_MSG_ERROR([$TCLSH_LOC is not version 8.3.1 or newer])
  fi

  # Now check to see if "make install" has been run in the tcl directory.
  # The installed executable name is something like tclsh8.3.
  # We also assume that wish is going to be installed in the same
  # location, which could be incorrect but oh well.

  TCL_INSTALL_LIB_DIR=$TCL_EXEC_PREFIX/lib
  TCLSH=$TCL_EXEC_PREFIX/bin/tclsh$TCL_VERSION
  WISH=$TCL_EXEC_PREFIX/bin/wish$TCL_VERSION

  if test ! -x "$TCLSH"; then
      AC_MSG_WARN([Tcl has not been installed yet, it must be installed before installing Tcl Blend])
  fi

fi

])