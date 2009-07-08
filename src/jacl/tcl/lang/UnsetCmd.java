/*
 * UnsetCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: UnsetCmd.java,v 1.2 1999/07/28 03:28:52 mo Exp $
 *
 */

package tcl.lang;

import java.util.*;

/**
 * This class implements the built-in "unset" command in Tcl.
 */

class UnsetCmd implements Command {
	/**
	 * Tcl_UnsetObjCmd -> UnsetCmd.cmdProc
	 * 
	 * Unsets Tcl variable (s). See Tcl user documentation * for details.
	 * 
	 * @exception TclException
	 *                If tries to unset a variable that does not exist.
	 */

	public void cmdProc(Interp interp, TclObject[] objv) throws TclException {
		int firstArg = 1;
		String opt;
		boolean noComplain = false;
		
		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv,
					"?-nocomplain? ?--? ?varName varName ...?");
		}
	
		 /*
	     * Simple, restrictive argument parsing.  The only options are --
	     * and -nocomplain (which must come first and be given exactly to
	     * be an option).
	     */
		
		opt = objv[firstArg].toString();

		if (opt.startsWith("-")) {
			if ("-nocomplain".equals(opt)) {
				noComplain = true;
				opt = objv[++firstArg].toString();
			} 
			if ("--".equals(opt)) {
				firstArg++;
			}
		}
		for (int i = firstArg; i < objv.length; i++) {
			try {
				interp.unsetVar(objv[i], noComplain ? 0 : TCL.LEAVE_ERR_MSG);
			} catch (TclException e) {
				if (!noComplain) {
					throw e;
				} else {
					interp.resetResult();
				}
			}
		}

		return;
	}
}
