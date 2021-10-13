package io.jenkins.plugins.ksm;

import hudson.Extension;
import java.util.List;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class KsmApplication extends AbstractDescribableImpl<KsmApplication> {

    private String credentialsId;
    private List<KsmSecret> secrets;

    @DataBoundConstructor
    public KsmApplication(String credentialsId, List<KsmSecret> secrets) {
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
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }
    @DataBoundSetter
    public void setSecrets(List<KsmSecret> secrets) {
        this.secrets = secrets;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<KsmApplication> {

        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item item,
                @QueryParameter String credentialsId
        ) {
            return KsmCommon.buildCredentialsIdListBox(item, credentialsId);
        }
    }
}
