package io.jenkins.plugins.ksm.workflow;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.FilePath;
import hudson.console.ConsoleLogFilter;
import io.jenkins.plugins.ksm.KsmApplication;
import io.jenkins.plugins.ksm.KsmCommon;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.log.KsmStepConsoleLogFilter;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.notation.KsmNotationItem;
import io.jenkins.plugins.ksm.notation.KsmTestNotation;
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
    public StepExecution start(StepContext context) {
        return new Execution(this, context);
    }

    protected static class Execution extends GeneralNonBlockingStepExecution {

        private static final long serialVersionUID = 1L;
        private final transient KsmStep step;

        public Execution(KsmStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        public boolean start() {
            run(this::doStart);
            return false;
        }

        private KsmNotation getNotationInstance() throws Exception {

            KsmNotation notation;
            // If there is a JSON file with fake records, use the test notation, so we don't call the actual
            // secrets' manager server.
            if (KsmTestNotation.hasDataFile()) {
                try {
                    notation = new KsmTestNotation();
                    ((KsmTestNotation) notation).loadJsonData();
                }
                catch(Exception e ) {
                    throw new AbortException("There was a problem loading the test JSON data: " + e.getMessage());
                }
            }
            else {
                notation = new KsmNotation();
            }
            return notation;
        }

        private void doStart() throws Exception {

            Logger LOGGER = Logger.getLogger(KsmStep.class.getName());
            LOGGER.log(Level.FINE, "Starting Keeper Secrets Manager workflow step");

            Run<?, ?> run = getContext().get(Run.class);
            assert run != null;
            TaskListener listener = getContext().get(TaskListener.class);
            assert listener != null;
            FilePath workspace = getContext().get(FilePath.class);
            assert workspace != null;

            // An array of secret values used to redact console logging.
            List<String> secretValues = new ArrayList<>();
            List<String> secretFiles = new ArrayList<>();

            // For each application, for all the secrets, replace them with actual secrets.
            EnvVars envVars = new EnvVars();
            for (KsmApplication application : step.application) {

                KsmCredential credential;
                try {
                    credential = KsmCredential.getCredentialFromId(application.getCredentialsId(), run.getParent());
                }
                catch(Exception e) {
                    throw new AbortException(KsmCommon.errorPrefix + e.getMessage());
                }
                if (!credential.getCredentialError().equals("")) {
                    throw new AbortException(KsmCommon.errorPrefix + "The credential '" + credential.getDescription()
                            + "' errors associated with it. Cannot not use.");
                }

                Map<String, KsmNotationItem> notationItems = new HashMap<>();
                for(KsmSecret secret : application.getSecrets()) {

                    // Make sure the Jenkinsfile script has valid values.
                    try {
                        secret.validate();
                    }
                    catch(Exception e) {
                        throw new AbortException(KsmCommon.errorPrefix + "The script has value problems: "
                            + e.getMessage());
                    }

                    // Get the name of secret which is either the env var name or file path.
                    String secretName = KsmSecret.buildSecretName(
                            secret.getDestination(),
                            secret.getEnvVar(),
                            secret.getFilePath()
                    );

                    // Parse the notation and check if it's valid.
                    try {
                        LOGGER.log(Level.FINE, "Parsing notation " + secret.getNotation() + " for secret "
                                + secretName);

                        KsmNotationItem notationItem = KsmNotation.parse(secret, false);
                        notationItems.put(secretName, notationItem);
                    }
                    catch(Exception e) {
                        throw new AbortException(KsmCommon.errorPrefix + "Could not parse the secret "
                                + secretName + ":" + e.getMessage());
                    }
                }

                try {
                    // Then run the environmental variables from this application.
                    getNotationInstance().run(credential, notationItems);
                }
                catch(Exception e) {
                    throw new AbortException(KsmCommon.errorPrefix + "The environmental variable replace had problems: "
                            + e.getMessage());
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

                // Set the environmental variables and file values from workflow.
                for(Map.Entry<String, KsmNotationItem> entry: notationItems.entrySet()) {
                    KsmNotationItem notationItem = entry.getValue();

                    Object value = notationItem.getValue();
                    if( notationItem.isDestinationEnvVar()) {
                        // Env Var have to be strings or the OS won't like them. Validate we have a string for the
                        // env var.
                        if (value instanceof String ) {
                            envVars.put(notationItem.getEnvVar(), (String) value);
                        }
                    }
                    else {
                        try {
                            KsmCommon.writeFileToWorkspace(
                                    workspace,
                                    notationItem.getFilePath(),
                                    value
                            );
                            secretFiles.add(notationItem.getFilePath());
                        } catch(IOException e) {
                            throw new AbortException(KsmCommon.errorPrefix + "Could not write secret to "
                                    + notationItem.getFilePath() + ": " + e.getMessage());
                        }
                    }
                    if (value instanceof String ) {
                        secretValues.add((String) value);
                    }
                }

                // Add the config of the credential to the env var. This config will allow any KSM apps within the
                // build to be used. The allowConfigInject boolean has to be true for this to happen. The existing and
                // new are the same since env vars are contained in the step.
                KsmCommon.addCredentialToEnv(credential, envVars, envVars);
            }

            getContext().newBodyInvoker()
                    .withContext(
                            EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class),
                                    new Overrider(envVars)))
                    .withContext(
                            BodyInvoker.mergeConsoleLogFilters(getContext().get(ConsoleLogFilter.class),
                                    new KsmStepConsoleLogFilter(run.getCharset().name(), secretValues)))
                    .withCallback(new doFinished(secretFiles, workspace))
                    .start();

            LOGGER.log(Level.FINE, "Finishing Keeper Secrets Manager workflow step");
        }

        // Apparently this is needed. Called with the withCallback. If removed, step won't finish.
        private final class doFinished extends TailCall {

            private static final long serialVersionUID = 2L;

            private final List<String> secretFiles;
            private final FilePath workspace;

            doFinished( List<String> secretFiles, FilePath workspace) {

                this.secretFiles = secretFiles;
                this.workspace = workspace;
            }

            @Override
            protected void finished(StepContext context) {
                new Callback(secretFiles, workspace).finished(context);
            }
        }
    }

    private static final class Overrider extends EnvironmentExpander {

        private static final long serialVersionUID = 3L;

        private final EnvVars envVars;

        Overrider(EnvVars envVars) {
            this.envVars = envVars;
        }

        @Override
        public void expand(@NonNull EnvVars env) {
            for (HashMap.Entry<String,String> envVar : envVars.entrySet()) {
                env.override(envVar.getKey(), envVar.getValue());
            }
        }

        private Object readResolve() {
            return this;
        }
    }

    private static final class Callback extends BodyExecutionCallback.TailCall {

        private static final long serialVersionUID = 1L;

        private final List<String> secretFiles;
        private final FilePath workspace;

        Callback(List<String> secretFiles, FilePath workspace) {

            this.secretFiles = secretFiles;
            this.workspace = workspace;
        }

        @Override
        protected void finished(StepContext context) {

            Logger LOGGER = Logger.getLogger(KsmStep.class.getName());
            for(String fileName : secretFiles) {
                try {
                    workspace.child(fileName).delete();
                }
                catch(Exception e) {
                    LOGGER.log(Level.WARNING, "Could not delete secret file " + fileName + ": " + e.getMessage());
                }
            }
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