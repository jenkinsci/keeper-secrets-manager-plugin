package io.jenkins.plugins.ksm;

import com.keepersecurity.secretsManager.core.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class KsmQuery {

    private static final Logger logger = Logger.getLogger(KsmQuery.class.getName());

    private static String handleException(Exception e) {

        String msg = e.getMessage();

        // See if this is one of our error message
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject obj = (JSONObject) jsonParser.parse(msg);
            msg = (String) obj.get("message");
        } catch (ParseException ignore) {
            // Don't do anything. Keep msg the same.
        }

        return msg;
    }

    public static LocalConfigStorage redeemToken(String token, String hostname) throws Exception {

        logger.log(Level.FINE, "Setting up the secrets manager options");

        // New style has the hostname prepended to the token, joined with a ":"
        // TODO: Let the SDK handle this, however it's not working now.
        if ( token.contains(":")) {
            String[] parts = token.split(":");
            if (parts.length != 2) {
                throw new Exception("The region code/token appears to be formatted incorrectly.");
            }

            hostname = parts[0];
            if((hostname == null) || (hostname.equals(""))) {
                throw new Exception("The region code/hostname of the token is blank.");
            }
            token = parts[1];
            if((token == null) || (token.equals(""))) {
                throw new Exception("The hash code of the token is blank.");
            }
        }

        logger.log(Level.FINE, "Redeem token " + token +" from host " + hostname);

        LocalConfigStorage storage = new LocalConfigStorage();
        try {
            SecretsManager.initializeStorage(storage, token, getHostname(hostname));
            SecretsManagerOptions options = new SecretsManagerOptions(storage);
            KeeperSecrets secrets = SecretsManager.getSecrets(options);
            List<KeeperRecord> records = secrets.getRecords();
            logger.log(Level.FINE, "Found " + records.size() + " records with token redemption.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Redeeming token resulted in error: " + e.getMessage());
            throw new Exception("Cannot redeem token: " + handleException(e));
        }
        return storage;
    }

    public static SecretsManagerOptions getOptions(String clientId, String privateKey, String appKey, String hostname,
                                                   boolean allowUnverifiedCertificate) {

        LocalConfigStorage storage = new LocalConfigStorage();
        storage.saveString("clientId", clientId.trim());
        storage.saveString("privateKey", privateKey.trim());
        storage.saveString("appKey", appKey.trim());
        storage.saveString("hostname", hostname.trim());

        logger.log(Level.FINE, "Setting up the secrets manager options");
        if (allowUnverifiedCertificate) {
            logger.log(Level.INFO, "Keeper Secrets Manager credential is skipping SSL certification verification. "
                    + "If you want to verify the SSL certification uncheck the skip checkbox in the Jenkins's "
                    + "Credentials manager.");
        }

        return new SecretsManagerOptions(storage, null, allowUnverifiedCertificate);
    }

    public static String testCredentials(String clientId, String privateKey, String appKey, String hostname,
                                         boolean allowUnverifiedCertificate) {

        logger.log(Level.FINE, "Testing credentials");

        try {
            SecretsManagerOptions options = getOptions(clientId, privateKey, appKey, getHostname(hostname),
                    allowUnverifiedCertificate);

            logger.log(Level.FINE, options.toString());
            KeeperSecrets secrets = SecretsManager.getSecrets(options);
            List<KeeperRecord> records = secrets.getRecords();
            logger.log(Level.FINE, "Found " + records.size() + " records");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Testing credentials resulted in an error: " + e.getMessage());
            return "Validation of the credentials resulted in an error: " + handleException(e);
        }

        return null;
    }

    private static String getHostname(String hostname) {
        // Allow for aliases.
        switch (hostname) {
            case "US":
                hostname = "keepersecurity.com";
                break;
            case "EU":
                hostname = "keepersecurity.eu";
                break;
            case "AU":
                hostname = "keepersecurity.com.au";
                break;
            case "US_GOV":
                hostname = "govcloud.keepersecurity.us";
                break;
            default:
                // nothing
        }

        return hostname;
    }
}
