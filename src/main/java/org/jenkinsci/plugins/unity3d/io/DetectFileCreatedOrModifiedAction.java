package org.jenkinsci.plugins.unity3d.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

/**
 * A Callable that waits until a file has been created or modified.
 * <p>
 * The task checks for changes in either the last modified timestamp (precise up to the second) or the file size to detect a change.
 * <p>
 * The task cannot currently be stopped, and its time between checks is hardcoded (50 msec).
 *
 * @author Jerome Lacoste
 */
public class DetectFileCreatedOrModifiedAction implements Callable<File> {
    private String path;
    private long origLastModified;
    private long origSize;
    private boolean origExists;
    private final int timeoutBetweenChecks;

    public DetectFileCreatedOrModifiedAction(String path) {
        this.path = path;
        File orig = new File(path);
        origExists = orig.exists();
        origLastModified = origExists ? orig.lastModified() : 0;
        origSize = origExists ? orig.length() : -1;
        timeoutBetweenChecks = 50;
    }

    public File call() throws FileNotFoundException {
        while (true) {
            File file = new File(path);
            if (hasChanged(file)) {
                return file;
            }
            synchronized (this) {
                try {
                    wait(timeoutBetweenChecks);
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
    }

    private boolean hasChanged(File file) {
        if (!origExists) return file.exists();
        else return file.length() != origSize || file.lastModified() > origLastModified;
    }
}
