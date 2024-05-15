package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class GitHelper extends BaseHelper implements Serializable {
    private ShellHelper shellHelper

    GitHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
    }

    String parseShortRev(String refSpec = 'HEAD') {
        def gitShortRev = getConfig('GIT_SHORT_REV_LENGTH', Integer)
        shellHelper.shForStdout "git rev-parse --short=$gitShortRev $refSpec"
    }

    String getLatestCommitFromReleaseBranch(int indexReleaseBranch) {
        def releaseCommitPrefix = getConfig('RELEASE_COMMIT_PREFIX', String)
        shellHelper.shForStdout "git log --grep='^$releaseCommitPrefix' --skip=$indexReleaseBranch -1 --format='%H'"
    }

    void authenticate(String userName, String password, String repository, String githubOrg) {
        shellHelper.sh """
          git config --global credential.helper store
          git ls-remote https://${userName}:${password}@github.com/${githubOrg}/${repository}.git
        """.trim().stripIndent()
    }

    boolean checkForFileChange(String path, String sourceRef, String targetRef) {
        def changedFiles = getChangedFiles(sourceRef, targetRef)
        return changedFiles.contains(path)
    }

    def getChangedFiles(String sourceCommitHash, String targetCommitHash) {
        return shellHelper.shForStdout("git diff --name-only $sourceCommitHash $targetCommitHash")
    }
}
