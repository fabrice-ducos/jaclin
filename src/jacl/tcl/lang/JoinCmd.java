/*
 * JoinCmd.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id$
 *
 */

package tcl.lang;

/**
 * This class implements the built-in "join" command in Tcl.
 */
class JoinCmd implements Command {

    /**
     * See Tcl user documentation for details.
     */
    public void cmdProc(Interp interp, TclObject argv[])
	    throws TclException {
	String sep = null;

	if (argv.length == 2) {
	    sep = null;
	} else if (argv.length == 3) {
	    sep = argv[2].toString();
	} else {
	    throw new TclNumArgsException(interp, 1, argv, 
		    "list ?joinString?");
	}
	TclObject list = argv[1];
	int size = TclList.getLength(interp, list);

	if (size == 0) {
	    interp.resetResult();
	    return;
	}

	StringBuffer sbuf = new
	        StringBuffer(TclList.index(interp, list, 0).toString());

	for (int i=1; i<size; i++) {
	    if (sep == null) {
		sbuf.append(' ');
	    } else {
		sbuf.append(sep);
	    }
	    sbuf.append(TclList.index(interp, list, i).toString());
	}
	interp.setResult(sbuf.toString());
    }
}

