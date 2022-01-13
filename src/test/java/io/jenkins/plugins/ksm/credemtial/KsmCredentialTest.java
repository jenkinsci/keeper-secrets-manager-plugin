package io.jenkins.plugins.ksm.credemtial;

import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.MockConfig;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.HashMap;

import static org.junit.Assert.*;

public class KsmCredentialTest {

    @ClassRule
    // Used to get an instance of Jenkins
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void testSmoke() throws Exception {

        HashMap<String, String> mockConfig = new MockConfig().makeConfig();

        KsmCredential credential = new KsmCredential(
                CredentialsScope.GLOBAL,
                "MYID",
                "MYCRED",
                "",
                Secret.fromString(mockConfig.get("clientId")),
                Secret.fromString(mockConfig.get("privateKey")),
                Secret.fromString(mockConfig.get("appKey")),
                mockConfig.get("hostname"),
                false,
                true);


        assertEquals("MYID", credential.getId());
        assertEquals("MYCRED", credential.getDescription());
        assertEquals(credential.getClientId().getPlainText(), mockConfig.get("clientId"));
        assertEquals(credential.getPrivateKey().getPlainText(), mockConfig.get("privateKey"));
        assertEquals(credential.getAppKey().getPlainText(), mockConfig.get("appKey"));
        assertEquals(credential.getHostname(), mockConfig.get("hostname"));
        assertFalse(credential.getSkipSslVerification());
        assertTrue(credential.getAllowConfigInject());

        assertEquals("", credential.getToken());
    }

    @Test
    public void testValidation() throws Exception {

        HashMap<String, String> mockConfig = new MockConfig().makeConfig();


        KsmCredential credential = new KsmCredential(
                CredentialsScope.GLOBAL,
                "MYID",
                "MYCRED",
                "",
                Secret.fromString(mockConfig.get("clientId")),
                Secret.fromString(mockConfig.get("privateKey")),
                Secret.fromString(mockConfig.get("appKey")),
                mockConfig.get("hostname"),
                false,
                true);


        KsmCredential.DescriptorImpl descriptor = (KsmCredential.DescriptorImpl) credential.getDescriptor();

        // Using the one time token has an error. Highlight the error in the UI.
        FormValidation result = descriptor.doCheckToken(KsmCredential.tokenErrorPrefix + " I HAD AN ERROR");
        assertEquals("ERROR: There appears to be an error with the token.",
                result.toString());

        // The token work, the field should be blank.
        result = descriptor.doCheckToken("");
        assertEquals("OK: <div/>", result.toString());

        // Error if the hostname is blank.
        result = descriptor.doCheckHostname("");
        assertEquals("ERROR: Hostname cannot be blank.", result.toString());

        // Ok if anything is in there.
        result = descriptor.doCheckHostname("US");
        assertEquals("OK: <div/>", result.toString());

        // Error if the description is blank.
        result = descriptor.doCheckDescription("");
        assertEquals("ERROR: Description cannot be blank.", result.toString());

        // Ok if anything is in there.
        result = descriptor.doCheckDescription("I HAVE TEXT");
        assertEquals("OK: <div/>", result.toString());
    }
}
