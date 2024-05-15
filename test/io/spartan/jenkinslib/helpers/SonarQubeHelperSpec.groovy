package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class SonarQubeHelperSpec extends WorkflowScriptSpecification {
    @Subject
    def helper = new SonarQubeHelper(ctx, libProperties)

    def 'test wrap with sonarqube server'() {
        given:
        def closure = {}

        when:
        helper.wrapWithSonarQubeServer closure

        then:
        1 * libProperties.get('SONARQUBE_SERVER_ID', String) >> 'SonarQube Server ID'
        1 * ctx.withSonarQubeEnv('SonarQube Server ID', _ as Closure)
    }
}
