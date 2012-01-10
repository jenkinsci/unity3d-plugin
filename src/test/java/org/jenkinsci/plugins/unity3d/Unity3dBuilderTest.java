package org.jenkinsci.plugins.unity3d;

import hudson.util.ArgumentListBuilder;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author Jerome Lacoste
 */
public class Unity3dBuilderTest {

    private String exe = "/Applications/Unity/Unity.app";

    private String argLine;

    private List<String> expectedArgs;

    @Test
    public void typicalExecuteMethodArguments() {
        argLine = "-quit -batchmode -nographics -executeMethod ExecuteClass.ExecuteMethod";
        expectedArgs = asList(exe, "-quit", "-batchmode", "-nographics", "-executeMethod", "ExecuteClass.ExecuteMethod");
        ensureCreateCommandlineArgs(expectedArgs);
    }

    @Test
    public void buildWindowsPlayer() {
        argLine = "-buildWindowsPlayer \"C:\\Temp\\The Win32.exe\"";
        expectedArgs = asList(exe, "-buildWindowsPlayer", "C:\\Temp\\The Win32.exe");
        ensureCreateCommandlineArgs(expectedArgs);
    }

    @Test
    public void buildOSXPlayer() {
        argLine = "-buildOSXPlayer the\\ dir.app";
        expectedArgs = asList(exe, "-buildOSXPlayer", "the dir.app");
        ensureCreateCommandlineArgs(expectedArgs);
    }

    private void ensureCreateCommandlineArgs(List<String> expectedArgs1) {
        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine);
        ArgumentListBuilder commandlineArgs = builder.createCommandlineArgs(exe);
        assertEquals(expectedArgs1, commandlineArgs.toList());
    }
}
