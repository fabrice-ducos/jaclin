/* 
 * javaIdle.c --
 *
 *	This file contains the native methods for the IdleHandler class.
 *
 * Copyright (c) 1998 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id$
 */

#include "java.h"
#include "javaNative.h"

/*
 * Static functions used in this file.
 */

static void	JavaIdleProc(ClientData clientData);

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_IdleHandler_doWhenIdle --
 *
 *	Create a C level idle handler for a Java IdleHandler object.
 *
 * Results:
 *	Returns the Tcl_IdleToken.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_IdleHandler_doWhenIdle(
    JNIEnv *env,		/* Java environment. */
    jobject idle)		/* Handle to IdleHandler object. */
{
    JNIEnv *oldEnv;

    JAVA_LOCK();
    Tcl_DoWhenIdle(JavaIdleProc, (ClientData) (*env)->NewGlobalRef(env, idle));
    JAVA_UNLOCK();
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_IdleHandler_cancelIdleCall --
 *
 *	Delete a C level idle handler for a Java IdleHandler object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

void JNICALL
Java_tcl_lang_IdleHandler_cancelIdleCall(
    JNIEnv *env,		/* Java environment. */
    jobject idle)		/* Handle to IdleHandler object. */
{
    JNIEnv *oldEnv;
    
    /*
     * Make a global reference, which should have the same numeric value
     * as the one we used in the client data argument.
     */

#ifdef JDK1_2
    jobject tmpIdle;

    tmpIdle = (*env)->NewGlobalRef(env, idle);
    JAVA_LOCK();
    Tcl_CancelIdleCall(JavaIdleProc, (ClientData) tmpIdle); 
    JAVA_UNLOCK();

    /*
     * Delete the ref in the local scope.
     * Formerly we also deleted the one that was used
     * in the client data for the idle proc.
     */

    (*env)->DeleteGlobalRef(env, tmpIdle);
#else
    /* This code causes tests/native/javaIdle.c to fail under JDK1.2 */
    idle = (*env)->NewGlobalRef(env, idle);
    JAVA_LOCK();
    Tcl_CancelIdleCall(JavaIdleProc, (ClientData) idle);
    JAVA_UNLOCK();

    /*
     * Delete both the ref in the local scope and the one that was used
     * in the client data for the idle proc.
     */

    (*env)->DeleteGlobalRef(env, idle);
    (*env)->DeleteGlobalRef(env, idle);
#endif
}

/*
 *----------------------------------------------------------------------
 *
 * JavaIdleProc --
 *
 *	This function is called when a Java idle event occurs.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Invokes arbitrary Java code.
 *
 *----------------------------------------------------------------------
 */

static void
JavaIdleProc(
    ClientData clientData)	/* Global IdleHandler reference */
{
    JNIEnv *env = JavaGetEnv(NULL);
    jobject exception;
    jobject idle = (jobject) clientData;

    /*
     * Call IdleHandler.invoke.
     */

    (*env)->MonitorExit(env, java.NativeLock);
    (*env)->CallVoidMethod(env, idle, java.invokeIdle);
    exception = (*env)->ExceptionOccurred(env);
    (*env)->ExceptionClear(env);
    (*env)->MonitorEnter(env, java.NativeLock);

    /*
     * Release the ref to the idle object now that it has fired.
     */

    (*env)->DeleteGlobalRef(env, idle);

    /*
     * Propagate the exception so the next level up can catch it.
     */

    if (exception) {
	(*env)->Throw(env, exception);
    }
}
