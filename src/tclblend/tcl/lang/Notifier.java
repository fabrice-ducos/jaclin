/*
 * Notifier.java --
 *
 *	Implements the Blend version of the Notifier class.
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

package tcl.lang;

import java.util.*;

/*
 * Implements the Blend version of the Notifier class. The Notifier is
 * the lowest-level part of the event system. It is used by
 * higher-level event sources such as file, JavaBean and timer
 * events. The Notifier manages an event queue that holds TclEvent
 * objects.
 *
 * The Notifier is designed to run in a multi-threaded
 * environment. Each notifier instance is associated with a primary
 * thread. Any thread can queue (or dequeue) events using the
 * queueEvent (or deleteEvents) call. However, only the primary thread
 * may process events in the queue using the doOneEvent()
 * call. Attepmts to call doOneEvent from a non-primary thread will
 * cause a TclRuntimeError.
 *
 * This class does not have a public constructor and thus cannot be
 * instantiated. The only way to for a Tcl extension to get an
 * Notifier is to call Interp.getNotifier() (or
 * Notifier.getNotifierForThread), which returns the Notifier for that
 * interpreter (thread).
 */

public class Notifier {

private static Notifier globalNotifier;

/*
 * First pending event, or null if none.
 */

private TclEvent firstEvent;

/*
 * Last pending event, or null if none.
 */

private TclEvent lastEvent;

/*
 * Last high-priority event in queue, or null if none.
 */

private TclEvent markerEvent;

/*
 * The primary thread of this notifier. Only this thread should process
 * events from the event queue.
 */

Thread primaryThread;

/*
 * Reference count of the notifier. It's used to tell when a notifier
 * is no longer needed.
 */

int refCount;

/*
 *----------------------------------------------------------------------
 *
 * Notifier --
 *
 *	Creates a Notifier instance.
 *
 * Side effects:
 *	Member fields are initialized.
 *
 *----------------------------------------------------------------------
 */

private
Notifier(
    Thread primaryTh)		// The primary thread for this Notifier.
{
    primaryThread = primaryTh;
    firstEvent        = null;
    lastEvent         = null;
    markerEvent       = null;
    refCount = 0;
    init();
}

/*
 *----------------------------------------------------------------------
 *
 * getNotifierForThread --
 *
 *	Get the notifier for this thread, creating the Notifier,
 *	when necessary. Note that this is a partial implementation
 *	that only supports a single notifier thread.
 *
 * Results:
 *	The Notifier for this thread.
 *
 * Side effects:
 *	The Notifier is created when necessary.
 *
 *----------------------------------------------------------------------
 */

public static synchronized Notifier
getNotifierForThread(
    Thread thread)		// The thread that owns this Notifier.
{
    if (globalNotifier == null) {
	globalNotifier = new Notifier(thread);
    } else if (globalNotifier.primaryThread != thread) {
	return null;
    } 
    return globalNotifier;
}

/*
 *----------------------------------------------------------------------
 *
 * preserve --
 *
 *	Increment the reference count of the notifier. The notifier will
 *	be kept in the notifierTable (and alive) as long as its reference
 *	count is greater than zero.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The refCount is incremented.
 *
 *----------------------------------------------------------------------
 */

public synchronized void
preserve()
{
    if (refCount < 0) {
	throw new TclRuntimeError("Attempting to preserve a freed Notifier");
    }
    ++refCount;
}

/*
 *----------------------------------------------------------------------
 *
 * release --
 *
 *	Decrement the reference count of the notifier. The notifier will
 *	be free when its refCount goes from one to zero.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	The notifier may be removed from the notifierTable when its
 *	refCount reaches zero.
 *
 *----------------------------------------------------------------------
 */

public synchronized void
release()
{
    if ((refCount == 0) && (primaryThread != null)) {
	throw new TclRuntimeError(
		"Attempting to release a Notifier before it's preserved");
    }
    if (refCount <= 0) {
	throw new TclRuntimeError("Attempting to release a freed Notifier");
    }
    --refCount;
    if (refCount == 0) {
	primaryThread = null;
	globalNotifier = null;
	dispose();
    }
}

/*
 *----------------------------------------------------------------------
 *
 * queueEvent --
 * 
 *	Insert an event into the event queue at one of three
 *	positions: the head, the tail, or before a floating marker.
 *	Events inserted before the marker will be processed in
 *	first-in-first-out order, but before any events inserted at
 *	the tail of the queue.  Events inserted at the head of the
 *	queue will be processed in last-in-first-out order.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	If this method is invoked by a non-primary thread, the
 *	primaryThread of this Notifier will be notified about the new
 *	event.
 *
 *----------------------------------------------------------------------
 */

public synchronized void
queueEvent(
    TclEvent evt,		// The event to put in the queue.
    int position)		// One of TCL.QUEUE_TAIL,
				// TCL.QUEUE_HEAD or TCL.QUEUE_MARK.
{
    evt.notifier = this;

    if (position == TCL.QUEUE_TAIL) {
	/*
	 * Append the event on the end of the queue.
	 */

	evt.next = null;

	if (firstEvent == null) {
	    firstEvent = evt;
	} else {
	    lastEvent.next = evt;
	}
	lastEvent = evt;
    } else if (position == TCL.QUEUE_HEAD) {
	/*
	 * Push the event on the head of the queue.
	 */

	evt.next = firstEvent;
	if (firstEvent == null) {
	    lastEvent = evt;
	}
	firstEvent = evt;
    } else if (position == TCL.QUEUE_MARK) {
	/*
	 * Insert the event after the current marker event and advance
	 * the marker to the new event.
	 */

	if (markerEvent == null) {
	    evt.next = firstEvent;
	    firstEvent = evt;
	} else {
	    evt.next = markerEvent.next;
	    markerEvent.next = evt;
	}
	markerEvent = evt;
	if (evt.next == null) {
	    lastEvent = evt;
	}
    } else {
	/*
	 * Wrong flag.
	 */

	throw new TclRuntimeError("wrong position \"" + position +
	       "\", must be TCL.QUEUE_HEAD, TCL.QUEUE_TAIL or TCL.QUEUE_MARK");
    }

    if (Thread.currentThread() != primaryThread) {
	alertNotifier();
    }
}

/*
 *----------------------------------------------------------------------
 *
 * deleteEvents --
 *
 *	Calls an EventDeleter for each event in the queue and deletes
 *	those for which deleter.deleteEvent() returns 1. Events
 *	for which the deleter returns 0 are left in the queue.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	Potentially removes one or more events from the event queue.
 *
 *----------------------------------------------------------------------
 */

public synchronized void
deleteEvents(
    EventDeleter deleter)	// The deleter that checks whether an event
				// should be removed.
{
    TclEvent evt, prev;

    for (prev = null, evt = firstEvent; evt != null; evt = evt.next) {
        if (deleter.deleteEvent(evt) == 1) {
            if (firstEvent == evt) {
                firstEvent = evt.next;
                if (evt.next == null) {
                    lastEvent = null;
                }
            } else {
                prev.next = evt.next;
            }
	    if (markerEvent == evt) {
		markerEvent = null;
	    }
        } else {
            prev = evt;
        }
    }
}

/*
 *----------------------------------------------------------------------
 *
 * serviceEvent --
 *
 *	Process one event from the event queue.
 *
 * Results:
 *	The return value is 1 if the procedure actually found an event
 *	to process. If no processing occurred, then 0 is returned.
 *
 * Side effects:
 *	Invokes all of the event handlers for the highest priority
 *	event in the event queue.  May collapse some events into a
 *	single event or discard stale events.
 *
 *----------------------------------------------------------------------
 */

synchronized int
serviceEvent(
    int flags)			// Indicates what events should be processed.
				// May be any combination of TCL.WINDOW_EVENTS
				// TCL.FILE_EVENTS, TCL.TIMER_EVENTS, or other
				// flags defined elsewhere.  Events not
				// matching this will be skipped for processing
				// later.
{
    TclEvent evt, prev;

    /*
     * No event flags is equivalent to TCL_ALL_EVENTS.
     */
    
    if ((flags & TCL.ALL_EVENTS) == 0) {
	flags |= TCL.ALL_EVENTS;
    }

    /*
     * Loop through all the events in the queue until we find one
     * that can actually be handled.
     */

    for (evt = firstEvent; evt != null; evt = evt.next) {
	/*
	 * Call the handler for the event.  If it actually handles the
	 * event then free the storage for the event.  There are two
	 * tricky things here, both stemming from the fact that the event
	 * code may be re-entered while servicing the event:
	 *
	 * 1. Set the "isProcessing" field to true. This is a signal to
	 *    ourselves that we shouldn't reexecute the handler if the
	 *    event loop is re-entered.
	 * 2. When freeing the event, must search the queue again from the
	 *    front to find it.  This is because the event queue could
	 *    change almost arbitrarily while handling the event, so we
	 *    can't depend on pointers found now still being valid when
	 *    the handler returns.
	 */

	boolean b = evt.isProcessing;
	evt.isProcessing = true;

	if ((b == false) && (evt.processEvent(flags) != 0)) {
	    evt.isProcessed = true;
	    if (evt.needsNotify) {
		synchronized(evt) {
		    evt.notifyAll();
		}
	    }
	    if (firstEvent == evt) {
		firstEvent = evt.next;
		if (evt.next == null) {
		    lastEvent = null;
		}
		if (markerEvent == evt) {
		    markerEvent = null;
		}
	    } else {
		for (prev = firstEvent; prev.next != evt; prev = prev.next) {
		    /* Empty loop body. */
		}
		prev.next = evt.next;
		if (evt.next == null) {
		    lastEvent = prev;
		}
		if (markerEvent == evt) {
		    markerEvent = prev;
		}
	    }
	    return 1;
	} else {
	    /*
	     * The event wasn't actually handled, so we have to
	     * restore the isProcessing field to allow the event to be
	     * attempted again.
	     */

	    evt.isProcessing = b;
	}

	/*
	 * The handler for this event asked to defer it.  Just go on to
	 * the next event.
	 */

	continue;
    }
    return 0;
}

/*
 *----------------------------------------------------------------------
 *
 * doOneEvent --
 *
 *	Process a single event of some sort.  If there's no work to
 *	do, wait for an event to occur, then process it. May delay
 *	execution of process while waiting for an event, unless
 *	TCL.DONT_WAIT is set in the flags argument.  This routine
 *	should only be called from the primary thread for the
 *	notifier.
 *	
 * Results:
 *	The return value is 1 if the procedure actually found an event
 *	to process. If no processing occurred, then 0 is returned
 *	(this can happen if the TCL.DONT_WAIT flag is set or if there
 *	are no event handlers to wait for in the set specified by
 *	flags).
 *
 * Side effects:
 *	May delay execution of process while waiting for an event,
 *	unless TCL.DONT_WAIT is set in the flags argument. Event
 *	sources are invoked to check for and queue events. Event
 *	handlers may produce arbitrary side effects.
 *
 *----------------------------------------------------------------------
 */

public native int
doOneEvent(
    int flags);			// Miscellaneous flag values: may be any
				// combination of TCL.DONT_WAIT,
				// TCL.WINDOW_EVENTS, TCL.FILE_EVENTS,
				// TCL.TIMER_EVENTS, TCL.IDLE_EVENTS,
				// or others defined by event sources.

/*
 *----------------------------------------------------------------------
 *
 * alertNotifier --
 *
 *	Wake up the C notifier.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	May cause the C level event loop to wake up.
 *
 *----------------------------------------------------------------------
 */

private final native void
alertNotifier();

/*
 *----------------------------------------------------------------------
 *
 * init --
 *
 *	Initialize the C event source for the Java notifier.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private final native void
init();

/*
 *----------------------------------------------------------------------
 *
 * dispose --
 *
 *	Tear down the C event source for the Java notifier.
 *
 * Results:
 *	None.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

final native void
dispose();

/*
 *----------------------------------------------------------------------
 *
 * hasEvents --
 *
 *	Check to see if there are events waiting to be processed.
 *
 * Results:
 *	Returns true if there are events on the Notifier queue.
 *
 * Side effects:
 *	None.
 *
 *----------------------------------------------------------------------
 */

private final synchronized boolean
hasEvents()
{
    return (firstEvent != null);
}

} // end Notifier




