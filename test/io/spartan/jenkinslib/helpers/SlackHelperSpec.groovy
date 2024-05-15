package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject
import spock.lang.Unroll

class SlackHelperSpec extends WorkflowScriptSpecification {
    @Subject
    def helper = new SlackHelper(ctx, libProperties)

    @Unroll
    def 'test send slack message to channel when build status is #buildStatus'() {
        given:
        def channelName = '#channel'
        def variable = [
                REPLACE     : 'VALUE',
                BUILD_STATUS: buildStatus
        ]

        when:
        helper.sendResultToChannel(channelName, variable)

        then:
        1 * ctx.libraryResource(_ as String) >> 'Message replaces with ${REPLACE}'

        and:
        1 * ctx.slackSend([
                color  : color,
                channel: channelName,
                message: "$statusMessage Message replaces with VALUE"
        ])

        where:
        buildStatus || color    | statusMessage
        'SUCCESS'   || 'good'   | ':checked:'
        'FAILED'    || 'danger' | ':x:'
    }
}
