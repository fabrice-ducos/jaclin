/*
 * MyClass --
 *
 *	This file tests the tcl.lang.reflect.PkgInvoker class. Please
 *	see the comments in PkgInvoker.java for details.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id: MyClass.java,v 1.2.1.1 1999/01/29 20:52:09 mo Exp $
 *
 */

package pkg2;

public class MyClass {

int someMethod()
{
    return 1234;
}

}

class ProtectedClass {

public ProtectedClass()
{
}

int someMethod()
{
    return 1234;
}

}

