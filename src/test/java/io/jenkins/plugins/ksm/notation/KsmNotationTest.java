package io.jenkins.plugins.ksm.notation;

import static org.junit.Assert.assertEquals;
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
    public void parseRejectsShortUid() {
        // The plugin currently enforces a 22-char record token. SDK supports
        // record-by-title lookup; broader support is tracked as a follow-up.
        Exception e = assertThrows(Exception.class,
                () -> KsmNotation.parse("ABC", "keeper://SHORT/field/login", false));
        assertTrue(e.getMessage().toLowerCase().contains("uid"));
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
