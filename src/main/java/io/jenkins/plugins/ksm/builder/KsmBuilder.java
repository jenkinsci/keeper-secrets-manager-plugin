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
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.Messages;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.notation.KsmNotationItem;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.AncestorInPath;
import javax.annotation.Nonnull;

import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.DescribableList;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;

import io.jenkins.plugins.ksm.KsmSecret;
import io.jenkins.plugins.ksm.KsmQuery;


public class KsmBuilder extends Builder implements SimpleBuildStep {

    private String credentialsId;
    private List<KsmSecret> secrets;

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

        KsmCredential credential = CredentialsProvider.findCredentialById(
                credentialsId,
                KsmCredential.class,
                build
        );
        if (credential == null) {
            listener.getLogger().println(KsmCommon.errorPrefix + "Cannot find the credential id " + credentialsId + ".");
            build.setResult(Result.FAILURE);
            return false;
        }

        HashMap<String, KsmNotationItem> notationItems = new HashMap<String, KsmNotationItem>();
        KsmQuery query = new KsmQuery(credential);

        listener.getLogger().println("CREDENTIALS");
        listener.getLogger().println("Client Key = " + credential.getClientKey());
        listener.getLogger().println("Client Id = " + credential.getClientId());
        listener.getLogger().println("Private Key = " + credential.getPrivateKey());
        listener.getLogger().println("Public Key = " + credential.getPublicKey());
        listener.getLogger().println("App Key = " + credential.getAppKey());
        listener.getLogger().println("Hostname = " + credential.getHostname());
        listener.getLogger().println("");

        // Create notation items from the build secrets and add them to a map. Use the env var name as the
        // key since that is unique.
        listener.getLogger().println("SECRETS");
        for (KsmSecret item:  secrets) {
            listener.getLogger().println("-----------------");
            listener.getLogger().println("EnvVar = " + item.getEnvVar());
            listener.getLogger().println("Notation " + item.getNotation());
            listener.getLogger().println("");

            try {
                KsmNotationItem notationItem = KsmNotation.parse(item.getEnvVar(), item.getNotation());
                notationItems.put(item.getEnvVar(), notationItem);

                listener.getLogger().println("UID = " + notationItem.getUid());
                listener.getLogger().println("Field Data Type = " + notationItem.getFieldDataType());
                listener.getLogger().println("Field Key = " + notationItem.getFieldKey());
                listener.getLogger().println("Single Value = " + notationItem.getReturnSingle());
                listener.getLogger().println("Array Index = " + notationItem.getArrayIndex());
                listener.getLogger().println("Dict Key = " + notationItem.getDictKey());
                listener.getLogger().println("");
            }
            catch(Exception e) {
                listener.getLogger().println(KsmCommon.errorPrefix + "The secret " + item.getEnvVar() +
                        " contains invalid syntax: " + e.getMessage());
                return false;
            }
        }
        listener.getLogger().println("");

        // Get existing environmental variables.
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        assert jenkins != null;
        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = jenkins.getGlobalNodeProperties();
        List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(hudson.slaves.EnvironmentVariablesNodeProperty.class);

        EnvironmentVariablesNodeProperty newEnvVarsNodeProperty;
        EnvVars envVars;

        if (envVarsNodePropertyList == null || envVarsNodePropertyList.isEmpty()) {
            newEnvVarsNodeProperty = new hudson.slaves.EnvironmentVariablesNodeProperty();
            globalNodeProperties.add(newEnvVarsNodeProperty);
            envVars = newEnvVarsNodeProperty.getEnvVars();
        } else {
            envVars = envVarsNodePropertyList.get(0).getEnvVars();
        }

        // Replace any env vars using the Keeper notation. This might be set on a node, load in a prior build step,
        // come from another env var plugin, etc. These are ones not from the Keeper plugin.
        for(Map.Entry<String, String> entry: envVars.entrySet()) {
            try {
                KsmNotationItem item = KsmNotation.find(entry.getKey(), entry.getValue());
                if (item != null) {
                    notationItems.put(entry.getKey(), item);
                }
            }
            catch( Exception e) {
                listener.getLogger().println(KsmCommon.errorPrefix + "The environmental variable " + entry.getKey() +
                        " contains invalid syntax: " + e.getMessage());
                return false;
            }
        }

        // Process all the notation. This will connect to Secret Manager, get the record, get the value and will store
        // the value in the item.
        try {
            query.run(notationItems);
        }
        catch(Exception e) {
            listener.getLogger().println(KsmCommon.errorPrefix + "The environmental variable replace had pproblems: "
                    + e.getMessage());
            return false;
        }

        // Set the environmental variables values.
        for(Map.Entry<String, KsmNotationItem> entry: notationItems.entrySet()) {
            KsmNotationItem notationItem = entry.getValue();
            envVars.put(entry.getKey(), notationItem.getValue());
        }

        return true;
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheck(@QueryParameter String value) {
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
