/* 
 * javaTimer.c --
 *
 *	This file contains the native methods for the TimerHandler class.
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
 * The following structure is used to maintain a mapping from timer tokens to
 * Java objects so they can be freed if the timer is deleted.
 */

typedef struct TimerInfo {
    jobject obj;
    Tcl_TimerToken token;
} TimerInfo;

/*
 * Static functions used in this file.
 */

static void	JavaTimerProc(ClientData clientData);

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_TimerHandler_createTimerHandler --
 *
 *	Create a C level timer handler for a Java TimerHandler object.
 *
 * Results:
 *	Returns the Tcl_TimerToken.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jlong JNICALL
Java_tcl_lang_TimerHandler_createTimerHandler(
    JNIEnv *env,		/* Java environment. */
    jobject timer,		/* Handle to TimerHandler object. */
    jint ms)
{
    jlong lvalue;
    JNIEnv *oldEnv;
    TimerInfo *infoPtr;

    JAVA_LOCK();
    infoPtr = (TimerInfo *) ckalloc(sizeof(TimerInfo));
    infoPtr->obj = (*env)->NewGlobalRef(env, timer);
    infoPtr->token = Tcl_CreateTimerHandler(ms, JavaTimerProc,
	    (ClientData) infoPtr);
    JAVA_UNLOCK();

    *(TimerInfo**)&lvalue = infoPtr;
    return lvalue;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_TimerHandler_deleteTimerHandler --
 *
 *	Delete a C level timer handler for a Java TimerHandler object.
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
Java_tcl_lang_TimerHandler_deleteTimerHandler(
    JNIEnv *env,		/* Java environment. */
    jobject timerObj,		/* Handle to TimerHandler object. */
    jlong info)			/* TimerInfo of timer to delete. */
{
    TimerInfo *infoPtr = *(TimerInfo**)&info;
    JNIEnv *oldEnv;

    JAVA_LOCK();
    
    Tcl_DeleteTimerHandler(infoPtr->token);
    (*env)->DeleteGlobalRef(env, infoPtr->obj);
    ckfree((char *)infoPtr);

    JAVA_UNLOCK();
}

/*
 *----------------------------------------------------------------------
 *
 * JavaTimerProc --
 *
 *	This function is called when a Java timer expires.
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
JavaTimerProc(
    ClientData clientData)	/* Pointer to TimerInfo. */
{
    TimerInfo *infoPtr = (TimerInfo *) clientData;
    JNIEnv *env = JavaGetEnv(NULL);
    jobject exception;

    /*
     * Call TimerHandler.invoke.
     */

    (*env)->MonitorExit(env, java.NativeLock);
    (*env)->CallVoidMethod(env, infoPtr->obj, java.invokeTimer);
    exception = (*env)->ExceptionOccurred(env);
    (*env)->ExceptionClear(env);
    (*env)->MonitorEnter(env, java.NativeLock);

    /*
     * Cean up the timer info since the timer has fired.
     */

    (*env)->DeleteGlobalRef(env, infoPtr->obj);
    ckfree((char *)infoPtr);

    /*
     * Propagate the exception so the next level up can catch it.
     */

    if (exception) {
	(*env)->Throw(env, exception);
    }
}
