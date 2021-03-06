/*
 * EvalCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: EvalCmd.java,v 1.2 2005/10/17 09:33:20 mdejong Exp $
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "eval" command in Tcl.
 */

class EvalCmd implements Command {
    /**
     * This procedure is invoked to process the "eval" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     * @exception TclException if script causes error.
     */

    public void cmdProc(
        Interp interp,
        TclObject[] objv)
	    throws TclException {
	if (objv.length < 2) {
	    throw new TclNumArgsException(interp, 1, objv, "arg ?arg ...?");
	}

	try {
	    if (objv.length == 2) {
		interp.eval(objv[1], 0);
	    } else {
		TclObject obj = Util.concat(1, objv.length-1, objv);
		interp.eval(obj, 0);
	    }
	} catch (TclException e) {
	    if (e.getCompletionCode() == TCL.ERROR) {
		interp.addErrorInfo("\n    (\"eval\" body line " +
				interp.errorLine + ")");
	    }
	    throw e;
	}
    }
}

