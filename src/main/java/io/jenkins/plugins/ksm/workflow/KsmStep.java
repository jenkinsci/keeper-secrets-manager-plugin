package io.jenkins.plugins.ksm.workflow;

import hudson.FilePath;
import hudson.Launcher;
import hudson.console.ConsoleLogFilter;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.notation.KsmNotationRequest;
import org.jenkinsci.plugins.workflow.steps.*;
import hudson.Extension;

import java.io.IOException;
import java.util.*;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.EnvVars;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;

import io.jenkins.plugins.ksm.KsmSecret;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.notation.KsmNotationRequest;


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
        private transient KsmStep step;

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
            TaskListener listener = getContext().get(TaskListener.class);
            listener.getLogger().println("Doing the step");

            // For each application, for all the secrets, replace them with actual secrets.
            HashMap<String,String> envVars = new HashMap<>();
            for (KsmApplication application : step.application) {
                for(KsmSecret secret : application.getSecrets()) {

                    try {
                        KsmNotationRequest req = KsmNotation.parse(secret.getNotation());
                        // TODO: Do replace

                        envVars.put(secret.getEnvVar(), secret.getNotation());
                    }
                    catch(Exception e) {
                        listener.getLogger().println("Could not replace env var " + secret.getEnvVar() + ":" + e.getMessage());
                    }
                }
            }

            getContext().newBodyInvoker().
                    withContext(
                            EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class),
                                    new Overrider(envVars))).
                    //withCallback(new doFinished(envVars)).
                    start();
        }

        private final class doFinished extends TailCall {

            private static final long serialVersionUID = 1;

            private final HashMap<String,String> envVars;

            doFinished( HashMap<String,String> envVars ) {
                this.envVars = envVars;
            }

            @Override protected void finished(StepContext context) throws Exception {
                new Callback(envVars).finished(context);
            }
        }
    }

    private static final class Overrider extends EnvironmentExpander {

        private static final long serialVersionUID = 1;

        private HashMap<String, String> envVars;

        Overrider(HashMap<String,String> envVars) {
            this.envVars = envVars;
        }

        @Override public void expand(EnvVars env) throws IOException, InterruptedException {
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

        private final HashMap<String,String> envVars;

        Callback(HashMap<String,String> envVars) {
            this.envVars = envVars;
        }

        @Override
        protected void finished(StepContext context) throws Exception {
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