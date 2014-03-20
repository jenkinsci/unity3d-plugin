package org.jenkinsci.plugins.unity3d;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LenientRunnable;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.File;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * @author Jerome Lacoste
 */
public class IntegrationTests extends HudsonTestCase {
    /*@Rule
    public JenkinsRule rule = new JenkinsRule();*/

    @Test
    @LocalData
    public void testEditorException() throws Exception {
        Unity3dInstallation[] installations = jenkins.getDescriptorByType(Unity3dInstallation.DescriptorImpl.class).getInstallations();
        assertEquals(1, installations.length);

        Unity3dInstallation inst = installations[0];
        String unityHome = inst.getHome();

        assumeTrue(new File(unityHome).exists()); // skip test if doesn't have unity

        FreeStyleProject job = (FreeStyleProject) jenkins.getItem("test_unity3d");
        assertNotNull(job);

        FreeStyleBuild build = job.scheduleBuild2(0).get();

        String log = FileUtils.readFileToString(build.getLogFile());

        System.out.println(log);
        assertTrue("Found cause for failure in console", log.contains("Exception: Simulated Exception"));
    }
}
