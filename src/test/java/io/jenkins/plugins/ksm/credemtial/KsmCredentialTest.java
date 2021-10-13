package io.jenkins.plugins.ksm.credemtial;

import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class KsmCredentialTest {

    @ClassRule
    // Used to get an instance of Jenkins
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void testSmoke() {

        String clientId = "HvZKz9VBARON9nfhgbpTw3sG5EA7AVOkMXdHFu+cxXd1sHbUCoWM113tp1GdZ9iuhX+9YYl2wyqir8j637uBCA==";
        String privateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgeBjadIx4hRcZMkADIvhb076KWfsp4cmhnufDovLV"
                + "bxmgCgYIKoZIzj0DAQehRANCAAQKFzyJLkhasgDWC1Z5wnhFZ5416BqRL8TRMpJj2nOvfPs/wfsf8MXW2HUp54qz"
                + "zgi/zwLkNiFBmIzTGWE/A9oE";
        String appKey = "zhLwBKEIdiXaqVwlpnIXEl6jm/nO7WpPxYhKZv2LPGY=";

        KsmCredential credential = new KsmCredential(
                CredentialsScope.GLOBAL,
                "MYID",
                "MYCRED",
                "",
                Secret.fromString(clientId),
                Secret.fromString(privateKey),
                Secret.fromString(appKey),
                "keepersecurity.com",
                false,
                true);


        assertEquals("MYID", credential.getId());
        assertEquals("MYCRED", credential.getDescription());
        assertEquals(credential.getClientId().getPlainText(), clientId);
        assertEquals(credential.getPrivateKey().getPlainText(), privateKey);
        assertEquals(credential.getAppKey().getPlainText(), appKey);
        assertEquals(credential.getHostname(), "keepersecurity.com");
        assertFalse(credential.getSkipSslVerification());
        assertTrue(credential.getAllowConfigInject());

        assertEquals("", credential.getToken());
    }

    @Test
    public void testValidation() {


        String clientId = "HvZKz9VBARON9nfhgbpTw3sG5EA7AVOkMXdHFu+cxXd1sHbUCoWM113tp1GdZ9iuhX+9YYl2wyqir8j637uBCA==";
        String privateKey = "MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgeBjadIx4hRcZMkADIvhb076KWfsp4cmhnufDovLV"
                + "bxmgCgYIKoZIzj0DAQehRANCAAQKFzyJLkhasgDWC1Z5wnhFZ5416BqRL8TRMpJj2nOvfPs/wfsf8MXW2HUp54qz"
                + "zgi/zwLkNiFBmIzTGWE/A9oE";
        String appKey = "zhLwBKEIdiXaqVwlpnIXEl6jm/nO7WpPxYhKZv2LPGY=";

        KsmCredential credential = new KsmCredential(
                CredentialsScope.GLOBAL,
                "MYID",
                "MYCRED",
                "",
                Secret.fromString(clientId),
                Secret.fromString(privateKey),
                Secret.fromString(appKey),
                "keepersecurity.com",
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
