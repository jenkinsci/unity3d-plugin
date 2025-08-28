package org.jenkinsci.plugins.unity3d;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.EnvVars;
import hudson.util.ArgumentListBuilder;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Jerome Lacoste
 */
class Unity3dBuilderTest {

    private static final String EXE = "/Applications/Unity/Unity.app";
    private static final String MODULE_ROOT_REMOTE = "/Users/Shared/Jenkins/Home/jobs/project1/workspace";

    @Test
    void typicalExecuteMethodArgumentsAddMissingProjectPath() {
        String argLine = "-quit -batchmode -nographics -executeMethod ExecuteClass.ExecuteMethod";
        List<String> expectedArgs = asList(
                EXE,
                "-projectPath",
                MODULE_ROOT_REMOTE,
                "-quit",
                "-batchmode",
                "-nographics",
                "-executeMethod",
                "ExecuteClass.ExecuteMethod");
        ensureCreateCommandlineArgs(argLine, expectedArgs);
    }

    @Test
    void typicalExecuteMethodArgumentsWithCustomProjectPath() {
        String argLine = "-quit -batchmode -nographics -executeMethod ExecuteClass.ExecuteMethod -projectPath XXXX";
        List<String> expectedArgs = asList(
                EXE,
                "-quit",
                "-batchmode",
                "-nographics",
                "-executeMethod",
                "ExecuteClass.ExecuteMethod",
                "-projectPath",
                "XXXX");
        ensureCreateCommandlineArgs(argLine, expectedArgs);
    }

    @Test
    void buildWindowsPlayerAddMissingProjectPath() {
        String argLine = "-buildWindowsPlayer \"C:\\Temp\\The Win32.exe\"";
        List<String> expectedArgs =
                asList(EXE, "-projectPath", MODULE_ROOT_REMOTE, "-buildWindowsPlayer", "C:\\Temp\\The Win32.exe");
        ensureCreateCommandlineArgs(argLine, expectedArgs);
    }

    @Test
    void buildOSXPlayerAddMissingProjectPath() {
        String argLine = "-buildOSXPlayer the\\ dir.app";
        List<String> expectedArgs = asList(EXE, "-projectPath", MODULE_ROOT_REMOTE, "-buildOSXPlayer", "the dir.app");
        ensureCreateCommandlineArgs(argLine, expectedArgs);
    }

    private void ensureCreateCommandlineArgs(String argLine, List<String> expectedArgs) {
        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine, "");
        ArgumentListBuilder commandlineArgs =
                builder.createCommandlineArgs(EXE, MODULE_ROOT_REMOTE, new EnvVars(), new Hashtable<>());
        assertEquals(expectedArgs, commandlineArgs.toList());
    }

    @Test
    void environmentAndBuildVariablesParsing() {
        EnvVars vars = new EnvVars();
        vars.put("param1", "value1");
        vars.put("param2", "value2");

        String param2overwrittenValue = "overwrittenValue";

        Map<String, String> buildParameters = new Hashtable<>();
        buildParameters.put("param2", param2overwrittenValue);

        String argLine = "-param1 $param1 -param2 $param2 -projectPath XXXX";
        List<String> expectedArgs =
                asList(EXE, "-param1", "value1", "-param2", param2overwrittenValue, "-projectPath", "XXXX");

        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine, "");
        ArgumentListBuilder commandlineArgs =
                builder.createCommandlineArgs(EXE, MODULE_ROOT_REMOTE, vars, buildParameters);
        assertEquals(expectedArgs, commandlineArgs.toList());
        assertEquals(argLine, builder.getArgLine(), "Serialized arg line not modified");
    }

    @Test
    void environmentAndBuildVariablesParsingWithEnvVarsThatReferencesBuildParameters() {
        EnvVars vars = new EnvVars();
        vars.put("ARGS", "-projectPath $param");

        Map<String, String> buildParameters = new Hashtable<>();
        buildParameters.put("param", "XXXX");

        String argLine = "-p1 v1 $ARGS";
        List<String> expectedArgs = asList(EXE, "-p1", "v1", "-projectPath", "XXXX");

        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine, "");
        ArgumentListBuilder commandlineArgs =
                builder.createCommandlineArgs(EXE, MODULE_ROOT_REMOTE, vars, buildParameters);
        assertEquals(expectedArgs, commandlineArgs.toList());
        assertEquals(argLine, builder.getArgLine(), "Serialized arg line not modified");
    }

    @Test
    void unstableErrorCodesParsing() {
        ensureUnstableReturnCodesParsingWorks(new Integer[] {}, "");
        ensureUnstableReturnCodesParsingWorks(new Integer[] {2, 3}, "2,3");
        ensureUnstableReturnCodesParsingWorks(new Integer[] {-1}, "-1");
        ensureUnstableReturnCodesParsingWorks(new Integer[] {2, 3}, "2, 3");
        ensureUnstableReturnCodesParsingWorks(new Integer[] {2, 3}, " 2 ,3 ");
        ensureUnstableReturnCodesParsingFails(" 2 , ,,");
    }

    private void ensureUnstableReturnCodesParsingWorks(Integer[] expectedResultCodes, String unstableReturnCodes) {
        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", null, unstableReturnCodes);
        assertEquals(new HashSet<>(asList(expectedResultCodes)), builder.toUnstableReturnCodesSet());
    }

    private void ensureUnstableReturnCodesParsingFails(String unstableReturnCodes) {
        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", null, unstableReturnCodes);
        assertThrows(Exception.class, builder::toUnstableReturnCodesSet);
    }
}
