package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class SlackHelper extends BaseHelper implements Serializable {
    SlackHelper(Script ctx, JenkinsLibProperties libProperties) {
        super(ctx, libProperties)
    }

    void sendResultToChannel(String channel, Map<String, String> variables) {
        def messageTemplate = ctx.libraryResource('message-template/slack_message_template') as String
        def replacedMessage = variables.inject(messageTemplate) { content, var ->
            content.replace("\${$var.key}", "${var.value}")
        }
        def color = '#FFFFFF' // white
        def buildStatus = variables.'BUILD_STATUS'
        if (buildStatus in [null, 'SUCCESS']) {
            replacedMessage = ":checked: $replacedMessage"
            color = 'good'
        } else {
            replacedMessage = ":x: $replacedMessage"
            color = 'danger'
        }
        ctx.log.info "Sending message $replacedMessage"
        ctx.slackSend color: color, channel: channel, message: replacedMessage
    }
}
