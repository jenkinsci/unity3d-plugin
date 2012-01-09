package org.jenkinsci.plugins.unity3d.io;

import hudson.Launcher;
import hudson.remoting.RemoteOutputStream;

import java.io.*;

/**
 * A Pipe that works for distributed and non distributed scenarios.
 * Jenkins's Pipe doesn't work for non distributed scenarios.
 * @author Jerome Lacoste
 */
public class Pipe {
    private InputStream in;
    private OutputStream os;

    public Pipe(InputStream is, OutputStream os) {
        this.in = is;
        this.os = os;
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return os;
    }

    public static Pipe createRemoteToLocal(Launcher launcher) throws IOException {
        PipedInputStream is = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream(is);

        boolean isLocal = launcher instanceof Launcher.LocalLauncher;
        OutputStream os = isLocal ? pos : new RemoteOutputStream(pos);
        return new Pipe(is, os);
    }
}
