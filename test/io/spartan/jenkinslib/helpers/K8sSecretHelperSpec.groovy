package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class K8sSecretHelperSpec extends WorkflowScriptSpecification {
    def shellHelper = Mock ShellHelper

    @Subject
    def helper = new K8sSecretHelper(ctx, libProperties, shellHelper)

    def 'test read secret from cluster'() {
        given:
        def namespace = 'namespace'
        def secretName = 'secret-name'

        when:
        helper.readSecretFromCluster(namespace, secretName)

        then:
        1 * shellHelper.shForStdout("kubectl -n $namespace get secret $secretName -oyaml") >> 'secret-content-as-yaml'

        and:
        1 * ctx.readYaml({ map ->
            map.text == 'secret-content-as-yaml'
        })
    }
}
