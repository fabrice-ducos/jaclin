/* 
 * javaUtil.c --
 *
 *	This file contains the native method implementations for the
 *	tcl.lang.Util class.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id$
 */

#include "java.h"
#include "javaNative.h"

/*
 * Declaration of non-exported Tcl routines that we need to use.
 */

EXTERN char *TclGetCwd(Tcl_Interp *);


/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Util_getBoolean --
 *
 *	Convert a string to a boolean value.
 *
 * Class:     tcl_lang_Util
 * Method:    getBoolean
 * Signature: (Ltcl/lang/Interp;Ljava/lang/String;)Z
 *
 * Results:
 *	Returns the boolean value or throws an exception.
 *
 * Side effects:
 *	May leave an error message in the interp result.
 *
 *----------------------------------------------------------------------
 */

jboolean JNICALL
Java_tcl_lang_Util_getBoolean(
    JNIEnv *env,		/* Java environment. */
    jclass utilClass,		/* Handle to Util class. */
    jobject interpObj,		/* Interp object. */
    jstring string)		/* String to convert. */
{
    int bool;
    const char *str;
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    JNIEnv *oldEnv;

    JAVA_LOCK();
    str = (string) ? (*env)->GetStringUTFChars(env, string, NULL) : "";

    if (Tcl_GetBoolean(interp, (/*UNCONST*/ char*) str, &bool) != TCL_OK) {
	JavaThrowTclException(env, interp, TCL_ERROR);
    }
    
    if (string) {
	(*env)->ReleaseStringUTFChars(env, string, str);
    }
    JAVA_UNLOCK();
    return (bool) ? JNI_TRUE : JNI_FALSE;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Util_getDouble --
 *
 *	Convert a string to a double value.
 *
 * Class:     tcl_lang_Util
 * Method:    getDouble
 * Signature: (Ltcl/lang/Interp;Ljava/lang/String;)D
 *
 * Results:
 *	Returns the double value or throws an exception.
 *
 * Side effects:
 *	May leave an error message in the interp result.
 *
 *----------------------------------------------------------------------
 */

jdouble JNICALL
Java_tcl_lang_Util_getDouble(
    JNIEnv *env,		/* Java environment. */
    jclass utilClass,		/* Handle to Util class. */
    jobject interpObj,		/* Interp object. */
    jstring string)		/* String to convert. */
{
    double doubleVal;
    const char *str;
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    JNIEnv *oldEnv;

    JAVA_LOCK();
    str = (string) ? (*env)->GetStringUTFChars(env, string, NULL) : "";

    if (Tcl_GetDouble(interp, (/*UNCONST*/ char*) str, &doubleVal) != TCL_OK) {
	JavaThrowTclException(env, interp, TCL_ERROR);
    }
    
    if (string) {
	(*env)->ReleaseStringUTFChars(env, string, str);
    }
    JAVA_UNLOCK();
    return (jdouble) doubleVal;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Util_getInt --
 *
 *	Convert a string to a integer value.
 *
 * Class:     tcl_lang_Util
 * Method:    getInt
 * Signature: (Ltcl/lang/Interp;Ljava/lang/String;)I
 *
 * Results:
 *	Returns the integer value or throws an exception.
 *
 * Side effects:
 *	May leave an error message in the interp result.
 *
 *----------------------------------------------------------------------
 */

jint JNICALL
Java_tcl_lang_Util_getInt(
    JNIEnv *env,		/* Java environment. */
    jclass utilClass,		/* Handle to Util class. */
    jobject interpObj,		/* Interp object. */
    jstring string)		/* String to convert. */
{
    int intVal;
    const char *str;
    Tcl_Interp *interp = JavaGetInterp(env, interpObj);
    JNIEnv *oldEnv;

    JAVA_LOCK();
    str = (string) ? (*env)->GetStringUTFChars(env, string, NULL) : "";

    if (Tcl_GetInt(interp, (/*UNCONST*/ char*) str, &intVal) != TCL_OK) {
	JavaThrowTclException(env, interp, TCL_ERROR);
    }
    
    if (string) {
	(*env)->ReleaseStringUTFChars(env, string, str);
    }
    JAVA_UNLOCK();
    return (jint) intVal;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Util_printDouble --
 *
 *	Convert a double to a string value.
 *
 * Class:     tcl_lang_Util
 * Method:    printDouble
 * Signature: (D)Ljava/lang/String;
 *
 * Results:
 *	Returns the string form of a double.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jobject JNICALL
Java_tcl_lang_Util_printDouble(
    JNIEnv *env,		/* Java environment. */
    jclass utilClass,		/* Handle to Util class. */
    jdouble value)		/* Value to convert. */
{
    char buf[TCL_DOUBLE_SPACE+1];
    JNIEnv *oldEnv;
    jobject obj;

    JAVA_LOCK();
    Tcl_PrintDouble(NULL, value, buf);
    obj = (*env)->NewStringUTF(env, buf);
    JAVA_UNLOCK();
    return obj;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Util_getCwd --
 *
 *	Retrieve the current working directory.
 *
 * Class:     tcl_lang_Util
 * Method:    getCwd
 * Signature: ()Ljava/lang/String;
 *
 * Results:
 *	Returns the string value of the working directory.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jobject JNICALL
Java_tcl_lang_Util_getCwd(
    JNIEnv *env,		/* Java environment. */
    jclass utilClass)		/* Handle to Util class. */
{
    JNIEnv *oldEnv;
    jobject obj;

    JAVA_LOCK();
    obj = (*env)->NewStringUTF(env, TclGetCwd(NULL));
    JAVA_UNLOCK();
    return obj;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_Util_stringMatch --
 *
 *	Compare a string against a globbing pattern.
 *
 * Results:
 *	Returns true if the string matches the pattern.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jboolean JNICALL
Java_tcl_lang_Util_stringMatch(
    JNIEnv *env,		/* Java environment. */
    jclass utilClass,		/* Handle to Util class. */
    jstring string,		/* String to compare. */
    jstring pattern)		/* Pattern to compare against. */
{
    const char *str, *pat;
    jboolean result;
    JNIEnv *oldEnv;
    
    if (!pattern || !string) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Bad argument to stringMatch.");
	return JNI_FALSE;
    }

    JAVA_LOCK();
    str = (*env)->GetStringUTFChars(env, string, NULL);
    pat = (*env)->GetStringUTFChars(env, pattern, NULL);
    result = (Tcl_StringMatch((/*UNCONST*/ char *) str,
	    (/*UNCONST*/ char *) pat) ? JNI_TRUE : JNI_FALSE);
    (*env)->ReleaseStringUTFChars(env, string, str);
    (*env)->ReleaseStringUTFChars(env, pattern, pat);
    JAVA_UNLOCK();
    return result;
}
