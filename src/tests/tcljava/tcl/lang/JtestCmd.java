/*
 * JtestCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: JtestCmd.java,v 1.3 2002/12/25 08:29:31 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.*;

/**
 * This class implements the built-in "Jtest" command in Tcl. This
 * command is used mainly for debug purposes. E.g., verify whether the
 * refCount is maintained properly.
 */

class JtestCmd implements Command {
    static final private String validCmds[] = {
	"compcode",
	"equal",
	"gc",
	"getobject",
	"refcount",
	"type",
    };

    static final private int OPT_COMPCODE 	= 0;
    static final private int OPT_EQUAL 		= 1;
    static final private int OPT_GC		= 2;
    static final private int OPT_GETOBJECT	= 3;
    static final private int OPT_REFCOUNT	= 4;
    static final private int OPT_TYPE 		= 5;

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	if (argv.length < 2) {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "option ?arg arg ...?");
	}
	int opt = TclIndex.get(interp, argv[1], validCmds, "option", 0);

	switch (opt) {
	case OPT_COMPCODE:
	    /*
	     * Returns a TclException completion code, or ""
	     */
	    if (argv.length != 3) {
		throw new TclException(interp, "wrong # args: should be \"" +
			argv[0] + " compcode script\"");
	    }

	    TclObject obj = argv[2];
	    obj.preserve();
	    try {
	        interp.eval(obj, TCL.EVAL_GLOBAL);
	    } catch (TclException e) {
	        interp.setResult(e.getCompletionCode());
	    } finally {
	        obj.release();
	    }
	    break;

	case OPT_EQUAL:
	    /*
	     * Returns if the two objects refer to the same Java object.
	     */
	    if (argv.length != 4) {
		throw new TclException(interp, "wrong # args: should be \"" +
			argv[0] + " equal object1 object2\"");
	    }

	    TclObject obj1 = argv[2];
	    TclObject obj2 = argv[3];

	    interp.setResult(obj1 == obj2);
	    break;

	case OPT_GC:
	    System.gc();
	    break;

	case OPT_GETOBJECT:
	    /*
	     * Wraps a TclObject into a ReflectObject so that
	     * it can be passed to methods that take TclObject's.
	     */

	    if (argv.length != 3) {
		throw new TclNumArgsException(interp, 2, argv, 
		        "tclvalue");
	    }
	    interp.setResult(ReflectObject.newInstance(interp,
					  TclObject.class, argv[2]));
	    break;

	case OPT_REFCOUNT:
	    /*
	     * Returns the reference count of an object.
	     * E.g. jtest refcount $obj
	     */
	    if (argv.length != 3) {
		throw new TclException(interp, "wrong # args: should be \"" +
			argv[0] + " type object\"");
	    }

	    TclObject o = argv[2];

	    /*
	     * The following script will return 1
	     *
	     *		set obj [java::new Object]
	     *		jtest refcount $obj
	     *
	     * Subtract 1 from the returned refCount to account for
	     * the reference added by the 3rd argument to jtest.
	     */	    
	    interp.setResult(o.getRefCount()-1);
	    break;

	case OPT_TYPE:
	    /*
	     * Returns the Java class name of an object.
	     * E.g. info type $a
	     */
	    if (argv.length != 3) {
		throw new TclException(interp, "wrong # args: should be \"" +
			argv[0] + " type object\"");
	    }

	    interp.setResult(TclString.newInstance(
		    argv[2].getInternalRep().getClass().getName()));
	    break;
	}
    }
}

