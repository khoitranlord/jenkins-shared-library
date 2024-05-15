package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class CredentialsHelper extends BaseHelper implements Serializable {
    CredentialsHelper(Script ctx, JenkinsLibProperties libProperties) {
        super(ctx, libProperties)
    }

    def <T> T withCredentials(
            List<Map<String, String>> credentials,
            Closure<T> closure
    ) {
        def credentialsSetup = credentials.collect {
            if (it.type == 'usernamePassword') {
                ctx.log.info "adding credentials from id '$it.credentialsId' in variables '${it.passwordVariable ?: 'PASSWORD'}' and '${it.usernameVariable ?: 'USERNAME'}' to context"
                ctx.usernamePassword(
                    credentialsId: it.credentialsId,
                    usernameVariable: it.usernameVariable ?: 'USERNAME',
                    passwordVariable: it.passwordVariable ?: 'PASSWORD'
                )
            } else if (it.type == 'file') {
                ctx.log.info "adding file '$it.credentialsId' in variable '${it.variable}' to context"
                ctx.file(credentialsId: it.credentialsId, variable: it.variable)
            } else if (it.type == 'string') {
                ctx.log.info "adding string '$it.credentialsId' in variable '${it.variable}' to context"
                ctx.string(credentialsId: it.credentialsId, variable: it.variable)
            } else {
                ctx.log.warn "unsupported credentials of type '$it.type'"
                [:]
            }
        }.findAll { it }
        ctx.withCredentials(credentialsSetup, closure)
    }
}
