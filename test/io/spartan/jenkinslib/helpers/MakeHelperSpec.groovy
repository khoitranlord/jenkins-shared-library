package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class MakeHelperSpec extends WorkflowScriptSpecification {
    def shellHelper = Mock ShellHelper

    @Subject
    def helper = new MakeHelper(ctx, libProperties, shellHelper)

    def 'test execute'() {
        given:
        def task = 'build'
        def env = [FOO: 'foo']

        when:
        helper.execute(task, [], env)

        then:
        1 * shellHelper.sh("FOO=foo make $task ")
    }

}
