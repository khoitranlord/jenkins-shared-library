package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject


class AWSHelperSpec extends WorkflowScriptSpecification {
    def credentialsHelper = Mock CredentialsHelper
    def shellHelper = Mock ShellHelper

    @Subject
    def helper = new AWSHelper(ctx, libProperties, credentialsHelper, shellHelper)

    def "test connect method with valid credentials"() {
        given:
        def environment = "dev"
        def awsAccountId = "012345678910"
        def oidcRole = "arn:aws:iam::${awsAccountId}:role/testRole"

        when:
        helper.connect(ctx, environment)

        then:
        1 * credentialsHelper.withCredentials(_ as List, _ as Closure) >> { list, cls ->
            assert list == [
                    [type: 'string', credentialsId: "aws-account-id-${environment}", variable: 'awsAccountId'],
                    [type: 'file', credentialsId: "aws-web-identity-token-file", variable: 'awsWebIdentityTokenFile']
            ]
            cls()
        }
        1 * credentialsHelper.withCredentials(_ as List, _ as Closure) >> { list, cls ->
            assert list == [
                [type: 'string', credentialsId: "jenkins-oidc-name", variable: 'jenkinsOidcName'],
            ]
            cls()
        }

        1 * ctx.env.getProperty('awsAccountId')
        1 * ctx.env.getProperty('jenkinsOidcName')
    }
}

