package io.jenkins.plugins.ksm;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.ksm.creds.KsmCredentials;

import java.util.Collections;
import java.util.List;

public class KsmCommon {

    public static ListBoxModel buildCredentialsIdListBox(ItemGroup context) {

        final ListBoxModel items = new ListBoxModel();

        List<KsmCredentials> ksmCredentials = CredentialsProvider.lookupCredentials(
                KsmCredentials.class,
                context,
                ACL.SYSTEM,
                Collections.emptyList()
        );
        for (KsmCredentials item : ksmCredentials) {
            items.add(item.getDescription(), item.getId());
        }
        return items;
    }
}
