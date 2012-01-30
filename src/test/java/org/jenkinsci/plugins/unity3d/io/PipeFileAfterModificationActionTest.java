package org.jenkinsci.plugins.unity3d.io;

import com.google.common.io.Files;
import hudson.util.ByteArrayOutputStream2;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

/**
 * @author Jerome Lacoste
 */
public class PipeFileAfterModificationActionTest {
    private String originalContent = "The original content of the file\n" 
            + "Multiple lines of \n"
            + "Build information";

    private String newContent = "The new content of the file\n" 
            + "Multiple lines of \n";

    private String newContent2 = "Build information";

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    @Test
    public void simulateEditorLogRewritten() throws Exception {
        testRewriteFile(0);
    }

    @Test
    public void simulateEditorLogSlowlyMoved() throws Exception {
        testRewriteFile(100);
    }

    private void testRewriteFile(int timeToWaitAfterRename) throws IOException, InterruptedException {
        // Given
        File fakeEditorLog = File.createTempFile("fake_editor", "log");
        Files.write(originalContent, fakeEditorLog, UTF_8);

        ByteArrayOutputStream2 collectedContent = new ByteArrayOutputStream2();

        final PipeFileAfterModificationAction task = new PipeFileAfterModificationAction(fakeEditorLog.getAbsolutePath(), collectedContent, true);
        final AtomicLong nbBytesRead = new AtomicLong();
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Long wrote = task.call();
                    nbBytesRead.compareAndSet(0, wrote);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        // simulate Editor.log being rewritten
        Thread.sleep(40);
        File prevEditorLog = File.createTempFile("fake_editor", "log");
        fakeEditorLog.renameTo(prevEditorLog);

        Thread.sleep(timeToWaitAfterRename);

        Files.write(newContent, fakeEditorLog, UTF_8);
        Thread.sleep(20);
        Files.append(newContent2, fakeEditorLog, UTF_8);
        Thread.sleep(80);
        String expectedContent = newContent + newContent2;

        // simulate remote cancellation. Using the remoting API, we cancel the task and this interrupts the remote thread
        t.interrupt();
        // give us the time to terminate properly the task
        Thread.sleep(50);

        assertEquals(expectedContent, new String(collectedContent.getBuffer(), UTF_8));

        Long read = (long) expectedContent.length();
        assertEquals(read, (Long) nbBytesRead.get());
    }
}
