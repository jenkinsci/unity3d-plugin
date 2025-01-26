package org.jenkinsci.plugins.unity3d.io;

import hudson.Launcher;
import hudson.remoting.FastPipedInputStream;
import hudson.remoting.FastPipedOutputStream;
import hudson.remoting.RemoteOutputStream;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A Pipe that works for distributed and non distributed scenarios.
 * Jenkins's Pipe doesn't work for non distributed scenarios.
 *
 * Note: that java.io.Piped*Stream are not thread friendly and cause issues like JENKINS-23958.
 * See comments in the issue for details.
 *
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

    /*
    // This breaks our PipeTest
    public static hudson.remoting.Pipe createRemoteToLocal3(Launcher launcher) throws IOException {
        return hudson.remoting.Pipe.createRemoteToLocal();
    }
    */

    public static Pipe createRemoteToLocal(Launcher launcher) throws IOException {
        FastPipedInputStream is = new FastPipedInputStream();
        FastPipedOutputStream pos = new FastPipedOutputStream(is);

        boolean isLocal = launcher instanceof Launcher.LocalLauncher;
        OutputStream os = isLocal ? pos : new RemoteOutputStream(pos);
        return new Pipe(is, os);
    }
}
