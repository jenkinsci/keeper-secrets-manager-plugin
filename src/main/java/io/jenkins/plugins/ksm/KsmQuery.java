package io.jenkins.plugins.ksm;

import com.keepersecurity.secretsManager.core.*;
import net.sf.json.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;


public class KsmQuery {

    private static final Logger logger = Logger.getLogger(KsmQuery.class.getName());

    private static String handleException(Exception e) {

        String msg = e.getMessage();

        // See if this is one of our error message
        try {
            JSONObject json = JSONObject.fromObject(msg);
            msg = json.getString("message");
        } catch (JSONException ignore) {
            // Don't do anything. Keep msg the same.
        }

        return msg;
    }

    public static LocalConfigStorage redeemToken(String token, String hostname,
                                                 boolean allowUnverifiedCertificate) throws Exception {

        logger.log(Level.FINE, "Redeem token " + token +" from host " + hostname + "; Skip SSL = " +
                allowUnverifiedCertificate);

        LocalConfigStorage storage = new LocalConfigStorage();
        try {
            SecretsManager.initializeStorage(storage, token, hostname);
            SecretsManagerOptions options = new SecretsManagerOptions(storage, null,
                    allowUnverifiedCertificate);
            KeeperSecrets secrets = SecretsManager.getSecrets(options);
            List<KeeperRecord> records = secrets.getRecords();
            logger.log(Level.FINE, "Found " + records.size() + " records with token redemption.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Redeeming token resulted in error: " + e.getMessage());
            throw new Exception("Cannot redeem token: " + handleException(e));
        }

        logger.log(Level.FINE, "Token redeemed");

        return storage;
    }

    public static SecretsManagerOptions getOptions(String clientId, String privateKey, String appKey, String hostname,
                                                   boolean allowUnverifiedCertificate) {

        LocalConfigStorage storage = new LocalConfigStorage();
        storage.saveString("clientId", clientId.trim());
        storage.saveString("privateKey", privateKey.trim());
        storage.saveString("appKey", appKey.trim());
        storage.saveString("hostname", hostname.trim());

        if (allowUnverifiedCertificate) {
            logger.log(Level.INFO, "Keeper Secrets Manager credential is skipping SSL certification verification. "
                    + "If you want to verify the SSL certification uncheck the skip checkbox in the Jenkins's "
                    + "Credentials manager.");
        }

        return new SecretsManagerOptions(storage, null, allowUnverifiedCertificate);
    }

    public static String testCredentials(String clientId, String privateKey, String appKey, String hostname,
                                         boolean allowUnverifiedCertificate) {

        logger.log(Level.FINE, "Testing credentials; SSL Skip = " + allowUnverifiedCertificate);

        try {
            SecretsManagerOptions options = getOptions(clientId, privateKey, appKey, hostname,
                    allowUnverifiedCertificate);
            KeeperSecrets secrets = SecretsManager.getSecrets(options);
            List<KeeperRecord> records = secrets.getRecords();
            logger.log(Level.FINE, "Found " + records.size() + " records");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Testing credentials resulted in an error: " + e.getMessage());
            return "Validation of the credentials resulted in an error: " + handleException(e);
        }

        return null;
    }
}
