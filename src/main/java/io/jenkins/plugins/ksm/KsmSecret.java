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

    private String envVar;
    private String notation;
    private String destination;
    private String filePath;

    public static final String destinationEnvVar = "env";
    public static final String destinationFilePath = "file";

    @DataBoundConstructor
    public KsmSecret(String notation, String destination, String envVar, String filePath) {
        this.notation = notation;
        this.destination = destination;
        this.envVar = envVar;
        this.filePath = filePath;
    }

    public String getNotation() {
        return notation;
    }
    public String getDestination() {
        return destination;
    }
    public String getEnvVar() {
        return envVar;
    }
    public String getFilePath() {
        return filePath;
    }


    @DataBoundSetter
    public void setNotation(String notation) {
        this.notation = notation;
    }
    @DataBoundSetter
    public void setDestination(String destination) {
        this.destination = destination;
    }
    @DataBoundSetter
    public void setEnvVar(String envVar) {
        this.envVar = envVar;
    }
    @DataBoundSetter
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void validate() throws Exception{

        if ((destination == null) || (destination.equals(""))) {
            throw new Exception("The destination is missing or blank.");
        }
        if ( (!destination.equals(KsmSecret.destinationEnvVar)) && (!destination.equals(KsmSecret.destinationFilePath))) {
            throw new Exception("The destination is not correct. Valid values is either 'env' or 'file'");
        }
        if ((destination.equals(KsmSecret.destinationEnvVar) && ((envVar==null)||envVar.equals("")))){
            throw new Exception("The envVar value is missing or blank");
        }
        if ((destination.equals(KsmSecret.destinationFilePath) && ((filePath==null)||filePath.equals("")))){
            throw new Exception("The filePath value is missing or blank");
        }
        if ((notation == null) || (notation.equals(""))) {
            throw new Exception("The notation is missing or blank.");
        }
        // When the notation is parsed, it will validate its structure.

        // if we go here we are good
    }

    // Used by the UI for the selecting the active option in a select box.
    public String isDestinationType(String destination) {
        return this.destination.equalsIgnoreCase(destination) ? "true" : "";
    }

    public String getName() {
        return KsmSecret.buildSecretName(destination, envVar, filePath);
    }

    public static String buildSecretName(String destination, String envVar, String filePath) {
        if ((destination.equals(KsmSecret.destinationEnvVar)) && (envVar != null)) {
            return envVar;
        }
        return filePath;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<KsmSecret> {

        public FormValidation doCheckFilePath(@QueryParameter String value) {
            if (value.equals("")) {
                return FormValidation.error("The file path cannot be blank.");
            }
            else if (!KsmCommon.validFilePath(value)) {
                return FormValidation.error("The file path is invalid.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckEnvVar(@QueryParameter String value) {

            // Make sure the env var name is not blank.
            if ((value == null) || value.trim().equals("")) {
                return FormValidation.error("The environmental variable cannot be blank.");
            }

            // https://pubs.opengroup.org/onlinepubs/9699919799/utilities/V3_chap02.html#tag_18_10_02
            Pattern pattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
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
