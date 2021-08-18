package io.jenkins.plugins.ksm.builder;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.EnvVars;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.ksm.KsmCommon;
import io.jenkins.plugins.ksm.creds.KsmCredentials;
import io.jenkins.plugins.ksm.Messages;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.notation.KsmNotationRequest;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.AncestorInPath;
import javax.annotation.Nonnull;

import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.DescribableList;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;

import io.jenkins.plugins.ksm.KsmSecret;


public class KsmBuilder extends Builder implements SimpleBuildStep {

    private String credentialsId;
    private List<KsmSecret> secrets = new ArrayList<>();

    @DataBoundConstructor
    public KsmBuilder(String credentialsId, List<KsmSecret> secrets) {
        this.credentialsId = credentialsId;
        this.secrets = secrets;
    }

    public String getCredentialsId() {
        return credentialsId;
    }
    public List<KsmSecret> getSecrets() {
        return secrets;
    }

    @DataBoundSetter
    public void setSecrets(List<KsmSecret> secrets) {
        this.secrets = secrets;
    }
    @DataBoundSetter
    public void setCredentialsId(String credId) {
        this.credentialsId = credId;
    }

    @Override
    public boolean perform(@Nonnull AbstractBuild<?, ?> build,
                           @Nonnull Launcher launcher,
                           @Nonnull BuildListener listener) throws InterruptedException, IOException {

        listener.getLogger().println("perform");

        KsmCredentials ksmCredential = CredentialsProvider.findCredentialById(
                credentialsId,
                KsmCredentials.class,
                build
        );
        if (ksmCredential == null) {
            // logger.error("Cannot find the credential id " + credId + " for KSM secrets");
            build.setResult(Result.FAILURE);
            return false;
        }

        listener.getLogger().println("CREDENTIALS");
        listener.getLogger().println("Client Key = " + ksmCredential.getClientKey());
        listener.getLogger().println("Client Id = " + ksmCredential.getClientId());
        listener.getLogger().println("Private Key = " + ksmCredential.getPrivateKey());
        listener.getLogger().println("App Key = " + ksmCredential.getAppKey());
        listener.getLogger().println("Hostname = " + ksmCredential.getHostname());
        listener.getLogger().println("");

        listener.getLogger().println("SECRETS");
        for (KsmSecret item:  secrets) {
            listener.getLogger().println("-----------------");
            listener.getLogger().println("EnvVar = " + item.getEnvVar());
            listener.getLogger().println("Notation " + item.getNotation());
            listener.getLogger().println("");

            try {
                KsmNotationRequest req = KsmNotation.parse(item.getNotation());
                listener.getLogger().println("UID = " + req.getUid());
                listener.getLogger().println("Field Data Type = " + req.getFieldDataType());
                listener.getLogger().println("Field Key = " + req.getFieldKey());
                listener.getLogger().println("Single Value = " + req.getReturnSingle());
                listener.getLogger().println("Array Index = " + req.getArrayIndex());
                listener.getLogger().println("Dict Key = " + req.getDictKey());
                listener.getLogger().println("");
            }
            catch(Exception e) {
                listener.getLogger().println("ERROR: " + e.getMessage());
            }
        }
        listener.getLogger().println("");

        Jenkins jenkins = Jenkins.getInstanceOrNull();
        assert jenkins != null;
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = jenkins.getGlobalNodeProperties();
        List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(hudson.slaves.EnvironmentVariablesNodeProperty.class);

        EnvironmentVariablesNodeProperty newEnvVarsNodeProperty = null;
        EnvVars envVars = null;

        if (envVarsNodePropertyList == null || envVarsNodePropertyList.isEmpty()) {
            newEnvVarsNodeProperty = new hudson.slaves.EnvironmentVariablesNodeProperty();
            globalNodeProperties.add(newEnvVarsNodeProperty);
            envVars = newEnvVarsNodeProperty.getEnvVars();
        } else {
            envVars = envVarsNodePropertyList.get(0).getEnvVars();
        }
        for (KsmSecret item : secrets) {
            envVars.put(item.getEnvVar(), item.getNotation());
        }

        return true;
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheck(@QueryParameter String value, @QueryParameter boolean useFrench) {
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.KsmBuilder_DescriptorImpl_DisplayName();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {
            return KsmCommon.buildCredentialsIdListBox(context);
        }
    }

}
