package org.jenkinsci.plugins.unity3d.workflow;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.inject.Inject;
import javax.annotation.CheckForNull;

import org.jenkinsci.plugins.unity3d.Unity3dBuilder;
import org.jenkinsci.plugins.unity3d.Unity3dInstallation;
import org.jenkinsci.plugins.unity3d.Helper;
import org.jenkinsci.plugins.unity3d.Messages;

/**
 *
 * @author rstyrczula
 */
public class Unity3dBuilderStep extends AbstractStepImpl {

    private final String unity3dName;
    private String argLine;
    @CheckForNull
    private String unstableReturnCodes;

    @DataBoundConstructor
    public Unity3dBuilderStep(String unity3dName) {
        this.unity3dName = unity3dName;
        this.argLine = DescriptorImpl.defaultArgLine;
        this.unstableReturnCodes = DescriptorImpl.defaultUnstableReturnCodes;
    }

    public String getUnity3dName() {
        return unity3dName;
    }

    public String getArgLine() {
        return argLine;
    }

    @DataBoundSetter
    public void setArgLine(String argLine) {
        this.argLine = argLine;
    }

    public @CheckForNull String getUnstableReturnCodes() {
        return unstableReturnCodes;
    }

    @DataBoundSetter
    public void setUnstableReturnCodes(@CheckForNull String unstableReturnCodes) {
        this.unstableReturnCodes = Util.fixNull(unstableReturnCodes);
    }

    public static class Execution extends AbstractSynchronousNonBlockingStepExecution {

        @StepContextParameter
        private transient Run<?, ?> run;
        
        @StepContextParameter
        private transient TaskListener listener;
        
        @StepContextParameter
        private transient Launcher launcher;
        
        @StepContextParameter
        private transient FilePath ws;
        
        @Inject(optional=true)
        private transient Unity3dBuilderStep step;
        
        private transient Unity3dBuilder builder;

        @Override
        protected Void run() throws Exception {
            builder = new Unity3dBuilder(step.getUnity3dName(), step.getArgLine(), step.getUnstableReturnCodes());
            builder.perform(run, ws, launcher, listener);
            builder = null;
            return null;
        }
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public static final String defaultArgLine = null;
        public static final String defaultUnstableReturnCodes = "";

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getDisplayName() {
            Unity3dBuilder.DescriptorImpl di = new Unity3dBuilder.DescriptorImpl();
            return di.getDisplayName();
        }

        @Override
        public String getFunctionName() {
            return "unity3d";
        }
        
        public FormValidation doCheckUnstableReturnCodes(@QueryParameter String value) {
            try {
                Helper.toIntegerSet(value);
                return FormValidation.ok();
            } catch (RuntimeException re) {
                return FormValidation.error(Messages.Unity3d_InvalidParamUnstableReturnCodes(value));
            }
        }

        public ListBoxModel doFillUnity3dNameItems() {
            ListBoxModel items = new ListBoxModel();
            Unity3dInstallation.DescriptorImpl di = new Unity3dInstallation.DescriptorImpl();
            for(Unity3dInstallation installation : di.getInstallations()) {
                items.add(installation.getName(), installation.getName());
            }
            return items;
        }
    }
}