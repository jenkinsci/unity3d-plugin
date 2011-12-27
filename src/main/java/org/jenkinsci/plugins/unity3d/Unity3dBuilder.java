package org.jenkinsci.plugins.unity3d;

import hudson.*;
import hudson.model.Computer;
import hudson.remoting.Pipe;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.StreamCopyThread;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;


import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
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

    private final String unity3dName;
    private final String executeMethod;

    @DataBoundConstructor
    public Unity3dBuilder(String unity3dName, String executeMethod) {
        this.unity3dName = unity3dName;
        this.executeMethod = executeMethod;
    }

    public String getExecuteMethod() {
        return executeMethod;
    }

    public String getUnity3dName() {
        return unity3dName;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        EnvVars env = build.getEnvironment(listener);

        Unity3dInstallation ai = getUnity3dInstallation();

        if(ai==null) {
            listener.fatalError(Messages.Unity3d_NoUnity3dInstallation());
            return false;
        }

        ai = ai.forNode(Computer.currentComputer().getNode(), listener);
        ai = ai.forEnvironment(env);

        String exe = ai.getExecutable(launcher);
        if (exe==null) {
            listener.fatalError(Messages.Unity3d_ExecutableNotFound(ai.getName()));
            return false;
        }

        if (executeMethod == null || executeMethod.length() == 0) {
            listener.fatalError(Messages.Unity3d_MissingExecuteMethod());
            return false;
        }
        FilePath moduleRoot = build.getModuleRoot();
        String moduleRootRemote = moduleRoot.getRemote();
        if (!moduleRoot.child("Assets").exists()) {
            listener.fatalError(Messages.Unity3d_MissingAssetsNotAUnity3dProjectDirectory(moduleRootRemote));
            return false;
        }

        ArgumentListBuilder args = createCommandLineArgs(exe, moduleRootRemote, executeMethod);
        
        try {
            Pipe pipe = Pipe.createRemoteToLocal();
            Future<Long> futureReadBytes = ai.pipeEditorLog(launcher, pipe);
            // Unity3dConsoleAnnotator ca = new Unity3dConsoleAnnotator(listener.getLogger(), build.getCharset());
            OutputStream ca = listener.getLogger();

            Thread copierThread = new StreamCopyThread("Pipe editor.log to ouput thread.", pipe.getIn(), ca);
            int r;
            try {
                copierThread.start();
                r = launcher.launch().cmds(args).envs(env).stdout(ca).pwd(build.getWorkspace()).join();
                // r == 11 means executeMethod could not be found ?
                if (r != 0) {
                    listener.fatalError(Messages.Unity3d_UnityExecFailed(r));
                    return false;
                }
                return true;
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
                }
                finally {
                    //ca.forceEol();
                }
            }
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            String errorMessage = Messages.Unity3d_ExecFailed();
            e.printStackTrace(listener.fatalError(errorMessage));
            return false;
        }
    }

    private ArgumentListBuilder createCommandLineArgs(String exe, String moduleRootRemote, final String executeMethod) {
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(exe);
        args.add("-projectpath", moduleRootRemote);
        args.add("-quit");
        args.add("-batchmode");
        args.add("-executeMethod", executeMethod);
        return args;
    }

    /**
     * Gets the Unity3d to invoke,
     * or null to invoke the default one.
     */
    public Unity3dInstallation getUnity3dInstallation() {
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

        public FormValidation doCheckExecuteMethod(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set an executeMethod");
            if (!value.contains("."))
                return FormValidation.warning("Isn't the executeMethod of the form ClassName.MethodName ?");
            return FormValidation.ok();
        }

        // not checking name, it comes from the drop down list...

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Invoke Unity3d";
        }
    }
}

