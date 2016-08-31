package org.jenkinsci.plugins.unity3d;

import hudson.AbortException;
import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.QuotedStringTokenizer;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import jenkins.tasks.SimpleBuildStep;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.unity3d.io.Pipe;
import org.jenkinsci.plugins.unity3d.io.StreamCopyThread;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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
public class Unity3dBuilder extends Builder implements SimpleBuildStep {
    private static final Logger log = Logger.getLogger(Unity3dBuilder.class.getName());

    /**
     * @since 0.1
     */
    private String unity3dName;
    /**
     * @since 0.1
     */
    private String argLine;
    private String unstableReturnCodes;

    @DataBoundConstructor
    public Unity3dBuilder(String unity3dName, String argLine, String unstableReturnCodes) {
        this.unity3dName = unity3dName;
        this.argLine = argLine;
        this.unstableReturnCodes = Util.fixNull(unstableReturnCodes);
    }

    @SuppressWarnings("unused")
    private Object readResolve() throws ObjectStreamException {
        if (unstableReturnCodes == null)
            unstableReturnCodes = "";
        return this;
    }

    /**
     * @since 0.1
     */
    public String getArgLine() {
        return argLine;
    }

    /**
     * @since 1.0
     */
    public String getUnstableReturnCodes() {
        return unstableReturnCodes;
    }

    /**
     * @since 1.4
     */
    @DataBoundSetter
    public void setUnstableReturnCodes(String unstableReturnCodes) {
        this.unstableReturnCodes = Util.fixNull(unstableReturnCodes);
    }

    Set<Integer> toUnstableReturnCodesSet() {
        return Helper.toIntegerSet(unstableReturnCodes);
    }

    private String getArgLineOrGlobalArgLine() {
        if (argLine != null && argLine.trim().length() > 0) {
            return argLine;
        } else {
            return getDescriptor().globalArgLine;
        }
    }

    public String getUnity3dName() {
        return unity3dName;
    }


    private static class PerformException extends Exception {
        private static final long serialVersionUID = 1L;
        
        private PerformException(String s) {
            super(s);
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override // SimpleBuildStep.perform
    public void perform(Run<?, ?> run, FilePath fp, Launcher lnchr, TaskListener tl) throws InterruptedException, AbortException {
        try
        {
            _perform(run, fp, lnchr, tl);
        }
        catch (PerformException e) {
            tl.fatalError(e.getMessage());
            throw new AbortException(e.getMessage());
        } catch (IOException e) {
            Util.displayIOException(e, tl);
            String errorMessage = Messages.Unity3d_ExecUnexpectedlyFailed();
            e.printStackTrace(tl.fatalError(errorMessage));
        }
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException {
        try {
            _perform(build, build.getWorkspace(), launcher, listener);
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

    private void _perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException, PerformException {
        EnvVars env = build.getEnvironment(listener);

        Unity3dInstallation ui = getAndConfigureUnity3dInstallation(listener, env, workspace);

        ArgumentListBuilder args = prepareCommandlineArguments(build, workspace, launcher, ui, env);

        String customLogFile = findLogFileArgument(args);

        Pipe pipe = Pipe.createRemoteToLocal(launcher);

        PrintStream ca = listener.getLogger();
        ca.println("Piping unity Editor.log from " + ui.getEditorLogPath(launcher, customLogFile));
        Future<Long> futureReadBytes = ui.pipeEditorLog(launcher, customLogFile, pipe.getOut());
        // Unity3dConsoleAnnotator ca = new Unity3dConsoleAnnotator(listener.getLogger(), build.getCharset());

        StreamCopyThread copierThread = new StreamCopyThread("Pipe editor.log to output thread.", pipe.getIn(), ca);
        try {
            copierThread.start();
            int r =  launcher.launch().cmds(args).envs(env).stdout(ca).pwd(workspace).join();
            // r == 11 means executeMethod could not be found ?
            checkProcResult(build, r);
        } finally {
            // give a bit of time for the piping to complete. Not really
            // sure why it's not properly flushed otherwise
            Thread.sleep(1000);
            if (!futureReadBytes.isDone()) {
                // NOTE According to the API, cancel() should cause future calls to get() to fail with an exception
                // Jenkins implementation doesn't seem to record it right now and just interrupts the remote task
                // but we won't use the value, in case that behavior changes, even for debugging / informative purposes
                // we still call cancel to stop the task.
                futureReadBytes.cancel(true);
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

    private void checkProcResult(Run<?, ?> build, int result) throws PerformException {
        log.info("Unity command line exited with error code: " + result);
        if (isBuildUnstable(result)) {
            log.info(Messages.Unity3d_BuildMarkedAsUnstableBecauseOfStatus(result));
            build.setResult(Result.UNSTABLE);
        } else if (!isBuildSuccess(result)) {
            throw new PerformException(Messages.Unity3d_UnityExecFailed(result));
        }
    }

    private boolean isBuildUnstable(int result) {
        Set<Integer> codes = toUnstableReturnCodesSet();
        return codes.size() > 0 && codes.contains(result);
    }

    private boolean isBuildSuccess(int result) {
        // we could add a set of success results as well, if needed.
        return result == 0;
    }

    /** Find the -logFile argument from the built arg line **/
    private String findLogFileArgument(ArgumentListBuilder args) {
        return Helper.findCommandlineArgument(args, "-logFile");
    }

    private ArgumentListBuilder prepareCommandlineArguments(Run<?,?> build, FilePath workspace, Launcher launcher, Unity3dInstallation ui, EnvVars vars) throws IOException, InterruptedException, PerformException {
        String exe;
        try {
            exe = ui.getExecutable(launcher);
        } catch (RuntimeException re) {
            throw new PerformException(re.getMessage());
        }
        
        if(build instanceof AbstractBuild<?, ?>) {
            AbstractBuild<?, ?> abstractBuild = (AbstractBuild<?, ?>)build;
            FilePath moduleRoot = abstractBuild.getModuleRoot();
            String moduleRootRemote = moduleRoot.getRemote();
            Map<String,String> buildParameters = abstractBuild.getBuildVariables();

            return createCommandlineArgs(exe, moduleRootRemote, vars, buildParameters);
        } else {
            // build variables should be parsed by groovy script
            return createCommandlineArgs(exe, workspace.getRemote(), vars, null);
        }
    }
    
    private Unity3dInstallation getAndConfigureUnity3dInstallation(TaskListener listener, EnvVars env, FilePath workspace) throws PerformException, IOException, InterruptedException {
        Unity3dInstallation ui = getUnity3dInstallation();

        if(ui==null) {
            throw new PerformException(Messages.Unity3d_NoUnity3dInstallation());
        }

        ui = ui.forNode(workspace.toComputer().getNode(), listener);
        ui = ui.forEnvironment(env);
        return ui;
    }

    ArgumentListBuilder createCommandlineArgs(String exe, String moduleRootRemote, EnvVars vars, Map<String,String> buildVariables) {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(exe);

        String theArgLine = getArgLineOrGlobalArgLine();

        String finalArgLine = Util.replaceMacro(theArgLine, buildVariables);
        finalArgLine = Util.replaceMacro(finalArgLine, vars);
        finalArgLine = Util.replaceMacro(finalArgLine, buildVariables);

        if (!finalArgLine.contains("-projectPath")) {
            args.add("-projectPath", moduleRootRemote);
        }

        args.add(QuotedStringTokenizer.tokenize(finalArgLine));
        return args;
    }

    /**
     * @return the Unity3d to invoke,
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
        private String globalArgLine;

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

        public FormValidation doCheckUnstableReturnCodes(@QueryParameter String value) {
            try {
                Helper.toIntegerSet(value);
                return FormValidation.ok();
            } catch (RuntimeException re) {
                return FormValidation.error(Messages.Unity3d_InvalidParamUnstableReturnCodes(value));
            }
        }

        public String getGlobalArgLine() {
            return globalArgLine;
        }

        public void setGlobalArgLine(String globalArgLine) {
            //log.info("setGlobalArgLine: " + globalArgLine);
            this.globalArgLine = globalArgLine;
            save();
        }

        @Override
        public boolean configure( StaplerRequest req, JSONObject o ) {
            globalArgLine = Util.fixEmptyAndTrim(o.getString("globalArgLine"));
            save();

            return true;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public String getDisplayName() {
            return "Invoke Unity3d Editor";
        }
    }
}

