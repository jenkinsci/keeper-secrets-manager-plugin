package io.jenkins.plugins.ksm.builder;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.ksm.KsmCommon;
import io.jenkins.plugins.ksm.Messages;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.notation.KsmNotationItem;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.kohsuke.stapler.AncestorInPath;
import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.jenkins.plugins.ksm.KsmSecret;


public class KsmBuilder extends Builder implements SimpleBuildStep {

    private transient String credentialsId;
    private transient List<KsmSecret> secrets;
    private transient KsmNotation notation;

    @DataBoundConstructor
    public KsmBuilder(String credentialsId, List<KsmSecret> secrets) {
        this.credentialsId = credentialsId;
        this.secrets = secrets;
        this.notation = new KsmNotation();
    }
    public KsmBuilder(String credentialsId, List<KsmSecret> secrets, KsmNotation notation) {
        this.credentialsId = credentialsId;
        this.secrets = secrets;
        this.notation = notation;
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
    public void perform(
            @Nonnull Run< ?,?> run,
            @Nonnull FilePath workspace,
            @Nonnull EnvVars env,
            @Nonnull Launcher launcher,
            @Nonnull TaskListener listener
    ) throws IOException {

        Logger LOGGER = Logger.getLogger(KsmBuilder.class.getName());
        LOGGER.log(Level.FINE, "Starting Keeper Secrets Manager builder step.");

        // Find the credentials use for this build step.
        LOGGER.log(Level.FINE, "Finding Credentials Id " + getCredentialsId());
        KsmCredential credential = CredentialsProvider.findCredentialById(
                getCredentialsId(),
                KsmCredential.class,
                run
        );
        if (credential == null) {
            throw new AbortException(KsmCommon.errorPrefix + "Cannot find the credential id " + credentialsId + ".");
        }
        if (!credential.getCredentialError().equals("")) {
            throw new AbortException(KsmCommon.errorPrefix + "Credential id " + credentialsId + " has errors associated with it. Cannot not use.");
        }

        Map<String, KsmNotationItem> notationItems = new HashMap<>();

        // Create notation items from the build secrets and add them to a map. Use the env var name as the
        // key since that is unique.
        for (KsmSecret item:  secrets) {
            try {
                LOGGER.log(Level.FINE, "Parsing notation " + item.getNotation() + " for env var " + item.getEnvVar());
                KsmNotationItem notationItem = KsmNotation.parse(item.getEnvVar(), item.getNotation(), false);
                notationItems.put(item.getEnvVar(), notationItem);
            }
            catch(Exception e) {
                throw new AbortException(KsmCommon.errorPrefix + "The secret " + item.getEnvVar() +
                        " contains invalid syntax: " + e.getMessage());
            }
        }

        // Find any default env vars using the Keeper notation.
        for(Map.Entry<String, String> entry: env.entrySet()) {
            try {
                KsmNotationItem item = KsmNotation.find(entry.getKey(), entry.getValue(), true);
                if (item != null) {
                    LOGGER.log(Level.FINE, "Found a global env var " + entry.getValue() + " with value " +
                            entry.getValue());
                    notationItems.put(entry.getKey(), item);
                }
            }
            catch( Exception e) {
                throw new AbortException(KsmCommon.errorPrefix + "The environmental variable " + entry.getKey() +
                        " contains invalid syntax: " + e.getMessage());
            }
        }

        // Process all the notation. This will connect to Secret Manager, get the record, get the value and will store
        // the value in the item.
        try {
            notation.run(credential, notationItems);
        }
        catch(Exception e) {
            throw new AbortException(KsmCommon.errorPrefix + "The environmental variable replace had problems: "
                    + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        // Set the environmental variables values.
        EnvVars ksmEnvVars = new EnvVars();
        for(Map.Entry<String, KsmNotationItem> entry: notationItems.entrySet()) {
            KsmNotationItem notationItem = entry.getValue();

            // Only set env vars that did not have an error when the notation was being parsed. We might allow
            // these variable to fail, we just don't want to set them when they do.
            if ( notationItem.getError() == null ) {
                try {
                    ksmEnvVars.put(entry.getKey(), notationItem.getValue());
                }
                catch (java.lang.IllegalArgumentException e) {

                    // User might use notation to return a file. If binary, java with throw an exception. It will work
                    // for text.
                    if( e.getMessage().contains("Invalid environment variable value") ) {
                        throw new AbortException(KsmCommon.errorPrefix + "Invalid environment variable value. You might be "
                                + "trying to set binary data value into an environmental variable.");
                    }
                    throw e;
                }
            }
            else {
                LOGGER.log(Level.WARNING, "The env var " + entry.getKey() + " notation "  + notationItem.getNotation()
                        + " could not be found: " + notationItem.getError());
            }
        }

        // Add the config of the credential to the env var. This config will allow any KSM apps within the
        // build to be used. The allowConfigInject boolean has to be true for this to happen.
        KsmCommon.addCredentialToEnv(credential, ksmEnvVars, env);

        // Add a new action for populating environmental variables. This will be used by following steps to
        // populate their environment variables.
        run.addAction(new KsmEnvironmentContributingAction(ksmEnvVars));

        LOGGER.log(Level.FINE, "Finishing Keeper Secrets Manager builder step.");
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.KsmBuilder_DescriptorImpl_DisplayName();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup<?> context) {
            return KsmCommon.buildCredentialsIdListBox(context);
        }
    }
}
