package io.jenkins.plugins.ksm.credential;

import com.keepersecurity.secretsManager.core.LocalConfigStorage;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import io.jenkins.plugins.ksm.KsmQuery;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class KsmCredential extends BaseStandardCredentials {

    private String token;
    private Secret clientId;
    private Secret privateKey;
    private Secret appKey;
    private String hostname;
    private boolean skipSslVerification;
    private boolean allowConfigInject;

    public final static String tokenErrorPrefix = "Error:";

    @DataBoundConstructor
    public KsmCredential(CredentialsScope scope, String id, String description,
                         String token,
                         Secret clientId, Secret privateKey, Secret appKey,
                         String hostname,
                         boolean skipSslVerification, boolean allowConfigInject) {
        super(scope, id, description);

        // If the token is not blank, or already an error, redeem the token.
        if (!token.trim().equals("") && (!token.trim().startsWith(KsmCredential.tokenErrorPrefix))){
            try {
                LocalConfigStorage storage = KsmQuery.redeemToken(token, hostname);
                clientId = Secret.fromString(storage.getString("clientId"));
                appKey = Secret.fromString(storage.getString("appKey"));
                privateKey = Secret.fromString(storage.getString("privateKey"));
                token = "";
            }
            catch(Exception e) {

                // Why do this? Need a way to show the error. We can't throw an error or Jenkins will go to a 500
                // error message page. Only way is to store the error in the token ... until we find a better way.
                token = tokenErrorPrefix + " " + e.getMessage();
            }
        }

        // If keys are set, make sure token is blank.
        if (
                (!Secret.toString(clientId).equals(""))
            && (!Secret.toString(privateKey).equals(""))
            && (!Secret.toString(appKey).equals(""))
        ){
            token = "";
        }

        this.token = token.trim();
        this.clientId = clientId;
        this.privateKey = privateKey;
        this.appKey = appKey;
        this.hostname = hostname.trim();
        this.skipSslVerification = skipSslVerification;
        this.allowConfigInject = allowConfigInject;
    }

    public String getToken() {
        return token;
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
    public boolean getSkipSslVerification() {
        return skipSslVerification;
    }
    public boolean getAllowConfigInject() {
        return allowConfigInject;
    }

    public String getCredentialError() {
        return token;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Keeper Secrets Manager";
        }

        // If the token has the start with "Error" then there was a problem with the token. The field
        // token will have the actual error.
        public FormValidation doCheckToken(@QueryParameter String token) {
            if(token.startsWith(KsmCredential.tokenErrorPrefix)) {
                return FormValidation.error("There appears to be an error with the token.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckHostname(@QueryParameter String hostname) {
            if(hostname.trim().equals("")) {
                return FormValidation.error("Hostname cannot be blank.");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doTestCredential(
                @QueryParameter String hostname,
                @QueryParameter String clientId,
                @QueryParameter String privateKey,
                @QueryParameter String appKey,
                @QueryParameter boolean skipSslVerification
        ) {
            String error = KsmQuery.testCredentials(clientId, privateKey, appKey, hostname, skipSslVerification);
            if (error != null) {
                return FormValidation.error(error);
            }

            return FormValidation.ok();
        }
    }
}