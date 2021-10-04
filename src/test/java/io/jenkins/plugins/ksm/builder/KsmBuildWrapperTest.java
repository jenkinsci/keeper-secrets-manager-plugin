package io.jenkins.plugins.ksm.builder;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tasks.Shell;
import hudson.util.Secret;
import io.jenkins.plugins.ksm.KsmApplication;
import io.jenkins.plugins.ksm.KsmSecret;
import io.jenkins.plugins.ksm.Messages;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import io.jenkins.plugins.ksm.notation.KsmNotation;
import io.jenkins.plugins.ksm.notation.KsmTestNotation;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

class TestWrapper extends KsmBuildWrapper {

    public TestWrapper(List<KsmApplication> application, KsmNotation notation) {
        super(application, notation);
    }

    public void run(PrintStream logger) throws AbortException {
        this.consoleLogger = logger;
        getSecrets();
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public DescriptorImpl() {
            super(TestWrapper.class);
            load();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.KsmBuilder_DescriptorImpl_DisplayName();
        }
    }
}

public class KsmBuildWrapperTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @SuppressWarnings("unchecked")
    @Test
    public void testBuilder() throws Exception {

        List<KsmSecret> secrets = new ArrayList<KsmSecret>();
        secrets.add(new KsmSecret(
                "keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/login",
                KsmSecret.destinationEnvVar,
                "LOGIN",
                null));
        secrets.add(new KsmSecret(
                "keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/password",
                KsmSecret.destinationEnvVar,
                "PASSWORD",
                null));
        secrets.add(new KsmSecret(
                "keeper://A_7YpGBUgRTeDEQLhVRo0Q/field/url",
                KsmSecret.destinationFilePath,
                null,
                "url.txt"));
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
                "MYCRED",
                "PID-8adae030041e460eb00099f746e8a126",
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

        String credentialPublicId = credential.getPublicId();

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

        notation.addTestData(obj.toJSONString());

        KsmApplication application = new KsmApplication(credentialPublicId, secrets);
        List<KsmApplication> applications = new ArrayList<>();
        applications.add(application);

        FreeStyleProject project = j.createFreeStyleProject();
        TestWrapper buildWrapper = new TestWrapper(applications, notation);

        Run<?, ?> build = mock(Build.class);
        when(build.getParent()).thenReturn(null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream logger = new PrintStream(outputStream);

        buildWrapper.run(logger);

        FilePath workspace = j.jenkins.getWorkspaceFor(project);

        project.getBuildWrappersList().add(buildWrapper);
        project.getBuildersList().add(Functions.isWindows() ?
                new BatchFile("echo %LOGIN% > login.txt") :
                new Shell("echo $LOGIN > login.txt"));
        project.getBuildersList().add(Functions.isWindows() ?
                new BatchFile("echo %PASSWORD% > password.txt") :
                new Shell("echo $PASSWORD > password.txt"));
        project.getBuildersList().add(Functions.isWindows() ?
                new BatchFile("echo %LOGIN%") :
                new Shell("echo $LOGIN"));
        project.getBuildersList().add(Functions.isWindows() ?
                new BatchFile("echo %PASSWORD%") :
                new Shell("echo $PASSWORD"));
        project.getBuildersList().add(Functions.isWindows() ?
                new BatchFile("copy url.txt secret.txt"):
                new Shell("cp url.txt secret.txt"));
        project.getBuildersList().add(Functions.isWindows() ?
                new BatchFile("type secret.txt"):
                new Shell("cat secret.txt"));
        // TODO - redact binary ... if possible
        //project.getBuildersList().add(Functions.isWindows() ?
        //        new BatchFile("type my.png"):
        //        new Shell("cat my.png"));
        project.getBuildersList().add(Functions.isWindows() ?
                new BatchFile("copy my.png test.png"):
                new Shell("cp my.png test.png"));
        FreeStyleBuild buildResult = j.buildAndAssertSuccess(project);

        // Peak at the console output
        Reader r = buildResult.getLogText().readAll();
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = r.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }
        r.close();
        String targetString = buffer.toString();
        System.out.println("-----------------------");
        System.out.println(targetString);
        System.out.println("-----------------------");

        // Log from file. Allowed containing secrets. This should also handle the dollar symbol correctly.
        String log = Objects.requireNonNull(buildResult.getWorkspace()).child("login.txt").readToString().trim();
        assertEquals("My$Login", log);

        log = Objects.requireNonNull(buildResult.getWorkspace()).child("password.txt").readToString().trim();
        assertEquals("Pa$$w0rd!!", log);

        // Make sure the file, we created, with the secret doesn't exist anymore.
        assertFalse(workspace.child("url.txt").exists());
        assertFalse(workspace.child("my.png").exists());

        // Check our copy of the file to see if it contains the correct URL secret.
        assertTrue(workspace.child("secret.txt").exists());
        String secret_url = workspace.child("secret.txt").readToString();
        assertEquals("http://localhost", secret_url);

        // The console log should contain 'echo ****' where the secrets were redacted
        j.assertLogContains("echo '****'", buildResult);

        // The log should not contain our secrets.
        j.assertLogNotContains("My$Login", buildResult);
        j.assertLogNotContains("Pa$$w0rd!!", buildResult);
        j.assertLogNotContains("http://localhost", buildResult);
    }
}
