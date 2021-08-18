package io.jenkins.plugins.ksm.workflow;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.security.ACL;
import io.jenkins.plugins.ksm.KsmCommon;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.notation.KsmNotationItem;
import org.jenkinsci.plugins.workflow.steps.*;
import hudson.Extension;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.EnvVars;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;

import io.jenkins.plugins.ksm.KsmSecret;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.KsmQuery;


public class KsmStep extends Step {

    private List<KsmApplication> application;

    @DataBoundConstructor
    public KsmStep(List<KsmApplication> application) {
        this.application = application;
    }

    public List<KsmApplication> getApplication() {
        return application;
    }

    @DataBoundSetter
    public void setApplication(List<KsmApplication> application) {
        this.application = application;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    protected static class Execution extends GeneralNonBlockingStepExecution {

        private static final long serialVersionUID = 1;
        private final transient KsmStep step;

        public Execution(KsmStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        public boolean start() throws Exception {
            run(this::doStart);
            return false;
        }

        private void doStart() throws Exception {

            Logger LOGGER = Logger.getLogger(KsmStep.class.getName());
            LOGGER.log(Level.FINE, "Starting Keeper Secrets Manager workflow step");

            TaskListener listener = getContext().get(TaskListener.class);
            assert listener != null;
            EnvVars existingEnvVars = getContext().get(EnvVars.class);

            // Find any env vars using the Keeper notation. This might be set on a node, load in a prior build step,
            // come from another env var plugin, etc. These are ones not from the Keeper plugin. The problem is we
            // are not sure which application contains the record.
            Map<String, KsmNotationItem> globalNotationItems = new HashMap<String, KsmNotationItem>();
            if (existingEnvVars != null) {
                for (Map.Entry<String, String> entry : existingEnvVars.entrySet()) {
                    LOGGER.log(Level.FINE, "Checking global env var " + entry.getKey() + " for notation.");
                    try {
                        KsmNotationItem item = KsmNotation.find(entry.getKey(), entry.getValue(), true);
                        if (item != null) {
                            LOGGER.log(Level.FINE, " * found " + entry.getValue());
                            globalNotationItems.put(entry.getKey(), item);
                        }
                    } catch (Exception e) {
                        throw new Exception(KsmCommon.errorPrefix + "The global environmental variable " + entry.getKey() +
                                " contains invalid syntax: " + e.getMessage());
                    }
                }
            }

            // For each application, for all the secrets, replace them with actual secrets.
            EnvVars envVars = new EnvVars();
            for (KsmApplication application : step.application) {

                String credentialsId = application.getCredentialsId();

                LOGGER.log(Level.FINE, "Processing secrets for credentials id " + credentialsId);

                // Get the credential. First get all KsmCredential, then find the best match.
                // TODO: Switch to ACL.SYSTEM2 when CredentialsProvider.lookupCredentials is updated.
                List<KsmCredential> credentials = CredentialsProvider.lookupCredentials(
                        KsmCredential.class,
                        (Item) null,
                        ACL.SYSTEM,
                        Collections.emptyList()
                );
                KsmCredential credential = CredentialsMatchers.firstOrNull(credentials,
                        CredentialsMatchers.withId(credentialsId));
                if (credential == null) {
                    throw new Exception(KsmCommon.errorPrefix + "Cannot find the credential id " + credentialsId);
                }

                Map<String, KsmNotationItem> notationItems = new HashMap<String, KsmNotationItem>();
                KsmQuery query = new KsmQuery(credential);

                for(KsmSecret secret : application.getSecrets()) {
                    try {
                        LOGGER.log(Level.FINE, "Parsing notation " + secret.getNotation() + " for env var " + secret.getEnvVar());
                        KsmNotationItem notationItem = KsmNotation.parse(secret.getEnvVar(), secret.getNotation(), false);
                        notationItems.put(secret.getEnvVar(), notationItem);
                    }
                    catch(Exception e) {
                        throw new Exception(KsmCommon.errorPrefix + "Could not replace env var " + secret.getEnvVar() +
                                ":" + e.getMessage());
                    }
                }

                try {
                    // Run the notation from the global environmental variables first.
                    query.run(globalNotationItems);

                    // Then run the environmental variables from this application.
                    query.run(notationItems);
                }
                catch(Exception e) {
                    throw new Exception(KsmCommon.errorPrefix + "The environmental variable replace had problems: "
                            + e.getMessage());
                }

                // Set the environmental variables values from global. These are allowed to fail.
                for(Map.Entry<String, KsmNotationItem> entry: globalNotationItems.entrySet()) {
                    KsmNotationItem notationItem = entry.getValue();
                    if (notationItem.getError() == null) {
                        envVars.put(entry.getKey(), notationItem.getValue());
                    }
                    else {
                        String msg = "The env var " + entry.getKey() + " notation "  + notationItem.getNotation()
                                + " could not be found for credential id " + credentialsId + ". It may have been found"
                                + " for another credential, if you are using multiple credentials: "
                                + notationItem.getError();
                        listener.getLogger().println(msg);
                        LOGGER.log(Level.INFO, msg);
                    }
                }

                // Set the environmental variables values from workflow.
                for(Map.Entry<String, KsmNotationItem> entry: notationItems.entrySet()) {
                    KsmNotationItem notationItem = entry.getValue();
                    envVars.put(entry.getKey(), notationItem.getValue());
                }

                // Add the config of the credential to the env var. This config will allow any KSM apps within the
                // build to be used. The allowConfigInject boolean has to be true for this to happen. The existing and
                // new are the same since env vars are contained in the step.
                KsmCommon.addCredentialToEnv(credential, envVars, envVars);
            }

            getContext().newBodyInvoker().
                    withContext(
                            EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class),
                                    new Overrider(envVars))).
                    withCallback(new doFinished(envVars)).
                    start();

            LOGGER.log(Level.FINE, "Finishing Keeper Secrets Manager workflow step");
        }

        private final class doFinished extends TailCall {

            private static final long serialVersionUID = 1;

            private final EnvVars envVars;

            doFinished( EnvVars envVars ) {
                this.envVars = envVars;
            }

            @Override protected void finished(StepContext context) throws Exception {
                new Callback(envVars).finished(context);
            }
        }
    }

    private static final class Overrider extends EnvironmentExpander {

        private static final long serialVersionUID = 1;

        private final EnvVars envVars;

        Overrider(EnvVars envVars) {
            this.envVars = envVars;
        }

        @Override
        public void expand(@NonNull EnvVars env) throws IOException, InterruptedException {
            for (HashMap.Entry<String,String> envVar : envVars.entrySet()) {
                env.override(envVar.getKey(), envVar.getValue());
            }
        }

        private Object readResolve() {
            return this;
        }
    }

    private static final class Callback extends BodyExecutionCallback.TailCall {

        private static final long serialVersionUID = 1;

        private final EnvVars envVars;

        Callback(EnvVars envVars) {
            this.envVars = envVars;
        }

        @Override
        protected void finished(StepContext context) {
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "withKsm";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Inject secrets with Keeper Secrets Manager";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
           return Collections.unmodifiableSet(
                    new HashSet<>(Arrays.asList(TaskListener.class, Run.class, EnvVars.class))
            );
        }
    }
}