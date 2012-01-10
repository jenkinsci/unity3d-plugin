package org.jenkinsci.plugins.unity3d;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Computer;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.QuotedStringTokenizer;
import org.jenkinsci.plugins.unity3d.io.Pipe;
import org.jenkinsci.plugins.unity3d.io.StreamCopyThread;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.*;
import java.util.concurrent.Future;

/**
 * Unity3d builder
 * <p>
 * Features:<br/>
 * <u>
 *  <li>supports local and remote execution</li>
 *  <li>pipe the editor.log into the console</li>
 * </u>
 * @author Jerome Lacoste
 */
public class Unity3dBuilder extends Builder {

    private String unity3dName;
    private String argLine;

    @DataBoundConstructor
    public Unity3dBuilder(String unity3dName, String argLine) {
        this.unity3dName = unity3dName;
        this.argLine = argLine;
    }

    public String getArgLine() {
        return argLine;
    }

    public String getUnity3dName() {
        return unity3dName;
    }
    
    private static class PerformException extends Exception {
        private PerformException(String s) {
            super(s);
        }
    }
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException {
        try {
            _perform(build, launcher, listener);
            return true;
        } catch (PerformException e) {
            listener.fatalError(e.getMessage());
            return false;
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            String errorMessage = Messages.Unity3d_ExecUnexpectedlyFailed();
            e.printStackTrace(listener.fatalError(errorMessage));
            return false;
        }
    }

    private void _perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, PerformException {
        EnvVars env = build.getEnvironment(listener);

        Unity3dInstallation ui = getAndConfigureUnity3dInstallation(listener, env);

        ArgumentListBuilder args = prepareCommandlineArguments(build, launcher, ui);

        Pipe pipe = Pipe.createRemoteToLocal(launcher);

        PrintStream ca = listener.getLogger();
        ca.println("Piping unity Editor.log from " + ui.getEditorLogPath(launcher));
        Future<Long> futureReadBytes = ui.pipeEditorLog(launcher, pipe.getOut());
        // Unity3dConsoleAnnotator ca = new Unity3dConsoleAnnotator(listener.getLogger(), build.getCharset());

        StreamCopyThread copierThread = new StreamCopyThread("Pipe editor.log to output thread.", pipe.getIn(), ca);
        int r;
        try {
            copierThread.start();
            r = launcher.launch().cmds(args).envs(env).stdout(ca).pwd(build.getWorkspace()).join();
            // r == 11 means executeMethod could not be found ?
            if (r != 0) {
                throw new PerformException(Messages.Unity3d_UnityExecFailed(r));
            }
        } finally {
            if (!futureReadBytes.isDone()) {
                // NOTE According to the API, cancel() should cause future calls to get() to fail with an exception
                // Jenkins implementation doesn't seem to record it right now and just interrupts the remote task
                // but we won't use the value, in case that behavior changes, even for debugging / informative purposes
                // we still call cancel to stop the task.
                boolean cancel = futureReadBytes.cancel(true);
                // listener.getLogger().print("Read " + futureReadBytes.get() + " bytes from Editor.log");
            }
            try {
                copierThread.join();
                if (copierThread.getFailure() != null) {
                   ca.println("Failure on remote ");
                   copierThread.getFailure().printStackTrace(ca);
                }
            }
            finally {
                //ca.forceEol();
            }
        }
    }

    private ArgumentListBuilder prepareCommandlineArguments(AbstractBuild build, Launcher launcher, Unity3dInstallation ui) throws IOException, InterruptedException, PerformException {
        String exe = ui.getExecutable(launcher);
        if (exe==null) {
            throw new PerformException(Messages.Unity3d_ExecutableNotFound(ui.getName()));
        }

        FilePath moduleRoot = build.getModuleRoot();
        String moduleRootRemote = moduleRoot.getRemote();
        if (!moduleRoot.child("Assets").exists()) {
            throw new PerformException(Messages.Unity3d_MissingAssetsNotAUnity3dProjectDirectory(moduleRootRemote));
        }

        return createCommandlineArgs(exe);
    }

    private Unity3dInstallation getAndConfigureUnity3dInstallation(BuildListener listener, EnvVars env) throws PerformException, IOException, InterruptedException {
        Unity3dInstallation ui = getUnity3dInstallation();

        if(ui==null) {
            throw new PerformException(Messages.Unity3d_NoUnity3dInstallation());
        }

        ui = ui.forNode(Computer.currentComputer().getNode(), listener);
        ui = ui.forEnvironment(env);
        return ui;
    }

    ArgumentListBuilder createCommandlineArgs(String exe) {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(exe);
        args.add(QuotedStringTokenizer.tokenize(argLine));
        return args;
    }

    /**
     * Gets the Unity3d to invoke,
     * or null to invoke the default one.
     */
    private Unity3dInstallation getUnity3dInstallation() {
        for( Unity3dInstallation i : getDescriptor().getInstallations() ) {
            if(unity3dName!=null && unity3dName.equals(i.getName()))
                return i;
        }
        return null;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @CopyOnWrite
        private volatile Unity3dInstallation[] installations = new Unity3dInstallation[0];

        public DescriptorImpl() {
            load();
        }

        public Unity3dInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(Unity3dInstallation.DescriptorImpl.class);
        }

        public Unity3dInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(Unity3dInstallation... antInstallations) {
            this.installations = antInstallations;
            save();
        }

        public FormValidation doCheckArgLine(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set some arguments");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Invoke Unity3d Editor";
        }
    }
}

