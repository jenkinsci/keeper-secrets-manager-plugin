package io.jenkins.plugins.ksm;

import com.keepersecurity.secretsManager.core.*;
import static com.keepersecurity.secretsManager.core.SecretsManager.*;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.notation.KsmNotationItem;

import java.util.*;

import hudson.util.Secret;


public class KsmQuery {

    KsmCredential credential;

    public KsmQuery(KsmCredential credential) {
        this.credential = credential;
    }

    public void run(Map<String, KsmNotationItem> requests) throws Exception {

        // Find all the unique record UIDs in the requests.
        Set<String> uniqueUids = new HashSet<>();
        for(Map.Entry<String, KsmNotationItem> entry: requests.entrySet()) {
            uniqueUids.add(entry.getValue().getUid());
        }

        // Set up our creds to the secrets manager.
        LocalConfigStorage storage = new LocalConfigStorage();
        storage.saveString("clientKey", Secret.toString(credential.getClientKey()));
        storage.saveString("clientId", Secret.toString(credential.getClientId()));
        storage.saveString("privateKey", Secret.toString(credential.getPrivateKey()));
        storage.saveString("publicKey", Secret.toString(credential.getPublicKey()));
        storage.saveString("appKey", Secret.toString(credential.getAppKey()));
        initializeStorage(storage, Secret.toString(credential.getClientKey()), credential.getHostname());

        // Query the unique record ids.
        SecretsManagerOptions options = new SecretsManagerOptions(storage);
        KeeperSecrets secrets = SecretsManager.getSecrets(options, new ArrayList<>(uniqueUids));

        // Place the secret into a lookup map using their Uid as the key.
        Map<String, KeeperRecord> secretMap = new HashMap<>();
        for(KeeperRecord secret: secrets.getRecords()) {
            secretMap.put(secret.getRecordUid(), secret);
        }

        KsmRecordField recordField = new KsmRecordField();

        for(Map.Entry<String, KsmNotationItem> entry: requests.entrySet()) {
            KsmNotationItem request = entry.getValue();
            KeeperRecord record = secretMap.get(request.getUid());

            String fieldKey = request.getFieldKey();
            String dictKey = request.getDictKey();

            // If we want to entire value, set the index to -1
            Integer arrayIndex = request.getArrayIndex();
            if ( !request.getReturnSingle()) {
                arrayIndex = -1;
            }

            switch (request.getFieldDataType()) {
                case STANDARD:
                    request.setValue(recordField.getStandardFieldValue(record, fieldKey, arrayIndex, dictKey));
                    break;
                case CUSTOM:
                    request.setValue(recordField.getCustomFieldValue(record, fieldKey, arrayIndex, dictKey));
                    break;
                case FILE:
                    KeeperFile file = record.getFileByName(fieldKey);
                    if (file != null) {
                        byte[] fileBytes = downloadFile(file);
                        request.setValue(Arrays.toString(fileBytes));
                    }
                    break;
            }
        }
    }
}
