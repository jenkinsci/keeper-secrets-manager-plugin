package io.jenkins.plugins.ksm.notation;
import org.junit.*;

public class KSMNotationTest {

    @Test
    public void testSmokeTest() {

        try {
            KsmNotationItem result = KsmNotation.parse("ABC", "keeper://XXXX/field/password");
            Assert.assertEquals("XXXX", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.STANDARD, result.getFieldDataType());
            Assert.assertEquals("password", result.getFieldKey());
            Assert.assertEquals(Boolean.TRUE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(0), result.getArrayIndex());
            Assert.assertNull(result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }

        try {
            KsmNotationItem result = KsmNotation.parse("ABC", "keeper://XXXX/field/password[0]");
            Assert.assertEquals("XXXX", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.STANDARD, result.getFieldDataType());
            Assert.assertEquals("password", result.getFieldKey());
            Assert.assertEquals(Boolean.TRUE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(0), result.getArrayIndex());
            Assert.assertNull(result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }

        try {
            KsmNotationItem result = KsmNotation.parse("ABC", "keeper://XXXX/field/password[]");
            Assert.assertEquals("XXXX", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.STANDARD, result.getFieldDataType());
            Assert.assertEquals("password", result.getFieldKey());
            Assert.assertEquals(Boolean.FALSE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(0), result.getArrayIndex());
            Assert.assertNull(result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }

        try {
            KsmNotationItem result = KsmNotation.parse("ABC","keeper://XXXX/custom_field/name[first]");
            Assert.assertEquals("XXXX", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.CUSTOM, result.getFieldDataType());
            Assert.assertEquals("name", result.getFieldKey());
            Assert.assertEquals(Boolean.TRUE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(0), result.getArrayIndex());
            Assert.assertEquals("first", result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }

        try {
            KsmNotationItem result = KsmNotation.parse("ABC", "keeper://XXXX/custom_field/name[2][last]");
            Assert.assertEquals("XXXX", result.getUid());
            Assert.assertEquals(KsmFieldDataEnumType.CUSTOM, result.getFieldDataType());
            Assert.assertEquals("name", result.getFieldKey());
            Assert.assertEquals(Boolean.TRUE, result.getReturnSingle());
            Assert.assertEquals(Integer.valueOf(2), result.getArrayIndex());
            Assert.assertEquals("last", result.getDictKey());
        } catch(Exception e) {
            Assert.fail("Exception was thrown " + e.getMessage());
        }
    }
}
