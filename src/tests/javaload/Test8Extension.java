/*
 * Test8Extension.java
 *
 *    Nothing new, this is to test env(LOAD)
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
Test8Extension extends Extension {
    private Test8Extension() {}
    
    public void init(Interp interp) {
	interp.createCommand("test2", new Test2Cmd());
    }
}

