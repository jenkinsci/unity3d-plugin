package org.jenkinsci.plugins.unity3d.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.util.ByteArrayOutputStream2;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * @author Jerome Lacoste
 */
class PipeFileAfterModificationActionTest {

    private static final String ORIGINAL_CONTENT =
            """
            The original content of the file
            Multiple lines of\s
            Build information""";

    private static final String NEW_CONTENT =
            """
            The new content of the file
            Multiple lines of\s
            """;

    private static final String NEW_CONTENT_2 = "Build information";

    @Test
    void simulateEditorLogRewritten() throws Exception {
        testRewriteFile(0);
    }

    @Test
    void simulateEditorLogSlowlyMoved() throws Exception {
        testRewriteFile(100);
    }

    private void testRewriteFile(int timeToWaitAfterRename) throws Exception {
        // Given
        File fakeEditorLog = File.createTempFile("fake_editor", "log");
        Files.writeString(fakeEditorLog.toPath(), ORIGINAL_CONTENT, StandardCharsets.UTF_8);

        ByteArrayOutputStream2 collectedContent = new ByteArrayOutputStream2();

        final PipeFileAfterModificationAction task =
                new PipeFileAfterModificationAction(fakeEditorLog.getAbsolutePath(), collectedContent, true);
        final AtomicLong nbBytesRead = new AtomicLong();
        Thread t = new Thread(() -> {
            try {
                Long wrote = task.call();
                nbBytesRead.compareAndSet(0, wrote);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t.start();

        // simulate Editor.log being rewritten
        Thread.sleep(40);
        File prevEditorLog = File.createTempFile("fake_editor", "log");
        fakeEditorLog.renameTo(prevEditorLog);

        Thread.sleep(timeToWaitAfterRename);

        Files.writeString(fakeEditorLog.toPath(), NEW_CONTENT, StandardCharsets.UTF_8);
        Thread.sleep(20);
        Files.writeString(fakeEditorLog.toPath(), NEW_CONTENT_2, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        Thread.sleep(80);
        String expectedContent = NEW_CONTENT + NEW_CONTENT_2;

        // simulate remote cancellation. Using the remoting API, we cancel the task and this interrupts the remote
        // thread
        t.interrupt();
        // give us the time to terminate properly the task
        Thread.sleep(50);

        assertEquals(expectedContent, new String(collectedContent.getBuffer(), StandardCharsets.UTF_8));

        Long read = (long) expectedContent.length();
        assertEquals(read, (Long) nbBytesRead.get());
    }
}
