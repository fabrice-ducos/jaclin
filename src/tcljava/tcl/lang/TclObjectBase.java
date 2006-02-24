/*
 * TclObjectBase.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TclObjectBase.java,v 1.3 2006/02/14 04:13:27 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * This class implements the basic notion of an "object" in Tcl. The
 * fundamental representation of an object is its string value. However,
 * an object can also have an internal representation, which is a "cached"
 * reprsentation of this object in another form. The type of the internal
 * rep of Tcl objects can mutate. This class provides the storage of the
 * string rep and the internal rep, as well as the facilities for mutating
 * the internal rep. The Jacl or TclBlend specific implementation of
 * TclObject will extend this abstract base class.
 */

abstract class TclObjectBase {
    // Internal representation of the object. A valid TclObject
    // will always have a non-null internal rep.

    protected InternalRep internalRep;

    // Reference count of this object. When 0 the object will be deallocated.

    protected int refCount;

    // String  representation of the object.

    protected String stringRep;

    // Setting to true will enable a feature that keeps
    // track of TclObject allocation, internal rep allocation,
    // and transitions from one internal rep to another.

    static final boolean saveObjRecords = false;
    static Hashtable objRecordMap = ( saveObjRecords ? new Hashtable() : null );

    // Only set this to true if running test code and
    // the user wants to run extra ref count checks.
    // Setting this to true will make key methods larger.
    static final boolean extraRefCountChecks = false;

    /**
     * Creates a TclObject with the given InternalRep. This method should be
     * called only by an InternalRep implementation.
     *
     * @param rep the initial InternalRep for this object.
     */
    protected TclObjectBase(InternalRep rep) {
	if (rep == null) {
	    throw new TclRuntimeError("null InternalRep");
	}
	internalRep = rep;
	stringRep = null;
	refCount = 0;

	if (TclObjectBase.saveObjRecords) {
	    String key = "TclObject";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    }

    /**
     * Creates a TclObject with the given InternalRep and stringRep.
     * This constructor is used by the TclString class only. No other place
     * should call this constructor.
     *
     * @param rep the initial InternalRep for this object.
     * @param s the initial string rep for this object.
     */
    protected TclObjectBase(TclString rep, String s) {
	if (rep == null) {
	    throw new TclRuntimeError("null InternalRep");
	}
	internalRep = rep;
	stringRep = s;
	refCount = 0;

	if (TclObjectBase.saveObjRecords) {
	    String key = "TclObject";
	    Integer num = (Integer) TclObject.objRecordMap.get(key);
	    if (num == null) {
	        num = new Integer(1);
	    } else {
	        num = new Integer(num.intValue() + 1);
	    }
	    TclObject.objRecordMap.put(key, num);
	}
    }

    /**
     * Returns the handle to the current internal rep. This method should be
     * called only by an InternalRep implementation.
     *
     * @return the handle to the current internal rep.
     */
    public final InternalRep getInternalRep() {
	if (extraRefCountChecks) {
	    if (internalRep == null) {
	        disposedError();
	    }
        }

	return internalRep;
    }

    /**
     * Change the internal rep of the object. The old internal rep
     * will be deallocated as a result. This method should be
     * called only by an InternalRep implementation.
     *
     * @param rep the new internal rep.
     */
    public void setInternalRep(InternalRep rep) {
	if (internalRep == null) {
	    disposedError();
	}
	if (rep == null) {
	    throw new TclRuntimeError("null InternalRep");
	}
	if (rep == internalRep) {
	    return;
	}

        //System.out.println("TclObject setInternalRep for \"" + stringRep + "\"");
        //System.out.println("from \"" + internalRep.getClass().getName() +
        //    "\" to \"" + rep.getClass().getName() + "\"");
	internalRep.dispose();
	internalRep = rep;
    }

    /**
     * Returns the string representation of the object.
     *
     * @return the string representation of the object.
     */

    public /*final*/ String toString() {
        // FIXME: This method is not final because of a bug
        // in pizza and janino having to do with a method
        // declared in an inaccessble base class.
	if (extraRefCountChecks) {
	    if (internalRep == null) {
	        disposedError();
	    }
        }

	if (internalRep == null) {
	    disposedError();
	}
	if (stringRep == null) {
	    stringRep = internalRep.toString();
	}
	return stringRep;
    }

    /**
     * Sets the string representation of the object to null.  Next
     * time when toString() is called, getInternalRep().toString() will
     * be called. This method should be called ONLY when an InternalRep
     * is about to modify the value of a TclObject.
     *
     * @exception TclRuntimeError if object is not exclusively owned.
     */
    public final void invalidateStringRep() throws TclRuntimeError {
	if (internalRep == null) {
	    disposedError();
	}
	if (refCount > 1) {
	    throw new TclRuntimeError("string representation of object \"" +
		    toString() + "\" cannot be invalidated: refCount = " +
		    refCount);
	}
	stringRep = null;
    }

    /**
     * Returns true if the TclObject is shared, false otherwise.
     *
     */
    public final boolean isShared() {
	if (extraRefCountChecks) {
	    if (internalRep == null) {
	        disposedError();
	    }
        }

	return (refCount > 1);
    }

    /**
     * Tcl_DuplicateObj -> duplicate
     *
     * Duplicate a TclObject, this method provides the preferred
     * means to deal with modification of a shared TclObject.
     * It should be invoked in conjunction with isShared instead
     * of using the deprecated takeExclusive method.
     *
     * Example:
     *
     *		if (tobj.isShared()) {
     *		    tobj = tobj.duplicate();
     *		}
     *		TclString.append(tobj, "hello");
     *
     * @return an TclObject with a refCount of 0.
     */

    public final TclObject duplicate() {
	if (internalRep == null) {
	    disposedError();
	}
	if (internalRep instanceof TclString) {
	    if (stringRep == null) {
	        stringRep = internalRep.toString();
	    }
	}
	TclObject newObj = new TclObject(internalRep.duplicate());
	newObj.stringRep = this.stringRep;
	newObj.refCount = 0;
	return newObj;
    }

    /**
     * @deprecated The takeExclusive method has been deprecated
     * in favor of the new duplicate() method. The takeExclusive
     * method would modify the ref count of the original object
     * and return an object with a ref count of 1 instead of 0.
     * These two behaviors lead to lots of useless duplication
     * of objects that could be modified directly.
     */

    public final TclObject takeExclusive() throws TclRuntimeError {
	if (internalRep == null) {
	    disposedError();
	}
	if (refCount == 1) {
	    return (TclObject) this;
	} else if (refCount > 1) {
	    if (internalRep instanceof TclString) {
		if (stringRep == null) {
		    stringRep = internalRep.toString();
		}
	    }
	    TclObject newObj = new TclObject(internalRep.duplicate());
	    newObj.stringRep = this.stringRep;
	    newObj.refCount = 1;
	    refCount--;
	    return newObj;
	} else {
	    throw new TclRuntimeError("takeExclusive() called on object \"" +
		    toString() + "\" with: refCount = 0");
	}
    }

    /**
     * Returns the refCount of this object.
     *
     * @return refCount.
     */
    final int getRefCount() {
	return refCount;
    }

    /**
     * Dispose of the TclObject when the refCount reaches 0.
     *
     */
    protected final void disposeObject() {
	internalRep.dispose();

	// Setting the internalRep to null means any further
	// use of the object will generate an error.

	internalRep = null;
	stringRep = null;
    }

    /**
     * Raise a TclRuntimeError in the case where a
     * TclObject was disposed of because the last
     * ref was released.
     */

    protected final void disposedError() {
	throw new TclRuntimeError("TclObject has been deallocated");
    }

    /**
     * Return a String that describes TclObject and internal
     * rep type allocations and conversions. The string is
     * in lines separated by newlines. The saveObjRecords
     * needs to be set to true and Jacl recompiled for
     * this method to return a useful value.
     */

    static String getObjRecords() {
        if (TclObjectBase.saveObjRecords) {
            StringBuffer sb = new StringBuffer(64);
            for ( Enumeration keys = TclObject.objRecordMap.keys() ;
                    keys.hasMoreElements() ; ) {
                String key = (String) keys.nextElement();
                Integer num = (Integer) TclObject.objRecordMap.get(key);
                sb.append(key);
                sb.append(" ");
                sb.append(num.intValue());
                sb.append("\n");
            }
            TclObject.objRecordMap = new Hashtable();
            return sb.toString();
        } else {
            return "";
        }
    }

    // Return true if this TclObject has no string rep.
    // This could be the case with a "pure" integer
    // or double, or boolean, that was created from
    // a primitive value. Once the toString() method
    // has been invoked, this method will return true
    // unless the string rep is again invalidated.

    final boolean hasNoStringRep() {
        return (stringRep == null);
    }
}

