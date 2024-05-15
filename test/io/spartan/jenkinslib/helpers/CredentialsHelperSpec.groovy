package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class CredentialsHelperSpec extends WorkflowScriptSpecification {
    @Subject
    def helper = new CredentialsHelper(ctx, libProperties)

    def 'test withCredentials method with type usernamePassword and not override variable'() {
        given:
        def credentialsId = 'username-password-credentials'
        def credentialsList = [
                [type: 'usernamePassword', credentialsId: credentialsId]
        ]
        def closure = {}

        when:
        helper.withCredentials(credentialsList, closure)

        then:
        1 * ctx.usernamePassword({ map ->
            map.credentialsId == credentialsId
            map.usernameVariable == 'USERNAME'
            map.passwordVariable == 'PASSWORD'
        }) >> [('$class'): 'UsernamePasswordMultiBinding']

        and:
        1 * ctx.withCredentials({ list ->
            list[0].'$class' == 'UsernamePasswordMultiBinding'
        }, closure)
    }

    def 'test withCredentials method with type usernamePassword and override variable'() {
        given:
        def credentialsId = 'username-password-credentials'
        def credentialsList = [
                [
                        type            : 'usernamePassword',
                        credentialsId   : credentialsId,
                        usernameVariable: 'CUSTOM_USERNAME_VAR',
                        passwordVariable: 'CUSTOM_PASSWORD_VAR'
                ]
        ]
        def closure = {}

        when:
        helper.withCredentials(credentialsList, closure)

        then:
        1 * ctx.usernamePassword({ map ->
            map.credentialsId == credentialsId
            map.usernameVariable == 'CUSTOM_USERNAME_VAR'
            map.passwordVariable == 'CUSTOM_PASSWORD_VAR'
        }) >> [('$class'): 'UsernamePasswordMultiBinding']

        and:
        1 * ctx.withCredentials({ list ->
            list[0].'$class' == 'UsernamePasswordMultiBinding'
        }, closure)
    }

    def 'test withCredentials with multiple type'() {
        given:
        def usernamePasswordCredentialsId = 'username-password-credentials'
        def fileCredentialsId = 'file-credentials'
        def stringCredentialsId = 'string-credentials'
        def credentialsList = [
                [type: 'usernamePassword', credentialsId: usernamePasswordCredentialsId],
                [type: 'file', credentialsId: fileCredentialsId, variable: 'FILE_VAR'],
                [type: 'string', credentialsId: stringCredentialsId, variable: 'STRING_VAR']
        ]
        def closure = {}

        when:
        helper.withCredentials(credentialsList, closure)

        then:
        1 * ctx.usernamePassword({ map ->
            map.credentialsId == usernamePasswordCredentialsId
            map.usernameVariable == 'USERNAME'
            map.passwordVariable == 'PASSWORD'
        }) >> [('$class'): 'UsernamePasswordMultiBinding']

        1 * ctx.file({ map ->
            map.credentialsId == fileCredentialsId
            map.variable == 'FILE_VAR'
        }) >> [('$class'): 'FileBinding']

        1 * ctx.string({ map ->
            map.credentialsId == stringCredentialsId
            map.variable == 'STRING_VAR'
        }) >> [('$class'): 'StringBinding']


        and:
        1 * ctx.withCredentials({ list ->
            list[0].'$class' == 'UsernamePasswordMultiBinding'
            list[1].'$class' == 'FileBinding'
            list[2].'$class' == 'StringBinding'
        }, closure)
    }
}
