package io.jenkins.plugins.ksm.builder;

import hudson.model.EnvironmentContributingAction;
import hudson.EnvVars;
import hudson.model.Run;

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public class KsmEnvironmentContributingAction implements EnvironmentContributingAction {

    private transient final Map<String, String> ksmEnvVars;

    public KsmEnvironmentContributingAction(@CheckForNull Map<String, String> ksmEnvVars) {
        this.ksmEnvVars = ksmEnvVars;
    }

    @Override
    public void buildEnvironment(@Nonnull Run<?,?> run, @CheckForNull EnvVars envVars) {

        if (envVars == null) {
            return;
        }

        if (ksmEnvVars == null) {
            return;
        }

        for (Map.Entry<String, String> entry : ksmEnvVars.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && value != null) {
                envVars.put(key, value);
            }
        }
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "KsmBuilderAction";
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
