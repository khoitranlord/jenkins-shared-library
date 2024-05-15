package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class DatadogHelperSpec extends WorkflowScriptSpecification {
    def credentialsHelper = Mock CredentialsHelper

    @Subject
    def helper = new DatadogHelper(ctx, libProperties, credentialsHelper)

    def 'test get web config'() {
        given:
        def serviceName = 'service-name'

        when:
        helper.getWebConfig(ctx, serviceName)

        then:
            1 * credentialsHelper.withCredentials([
                [type: 'string', credentialsId: "$serviceName-dd-application-id", variable: 'ddApplicationId'],
                [type: 'string', credentialsId: "$serviceName-dd-client-token", variable: 'ddClientToken'],
                [type: 'string', credentialsId: "$serviceName-dd-service", variable: 'ddService'],
            ], _ as Closure) >> { _, cls -> cls() }
    }
}
