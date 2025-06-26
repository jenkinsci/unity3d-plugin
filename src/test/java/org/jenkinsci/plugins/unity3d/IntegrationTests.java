package org.jenkinsci.plugins.unity3d;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * @author Jerome Lacoste
 */
public class IntegrationTests {

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Test
    @LocalData
    public void testEditorException() throws Exception {
        ensureUnityHomeExists();

        FreeStyleProject job = (FreeStyleProject) rule.jenkins.getItem("test_unity3d");
        assertNotNull(job);

        FreeStyleBuild build = job.scheduleBuild2(0).get();

        rule.assertLogContains("Exception: Simulated Exception", build);
    }

    private void ensureUnityHomeExists() {
        Unity3dInstallation[] installations = rule.jenkins
                .getDescriptorByType(Unity3dInstallation.DescriptorImpl.class)
                .getInstallations();
        assertEquals(1, installations.length);

        Unity3dInstallation inst = installations[0];
        String unityHome = inst.getHome();

        assumeTrue(new File(unityHome).exists()); // skip test if doesn't have unity
    }

    @Test
    @LocalData
    public void testEditorExceptionWithCustomLogFile() throws Exception {
        ensureUnityHomeExists();

        FreeStyleProject job = (FreeStyleProject) rule.jenkins.getItem("test_unity3d");
        assertNotNull(job);

        FreeStyleBuild build = job.scheduleBuild2(0).get();

        rule.assertLogContains("Exception: Simulated Exception", build);
    }

    @Test
    @LocalData
    public void testExpectADifferentExitCode() throws Exception {
        ensureUnityHomeExists();
        FreeStyleProject job = (FreeStyleProject) rule.jenkins.getItem("test_unity3d");
        assertNotNull(job);

        FreeStyleBuild build = job.scheduleBuild2(0).get();

        rule.assertBuildStatus(Result.UNSTABLE, build);
    }
}
