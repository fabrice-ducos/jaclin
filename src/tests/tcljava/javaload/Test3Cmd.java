/*
 *  Test3.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Test3Cmd.java,v 1.1 1999/05/10 04:08:55 dejong Exp $
 *
 */

package tests.javaload;

import tcl.lang.*;

public class
Test3Cmd implements Command {
    public void cmdProc(Interp interp, TclObject argv[]) throws TclException {
	interp.setResult("test works");
    }
}

