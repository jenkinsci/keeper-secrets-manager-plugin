package io.jenkins.plugins.ksm;

import java.util.List;
import java.util.HashMap;
import java.util.Arrays;

//import com.keepersecurity.secretsManager.core.LocalConfigStorage;
//import com.keepersecurity.secretsManager.core.SecretsManagerOptions;
//import com.keepersecurity.secretsManager.core.SecretsManager;
//import com.keepersecurity.secretsManager.core.KeeperRecord;
//import com.keepersecurity.secretsManager.core.KeeperSecrets;
import com.keepersecurity.secretsManager.core.*;

import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.notation.KsmNotationRequest;
import io.jenkins.plugins.ksm.notation.KsmFieldDataEnumType;

import java.lang.reflect.*;

public class KsmQuery {

    private final SecretsManagerOptions options;

    // A simple cache by UID
    private HashMap<String, KeeperRecord> lookup;

    public KsmQuery(String clientKey, String hostName, String clientId, String privateKey, String appKey ) {

        LocalConfigStorage storage = new LocalConfigStorage();
        storage.saveString("clientKey", clientKey);
        storage.saveString("hostname", hostName);
        storage.saveString("clientId", clientId);
        storage.saveString("privateKey", privateKey);
        storage.saveString("appKey", appKey);

        options = new SecretsManagerOptions(storage);
    }

    public SecretsManagerOptions getOptions() {
        return options;
    }

    public void query(String[] uids) {

        // Make sure we have a unique list of UIDs
        HashMap<String, String> tempHm = new HashMap<String, String>();
        for(String uid: uids) {
            tempHm.put(uid, uid);
        }
        String[] uniqueIds = tempHm.keySet().toArray(new String[0]);

        KeeperSecrets secrets = SecretsManager.getSecrets(options, Arrays.asList(uniqueIds));

        lookup.clear();
        for (KeeperRecord record: secrets.getRecords()) {
            lookup.put(record.getRecordUid(), record);
        }
    }

    public int count() {
        return lookup.size();
    }

    public KeeperRecord getRecord(String uid) {
        return lookup.get(uid);
    }

    private void getField(KeeperRecord record, String fieldTypeName) {

        String className = "";
        String getter = "";
        switch (fieldTypeName) {
            case "password":
                className = "Password";
                getter = "getPassword";
                break;
            case "login":
                className = "Login";
                getter = "getLogin";
                break;
        }
        try {
            Class c = Class.forName(className);
            Object value = c.getMethod(getter).invoke(record);
        } catch (Throwable e) {
            // Nothing
        }
    }

    public void getValueWithNotation(KsmNotationRequest request) throws Exception {

        KeeperRecord record = getRecord(request.getUid());
        if ( record == null ) {
            throw new Exception("Cannot find record for Uid " + request.getUid());
        }

        Object[] value = {};
        switch( request.getFieldDataType() ) {
            case STANDARD:
                break;
            case CUSTOM:
                break;
            case FILE:
                break;
        }
    }
}
