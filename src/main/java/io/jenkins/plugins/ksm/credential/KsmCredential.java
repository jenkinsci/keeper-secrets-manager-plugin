package io.jenkins.plugins.ksm.credential;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import io.jenkins.plugins.ksm.KsmQuery;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class KsmCredential extends BaseStandardCredentials {

    private Secret clientId;
    private Secret privateKey;
    private Secret appKey;
    private String hostname;
    private boolean useSkipSslVerification;
    private String keyId;
    private boolean allowConfigInject;

    @DataBoundConstructor
    public KsmCredential(CredentialsScope scope, String id, String description,
                         Secret clientId, Secret privateKey, Secret appKey,
                         String hostname, boolean useSkipSslVerification, String keyId, boolean allowConfigInject) {
        super(scope, id, description);

        this.clientId = clientId;
        this.privateKey = privateKey;
        this.appKey = appKey;
        this.hostname = hostname;
        this.useSkipSslVerification =useSkipSslVerification;
        this.keyId = keyId;
        this.allowConfigInject = allowConfigInject;
    }

    public Secret getClientId() {
        return clientId;
    }
    public Secret getPrivateKey() {
        return privateKey;
    }
    public Secret getAppKey() {
        return appKey;
    }
    public String getHostname() {
        return hostname;
    }
    public boolean getUseSkipSslVerification() {
        return useSkipSslVerification;
    }
    public String getKeyId() {
        return keyId;
    }
    public boolean allowConfigInject() {
        return allowConfigInject;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return "Keeper Secrets Manager";
        }

        public FormValidation doTestCredential(
                @QueryParameter String hostname,
                @QueryParameter String clientId,
                @QueryParameter String privateKey,
                @QueryParameter String appKey,
                @QueryParameter String useSkipSslVerification
        ) {
            String error = KsmQuery.testCredentials(clientId, privateKey, appKey, hostname);
            if (error != null) {
                return FormValidation.error(error);
            }

            return FormValidation.ok();
        }
    }
}