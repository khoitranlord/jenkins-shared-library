package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class TimeoutHelper extends BaseHelper implements Serializable {

    TimeoutHelper(Script ctx, JenkinsLibProperties libProperties) {
        super(ctx, libProperties)
    }

    def <T> T withTimeout(int timeout, Closure<T> closure) {
        if (timeout && timeout > 0) {
            def timeoutConfig = [time: timeout, unit: 'MINUTES']
            ctx.timeout timeoutConfig, closure
        } else {
            closure.call()
        }
    }
}
