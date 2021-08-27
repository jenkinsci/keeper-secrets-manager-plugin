package io.jenkins.plugins.ksm.notation;

import com.keepersecurity.secretsManager.core.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.lang.reflect.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class KsmFieldParse {
    
    private HashMap<String, String> mapFieldStrToClass;
    private HashMap<Class<?>,List<String>> inflateRefTypes;
    private SecretsManagerOptions options;

    public KsmFieldParse(SecretsManagerOptions options) throws Throwable {
        this.options = options;
        init();
    }

    private void init() throws Throwable {

        // The field type matching is case-insensitive.
        mapFieldStrToClass = new HashMap<>();
        mapFieldStrToClass.put("password", "Password");
        mapFieldStrToClass.put("login", "Login");
        mapFieldStrToClass.put("url", "Url");
        mapFieldStrToClass.put("fileref", "FileRef");
        mapFieldStrToClass.put("otp", "OneTimePassword");
        mapFieldStrToClass.put("name", "Name");
        mapFieldStrToClass.put("birthdate", "BirthDate");
        mapFieldStrToClass.put("date", "Date");
        mapFieldStrToClass.put("expirationdate", "ExpirationDate");
        mapFieldStrToClass.put("text", "Text");
        mapFieldStrToClass.put("securityquestion", "SecurityQuestions");
        mapFieldStrToClass.put("multiline", "Multiline");
        mapFieldStrToClass.put("email", "Email");
        mapFieldStrToClass.put("cardref", "CardRef");
        mapFieldStrToClass.put("addressref", "AddressRef");
        mapFieldStrToClass.put("pincode", "PinCode");
        mapFieldStrToClass.put("phone", "Phones");
        mapFieldStrToClass.put("secret", "HiddenField");
        mapFieldStrToClass.put("note", "SecureNote");
        mapFieldStrToClass.put("accountnumber", "AccountNumber");
        mapFieldStrToClass.put("paymentcard", "PaymentCards");
        mapFieldStrToClass.put("bankaccount", "BankAccounts");
        mapFieldStrToClass.put("keypair", "KeyPairs");
        mapFieldStrToClass.put("host", "Hosts");
        mapFieldStrToClass.put("address", "Addresses");
        mapFieldStrToClass.put("licensenumber", "LicenseNumber");

        // field types that will be inflated. This take a class and give you the field type string.
        inflateRefTypes = new HashMap<Class<?>,List<String>>();
        inflateRefTypes.put(this.getFieldTypeClass("addressRef"), Arrays.asList("address"));
        inflateRefTypes.put(this.getFieldTypeClass("cardRef"), Arrays.asList("paymentCard", "text", "pinCode", "addressRef"));
    }

    /**
     * Convert a field type string to a field class
     *
     * Perform a case-insensitive match on the field type to get the Class name. This
     * is done since the Web UI displays mix-case for the field type.
     *
     * @param fieldType the field type string
     * @return Class<?>
     */

    public Class<?> getFieldTypeClass(String fieldType) throws Throwable{
        return Class.forName("com.keepersecurity.secretsManager.core." + mapFieldStrToClass.get(fieldType.toLowerCase(Locale.ROOT)));
    }

    /**
     *
     * @param record   an instance of the KeeperRecord
     * @param fieldKey the label or type of the field to find
     * @return KeeperRecordField
     */

    public KeeperRecordField findAnyField(KeeperRecord record, String fieldKey) {

        KeeperRecordField foundField = findStandardField(record, fieldKey);
        if (foundField == null) {
            foundField = findCustomField(record, fieldKey);
        }
        return foundField;
    }

    /**
     * A field search that will check the label and type of the field
     * @param fields   a list of KeeperRecordField, either standard or custon fields.
     * @param fieldKey the label or type of the field to find
     * @return KeeperRecordField
     */

    private KeeperRecordField findfield(List<KeeperRecordField> fields, String fieldKey) {

        KeeperRecordField foundField = null;

        // Some clients will not include custom fields if there is none. So there will not even be a
        // empty array.
        if ((fields == null) || (fields.size() == 0)) {
            return null;
        }

        // Check to see if the field key matches a label first.
        for (KeeperRecordField field : fields) {
            try {
                Object label = field.getClass().getMethod("getLabel").invoke(field);
                if (fieldKey.equals(label)) {
                    foundField = field;
                    break;
                }
            }
            catch(Throwable ignore) {
            }
        }
        if (foundField == null ) {

            // Else check if the field key is a field type of the class.
            try {
                Class<?> fieldClass = getFieldTypeClass(fieldKey);

                for (KeeperRecordField field : fields) {
                    if (field.getClass() == fieldClass) {
                        foundField = field;
                        break;
                    }
                }
            } catch (Throwable ignore) {
            }
        }

        return foundField;
    }

    /**
     * Find the field key in the standard fields.
     * @param record   the record that contains the standard fields
     * @param fieldKey the label or type of the field to find
     * @return KeeperRecordField
     */

    public KeeperRecordField findStandardField(KeeperRecord record, String fieldKey) {
        KeeperRecordData data = record.getData();
        return findfield(data.getFields(), fieldKey);
    }

    /**
     * Find the field key in the custom fields.
     * @param record   the record that contains the standard fields
     * @param fieldKey the label or type of the field to find
     * @return KeeperRecordField
     */

    public KeeperRecordField findCustomField(KeeperRecord record, String fieldKey) {
        KeeperRecordData data = record.getData();
        return findfield(data.getCustom(), fieldKey);
    }

    /**
     * Get a value from the standard fields
     * @param record      the record that contains the standard fields
     * @param fieldKey    the label or type of the field to find
     * @param arrayIndex  the index of item in an array, if applicable
     * @param dictKey     the dictionary key of the value object, if applicable
     * @return String
     * @throws Exception thrown when no value is found
     */

    public String getStandardFieldValue(KeeperRecord record, String fieldKey, int arrayIndex, String dictKey) throws Exception {

        // Find the standard field based on the field type.
        KeeperRecordField recordField = findStandardField(record, fieldKey);
        if ( recordField == null ) {
            throw new Exception("Cannot find the standard field " + fieldKey + " in the record.");
        }
        return getFieldValue(recordField, arrayIndex, dictKey);
    }

    /**
     * Get a value from the custom fields
     * @param record      the record that contains the standard fields
     * @param fieldKey    the label or type of the field to find
     * @param arrayIndex  the index of item in an array, if applicable
     * @param dictKey     the dictionary key of the value object, if applicable
     * @return String
     * @throws Exception thrown when no value is found
     */

    public String getCustomFieldValue(KeeperRecord record, String fieldKey, int arrayIndex, String dictKey) throws Exception {

        // Find the custom field based on the field type.
        KeeperRecordField recordField = findCustomField(record, fieldKey);
        if (recordField == null) {
            throw new Exception("Cannot find the standard field " + fieldKey + " in the record.");
        }
        return getFieldValue(recordField, arrayIndex, dictKey);
    }

    /**
     * Convert an field instance to a map of attributes values
     */

    private Map<String, Object> instanceToValueMap(Object field) {

        Map<String, Object> valueMap = new HashMap<String, Object>();

        Class<?> c = field.getClass();
        Field[] fields = c.getDeclaredFields();
        for (Field f : fields) {
            try {
                f.setAccessible(true);
                Object value = f.get(field);
                if (value instanceof String) {
                    valueMap.put(f.getName(), value);
                }
            } catch (IllegalAccessException e) {
                // Not going to happen
            }
        }
        return valueMap;
    }

    /**
     * Convert an object into a JSON onbject.
     * @param data an object with attribute fields
     * @return
     */

    @SuppressWarnings("unchecked")
    private JSONObject makeJSONObject(Object data) {
        JSONObject jsonObject = new JSONObject();

        // Easy, if a Map, just copy values into JSON object
        if (data instanceof Map ) {
            for(Map.Entry<String, Object> entry: ((Map<String, Object>) data).entrySet()) {
                jsonObject.put(entry.getKey(), (String) entry.getValue());
            }
        }
        // Else it's an object with fields. Find the fields, get the name and value, add to JSON object.
        else {
            System.out.println("IM IN HERE");
            Class<?> c = data.getClass();
            Field[] fields = c.getDeclaredFields();
            for (Field f : fields) {
                try {
                    f.setAccessible(true);
                    Object value = f.get(data);
                    if (value instanceof String) {
                        jsonObject.put(f.getName(), value);
                    }
                } catch (IllegalAccessException e) {
                    // Not going to happen
                }
            }
        }
        return jsonObject;
    }

    /**
     * Convert an object into JSON
     * @param data
     * @return
     */

    private String makeJSON(Object data)  {
        return makeJSONObject(data).toJSONString();
    }

    /**
     * Take the full value of field and make it JSON. This will be an array.
     * @param data an array of objects
     * @return
     */

    // The full, raw, value of a field is always an array.
    @SuppressWarnings("unchecked")
    private String makeJSONFullValue(Object data)  {
        JSONArray arr = new JSONArray();
        for(Object item: (List<Object>) data ) {
            if (item instanceof String) {
                arr.add(item);
            }
            else {
                arr.add(makeJSONObject(item));
            }
        }
        return arr.toJSONString();
    }

    /**
     * Format the return string
     *
     * If the value is not a string, make it JSON and return that as the string. Else just return the string value.
     * @param value a string or dictionary value
     * @return
     */

    private String makeValueString(Object value) {

        String returnValue;

        // The value is either
        if ( value instanceof String ) {
            returnValue = (String) value;
        }
        else {
            returnValue = makeJSON(value);
        }

        return returnValue;
    }

    /**
     * Common method to get a value for a standard and custom field
     * @param recordField the field that contains the value
     * @param arrayIndex  the index into an array for the value, if applicable
     * @param dictKey     the dictionary key in an object to for the value, if applicable
     * @return the string value
     * @throws Exception
     */

    @SuppressWarnings("unchecked")
    public String getFieldValue(KeeperRecordField recordField, int arrayIndex, String dictKey) throws Exception {

        String returnValue;

        // Fields values are always arrays. If the server gave us something different, the Exception is the least
        // of our problems.
        List<Object> values = (List<Object>) recordField.getClass().getMethod("getValue").invoke(recordField);

        List<String> refTypes = inflateRefTypes.get(recordField.getClass());
        if (refTypes != null ) {
            values = inflateFieldValue(values, refTypes);
        }

        // If the array index is < 0; we want the entire array.
        if (arrayIndex <0) {
            // If we want the full value, it's going to be an array.
            returnValue = makeJSONFullValue(values);
        }
        else {
            // Get the item from the array
            Object value = values.get(arrayIndex);

            // If we are not trying to access a dictionary return value.
            if (dictKey == null) {
                returnValue = makeValueString(value);
            }
            // Else we are trying to access a value in a dictionary.
            else {
                if (value instanceof Map) {
                    returnValue = (String) ((Map<String, Object>) value).get(dictKey);
                }
                else {
                    char first = Character.toUpperCase(dictKey.charAt(0));
                    String getter = "get" + first + dictKey.substring(1);
                    returnValue = makeValueString(value.getClass().getMethod(getter).invoke(value));
                }
            }
        }

        return returnValue;
    }

    @SuppressWarnings("unchecked")
    public List<Object> inflateFieldValue(List<Object> uids, List<String> replaceFields) throws Exception {

        List<Object> values = new ArrayList<>();
        List<String> stringUids = new ArrayList<>();
        for(Object uid: uids) {
            stringUids.add((String) uid);
        }

        Map<String, KeeperRecord> lookup = new HashMap<String, KeeperRecord>();
        KeeperSecrets secrets = SecretsManager.getSecrets(options, stringUids);
        List<KeeperRecord> records= secrets.getRecords();
        for(KeeperRecord record: records) {
            lookup.put(record.getRecordUid(), record);
        }

        for(String uid: stringUids) {
            KeeperRecord record = lookup.get(uid);
            Object newValue = null;
            String lastLabel = null;
            if (record != null) {
                for(String replacementKey: replaceFields) {
                    KeeperRecordField realField = this.findStandardField(record, replacementKey);
                    // If the field in missing from the record, just skip it.
                    if (realField == null) {
                        continue;
                    }
                    List<Object> realValues = (List<Object>) realField.getClass().getMethod("getValue").invoke(realField);
                    Object realValue = realValues.get(0);

                    String currentLabel = (String) realField.getClass().getMethod("getLabel").invoke(realField);
                    if (currentLabel == null) {
                        currentLabel = replacementKey;
                    }

                    // If the real value is a KeeperRecordField, check if it also needs to be inflated.
                    if (realValue instanceof KeeperRecordField) {
                        List<String> refTypes = inflateRefTypes.get(realValue.getClass());
                        if (refTypes != null) {
                            realValue = inflateFieldValue(values, refTypes);
                        }
                    }
                    // If the real value is not a string, it's an object. We need to get the fields in it.
                    if (!(realValue instanceof String)) {
                        Map<String, Object> valueMap = instanceToValueMap(realValue);
                        // If the new value has not been set, set to a Map
                        if (newValue == null) {
                            newValue = valueMap;
                        }
                        // Else if the new value is a ready a map, copy the value map into new value
                        else if (newValue instanceof Map) {
                            for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                                ((Map<String, Object>) newValue).put(entry.getKey(), entry.getValue());
                            }
                        }
                        // Else the new value is a string, covert to map
                        else {
                            valueMap.put(lastLabel, newValue);
                            newValue = valueMap;
                        }
                    } else {
                        // If the enw value is null, set the new value as a String
                        if (newValue == null) {
                            newValue = realValue;
                        }
                        // If the new value is a Map, then just add the real value using the label/type as the key.
                        else if (newValue instanceof Map) {
                            ((Map<String, Object>) newValue).put(currentLabel, realValue);
                        } else {
                            Map<String, Object> valueMap = new HashMap<String, Object>();
                            valueMap.put(lastLabel, newValue);
                            valueMap.put(currentLabel, realValue);
                            newValue = valueMap;
                        }
                    }
                    lastLabel = currentLabel;
                }
            }

            if (newValue != null) {
                values.add(newValue);
            }
        }

        return values;
    }

    public void run(KsmNotationItem item, KeeperRecord record) throws Exception {

        String fieldKey = item.getFieldKey();
        String dictKey = item.getDictKey();

        // If we want to entire value, set the index to -1
        Integer arrayIndex = item.getArrayIndex();
        if (!item.getReturnSingle()) {
            arrayIndex = -1;
        }

        try {
            item.clearError();
            switch (item.getFieldDataType()) {
                case STANDARD:
                    item.setValue(this.getStandardFieldValue(record, fieldKey, arrayIndex, dictKey));
                    break;
                case CUSTOM:
                    item.setValue(this.getCustomFieldValue(record, fieldKey, arrayIndex, dictKey));
                    break;
                case FILE:
                    KeeperFile file = record.getFileByName(fieldKey);
                    if (file != null) {
                        byte[] fileBytes = SecretsManager.downloadFile(file);
                        item.setValue(new String(fileBytes, StandardCharsets.UTF_8));
                    }
                    break;
            }
        } catch (Exception e) {
            String msg = e.toString();
            if(e instanceof NullPointerException) {
               msg = "Cannot find field in record.";
            }
            item.setError(msg);
            if (!item.getAllowFailure()) {
                throw new Exception("For environment variable " + item.getName() + ", " + item.getNotation() + ": " +
                        msg
                );
            }
        }
    }
}
