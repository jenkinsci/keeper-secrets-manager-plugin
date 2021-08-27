package io.jenkins.plugins.ksm.notation;

import com.keepersecurity.secretsManager.core.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.SuppressWarnings;


public class KsmNotation {

    // A notation might start with a prefix, that will need to be removed. This is the that String prefix.
    public static final String notationPrefix = "keeper";

    public static KsmNotationItem find(String name, String notation, boolean allowFailure) throws Exception {

        KsmNotationItem request = null;
        if (notation.startsWith(notationPrefix)) {
            request = parse(name, notation, allowFailure);
        }
        return request;
    }

    @SuppressWarnings("unused")
    public static KsmNotationItem parse(String name, String notation, boolean allowFailure) throws Exception {

        // If the notation starts with the notation prefix, normally used with env vars, remove it.
        if (notation.startsWith(notationPrefix)) {
            String[] notationParts = notation.split("//");
            if ( notationParts.length < 2 ) {
                throw new Exception("Notation is missing information about the uid, field data type, and field key.");
            }
            notation = notationParts[1];
        }

        String[] notationParts = notation.split("/");
        if ( notationParts.length < 3 ) {
            throw new Exception("Notation format appears to be missing values. There should be 3 values separated by a '/' character.");
        }
        if ( notationParts.length > 3 ) {
            String msg = "Notation format appears to contain too many values. There should be 3 values separated by a '/' character.";

            // There could be a change the keeper:// is misspelled.
            if (notation.startsWith(notationPrefix.substring(0,1))) {
                msg += "  The keeper:// prefix might be misspelled.";
            }
            throw new Exception(msg);
        }
        String uid = notationParts[0];
        if (uid.length() != 22) {
            throw new Exception("The record uid is not the correct length.");
        }

        KsmFieldDataEnumType fieldDataType = KsmFieldDataEnumType.getEnumByString(notationParts[1]);
        if (fieldDataType == null) {
            throw new Exception("The field type can only be field, custom_field, or file. The field type of " + notationParts[1] + " is invalid.");
        }
        String fieldKey = notationParts[2];

        Boolean returnSingle = Boolean.TRUE;
        int index = 0;
        String dictKey = null;

        Pattern predicatePattern = Pattern.compile("\\[.*$");
        Matcher predicateMatcher = predicatePattern.matcher(fieldKey);
        if (predicateMatcher.find()) {
           String match = predicateMatcher.group(0);
           String[] predicateParts = match.split("]");

           if (predicateParts.length > 2 ) {
               throw new Exception("The predicate of the notation appears to be invalid. Too many [], max 2 allowed.");
           }

           // This will remove the preceding '[' character.
           String firstPredicate = predicateParts[0].substring(1);

           // If there was a value, then we need to find out if it's a index into an array or a dictionary key
           if ( !firstPredicate.equals("") ) {

               // Is the first predicate an index into an array?
               try {
                   index = Integer.parseInt(firstPredicate);
               }
               catch (NumberFormatException e) {
                   // Is the first predicate a key to a dictionary?
                   Pattern dictKeyPattern = Pattern.compile("^[a-zA-Z0-9_]+$");
                   Matcher dictKeyMatcher = dictKeyPattern.matcher(firstPredicate);
                   if(dictKeyMatcher.find()) {
                       dictKey = firstPredicate;
                   }
               }
           }
           // Indicate that we wanted the entire array, not just a single value.
           else {
               returnSingle = Boolean.FALSE;
           }

           // Is there a second predicate [first][second]
           if (predicateParts.length == 2) {
               if (!returnSingle) {
                   throw new Exception("If the second [] is a dictionary key, the first [] needs to have any index.");
               }
               // This will remove the preceding '[' character.
               String secondPredicate = predicateParts[1].substring(1);
               try {
                   int ignored = Integer.parseInt(secondPredicate);
                   throw new Exception("If the second [] is a dictionary key, the first [] needs to have any index.");
               } catch (NumberFormatException ignored) {}
               Pattern dictKeyPattern = Pattern.compile("^[a-zA-Z0-9_]+$");
               Matcher dictKeyMatcher = dictKeyPattern.matcher(secondPredicate);
               if(dictKeyMatcher.find()) {
                   dictKey = secondPredicate;
               }
               else {
                   throw new Exception("The second [] must have key for the dictionary. Cannot be blank.");
               }
           }

           // Remove the predicate from the key. We know one exists, else we wouldn't be in this conditional block.
           String[] keyParts = fieldKey.split("\\[");
           fieldKey = keyParts[0];
        }

        return new KsmNotationItem(name, notation, uid, fieldDataType, fieldKey, returnSingle, index, dictKey, allowFailure);
    }

    /**
     * Run over the notation item and get their values
     *
     *
     * @param items   list of notation items
     * @param options connection information for Keeper Secrets Manager
     * @throws Exception
     */

    public static void run(Map<String, KsmNotationItem> items, SecretsManagerOptions options) throws Throwable {

        // Find all the unique record UIDs in the requests.
        Set<String> uniqueUids = new HashSet<>();
        for (Map.Entry<String, KsmNotationItem> entry : items.entrySet()) {
            uniqueUids.add(entry.getValue().getUid());
        }

        // Query the unique record ids.
        KeeperSecrets secrets = SecretsManager.getSecrets(options, new ArrayList<>(uniqueUids));

        // Place the secret into a lookup map using their Uid as the key.
        Map<String, KeeperRecord> secretMap = new HashMap<>();
        for (KeeperRecord secret : secrets.getRecords()) {
            secretMap.put(secret.getRecordUid(), secret);
        }

        KsmFieldParse recordField = new KsmFieldParse(options);

        for (Map.Entry<String, KsmNotationItem> entry : items.entrySet()) {
            KsmNotationItem item = entry.getValue();
            KeeperRecord record = secretMap.get(item.getUid());

            if ( record == null) {
                String msg = "Cannot find record with uid " + item.getUid();
                item.setError(msg);
                if (!item.getAllowFailure()) {
                    throw new Exception("For environment variable " + item.getName() + ", " + item.getNotation() + ": " +
                            msg
                    );
                }
                continue;
            }
            recordField.run(item, record);
        }
    }
}
