package io.jenkins.plugins.ksm.credential;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.keepersecurity.secretsManager.core.LocalConfigStorage;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.Secret;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import io.jenkins.plugins.ksm.KsmQuery;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import jenkins.model.Jenkins;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.jenkins.ui.icon.Icon;
import org.jenkins.ui.icon.IconSet;
import org.jenkins.ui.icon.IconType;

public class KsmCredential extends BaseStandardCredentials {

    private String publicId;
    private String token;
    private Secret clientId;
    private Secret privateKey;
    private Secret appKey;
    private String hostname;
    private boolean skipSslVerification;
    private boolean allowConfigInject;

    public final static String tokenErrorPrefix = "Error:";
    public final static int tokenHashLength = 43;

    @DataBoundConstructor
    public KsmCredential(CredentialsScope scope, String id, String description, String publicId,
                         String token,
                         Secret clientId, Secret privateKey, Secret appKey,
                         String hostname,
                         boolean skipSslVerification, boolean allowConfigInject) {
        super(scope, id, description);

        // If the public id is blank, generate an id. Make it not look like a UUID so not to confuse it
        // with a real id.
        if(publicId.equals("")) {
            publicId = "PID-" + UUID.randomUUID().toString().replace("-","");
        }

        // If the token is not blank, or already an error, redeem the token.
        if (!token.trim().equals("") && (!token.trim().startsWith(KsmCredential.tokenErrorPrefix))){
            try {
                LocalConfigStorage storage = KsmQuery.redeemToken(token, hostname);
                clientId = Secret.fromString(storage.getString("clientId"));
                appKey = Secret.fromString(storage.getString("appKey"));
                privateKey = Secret.fromString(storage.getString("privateKey"));
                hostname = storage.getString("hostname");
                token = "";
            }
            catch(Exception e) {

                // Why do this? Need a way to show the error. We can't throw an error or Jenkins will go to a 500
                // error message page. Only way is to store the error in the token ... until we find a better way.
                token = tokenErrorPrefix + " " + e.getMessage();
            }
        }

        // If keys are set, make sure token is blank. It's been redeemed.
        if (
                (!Secret.toString(clientId).equals(""))
            && (!Secret.toString(privateKey).equals(""))
            && (!Secret.toString(appKey).equals(""))
        ){
            token = "";
        }

        this.publicId = publicId;
        this.token = token.trim();
        this.clientId = clientId;
        this.privateKey = privateKey;
        this.appKey = appKey;
        this.hostname = hostname;
        this.skipSslVerification = skipSslVerification;
        this.allowConfigInject = allowConfigInject;
    }

    public String getPublicId() {
        return publicId;
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

    public static KsmCredential getCredentialFromPublicId(String publicID) throws Exception {

        // Validate a public id was passed in. This in mainly for script where a person can mistype the id.
        if (publicID == null) {
            throw new Exception("The public credential id is not defined.");
        }
        if (publicID.equals("")) {
            throw new Exception("The public credential id is blank.");
        }
        if (!publicID.startsWith("PID-") || publicID.length() != 36) {
            throw new Exception("The provided public credential id does not look correct. It's either missing the "
                + "PID prefix or is not the correct length.");
        }

        // TODO: Switch to ACL.SYSTEM2 when CredentialsProvider.lookupCredentials is updated.
        List<KsmCredential> credentials = CredentialsProvider.lookupCredentials(
                KsmCredential.class,
                (Item) null,
                ACL.SYSTEM,
                Collections.emptyList()
        );
        for(KsmCredential credential: credentials) {
            if(credential.getPublicId().equals(publicID)) {
                return credential;
            }
        }
        throw new Exception("Cannot find the credential for the public id.");
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

            // Token is allowed to be blank
            if(token.equals("")) {
                return FormValidation.ok();
            }

            if(token.startsWith(KsmCredential.tokenErrorPrefix)) {
                return FormValidation.error("There appears to be an error with the token.");
            }
            if(token.contains(":")) {
                // If the token has the region code, split and check the token size.
                String[] tokenParts = token.split(":");
                if(tokenParts.length != 2) {
                    return FormValidation.error("The token appears not to be the correct formatted.");
                }
                if(tokenParts[0].equals("")) {
                    return FormValidation.error("The token region code, before the colon, appears to be blank.");
                }
                if(tokenParts[1].length() != KsmCredential.tokenHashLength) {
                    return FormValidation.error("The token hash, after the colon, appears not to be the correct length.");
                }
            }
            else {
                if(token.length() != KsmCredential.tokenHashLength) {
                    return FormValidation.error("The token appears not to be the correct length.");
                }
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckHostname(@QueryParameter String hostname) {
            if(hostname.trim().equals("")) {
                return FormValidation.error("Hostname cannot be blank.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckDescription(@QueryParameter String description) {
            if(description.trim().equals("")) {
                return FormValidation.error("Description cannot be blank.");
            }
            return FormValidation.ok();
        }

        @POST
        @Restricted(NoExternalUse.class)
        public FormValidation doTestCredential(
                @QueryParameter String hostname,
                @QueryParameter String clientId,
                @QueryParameter String privateKey,
                @QueryParameter String appKey,
                @QueryParameter boolean skipSslVerification
        ) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            String error = KsmQuery.testCredentials(clientId, privateKey, appKey, hostname, skipSslVerification);
            if (error != null) {
                return FormValidation.error(error);
            }

            return FormValidation.ok();
        }

        public String getIconClassName() {
            return "icon-ksm";
        }

        static {
            for (String name : new String[]{
                    "ksm"
            }) {
                IconSet.icons.addIcon(new Icon(
                        String.format("icon-ksm icon-sm", name),
                        String.format("keeper-secrets-manager/images/%s.svg", name),
                        Icon.ICON_SMALL_STYLE, IconType.PLUGIN)
                );
                IconSet.icons.addIcon(new Icon(
                        String.format("icon-ksm icon-md", name),
                        String.format("keeper-secrets-manager/images/%s.svg", name),
                        Icon.ICON_MEDIUM_STYLE, IconType.PLUGIN)
                );
                IconSet.icons.addIcon(new Icon(
                        String.format("icon-ksm icon-lg", name),
                        String.format("keeper-secrets-manager/images/%s.svg", name),
                        Icon.ICON_LARGE_STYLE, IconType.PLUGIN)
                );
                IconSet.icons.addIcon(new Icon(
                        String.format("icon-ksm icon-xlg", name),
                        String.format("keeper-secrets-manager/images/%s.svg", name),
                        Icon.ICON_XLARGE_STYLE, IconType.PLUGIN)
                );
            }
        }
    }
}