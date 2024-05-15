package io.spartan.jenkinslib

class JenkinsLibProperties {
    Script ctx
    private Map<String, ?> libProperties
    private Map<String, ?> extraLibProperties

    JenkinsLibProperties(ctx, Map<String, ?> extraLibProperties = [:]) {
        this.ctx = ctx
        this.libProperties = [:]
        this.extraLibProperties = extraLibProperties
    }

    def <T> T get(String key, Class<T> type, T fallbackValue = null) {
        if (libProperties.isEmpty()) {
            lazyReadProperties()
        }

        if (!libProperties.containsKey(key)) {
            if (fallbackValue) {
                return fallbackValue
            } else {
                ctx.error "no property '$key' available"
            }
        }

        def value = libProperties."$key"
        if (type in Boolean) {
            Boolean.parseBoolean(value as String) as T
        } else if (type in Integer) {
            Integer.parseInt(value as String) as T
        } else if (type in List) {
            if (value instanceof List) {
                value as T
            } else if (value instanceof String) {
                (value as String).split(',').collect { it.trim() }.findAll { it } as T
            } else {
                ctx.error "could not return value of key '$key' of type '${value.type}' as list"
            }
        } else {
            type.cast(value)
        }
    }

    private void lazyReadProperties() {
        this.libProperties = ['default']
                .collect {
                    ctx.readProperties(text: ctx.libraryResource("config/jenkinslib-${it}.properties")) as Map<String, ?>
                } // read all property files for given profiles
                .findAll { it } // remove all nulls and empties
                .inject([:] as Map<String, ?>) { a, b -> a + b } // reduce to single map

        this.extraLibProperties.each {
            if (it.value == this.libProperties[it.key]) {
                ctx.log.warn "you're overwriting the value of '$it.key' with the default value '$it.value'"
            }
            this.libProperties[it.key] = it.value
        }
    }
}
