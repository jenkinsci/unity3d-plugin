package org.jenkinsci.plugins.unity3d;

import hudson.util.ArgumentListBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Unity3dBuilderTest {

    private String unity3dName = "Unity 3.5";
    private String exe = "/Applications/Unity/Unity.app";
    private String moduleRootRemote = "C:\\Jenkins\\Workspace\\Project1";

    private Unity3dBuilder builder;
    private String argLine;

    private List<String> expectedArgs;

    @Test
    public void typicalExecuteMethodArguments() {
        argLine = "-quit -batchmode -nographics -executeMethod ExecuteClass.ExecuteMethod";
        expectedArgs = Arrays.asList(exe, "-quit", "-batchmode", "-nographics", "-executeMethod", "ExecuteClass.ExecuteMethod");
        ensureCreateArgs();
    }

    @Test
    public void buildWindowsPlayer() {
        argLine = "-buildWindowsPlayer \"C:\\Temp\\The Win32.exe\"";
        expectedArgs = Arrays.asList(exe, "-buildWindowsPlayer", "C:\\Temp\\The Win32.exe");
        ensureCreateArgs();
    }

    //@Test
    public void buildOSXPlayer() {
        argLine = "-buildOSXPlayer the\\ dir.app";
        expectedArgs = Arrays.asList(exe, "-buildOSXPlayer", "the dir.app");
        ensureCreateArgs();
    }

    private void ensureCreateArgs() {
        builder = new Unity3dBuilder(unity3dName, argLine);
        ArgumentListBuilder commandlineArgs = builder.createCommandlineArgs(exe);
        assertEquals(expectedArgs, commandlineArgs.toList());
    }
}
