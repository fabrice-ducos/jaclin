/* 
 * CObject.java --
 *
 *	This class is used as the internal rep for TclObject instances
 *	that refer to C based Tcl_Obj structures.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id$
 */

package tcl.lang;

/*
 * The CObject class encapsulates a reference to a Tcl_Obj implemented in
 * native code.  When an object is passed to Java from C, a new CObject is
 * constructed to hold the object pointer.  There is always one reference added
 * to the C object for each instance of this class that refers to a given
 * Tcl_Obj*.
 */

class CObject extends InternalRep {

/*
 * This long really contains a Tcl_Obj*.  It is declared with package
 * visibility so that subclasses that define type specific functionality
 * can get to the Tcl_Obj.
 */

long objPtr;


/*
 *----------------------------------------------------------------------
 *
 * CObject --
 *
 *	Construct a new CObject around a newly allocated Tcl_Obj.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Allocates a new Tcl_Obj and increments its reference count.
 *
 *----------------------------------------------------------------------
 */

CObject()
{
    this.objPtr = newCObject(null);
    incrRefCount(objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * CObject --
 *
 *	Construct a new CObject to wrap the given Tcl_Obj.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Increments the reference count of the Tcl_Obj.
 *
 *----------------------------------------------------------------------
 */

CObject(
    long objPtr)		// Pointer to Tcl_Obj from C.
{
    this.objPtr = objPtr;
    incrRefCount(objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * dispose --
 *
 *	Dispose of the CObject.  
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Decrements the reference count of the Tcl_Obj.
 *
 *----------------------------------------------------------------------
 */

public void
dispose()
{
    decrRefCount(objPtr);
    objPtr = 0;
}

/*
 *----------------------------------------------------------------------
 *
 * makeReference --
 *
 *	Convert the underlying Tcl_Obj into a TclObject reference.
 *	This method is only called from Interp.setInternalRep().
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	May change the type of the underlying Tcl_Obj and increment
 *	the refcount of the TclObject.
 *
 *----------------------------------------------------------------------
 */

final void
makeReference(
    TclObject object)		// The object to create a new reference to.
{
    makeRef(objPtr, object);
}

/*
 *----------------------------------------------------------------------
 *
 * duplicate --
 *
 *	Makes a new CObject that refers to the same Tcl_Obj.  Note
 *	that we don't duplicate the Tcl_Obj at this time because it
 *	will get duplicated on demand the first time we try to modify
 *	it since its refcount will be >= 2 after this call.
 *
 * Results:
 *	Returns a new CObject instance
 *
 * Side effects:
 *	Increments the reference count of the Tcl_Obj.
 *
 *----------------------------------------------------------------------
 */

public InternalRep
duplicate()
{
    return new CObject(objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * toString --
 *
 *	Return the string form of the internal rep.  Calls down to
 *	native code to get the string rep of the Tcl_Obj.
 *
 * Results:
 *	Returns the string rep.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public String
toString()
{
    return getString(objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * newInstance --
 *
 *	Construct a new TclObject from a Tcl_Obj*.  This routine is only
 *	called from C.
 *
 * Results:
 *	Returns a newly allocated TclObject.
 *
 * Side effects:
 *	Constructs a new CObject.
 *
 *----------------------------------------------------------------------
 */

private static TclObject
newInstance(
    long objPtr)		// Tcl_Obj to wrap.
{
    return new TclObject(new CObject(objPtr));
}

/*
 *----------------------------------------------------------------------
 *
 * finalize --
 *
 *	Clean up the native Tcl_Obj when a CObject is collected.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Decrements the ref count of the underlying Tcl_Obj.
 *
 *----------------------------------------------------------------------
 */

protected void finalize() throws Throwable
{
    if (objPtr != 0) {
	/*
	 * If the object is finalized while the reference is still valid, 
	 * we need to sever the connection to the underlying Tcl_Obj*.
	 */

	decrRefCount(objPtr);
    }
    super.finalize();
}

/*
 *----------------------------------------------------------------------
 *
 * decrRefCount --
 *
 *	Decrement the refcount of a Tcl_Obj.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	May delete the Tcl_Obj.
 *
 *----------------------------------------------------------------------
 */

static final native void
decrRefCount(
    long objPtr);		// Pointer to Tcl_Obj.

/*
 *----------------------------------------------------------------------
 *
 * incrRefCount --
 *
 *	 Increment the reference count of a Tcl_Obj.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static final native void
incrRefCount(
    long objPtr);		// Pointer to Tcl_Obj.

/*
 *----------------------------------------------------------------------
 *
 * newCObject --
 *
 *	Allocate a new Tcl_Obj with the given string rep.
 *
 * Results:
 *	Returns the address of the new Tcl_Obj with refcount of 0.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */


static final native long
newCObject(
    String rep);		// Initial string rep.

/*
 *----------------------------------------------------------------------
 *
 * getString --
 *
 *	Retrieve the string rep of a Tcl_Obj.
 *
 * Results:
 *	Returns a string.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private static final native String
getString(
    long objPtr);		// Pointer to Tcl_Obj.

/*
 *----------------------------------------------------------------------
 *
 * makeRef --
 *
 *	Convert a Tcl_Obj into a reference to a TclObject.  This routine
 *	is used when the internal rep is being set from the Java side.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Changes the internal representation of the Tcl_Obj.
 *
 *----------------------------------------------------------------------
 */

private static final native void
makeRef(
    long objPtr,		// Pointer to Tcl_Obj.
    TclObject object);		// Object that Tcl_Obj should refer to.

} // end CObject
