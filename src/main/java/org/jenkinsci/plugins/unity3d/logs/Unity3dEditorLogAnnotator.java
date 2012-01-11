/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.unity3d.logs;

import hudson.console.LineTransformationOutputStream;
import org.jenkinsci.plugins.unity3d.logs.block.MatchedBlock;
import org.jenkinsci.plugins.unity3d.logs.line.Line;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Filter {@link java.io.OutputStream} that places an annotation that marks Ant target execution.
 *
 * @author Jerome Lacoste
 */
public class Unity3dEditorLogAnnotator extends LineTransformationOutputStream implements EditorLogParserImpl.LogListener {
    private final OutputStream out;
    private final Charset charset;

    private EditorLogParser logParser;

    public Unity3dEditorLogAnnotator(OutputStream out, Charset charset) {
        this.out = out;
        this.charset = charset;
        this.logParser = new EditorLogParserImpl();
    }

    public void activityStarted(MatchedBlock block) {
        
    }

    public void activityFinished(MatchedBlock block) {
        
    }

    public void logMessage(String line, Line.Type type) {
        switch (type) {
            case Normal:
                //out.write();
                break;
            case Warning:
                break;
            case Failure:
                break;
            case Error:
                break;
        }
    }

    @Override
    protected void eol(byte[] b, int len) throws IOException {
        String line = charset.decode(ByteBuffer.wrap(b, 0, len)).toString();

        // trim off CR/LF from the end
        line = trimEOL(line);

        handle(line);
        
        out.write(b,0,len);
    }

    private void handle(String line) {
       logParser.log(line);
    }

    private boolean endsWith(String line, char c) {
        int len = line.length();
        return len>0 && line.charAt(len-1)==c;
    }

    @Override
    public void close() throws IOException {
        super.close();
        out.close();
    }

}
