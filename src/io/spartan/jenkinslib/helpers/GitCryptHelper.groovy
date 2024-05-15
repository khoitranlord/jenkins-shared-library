package io.spartan.jenkinslib.helpers
import io.spartan.jenkinslib.JenkinsLibProperties

class GitCryptHelper extends BaseHelper implements Serializable {
    private ShellHelper shellHelper

    GitCryptHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
    }

    void runGitCrypt(String gitCryptKey) {
        shellHelper.sh "echo \"$gitCryptKey\" | base64  -d > ./git-crypt-key"
        shellHelper.sh "git-crypt unlock ./git-crypt-key"
        shellHelper.sh "rm ./git-crypt-key"
    }
}
