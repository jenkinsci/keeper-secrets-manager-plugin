package io.jenkins.plugins.ksm;

import com.keepersecurity.secretsManager.core.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MockConfig {

    private String makeRandomString(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                          "abcdefghijklmnopqrstuvwzyz" +
                          "0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(alphabet.length());
            char randomChar = alphabet.charAt(index);
            sb.append(randomChar);
        }
        return sb.toString();
    }

    public HashMap<String, String> makeConfig() throws Exception {

        HashMap<String, String> config = new HashMap<>();

        String hostname = makeRandomString(10) + ".com";
        String tokenRndStr = makeRandomString(32) + ".com";
        String token = Base64.getUrlEncoder()
                .encodeToString(tokenRndStr.getBytes(StandardCharsets.UTF_8));

        LocalConfigStorage storage = new LocalConfigStorage();
        try {
            SecretsManager.initializeStorage(storage, token, hostname);
        } catch (Exception e) {
            throw new Exception("Cannot mock config: " + e.getMessage());
        }

        String appKeyRandomString = makeRandomString(32);
        String appKey = Base64.getEncoder()
                .encodeToString(appKeyRandomString.getBytes(StandardCharsets.UTF_8));

        config.put("hostname", hostname);
        config.put("appKey", appKey);
        config.put("clientId", storage.getString(SecretsManager.KEY_CLIENT_ID));
        config.put("privateKey", storage.getString(SecretsManager.KEY_PRIVATE_KEY));
        config.put("serverPublicKeyId", storage.getString(SecretsManager.KEY_SERVER_PUBIC_KEY_ID));

        return config;
    }

}
