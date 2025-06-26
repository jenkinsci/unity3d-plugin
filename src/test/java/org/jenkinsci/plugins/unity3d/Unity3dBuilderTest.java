package org.jenkinsci.plugins.unity3d;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import hudson.EnvVars;
import hudson.util.ArgumentListBuilder;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jerome Lacoste
 */
public class Unity3dBuilderTest {

    private String exe = "/Applications/Unity/Unity.app";
    private String moduleRootRemote = "/Users/Shared/Jenkins/Home/jobs/project1/workspace";

    private String argLine;

    private List<String> expectedArgs;

    @Test
    public void typicalExecuteMethodArgumentsAddMissingProjectPath() {
        argLine = "-quit -batchmode -nographics -executeMethod ExecuteClass.ExecuteMethod";
        expectedArgs = asList(exe, "-projectPath", moduleRootRemote, "-quit", "-batchmode", "-nographics", "-executeMethod", "ExecuteClass.ExecuteMethod");
        ensureCreateCommandlineArgs(expectedArgs);
    }

    @Test
    public void typicalExecuteMethodArgumentsWithCustomProjectPath() {
        argLine = "-quit -batchmode -nographics -executeMethod ExecuteClass.ExecuteMethod -projectPath XXXX";
        expectedArgs = asList(exe, "-quit", "-batchmode", "-nographics", "-executeMethod", "ExecuteClass.ExecuteMethod", "-projectPath", "XXXX");
        ensureCreateCommandlineArgs(expectedArgs);
    }

    @Test
    public void buildWindowsPlayerAddMissingProjectPath() {
        argLine = "-buildWindowsPlayer \"C:\\Temp\\The Win32.exe\"";
        expectedArgs = asList(exe, "-projectPath", moduleRootRemote, "-buildWindowsPlayer", "C:\\Temp\\The Win32.exe");
        ensureCreateCommandlineArgs(expectedArgs);
    }

    @Test
    public void buildOSXPlayerAddMissingProjectPath() {
        argLine = "-buildOSXPlayer the\\ dir.app";
        expectedArgs = asList(exe, "-projectPath", moduleRootRemote, "-buildOSXPlayer", "the dir.app");
        ensureCreateCommandlineArgs(expectedArgs);
    }

    private void ensureCreateCommandlineArgs(List<String> expectedArgs1) {
        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine, "");
        ArgumentListBuilder commandlineArgs = builder.createCommandlineArgs(exe, moduleRootRemote, new EnvVars(), new Hashtable<>());
        assertEquals(expectedArgs1, commandlineArgs.toList());
    }

    @Test
    public void environmentAndBuildVariablesParsing() {
    	EnvVars vars = new EnvVars();
        vars.put("param1", "value1");
        vars.put("param2", "value2");

        String param2overwrittenValue = "overwrittenValue";

        Map<String,String> buildParameters = new Hashtable<>();
        buildParameters.put("param2", param2overwrittenValue);

        argLine = "-param1 $param1 -param2 $param2 -projectPath XXXX";
        expectedArgs = asList(exe, "-param1", "value1", "-param2", param2overwrittenValue, "-projectPath", "XXXX");

        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine, "");
        ArgumentListBuilder commandlineArgs = builder.createCommandlineArgs(exe, moduleRootRemote, vars, buildParameters);
        assertEquals(expectedArgs, commandlineArgs.toList());
        assertEquals("Serialized arg line not modified", argLine, builder.getArgLine());
    }

    @Test
    public void environmentAndBuildVariablesParsingWithEnvVarsThatReferencesBuildParameters() {
        EnvVars vars = new EnvVars();
        vars.put("ARGS", "-projectPath $param");

        Map<String,String> buildParameters = new Hashtable<>();
        buildParameters.put("param", "XXXX");

        argLine = "-p1 v1 $ARGS";
        expectedArgs = asList(exe, "-p1", "v1", "-projectPath", "XXXX");

        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine, "");
        ArgumentListBuilder commandlineArgs = builder.createCommandlineArgs(exe, moduleRootRemote, vars, buildParameters);
        assertEquals(expectedArgs, commandlineArgs.toList());
        assertEquals("Serialized arg line not modified", argLine, builder.getArgLine());
    }


    @Test
    public void unstableErrorCodesParsing() throws Exception {
        ensureUnstableReturnCodesParsingWorks(new Integer[]{}, "");
        ensureUnstableReturnCodesParsingWorks(new Integer[]{2, 3}, "2,3");
        ensureUnstableReturnCodesParsingWorks(new Integer[]{-1}, "-1");
        ensureUnstableReturnCodesParsingWorks(new Integer[]{2, 3}, "2, 3");
        ensureUnstableReturnCodesParsingWorks(new Integer[]{2, 3}, " 2 ,3 ");
        ensureUnstableReturnCodesParsingFails(" 2 , ,,");
    }

    private void ensureUnstableReturnCodesParsingWorks(Integer[] expectedResultCodes, String unstableReturnCodes) {
        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine, unstableReturnCodes);
        assertEquals(new HashSet<>(asList(expectedResultCodes)), builder.toUnstableReturnCodesSet());
    }
    private void ensureUnstableReturnCodesParsingFails(String unstableReturnCodes) {
        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine, unstableReturnCodes);
        try {
            builder.toUnstableReturnCodesSet();
            Assert.fail("Expected failure");
        } catch (Exception expected) {
            //
        }
    }
}
