package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

import java.time.Instant
import java.time.temporal.ChronoUnit

class NodeHelper extends BaseHelper implements Serializable {

    NodeHelper(Script ctx, JenkinsLibProperties libProperties) {
        super(ctx, libProperties)
    }

    def <T> T node(String label, Closure<T> closure) {
        def nodeEnvVariables = []
        def currentBuildTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        nodeEnvVariables.add "BUILD_TIME=${currentBuildTime}"

        ctx.log.info "allocating node with label $label and activate timestamp"
        ctx.node label, {
            nodeEnvVariables.add "HOME=${ctx.env.'WORKSPACE'}"
            nodeEnvVariables.add "GRADLE_USER_HOME=${ctx.env.'WORKSPACE'}/.gradle"
            ctx.withEnv(nodeEnvVariables) {
                def result = closure()
                ctx.cleanWs(
                        cleanWhenSuccess: getConfig('JENKINS_CLEAN_WORKSPACE_WHEN_SUCCESS', Boolean),
                        cleanWhenAborted: getConfig('JENKINS_CLEAN_WORKSPACE_WHEN_ABORTED', Boolean),
                        cleanWhenNotBuilt: getConfig('JENKINS_CLEAN_WORKSPACE_WHEN_NOT_BUILT', Boolean),
                        cleanWhenFailure: getConfig('JENKINS_CLEAN_WORKSPACE_WHEN_FAILURE', Boolean),
                        cleanWhenUnstable: getConfig('JENKINS_CLEAN_WORKSPACE_WHEN_UNSTABLE', Boolean),
                        notFailBuild: getConfig('JENKINS_CLEAN_WORKSPACE_NOT_FAIL_BUILD', Boolean)
                )

                result
            }
        }
    }
}
