/* 
 * TclList.java --
 *
 *	This file contains the native implementation of Tcl lists.
 *
 * Copyright (c) 1997 by Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 *
 * RCS: @(#) $Id$
 */

package tcl.lang;

import java.util.*;

/*
 * This class implements the list object type in Tcl.  It is based on the
 * native list implementation, so it extends CObject to inherit the
 * shadowing behavior.
 */

public class TclList extends CObject {

/*
 *----------------------------------------------------------------------
 *
 * TclList --
 *
 *	This constructor creates a new Tcl_Obj.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

TclList()
{
    super();
}

/*
 *----------------------------------------------------------------------
 *
 * TclList --
 *
 *	Construct a new TclList from the given Tcl_Obj reference.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Increments the reference count of the Tcl_Obj.
 *
 *----------------------------------------------------------------------
 */

TclList(
    long objPtr)		// Pointer to Tcl_Obj from C.
{
    super(objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * newInstance --
 *
 *	Construct a new TclObject containing an empty TclList.
 *
 * Results:
 *	Returns a new TclObject.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public static TclObject
newInstance()
{
    return new TclObject(new TclList());
}

/*
 *----------------------------------------------------------------------
 *
 * setListFromAny --
 *
 *	Called to convert an object's internal rep to a list.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static void
setListFromAny(
    Interp interp,		// Interp to report errors in, or null.
    TclObject tobj)		// Object to convert.
throws
    TclException		// Throws an exception of the object isn't a
				// valid Tcl list.
{
    InternalRep rep = tobj.getInternalRep();

    /*
     * If the object is already a Tcl_Obj reference, then we only need to check
     * to see if it is a valid list and copy the reference into a new TclList.
     * Otherwise we need to create a new Tcl_Obj to hold the list.
     */

    if (!(rep instanceof TclList)) {
	TclList tlist;
	long interpPtr = (interp == null) ? 0 : interp.interpPtr;

	if (rep instanceof CObject) {
	    CObject cobj = (CObject) rep;
	    listLength(interpPtr, cobj.objPtr);
	    tlist = new TclList(cobj.objPtr);
	} else {
	    tlist = new TclList(splitList(interpPtr, tobj.toString()));
	}
	tobj.setInternalRep(tlist);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * append --
 *
 *	Appends a list element to a TclObject.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Modifies the underlying Tcl_Obj list.
 *
 *----------------------------------------------------------------------
 */

public static final void
append(
    Interp interp,		// Current interpreter.
    TclObject tobj,		// The TclObject to append an element to.
    TclObject elemObj)		// The element to append to the object.
throws
    TclException		// If tobj cannot be converted into a list.
{
    setListFromAny(interp, tobj);
    tobj.invalidateStringRep();

    TclList tlist = (TclList)tobj.getInternalRep();

    /*
     * We have to update the objPtr after the append, because the append may
     * duplicate the Tcl_Obj if it was shared at the C level.  Because the
     * TclList acts as an extra reference on the Tcl_Obj, we are guaranteed
     * that there will be no circular references when we are done.  After
     * append returns, the refcount on the Tcl_Obj will be 1.
     */

    tlist.objPtr = append(tlist.objPtr, elemObj);
}

/*
 *----------------------------------------------------------------------
 *
 * getLength --
 *
 *	Queries the length of the list. If tobj is not a list object,
 *	an attempt will be made to convert it to a list.
 *
 * Results:
 *	The length of the list.
 *
 * Side effects:
 *	Will attempt to convert the object into a TclList.
 *
 *----------------------------------------------------------------------
 */

public static final int
getLength(
    Interp interp,		// Current interpreter.
    TclObject tobj)		// The TclObject to append an element to.
throws
    TclException		// If tobj is not a valid list.
{
    long interpPtr = (interp == null) ? 0 : interp.interpPtr;
    setListFromAny(interp, tobj);

    TclList tlist = (TclList)tobj.getInternalRep();
    return listLength(interpPtr, tlist.objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * getElements --
 *
 *	Retrieve all of the elements of a list.  The objects referenced
 *	by the returned array should be treated as readonly and their
 *	ref counts are _not_ incremented; the caller must do that if
 *	it holds on to a reference.
 *
 * Results:
 *	Returns a TclObject array of the elements in the list object.
 *
 * Side effects:
 *	Attempts to convert the object to a list and may throw an
 *	exception if the object isn't a list.  
 *
 *----------------------------------------------------------------------
 */

public static TclObject[]
getElements(
    Interp interp,		// Current interpreter.
    TclObject tobj)		// The TclObject to get the elements of.
throws
    TclException		// If tobj is not a valid list.
{
    setListFromAny(interp, tobj);
    TclList tlist = (TclList)tobj.getInternalRep();
    return getElements(tlist.objPtr);
}

/*
 *----------------------------------------------------------------------
 *
 * index --
 *
 *	Retrieve the index'th object from the list.The first element has
 *	index 0. If index is negative or greater than or equal to the
 *	number of elements in the list, a null is returned.
 *
 * Results:
 *	Returns the index'th TclObject in the list.
 *
 * Side effects:
 *	Attempts to convert the object to a list and may throw an
 *	exception if the object isn't a list.  
 *
 *----------------------------------------------------------------------
 */

public static final TclObject
index(
    Interp interp,		// Current interpreter.
    TclObject tobj,		// The TclObject to get the element from.
    int index)			// The index of the requested element.
throws
    TclException		// If tobj is not a valid list.
{
    setListFromAny(interp, tobj);

    TclList tlist = (TclList)tobj.getInternalRep();
    return index(tlist.objPtr, index);
}

/*
 *----------------------------------------------------------------------
 *
 * replace --
 *
 *	This procedure replaces zero or more elements of the list
 *	referenced by tobj with the objects from an TclObject array.
 *	If tobj is not a list object, an attempt will
 *	be made to convert it to a list.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Updates the Tcl_Obj list.
 *
 *----------------------------------------------------------------------
 */

public static final void
replace(
    Interp interp,		// Current interpreter.
    TclObject tobj,		// The TclObject to get the element from.
    int index,			// The starting index of the replace
				// operation. <=0 means the beginning of the
				// list. >= TclList.getLength(tobj) means the
				// end of the list.
    int count,			// the number of elements to delete from the
				// list. <=0 means no elements should be
				// deleted and the operation is equivalent to
				// an insertion operation.
    TclObject elements[],	// The element(s) to insert.
    int from,			// Insert starting at elements[from]
    int to)			// Insert up to elements[to] (inclusive)
throws
    TclException		// If tobj is not a valid list.
{
    setListFromAny(interp, tobj);
    tobj.invalidateStringRep();
    TclList tlist = (TclList)tobj.getInternalRep();
    tlist.objPtr = replace(tlist.objPtr, index, count, elements, from, to);
}

/*
 * Native method declarations.
 */

private static final native long append(long objPtr, TclObject element);
private static final native TclObject[] getElements(long objPtr);
private static final native TclObject index(long objPtr, int index);
private static final native int listLength(long interp, long objPtr)
	throws TclException;
private static final native long replace(long objPtr, int index, int count,
	TclObject elements[], int from, int to);
private static final native long splitList(long interp, String s);

} // end TclList
