package io.jenkins.plugins.ksm;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.ksm.credential.KsmCredential;
import java.util.Collections;
import java.util.List;

public class KsmCommon {

    public static final String errorPrefix = "Keeper Secrets Manager ERROR: ";

    public static ListBoxModel buildCredentialsIdListBox(ItemGroup<?> context) {

        final ListBoxModel items = new ListBoxModel();

        // Using ACL.SYSTEM, the cred stuff doesn't support ACL.SYSTEM2 yet.
        // TODO: Switch to ACL.SYSTEM2 when CredentialsProvider supports it.
        List<KsmCredential> ksmCredentials = CredentialsProvider.lookupCredentials(
                KsmCredential.class,
                context,
                ACL.SYSTEM,
                Collections.emptyList()
        );
        for (KsmCredential item : ksmCredentials) {
            items.add(item.getDescription(), item.getId());
        }
        return items;
    }
}
