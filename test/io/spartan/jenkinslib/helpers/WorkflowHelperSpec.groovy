package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class WorkflowHelperSpec extends WorkflowScriptSpecification {
    @Subject
    def helper = new WorkflowHelper(ctx, libProperties)

    def 'test conditionalStage'() {
        given:
        def stageName = 'stage-name'
        def closure = {}

        when:
        helper.conditionalStage(stageName, condition, closure)

        then:
        1 * ctx.stage(stageName, _ as Closure) >> { _, cls -> cls() }

        and:
        expectedCallSkip * ctx.markStageSkipped(stageName)

        where:
        condition || expectedCallSkip
        true      || 0
        false     || 1
    }
}
