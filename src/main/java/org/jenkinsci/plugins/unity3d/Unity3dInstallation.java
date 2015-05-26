package org.jenkinsci.plugins.unity3d;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import org.jenkinsci.plugins.unity3d.io.PipeFileAfterModificationAction;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Represents a Unity3d installation (name, home_dir, etc.)
 *
 * @author Jerome Lacoste
 */
public class Unity3dInstallation
        extends ToolInstallation
        implements EnvironmentSpecific<Unity3dInstallation>, NodeSpecific<Unity3dInstallation> {

    private static final Logger log = Logger.getLogger(Unity3dInstallation.class.getName());

    @DataBoundConstructor
    public Unity3dInstallation(final String name, final String home, final List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    public Unity3dInstallation(final Unity3dInstallation source, final String home, final List<? extends ToolProperty<?>> properties) {
        super(source.getName(), home, properties);
    }

    public Unity3dInstallation forEnvironment(EnvVars env) {
        return new Unity3dInstallation(this, env.expand(getHome()), getProperties().toList());
    }

    public Unity3dInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new Unity3dInstallation(this, translateFor(node, log), getProperties().toList());
    }

    /**
     * Gets the executable path of this Unity3dBuilder on the given target system.
     */
    public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<String, IOException>() {
            public String call() throws IOException {
                return checkUnity3dExecutablePath(getHome());
            }
        });
    }

    private static String checkUnity3dExecutablePath(String home) {
        String unityHome = Util.replaceMacro(home, EnvVars.masterEnvVars);
        log.fine("UNITY_HOME:" + unityHome);
        File value = new File(unityHome);

        File unityExe = getExeFile(value);

        String path = unityExe.getAbsolutePath();

        if (!value.isDirectory() || !unityExe.exists()) {
            throw new RuntimeException(FormValidation.error(Messages.Unity3d_InvalidUnityHomeConfiguration(value, path)).getMessage());
        }

        return path;
    }

    private static File getExeFile(File unityHome) {
        if (Functions.isWindows()) {
            return new File(unityHome, "Editor/Unity.exe");
        } else { // mac assumed
            return new File(unityHome, "Contents/MacOS/Unity");
        }
    }

    /**
     * Create a long running task that pipes the Unity3d editor.log into the specified pipe.
     * <p>
     * This future can be {@link Future#cancel(boolean) cancelled} in order for the pipe to be closed properly.
     * @param launcher
     * @param ros the output stream to write into
     * @return the number of bytes read
     * @throws IOException
     */
    public Future<Long> pipeEditorLog(final Launcher launcher, final String customLogFile, final OutputStream ros) throws IOException {
        return launcher.getChannel().callAsync(new Callable<Long, IOException>() {
            public Long call() throws IOException {
                return new PipeFileAfterModificationAction(getEditorLogFile(customLogFile).getAbsolutePath(), ros, true).call();
            }
        });
    }

    /**
     * Returns the Editor.log path on the remote machine
     * @param launcher
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public String getEditorLogPath(final Launcher launcher, final String customLogFile) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<String, IOException>() {
            public String call() throws IOException {
                return getEditorLogFile(customLogFile).getAbsolutePath();
            }
        });
    }

    private File getEditorLogFile(String customLogFile) {
        if (customLogFile != null) return new File(customLogFile);

        if (Functions.isWindows()) {
            String localAppData = EnvVars.masterEnvVars.get("LOCALAPPDATA");
            if (localAppData == null) {
                throw new RuntimeException("Empty LOCALAPPDATA environment variable. Use -logFile command line argument as workaround. Unable to find Editor.log location (see JENKINS-24265).");
            }
            File applocaldata = new File(localAppData);
            return new File(applocaldata, "Unity/Editor/Editor.log");
        } else { // mac assumed
            File userhome = new File(EnvVars.masterEnvVars.get("HOME"));
            return new File(userhome, "Library/Logs/Unity/Editor.log");
        }
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<Unity3dInstallation> {

        @Override
        public String getDisplayName() {
            return "Unity3d";
        }

        // for compatibility reasons, the persistence is done by Unity3dBuilder.DescriptorImpl
        @Override
        public Unity3dInstallation[] getInstallations() {
            return Hudson.getInstance().getDescriptorByType(Unity3dBuilder.DescriptorImpl.class).getInstallations();
        }

        @Override
        public void setInstallations(Unity3dInstallation... installations) {
            Hudson.getInstance().getDescriptorByType(Unity3dBuilder.DescriptorImpl.class).setInstallations(installations);
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.emptyList();
        }

        /**
         * Checks if the UNITY_HOME is valid.
         */
        public FormValidation doCheckHome(@QueryParameter String value) {
            // this can be used to check the existence of a file on the server, so needs to be protected
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER))
                return FormValidation.ok();

            if (value.equals(""))
                return FormValidation.ok();

            try {
                checkUnity3dExecutablePath(value);
            } catch (RuntimeException re) {
                return FormValidation.error(re.getMessage());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }
    }

}
