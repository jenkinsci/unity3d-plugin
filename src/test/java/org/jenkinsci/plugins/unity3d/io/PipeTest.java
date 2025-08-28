package org.jenkinsci.plugins.unity3d.io;

import static hudson.Functions.isWindows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import hudson.Launcher;
import hudson.remoting.Future;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;
import hudson.util.StreamCopyThread;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import jenkins.security.MasterToSlaveCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * This test was written to find a solution to the piping issue.
 * The Pipe class in Jenkins 1.446 doesn't work properly with asyncCall if the master and slave are on the same machine.
 *
 * See ProcTest in Jenkins for similar tests.
 *
 * @author Jerome Lacoste
 */
@WithJenkins
class PipeTest {

    private JenkinsRule r;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        r = rule;
    }

    private VirtualChannel createSlaveChannel() throws Exception {
        DumbSlave s = r.createSlave();
        s.toComputer().connect(false).get();
        VirtualChannel ch = null;
        while (ch == null) {
            ch = s.toComputer().getChannel();
            Thread.sleep(100);
        }
        return ch;
    }

    @Test
    void testPipingFromRemoteWithLocalLaunch() throws Exception {
        doPipingFromRemoteTest(
                new Launcher.LocalLauncher(new StreamTaskListener(System.out, Charset.defaultCharset())));
    }

    @Test
    void testPipingFromRemoteWithRemoteLaunch() throws Exception {
        assumeFalse(
                isWindows(),
                "Windows cant delete open log files, so ignore this test because of java.io.IOException: Unable to delete <templogfile>");

        doPipingFromRemoteTest(new Launcher.RemoteLauncher(
                new StreamTaskListener(System.out, Charset.defaultCharset()), createSlaveChannel(), true));
    }

    private void doPipingFromRemoteTest(Launcher l) throws Exception {
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
