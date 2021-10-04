package io.jenkins.plugins.ksm;

import hudson.Extension;
import java.util.List;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import hudson.model.ItemGroup;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class KsmApplication extends AbstractDescribableImpl<KsmApplication> {

    private String credentialPublicId;
    private List<KsmSecret> secrets;

    @DataBoundConstructor
    public KsmApplication(String credentialPublicId, List<KsmSecret> secrets) {
        this.credentialPublicId = credentialPublicId;
        this.secrets = secrets;
    }

    public String getCredentialPublicId() {
        return credentialPublicId;
    }
    public List<KsmSecret> getSecrets() {
        return secrets;
    }

    @DataBoundSetter
    public void setCredentialPublicId(String credentialPublicId) {
        this.credentialPublicId = credentialPublicId;
    }
    @DataBoundSetter
    public void setSecrets(List<KsmSecret> secrets) {
        this.secrets = secrets;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<KsmApplication> {

        public ListBoxModel doFillCredentialPublicIdItems(@AncestorInPath ItemGroup context) {
            return KsmCommon.buildCredentialsIdListBox(context);
        }
    }
}
