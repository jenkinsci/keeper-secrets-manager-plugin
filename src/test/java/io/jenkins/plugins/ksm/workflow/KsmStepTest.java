package io.jenkins.plugins.ksm.workflow;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.FilePath;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.KsmSecret;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.notation.KsmTestNotation;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jvnet.hudson.test.JenkinsRule;
import java.io.UnsupportedEncodingException;
import java.util.*;
import org.junit.*;

import static org.junit.Assert.assertFalse;

public class KsmStepTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @SuppressWarnings("unchecked")
    @Before
    public void makeTestData() throws UnsupportedEncodingException {

        // Tiny 2x2 PNG
        String pngBase64 =
                "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAAXNSR0IArs4c6QAAAMJlWElmTU0AKgAAAAgABwESAAMAAAABAAEAAA"
                + "EaAAUAAAABAAAAYgEbAAUAAAABAAAAagEoAAMAAAABAAIAAAExAAIAAAARAAAAcgEyAAIAAAAUAAAAhIdpAAQAAAABAAAAmAAAAA"
                + "AAAABIAAAAAQAAAEgAAAABUGl4ZWxtYXRvciAzLjkuOAAAMjAyMTowNzoyMiAxNDowNzo3NgAAA6ABAAMAAAABAAEAAKACAAQAAA"
                + "ABAAAAAqADAAQAAAABAAAAAgAAAAByx+BYAAAACXBIWXMAAAsTAAALEwEAmpwYAAADpmlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAA"
                + "AAPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNi4wLjAiPgogICA8cmRmOlJERi"
                + "B4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaX"
                + "B0aW9uIHJkZjphYm91dD0iIgogICAgICAgICAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKIC"
                + "AgICAgICAgICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICAgICAgICAgIHhtbG5zOnhtcD"
                + "0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyI+CiAgICAgICAgIDx0aWZmOkNvbXByZXNzaW9uPjA8L3RpZmY6Q29tcHJlc3"
                + "Npb24+CiAgICAgICAgIDx0aWZmOlJlc29sdXRpb25Vbml0PjI8L3RpZmY6UmVzb2x1dGlvblVuaXQ+CiAgICAgICAgIDx0aWZmOl"
                + "hSZXNvbHV0aW9uPjcyPC90aWZmOlhSZXNvbHV0aW9uPgogICAgICAgICA8dGlmZjpZUmVzb2x1dGlvbj43MjwvdGlmZjpZUmVzb2"
                + "x1dGlvbj4KICAgICAgICAgPHRpZmY6T3JpZW50YXRpb24+MTwvdGlmZjpPcmllbnRhdGlvbj4KICAgICAgICAgPGV4aWY6UGl4ZW"
                + "xYRGltZW5zaW9uPjI8L2V4aWY6UGl4ZWxYRGltZW5zaW9uPgogICAgICAgICA8ZXhpZjpDb2xvclNwYWNlPjE8L2V4aWY6Q29sb3"
                + "JTcGFjZT4KICAgICAgICAgPGV4aWY6UGl4ZWxZRGltZW5zaW9uPjI8L2V4aWY6UGl4ZWxZRGltZW5zaW9uPgogICAgICAgICA8eG"
                + "1wOkNyZWF0b3JUb29sPlBpeGVsbWF0b3IgMy45Ljg8L3htcDpDcmVhdG9yVG9vbD4KICAgICAgICAgPHhtcDpNb2RpZnlEYXRlPj"
                + "IwMjEtMDctMjJUMTQ6MDc6NzY8L3htcDpNb2RpZnlEYXRlPgogICAgICA8L3JkZjpEZXNjcmlwdGlvbj4KICAgPC9yZGY6UkRGPg"
                + "o8L3g6eG1wbWV0YT4KPKL2agAAABNJREFUCB1j/M8gy8DAwATEQAAADlwBIHTDGBYAAAAASUVORK5CYII=";

        // We need to get our test data into the Step. This will write a file that will be read.
        KsmTestNotation notation = new KsmTestNotation();

        // Make some test JSON
        JSONObject obj = new JSONObject();
        JSONArray array = new JSONArray();
        JSONObject secret1 = new JSONObject();
        secret1.put("uid", "A_7YpGBUgRTeDEQLhVRo0Q");
        secret1.put("title", "My Record");
        secret1.put("type", "login");
        secret1.put("note", "None");
        JSONArray fields = new JSONArray();
        // Field login
        JSONObject field = new JSONObject();
        field.put("label", "login");
        field.put("fieldType", "Login");
        JSONArray value = new JSONArray();
        value.add("My$Login");
        field.put("values", value);
        fields.add(field);
        // Field password
        field = new JSONObject();
        field.put("label", "password");
        field.put("fieldType", "Password");
        value = new JSONArray();
        value.add("Pa$$w0rd!!");
        field.put("values", value);
        fields.add(field);
        // Field URL
        field = new JSONObject();
        field.put("label", "url");
        field.put("fieldType", "Url");
        value = new JSONArray();
        value.add("http://localhost");
        field.put("values", value);
        fields.add(field);

        secret1.put("fields", fields);

        // File section
        JSONArray files = new JSONArray();
        JSONObject file = new JSONObject();
        file.put("name", "my.png");
        file.put("data", pngBase64);
        files.add(file);
        secret1.put("files", files);

        array.add(secret1);
        obj.put("secrets", array);
        // DONE making test JSON

        notation.writeJsonData(obj.toJSONString());
    }

    @After
    public void removeTestData() {
        KsmTestNotation.removeJsonData();
    }

    @Test
    public void testStep() throws Exception {

        List<KsmSecret> secrets = new ArrayList<KsmSecret>();
        secrets.add(new KsmSecret(
                "keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/login",
                KsmSecret.destinationEnvVar,
                "LOGIN",
                null));
        secrets.add(new KsmSecret(
                "keeper://A_7YpGBUgRTeDEQLhVRo0Q/file/my.png",
                KsmSecret.destinationFilePath,
                null,
                "my.png"));

        ArrayList<String> uids = new ArrayList<>();
        uids.add("A_7YpGBUgRTeDEQLhVRo0Q");

        KsmCredential credential = new KsmCredential(
                CredentialsScope.GLOBAL,
                "MYID",
                "",
                "",
                Secret.fromString("HvZKz9VBARON9nfhgbpTw3sG5EA7AVOkMXdHFu+cxXd1sHbUCoWM113tp1GdZ9iuhX+9YYl2wyqir8j637uBCA=="),
                Secret.fromString("MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgeBjadIx4hRcZMkADIvhb076KWfsp4cmhnufDovLV"
                        + "bxmgCgYIKoZIzj0DAQehRANCAAQKFzyJLkhasgDWC1Z5wnhFZ5416BqRL8TRMpJj2nOvfPs/wfsf8MXW2HUp54qz"
                        + "zgi/zwLkNiFBmIzTGWE/A9oE"),
                Secret.fromString("zhLwBKEIdiXaqVwlpnIXEl6jm/nO7WpPxYhKZv2LPGY="),
                "keepersecurity.com",
                false,
                true);

        String credentialId = credential.getId();

        SystemCredentialsProvider.getInstance()
                .setDomainCredentialsMap(Collections.singletonMap(Domain.global(), Arrays
                        .asList(credential)));

        WorkflowJob job = j.getInstance().createProject(WorkflowJob.class, "test-workflow-job");
        String pipelineScript
                = "node {\n"
                + "        withKsm(application: [\n"
                + "          [ credentialsId: '" + credentialId + "',\n"
                + "            secrets: [\n"
                + "              [destination: 'env', envVar: 'MY_LOGIN', notation: 'keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/login'],\n"
                + "              [destination: 'file', filePath: 'out.txt', notation: 'keeper://A_7YpGBUgRTeDEQLhVRo0Q/file/my.png']\n"
                + "            ]\n"
                + "          ]\n"
                + "        ]) {\n"
                + "          echo MY_LOGIN"
                + "        }\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        FilePath workspace = j.jenkins.getWorkspaceFor(job);
        assert workspace != null;

        // The value SECRET_LOGIN should be redacted to ****
        j.assertLogContains("****", completedBuild);

        // Make sure the png is missing
        assertFalse(workspace.child("my.png").exists());
    }
}