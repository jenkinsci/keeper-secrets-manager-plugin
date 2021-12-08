package io.jenkins.plugins.ksm.notation;

import com.keepersecurity.secretsManager.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import java.util.Base64;

public class KsmTestNotation extends KsmNotation {

    private KeeperSecrets secrets;
    private final static String ksmDataFile = "ksmTestData.json";
    private final Map<String, byte[]> fileCache = new HashMap<>();

    /**
     * This method will add test data. It takes a JSON structure which it will turn
     * into KeeperSecrets. To be perfectly honest, this is just overkill for one project.
     *
     * Right now it's limited to the Login and Password fields, but can be expanded.
     *
     * {
     *      "secrets": [
     *          {
     *              "uid": "XXXXX",
     *              "title": "My Title",
     *              "type": "login",
     *              "note": "Some note",
     *              "fields": [
     *                  {
     *                      "label": "Field Label",
     *                      "fieldType": "Login",
     *                      "values": ["MY_LOGIN"]
     *                  }
     *              ],
     *              "files": [
     *                  {
     *                      "name": "File name and title",
     *                      "data": "BASE64"
     *                  }
     *              ]
     *          }
     *      ]
     * }
     *
     * @param jsonString The JSON string
     */

    public void addTestData(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        JSONArray secretArr = obj.getJSONArray("secrets");
        List<KeeperRecord> records = new ArrayList<>();
        for (int index = 0; index < secretArr.length(); index++)
        {
            String uid = secretArr.getJSONObject(index).getString("uid");
            String title = secretArr.getJSONObject(index).getString("title");
            String recordType = secretArr.getJSONObject(index).getString("type");
            String note = secretArr.getJSONObject(index).getString("note");

            List<KeeperRecordField> standardFields = new ArrayList<>();
            JSONArray fieldArr = secretArr.getJSONObject(index).getJSONArray("fields");
            for (int fieldIndex = 0; fieldIndex < fieldArr.length(); fieldIndex++) {

                List<String> mockValue = new ArrayList<>();
                for (Object value : fieldArr.getJSONObject(fieldIndex).getJSONArray("values")) {
                    mockValue.add((String) value);
                }

                String label = fieldArr.getJSONObject(fieldIndex).getString("label");
                String fieldType = fieldArr.getJSONObject(fieldIndex).getString("fieldType");

                // This is really limited right now. Only does Login field, but can be expanded.
                // If you add stuff, remember to add an entry in the resources/META-INF/hudson.remoting.ClassFilter
                KeeperRecordField field = null;
                switch (fieldType) {
                    case("Login"):
                        field = new Login(label, true, false, mockValue);
                        break;
                    case("Password"):
                        field = new Password(label, true, false, false, null, mockValue);
                        break;
                    case("Url"):
                        field = new Url(label, true, false, mockValue);
                        break;
                    default:
                        //
                }
                standardFields.add(field);
            }

            KeeperRecordData data = new KeeperRecordData(
                    title,
                    recordType,
                    standardFields,
                    null,
                    note
            );

            List<KeeperFile> files = new ArrayList<>();
            JSONArray fileArr = secretArr.getJSONObject(index).getJSONArray("files");
            for (int fileIndex = 0; fileIndex < fileArr.length(); fileIndex++) {
                byte[] content = fileArr.getJSONObject(fileIndex).getString("data").getBytes(StandardCharsets.UTF_8);

                fileCache.put(
                        fileArr.getJSONObject(fileIndex).getString("name"),
                        Base64.getDecoder().decode(content)
                );

                KeeperFileData fileData = new KeeperFileData(
                        fileArr.getJSONObject(fileIndex).getString("name"),
                        fileArr.getJSONObject(fileIndex).getString("name"),
                        "",
                        content.length,
                        1L
                );
                KeeperFile keeperFile = new KeeperFile(
                        "HI".getBytes(StandardCharsets.UTF_8),
                        "fileUId",
                        fileData,
                        "http://localhost",
                        null
                );
                files.add(keeperFile);
            }

            KeeperRecord record = new KeeperRecord(
                    "HI".getBytes(StandardCharsets.UTF_8),
                    uid,
                    null,
                    null,
                    data,
                    0L,
                    files
            );
            records.add(record);
        }

        this.secrets = new KeeperSecrets(records);
    }

    public KeeperSecrets getSecrets(SecretsManagerOptions options, List<String> uids) {
        return this.secrets;
    }

    public byte[] downloadDataFile(KeeperFile file) {
        String name = file.getData().getName();
        return fileCache.get(name);
    }

    private static String getDataFilePath() {

        String tmpdir = System.getProperty("java.io.tmpdir");
        return new File(tmpdir, KsmTestNotation.ksmDataFile).getPath();
    }

    public void writeJsonData(String jsonString) {
        System.out.println("KSM: Saving JSON test data to " + KsmTestNotation.getDataFilePath());
        try {
            FileOutputStream fileOut =
                    new FileOutputStream(KsmTestNotation.getDataFilePath());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeUTF(jsonString);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public static boolean hasDataFile() {
        File f = new File(KsmTestNotation.getDataFilePath());
        return f.exists();
    }

    public void loadJsonData() throws IOException {
        System.out.println("KSM: Loading JSON test data from " + KsmTestNotation.getDataFilePath());
        FileInputStream fileIn = new FileInputStream(KsmTestNotation.getDataFilePath());
        ObjectInputStream in = new ObjectInputStream(fileIn);
        String jsonString = in.readUTF();
        in.close();
        fileIn.close();
        this.addTestData(jsonString);
    }

    @SuppressWarnings("unused")
    public static void removeJsonData() {
        try {
            File f = new File(KsmTestNotation.getDataFilePath());
            boolean wasDeleted = f.delete();
            if (wasDeleted) {
                System.out.println("KSM: Removed JSON test data from " + KsmTestNotation.getDataFilePath());
            }
        }
        catch(Exception ignore ) {
            //
        }
    }
}
