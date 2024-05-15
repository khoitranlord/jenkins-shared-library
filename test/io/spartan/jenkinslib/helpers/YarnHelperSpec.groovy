package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class YarnHelperSpec extends WorkflowScriptSpecification {
    def shellHelper = Mock ShellHelper

    @Subject
    def helper = new YarnHelper(ctx, libProperties, shellHelper)

    def 'test execute'() {
        given:
        def task = 'install'
        def env = [CI: 'false']

        when:
        helper.execute(task, [], env, directory)

        then:
        expectedSwitchDir * ctx.dir(directory, _ as Closure) >> { _, cls -> cls() }

        and:
        1 * shellHelper.sh("CI=false yarn $task ")

        where:
        directory || expectedSwitchDir
        'web-dir' || 1
        null      || 0
    }
}
