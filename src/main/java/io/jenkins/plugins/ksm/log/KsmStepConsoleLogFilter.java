package io.jenkins.plugins.ksm.log;

import hudson.console.ConsoleLogFilter;
import hudson.console.LineTransformationOutputStream;
import hudson.model.Run;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KsmStepConsoleLogFilter extends ConsoleLogFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String charsetName;
    private final List<String> secretList;

    public KsmStepConsoleLogFilter(final String charsetName, List<String> secretList) {
        this.charsetName = charsetName;

        // Sort the secrets, longest to shortest
        if (secretList != null) {
            secretList.sort((s1, s2) -> s2.length() - s1.length());
        }
        this.secretList = secretList;
    }

    @Override
    public OutputStream decorateLogger(Run run, final OutputStream logger) {
        return new LineTransformationOutputStream() {

            @Override
            protected void eol(byte[] b, int len) throws IOException {
                String regexPattern = getSecretRegexPattern();

                // If no secrets, don't worry about the match, just write and return.
                if(regexPattern.equals("")) {
                    logger.write(b, 0, len);
                    return;
                }

                Pattern p = Pattern.compile(regexPattern);
                Matcher m = p.matcher(new String(b, 0, len, charsetName));
                if (m.find()) {
                    logger.write(m.replaceAll("****").getBytes(charsetName));
                } else {
                    logger.write(b, 0, len);
                }
            }
        };
    }

    /**
     * Make an "or" regular express of descending secret lengths. This is so we redact the full secret and
     * not leave particles. ie PASS vs PASSWORD. If PASS was redacted before PASSWORD, it would leave WORD
     * in the console log.
     *
     * @return regular expression "or" string
     */

    public String getSecretRegexPattern() {
        if((secretList == null) || (secretList.isEmpty())) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for(String s: secretList) {
            // The debate! Is a value of secrets really a secret? If the value is " " replacing all the single
            // spaces would make the console log worthless. We are going to consider a secret of all spaces as
            // not a secret.
            if (s.trim().equals("")) {
                continue;
            }

            if(sb.length() > 0) {
                sb.append("|");
            }
            // Quote the secret value since it might contain regex syntax. ie \QSECRET\E
            sb.append(Pattern.quote(s));
        }
        return sb.toString();
    }
}
