/*
 * Interp.java --
 *
 *	Implements the core Tcl interpreter.
 *
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1997-1998 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Interp.java,v 1.26 2000/03/02 04:08:34 mo Exp $
 *
 */

package tcl.lang;

import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * The Tcl interpreter class.
 */

public class Interp {

// The following three variables are used to maintain a translation
// table between ReflectObject's and their string names. These two
// variables are accessed by the ReflectObject class (the variables
// are here because we want one translation table per Interp).

//  Translates integer ID to ReflectObject.


Hashtable reflectIDTable = new Hashtable();

// Translates Object to ReflectObject. This makes sure we have only
// one ReflectObject internalRep for the same Object -- this
// way Object identity can be done by string comparison.

Hashtable reflectObjTable = new Hashtable();

// Number of reflect objects created so far inside this Interp
// (including those that have be freed)

long reflectObjCount = 0;

// The interpreter will execute scripts only inside this thread. All
// calls to Interp.eval() by any other thread will be routed to this
// thread.

private Thread primaryThread;

// The number of chars to copy from an offending command into error
// message.

private static final int MAX_ERR_LENGTH = 200;


// We pretend this is Tcl 8.0, patch level 0.

static final String TCL_VERSION     = "8.0";
static final String TCL_PATCH_LEVEL = "8.0";


// Total number of times a command procedure
// has been called for this interpreter.

protected int cmdCount;

// FIXME : remove later
// Table of commands for this interpreter.
//Hashtable cmdTable;

// Table of channels currently registered in this interp.

Hashtable interpChanTable;

// The Notifier associated with this Interp.

private Notifier notifier;

// Hash table for associating data with this interpreter. Cleaned up
// when this interpreter is deleted.

Hashtable assocData;

// Current working directory.

private File workingDir;

// Points to top-most in stack of all nested procedure
// invocations.  null means there are no active procedures.

CallFrame frame;

// Points to the call frame whose variables are currently in use
// (same as frame unless an "uplevel" command is being
// executed).  null means no procedure is active or "uplevel 0" is
// being exec'ed.

CallFrame varFrame;

// The interpreter's global namespace.

NamespaceCmd.Namespace globalNs;

// FIXME : does globalFrame need to be replaced by globalNs?
// Points to the global variable frame.

//CallFrame globalFrame;

// The script file currently under execution. Can be null if the
// interpreter is not evaluating any script file.

String scriptFile;

// Number of times the interp.eval() routine has been recursively
// invoked.

int nestLevel;

// Used to catch infinite loops in Parser.eval2.

int maxNestingDepth;

// Flags used when evaluating a command.

int evalFlags;

// Flags used when evaluating a command.

int flags;

// Offset of character just after last one compiled or executed
// by Parser.eval2().

int termOffset;

// The expression parser for this interp.

Expression expr;

// Used by the Expression class.  If it is equal to zero, then the 
// parser will evaluate commands and retrieve variable values from 
// the interp.

int noEval;

// Used in the Expression.java file for the 
// SrandFunction.class and RandFunction.class.
// Set to true if a seed has been set.

boolean randSeedInit;

// Used in the Expression.java file for the SrandFunction.class and
// RandFunction.class.  Stores the value of the seed.

long randSeed;

// If returnCode is TCL.ERROR, stores the errorInfo.

String errorInfo;

// If returnCode is TCL.ERROR, stores the errorCode.

String errorCode;

// Completion code to return if current procedure exits with a
// TCL_RETURN code.

protected int returnCode;

// True means the interpreter has been deleted: don't process any
// more commands for it, and destroy the structure as soon as all
// nested invocations of eval() are done.

protected boolean deleted;
 
// True means an error unwind is already in progress. False
// means a command proc has been invoked since last error occured.

protected boolean errInProgress;

// True means information has already been logged in $errorInfo
// for the current eval() instance, so eval() needn't log it
// (used to implement the "error" command).

protected boolean errAlreadyLogged;

// True means that addErrorInfo has been called to record
// information for the current error. False means Interp.eval
// must clear the errorCode variable if an error is returned.

protected boolean errCodeSet;

// When TCL_ERROR is returned, this gives the line number within
// the command where the error occurred (1 means first line).


int errorLine;

// Stores the current result in the interpreter.

private TclObject m_result;

// Value m_result is set to when resetResult() is called.

private TclObject m_nullResult;

// Used ONLY by PackageCmd.

Hashtable packageTable;
String packageUnknown;


// Used ONLY by the Parser.

TclObject[][][] parserObjv;
int[]           parserObjvUsed;

TclToken[]      parserTokens;
int             parserTokensUsed;


// Used ONLY by JavaImportCmd
Hashtable[] importTable = {new Hashtable(), new Hashtable()};



/*
 *----------------------------------------------------------------------
 *
 * Interp --
 *	Initializes an interpreter object.
 *
 * Side effects:
 *	Various parts of the interpreter are initialized; built-in
 *	commands are created; global variables are initialized, etc.
 *
 *----------------------------------------------------------------------
 */

public 
Interp() 
{

    //freeProc         = null;
    errorLine        = 0;

    // An empty result is used pretty often. We will use a shared
    // TclObject instance to represent the empty result so that we
    // don't need to create a new TclObject instance every time the
    // interpreter result is set to empty.

    m_nullResult = TclString.newInstance("");
    m_nullResult.preserve();
    m_result = m_nullResult;  // correcponds to iPtr->objResultPtr

    expr             = new Expression();
    nestLevel        = 0;
    maxNestingDepth  = 1000;

    frame            = null;
    varFrame         = null;
    
    returnCode       = TCL.OK;
    errorInfo        = null;
    errorCode        = null;

    packageTable     = new Hashtable();
    packageUnknown   = null;
    cmdCount         = 0;
    termOffset       = 0;
    //resolver       = null;
    evalFlags        = 0;
    scriptFile       = null;
    flags            = 0;
    assocData        = null;


    globalNs         = null; // force creation of global ns below
    globalNs         = NamespaceCmd.createNamespace(this, null, null);
    if (globalNs == null) {
	throw new TclRuntimeError("Interp(): can't create global namespace");
    }

    
    // Init things that are specific to the Jacl implementation

    workingDir       = new File(Util.tryGetSystemProperty("user.dir", "."));
    noEval           = 0;

    primaryThread    = Thread.currentThread();
    notifier	     = Notifier.getNotifierForThread(primaryThread);
    notifier.preserve();

    randSeedInit     = false;

    deleted          = false;
    errInProgress    = false;
    errAlreadyLogged = false;
    errCodeSet       = false;
    
    dbg              = initDebugInfo();



    // init parser variables
    Parser.init(this);
    TclParse.init(this);

    // Initialize the Global (static) channel table and the local
    // interp channel table.

    interpChanTable = TclIO.getInterpChanTable(this);

    // Sets up the variable trace for tcl_precision.

    Util.setupPrecisionTrace(this);

    // Create the built-in commands.

    createCommands();

    try {
	// Set up tcl_platform, tcl_version, tcl_library and other
	// global variables.

	setVar("tcl_platform", "platform", "java", TCL.GLOBAL_ONLY);
	setVar("tcl_platform", "byteOrder", "bigEndian", TCL.GLOBAL_ONLY);
	
	setVar("tcl_platform", "os", 
		Util.tryGetSystemProperty("os.name", "?"), TCL.GLOBAL_ONLY);
	setVar("tcl_platform", "osVersion", 
		Util.tryGetSystemProperty("os.version", "?"), TCL.GLOBAL_ONLY);
	setVar("tcl_platform", "machine", 
		Util.tryGetSystemProperty("os.arch", "?"), TCL.GLOBAL_ONLY);
	
	setVar("tcl_version", TCL_VERSION, TCL.GLOBAL_ONLY);
	setVar("tcl_patchLevel", TCL_PATCH_LEVEL, TCL.GLOBAL_ONLY);
	setVar("tcl_library", "resource:/tcl/lang/library",
		TCL.GLOBAL_ONLY);
	if (Util.isWindows()) {
	    setVar("tcl_platform", "host_platform", "windows",
		    TCL.GLOBAL_ONLY);
	} else if (Util.isMac()) {
	    setVar("tcl_platform", "host_platform", "macintosh",
		    TCL.GLOBAL_ONLY);
	} else {
	    setVar("tcl_platform", "host_platform", "unix",
		    TCL.GLOBAL_ONLY);
	}

	// Create the env array an populated it with proper
	// values.

	Env.initialize(this);

	// Register Tcl's version number. Note: This MUST be 
	// done before the call to evalResource, otherwise
	// calls to "package require tcl" will fail.
	
	pkgProvide("Tcl", TCL_VERSION);
	
	// Source the init.tcl script to initialize auto-loading.
	
	evalResource("/tcl/lang/library/init.tcl");

    } catch (TclException e) {
	System.out.println(getResult());
	e.printStackTrace();
	throw new TclRuntimeError("unexpected TclException: " + e);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * dispose --
 *
 *	This method cleans up the state of the interpreter so that
 *	it can be garbage collected safely. This routine needs to
 *	break any circular references that might keep the interpreter
 *	alive indefinitely.
 *
 *	[ToDo]
 *	The code in this method should run in an EventuallyFree proc,
 *	so that the interpreter won't be deleted until no active code
 *	depends on the interp.
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
    if (deleted) {
	return;
    }

    deleted = true;

    if (nestLevel > 0) {
        throw new TclRuntimeError("dispose() called with active evals");
    }

    // Remove our association with the notifer (if we had one).

    if (notifier != null) {
	notifier.release();
	notifier = null;
    }

    // Dismantle everything in the global namespace except for the
    // "errorInfo" and "errorCode" variables. These might be needed
    // later on if errors occur while deleting commands. We are careful
    // to destroy and recreate the "errorInfo" and "errorCode"
    // variables, in case they had any traces on them.
    //
    // Dismantle the namespace here, before we clear the assocData. If any
    // background errors occur here, they will be deleted below.

    
    // FIXME : check impl of TclTeardownNamespace
    NamespaceCmd.teardownNamespace(globalNs);

    // Delete all variables.

    TclObject errorInfoObj = null, errorCodeObj = null;

    try {
	errorInfoObj = getVar("errorInfo", null, TCL.GLOBAL_ONLY);
    } catch (TclException e) {
	// Do nothing when var does not exist.
    }
    
    if (errorInfoObj != null) {
	errorInfoObj.preserve();
    }

    try {
	errorCodeObj = getVar("errorCode", null, TCL.GLOBAL_ONLY);
    } catch (TclException e) {
	// Do nothing when var does not exist.
    }

    if (errorCodeObj != null) {
	errorCodeObj.preserve();
    }

    frame = null;
    varFrame = null;

    try {
	if (errorInfoObj != null) {
	    setVar("errorInfo", null, errorInfoObj, TCL.GLOBAL_ONLY);
	    errorInfoObj.release();
	}
	if (errorCodeObj != null) {
	    setVar("errorCode", null, errorCodeObj, TCL.GLOBAL_ONLY);
	    errorCodeObj.release();
	}
    } catch (TclException e) {
	// Ignore it -- same behavior as Tcl 8.0.
    }

    // Tear down the math function table.

    expr = null;

    // Remove all the assoc data tied to this interp and invoke
    // deletion callbacks; note that a callback can create new
    // callbacks, so we iterate.

    while (assocData != null) {
	Hashtable table = assocData;
	assocData = null;

	for (Enumeration e = table.keys(); e.hasMoreElements();) {
	    Object key = e.nextElement();
	    AssocData data = (AssocData) table.get(key);
	    data.disposeAssocData(this);
	    table.remove(key);
	}
    }

    // Finish deleting the global namespace.
    
    // FIXME : check impl of Tcl_DeleteNamespace
    NamespaceCmd.deleteNamespace(globalNs);
    globalNs = null;

    // Free up the result *after* deleting variables, since variable
    // deletion could have transferred ownership of the result string
    // to Tcl.

    frame = null;
    varFrame = null;    

    resetResult();
}

/*
 *----------------------------------------------------------------------
 *
 * finalize --
 *
 *	Interpreter finalization method.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Calls dispose() to ensure everything has been cleaned up.
 *
 *----------------------------------------------------------------------
 */

protected void
finalize()
{
    dispose();
}

/*
 *----------------------------------------------------------------------
 *
 * createCommands --
 *
 *	Create the build-in commands. These commands are loaded on
 *	demand -- the .class file of a Command class are loaded into
 *	the JVM the first time when the given command is executed.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Commands are registered.
 *
 *----------------------------------------------------------------------
 */

protected void 
createCommands() 
{
    Extension.loadOnDemand(this, "after",	  "tcl.lang.AfterCmd");
    Extension.loadOnDemand(this, "append",	  "tcl.lang.AppendCmd");
    Extension.loadOnDemand(this, "array",	  "tcl.lang.ArrayCmd");
    Extension.loadOnDemand(this, "binary",	  "tcl.lang.BinaryCmd");
    Extension.loadOnDemand(this, "break",	  "tcl.lang.BreakCmd");
    Extension.loadOnDemand(this, "case",	  "tcl.lang.CaseCmd");
    Extension.loadOnDemand(this, "catch",	  "tcl.lang.CatchCmd");
    Extension.loadOnDemand(this, "cd",	  	  "tcl.lang.CdCmd");
    Extension.loadOnDemand(this, "clock",	  "tcl.lang.ClockCmd");
    Extension.loadOnDemand(this, "close",	  "tcl.lang.CloseCmd");
    Extension.loadOnDemand(this, "continue",  	  "tcl.lang.ContinueCmd");
    Extension.loadOnDemand(this, "concat",	  "tcl.lang.ConcatCmd");
    Extension.loadOnDemand(this, "eof",	 	  "tcl.lang.EofCmd");
    Extension.loadOnDemand(this, "eval",	  "tcl.lang.EvalCmd");
    Extension.loadOnDemand(this, "error",	  "tcl.lang.ErrorCmd");
    if (! Util.isMac()) {
	Extension.loadOnDemand(this, "exec",	  "tcl.lang.ExecCmd");
    }
    Extension.loadOnDemand(this, "exit",	  "tcl.lang.ExitCmd");
    Extension.loadOnDemand(this, "expr",	  "tcl.lang.ExprCmd");
    Extension.loadOnDemand(this, "fblocked", 	  "tcl.lang.FblockedCmd");
    Extension.loadOnDemand(this, "fconfigure",	  "tcl.lang.FconfigureCmd");
    Extension.loadOnDemand(this, "file",      	  "tcl.lang.FileCmd");
    Extension.loadOnDemand(this, "flush",    	  "tcl.lang.FlushCmd");
    Extension.loadOnDemand(this, "for",      	  "tcl.lang.ForCmd");
    Extension.loadOnDemand(this, "foreach",  	  "tcl.lang.ForeachCmd");
    Extension.loadOnDemand(this, "format",   	  "tcl.lang.FormatCmd");
    Extension.loadOnDemand(this, "gets",   	  "tcl.lang.GetsCmd");
    Extension.loadOnDemand(this, "global",   	  "tcl.lang.GlobalCmd");
    Extension.loadOnDemand(this, "glob",     	  "tcl.lang.GlobCmd");
    Extension.loadOnDemand(this, "if",	  	  "tcl.lang.IfCmd");
    Extension.loadOnDemand(this, "incr",	  "tcl.lang.IncrCmd");
    Extension.loadOnDemand(this, "info",	  "tcl.lang.InfoCmd");
    Extension.loadOnDemand(this, "list",     	  "tcl.lang.ListCmd");
    Extension.loadOnDemand(this, "join",     	  "tcl.lang.JoinCmd");
    Extension.loadOnDemand(this, "lappend",	  "tcl.lang.LappendCmd");
    Extension.loadOnDemand(this, "lindex",	  "tcl.lang.LindexCmd");
    Extension.loadOnDemand(this, "linsert", 	  "tcl.lang.LinsertCmd");
    Extension.loadOnDemand(this, "llength", 	  "tcl.lang.LlengthCmd");
    Extension.loadOnDemand(this, "lrange",  	  "tcl.lang.LrangeCmd");
    Extension.loadOnDemand(this, "lreplace",	  "tcl.lang.LreplaceCmd");
    Extension.loadOnDemand(this, "lsearch",  	  "tcl.lang.LsearchCmd");
    Extension.loadOnDemand(this, "lsort",   	  "tcl.lang.LsortCmd");
    Extension.loadOnDemand(this, "namespace",	  "tcl.lang.NamespaceCmd");
    Extension.loadOnDemand(this, "open",    	  "tcl.lang.OpenCmd");
    Extension.loadOnDemand(this, "package",	  "tcl.lang.PackageCmd");
    Extension.loadOnDemand(this, "proc",	  "tcl.lang.ProcCmd");
    Extension.loadOnDemand(this, "puts",	  "tcl.lang.PutsCmd");
    Extension.loadOnDemand(this, "pwd",	  	  "tcl.lang.PwdCmd");
    Extension.loadOnDemand(this, "read",	  "tcl.lang.ReadCmd");
    Extension.loadOnDemand(this, "regsub",	  "tcl.lang.RegsubCmd");
    Extension.loadOnDemand(this, "rename",	  "tcl.lang.RenameCmd");
    Extension.loadOnDemand(this, "return",	  "tcl.lang.ReturnCmd");
    Extension.loadOnDemand(this, "scan",	  "tcl.lang.ScanCmd");
    Extension.loadOnDemand(this, "seek",	  "tcl.lang.SeekCmd");
    Extension.loadOnDemand(this, "set",	  	  "tcl.lang.SetCmd");
    Extension.loadOnDemand(this, "socket",	  "tcl.lang.SocketCmd");
    Extension.loadOnDemand(this, "source",	  "tcl.lang.SourceCmd");
    Extension.loadOnDemand(this, "split",	  "tcl.lang.SplitCmd");
    Extension.loadOnDemand(this, "string",	  "tcl.lang.StringCmd");
    Extension.loadOnDemand(this, "subst",	  "tcl.lang.SubstCmd");
    Extension.loadOnDemand(this, "switch",	  "tcl.lang.SwitchCmd");
    Extension.loadOnDemand(this, "tell",  	  "tcl.lang.TellCmd");
    Extension.loadOnDemand(this, "time",	  "tcl.lang.TimeCmd");
    Extension.loadOnDemand(this, "trace",	  "tcl.lang.TraceCmd");
    Extension.loadOnDemand(this, "unset",	  "tcl.lang.UnsetCmd");
    Extension.loadOnDemand(this, "update",	  "tcl.lang.UpdateCmd");
    Extension.loadOnDemand(this, "uplevel",	  "tcl.lang.UplevelCmd");
    Extension.loadOnDemand(this, "upvar",	  "tcl.lang.UpvarCmd");
    Extension.loadOnDemand(this, "variable",	  "tcl.lang.VariableCmd");
    Extension.loadOnDemand(this, "vwait",	  "tcl.lang.VwaitCmd");
    Extension.loadOnDemand(this, "while",	  "tcl.lang.WhileCmd");


    // Add "regexp" and related commands to this interp.
    RegexpCmd.init(this);


    // The Java package is only loaded when the user does a
    // "package require java" in the interp. We need to create a small
    // command that will load when "package require java" is called.

    Extension.loadOnDemand(this, "jaclloadjava", "tcl.lang.JaclLoadJavaCmd");
    
    try {
        eval("package ifneeded java 1.2.6 jaclloadjava");
    } catch (TclException e) {
	System.out.println(getResult());
	e.printStackTrace();
	throw new TclRuntimeError("unexpected TclException: " + e);
    }

}

/*
 *----------------------------------------------------------------------
 *
 * setAssocData --
 *
 *	Creates a named association between user-specified data and
 *	this interpreter. If the association already exists the data
 *	is overwritten with the new data. The data.deleteAssocData()
 *	method will be invoked when the interpreter is deleted.
 *
 *	NOTE: deleteAssocData() is not called when an old data is
 *	replaced by a new data. Caller of setAssocData() is
 *	responsible with deleting the old data.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Sets the associated data, creates the association if needed.
 *
 *----------------------------------------------------------------------
 */

public void
setAssocData(
    String name,		// Name for association.
    AssocData data)		// Object associated with the name.
{
    if (assocData == null) {
	assocData = new Hashtable();
    }
    assocData.put(name, data);
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
 *	Deletes the association.
 *
 *----------------------------------------------------------------------
 */

public void
deleteAssocData(
    String name)		// Name of association.
{
    if (assocData == null) {
	return;
    }

    assocData.remove(name);
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
    if (assocData == null) {
	return null;
    } else {
	return (AssocData) assocData.get(name);
    }
}

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

public void
backgroundError()
{
    BgErrorMgr mgr = (BgErrorMgr)getAssocData("tclBgError");
    if (mgr == null) {
	mgr = new BgErrorMgr(this);
	setAssocData("tclBgError", mgr);
    }
    mgr.addBgError();
}

/*-----------------------------------------------------------------
 *
 *	                     VARIABLES
 *
 *-----------------------------------------------------------------
 */

/*
 *----------------------------------------------------------------------
 *
 * setVar --
 *
 *	Sets a variable whose name and value are stored in TclObject.
 *
 * Results:
 *	The TclObject, as it was set is returned.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

final TclObject 
setVar(
    TclObject nameObj,		// Name of variable, array, or array element
				// to set.
    TclObject value,		// New value for variable.
    int flags)			// Various flags that tell how to set value:
				// any of GLOBAL_ONLY, NAMESPACE_ONLY,
				// APPEND_VALUE, or LIST_ELEMENT. 
throws 
    TclException 
{
    return Var.setVar(this, nameObj, value, (flags|TCL.LEAVE_ERR_MSG));
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

public final 
TclObject 
setVar(
    String name,		// Name of variable, array, or array element
				// to set.
    TclObject value,		// New value for variable.
    int flags)			// Various flags that tell how to set value:
				// any of GLOBAL_ONLY, NAMESPACE_ONLY,
				// APPEND_VALUE, or LIST_ELEMENT. 
throws
    TclException 
{
    return Var.setVar(this, name, value, (flags|TCL.LEAVE_ERR_MSG));
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

public final TclObject 
setVar(
    String name1,		// If name2 is null, this is name of a scalar
				// variable. Otherwise it is the name of an
				// array. 
    String name2,		// Name of an element within an array, or
				// null.
    TclObject value,		// New value for variable.
    int flags)			// Various flags that tell how to set value:
				// any of GLOBAL_ONLY, NAMESPACE_ONLY,
				// APPEND_VALUE or LIST_ELEMENT.
throws
    TclException
{
    return Var.setVar(this, name1, name2, value, (flags|TCL.LEAVE_ERR_MSG));
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

final void
setVar(
    String name,		// Name of variable, array, or array element
				// to set.
    String strValue,		// New value for variable.
    int flags)			// Various flags that tell how to set value:
				// any of GLOBAL_ONLY, NAMESPACE_ONLY,
				// APPEND_VALUE, orLIST_ELEMENT. 
throws 
    TclException 
{
    Var.setVar(this, name, TclString.newInstance(strValue),
	       (flags|TCL.LEAVE_ERR_MSG));
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

final void
setVar(
    String name1,		// If name2 is null, this is name of a scalar
				// variable. Otherwise it is the name of an
				// array.  
    String name2,		// Name of an element within an array, or
				// null. 
    String strValue,		// New value for variable. 
    int flags)			// Various flags that tell how to set value:
				// any of TCL.GLOBAL_ONLY, TCL.NAMESPACE_ONLY,
				// TCL.APPEND_VALUE, or TCL.LIST_ELEMENT.
throws 
    TclException
{
    Var.setVar(this, name1, name2, TclString.newInstance(strValue),
	    (flags|TCL.LEAVE_ERR_MSG));
}

/*
 *----------------------------------------------------------------------
 *
 * getVar --
 *
 *	Get the value of a variable.
 *
 * Results:
 *	Returns the value of the variable. If the variable is not defined
 *      a TclException will be raised.
 *
 * Side effects:
 *	May trigger traces.
 *
 *----------------------------------------------------------------------
 */

final TclObject 
getVar(
    TclObject nameObj,		// The name of a variable, array, or array
				// element.
    int flags)			// Various flags that tell how to get value:
				// any of TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
throws
    TclException 
{
    return Var.getVar(this, nameObj, (flags|TCL.LEAVE_ERR_MSG));
}

/*
 *----------------------------------------------------------------------
 *
 * getVar --
 *
 *	Get the value of a variable.
 *
 * Results:
 *	Returns the value of the variable. If the variable is not defined
 *      a TclException will be raised.
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
				// any of TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
throws
    TclException
{
    return Var.getVar(this, name, (flags|TCL.LEAVE_ERR_MSG));
}

/*
 *----------------------------------------------------------------------
 *
 * getVar --
 *
 *	Get the value of a variable.
 *
 * Results:
 *	Returns the value of the variable. If the variable is not defined
 *      a TclException will be raised.
 *
 * Side effects:
 *	May trigger traces.
 *
 *----------------------------------------------------------------------
 */

public final TclObject
getVar(
    String name1,		// If name2 is null, this is name of a scalar
				// variable. Otherwise it is the name of an
				// array. 
    String name2,		// Name of an element within an array, or
				// null.
    int flags)			// Flags that tell how to get value:
				// TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY. 
throws
    TclException
{
    return Var.getVar(this, name1, name2, (flags|TCL.LEAVE_ERR_MSG));
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

final void
unsetVar(
    TclObject nameObj,		// The name of a variable, array, or array
				// element.
    int flags)			// Various flags that tell how to get value:
				// any of TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
throws 
    TclException
{
    Var.unsetVar(this, nameObj, (flags|TCL.LEAVE_ERR_MSG));
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
				// any of TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY.
throws 
    TclException 
{
    Var.unsetVar(this, name, (flags|TCL.LEAVE_ERR_MSG));
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
    String name1,		// If name2 is null, this is name of a scalar
				// variable. Otherwise it is the name of an
				// array. 
    String name2,		// Name of an element within an array, or
				// null.
    int flags)			// Flags that tell how to get value:
				// TCL.GLOBAL_ONLY or TCL.NAMESPACE_ONLY. 
throws
    TclException
{
    Var.unsetVar(this, name1, name2, (flags|TCL.LEAVE_ERR_MSG));
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

void
traceVar(
    TclObject nameObj,		// Name of variable;  may end with "(index)"
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
    Var.traceVar(this, nameObj, flags, trace);
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
    Var.traceVar(this, name, flags, trace);
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

public void
traceVar(
    String part1,		// Name of scalar variable or array.
    String part2,		// Name of element within array;  null means
				// trace applies to scalar variable or array
				// as-a-whole.  
    VarTrace trace,		// Object to notify when specified ops are
				// invoked upon varName.
    int flags)			// OR-ed collection of bits, including any
				// of TCL.TRACE_READS, TCL.TRACE_WRITES,
				// TCL.TRACE_UNSETS, TCL.GLOBAL_ONLY, and
				// TCL.NAMESPACE_ONLY.
throws
    TclException
{
    Var.traceVar(this, part1, part2, flags, trace);
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

void
untraceVar(
    TclObject nameObj,		// Name of variable;  may end with "(index)"
				// to signify an array reference.
    VarTrace trace,		// Object associated with trace.
    int flags)			// OR-ed collection of bits describing current
				// trace, including any of TCL.TRACE_READS,
				// TCL.TRACE_WRITES, TCL.TRACE_UNSETS,
				// TCL.GLOBAL_ONLY and TCL.NAMESPACE_ONLY.
{
    Var.untraceVar(this, nameObj, flags, trace);
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

public void
untraceVar(
    String name,		// Name of variable;  may end with "(index)"
				// to signify an array reference.
    VarTrace trace,		// Object associated with trace.
    int flags)			// OR-ed collection of bits describing current
				// trace, including any of TCL.TRACE_READS,
				// TCL.TRACE_WRITES, TCL.TRACE_UNSETS,
				// TCL.GLOBAL_ONLY and TCL.NAMESPACE_ONLY.
{
    Var.untraceVar(this, name, flags, trace);
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

public void 
untraceVar(
    String part1,		// Name of scalar variable or array.
    String part2,		// Name of element within array;  null means
				// trace applies to scalar variable or array
				// as-a-whole.  
    VarTrace trace,		// Object associated with trace.
    int flags)			// OR-ed collection of bits describing current
				// trace, including any of TCL.TRACE_READS,
				// TCL.TRACE_WRITES, TCL.TRACE_UNSETS,
				// TCL.GLOBAL_ONLY and TCL.NAMESPACE_ONLY.
{
    Var.untraceVar(this, part1, part2, flags, trace);
}

/*
 *----------------------------------------------------------------------
 *
 * Tcl_CreateCommand -> createCommand
 *
 *	Define a new command in the interpreter.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	If a command named cmdName already exists for interp, it is
 *	deleted. In the future, when cmdName is seen as the name of a
 *	command by eval(), cmd will be called. When the command is
 *	deleted from the table, cmd.disposeCmd() be called if cmd
 *	implements the CommandWithDispose interface.
 *
 *----------------------------------------------------------------------
 */

public void
createCommand(
    String cmdName,		// Name of command.
    Command cmdImpl)		// Command object to associate with
				// cmdName.
{
    ImportRef oldRef = null;
    NamespaceCmd.Namespace ns;
    WrappedCommand cmd, refCmd;
    String tail;
    ImportedCmdData data;

    if (deleted) {
	// The interpreter is being deleted.  Don't create any new
	// commands; it's not safe to muck with the interpreter anymore.

	return;
    }

    // Determine where the command should reside. If its name contains 
    // namespace qualifiers, we put it in the specified namespace; 
    // otherwise, we always put it in the global namespace.

    if (cmdName.indexOf("::") != -1) {
	// Java does not support passing an address so we pass
	// an array of size 1 and then assign arr[0] to the value
	NamespaceCmd.Namespace[] nsArr     = new NamespaceCmd.Namespace[1];
	NamespaceCmd.Namespace[] dummyArr  = new NamespaceCmd.Namespace[1];
	String[]    tailArr   = new String[1];

	NamespaceCmd.getNamespaceForQualName(this, cmdName, null,
		         NamespaceCmd.CREATE_NS_IF_UNKNOWN, nsArr,
			 dummyArr, dummyArr, tailArr);

	ns = nsArr[0];
	tail = tailArr[0];

       if ((ns == null) || (tail == null)) {
	    return;
	}
    } else {
	ns =  globalNs;
	tail = cmdName;
    }

    cmd = (WrappedCommand) ns.cmdTable.get(tail);
    if (cmd != null) {
	// Command already exists. Delete the old one.
	// Be careful to preserve any existing import links so we can
	// restore them down below.  That way, you can redefine a
	// command and its import status will remain intact.

	oldRef = cmd.importRef;
	cmd.importRef = null;

	deleteCommandFromToken(cmd);

	// FIXME : create a test case for this condition!

	cmd = (WrappedCommand) ns.cmdTable.get(tail);
	if (cmd != null) {
	    // If the deletion callback recreated the command, just throw
            // away the new command (if we try to delete it again, we
            // could get stuck in an infinite loop).
	    
	    cmd.table.remove(cmd.hashKey);
	}
    }

    cmd = new WrappedCommand();
    ns.cmdTable.put(tail, cmd);
    cmd.table = ns.cmdTable;
    cmd.hashKey = tail;
    cmd.ns = ns;
    cmd.cmd = cmdImpl;
    cmd.deleted = false;
    // FIXME : import feature not implemented
    //cmd.importRef = null;

    // Plug in any existing import references found above.  Be sure
    // to update all of these references to point to the new command.

    if (oldRef != null) {
	cmd.importRef = oldRef;
	while (oldRef != null) {
	    refCmd = oldRef.importedCmd;
	    data = (ImportedCmdData) refCmd.cmd;
	    data.realCmd = cmd;
	    oldRef = oldRef.next;
	}
    }

    // There are no shadowed commands in Jacl because they are only
    // used in the 8.0 compiler

    return;
}

/*
 *----------------------------------------------------------------------
 *
 * Tcl_GetCommandFullName -> getCommandFullName
 *
 *	Given a token returned by, e.g., Tcl_CreateCommand or
 *	Tcl_FindCommand, this procedure appends to an object the command's
 *	full name, qualified by a sequence of parent namespace names. The
 *	command's fully-qualified name may have changed due to renaming.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The command's fully-qualified name is returned.
 *
 *----------------------------------------------------------------------
 */

String getCommandFullName(
    WrappedCommand cmd)	        // Token for the command.
{
    Interp interp = this;
    StringBuffer name = new StringBuffer();

    // Add the full name of the containing namespace, followed by the "::"
    // separator, and the command name.

    if (cmd != null) {
	if (cmd.ns != null) {
	    name.append(cmd.ns.fullName);
	    if (cmd.ns != interp.globalNs) {
		name.append("::");
	    }
	}
	if (cmd.table != null) {
	    name.append(cmd.hashKey);
	}
    }

    return name.toString();
}

/*
 *----------------------------------------------------------------------
 *
 * Tcl_DeleteCommand -> deleteCommand
 *
 *	Remove the given command from the given interpreter.
 *
 * Results:
 *	0 is returned if the command was deleted successfully.
 *	-1 is returned if there didn't exist a command by that
 *	name.
 *
 * Side effects:
 *	CmdName will no longer be recognized as a valid command for
 *	the interpreter.
 *
 *----------------------------------------------------------------------
 */

public int
deleteCommand(
    String cmdName)		// Name of command to remove.
{
    WrappedCommand cmd;

    //  Find the desired command and delete it.

    try {
	cmd = NamespaceCmd.findCommand(this, cmdName, null, 0);
    } catch (TclException e) {
	// This should never happen
	throw new TclRuntimeError("unexpected TclException: " + e);
    }
    if (cmd == null) {
	return -1;
    }
    return deleteCommandFromToken(cmd);
}

/*
 *----------------------------------------------------------------------
 *
 * Tcl_DeleteCommandFromToken -> deleteCommandFromToken
 *
 *	Remove the given command from the given interpreter.
 *
 * Results:
 *	0 is returned if the command was deleted successfully.
 *	-1 is returned if there didn't exist a command by that
 *	name.
 *
 * Side effects:
 *	cmdName will no longer be recognized as a valid command for
 *	the interpreter.
 *
 *----------------------------------------------------------------------
 */

protected int
deleteCommandFromToken(
    WrappedCommand cmd)                // Wrapper Token for command to delete.
{
    if (cmd == null) {
	return -1;
    }

    ImportRef ref, nextRef;
    WrappedCommand importCmd;

    // The code here is tricky.  We can't delete the hash table entry
    // before invoking the deletion callback because there are cases
    // where the deletion callback needs to invoke the command (e.g.
    // object systems such as OTcl). However, this means that the
    // callback could try to delete or rename the command. The deleted
    // flag allows us to detect these cases and skip nested deletes.

    // FIXME : how can this be implemented in Jacl?

    if (cmd.deleted) {
	// Another deletion is already in progress.  Remove the hash
	// table entry now, but don't invoke a callback or free the
	// command structure.

	cmd.table.remove(cmd.hashKey);
	cmd.table = null;
	cmd.hashKey = null;
	return 0;
    }

    cmd.deleted = true;
    if (cmd.cmd instanceof CommandWithDispose) {
	((CommandWithDispose) cmd.cmd).disposeCmd();
    }

    // If this command was imported into other namespaces, then imported
    // commands were created that refer back to this command. Delete these
    // imported commands now.

    for (ref = cmd.importRef;  ref != null;
            ref = nextRef) {
	nextRef = ref.next;
	importCmd = ref.importedCmd;
	deleteCommandFromToken(importCmd);
    }

    // FIXME : what does this mean? Is this a mistake in the C comment?

    // Don't use hPtr to delete the hash entry here, because it's
    // possible that the deletion callback renamed the command.
    // Instead, use cmdPtr->hptr, and make sure that no-one else
    // has already deleted the hash entry.

    if (cmd.table != null) {
	cmd.table.remove(cmd.hashKey);
	cmd.table = null;
	cmd.hashKey = null;
    }

    // Drop the reference to the Command instance inside the WrappedCommand

    cmd.cmd = null;

    // We do not need to cleanup the WrappedCommand because GC will get it.

    return 0;
}

/*
 *----------------------------------------------------------------------
 *
 * TclRenameCommand -> renameCommand
 *
 *      Called to give an existing Tcl command a different name. Both the
 *      old command name and the new command name can have "::" namespace
 *      qualifiers. If the new command has a different namespace context,
 *      the command will be moved to that namespace and will execute in
 *	the context of that new namespace.
 *
 *      If the new command name is null or the empty string, the command is
 *      deleted.
 *
 * Results:
 *      Returns if successful, raises TclException if anything goes wrong.
 *
 *----------------------------------------------------------------------
 */

protected void renameCommand(
	          String oldName,    // Existing command name.
		  String newName)    // New command name.
    throws TclException
{
    Interp interp = this;
    String newTail;
    NamespaceCmd.Namespace cmdNs, newNs;
    WrappedCommand cmd;
    Hashtable table,   oldTable;
    String    hashKey, oldHashKey;

    // Find the existing command. An error is returned if cmdName can't
    // be found.

    cmd = NamespaceCmd.findCommand(interp, oldName, null, 0);
    if (cmd == null) {
	throw new TclException(interp, "can't " +
	    (((newName == null)||(newName.length() == 0)) ? "delete" : "rename") +
	    " \"" + oldName + "\": command doesn't exist");
    }
    cmdNs = cmd.ns;

    // If the new command name is NULL or empty, delete the command. Do this
    // with Tcl_DeleteCommandFromToken, since we already have the command.
    
    if ((newName == null) || (newName.length() == 0)) {
	deleteCommandFromToken(cmd);
	return;
    }

    // Make sure that the destination command does not already exist.
    // The rename operation is like creating a command, so we should
    // automatically create the containing namespaces just like
    // Tcl_CreateCommand would.

    NamespaceCmd.Namespace[] newNsArr   = new NamespaceCmd.Namespace[1];
    NamespaceCmd.Namespace[] dummyArr   = new NamespaceCmd.Namespace[1];
    String[]                 newTailArr = new String[1];

    NamespaceCmd.getNamespaceForQualName(interp, newName, null,
        NamespaceCmd.CREATE_NS_IF_UNKNOWN, newNsArr,
	dummyArr, dummyArr, newTailArr);

    newNs   = newNsArr[0];
    newTail = newTailArr[0];

    if ((newNs == null) || (newTail == null)) {
	throw new TclException(interp,
	    "can't rename to \"" + newName + "\": bad command name");
    }
    if (newNs.cmdTable.get(newTail) != null) {
	throw new TclException(interp,
	    "can't rename to \"" + newName + "\": command already exists");
    }

    // Warning: any changes done in the code here are likely
    // to be needed in Tcl_HideCommand() code too.
    // (until the common parts are extracted out)     --dl

    // Put the command in the new namespace so we can check for an alias
    // loop. Since we are adding a new command to a namespace, we must
    // handle any shadowing of the global commands that this might create.

    oldTable    = cmd.table;
    oldHashKey  = cmd.hashKey;
    newNs.cmdTable.put(newTail, cmd);
    cmd.table   = newNs.cmdTable;
    cmd.hashKey = newTail;
    cmd.ns      = newNs;

    // FIXME : this is a nasty hack that fixes renaming for Procedures
    // that move from one namespace to another, but the real problem
    // is that a rename does not work for Command instances in general

    if (cmd.cmd instanceof Procedure) {
	Procedure p = (Procedure) cmd.cmd;
	p.ns = cmd.ns;
    }

    /*
    // Now check for an alias loop. If we detect one, put everything back
    // the way it was and report the error.

    result = TclPreventAliasLoop(interp, interp, (Tcl_Command) cmdPtr);
    if (result != TCL_OK) {
        Tcl_DeleteHashEntry(cmdPtr->hPtr);
        cmdPtr->hPtr = oldHPtr;
        cmdPtr->nsPtr = cmdNsPtr;
        return;
    }

    */

    // The new command name is okay, so remove the command from its
    // current namespace. This is like deleting the command, so bump
    // the cmdEpoch to invalidate any cached references to the command.

    oldTable.remove(oldHashKey);

    return;
}

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

public Command
getCommand(
    String cmdName) 		// String name of the command.
{
    //  Find the desired command and return it.

    WrappedCommand cmd;

    try {
	cmd = NamespaceCmd.findCommand(this, cmdName, null, 0);
    } catch (TclException e) {
	// This should never happen
	throw new TclRuntimeError("unexpected TclException: " + e);
    }

    return ((cmd == null) ? null : cmd.cmd);
}

/*
 *----------------------------------------------------------------------
 *
 * commandComplete --
 *
 *	Check if the string is a complete Tcl command string.
 *
 * Result:
 *	A boolean value indicating whether the string is a complete Tcl
 *	command string.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public static boolean
commandComplete(
    String string)		// The string to check.
{
    return Parser.commandComplete(string, string.length());
}


/*-----------------------------------------------------------------
 *
 *	                     EVAL
 *
 *-----------------------------------------------------------------
 */


/*
 *----------------------------------------------------------------------
 *
 * getResult --
 *
 *	Queries the value of the result.
 *
 * Results:
 *	The current result in the interpreter.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

public  final TclObject
getResult() 
{
    return m_result;
}

/*
 *----------------------------------------------------------------------
 *
 * setResult --
 *
 *	Arrange for the given Tcl Object to be placed as the result 
 *	object for the interpreter.  Convenience functions are also
 *	available to create a Tcl Object out of the most common Java
 *	types.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The object result for the interpreter is updated.
 *
 *----------------------------------------------------------------------
 */

public final void 
setResult(
    TclObject r)	// A Tcl Object to be set as the result.
{
    if (r == null) {
	throw new NullPointerException(
		"Interp.setResult() called with null TclObject argument.");
    }
    if (m_result != m_nullResult) {
	m_result.release();
    }

    m_result = r;
    m_result.preserve();
}

/*
 *----------------------------------------------------------------------
 *
 * setResult --
 *
 *	Arrange for the given Tcl Object to be placed as the result 
 *	object for the interpreter.  Convenience functions are also
 *	available to create a Tcl Object out of the most common Java
 *	types.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The object result for the interpreter is updated.
 *
 *----------------------------------------------------------------------
 */

public final void 
setResult(
    String r)		// A string result.
{
    if (r == null) {
	resetResult();
    } else {
	setResult(TclString.newInstance(r));
    }
}

/*
 *----------------------------------------------------------------------
 *
 * setResult --
 *
 *	Arrange for the given Tcl Object to be placed as the result 
 *	object for the interpreter.  Convenience functions are also
 *	available to create a Tcl Object out of the most common Java
 *	types.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The object result for the interpreter is updated.
 *
 *----------------------------------------------------------------------
 */

public final void 
setResult(
    int r)		// An int result.
{
    setResult(TclInteger.newInstance(r));
}

/*
 *----------------------------------------------------------------------
 *
 * setResult --
 *
 *	Arrange for the given Tcl Object to be placed as the result 
 *	object for the interpreter.  Convenience functions are also
 *	available to create a Tcl Object out of the most common Java
 *	types.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The object result for the interpreter is updated.
 *
 *----------------------------------------------------------------------
 */

public final void 
setResult(
    double r)		// A double result.
{
    setResult(TclDouble.newInstance(r));
}

/*
 *----------------------------------------------------------------------
 *
 * setResult --
 *
 *	Arrange for the given Tcl Object to be placed as the result 
 *	object for the interpreter.  Convenience functions are also
 *	available to create a Tcl Object out of the most common Java
 *	types.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The object result for the interpreter is updated.
 *
 *----------------------------------------------------------------------
 */

public final void 
setResult(
    boolean r)		// A boolean result.
{
    setResult(TclBoolean.newInstance(r));
}

/*
 *----------------------------------------------------------------------
 *
 * resetResult --
 *
 *	This procedure resets the interpreter's result object.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	It resets the result object to an unshared empty object. It 
 *	also clears any error information for the interpreter.
 *
 *----------------------------------------------------------------------
 */

public final void 
resetResult() 
{
    if (m_result != m_nullResult) {
	m_result.release();
	m_result = m_nullResult;
    }
    errAlreadyLogged = false;
    errInProgress = false;
    errCodeSet = false;
}

/*
 *----------------------------------------------------------------------
 *
 * Tcl_AppendElement --
 *
 *	Convert a string to a valid Tcl list element and append it to the
 *	result (which is ostensibly a list).
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The result in the interpreter given by the first argument is
 *	extended with a list element converted from string. A separator
 *	space is added before the converted list element unless the current
 *	result is empty, contains the single character "{", or ends in " {".
 *
 *	If the string result is empty, the object result is moved to the
 *	string result, then the object result is reset.
 *
 *----------------------------------------------------------------------
 */

void
appendElement(
    String string)		/* String to convert to list element and
				 * add to result. */
throws 
    TclException
{
    TclObject result;

    result = getResult();
    result.preserve();
    result = result.takeExclusive();
    TclList.append(this, result, TclString.newInstance(string));
    setResult(result.toString());
    result.release();
}

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

public void 
eval(
    String string,	// A script to evaluate.
    int flags)		// Flags, either 0 or TCL_GLOBAL_ONLY.
throws 
    TclException 	// A standard Tcl exception.
{
    CharPointer script = new CharPointer(string);
    Parser.eval2(this, script.array, script.index, script.length(), flags);
}

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

public void 
eval(
    String script)	// A script to evaluate.
throws 
    TclException 	// A standard Tcl exception.
{
    eval(script, 0);
}

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

public void 
eval(
    TclObject tobj,	// A Tcl object holding a script to evaluate.
    int flags)		// Flags, either 0 or TCL_GLOBAL_ONLY.
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
    int flags)		// Flags, either 0 or TCL_GLOBAL_ONLY.
throws 
    TclException 	// A standard Tcl exception.
{
    // Append the script to the event list by calling "history add <script>".
    // We call the eval method with the command of type TclObject, so that
    // we don't have to deal with funny chars ("{}[]$\) in the script.

    try {
	TclObject cmd = TclList.newInstance();
	TclList.append(this, cmd, TclString.newInstance("history"));
	TclList.append(this, cmd, TclString.newInstance("add"));
	TclList.append(this, cmd, script);
	eval(cmd, 0);
    } catch (Exception e) {}

    // Finally evaluate the script.

    eval(script, flags);
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
    String fileContent;		// Contains the content of the file.

    fileContent = readScriptFromFile(s);

    if (fileContent == null) {
	throw new TclException(this, "couldn't read file \"" + s + "\"");
    }

    String oldScript = scriptFile;
    scriptFile = s;

    try {
	pushDebugStack(s, 1);
	eval(fileContent, 0);
    }
    catch( TclException e) {
	if( e.getCompletionCode() == TCL.ERROR ) {
	    addErrorInfo("\n    (file \"" + s + "\" line " 
			 + errorLine + ")");
	}
	throw e;
    }
    finally {
	scriptFile = oldScript;
	popDebugStack();
    }
}

/*
 *----------------------------------------------------------------------
 *
 * evalURL --
 *
 *	Loads a Tcl script from a URL and evaluate it in the
 *	current interpreter.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The side effects will be determined by the exact Tcl code to be 
 *	evaluated.
 *
 *----------------------------------------------------------------------
 */

void
evalURL(
    URL context, 		// URL context under which s is interpreted.
    String s)  			// The name of URL.
throws 
    TclException
{
    String fileContent;		// Contains the content of the file.

    fileContent = readScriptFromURL(context, s);
    if (fileContent == null) {
	throw new TclException(this, "cannot read URL \"" + s + "\"");
    }

    String oldScript = scriptFile;
    scriptFile = s;

    try {
	eval(fileContent, 0);
    }
    finally {
	scriptFile = oldScript;
    }
}

/*
 *----------------------------------------------------------------------
 *
 * readScriptFromFile --
 *
 *	Read the script file into a string.
 *
 * Results:
 *	Returns the content of the script file.
 *
 * Side effects:
 *	If a new File object cannot be created for s, the result is reset.
 *
 *----------------------------------------------------------------------
 */

private String
readScriptFromFile(
    String s)			// The name of the file.
{
    File sourceFile;
    FileInputStream fs;
    try {
	sourceFile = FileUtil.getNewFileObj(this, s);
	fs = new FileInputStream(sourceFile);
    } catch (TclException e) {
	resetResult();	
	return null;
    } catch (FileNotFoundException e) {
	return null;
    } catch (SecurityException sec_e) {
	return null;
    }

    try {
	byte charArray[] = new byte[fs.available()];
	fs.read(charArray);
	return new String(charArray);
    } catch (IOException e) {
	return null;
    } finally {
	closeInputStream(fs);
    }
}

/*
 *----------------------------------------------------------------------
 *
 * readScriptFromURL --
 *
 *	Read the script file into a string, treating the file as 
 *	an URL.
 *
 * Results:
 *	The content of the script file.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private String
readScriptFromURL(
    URL context, 	// Use as the URL context if s is a relative URL.
    String s)		// ???
{
    Object content = null;
    URL url;

    try {
	url = new URL(context, s);
    }
    catch (MalformedURLException e) {
	return null;
    }

    try {
	content = url.getContent();
    }
    catch (UnknownServiceException e) {
	Class jar_class;
	
	try {
	    jar_class = Class.forName("java.net.JarURLConnection");
	} catch (Exception e2) {
	    return null;
	}

	Object jar;
	try {
	    jar = url.openConnection();
	} catch (IOException e2) {
	    return null;
	}

	if ( jar == null ) {
	    return null;
	}
	
	// We must call JarURLConnection.getInputStream() dynamically
	// Because the class JarURLConnection does not exist in JDK1.1
	
	try {
	    Method m = jar_class.getMethod("openConnection",null);
	    content = m.invoke(jar,null);
	} catch (Exception e2) {
	    return null;
	}
    }
    catch (IOException e) {
	return null;
    }
    catch (SecurityException e) {
	return null;
    }

    if (content instanceof String) {
	return (String)content;
    } else if (content instanceof InputStream) {
// FIXME : use custom stream handler
	InputStream fs = (InputStream) content;

	try {
// FIXME : read does not check return values
	    byte charArray[] = new byte[fs.available()];
	    fs.read(charArray);
	    return new String(charArray);
	} catch (IOException e2) {
	    return null;
	} finally {
	    closeInputStream(fs);
	}
    } else {
	return null;
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
    catch (IOException e) {;}
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
    InputStream stream = null;

    try {
	stream = Class.class.getResourceAsStream(resName);
    } catch (SecurityException e2) {
	// This catch is necessary if Jacl is to work in an applet
        // at all. Note that java::new will not work from within Jacl
        // in an applet.

        System.err.println("evalResource: Ignoring SecurityException, " +
                "it is likely we are running in an applet: " +
                "cannot read resource \"" + resName + "\"" + e2);

	return;
    }

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
 * backslash --
 *
 *	Figure out how to handle a backslash sequence.  The index
 *	of the ChapPointer must be pointing to the first /.
 *
 * Results:
 *	The return value is an instance of BackSlashResult that 
 *	contains the character that should be substituted in place 
 *	of the backslash sequence that starts at src.index, and
 *	an index to the next character after the backslash sequence.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

static BackSlashResult 
backslash(
    String s, 
    int i, 
    int len)
{
     CharPointer script = new CharPointer(s.substring(0, len));
     script.index = i;
     return Parser.backslash(script.array,script.index);
}



/*
 *----------------------------------------------------------------------
 *
 * setErrorCode --
 *
 *	This procedure is called to record machine-readable information
 *	about an error that is about to be returned. The caller should
 *	build a list object up and pass it to this routine.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The errorCode global variable is modified to be the new value.
 *	A flag is set internally to remember that errorCode has been
 *	set, so the variable doesn't get set automatically when the
 *	error is returned.
 *
 *	If the errorCode variable have write traces, any arbitrary side
 *	effects may happen in those traces. TclException's caused by the
 *	traces, however, are ignored and not passed back to the caller
 *	of this function.
 *
 *----------------------------------------------------------------------
 */
public void
setErrorCode(
    TclObject code)		// The errorCode object.
{
    try {
	setVar("errorCode", null, code, TCL.GLOBAL_ONLY);
	errCodeSet = true;
    } catch (TclException excp) {
	// Ignore any TclException's, possibly caused by variable traces on
	// the errorCode variable. This is compatible with the behavior of
	// the Tcl C API.
    }
}



/*
 *----------------------------------------------------------------------
 *
 * addErrorInfo --
 *
 *	Add information to the "errorInfo" variable that describes the
 *	current error.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The contents of message are added to the "errorInfo" variable.
 *	If eval() has been called since the current value of errorInfo
 *	was set, errorInfo is cleared before adding the new message.
 *	If we are just starting to log an error, errorInfo is initialized
 *	from the error message in the interpreter's result.
 *
 *	If the errorInfo variable have write traces, any arbitrary side
 *	effects may happen in those traces. TclException's caused by the
 *	traces, however, are ignored and not passed back to the caller
 *	of this function.
 *
 *----------------------------------------------------------------------
 */

public void
addErrorInfo(
    String message)		// The message to record.
{
    if (!errInProgress) {
	errInProgress = true;

	try {
	    setVar("errorInfo", null, getResult().toString(),
		   TCL.GLOBAL_ONLY);
	} catch (TclException e1) {
	    // Ignore (see try-block above).
	}

	// If the errorCode variable wasn't set by the code
	// that generated the error, set it to "NONE".

	if (!errCodeSet) {
	    try {
		setVar("errorCode", null, "NONE", TCL.GLOBAL_ONLY);
	    } catch (TclException e1) {
		// Ignore (see try-block above).
	    }
	}
    }

    try {
	setVar("errorInfo", null, message,
		TCL.APPEND_VALUE|TCL.GLOBAL_ONLY);
    }  catch (TclException e1) {
	// Ignore (see try-block above).
    }
}

/*
 *----------------------------------------------------------------------
 *
 * updateReturnInfo --
 *
 *	This internal method is used by various parts of the Jacl
 *	interpreter when a TclException of TCL.RETURN is received. The
 *	most common case is when the "return" command is executed
 *	inside a Tcl procedure. This method examines fields such as
 *	interp.returnCode and interp.errorCode and determines the real
 *	return status of the Tcl procedure accordingly.
 *
 * Results:
 *	The return value is the true completion code to use for
 *	the Tcl procedure, instead of TCL.RETURN. It's the same
 *	value that was given to the "return -code" option.
 *
 *	If an internal value of TCL.OK is returned, it means than the
 *	caller of this method should ignore any TclException that it has
 *	just received and continue execution.
 *
 * Side effects:
 *	The errorInfo and errorCode variables may get modified.
 *
 *----------------------------------------------------------------------
 */

protected int
updateReturnInfo()
{
    int code;

    code = returnCode;
    returnCode = TCL.OK;

    if (code == TCL.ERROR) {
	try {
	    setVar("errorCode", null, (errorCode != null) ? errorCode : "NONE",
		    TCL.GLOBAL_ONLY);
	} catch (TclException e) {
	    // An error may happen during a trace to errorCode. We ignore it.
	    // This may leave error messages inside Interp.result (which
	    // is compatible with Tcl 8.0 behavior.
	}
	errCodeSet = true;

	if (errorInfo != null) {
	    try {
		setVar("errorInfo", null, errorInfo, TCL.GLOBAL_ONLY);
	    } catch (TclException e) {
		// An error may happen during a trace to errorInfo. We
		// ignore it.  This may leave error messages inside
		// Interp.result (which is compatible with Tcl 8.0
		// behavior.
	    }
	    errInProgress = true;
	}
    }

    return code;
}

/*
 *----------------------------------------------------------------------
 *
 * newCallFrame --
 *
 *	Creates a new callframe. This method can be overrided to 
 *	provide debugging support.
 *
 * Results:
 *	A new CallFrame.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

protected CallFrame
newCallFrame(
    Procedure proc, 		// The procedure which will later be 
				// execute inside the new callframe.
    TclObject[] objv)  		// The arguments to pass to the procedure.
throws 
    TclException 		// Incorrect number of arguments passed.
{
    return new CallFrame(this, proc, objv);
}

/*
 *----------------------------------------------------------------------
 *
 * newCallFrame --
 *
 *	Creates a new callframe. This method can be overrided to 
 *	provide debugging support.
 *
 * Results:
 *	A new CallFrame.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

protected CallFrame 
newCallFrame() 
{
    return new CallFrame(this);
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
 *	If the working dir is null, set it.
 *
 *----------------------------------------------------------------------
 */

File
getWorkingDir()
{
    if (workingDir == null) {
	try {
	    String dirName = getVar("env", "HOME", 0).toString();
	    workingDir = FileUtil.getNewFileObj(this, dirName);
	} catch(TclException e) {
	    resetResult();
	}
	workingDir = new File(Util.tryGetSystemProperty("user.home", "."));
    }
    return workingDir;
}

/*
 *----------------------------------------------------------------------
 *
 * setWorkingDir --
 *
 *	Set the current working directory for this interpreter.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Set the working directory or throw a TclException.
 *
 *----------------------------------------------------------------------
 */

void
setWorkingDir(
    String dirName)
throws
    TclException
{
    File dirObj = FileUtil.getNewFileObj(this, dirName);

    //  Use the canonical name of the path, if possible.

    try {
	dirObj = new File(dirObj.getCanonicalPath());
    } catch (IOException e) {
    }


    if (dirObj.isDirectory()) {
	workingDir = dirObj;
    } else {
	throw new TclException(this, 
		"couldn't change working directory to \""
                + dirObj.getName() + "\": no such file or directory");
    }    
}

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
 * pkgProvide --
 *
 *	This procedure is invoked to declare that a particular version
 *	of a particular package is now present in an interpreter.  There
 *	must not be any other version of this package already
 *	provided in the interpreter.
 *
 * Results:
 *	Normally does nothing; if there is already another version
 *	of the package loaded then an error is raised.
 *
 * Side effects:
 *	The interpreter remembers that this package is available,
 *	so that no other version of the package may be provided for
 *	the interpreter.
 *
 *----------------------------------------------------------------------
 */
 
public final void
pkgProvide(
    String name, 
    String version)
throws 
    TclException
{
    PackageCmd.pkgProvide(this,name,version);
}

/*
 *----------------------------------------------------------------------
 *
 * pkgRequire --
 *
 *	This procedure is called by code that depends on a particular
 *	version of a particular package.  If the package is not already
 *	provided in the interpreter, this procedure invokes a Tcl script
 *	to provide it.  If the package is already provided, this
 *	procedure makes sure that the caller's needs don't conflict with
 *	the version that is present.
 *
 * Results:
 *	If successful, returns the version string for the currently
 *	provided version of the package, which may be different from
 *	the "version" argument.  If the caller's requirements
 *	cannot be met (e.g. the version requested conflicts with
 *	a currently provided version, or the required version cannot
 *	be found, or the script to provide the required version
 *	generates an error), a TclException is raised.
 *
 * Side effects:
 *	The script from some previous "package ifneeded" command may
 *	be invoked to provide the package.
 *
 *----------------------------------------------------------------------
 */

public final String
pkgRequire(
    String pkgname, 
    String version, 
    boolean exact)
throws
    TclException
{
    return PackageCmd.pkgRequire(this, pkgname, version, exact);
}

/*
 * Debugging API.
 *
 * The following section defines two debugging API functions for
 * logging information about the point of execution of Tcl scripts:
 *
 * - pushDebugStack() is called when a procedure body is
 *       executed, or when a file is source'd.
 *	   - popDebugStack() is called when the flow of control is about
 *       to return from a procedure body, or from a source'd file.
 *
 * Two other API functions are used to determine the current point of
 * execution:
 *
 *	   - getScriptFile() returns the script file current being executed.
 *	   - getArgLineNumber(i) returns the line number of the i-th argument
 *	     of the current command.
 *
 * Note: The point of execution is automatically maintained for
 *       control structures such as while, if, for and foreach,
 *	     as long as they use Interp.eval(argv[?]) to evaluate control
 *	     blocks.
 *	    
 *	     The case and switch commands need to set dbg.cmdLine explicitly
 *	     because they may evaluate control blocks that are not elements
 *	     inside the argv[] array. ** This feature not yet implemented. **
 *
 *	     The proc command needs to call getScriptFile() and
 *       getArgLineNumber(3) to find out the location of the proc
 *       body.
 *
 * The debugging API functions in the Interp class are just dummy stub
 * functions. These functions are usually implemented in a subclass of
 * Interp (e.g. DbgInterp) that has real debugging support.
 *
 */

protected DebugInfo dbg;

/**
 * Initialize the debugging information.
 * @return a DebugInfo object used by Interp in non-debugging mode.
 */
protected DebugInfo 
initDebugInfo() 
{
    return new DebugInfo(null, 1);
}

/**
 * Add more more level at the top of the debug stack.
 *
 * @param fileName the filename for the new stack level
 * @param lineNumber the line number at which the execution of the
 *	   new stack level begins.
 */
void pushDebugStack(
    String fileName, 
    int lineNumber)
{
    // do nothing.
}

/**
 * Remove the top-most level of the debug stack.
 */
void 
popDebugStack() 
throws
    TclRuntimeError {
    // do nothing
}
/**
 * Returns the name of the script file currently under execution.
 *
 * @return the name of the script file currently under execution.
 */
String getScriptFile() 
{
    return dbg.fileName;
}
/**
 * Returns the line number where the given command argument begins. E.g, if
 * the following command is at line 10:
 *
 *	foo {a
 *      b } c
 *
 * getArgLine(0) = 10
 * getArgLine(1) = 10
 * getArgLine(2) = 11
 * 
 * @param index specifies an argument.
 * @return the line number of the given argument.
 */   
int 
getArgLineNumber(
    int index)
{
    return 0;
}


} // end Interp

