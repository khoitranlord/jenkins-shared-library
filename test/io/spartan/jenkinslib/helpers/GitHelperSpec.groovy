package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class GitHelperSpec extends WorkflowScriptSpecification {
    def shellHelper =  Mock ShellHelper

    @Subject
    def helper = new GitHelper(ctx, libProperties, shellHelper)

    def 'test parseShortRev'() {
        when:
        helper.parseShortRev()

        then:
        1 * libProperties.get('GIT_SHORT_REV_LENGTH', Integer) >> 8
        1 * shellHelper.shForStdout('git rev-parse --short=8 HEAD')
    }
}
