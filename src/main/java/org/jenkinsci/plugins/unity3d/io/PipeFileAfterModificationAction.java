package org.jenkinsci.plugins.unity3d.io;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;

/**
 * A Callable that allows piping the output of a file into.
 * <p>
 * Useful when the file is been modified and one needs to loop.
 * <p>
 * Note that you can interrupt this task to cancel it.
 *
 * @author Jerome Lacoste
 */
public class PipeFileAfterModificationAction implements Callable<Long> {
    private final String path;
    private final OutputStream out;
    private final boolean closeOut;
    private final int waitBetweenCopyLoops;

    public PipeFileAfterModificationAction(String path, OutputStream out, boolean closeOut) {
        this.path = path;
        if (out == null) {
            throw new NullPointerException("out is null");
        }
        this.out = out;
        this.closeOut = closeOut;
        waitBetweenCopyLoops = 50;
    }

    public PipeFileAfterModificationAction(String path, OutputStream out) {
        this(path, out, false);
    }

    /**
     * Wait until the file has been modified and then copy its contents into the output, looping repeatedly as the file is been modified.
     * @return the number of bytes copied
     * @throws IOException
     */
    public Long call() throws IOException {
        File file = new DetectFileCreatedOrModifiedAction(path).call();
        long pos = 0;
        if (file != null) {
            RandomAccessFile raf = null;
            try {
                while (true) {
                    if (file.exists()) {
                        // I once experienced a FileNotFoundException here, let's see if this happens again
                        raf = new RandomAccessFile(path, "r");
                        pos = continueCopyingFrom(raf, pos);
                        raf.close();
                    }
                    synchronized (this) {
                        try {
                            wait(waitBetweenCopyLoops);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace(new PrintStream(out));
            } finally {
                if (raf != null) {
                    try {
                        pos = raf.getFilePointer();
                    } catch (IOException ignore) {
                    }
                    try {
                        raf.close();
                    } catch (IOException ignore) {
                    }
                }
                if (closeOut)
                    out.close();
            }
        }
        return pos;
    }

    private long continueCopyingFrom(RandomAccessFile raf, long from) throws IOException {
        raf.seek(from);

        byte[] buf = new byte[8192];
        int len;
        while ((len = raf.read(buf)) > 0)
            out.write(buf, 0, len);

        return raf.getFilePointer();
    }

}
