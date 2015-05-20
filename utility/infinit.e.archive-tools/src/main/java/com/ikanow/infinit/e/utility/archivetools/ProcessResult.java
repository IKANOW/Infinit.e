package com.ikanow.infinit.e.utility.archivetools;

import com.google.common.io.ByteStreams;

import java.io.IOException;

/**
 * Created by Mike Grill on 3/13/15.
 * Organized storage for the results from running a Process.
 */
public class ProcessResult {

    private byte[] _stdOut;
    private byte[] _stdErr;
    private int _code;

    /**
     * Get the result from a process
     * @param p Process to get results from
     * @throws IOException
     * @throws InterruptedException
     */
    public ProcessResult( Process p ) throws IOException, InterruptedException {
        this(
            ByteStreams.toByteArray(p.getInputStream()),
            ByteStreams.toByteArray(p.getErrorStream()),
            p.waitFor()
        );
    }

    public ProcessResult( byte[] stdout, byte[] stderr, int code ){
        this._stdOut = stdout;
        this._stdErr = stderr;
        this._code = code;
    }

    public boolean hasStdOut(){ return this._stdOut.length > 0; }
    public byte[] getStdOut(){ return this._stdOut; }

    public boolean hasStdErr(){ return this._stdErr.length > 0; }
    public String getStdErr(){ return new String( this._stdErr ); }

    public int getExitCode(){
        return _code;
    }
}
