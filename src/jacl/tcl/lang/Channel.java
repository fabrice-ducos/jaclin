/*
 * Channel.java
 *
 * Copyright (c) 1997 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: Channel.java,v 1.13 2001/11/20 19:07:30 mdejong Exp $
 */

package tcl.lang;
import java.io.*;
import java.util.Hashtable;

/**
 * The Channel class provides functionality that will
 * be needed for any type of Tcl channel. It performs
 * generic reads, writes, without specifying how a
 * given channel is actually created. Each new channel
 * type will need to extend the abstract Channel class
 * and override any methods it needs to provide a
 * specific implementation for.
 */

abstract class Channel {

    /**
     * The read, write, append and create flags are set here.  The 
     * variables used to set the flags are found in the class TclIO.
     */

    protected int mode;

    /**
     * This is a unique name that sub-classes need to set.  It is used
     * as the key in the hashtable of registered channels (in interp).
     */

    private String chanName;

    /**
     * How many interpreters hold references to this IO channel?
     */

    protected int refCount = 0;

    /**
     * Generic Reader/Writer objects. A BufferedReader is required to gain
     * access to line oriented input inside the read() method.
     */

    protected BufferedReader reader = null;
    protected BufferedWriter writer = null;

    /**
     * Flag that is set on each read/write.  If the read/write encountered an EOF
     * then it's set to true, else false.
     */

    protected boolean eofCond = false;

    /**
     * Buffer size used when reading blocks of input.
     */

    protected static final int BUF_SIZE = 1024;

    /**
     * Set to false when channel is in non-blocking mode.
     */
    
    protected boolean blocking = true;

    /**
     * Read data from the Channel.
     * 
     * @param interp is used for TclExceptions.  
     * @param readType is used to specify the type of read (line, all, etc).
     * @param numBytes the number of byte to read (if applicable).
     * @return String of data that was read from the Channel (can not be null)
     * @exception TclException is thrown if read occurs on WRONLY channel.
     * @exception IOException is thrown when an IO error occurs that was not
     *                correctly tested for.  Most cases should be caught.
     */

    String read(Interp interp, int readType, int numBytes) 
            throws IOException, TclException {

        if ((mode & (TclIO.RDONLY|TclIO.RDWR)) == 0)
            throw new TclException(interp, "channel \"" + getChanName() +
                "\" wasn't opened for reading");

        eofCond = false;

        switch (readType) {
            case TclIO.READ_ALL: {
                char[] charArr = new char[BUF_SIZE];
                StringBuffer sbuf = new StringBuffer(BUF_SIZE);
                int numRead;

                while((numRead = reader.read(charArr, 0, BUF_SIZE)) != -1) {
                    sbuf.append(charArr,0, numRead);
                }
                eofCond = true;
                return sbuf.toString();
            }
            case TclIO.READ_LINE: {
                String line = reader.readLine();
                if (line == null) {
                    eofCond = true;
                    return "";
                } else {
                    // FIXME: Is it possible to check for EOF using
                    // reader.ready() when a whole line is returned
                    // but we ran into an EOF (no newline termination)?
                    return line;
                }
            }
            case TclIO.READ_N_BYTES: {
                char[] charArr = new char[numBytes];
                int numRead = reader.read(charArr, 0, numBytes);
                if (numRead == -1) {
                    eofCond = true;
                    return "";
                }
                return new String(charArr,0,numRead);
            }
            default : {
                throw new TclRuntimeError(
                    "Channel.read: Invalid read mode.");
            }
        }
    }

    /** 
     * Write data to the Channel
     * 
     * @param interp is used for TclExceptions.  
     * @param outStr the string to write to the sub-classed channel.
     */

    void write(Interp interp, String outStr)
	    throws IOException, TclException {

        if ((mode & (TclIO.WRONLY|TclIO.RDWR)) == 0)
            throw new TclException(interp, "channel \"" + getChanName() +
                "\" wasn't opened for writing");

        if (writer != null) {
            eofCond = false;

            try
            {
                writer.write(outStr);
            }
            catch (EOFException e)
            {
                eofCond = true;
                throw e;
            }
        }
    }

    /** 
     * Close the Channel.  The channel is only closed, it is 
     * the responsibility of the "closer" to remove the channel from 
     * the channel table.
     */

    void close() throws IOException {

        IOException ex = null;

        if (reader != null) {
            try { reader.close(); } catch (IOException e) { ex = e; }
            reader = null;
        }
        if (writer != null) {
           try { writer.close(); } catch (IOException e) { ex = e; }
            writer = null;
        }

        if (ex != null)
            throw ex;
    }

    /** 
     * Flush the Channel.
     *
     * @exception TclException is thrown when attempting to flush a 
     *            read only channel.
     * @exception IOEcception is thrown for all other flush errors.
     */

    void flush(Interp interp) 
            throws IOException, TclException {

        if ((mode & (TclIO.WRONLY|TclIO.RDWR)) == 0)
            throw new TclException(interp, "channel \"" + getChanName() +
                    "\" wasn't opened for writing");

        if (writer != null) {
            try {
                writer.flush();
            } catch (EOFException e) {
                // FIXME: We need to clear eofCond on next write if set!
                eofCond = true;
                throw e;
            }
        }
    }

    /** 
     * Move the current file pointer. If seek is not supported on the
     * given channel then -1 will be returned. A subclass should
     * override this method if it supports the seek operation.
     * 
     * @param interp currrent interpreter.
     * @param offset The number of bytes to move the file pointer.
     * @param mode where to begin incrementing the file pointer; beginning,
     *             current, end.
     */

    void seek(Interp interp, long offset, int mode)
            throws IOException, TclException {
        throw new TclPosixException(interp, TclPosixException.EINVAL, true,
		"error during seek on \"" + getChanName() + "\"");
    }

    /** 
     * Return the current file pointer. If tell is not supported on the
     * given channel then -1 will be returned. A subclass should override
     * this method if it supports the tell operation.
     */

    long tell() throws IOException {
        return (long) -1;
    }

    /**
     * Returns true if the last read reached the EOF.
     */

    final boolean eof() {
        return eofCond;
    }

    /** 
     * Gets the chanName that is the key for the chanTable hashtable.
     * @return channelId
     */

    String getChanName() {
        return chanName;
    }


    /** 
     * Sets the chanName that is the key for the chanTable hashtable.
     * @param chan the unique channelId
     */

    void setChanName(String chan) {
        chanName = chan;
    }


    /** 
     * Gets the mode that is the read-write etc settings for this channel.
     * @return mode
     */

    int getMode() {
        return mode;
    }


    /** 
     * Query blocking mode.
     */

    boolean getBlocking() {
        return blocking;
    }


    /** 
     * Set blocking mode.
     */

    void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

}
