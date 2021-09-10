package io.jenkins.plugins.ksm.workflow;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.KsmSecret;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.notation.KsmTestNotation;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jvnet.hudson.test.JenkinsRule;
import java.util.*;
import org.junit.*;

public class KsmStepTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Before
    public void makeTestData() {

        // We need to get our test data into the Step. This will write a file that will be read.
        KsmTestNotation notation = new KsmTestNotation();
        String jsonString = "{\"secrets\":[{\"uid\": \"A_7YpGBUgRTeDEQLhVRo0Q\",\"title\":\"My Record\","
                + "\"type\":\"login\",\"note\":\"None\",\"fields\":["
                + "{\"label\":\"login\",\"fieldType\":\"Login\",\"values\":[\"SECRET_LOGIN\"]}"
                + "]}]}";
        notation.writeJsonData(jsonString);
    }

    @After
    public void removeTestData() {
        KsmTestNotation.removeJsonData();
    }

    @Test
    public void testStep() throws Exception {

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

        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "test-workflow-job");
        String pipelineScript
                = "node {\n"
                + "        withKsm(application: [\n"
                + "          [ credentialsId: '" + credential.getId() + "',\n"
                + "            secrets: [\n"
                + "              [envVar: 'MY_LOGIN', notation: 'keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/login']\n"
                + "            ]\n"
                + "          ]\n"
                + "        ]) {\n"
                + "          echo MY_LOGIN"
                + "        }\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        j.assertLogContains("SECRET_LOGIN", completedBuild);
    }
}
