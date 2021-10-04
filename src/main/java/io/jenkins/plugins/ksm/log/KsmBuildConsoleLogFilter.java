package io.jenkins.plugins.ksm.log;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


public class KsmBuildConsoleLogFilter {

    public static class MaskingOutputStream extends LineTransformationOutputStream.Delegating {

        private final @NonNull List<String> secretValues;
        private final @NonNull String charsetName;
        private final KsmLogCommon logCommon;

        protected MaskingOutputStream(@NonNull OutputStream out, @NonNull List<String> secretValues, @NonNull String charsetName) {
            super(out);
            this.secretValues = secretValues;
            this.charsetName = charsetName;
            this.logCommon = new KsmLogCommon(secretValues, charsetName);
        }

        @Override
        protected void eol(byte[] b, int len) throws IOException {
            logCommon.eol(out, b, len);
        }
    }
}
