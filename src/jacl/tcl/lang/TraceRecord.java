/*
 * TraceRecord.java --
 *
 *	This class is used internally by CallFrame to store one
 *	variable trace.
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TraceRecord.java,v 1.2 1999/06/30 00:13:39 mo Exp $
 *
 */

package tcl.lang;

/**
 * This class is used internally by CallFrame to store one variable
 * trace.
 */

class TraceRecord {

/**
 * Stores info about the conditions under which this trace should be
 * triggered. Should be a combination of TCL.TRACE_READS, TCL.TRACE_WRITES
 * or TCL.TRACE_UNSETS.
 */

int flags;

/**
 * Stores the trace procedure to invoke when a trace is fired.
 */

VarTrace trace;

} // end TraceRecord
