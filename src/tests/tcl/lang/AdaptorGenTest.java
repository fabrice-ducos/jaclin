/*
 * AdaptorGenTest.java --
 *
 *	Test program to obtain output of the AdaptorGen
 *	class.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 * RCS: @(#) $Id$
 */


package tcl.lang;

import java.util.*;
import java.lang.reflect.*;
import java.beans.*;
import java.io.*;

/*
 * This program can be used to test the operation of the AdaptorGen
 * class -- it saves the data generated by the AdaptorGen class into a
 * .class file, which can then be examined using tools such as javap.
 */

public class AdaptorGenTest {

public static void main(String args[])
{
    if (args.length != 4) {
	System.out.println("usage: java tcl.lang.AdaptorGenTest clsName eventSetName genClsName fileName");
	System.out.println("    clsName	 - fully qualified name of a class");
	System.out.println("    eventSetName - fully qualified name of an event listener interface");
	System.out.println("                   supported by this class");
	System.out.println("    genClsName   - fully qualified name of the generated adaptor class");
	System.out.println("    fileName     - name of the .class file to store the adaptor class");
	System.exit(0);
    }

    String clsName = args[0];
    String eventSetName = args[1];
    String genClsName = args[2];
    String fileName = args[3];

    Class cls = null;
    EventSetDescriptor desc, d[];
    AdaptorGen gen;

    try {
	cls = Class.forName(clsName);
    } catch (Exception e) {
	e.printStackTrace();
	System.exit(0);
    }	

    BeanInfo beanInfo = null;

    try {
	beanInfo = Introspector.getBeanInfo(cls);
    } catch (IntrospectionException e) {
	e.printStackTrace();
	System.exit(0);
    }

    EventSetDescriptor events[] = beanInfo.getEventSetDescriptors();
    if (events == null) {
	System.out.println("no events for class " + cls);
	System.exit(0);
    }

    desc = null;
    for (int i = 0; i < events.length; i++) {
	if (events[i].getListenerType().getName().equals(eventSetName)) {
	    desc = events[i];
	    break;
	}
    }

    if (desc != null) {
	gen = new AdaptorGen();
	byte code[] = gen.generate(desc, EventAdaptor.class,
		genClsName);
	try {
	    FileOutputStream out = new FileOutputStream(fileName);
	    out.write(code);
	    out.close();
	} catch (IOException e) {
	    e.printStackTrace();
	    System.exit(0);
	}
    } else {
	System.out.println("cannot find event set" + eventSetName);
    }
}

} // end AdaptorGenTest

