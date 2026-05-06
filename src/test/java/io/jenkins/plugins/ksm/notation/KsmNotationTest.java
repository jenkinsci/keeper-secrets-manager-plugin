package io.jenkins.plugins.ksm.notation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.*;

public class KsmNotationTest {

    private static final String GOOD_UID = "A_7YpGBUgRTeDEQLhVRo0Q";

    @Test
    public void testSmokeTest() {

        try {
            KsmNotationItem result = KsmNotation.parse("ABC", "keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/password", false);
            Assert.assertEquals("A_7YpGBUgRTeDEQLhVRo0Q", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.STANDARD, result.getFieldDataType());
            Assert.assertEquals("password", result.getFieldKey());
            Assert.assertEquals(Boolean.TRUE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(0), result.getArrayIndex());
            Assert.assertNull(result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }

        try {
            KsmNotationItem result = KsmNotation.parse("ABC", "keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/password[0]", false);
            Assert.assertEquals("A_7YpGBUgRTeDEQLhVRo0Q", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.STANDARD, result.getFieldDataType());
            Assert.assertEquals("password", result.getFieldKey());
            Assert.assertEquals(Boolean.TRUE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(0), result.getArrayIndex());
            Assert.assertNull(result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }

        try {
            KsmNotationItem result = KsmNotation.parse("ABC", "keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/password[]", false);
            Assert.assertEquals("A_7YpGBUgRTeDEQLhVRo0Q", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.STANDARD, result.getFieldDataType());
            Assert.assertEquals("password", result.getFieldKey());
            Assert.assertEquals(Boolean.FALSE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(0), result.getArrayIndex());
            Assert.assertNull(result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }

        try {
            KsmNotationItem result = KsmNotation.parse("ABC","keeper://A_7YpGBUgRTeDEQLhVRo0Q/custom_field/name[first]", false);
            Assert.assertEquals("A_7YpGBUgRTeDEQLhVRo0Q", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.CUSTOM, result.getFieldDataType());
            Assert.assertEquals("name", result.getFieldKey());
            Assert.assertEquals(Boolean.TRUE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(0), result.getArrayIndex());
            Assert.assertEquals("first", result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }

        try {
            KsmNotationItem result = KsmNotation.parse("ABC", "keeper://A_7YpGBUgRTeDEQLhVRo0Q/custom_field/name[2][last]", false);
            Assert.assertEquals("A_7YpGBUgRTeDEQLhVRo0Q", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.CUSTOM, result.getFieldDataType());
            Assert.assertEquals("name", result.getFieldKey());
            Assert.assertEquals(Boolean.TRUE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(2), result.getArrayIndex());
            Assert.assertEquals("last", result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }
    }

    @Test
    public void parseAcceptsNotationWithoutPrefix() throws Exception {
        KsmNotationItem result = KsmNotation.parse("ABC", GOOD_UID + "/field/login", false);
        assertEquals(GOOD_UID, result.getUid());
        assertEquals(KsmFieldDataEnumType.STANDARD, result.getFieldDataType());
        assertEquals("login", result.getFieldKey());
    }

    @Test
    public void parseAcceptsFileSelector() throws Exception {
        KsmNotationItem result = KsmNotation.parse("ABC", "keeper://" + GOOD_UID + "/file/my.png", false);
        assertEquals(KsmFieldDataEnumType.FILE, result.getFieldDataType());
        assertEquals("my.png", result.getFieldKey());
    }

    @Test
    public void parseRejectsMissingFieldType() {
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper://" + GOOD_UID, false));
        assertTrue(e.getMessage().toLowerCase().contains("missing"));
    }

    @Test
    public void parseRejectsTooFewParts() {
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper://" + GOOD_UID + "/field", false));
        assertTrue(e.getMessage().toLowerCase().contains("missing"));
    }

    @Test
    public void parseRejectsTooManyParts() {
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper://" + GOOD_UID + "/field/login/extra", false));
        assertTrue(e.getMessage().toLowerCase().contains("too many"));
    }

    @Test
    public void parseAcceptsRecordTitleAsToken() throws Exception {
        // The SDK supports record lookup by title; the plugin no longer
        // rejects non-22-char tokens (#43).
        KsmNotationItem result = KsmNotation.parse("ABC",
                "keeper://My Record Title/field/login", false);
        assertEquals("My Record Title", result.getUid());
        assertEquals(KsmFieldDataEnumType.STANDARD, result.getFieldDataType());
        assertEquals("login", result.getFieldKey());
    }

    @Test
    public void parseRejectsEmptyToken() {
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper:///field/login", false));
        assertTrue(e.getMessage().toLowerCase().contains("missing"));
    }

    @Test
    public void looksLikeUidAcceptsTwentyTwoCharBase64() {
        assertTrue(KsmNotation.looksLikeUid("A_7YpGBUgRTeDEQLhVRo0Q"));
        assertTrue(KsmNotation.looksLikeUid("abcdefghijklmnopqrstuv"));
        assertTrue(KsmNotation.looksLikeUid("ABCDEFGHIJKLMNOPQRSTUV"));
        assertTrue(KsmNotation.looksLikeUid("0123456789012345678901"));
        assertTrue(KsmNotation.looksLikeUid("aaaa-aaaa_aaaa-aaaa_aa"));
    }

    @Test
    public void looksLikeUidAcceptsLeadingDash() {
        // Legacy records may have UIDs starting with '-'. Newer records do not,
        // but we still see them in the wild and must not misclassify them as
        // titles.
        assertTrue(KsmNotation.looksLikeUid("-AbCdEfGhIjKlMnOpQrStU"));  // 22 chars
        assertTrue(KsmNotation.looksLikeUid("-_---_---_---_---_---_"));  // 22 chars
        assertTrue(KsmNotation.looksLikeUid("-1234567890ABCDEFGHIJK"));  // 22 chars
    }

    @Test
    public void looksLikeUidRejectsOtherFormats() {
        assertFalse(KsmNotation.looksLikeUid(null));
        assertFalse(KsmNotation.looksLikeUid(""));
        assertFalse(KsmNotation.looksLikeUid("short"));
        assertFalse(KsmNotation.looksLikeUid("waytoolongwaytoolongwaytoolongwaytoolong"));
        assertFalse(KsmNotation.looksLikeUid("contains spaces in name"));
        assertFalse(KsmNotation.looksLikeUid("hasplus+signsinit_aaaa"));
        assertFalse(KsmNotation.looksLikeUid("My Record Title"));
    }

    @Test
    public void parseAcceptsLeadingDashUid() throws Exception {
        // Legacy UIDs may start with '-'; ensure the parser does not treat them
        // as something else and that looksLikeUid agrees.
        String uid = "-AbCdEfGhIjKlMnOpQrStU";  // 22 chars
        KsmNotationItem result = KsmNotation.parse("ABC", "keeper://" + uid + "/field/login", false);
        assertEquals(uid, result.getUid());
        assertTrue(KsmNotation.looksLikeUid(result.getUid()));
    }

    @Test
    public void parseSetsCorrectEnumForEachSelector() throws Exception {
        assertEquals(KsmFieldDataEnumType.STANDARD,
                KsmNotation.parse("X", "keeper://" + GOOD_UID + "/field/login", false).getFieldDataType());
        assertEquals(KsmFieldDataEnumType.CUSTOM,
                KsmNotation.parse("X", "keeper://" + GOOD_UID + "/custom_field/MyLabel", false).getFieldDataType());
        assertEquals(KsmFieldDataEnumType.FILE,
                KsmNotation.parse("X", "keeper://" + GOOD_UID + "/file/qr.png", false).getFieldDataType());
    }

    @Test
    public void parseExtractsArrayIndexFromFirstPredicate() throws Exception {
        KsmNotationItem r = KsmNotation.parse("X", "keeper://" + GOOD_UID + "/custom_field/phone[2]", false);
        assertEquals("phone", r.getFieldKey());
        assertEquals(Integer.valueOf(2), r.getArrayIndex());
        assertEquals(Boolean.TRUE, r.getReturnSingle());
        assertNull(r.getDictKey());
    }

    @Test
    public void parseExtractsDictKeyFromFirstPredicate() throws Exception {
        // Mirrors SDK test: /custom_field/name[first] returns the dict key.
        KsmNotationItem r = KsmNotation.parse("X", "keeper://" + GOOD_UID + "/custom_field/name[first]", false);
        assertEquals("name", r.getFieldKey());
        assertEquals(Integer.valueOf(0), r.getArrayIndex());
        assertEquals("first", r.getDictKey());
    }

    @Test
    public void parseExtractsBothPredicates() throws Exception {
        // Mirrors SDK test: /custom_field/phone[0][number].
        KsmNotationItem r = KsmNotation.parse("X", "keeper://" + GOOD_UID + "/custom_field/phone[0][number]", false);
        assertEquals("phone", r.getFieldKey());
        assertEquals(Integer.valueOf(0), r.getArrayIndex());
        assertEquals("number", r.getDictKey());
        assertEquals(Boolean.TRUE, r.getReturnSingle());
    }

    @Test
    public void parseEmptyBracketsMeansFullArray() throws Exception {
        KsmNotationItem r = KsmNotation.parse("X", "keeper://" + GOOD_UID + "/field/login[]", false);
        assertEquals(Boolean.FALSE, r.getReturnSingle());
        assertEquals(Integer.valueOf(0), r.getArrayIndex());
        assertNull(r.getDictKey());
    }

    @Test
    public void parseHandlesLabelsContainingSpaces() throws Exception {
        // SDK supports custom field labels with spaces like "My Custom 2".
        KsmNotationItem r = KsmNotation.parse("X",
                "keeper://" + GOOD_UID + "/custom_field/My Custom 2[1]", false);
        assertEquals("My Custom 2", r.getFieldKey());
        assertEquals(Integer.valueOf(1), r.getArrayIndex());
    }

    @Test
    public void parseHandlesFileNameWithDot() throws Exception {
        KsmNotationItem r = KsmNotation.parse("X",
                "keeper://" + GOOD_UID + "/file/qr.png", false);
        assertEquals(KsmFieldDataEnumType.FILE, r.getFieldDataType());
        assertEquals("qr.png", r.getFieldKey());
    }

    @Test
    public void parseTitleTokenIsNotMisclassifiedAsUid() throws Exception {
        KsmNotationItem r = KsmNotation.parse("X",
                "keeper://My Record/field/login", false);
        assertEquals("My Record", r.getUid());
        assertFalse(KsmNotation.looksLikeUid(r.getUid()));
    }

    @Test
    public void parseHonoursAllowFailureFlag() throws Exception {
        KsmNotationItem r = KsmNotation.parse("X",
                "keeper://" + GOOD_UID + "/field/login", true);
        assertTrue(r.getAllowFailure());
        KsmNotationItem r2 = KsmNotation.parse("X",
                "keeper://" + GOOD_UID + "/field/login", false);
        assertFalse(r2.getAllowFailure());
    }

    @Test
    public void parseRejectsUnknownFieldType() {
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper://" + GOOD_UID + "/bogus/login", false));
        assertTrue(e.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    public void parseRejectsThreeBrackets() {
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper://" + GOOD_UID + "/custom_field/name[1][a][b]", false));
        assertTrue(e.getMessage().toLowerCase().contains("max 2"));
    }

    @Test
    public void parseRejectsEmptySecondPredicate() {
        // Second [] must be a dictionary key, never numeric and never blank.
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper://" + GOOD_UID + "/custom_field/name[0][]", false));
        assertTrue(e.getMessage().toLowerCase().contains("blank")
                || e.getMessage().toLowerCase().contains("dictionary"));
    }

    @Test
    public void parseRejectsNumericSecondPredicate() {
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper://" + GOOD_UID + "/custom_field/name[0][1]", false));
        assertTrue(e.getMessage().toLowerCase().contains("dictionary"));
    }

    @Test
    public void parseRejectsArrayWithDictKey() {
        // First [] returns the entire array; cannot combine with a second key.
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper://" + GOOD_UID + "/custom_field/name[][first]", false));
        assertTrue(e.getMessage().toLowerCase().contains("index"));
    }

    @Test
    public void parseTreatsUnparseableFirstPredicateAsNoKey() throws Exception {
        // A predicate like [a-b] does not match the int parser nor the
        // dictKey regex (no hyphens). Behavior is "no key, default index 0".
        KsmNotationItem result = KsmNotation.parse("ABC",
                "keeper://" + GOOD_UID + "/custom_field/name[a-b]", false);
        assertEquals(Integer.valueOf(0), result.getArrayIndex());
        assertNull(result.getDictKey());
    }
}
