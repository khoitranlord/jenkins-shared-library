package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class ShellHelper extends BaseHelper implements Serializable {
    ShellHelper(Script ctx, JenkinsLibProperties libProperties) {
        super(ctx, libProperties)
    }

    void sh(String script) {
        script = script.trim()
        ctx.log.info "running script '$script'"
        ctx.sh(script: script.trim())
    }

    String shForStdout(String script) {
        script = script.trim()
        ctx.log.info "running script '$script' and returning trimmed stdout"
        ctx.sh(script: script, returnStdout: true).trim()
    }

    Integer shForStatus(String script) {
        script = script.trim()
        ctx.log.info "running script '$script' and returning status code"
        ctx.sh(script: script, returnStatus: true)?.toInteger()
    }
}
