package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class NodeHelperSpec extends WorkflowScriptSpecification {
    @Subject
    def helper = new NodeHelper(ctx, libProperties)

    def 'test node allocated'() {
        given:
        def label = 'builder'
        def closure = {}

        def currentWorkspace = '/home/agent/build-dir'

        when:
        helper.node(label, closure)

        then:
        1 * ctx.node(label, _ as Closure) >> { _, cls -> cls() }

        and:
        2 * ctx.env.getProperty('WORKSPACE') >> currentWorkspace
        1 * ctx.withEnv({ list ->
            list[1] == "HOME=$currentWorkspace"
            list[2] == "GRADLE_USER_HOME=$currentWorkspace/.gradle"
        }, _ as Closure) >> { _, cls -> cls() }

        and:
        1 * libProperties.get('JENKINS_CLEAN_WORKSPACE_WHEN_SUCCESS', Boolean) >> true
        1 * libProperties.get('JENKINS_CLEAN_WORKSPACE_WHEN_ABORTED', Boolean) >> true
        1 * libProperties.get('JENKINS_CLEAN_WORKSPACE_WHEN_NOT_BUILT', Boolean) >> true
        1 * libProperties.get('JENKINS_CLEAN_WORKSPACE_WHEN_FAILURE', Boolean) >> true
        1 * libProperties.get('JENKINS_CLEAN_WORKSPACE_WHEN_UNSTABLE', Boolean) >> true
        1 * libProperties.get('JENKINS_CLEAN_WORKSPACE_NOT_FAIL_BUILD', Boolean) >> true
        1 * ctx.cleanWs({ map ->
            map.cleanWhenSuccess == true
            map.cleanWhenAborted == true
            map.cleanWhenNotBuilt == true
            map.cleanWhenFailure == true
            map.cleanWhenUnstable == true
            map.notFailBuild == true
        })
    }
}
