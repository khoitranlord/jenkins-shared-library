package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

abstract class BaseHelper implements Serializable {
    public Script ctx

    JenkinsLibProperties libProperties

    BaseHelper(Script ctx, JenkinsLibProperties libProperties) {
        this.ctx = ctx
        this.libProperties = libProperties
    }

    def <T> T getConfig(String key, Class<T> type) {
        libProperties.get key, type
    }
}
