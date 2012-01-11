package org.jenkinsci.plugins.unity3d.logs;

import org.jenkinsci.plugins.unity3d.logs.block.Block;
import org.jenkinsci.plugins.unity3d.logs.block.MatchedBlock;
import org.jenkinsci.plugins.unity3d.logs.block.UnityBlockList;
import org.jenkinsci.plugins.unity3d.logs.line.Line;
import org.jenkinsci.plugins.unity3d.logs.line.UnityLineList;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User: clement.dagneau
 * Date: 15/12/2011
 * Time: 16:02
 */
public class EditorLogParserImpl implements EditorLogParser {
    private final Stack<MatchedBlock> blockStack = new Stack<MatchedBlock>();
    private LogListener listener;

    interface LogListener {
        void activityStarted(MatchedBlock block);
        void activityFinished(MatchedBlock block);
        void logMessage(String line, Line.Type type);
    }

    public void setListener(LogListener listener) {
        this.listener = listener;
    }

    EditorLogParserImpl() {
        for (Block block : UnityBlockList.editorLogBlocks)
            block.init();
    }


    public void logActivityStart(MatchedBlock block)
    {
        if (listener != null)
           listener.activityStarted(block);
    }

    public void logActivityEnd(MatchedBlock block)
    {
        if (listener != null)
            listener.activityFinished(block);
    }

    private void logBlockStart(MatchedBlock block) {
        logActivityStart(block);
        blockStack.push(block);
    }

    private void logBlockEnd() {
        logActivityEnd(blockStack.pop());
    }


    public void log(String message) {
        // Check if new message is the end of the current block (if it exists).
        if (!blockStack.empty()) {
            Block.MatchType match = blockStack.peek().matchesEnd(message);

            if (match == Block.MatchType.Inclusive) {
                // include this line in the block
                logLine(message);
                logBlockEnd();
                return;

            } else if (match == Block.MatchType.Exclusive) {
                logBlockEnd();
            }
        }

        // Check if line is the beginning of a new block.
        for (Block block : UnityBlockList.editorLogBlocks) {

            MatchedBlock matchedBlock = block.matchesBeginning(message);
            if (null != matchedBlock) {

                if (matchedBlock.matchType == Block.MatchType.Inclusive) {

                    logBlockStart(matchedBlock);
                    break;

                } else if (matchedBlock.matchType == Block.MatchType.Exclusive) {

                    // exclude the line from the block, so log it out now
                    logLine(message);
                    logBlockStart(matchedBlock);
                    return;
                }
            }
        }

        // no blocks starting/ending so just log out!
        logLine(message);
    }


    private void logLine(String message) {
        // Now check message
        for (Line line : UnityLineList.lines) {
            if (line.matches(message)) {
                log(message, line.getType());
                return;
            }
        }

        // There is not match. Just log a regular message.
        log(message, Line.Type.Normal);
    }

    private void log(String message, Line.Type type) {
        if (listener != null)
            listener.logMessage(message, type);
    }

    public void logException(Exception e) {
        final Writer stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));

        log("Exception: " + stackTrace.toString(), Line.Type.Failure);
    }
}
