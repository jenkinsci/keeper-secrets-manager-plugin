package io.jenkins.plugins.ksm;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import jenkins.model.Jenkins;
import net.sf.json.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KsmCommon {

    public static final String errorPrefix = "Keeper Secrets Manager: ";
    public static final String configPrefix = "KSM_CONFIG_BASE64_";
    public static final String sdkConfigEnvName = "KSM_CONFIG";
    public static final String configDescPrefix = "KSM_CONFIG_BASE64_DESC_";
    public static final String configCountKey = "KSM_CONFIG_COUNT";

    // Using ACL.SYSTEM, the cred stuff doesn't support ACL.SYSTEM2 yet.
    // TODO: Switch to ACL.SYSTEM2 when CredentialsProvider supports it.
    @SuppressWarnings("deprecation")
    public static ListBoxModel buildCredentialsIdListBox(Item item, String credentialsId) {

        Jenkins instance = Jenkins.get();
        StandardListBoxModel result = new StandardListBoxModel();
        if (item == null) {
            if (!instance.hasPermission(Jenkins.ADMINISTER)) {
                return result.includeCurrentValue(credentialsId); // (2)
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ)
                    && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return result.includeCurrentValue(credentialsId); // (2)
            }
        }
        return result
                .includeEmptyValue()
                .includeMatchingAs(
                        ACL.SYSTEM,
                        instance,
                        KsmCredential.class,
                        Collections.emptyList(),
                        CredentialsMatchers.always()
                )
                .includeCurrentValue(credentialsId);
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
            String json = obj.toString();
            String configBase64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            // Get the credential's description. If blank, make one based on the config #.
            String description = credential.getDescription();
            if (description.equals("")) {
                description = "CONFIG_PROFILE_" + configCount;
            }

            // Place the config and desc into the env var for the CLI
            newEnvVars.put(configCountKey, String.valueOf(configCount));
            newEnvVars.put(configPrefix + configCount, configBase64);
            newEnvVars.put(configDescPrefix + configCount, description);

            // Place the config for any of the SDKs. Can only do one, so do the first.
            if (configCount == 1 ) {
                newEnvVars.put(sdkConfigEnvName, configBase64);
            }

            if (credential.getSkipSslVerification()) {
                newEnvVars.put("KSM_SKIP_VERIFY", "True");
            }
        }
    }

    public static boolean validFilePath(String fileStr) {

        // Since we are concat paths, don't allow the file to start with / or \
        if (fileStr.startsWith("/") || fileStr.startsWith("\\")) {
            return false;
        }
        Pattern pattern = Pattern.compile("^.:\\\\|/", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fileStr);
        boolean matchFound = matcher.find();
        if (matchFound) {
            return false;
        }

        File file = new File(fileStr);
        do {
            // Not sure if possible in Jenkins FilePath, however don't allow walk back up directory tree. No
            // reason to ever use .. a file path.
            if(file.getName().equals("..")) {
                return false;
            }
            file = file.getParentFile();
        } while ((file != null) && (file.getParentFile() != null));

        return true;
    }


    public static void writeFileToWorkspace(FilePath workspace, String fileName, Object value) throws IOException, InterruptedException {

        // Make sure the file name is a valid location for a secret.
        if (fileName.equals("")) {
            throw new AbortException("The file path is blank. Cannot save secret.");
        }
        if (!validFilePath(fileName)) {
            throw new AbortException("The file path " + fileName + " is invalid. Cannot save secret to that file path.");
        }

        FilePath dir = workspace.child(fileName).getParent();
        if (dir != null) {
            dir.mkdirs();
        }

        OutputStream bos = workspace.child(fileName).write();
        if (value instanceof String) {
            value = ((String) value).getBytes(Charset.defaultCharset());
        }
        bos.write((byte[]) value);
        bos.close();
    }
 }
