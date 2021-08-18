package io.jenkins.plugins.ksm;

import com.keepersecurity.secretsManager.core.*;
import java.util.*;
import java.lang.reflect.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class KsmRecordField {
    
    public HashMap<String, String> map;

    public KsmRecordField() {
        this.map = getMap();
    }

    public HashMap<String, String> getMap() {

        HashMap<String, String> map = new HashMap<>();
        map.put("password", "Password");
        map.put("login", "Login");
        map.put("url", "Url");
        map.put("fileRef", "FileRef");
        map.put("otp", "OneTimePassword");
        map.put("name", "Name");
        map.put("birthDate", "BirthDate");
        map.put("date", "Date");
        map.put("expirationDate", "ExpirationDate");
        map.put("text", "Text");
        map.put("securityQuestion", "SecurityQuestions");
        map.put("multiline", "Multiline");
        map.put("email", "Email");
        map.put("cardRef", "CardRef");
        map.put("addressRef", "AddressRef");
        map.put("pinCode", "PinCode");
        map.put("phone", "Phones");
        map.put("secret", "HiddenField");
        map.put("note", "SecureNote");
        map.put("accountNumber", "AccountNumber");
        map.put("paymentCard", "PaymentCards");
        map.put("bankAccount", "BankAccounts");
        map.put("keyPair", "KeyPairs");
        map.put("host", "Hosts");
        map.put("address", "Addresses");
        map.put("licenseNumber", "LicenseNumber");

        return map;
    }

    public Class<?> getFieldTypeClass(String fieldType) throws Throwable{
        return Class.forName("com.keepersecurity.secretsManager.core." + map.get(fieldType));
    }

    public KeeperRecordField findField(KeeperRecord record, String fieldKey) {

        KeeperRecordField foundField = findStandardField(record, fieldKey);
        if (foundField == null) {
            foundField = findCustomField(record, fieldKey);
        }
        return foundField;
    }

    public KeeperRecordField findStandardField(KeeperRecord record, String fieldType) {

        KeeperRecordField foundField = null;
        try {
            Class<?> fieldClass = getFieldTypeClass(fieldType);

            KeeperRecordData data = record.getData();
            for (KeeperRecordField item : data.getFields()) {
                if ( item.getClass() == fieldClass ) {
                    foundField = item;
                    break;
                }
            }
        }
        catch(Throwable ignore) {
        }

        return foundField;
    }

    public KeeperRecordField findCustomField(KeeperRecord record, String fieldKey) {

        KeeperRecordField foundField = null;

        KeeperRecordData data = record.getData();
        List<KeeperRecordField> customFields = data.getCustom();

        // Some clients will not include custom fields if there is none. So there will not even be a
        // empty array.
        if (customFields == null) {
            return null;
        }

        // Check to see if the field key matches a label first.
        for (KeeperRecordField field : customFields) {
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

                for (KeeperRecordField field : customFields) {
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

    public String getStandardFieldValue(KeeperRecord record, String fieldType, int arrayIndex, String dictKey) throws Exception {

        // Find the standard field based on the field type.
        KeeperRecordField recordField = findStandardField(record, fieldType);
        if ( recordField == null ) {
            throw new Exception("Cannot find the standard field " + fieldType + " in the record.");
        }
        return getFieldValue(recordField, arrayIndex, dictKey);
    }

    public String getCustomFieldValue(KeeperRecord record, String fieldType, int arrayIndex, String dictKey) throws Exception {

        // Find the standard field based on the field type.
        KeeperRecordField recordField = findCustomField(record, fieldType);
        if (recordField == null) {
            throw new Exception("Cannot find the standard field " + fieldType + " in the record.");
        }
        return getFieldValue(recordField, arrayIndex, dictKey);
    }

    @SuppressWarnings("unchecked")
    private JSONObject makeJSONObject(Object data) {
        JSONObject jsonObject = new JSONObject();
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
        return jsonObject;
    }

    private String makeJSON(Object data)  {
        return makeJSONObject(data).toJSONString();
    }

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


    public String getFieldValue(KeeperRecordField recordField, int arrayIndex, String dictKey) throws Exception {

        String returnValue;

        // Fields values are always arrays. If the server gave us something different, the Exception is the least
        // of our problems.
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) recordField.getClass().getMethod("getValue").invoke(recordField);

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
                char first = Character.toUpperCase(dictKey.charAt(0));
                String getter = "get" + first + dictKey.substring(1);
                returnValue = makeValueString(value.getClass().getMethod(getter).invoke(value));
            }
        }

        return returnValue;
    }
}
