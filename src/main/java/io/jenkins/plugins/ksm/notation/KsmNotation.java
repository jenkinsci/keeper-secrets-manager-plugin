package io.jenkins.plugins.ksm.notation;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.SuppressWarnings;


public class KsmNotation {

    // A notation might start with a prefix, that will need to be removed. This is the that String prefix.
    public static final String notationPrefix = "keeper";

    @SuppressWarnings("unused")
    public static KsmNotationRequest parse(String notation) throws Exception {

        /**
         * Notation : A system of figures or symbols used in a specialized field to represent numbers, quantities, tones,
         * or values
         *
         * <uid>/<field|custom_field|file>/<label|type>[INDEX][FIELD]
         *
         * Examples:
         *
         *  EG6KdJaaLG7esRZbMnfbFA/field/password                => MyPasswprd
         *  EG6KdJaaLG7esRZbMnfbFA/field/password[0]             => MyPassword
         *  EG6KdJaaLG7esRZbMnfbFA/field/password[]              => ["MyPassword"]
         *  EG6KdJaaLG7esRZbMnfbFA/custom_field/name[first]      => John
         *  EG6KdJaaLG7esRZbMnfbFA/custom_field/name[last]       => Smitht
         *  EG6KdJaaLG7esRZbMnfbFA/custom_field/phone[0][number] => "555-5555555"
         *  EG6KdJaaLG7esRZbMnfbFA/custom_field/phone[1][number] => "777-7777777"
         *  EG6KdJaaLG7esRZbMnfbFA/custom_field/phone[]          => [{"number": "555-555...}, { "number": "777.....}]
         *  EG6KdJaaLG7esRZbMnfbFA/custom_field/phone[0]         => [{"number": "555-555...}]
         *
         */

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
        String uid = notationParts[0];
        KsmFieldDataEnumType fieldDataType = KsmFieldDataEnumType.getEnumByString(notationParts[1]);
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


        return new KsmNotationRequest(uid, fieldDataType, fieldKey, returnSingle, index, dictKey);
    }
}
