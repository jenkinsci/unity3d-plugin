package org.jenkinsci.plugins.unity3d.logs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.jenkinsci.plugins.unity3d.logs.block.MatchedBlock;
import org.jenkinsci.plugins.unity3d.logs.line.Line;
import org.junit.jupiter.api.Test;

/**
 * Created by IntelliJ IDEA.
 * User: lacostej
 * Date: 1/11/12
 * Time: 8:11 AM
 * To change this template use File | Settings | File Templates.
 */
class EditorLogParserImplTest {

    private static final EditorLogParserImpl PARSER = new EditorLogParserImpl();

    @Test
    void testLog() throws Exception {
        EditorLogParserImpl.LogListener listener = new EditorLogParserImpl.LogListener() {
            public void activityStarted(MatchedBlock block) {
                System.out.println("BLOCK START: " + block.getName());
            }

            public void activityFinished(MatchedBlock block) {
                System.out.println("BLOCK END: " + block.getName());
            }

            public void logMessage(String line, Line.Type type) {
                if (type != Line.Type.Normal) {
                    System.out.println("=== " + type + " => " + line);
                }
            }
        };
        PARSER.setListener(listener);
        try (InputStream is = findResource("/example_Editor.log");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                PARSER.log(line);
            }
        }
    }

    private InputStream findResource(String resourceName) {
        return this.getClass().getResourceAsStream(resourceName);
    }
}
