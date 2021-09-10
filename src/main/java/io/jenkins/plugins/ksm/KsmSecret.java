package io.jenkins.plugins.ksm;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KsmSecret extends AbstractDescribableImpl<KsmSecret> {

    private transient String envVar;
    private transient String notation;

    @DataBoundConstructor
    public KsmSecret(String envVar, String notation) {
        this.envVar = envVar.trim();
        this.notation = notation.trim();
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

        public FormValidation doCheckEnvVar(@QueryParameter String value) {

            // Make sure the env var name is not blank.
            if ((value == null) || value.trim().equals("")) {
                return FormValidation.error("The environmental variable can not be blank.");
            }

            // https://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html#tag_18_10_02
            Pattern pattern = Pattern.compile("^[a-zA-Z_]+[a-zA-Z0-9_]*$");
            Matcher matcher = pattern.matcher(value.trim());
            if (!matcher.find()) {
                return FormValidation.error("The environmental variable contains invalid characters.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckNotation(@QueryParameter String value) {

            try {
                KsmNotation.parse("TEST", value, true);
            }
            catch(Exception e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok();
        }
    }
}
