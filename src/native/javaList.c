/* 
 * javaList.c --
 *
 *	This file contains the native method implementation the
 *	TclList class.
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
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_TclList_append --
 *
 *	Append an element to the list.
 *
 * Class:     tcl_lang_TclList
 * Method:    append
 * Signature: (JLtcl/lang/TclObject;)J
 *
 * Results:
 *	Returns the resulting list object.
 *
 * Side effects:
 *	May copy the list if it is currently shared.
 *
 *----------------------------------------------------------------------
 */

jlong JNICALL
Java_tcl_lang_TclList_append(
    JNIEnv *env,		/* Java environment. */
    jclass listClass,		/* Handle to TclList class. */
    jlong list,			/* Address of Tcl_Obj. */
    jobject element)		/* Handle to element to append. */
{
    Tcl_Obj *oldListPtr = *(Tcl_Obj **)&list;
    Tcl_Obj *objPtr, *listPtr;
    JNIEnv *oldEnv;

    if (!oldListPtr || !element) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid TclList.");
	return list;
    }

    JAVA_LOCK();

    objPtr = JavaGetTclObj(env, element);
    
    /*
     * Copy the list if it is shared.  Note that we have to adjust the
     * refcount since we will be replacing the reference from the
     * TclObject in Java.
     */

    if (Tcl_IsShared(oldListPtr)) {
	listPtr = Tcl_DuplicateObj(oldListPtr);
	Tcl_IncrRefCount(listPtr);
	Tcl_DecrRefCount(oldListPtr);
    } else {
	listPtr = oldListPtr;
    }

    /*
     * This should never fail, because the calling code converts the object to
     * a list before calling this routine.
     */

    if (Tcl_ListObjAppendElement(NULL, listPtr, objPtr) != TCL_OK) {
	JavaThrowTclException(env, NULL, TCL_ERROR);
    }
    
    *(Tcl_Obj **)&list = listPtr;

    JAVA_UNLOCK();

    return list;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_TclList_getElements --
 *
 *	Retrieved the contents of the specified list.
 *
 * Class:     tcl_lang_TclList
 * Method:    getElements
 * Signature: (J)[Ltcl/lang/TclObject;
 *
 * Results:
 *	Returns a newly allocated jobjectArray containing the elements
 *	of the list.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jobjectArray JNICALL
Java_tcl_lang_TclList_getElements(
    JNIEnv *env,		/* Java environment. */
    jclass listClass,		/* Handle to TclList class. */
    jlong list)			/* Address of Tcl_Obj. */
{
    Tcl_Obj *listPtr = *(Tcl_Obj **)&list;
    Tcl_Obj **objvPtr;
    int objc, i;
    jarray array;
    JNIEnv *oldEnv;

    if (!listPtr) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid TclList.");
	return NULL;
    }

    JAVA_LOCK();
    /*
     * This should never fail, because the calling code converts the object to
     * a list before calling this routine.
     */

    if (Tcl_ListObjGetElements(NULL, listPtr, &objc, &objvPtr) == TCL_ERROR) {
	JavaThrowTclException(env, NULL, TCL_ERROR);
    }

    array = (*env)->NewObjectArray(env, objc, java.TclObject, NULL);
    for (i = 0; i < objc; i++) {
	(*env)->SetObjectArrayElement(env, array, i,
		JavaGetTclObject(env, objvPtr[i], NULL));
    }
    JAVA_UNLOCK();
    return array;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_TclList_index --
 *
 *	Returns the object at the specified index in the list.
 *
 * Class:     tcl_lang_TclList
 * Method:    index
 * Signature: (JI)Ltcl/lang/TclObject;
 *
 * Results:
 *	Returns the TclObject.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jobject JNICALL
Java_tcl_lang_TclList_index(
    JNIEnv *env,		/* Java environment. */
    jclass listClass,		/* Handle to TclList class. */
    jlong list,			/* Address of Tcl_Obj. */
    jint index)			/* Index of element to return. */
{
    Tcl_Obj *listPtr = *(Tcl_Obj **)&list;
    Tcl_Obj *objPtr;
    JNIEnv *oldEnv;
    jobject obj;

    if (!listPtr) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid TclList.");
	return NULL;
    }

    JAVA_LOCK();
    /*
     * This should never fail, because the calling code converts the object to
     * a list before calling this routine.
     */

    if (Tcl_ListObjIndex(NULL, listPtr, index, &objPtr) == TCL_ERROR) {
	JavaThrowTclException(env, NULL, TCL_ERROR);
    }

    obj = JavaGetTclObject(env, objPtr, NULL); 
    JAVA_UNLOCK();
    return obj;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_TclList_listLength --
 *
 *	Retrieve the length of the list.  This routine is used to
 *	convert a Tcl_Obj into a list.
 *
 * Class:     tcl_lang_TclList
 * Method:    listLength
 * Signature: (JJ)I
 *
 * Results:
 *	The length of the list.
 *
 * Side effects:
 *	Converts the object into a list if possible.
 *
 *----------------------------------------------------------------------
 */

jint JNICALL
Java_tcl_lang_TclList_listLength(
    JNIEnv *env,		/* Java environment. */
    jclass listClass,		/* Handle to TclList class. */
    jlong interpPtr,		/* Interpreter for error reporting. */
    jlong list)			/* Address of Tcl_Obj. */ 
{
    Tcl_Obj *listPtr = *(Tcl_Obj **)&list;
    Tcl_Interp *interp = *(Tcl_Interp **)&interpPtr;
    int length;
    JNIEnv *oldEnv;

    if (!listPtr) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid TclList.");
	return 0;
    } else if (!interp) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid Interp.");
	return 0;
    }

    JAVA_LOCK();
    if (Tcl_ListObjLength(interp, listPtr, &length) == TCL_ERROR) {
	JavaThrowTclException(env, interp, TCL_ERROR);
    }
    JAVA_UNLOCK();
    return length;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_TclList_replace --
 *
 *	Replace a range of elements in a list with the specified objects.
 *
 * Class:     tcl_lang_TclList
 * Method:    replace
 * Signature: (JII[Ltcl/lang/TclObject;II)J
 *
 * Results:
 *	Returns the resulting list object.
 *
 * Side effects:
 *	May copy the list if it is shared.
 *
 *----------------------------------------------------------------------
 */

jlong JNICALL
Java_tcl_lang_TclList_replace(
    JNIEnv *env,		/* Java environment. */
    jclass listClass,		/* Handle to TclList class. */
    jlong list,			/* Address of Tcl_Obj. */
    jint index,			/* The starting index. */
    jint count,			/* The number of elements to replace. */
    jobjectArray elements,	/* The elements to insert. */
    jint from,			/* Start inserting from elements[from]. */
    jint to)			/* Insert up to and including elements[to]. */
{
    Tcl_Obj *oldListPtr = *(Tcl_Obj **)&list;
    Tcl_Obj **objv, *listPtr;
    int objc, i;
    jobject element;
    JNIEnv *oldEnv;

    if (!oldListPtr) {
	jclass nullClass = (*env)->FindClass(env,
		"java/lang/NullPointerException");
	(*env)->ThrowNew(env, nullClass, "Invalid TclList.");
	return list;
    }

    JAVA_LOCK();
    if (!elements || (to < from)) {
	objv = NULL;
	objc = 0;
    } else {
	/*
	 * Copy the elements of the list into an objv array.  Since some
	 * of the objects may be newly allocated, we bump the refcount by
	 * one until we're done with the array.  This ensures that we
	 * can cleanly recover from errors without leaking memory.
	 */

	objc = (to - from) + 1;
	objv = (Tcl_Obj **) ckalloc(objc*sizeof(Tcl_Obj *));
	for (i = 0; i < objc; i++) {
	    element = (*env)->GetObjectArrayElement(env, elements, i+from);

	    /*
	     * If an error occurred, we need to decrement the refcount
	     * on all of the elements we've copied so far.  This will
	     * ensure that any newly allocated objects are deallocted.
	     */

	    if ((*env)->ExceptionOccurred(env)) {
		for (i--; i >= 0; i--) {
		    Tcl_DecrRefCount(objv[i]);
		}
		ckfree((char *)objv);
		JAVA_UNLOCK();
		return list;
	    }
	    objv[i] = JavaGetTclObj(env, element);
	    Tcl_IncrRefCount(objv[i]);
	}
    }

    /*
     * Copy the list if it is shared.  Note that we have to adjust the
     * refcount since we will be replacing the reference from the
     * TclObject in Java.
     */

    if (Tcl_IsShared(oldListPtr)) {
	listPtr = Tcl_DuplicateObj(oldListPtr);
	Tcl_IncrRefCount(listPtr);
	Tcl_DecrRefCount(oldListPtr);
    } else {
	listPtr = oldListPtr;
    }

    /*
     * Do the actual replace operation.  This should never fail since
     * the Java code converts the object to a list before calling this
     * routine.
     */

    if (objc > 0) {
	if (Tcl_ListObjReplace(NULL, listPtr, index, count, objc, objv) ==
		TCL_ERROR) {
	    JavaThrowTclException(env, NULL, TCL_ERROR);
	}
    }

    /*
     * Discard the temporary array, releasing the object references.
     */

    if (objv) {
	for (i = 0; i < count; i++) {
	    Tcl_DecrRefCount(objv[i]);
	}
	ckfree((char *)objv);
    }

    /*
     * Return the new value of the list.
     */

    *(Tcl_Obj **)&list = listPtr;

    JAVA_UNLOCK();
    return list;
}

/*
 *----------------------------------------------------------------------
 *
 * Java_tcl_lang_TclList_splitList --
 *
 *	Convert a string into a new list object with refcount of 0.
 *
 * Class:     tcl_lang_TclList
 * Method:    splitList
 * Signature: (JLjava/lang/String;)J
 *
 * Results:
 *	Returns a newly allocated Tcl_Obj.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

jlong JNICALL
Java_tcl_lang_TclList_splitList(
    JNIEnv *env,		/* Java environment. */
    jclass listClass,		/* Handle to TclList class. */
    jlong interpPtr,		/* Interpreter for error reporting. */
    jstring string)		/* String to convert. */ 
{
    Tcl_Interp *interp = *(Tcl_Interp **)&interpPtr;
    Tcl_Obj *listPtr;
    int length;
    jlong list;
    JNIEnv *oldEnv;
    
    /*
     * Copy the string into a new Tcl_Obj and convert it to a list.
     */

    JAVA_LOCK();
    listPtr = Tcl_NewObj();
    if (string) {
	listPtr->bytes = JavaGetString(env, string, &listPtr->length);
    }
    if (Tcl_ListObjLength(interp, listPtr, &length) == TCL_ERROR) {
	Tcl_DecrRefCount(listPtr);
	JavaThrowTclException(env, interp, TCL_ERROR);
	list = 0;
    } else {
	*(Tcl_Obj **)&list = listPtr;
    }

    JAVA_UNLOCK();
    return list;
}

