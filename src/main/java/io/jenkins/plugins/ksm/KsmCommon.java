package io.jenkins.plugins.ksm;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.EnvVars;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Base64;

public class KsmCommon {

    public static final String errorPrefix = "Keeper Secrets Manager ERROR: ";
    public static final String configPrefix = "KSM_CONFIG_BASE64_";
    public static final String configDescPrefix = "KSM_CONFIG_BASE64_DESC_";
    public static final String configCountKey = "KSM_CONFIG_COUNT";

    // Using ACL.SYSTEM, the cred stuff doesn't support ACL.SYSTEM2 yet.
    // TODO: Switch to ACL.SYSTEM2 when CredentialsProvider supports it.
    @SuppressWarnings("deprecation")
    public static ListBoxModel buildCredentialsIdListBox(ItemGroup<?> context) {

        final ListBoxModel items = new ListBoxModel();


        List<KsmCredential> ksmCredentials = CredentialsProvider.lookupCredentials(
                KsmCredential.class,
                context,
                ACL.SYSTEM,
                Collections.emptyList()
        );
        for (KsmCredential item : ksmCredentials) {
            String name = item.getDescription();
            if (name.equals("")) {
                name = item.getId();
            }
            items.add(name, item.getId());
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    public static void addCredentialToEnv(KsmCredential credential, EnvVars newEnvVars, EnvVars existingEnvVars) {

        if (credential.getAllowConfigInject()) {

            // Find the current config count from the existing env var and increment it
            String configCountStr = existingEnvVars.get(configCountKey);
            if(configCountStr == null) {
                configCountStr = "0";
            }
            int configCount = Integer.parseInt(configCountStr);
            configCount += 1;

            // Set the count in the existing and the new env vars.
            newEnvVars.put(configCountKey, String.valueOf(configCount));
            existingEnvVars.put(configCountKey, String.valueOf(configCount));

            JSONObject obj = new JSONObject();
            obj.put("clientId", Secret.toString(credential.getClientId()));
            obj.put("privateKey", Secret.toString(credential.getPrivateKey()));
            obj.put("appKey", Secret.toString(credential.getAppKey()));
            obj.put("hostname", credential.getHostname());
            String json = obj.toJSONString();
            String configBase64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            // Get the credential's description. If blank, make one based on the config #.
            String description = credential.getDescription();
            if (description.equals("")) {
                description = "CONFIG_PROFILE_" + configCount;
            }

            // Place the config and desc into the env var
            newEnvVars.put(configCountKey, String.valueOf(configCount));
            newEnvVars.put(configPrefix + configCount, configBase64);
            newEnvVars.put(configDescPrefix + configCount, description);

            if (credential.getUseSkipSslVerification()) {
                newEnvVars.put("KSM_SKIP_VERIFY", "True");
            }
        }
    }
}
