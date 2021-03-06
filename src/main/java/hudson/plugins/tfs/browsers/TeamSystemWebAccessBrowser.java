package hudson.plugins.tfs.browsers;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.plugins.tfs.TeamFoundationServerScm;
import hudson.plugins.tfs.model.ChangeSet;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class TeamSystemWebAccessBrowser extends TeamFoundationServerRepositoryBrowser {

    private static final long serialVersionUID = 1L;

    private final String url;

    @DataBoundConstructor
    public TeamSystemWebAccessBrowser(String urlExample) {
        this.url = Util.fixEmpty(urlExample);
    }

    public String getUrl() {
        return url;
    }

    private String getServerConfiguration(ChangeSet changeset) {
        AbstractProject<?, ?> project = changeset.getParent().build.getProject();
        SCM scm = project.getScm();
        if (scm instanceof TeamFoundationServerScm) {
            return ((TeamFoundationServerScm) scm).getServerUrl(changeset.getParent().build);
        } else {
            throw new IllegalStateException("TFS repository browser used on a non TFS SCM");
        }
    }

    private String getBaseUrlString(ChangeSet changeSet) throws MalformedURLException {
        String baseUrl;
        if (url != null) {
            baseUrl = DescriptorImpl.getBaseUrl(url);
        } else {
            baseUrl = String.format("%s/", getServerConfiguration(changeSet));
        }
        return baseUrl;
    }

    /**
     * http://tswaserver:8090/cs.aspx?cs=99
     */
    @Override
    public URL getChangeSetLink(ChangeSet changeSet) throws IOException {
//        return new URL(String.format("%scs.aspx?cs=%s", getBaseUrlString(changeSet), changeSet.getVersion()));
        return new URL(String.format("%s_versionControl/changeset?id=%s", getBaseUrlString(changeSet), changeSet.getVersion()));
    }

    /**
     * http://tswaserver:8090/view.aspx?path=$/Project/Folder/file.cs&cs=99
     * http://tfs.seek.int:8080/tfs/SEEK/Online/_VersionControl#path=%24%2FOnline%2FTrunk%2FAutomation%2FWebdriver%2FSEEK.Search.API%2FApp.config&_a=contents
     *
     * @param item
     * @return
     */
    public URL getFileLink(ChangeSet.Item item) throws IOException {
        return new URL(String.format("%s_VersionControl#path=%s&_a=contents", getBaseUrlString(item.getParent()), item.getPath()));
    }

    /**
     * http://tswaserver:8090/diff.aspx?opath=$/Project/Folder/file.cs&ocs=99&mpath=$/Project/Folder/file.cs&mcs=98
     * _VersionControl#path=%24%2FOnline%2FTrunk%2FAutomation%2FWebdriver%2FAutomationSCM.msbuild&_a=compare
     *
     * @param item
     * @return
     * @throws IOException
     */
    public URL getDiffLink(ChangeSet.Item item) throws IOException {
        ChangeSet parent = item.getParent();
        if (item.getEditType() != EditType.EDIT) {
            return null;
        }
        try {
            return new URL(
                    String.format("$s_VersionControl#path=%s&_a=compare",
                            getBaseUrlString(parent),
                            item.getPath()
                    ));
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private String getPreviousChangeSetVersion(ChangeSet changeset) throws NumberFormatException {
        return Integer.toString(Integer.parseInt(changeset.getVersion()) - 1);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {

        public DescriptorImpl() {
            super(TeamSystemWebAccessBrowser.class);
        }

        @Override
        public String getDisplayName() {
            return "Team System Web Access";
        }

        public static String getBaseUrl(String urlExample) throws MalformedURLException {
            URL url = new URL(urlExample);
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), String.format("/%s", FilenameUtils.getPath(url.getPath()))).toString();
        }
    }
}
