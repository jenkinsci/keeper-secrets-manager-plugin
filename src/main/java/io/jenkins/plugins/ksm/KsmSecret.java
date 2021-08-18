package io.jenkins.plugins.ksm;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class KsmSecret extends AbstractDescribableImpl<KsmSecret> {

    private String envVar;
    private String notation;

    @DataBoundConstructor
    public KsmSecret(String envVar, String notation) {
        this.envVar = envVar;
        this.notation = notation;
    }

    public String getEnvVar() {
        return envVar;
    }
    public String getNotation() {
        return notation;
    }

    @DataBoundSetter
    public void setEnvVar(String envVar) {
        this.envVar = envVar;
    }
    @DataBoundSetter
    public void setNotation(String notation) {
        this.notation = notation;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<KsmSecret> {

    }
}
