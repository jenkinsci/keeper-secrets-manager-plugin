package io.jenkins.plugins.ksm.creds;

import hudson.Extension;
import hudson.util.Secret;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import org.kohsuke.stapler.DataBoundConstructor;


public class KsmCredentials extends BaseStandardCredentials {

    private Secret clientKey;
    private Secret clientId;
    private Secret privateKey;
    private Secret appKey;
    private String hostname;
    private Boolean useSkipSslVerification;
    private String keyId;

    @DataBoundConstructor
    public KsmCredentials(CredentialsScope scope, String id, String description,
                          Secret clientKey, Secret clientId, Secret privateKey, Secret appKey,
                          String hostname, Boolean useSkipSslVerification, String keyId) {
        super(scope, id, description);
        this.clientKey = clientKey;
        this.clientId = clientId;
        this.privateKey = privateKey;
        this.appKey = appKey;
        this.hostname = hostname;
        this.useSkipSslVerification =useSkipSslVerification;
        this.keyId = keyId;
    }


    public Secret getClientKey() {
        return clientKey;
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

    public Boolean getUseSkipSslVerification() {
        return useSkipSslVerification;
    }
    public String getKeyId() {
        return keyId;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return "Keeper Secrets Manager";
        }
    }
}