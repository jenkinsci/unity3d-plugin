package org.jenkinsci.plugins.unity3d;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import hudson.EnvVars;
import hudson.util.ArgumentListBuilder;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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
        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine);
        ArgumentListBuilder commandlineArgs = builder.createCommandlineArgs(exe, moduleRootRemote, new EnvVars(), new Hashtable<String,String>());
        assertEquals(expectedArgs1, commandlineArgs.toList());
    }
    
    @Test
    public void environmentAndBuildVariablesParsing() {
    	EnvVars vars = new EnvVars();
        vars.put("param1", "value1");
        vars.put("param2", "value2");
        
        String param2overwrittenValue = "overwrittenValue";
        
        Map<String,String> buildParameters = new Hashtable<String,String>();
        buildParameters.put("param2", param2overwrittenValue);
    	
        argLine = "-param1 $param1 -param2 $param2 -projectPath XXXX";
        expectedArgs = asList(exe, "-param1", "value1", "-param2", param2overwrittenValue, "-projectPath", "XXXX");
       
        Unity3dBuilder builder = new Unity3dBuilder("Unity 3.5", argLine);
        ArgumentListBuilder commandlineArgs = builder.createCommandlineArgs(exe, moduleRootRemote, vars, buildParameters);
        assertEquals(expectedArgs, commandlineArgs.toList());
    }
}
