package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class ShellHelperSpec extends WorkflowScriptSpecification {
    @Subject
    def helper = new ShellHelper(ctx, libProperties)

    def 'test sh'() {
        given:
        def script = './gradlew clean build'

        when:
        helper.sh(script)

        then:
        1 * ctx.log.info("running script '$script'")
        1 * ctx.sh(script: script)
    }

    def 'test shForStdout'() {
        given:
        def scriptWithSpace = './gradlew clean build'

        when:
        def result = helper.shForStdout(scriptWithSpace)

        then:
        1 * ctx.sh(script: scriptWithSpace, returnStdout: true) >> scriptWithSpace

        and:
        result == scriptWithSpace.trim()
    }

    def 'test shForStatus'() {
        given:
        def script = './gradlew clean build'
        def successStatus = '0'

        when:
        def result = helper.shForStatus(script)

        then:
        1 * ctx.sh(script: script, returnStatus: true) >> successStatus

        and:
        result == successStatus.toInteger()
    }
}
