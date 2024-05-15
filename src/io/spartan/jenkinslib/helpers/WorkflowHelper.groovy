package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class WorkflowHelper extends BaseHelper implements Serializable {

    WorkflowHelper(Script ctx, JenkinsLibProperties libProperties) {
        super(ctx, libProperties)
    }

    def <T> T conditionalStage(String stageName, boolean condition, Closure<T> closure) {
        ctx.stage stageName, {
            if (condition) {
                closure()
            } else {
                ctx.log.info "skipping stage '$stageName'"
                ctx.markStageSkipped(stageName)
                null
            }
        }
    }
}
