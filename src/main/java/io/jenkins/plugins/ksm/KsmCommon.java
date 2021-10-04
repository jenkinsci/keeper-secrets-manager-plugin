package io.jenkins.plugins.ksm;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Base64;

public class KsmCommon {

    public static final String errorPrefix = "Keeper Secrets Manager: ";
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
            items.add(name, item.getPublicId());
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
