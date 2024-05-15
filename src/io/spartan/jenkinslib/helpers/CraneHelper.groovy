package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class CraneHelper extends BaseHelper implements Serializable {
    private ShellHelper shellHelper
    private FileHelper fileHelper

    CraneHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper, FileHelper fileHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
        this.fileHelper = fileHelper
    }

    void login(String registry, String username, String password) {
        def passwordFileName = 'password'
        try {
            fileHelper.writeFile(passwordFileName, password)
            shellHelper.sh "cat $passwordFileName | crane auth login ${registry} -u $username --password-stdin"
        } finally {
            if (ctx.fileExists(passwordFileName)) {
                shellHelper.sh "rm $passwordFileName"
            }
        }
    }

    void pull(String repoUrn, String imageName, String imageTag) {
        shellHelper.sh "crane pull $repoUrn/$imageName:$imageTag $imageName"
    }

    void push(String repoUrn, String imageName, String imageTag) {
        shellHelper.sh "crane push $imageName $repoUrn/$imageName:$imageTag"
    }

    void copy(String imageName, String imageTag, String srcRepoUrn, String destRepoUrn) {
        shellHelper.sh "crane cp $srcRepoUrn/$imageName:$imageTag $destRepoUrn/$imageName:$imageTag"
    }
}
