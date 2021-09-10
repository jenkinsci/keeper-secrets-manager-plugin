package io.jenkins.plugins.ksm.builder;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.KsmSecret;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.notation.KsmTestNotation;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.Assert.*;

import java.util.*;


public class KsmBuilderTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void testBuilder() throws Exception {

        List<KsmSecret> secrets = new ArrayList<KsmSecret>();
        secrets.add(new KsmSecret("LOGIN", "keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/login"));

        ArrayList<String> uids = new ArrayList<>();
        uids.add("A_7YpGBUgRTeDEQLhVRo0Q");

        KsmCredential credential = new KsmCredential(
                CredentialsScope.GLOBAL,
                "MYID",
                "MYCRED",
                "",
                Secret.fromString("HvZKz9VBARON9nfhgbpTw3sG5EA7AVOkMXdHFu+cxXd1sHbUCoWM113tp1GdZ9iuhX+9YYl2wyqir8j637uBCA=="),
                Secret.fromString("MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgeBjadIx4hRcZMkADIvhb076KWfsp4cmhnufDovLV"
                                + "bxmgCgYIKoZIzj0DAQehRANCAAQKFzyJLkhasgDWC1Z5wnhFZ5416BqRL8TRMpJj2nOvfPs/wfsf8MXW2HUp54qz"
                                + "zgi/zwLkNiFBmIzTGWE/A9oE"),
                Secret.fromString("zhLwBKEIdiXaqVwlpnIXEl6jm/nO7WpPxYhKZv2LPGY="),
                "keepersecurity.com",
                false,
                true);

        SystemCredentialsProvider.getInstance()
                .setDomainCredentialsMap(Collections.singletonMap(Domain.global(), Arrays
                        .asList(credential)));

        KsmTestNotation notation = new KsmTestNotation();
        String jsonString = "{\"secrets\":[{\"uid\": \"A_7YpGBUgRTeDEQLhVRo0Q\",\"title\":\"My Record\","
                + "\"type\":\"login\",\"note\":\"None\",\"fields\":["
                + "{\"label\":\"login\",\"fieldType\":\"Login\",\"values\":[\"MY_LOGIN\"]}"
                + "]}]}";
        notation.addTestData(jsonString);


        FreeStyleProject project = j.createFreeStyleProject();
        KsmBuilder builder = new KsmBuilder(credential.getId(), secrets, notation);
        project.getBuildersList().add(builder);
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo %LOGIN% > out.txt") : new Shell("echo $LOGIN > out.txt"));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        String log = Objects.requireNonNull(build.getWorkspace()).child("out.txt").readToString().trim();
        assertEquals("MY_LOGIN", log);
    }
}
