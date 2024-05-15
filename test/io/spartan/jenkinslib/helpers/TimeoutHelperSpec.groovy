package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class TimeoutHelperSpec extends WorkflowScriptSpecification {
    @Subject
    def helper = new TimeoutHelper(ctx, libProperties)

    def 'test withTimeout method'() {
        given:
        def closure = {}

        when:
        helper.withTimeout(timeout, closure)

        then:
        expectedCallTimeout * ctx.timeout([time: timeout, unit: 'MINUTES'], closure)

        where:
        timeout || expectedCallTimeout
        5       || 1
        0       || 0
    }
}
