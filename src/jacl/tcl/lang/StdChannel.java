/*
 * StdChannel.java --
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: StdChannel.java,v 1.11 2001/11/20 00:08:29 mdejong Exp $
 *
 */

package tcl.lang;
import java.util.*;
import java.io.*;

/**
 * Subclass of the abstract class Channel.  It implements all of the 
 * methods to perform read, write, open, close, etc on system stdio channels.
 */

class StdChannel extends Channel {
  
    /**
     * stdType store which type, of the three below, this StdChannel is.
     */

    private int stdType = -1;

    /**
     * Flags indicating the type of this StdChannel.
     */

    static final int STDIN    = 0;
    static final int STDOUT   = 1; 
    static final int STDERR   = 2; 

    /**
     * Constructor that does nothing.  Open() must be called before
     * any of the subsequent read, write, etc calls can be made.
     */

    StdChannel() {}

    /**
     * Constructor that will automatically call open.
     *
     * @param stdName name of the stdio channel; stdin, stderr or stdout.
     */

    StdChannel(String stdName) {
	if (stdName.equals("stdin")) {
	    open(STDIN);
	} else if (stdName.equals("stdout")) {
	    open(STDOUT);
	} else if (stdName.equals("stderr")) {
	    open(STDERR);
	} else {
	    throw new TclRuntimeError(
                    "Error: unexpected type for StdChannel");
	}
    }


    StdChannel(int type) {
        open(type);
    }


    /**
     * Set the channel type to one of the three stdio types.  Throw a 
     * tclRuntimeEerror if the stdName is not one of the three types.  If
     * it is a stdin channel, initialize the "in" data member.  Since "in"
     * is static it may have already be initialized, test for this case 
     * first.  Set the names to fileX, this will be the key in the chanTable 
     * hashtable to access this object.  Note: it is not put into the hash 
     * table in this function.  The calling function is responsible for that.
     *
     * @param stdName String that equals stdin, stdout, stderr
     * @return The name of the channelId
     */

    String open(int type) {

        switch (type) {
	    case STDIN:
	        mode = TclIO.RDONLY;
		if (reader == null) {
		    reader = new BufferedReader(
                        new InputStreamReader(System.in)); 
		}
		break;
	    case STDOUT:
	        mode = TclIO.WRONLY;
		if (writer == null) {
		    writer = new BufferedWriter(
                        new OutputStreamWriter(System.out)); 
		}
		break;
	    case STDERR:
	        mode = TclIO.WRONLY;
		if (writer == null) {
		    writer = new BufferedWriter(
                        new OutputStreamWriter(System.err)); 
		}
		break;
	    default:
		throw new RuntimeException(
		    "type does not match one of STDIN, STDOUT, or STDERR");
	}

        stdType = type;
	setChanName("file" + type);
	
	return getChanName();
    }


    /**
     * Read data from a stdio channel. The read can be for the entire buffer,
     * line or a specified number of bytes.
     *
     * @param interp The currrent interpreter.
     * @param readType Specifies if the read should read the entire buffer, 
     *            the next line, or a specified number of bytes.  See the 
     *            TclIO class for more information on read types.
     * @param numBytes Number of bytes to read.  Only used when the readType
     *            is TclIO.READ_N_BYTES.
     * @return String of data that was read from the Channel (can not be null)
     * @exception TclException is thrown if read occurs on WRONLY channel.
     * @exception IOException is thrown when an IO error occurs that was not
     *                correctly tested for.  Most cases should be caught.
     */

    String read(Interp interp, int readType, int numBytes) 
            throws IOException, TclException {

        if (stdType != STDIN) {
	      throw new TclException(interp, "channel \"" +
	        getChanName() + "\" wasn't opened for reading");
	}

	eofCond = false;

	switch (readType) {
	    case TclIO.READ_ALL: {
		char[] charArr = new char[BUF_SIZE];
		StringBuffer sbuf = new StringBuffer();
		int numRead;
		    
		while((numRead = reader.read(charArr, 0, BUF_SIZE)) != -1) {
		    sbuf.append(charArr,0, numRead);
		}
		eofCond = true;
		return sbuf.toString();
	    } 
	    case TclIO.READ_LINE: {
                // FIXME: Can we check for EOF using reader.ready() ???
		String line = reader.readLine();
		if (line == null) {
		    eofCond = true;
		    return "";
		} else {
		    return line;
		}
	    }
	    case TclIO.READ_N_BYTES: {
		char[] charArr = new char[numBytes];
		int numRead;
		numRead = reader.read(charArr, 0, numBytes);
		if (numRead == -1) {
		    eofCond = true;
		    return "";
		}
		return( new String(charArr,0,numRead) );
	    }
	    default : {
	        throw new TclRuntimeError(
                        "StdChannel.read: Incorrect read mode.");
	    }
	}
    }


    /**
     * Write to stdout or stderr.  If the stdType is not set to 
     * STDOUT or STDERR this is an error; either the stdType wasnt
     * correctly initialized, or this was called on a STDIN channel.
     *
     * @param interp the current interpreter.
     * @param s the string to write 
     */

    void write(Interp interp, String s) 
            throws IOException, TclException {

        super.write(interp, s);

        // The OutputStreamWriter class will buffer even if you don't
        // wrap it in a BufferedWriter. The stderr file object must
        // not require an explicit flush so we just hack a flush in.
        if (stdType == STDERR)
            flush(interp);
    }
}
