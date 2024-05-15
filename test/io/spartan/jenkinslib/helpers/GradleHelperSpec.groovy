package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class GradleHelperSpec extends WorkflowScriptSpecification {
    def shellHelper = Mock ShellHelper

    @Subject
    def helper = new GradleHelper(ctx, libProperties, shellHelper)

    def 'test execute method'() {
        given:
        def gradleCommand = 'build'
        def gradleTaskParams = ['-x check']
        def gradleParams = ['-Dorg.gradle.workers.max="8"']

        when:
        helper.execute(gradleCommand, gradleTaskParams, gradleParams)

        then:
        1 * shellHelper.sh('./gradlew -Dorg.gradle.workers.max="8" build -x check')
    }
}
