/*
 * Interp.java
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1998 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Interp.java,v 1.23 2002/12/29 03:20:46 mdejong Exp $
 *
 */

package tcl.lang;

import java.util.*;
import java.io.*;
import java.net.*;

/*
 * The Tcl interpreter class.
 */
public class Interp {


// Initialize the Interp class by loading the native methods.

static {
    String shlibname = "tclblend";

    try {
        System.loadLibrary(shlibname);
    } catch (UnsatisfiedLinkError e) {
        System.out.println("System.loadLibrary(\"" + shlibname + "\") failed because of UnsatisfiedLinkError");
        e.printStackTrace(System.out);
    } catch (Throwable t) {
        System.out.println("System.loadLibrary(\"" + shlibname + "\") failed because of Unknown Throwable");
        t.printStackTrace(System.out);
    }
}


// The interpPtr contains the C Tcl_Interp* used in native code.  This
// field is declared with package visibility so that it can be passed
// to native methods by other classes in this package.

long interpPtr;


// The following three variables are used to maintain a translation
// table between ReflectObject's and their string names. These
// variables are accessed by the ReflectObject class, they
// are defined here be cause we need them to be per interp data.

// Translates Object to ReflectObject. This makes sure we have only
// one ReflectObject internalRep for the same Object -- this
// way Object identity can be done by string comparison.

Hashtable reflectObjTable = new Hashtable();

// Number of reflect objects created so far inside this Interp
// (including those that have be freed)

long reflectObjCount = 0;

// Table used to store reflect hash index conflicts, see
// ReflectObject implementation for more details

Hashtable reflectConflictTable = new Hashtable();

// The Notifier associated with this Interp.

private Notifier notifier;

// Hash table for associating data with this interpreter. Cleaned up
// when this interpreter is deleted.

Hashtable assocDataTab;

// Used ONLY by JavaImportCmd
Hashtable[] importTable = {new Hashtable(), new Hashtable()};

// Used ONLY by CObject
Vector cobjCleanup = new Vector();

// True when callCommand should propagate exceptions
boolean propagateException = false;


/*
 *----------------------------------------------------------------------
 *
 * Interp --
 *
 *	Create a new Interp to wrap an existing C Tcl_Interp.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private
Interp(
    long l)			// Pointer to Tcl_Interp.
{
    interpPtr = l;

    notifier = Notifier.getNotifierForThread(Thread.currentThread());
    notifier.preserve();
}

/*
 *----------------------------------------------------------------------
 *
 * Interp --
 *
 *	Create a new Tcl interpreter.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Calls init() on the new interpreter.  If init() fails,
 *	disposes of the interpreter.
 *
 *----------------------------------------------------------------------
 */

public
Interp()
{
    interpPtr = create();

    notifier = Notifier.getNotifierForThread(Thread.currentThread());
    notifier.preserve();

    if (init(interpPtr) != TCL.OK) {
	String result = getResult().toString();
	dispose();
	throw new TclRuntimeError(result);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * create --
 *
 *	Call Tcl_CreateInterp to initialize a new interpreter.
 *
 * Results:
 *	Returns a new Tcl_Interp *.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private final native long
create();

/*
 *----------------------------------------------------------------------
 *
 * dispose --
 *
 *	This method cleans up the state of the interpreter so that
 *	it can be garbage collected safely.  This routine needs to
 *	break any circular references that might keep the interpreter
 *	alive indefinitely.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Cleans up the interpreter.
 *
 *----------------------------------------------------------------------
 */

public void
dispose()
{
    // Remove all the assoc data tied to this interp.
	
    if (assocDataTab != null) {
	for (Enumeration e = assocDataTab.keys(); e.hasMoreElements();) {
	    Object key = e.nextElement();
	    AssocData data = (AssocData) assocDataTab.get(key);
	    data.disposeAssocData(this);
	    assocDataTab.remove(key);
	}
	assocDataTab = null;
    }

    // Release the notifier.

    if (notifier != null) {
	notifier.release();
	notifier = null;
    }

    // Clean up the C state.

    if (interpPtr != 0) {
	doDispose(interpPtr);
	interpPtr = 0;
    }

    // See if we need to cleanup this Java thread
    Notifier.finalizeThreadCheck();
}

/*
 *----------------------------------------------------------------------
 *
 * finalize --
 *
 *	Interpreter finalization method. We print a message to
 *	stderr if the user neglected to dispose of an Interp
 *	properly. We can't call dispose here because the
 *	finalize method is called from the gc thread and
 *	Tcl thread specific data needs to be cleaned up
 *	in the thread it was allocated in.
 *
 * Results:
 *	Prints to stderr.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

protected void
finalize() throws Throwable
{
    if (notifier != null) {
        System.err.println("finalized interp has not been disposed");
    }
    super.finalize();
}

/*
 *----------------------------------------------------------------------
 *
 * getWorkingDir --
 *
 *	Retrieve the current working directory for this interpreter.
 *
 * Results:
 *	Returns the File for the directory.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

File
getWorkingDir()
{
    return new File(Util.getCwd());
}

/*
 *----------------------------------------------------------------------
 *
 * setVar --
 *
 *	Set the value of a variable.
 *
 * Results:
 *	Returns the new value of the variable.
 *
 * Side effects:
 *	May trigger traces.
 *
 *----------------------------------------------------------------------
 */

public final native TclObject
setVar(
    String name1,		// If name2 is null, this is name of a scalar
				// variable. Otherwise it is the name of an
				// array. 
    String name2,		// Name of an element within an array, or
				// null.
    TclObject value,		// New value for variable.
    int flags)			// Various flags that tell how to set value:
				// any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
				// TCL.APPEND_VALUE, TCL.LIST_ELEMENT, TCL.LEAVE_ERR_MSG,
				// or TCL.PARSE_PART1. 
throws
    TclException;

/*
 *----------------------------------------------------------------------
 *
 * setVar --
 *
 *	Set the value of a variable.
 *
 * Results:
 *	Returns the new value of the variable.
 *
 * Side effects:
 *	May trigger traces.
 *
 *----------------------------------------------------------------------
 */

public final TclObject
setVar(
    String name,		// Name of variable, array, or array element
				// to set.
    TclObject value,		// New value for variable.
    int flags)			// Various flags that tell how to set value:
				// any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
				// TCL.APPEND_VALUE, TCL.LIST_ELEMENT, or
				// TCL.LEAVE_ERR_MSG. 
throws
    TclException
{
    return setVar(name, null, value, (flags | TCL.PARSE_PART1));
}

/*
 *----------------------------------------------------------------------
 *
 * getVar --
 *
 *	Get the value of a variable.
 *
 * Results:
 *	Returns the value of the variable.
 *
 * Side effects:
 *	May trigger traces.
 *
 *----------------------------------------------------------------------
 */

public final native TclObject
getVar(
    String name1,		// If name2 is null, this is name of a scalar
				// variable. Otherwise it is the name of an
				// array. 
    String name2,		// Name of an element within an array, or
				// null.
    int flags)			// Various flags that tell how to get value:
				// any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
				// TCL.LEAVE_ERR_MSG, or TCL.PARSE_PART1. 
throws
    TclException;

/*
 *----------------------------------------------------------------------
 *
 * getVar --
 *
 *	Get the value of a variable.
 *
 * Results:
 *	Returns the value of the variable.
 *
 * Side effects:
 *	May trigger traces.
 *
 *----------------------------------------------------------------------
 */

public final TclObject
getVar(
    String name,		// The name of a variable, array, or array
				// element.
    int flags)			// Various flags that tell how to get value:
				// any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
				// or TCL.LEAVE_ERR_MSG.
throws TclException
{
    return getVar(name, null, (flags | TCL.PARSE_PART1));
}    

/*
 *----------------------------------------------------------------------
 *
 * unsetVar --
 *
 *	Unset a variable.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	May trigger traces.
 *
 *----------------------------------------------------------------------
 */

public final void
unsetVar(
    String name,		// The name of a variable, array, or array
				// element.
    int flags)			// Various flags that tell how to get value:
				// any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
				// or TCL.LEAVE_ERR_MSG.
throws
    TclException
{
    unsetVar(name, null, (flags | TCL.PARSE_PART1));
}

/*
 *----------------------------------------------------------------------
 *
 * unsetVar --
 *
 *	Unset a variable.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	May trigger traces.
 *
 *----------------------------------------------------------------------
 */

public final native void
unsetVar(
    String name1,		// If name2 is null, this is name of a scalar
				// variable. Otherwise it is the name of an
				// array. 
    String name2,		// Name of an element within an array, or
				// null.
    int flags)			// Various flags that tell how to get value:
				// any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
				// TCL.LEAVE_ERR_MSG, or TCL.PARSE_PART1. 
throws
    TclException;

/*
 *----------------------------------------------------------------------
 *
 * traceVar --
 *
 *	Add a trace to a variable.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void
traceVar(
    String name,		// Name of variable;  may end with "(index)"
				// to signify an array reference.
    VarTrace trace,		// Object to notify when specified ops are
				// invoked upon varName.
    int flags)			// OR-ed collection of bits, including any
				// of TCL.TRACE_READS, TCL.TRACE_WRITES,
				// TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY,
				// TCL.NAMESPACE_ONLY.
throws
    TclException
{
    traceVar(name, null, trace, (flags | TCL.PARSE_PART1));
}

/*
 *----------------------------------------------------------------------
 *
 * traceVar --
 *
 *	Add a trace to a variable.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public native void
traceVar(
    String part1,		// Name of scalar variable or array.
    String part2,		// Name of element within array;  null means
				// trace applies to scalar variable or array
				// as-a-whole.  
    VarTrace trace,		// Object to notify when specified ops are
				// invoked upon varName.
    int flags)			// OR-ed collection of bits, including any
				// of TCL.TRACE_READS, TCL.TRACE_WRITES,
				// TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY,
				// TCL.NAMESPACE_ONLY and
				// TCL.PARSE_PART1.
throws
    TclException;		// If variable doesn't exist.

/*
 *----------------------------------------------------------------------
 *
 * untraceVar --
 *
 *	Remove a trace from a variable.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void
untraceVar(
    String name,		// Name of variable;  may end with "(index)"
				// to signify an array reference.
    VarTrace trace,		// Object associated with trace.
    int flags)			// OR-ed collection of bits describing current
				// trace, including any of TCL.TRACE_READS,
				// TCL.TRACE_WRITES, TCL.TRACE_UNSETS,
				// TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY and
				// TCL.PARSE_PART1. 
throws
    TclException
{
    untraceVar(name, null, trace, (flags | TCL.PARSE_PART1));
}

/*
 *----------------------------------------------------------------------
 *
 * untraceVar --
 *
 *	Remove a trace from a variable.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public native void
untraceVar(
    String part1,		// Name of scalar variable or array.
    String part2,		// Name of element within array;  null means
				// trace applies to scalar variable or array
				// as-a-whole.  
    VarTrace trace,		// Object associated with trace.
    int flags)			// OR-ed collection of bits describing current
				// trace, including any of TCL.TRACE_READS,
				// TCL.TRACE_WRITES, TCL.TRACE_UNSETS,
				// TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY and
				// TCL.PARSE_PART1. 
throws
    TclException;

/*
 *----------------------------------------------------------------------
 *
 * createCommand --
 *
 *	Create a new Tcl command that is implemented by a Java object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public native void
createCommand(
    String name,		// Name of new command.
    Command cmd);		// Object that implements the command.

/*
 *----------------------------------------------------------------------
 *
 * deleteCommand --
 *
 *	Remove a command from the interpreter.
 *
 * Results:
 *	Returns 0 if the command was deleted successfully, else -1.
 *
 * Side effects:
 *	May invoke the disposeCmd() method on the Command object.
 *
 *----------------------------------------------------------------------
 */

public native int
deleteCommand(
    String name);		// Name of command to delete.

/*
 *----------------------------------------------------------------------
 *
 * getCommand --
 *
 *	Returns the command procedure of the given command.
 *
 * Results:
 *	The command procedure of the given command, or null if
 *      the command doesn't exist.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public native Command
getCommand(
    String name); 		// String name of the command.

/*
 *----------------------------------------------------------------------
 *
 * commandComplete --
 *
 *	Tests if the String is a complete command.
 *
 * Results:
 *	Boolean value.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public static native boolean
commandComplete(
    String cmd);	// Complete or partially complete command

/*
 *----------------------------------------------------------------------
 *
 * getResult --
 *
 *	Retrieve the result of the last interpreter action.
 *
 * Results:
 *	The result object.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public final native TclObject
getResult();

/*
 *----------------------------------------------------------------------
 *
 * setResult --
 *
 *	Set the interpreter result to the given TclObject.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public final native void
setResult(
    TclObject r);		// New result object.

/*
 *----------------------------------------------------------------------
 *
 * setResult --
 *
 *	These routines are convenience wrappers that accept
 *	commonly used Java types and set the interpreter result.
 *	Some create a TclObject type wrapper before setting
 *	the result to this object. Others use native code to
 *	set the interp result directly in C code instead of
 *	creating a TclObject wrapper, since a wrapper
 *	involves a non-trivial amount of overhead.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public final void
setResult(
    String r)			// String to use as result.
{
    setResult(TclString.newInstance(r));
}

public final void
setResult(
    double r)			// Double to use as result.
{
    setResult(TclDouble.newInstance(r));
}

public final native void
setResult(
    int r);			// int to use as result.

public final native void
setResult(
    boolean r);			// boolean to use as result.


/*
 *----------------------------------------------------------------------
 *
 * resetResult --
 *
 *	Clears the interpreter result.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public final native void
resetResult();

/*
 *----------------------------------------------------------------------
 *
 * eval --
 *
 *	Execute a Tcl command in a string or Tcl Object.
 *
 * Results:
 *	The return value is void.  However, a standard Tcl Exception
 *	may be generated.  The interpreter's result object will contain
 *	the value of the evaluation but will persist only until the next 
 *	call to one of the eval functions.
 *
 * Side effects:
 *	The side effects will be determined by the exact Tcl code to be 
 *	evaluated.
 *
 *----------------------------------------------------------------------
 */

private native void
evalNative(
    String script,	// A script to evaluate.
    int flags)		// Flags, either 0 or TCL.EVAL_GLOBAL.
throws 
    TclException; 	// A standard Tcl exception.


public void 
eval(
    String script,	// A script to evaluate.
    int flags)		// Flags, either 0 or TCL.EVAL_GLOBAL.
throws 
    TclException 	// A standard Tcl exception.
{
    boolean held = false;
    if (!propagateException) {
        propagateException = true;
        held = true;
    }
    try {
        evalNative(script, flags);
    } finally {
        if (held) {
            if (propagateException == false)
                throw new TclRuntimeError("propagateException was false");
            propagateException = false;
        }
    }
}


public void 
eval(
    String script)	// A script to evaluate.
throws 
    TclException 	// A standard Tcl exception.
{
    eval(script, 0);
}


public void 
eval(
    TclObject tobj,	// A Tcl object holding a script to evaluate.
    int flags)		// Flags, either 0 or TCL.EVAL_GLOBAL.
throws 
    TclException 	// A standard Tcl exception.
{
    eval(tobj.toString(), flags);
}

/*
 *----------------------------------------------------------------------
 *
 * Tcl_RecordAndEvalObj -> recordAndEval
 *
 *	This procedure adds its command argument to the current list of
 *	recorded events and then executes the command by calling eval.
 *
 * Results:
 *	The return value is void.  However, a standard Tcl Exception
 *	may be generated.  The interpreter's result object will contain
 *	the value of the evaluation but will persist only until the next 
 *	call to one of the eval functions.
 *
 * Side effects:
 *	The side effects will be determined by the exact Tcl code to be 
 *	evaluated.
 *
 *----------------------------------------------------------------------
 */

public void 
recordAndEval(
    TclObject script,	// A script to evaluate.
    int flags)		// Flags, either 0 or TCL.EVAL_GLOBAL.
throws 
    TclException 	// A standard Tcl exception.
{
    // FIXME : need native implementation
    throw new TclRuntimeError("Not implemented yet.");
}

/*
 *----------------------------------------------------------------------
 *
 * evalFile --
 *	Loads a Tcl script from a file and evaluates it in the
 * 	current interpreter.
 *
 * Results:
 * 	None.
 *
 * Side effects:
 *	The side effects will be determined by the exact Tcl code to be 
 *	evaluated.
 *
 *----------------------------------------------------------------------
 */

public void
evalFile(
    String s)			// The name of file to evaluate.
throws 
    TclException
{
    // FIXME : need implementation
    throw new TclRuntimeError("Not implemented yet.");
}

/*
 *----------------------------------------------------------------------
 *
 * evalResource --
 *
 *	Execute a Tcl script stored in the given Java resource location.
 *
 * Results:
 *	The return value is void.  However, a standard Tcl Exception
 *	may be generated. The interpreter's result object will contain
 *	the value of the evaluation but will persist only until the next 
 *	call to one of the eval functions.
 *
 * Side effects:
 *	The side effects will be determined by the exact Tcl code to be 
 *	evaluated.
 *
 *----------------------------------------------------------------------
 */

void 
evalResource(
    String resName) 	// The location of the Java resource. See
			// the Java documentation of
			// Class.getResourceAsStream()
			// for details on resources naming.
throws 
    TclException
{
    InputStream stream = Interp.class.getResourceAsStream(resName);
    if (stream == null) {
	throw new TclException(this, "cannot read resource \"" + resName
		+ "\"");
    }

    try {

	// FIXME : ugly JDK 1.2 only hack
	// Ugly workaround for compressed files BUG in JDK1.2
        // this bug first showed up in  JDK1.2beta4. I have sent
        // a number of emails to Sun but they have deemed this a "feature"
        // of 1.2. This is flat out wrong but I do not seem to change thier
        // minds. Because of this, there is no way to do non blocking IO
        // on a compressed Stream in Java. (mo)

        if (System.getProperty("java.version").startsWith("1.2") &&
            stream.getClass().getName().equals("java.util.zip.ZipFile$1")) {
	    
	  ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
	  byte[] buffer = new byte[1024];
	  int numRead;

	  // Read all data from the stream into a resizable buffer
	  while ((numRead = stream.read(buffer, 0, buffer.length)) != -1) {
	      baos.write(buffer, 0, numRead);
	  }

	  // Convert bytes into a String and eval them
	  eval(new String(baos.toByteArray()), 0);	  
	  
	} else {	  
	  // Other systems do not need the compressed jar hack

	  int num = stream.available();
	  byte[] byteArray = new byte[num];
	  int offset = 0;
	  while ( num > 0 ) {
	    int readLen = stream.read( byteArray, offset, num );
	    offset += readLen;
	    num -= readLen;
	  }

	  eval(new String(byteArray), 0);
	}

    } catch (IOException e) {
	return;
    } finally {
	closeInputStream(stream);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * closeInputStream --
 *
 *	Close the InputStream; catch any IOExceptions and ignore them.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private void
closeInputStream(
    InputStream fs)
{
    try {
	fs.close();
    }
    catch (IOException e) {}
}

/*
 *----------------------------------------------------------------------
 *
 * callCommand --
 *
 *	Invoke a Tcl command object and deal with any errors that result.
 *	This method may or may not let the exceptions propagate up
 *	to the caller, based on the propagateException flag. When
 *	invoked from Tcl, we would not want to leave a Java
 *	exception pending. When invoked from Java, we do want to
 *	propagate the exception up to the caller. This method is
 *	only ever invoked from function JavaCmdProc.
 *
 * Results:
 *	Returns the result code.
 *
 * Side effects:
 *	Whatever the command does.
 *
 *----------------------------------------------------------------------
 */

private int
callCommand(
    Command cmd,		// Command to invoke.
    TclObject argv[])		// Argument array for command.
        throws TclException
{
    try {
	CObject.cleanupPush(this);
	cmd.cmdProc(this, argv);
	return TCL.OK;
    } catch (TclException e) {
	if (propagateException)
	    throw e;
	else
	    return e.getCompletionCode();
    } catch (RuntimeException e) {
	// This should not happen, if it does it means there is
	// a bug somewhere in the implementation of a command.
	if (propagateException)
	    throw e;
	} else {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
	    PrintStream ps = new PrintStream(baos);
	    ps.println("RuntimeException in Java command implementation");
	    e.printStackTrace(ps);
	    setResult(baos.toString());
	    return TCL.ERROR;
	}
    } finally {
	CObject.cleanupPop(this);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * setErrorCode --
 *
 *	These functions set the errorCode variable in the interpreter
 *	to the given value.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Sets the interpreter error state so the interpreter doesn't
 *	set errorCode to NONE after the current eval returns.
 *
 *----------------------------------------------------------------------
 */

public native void
setErrorCode(
    TclObject code);

/*
 *----------------------------------------------------------------------
 *
 * addErrorInfo --
 *
 *	This function adds the given string to the errorInfo variable.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public native void
addErrorInfo(
    String message);		// Message to add to errorInfo

/*
 *----------------------------------------------------------------------
 *
 * backgroundError --
 *
 *	This procedure is invoked to handle errors that occur in Tcl
 *	commands that are invoked in "background" (e.g. from event or
 *	timer bindings).
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The command "bgerror" is invoked later as an idle handler to
 *	process the error, passing it the error message.  If that fails,
 *	then an error message is output on stderr.
 *
 *----------------------------------------------------------------------
 */

public native void
backgroundError();

/*
 *----------------------------------------------------------------------
 *
 * getNotifier --
 *
 *	Retrieve the Notifier associated with this Interp.
 *
 * Results:
 *	Returns the Notifier.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public Notifier
getNotifier()
{
    return notifier;
}

/*
 *----------------------------------------------------------------------
 *
 * setAssocData --
 *
 *	Creates a named association between user-specified data and
 *	this interpreter.  If the association already exists the
 *	olddata is overwritten with the new data. The
 *	data.deleteAssocData() method will be invoked if the
 *	interpreter is deleted before the association is deleted.
 *
 *	NOTE: deleteAssocData() is not called when old data is
 *	replaced by new data.  The caller of setAssocData() is
 *	responsible for deleting the old data.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Initializes the assocDataTab if necessary.
 *
 *----------------------------------------------------------------------
 */

public void
setAssocData(
    String name,		// Name for association.
    AssocData data)		// Object associated with the name.
{
    if (assocDataTab == null) {
	assocDataTab = new Hashtable();
    }
    assocDataTab.put(name, data);
}

/*
 *----------------------------------------------------------------------
 *
 * deleteAssocData --
 *
 *	Deletes a named association of user-specified data with
 *	the specified interpreter.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public void
deleteAssocData(
    String name)		// Name of association.
{
    if (assocDataTab == null) {
	return;
    }

    assocDataTab.remove(name);
}

/*
 *----------------------------------------------------------------------
 *
 * getAssocData --
 *
 *	Returns the AssocData instance associated with this name in
 *	the specified interpreter.
 *
 * Results:
 *	The AssocData instance in the AssocData record denoted by the
 *	named association, or null.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public AssocData
getAssocData(
    String name)			// Name of association.
{
    if (assocDataTab == null) {
	return null;
    } else {
	return (AssocData) assocDataTab.get(name);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * init --
 *
 *	Call the init methods on an interpreter pointer.
 *
 * Results:
 *	Returns TCL.OK if the intialization succeeded, else TCL.ERROR.
 *
 * Side effects:
 *	Calls Tcl_Init and Java_Init. 
 *
 *----------------------------------------------------------------------
 */

private final native int
init(
    long interpPtr);		// Tcl_Interp pointer.

/*
 *----------------------------------------------------------------------
 *
 * doDispose --
 *
 *	Call Tcl_DeleteInterp on the given interpPtr.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	lots of callbacks could be invoked.
 *
 *----------------------------------------------------------------------
 */

private static final native void
doDispose(
    long interpPtr);		// Tcl_Interp pointer.

/*
 *----------------------------------------------------------------------
 *
 * pkgProvide --
 *
 *	Call Tcl_PkgProvide on the given interpPtr.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Package and version are stored in the interpPtr.
 *
 *----------------------------------------------------------------------
 */
public final native void
pkgProvide(
    String name, 
    String version);

/*
 *----------------------------------------------------------------------
 *
 * pkgRequire --
 *	Loads the package to the interpPtr.
 *
 * Results:
 *	The version number of the loaded package on success,
 *	otherwise a TclException is generated.
 *
 * Side effects:
 *	Possibly evals a script.
 *
 *----------------------------------------------------------------------
 */

public final native String
pkgRequire(
    String pkgname, 
    String version, 
    boolean exact);

/*
 *----------------------------------------------------------------------
 *
 * createBTestCommand --
 *
 *	Create a Tcl command called "btest", used for
 *	test cases and debugging Tcl Blend.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

native void
createBTestCommand();

} // end Interp

