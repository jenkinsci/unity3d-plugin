package org.jenkinsci.plugins.unity3d.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

/**
 * A Callable that waits until a file has been created or modified.
 * @author Jerome Lacoste
 */
public class DetectFileCreatedOrModifiedAction implements Callable<File> {
    private String path;
    private long origLastModified;
    private boolean origExists;

    public DetectFileCreatedOrModifiedAction(String path) {
        this.path = path;
        File orig = new File(path);
        origExists = orig.exists();
        origLastModified = origExists ? orig.lastModified() : 0;
    }

    public File call() throws FileNotFoundException {
        while (true) {
            File file = new File(path);
            if (hasChanged(file)) {
                return file;
            }
            synchronized (this) {
                try {
                    wait(50);
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
    }

    private boolean hasChanged(File file) {
        if (!origExists) return file.exists();
        else return file.lastModified() > origLastModified;
    }
}
