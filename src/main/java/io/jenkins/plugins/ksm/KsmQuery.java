package io.jenkins.plugins.ksm;

import com.keepersecurity.secretsManager.core.*;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.notation.KsmNotationItem;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import hudson.util.Secret;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class KsmQuery {

    KsmCredential credential;

    private static final Logger logger = Logger.getLogger(KsmQuery.class.getName());

    public KsmQuery(KsmCredential credential) {
        this.credential = credential;
    }

    private static String handleException(Exception  e) {

        String msg = e.getMessage();

        // See if this is one of our error message
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject obj = (JSONObject) jsonParser.parse(msg);
            msg = (String) obj.get("message");
        }
        catch(ParseException ignore ){
            // Don't do anything. Keep msg the same.
        }

        return msg;
    }

    public static LocalConfigStorage redeemToken(String token, String hostname) throws Exception {

        logger.log(Level.FINE, "Setting up the secrets manager options");

        LocalConfigStorage storage = new LocalConfigStorage();
        try {
            SecretsManager.initializeStorage(storage, token, getHostname(hostname.trim()));
            SecretsManagerOptions options = new SecretsManagerOptions(storage);
            KeeperSecrets secrets = SecretsManager.getSecrets(options);
            List<KeeperRecord> records= secrets.getRecords();
            logger.log(Level.FINE, "Found " + records.size() + " records with token redemption.");
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Redeeming token resulted in error: " + e.getMessage());
            throw new Exception("Cannot initialize token: " + handleException(e));
        }
        return storage;
    }

    private static String getHostname(String hostname) {
        // Allow for aliases.
        switch(hostname) {
            case "US": hostname="keepersecurity.com"; break;
            case "EU": hostname="keepersecurity.eu"; break;
            case "AU": hostname="keepersecurity.com.au"; break;
            case "US_GOV": hostname="govcloud.keepersecurity.us"; break;
            default:
                // nothing
        }

        return hostname;
    }

    public static SecretsManagerOptions getOptions(String clientId, String privateKey, String appKey, String hostname) {

        LocalConfigStorage storage = new LocalConfigStorage();
        storage.saveString("clientId", clientId.trim());
        storage.saveString("privateKey", privateKey.trim());
        storage.saveString("appKey", appKey.trim());
        storage.saveString("hostname", getHostname(hostname.trim()));

        logger.log(Level.FINE, "Setting up the secrets manager options");

        return new SecretsManagerOptions(storage);
    }

    public static String testCredentials(String clientId, String privateKey, String appKey, String hostname) {

        logger.log(Level.FINE, "Testing credentials");

        try {
            SecretsManagerOptions options = getOptions(clientId, privateKey, appKey, hostname);

            logger.log(Level.FINE, options.toString());
            KeeperSecrets secrets = SecretsManager.getSecrets(options);
            List<KeeperRecord> records= secrets.getRecords();
            logger.log(Level.FINE, "Found " + records.size() + " records");
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Testing credentials resulted in an error: " + e.getMessage());
            return "Validation of the credentials resulted in an error: " + handleException(e);
        }

        return null;
    }

    public void run(Map<String, KsmNotationItem> items) throws Throwable {

        SecretsManagerOptions options = getOptions(
                Secret.toString(credential.getClientId()),
                Secret.toString(credential.getPrivateKey()),
                Secret.toString(credential.getAppKey()),
                credential.getHostname());

        KsmNotation.run(items, options);
    }
}
