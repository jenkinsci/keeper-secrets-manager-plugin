package io.jenkins.plugins.ksm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KsmCommonTest {

    @Test
    public void validFilePathRejectsAbsoluteUnixPath() {
        assertFalse(KsmCommon.validFilePath("/etc/passwd"));
        assertFalse(KsmCommon.validFilePath("/var/log/secret"));
    }

    @Test
    public void validFilePathRejectsAbsoluteWindowsPath() {
        assertFalse(KsmCommon.validFilePath("\\Windows\\System32\\secret"));
        assertFalse(KsmCommon.validFilePath("C:\\Users\\admin\\secret"));
        assertFalse(KsmCommon.validFilePath("c:/Users/admin/secret"));
    }

    @Test
    public void validFilePathRejectsParentTraversal() {
        assertFalse(KsmCommon.validFilePath("../escape"));
        assertFalse(KsmCommon.validFilePath("foo/../escape"));
        assertFalse(KsmCommon.validFilePath("a/b/../../escape"));
    }

    @Test
    public void validFilePathRejectsBareDotDot() {
        // A path of just "..": new File("..").getParentFile() is null, so the
        // walker must still inspect the first component, otherwise "..": passes.
        assertFalse(KsmCommon.validFilePath(".."));
    }

    @Test
    public void validFilePathAcceptsPlainFileName() {
        assertTrue(KsmCommon.validFilePath("secret.txt"));
        assertTrue(KsmCommon.validFilePath("password"));
        assertTrue(KsmCommon.validFilePath("my.png"));
    }

    @Test
    public void validFilePathAcceptsSubdirectory() {
        // writeFileToWorkspace() creates parent dirs via mkdirs(); valid file
        // paths must allow nested directories like "secrets/db-password".
        assertTrue(KsmCommon.validFilePath("secrets/db-password"));
        assertTrue(KsmCommon.validFilePath("a/b/c/secret.txt"));
    }
}
