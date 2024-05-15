package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class GitCryptHelperSpec extends WorkflowScriptSpecification {
    def shellHelper = Mock ShellHelper

    @Subject
    def helper = new GitCryptHelper(ctx, libProperties, shellHelper)

    def 'test execute'() {
        given:
        def gitCryptKey = 'ThisIsASampleGitCryptKey'

        when:
        helper.runGitCrypt(gitCryptKey)

        then:
        1 * shellHelper.sh("echo \"$gitCryptKey\" | base64  -d > ./git-crypt-key")
        1 * shellHelper.sh("git-crypt unlock ./git-crypt-key")
        1 * shellHelper.sh("rm ./git-crypt-key")
    }

}
