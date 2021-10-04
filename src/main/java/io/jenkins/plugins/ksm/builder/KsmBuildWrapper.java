package io.jenkins.plugins.ksm.builder;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.console.ConsoleLogFilter;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import io.jenkins.plugins.ksm.KsmApplication;
import io.jenkins.plugins.ksm.KsmCommon;
import io.jenkins.plugins.ksm.KsmSecret;
import io.jenkins.plugins.ksm.Messages;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.log.KsmBuildConsoleLogFilter;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.notation.KsmNotationItem;
import org.kohsuke.stapler.DataBoundConstructor;
import java.io.*;
import java.util.*;

public class KsmBuildWrapper extends BuildWrapper {

    private List<KsmApplication> application;
    private KsmNotation notation;
    protected PrintStream consoleLogger;
    private List<String> secretValues;
    private List<String> secretFiles;
    private Map<String, KsmNotationItem> notationItems;
    private String systemSecretError;

    @DataBoundConstructor
    public KsmBuildWrapper(List<KsmApplication> application) {
        this.application = application;
        this.notation = new KsmNotation();
    }
    public KsmBuildWrapper(List<KsmApplication> application, KsmNotation notation) {
        this.application = application;
        this.notation = notation;
    }

    public List<KsmApplication> getApplication() {
        return application;
    }
    public KsmNotation getNotation() {
        return notation;
    }

    protected void getSecrets() {

        notationItems = new HashMap<>();
        secretValues = new ArrayList<>();

        // Since we are initializing this from the console, any exceptions just kill the console. Handle the errors
        // in the setup.
        try {
            for (KsmApplication app : application) {

                KsmCredential credential = null;
                try {
                    credential = KsmCredential.getCredentialFromPublicId(app.getCredentialPublicId());
                } catch (Exception e) {
                    throw new AbortException(KsmCommon.errorPrefix + e.getMessage());
                }
                if (!credential.getCredentialError().equals("")) {
                    throw new AbortException(KsmCommon.errorPrefix
                            + "The credential has errors associated with it. Cannot not use.");
                }

                // Parse the secrets set in the build environment.
                for (KsmSecret secretItem : app.getSecrets()) {
                    // Allow failure will populate the error var in the notation item. No need to
                    // catch any exceptions.
                    KsmNotationItem notationItem;
                    try {
                        // TODO - allowFailure is for the 'run' not the 'parse'. If the parse fails
                        //  no item is created :/
                        notationItem = KsmNotation.parse(secretItem, false);
                        notationItems.put(notationItem.getName(), notationItem);
                    }
                    catch(Exception e ) {
                        notationItem = new KsmNotationItem(secretItem.getDestination(),
                                secretItem.getEnvVar(), secretItem.getFilePath(), e.getMessage());

                    }
                    notationItems.put(notationItem.getName(), notationItem);
                }

                try {
                    this.getNotation().run(credential, notationItems);
                } catch (Exception e) {
                    // This would be a like a network error, or the server is down.
                    throw new AbortException(KsmCommon.errorPrefix + "The secret replacement had problems: "
                            + e.getMessage());
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }

            for (Map.Entry<String, KsmNotationItem> entry : notationItems.entrySet()) {
                KsmNotationItem notationItem = entry.getValue();
                if (notationItem.getError() == null) {
                    Object value = notationItem.getValue();

                    // Make sure we are not trying to put binary data into an environmental variable.
                    if (notationItem.isDestinationEnvVar()) {
                        if (!(value instanceof String)) {
                            notationItem.setError("Attempted to store binary data in an environmental variable.");
                        }
                    }

                    // Only add the value if value is String.
                    if (value instanceof String) {
                        secretValues.add((String) notationItem.getValue());
                    }
                    // TODO - Try to figure out how to redact binary data. All attempts failed so far.
                    // What was done
                    // 1. Attempted a string with multiple character encoding.
                    // 2. Create a regex of binary matches \x81\x12\x33 (no \Q \E)
                    //
                    // Might have to not use regular expressions. Might need to scan char to char for matched. Yuck!
                }
            }
        } catch (Exception e) {
            systemSecretError = e.toString();
        }
    }

    @Override
    public Environment setUp(AbstractBuild build, final Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {

        // List of files we created that we need to delete when we tearDown
        secretFiles = new ArrayList<>();

        // Did we have a systemic error?
        if (systemSecretError != null) {
            throw new AbortException(KsmCommon.errorPrefix + systemSecretError);
        }

        // Check for error in parsing notation. Be nice a make a list of all the errors instead of aborting
        // on the first one.
        List<String> errors = new ArrayList<>();
        for(Map.Entry<String, KsmNotationItem> entry: notationItems.entrySet()) {
            KsmNotationItem notationItem = entry.getValue();
            if ( notationItem.getError() != null ) {
                errors.add(notationItem.getName() + "; " + notationItem.getError());
            }
        }
        if (errors.size() > 0) {
            // Throw a list of errors with the notation replacement.
            throw new AbortException(KsmCommon.errorPrefix + "Had " + errors.size() + " error getting secret values: "
                    + String.join(",  ", errors));
        }

        FilePath workspace = build.getWorkspace();

        return new Environment() {

            @Override
            public void buildEnvVars(Map<String, String> env) {

                for (Map.Entry<String, KsmNotationItem> entry : notationItems.entrySet()) {
                    KsmNotationItem notationItem = entry.getValue();

                    if (notationItem.getError() == null) {
                        if (notationItem.isDestinationEnvVar()) {

                            // At this point we know the value is a String.
                            String value = (String) notationItem.getValue();

                            if (value.contains("$")) {
                                value = value.replace("$", "$$$$");
                            }
                            env.put(notationItem.getEnvVar(), value);
                        } else {
                            try {
                                KsmCommon.writeFileToWorkspace(
                                        workspace,
                                        notationItem.getFilePath(),
                                        notationItem.getValue()
                                );
                                secretFiles.add(notationItem.getFilePath());
                            } catch (IOException | InterruptedException e) {
                                errors.add("Could not write secret file " + notationItem.getFilePath() + ": "
                                        + e.getMessage());
                            }
                        }
                    } else {
                        errors.add(notationItem.getError());
                    }
                }
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) {

                for(String filePath: secretFiles) {
                    try {
                        FilePath f = workspace.child(filePath);
                        f.delete();
                    }
                    catch(Exception e) {
                        System.out.println("Could not delete: " + e.getMessage());
                    }
                }
                return true;
            }
        };
    }

    @Override
    public void makeSensitiveBuildVariables(AbstractBuild build, Set<String> sensitiveVariables) {
        sensitiveVariables.addAll(secretValues);
    }

    private static final class Filter extends ConsoleLogFilter {

        private final List<String> secretValues;
        private final String charsetName;

        Filter(List<String> secretValues, String charsetName) {
            this.secretValues = secretValues;
            this.charsetName = charsetName;
        }

        @Override
        public OutputStream decorateLogger(Run build, OutputStream logger) throws IOException, InterruptedException {
            return new KsmBuildConsoleLogFilter.MaskingOutputStream(logger, secretValues, charsetName) {

                @Override
                public void close() throws IOException {
                    super.close();
                }
            };
        }
    }

    @Override
    public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) throws IOException, InterruptedException, Run.RunnerAbortedException {

        // Load secrets here, so we have a list of secret values to give the console logger to redact.
        this.getSecrets();

        return new Filter(
                secretValues,
                build.getCharset().name()
        ).decorateLogger(build, logger);
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(KsmBuildWrapper.class);
            load();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.KsmBuilder_DescriptorImpl_DisplayName();
        }
    }
}
