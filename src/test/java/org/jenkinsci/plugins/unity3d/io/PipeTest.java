package org.jenkinsci.plugins.unity3d.io;

import hudson.Launcher;
import hudson.remoting.Future;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;
import hudson.util.StreamCopyThread;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import jenkins.security.MasterToSlaveCallable;
import org.jvnet.hudson.test.HudsonTestCase;
/**
 * This test was written to find a solution to the piping issue.
 * The Pipe class in Jenkins 1.446 doesn't work properly with asyncCall if the master and slave are on the same machine.
 *
 * See ProcTest in Jenkins for similar tests.
 *
 * @author Jerome Lacoste
 */
public class PipeTest extends HudsonTestCase implements Serializable {

    private VirtualChannel createSlaveChannel() throws Exception {
        DumbSlave s = createSlave();
        s.toComputer().connect(false).get();
        VirtualChannel ch=null;
        while (ch==null) {
            ch = s.toComputer().getChannel();
            Thread.sleep(100);
        }
        return ch;
    }

    public void testPipingFromRemoteWithLocalLaunch() throws Exception {
        doPipingFromRemoteTest(new Launcher.LocalLauncher(
                new StreamTaskListener(System.out, Charset.defaultCharset())));
    }

    private static boolean isRunningOnWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    public void testPipingFromRemoteWithRemoteLaunch() throws Exception {
        // Windows cant delete open log files, so ignore this test because of
        // java.io.IOException: Unable to delete <templogfile>...
        if (isRunningOnWindows())
            return;

        doPipingFromRemoteTest(new Launcher.RemoteLauncher(
                new StreamTaskListener(System.out, Charset.defaultCharset()),
                createSlaveChannel(), true));
    }

    private void doPipingFromRemoteTest(Launcher l) throws IOException, InterruptedException, ExecutionException {
        Pipe pipe = Pipe.createRemoteToLocal(l);
        Future<String> piping = l.getChannel().callAsync(new PipingCallable(pipe.getOut()));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Thread t = new StreamCopyThread("Test", pipe.getIn(), os);
        t.start();
        
        assertEquals("DONE", piping.get());
        t.join();
        assertEquals("Hello", os.toString());

    }

    private static class PipingCallable extends MasterToSlaveCallable<String, Throwable> {
        private final OutputStream out;

        public PipingCallable(OutputStream out) {
            this.out = out;
        }

        public String call() throws Throwable {
            if (out == null) {
                throw new IllegalStateException("null output");
            }
            out.write("Hello".getBytes());
            out.close();
            return "DONE";
        }
    }
}
