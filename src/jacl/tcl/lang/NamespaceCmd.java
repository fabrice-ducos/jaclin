/*
 * NamespaceCmd.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: NamespaceCmd.java,v 1.2.1.1 1999/01/29 20:52:09 mo Exp $
 */

package tcl.lang;

/**
 * This class implements the built-in "namespace" command in Tcl.
 */

class NamespaceCmd implements Command {
    /**
     * This procedure is invoked to process the "namespace" Tcl command.
     * See the user documentation for details on what it does.
     *
     * @param interp the current interpreter.
     * @param argv command arguments.
     * @exception TclException if error occurs.
     */

    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {

	// unimplemented.	 

	throw new TclException(interp, "Namespace command is not yet implemented");
    }
}
