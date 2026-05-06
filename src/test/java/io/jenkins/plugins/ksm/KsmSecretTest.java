package io.jenkins.plugins.ksm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KsmSecretTest {

    private static final String NOTATION = "keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/login";

    @Test
    public void validateAcceptsEnvVarDestination() throws Exception {
        new KsmSecret(NOTATION, KsmSecret.destinationEnvVar, "MY_VAR", null).validate();
    }

    @Test
    public void validateAcceptsFilePathDestination() throws Exception {
        new KsmSecret(NOTATION, KsmSecret.destinationFilePath, null, "secret.txt").validate();
    }

    @Test
    public void validateRejectsMissingDestination() {
        Exception e = assertThrows(Exception.class,
                () -> new KsmSecret(NOTATION, null, "X", null).validate());
        assertTrue(e.getMessage().toLowerCase().contains("destination"));
    }

    @Test
    public void validateRejectsBlankDestination() {
        Exception e = assertThrows(Exception.class,
                () -> new KsmSecret(NOTATION, "", "X", null).validate());
        assertTrue(e.getMessage().toLowerCase().contains("destination"));
    }

    @Test
    public void validateRejectsUnknownDestination() {
        Exception e = assertThrows(Exception.class,
                () -> new KsmSecret(NOTATION, "elsewhere", "X", null).validate());
        assertTrue(e.getMessage().toLowerCase().contains("destination"));
    }

    @Test
    public void validateRejectsBlankEnvVarOnEnvDestination() {
        Exception e = assertThrows(Exception.class,
                () -> new KsmSecret(NOTATION, KsmSecret.destinationEnvVar, "", null).validate());
        assertTrue(e.getMessage().toLowerCase().contains("envvar"));
    }

    @Test
    public void validateRejectsNullEnvVarOnEnvDestination() {
        Exception e = assertThrows(Exception.class,
                () -> new KsmSecret(NOTATION, KsmSecret.destinationEnvVar, null, null).validate());
        assertTrue(e.getMessage().toLowerCase().contains("envvar"));
    }

    @Test
    public void validateRejectsBlankFilePathOnFileDestination() {
        Exception e = assertThrows(Exception.class,
                () -> new KsmSecret(NOTATION, KsmSecret.destinationFilePath, null, "").validate());
        assertTrue(e.getMessage().toLowerCase().contains("filepath"));
    }

    @Test
    public void validateRejectsBlankNotation() {
        Exception e = assertThrows(Exception.class,
                () -> new KsmSecret("", KsmSecret.destinationEnvVar, "MY_VAR", null).validate());
        assertTrue(e.getMessage().toLowerCase().contains("notation"));
    }

    @Test
    public void validateRejectsNullNotation() {
        Exception e = assertThrows(Exception.class,
                () -> new KsmSecret(null, KsmSecret.destinationEnvVar, "MY_VAR", null).validate());
        assertTrue(e.getMessage().toLowerCase().contains("notation"));
    }

    @Test
    public void buildSecretNameUsesEnvVarForEnvDestination() {
        assertEquals("LOGIN",
                KsmSecret.buildSecretName(KsmSecret.destinationEnvVar, "LOGIN", null));
    }

    @Test
    public void buildSecretNameUsesFilePathForFileDestination() {
        assertEquals("secret.txt",
                KsmSecret.buildSecretName(KsmSecret.destinationFilePath, null, "secret.txt"));
    }

    @Test
    public void buildSecretNameFallsBackToFilePathWhenEnvVarNull() {
        // For env destination, a null envVar falls through to filePath. Not a
        // typical config, but the contract should be deterministic.
        assertEquals("fallback.txt",
                KsmSecret.buildSecretName(KsmSecret.destinationEnvVar, null, "fallback.txt"));
    }

    @Test
    public void getNameDelegatesToBuildSecretName() {
        KsmSecret env = new KsmSecret(NOTATION, KsmSecret.destinationEnvVar, "PASSWORD", null);
        assertEquals("PASSWORD", env.getName());
        KsmSecret file = new KsmSecret(NOTATION, KsmSecret.destinationFilePath, null, "out.txt");
        assertEquals("out.txt", file.getName());
    }

    @Test
    public void isDestinationTypeIsCaseInsensitive() {
        KsmSecret s = new KsmSecret(NOTATION, KsmSecret.destinationEnvVar, "X", null);
        assertEquals("true", s.isDestinationType("ENV"));
        assertEquals("true", s.isDestinationType("env"));
        assertEquals("", s.isDestinationType("file"));
    }
}
