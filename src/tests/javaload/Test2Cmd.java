/*
 *  Test2.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id$
 *
 */

import tcl.lang.*;

public class
Test2Cmd implements Command {
    public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
	interp.setResult("test works");
    }
}

