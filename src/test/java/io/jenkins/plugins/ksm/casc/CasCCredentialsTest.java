package io.jenkins.plugins.ksm.casc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;

import com.cloudbees.plugins.credentials.CredentialsProvider;

import hudson.security.ACL;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.ksm.credential.KsmCredential;

/**
 * Test Jenkins configuration as code.
 *
 * @author Nikolas Falco
 */
public class CasCCredentialsTest {

    @ClassRule
    @ConfiguredWithCode("config-credentials.yaml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void import_system_credentials() {
        List<KsmCredential> creds = CredentialsProvider.lookupCredentials(KsmCredential.class, j.jenkins, ACL.SYSTEM, Collections.emptyList());
        assertThat(creds, hasSize(1));

        final KsmCredential c = creds.get(0);
        assertThat(c.getToken(), equalTo(""));
        assertThat(c.getDescription(), equalTo("test"));
        assertFalse(c.getSkipSslVerification());
        assertTrue(c.getAllowConfigInject());
    }
}