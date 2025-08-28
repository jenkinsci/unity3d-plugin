package org.jenkinsci.plugins.unity3d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * @author Jerome Lacoste
 */
@WithJenkins
class IntegrationTests {

    private JenkinsRule r;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        r = rule;
    }

    @Test
    @LocalData
    void testEditorException() throws Exception {
        ensureUnityHomeExists();

        FreeStyleProject job = (FreeStyleProject) r.jenkins.getItem("test_unity3d");
        assertNotNull(job);

        FreeStyleBuild build = job.scheduleBuild2(0).get();

        r.assertLogContains("Exception: Simulated Exception", build);
    }

    private void ensureUnityHomeExists() {
        Unity3dInstallation[] installations = r.jenkins
                .getDescriptorByType(Unity3dInstallation.DescriptorImpl.class)
                .getInstallations();
        assertEquals(1, installations.length);

        Unity3dInstallation inst = installations[0];
        String unityHome = inst.getHome();

        assumeTrue(
                unityHome != null && new File(unityHome).exists(),
                "Skip test due to missing unity installation: " + unityHome);
    }

    @Test
    @LocalData
    void testEditorExceptionWithCustomLogFile() throws Exception {
        ensureUnityHomeExists();

        FreeStyleProject job = (FreeStyleProject) r.jenkins.getItem("test_unity3d");
        assertNotNull(job);

        FreeStyleBuild build = job.scheduleBuild2(0).get();

        r.assertLogContains("Exception: Simulated Exception", build);
    }

    @Test
    @LocalData
    void testExpectADifferentExitCode() throws Exception {
        ensureUnityHomeExists();

        FreeStyleProject job = (FreeStyleProject) r.jenkins.getItem("test_unity3d");
        assertNotNull(job);

        FreeStyleBuild build = job.scheduleBuild2(0).get();

        r.assertBuildStatus(Result.UNSTABLE, build);
    }
}
