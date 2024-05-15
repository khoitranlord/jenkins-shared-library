package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class PodTemplateHelper extends BaseHelper implements Serializable {
    PodTemplateHelper(Script ctx, JenkinsLibProperties libProperties) {
        super(ctx, libProperties)
    }

    private List getAdditionalContainers(Map<String, Map> containerConfiguration) {
        containerConfiguration.collect {config ->
            def defaultContainerEnv = getConfig("${config.key.toUpperCase()}_DEFAULT_ENV", List).collectEntries {
                def pair = (it as String).split('=')
                [(pair.first()): pair.last()]
            }
            ctx.containerTemplate(
                    name: config.key,
                    image: getConfig("${config.key.toUpperCase()}_IMAGE", String),
                    command: getConfig("${config.key.toUpperCase()}_COMMAND", String),
                    args: getConfig("${config.key.toUpperCase()}_ARGS", String),
                    resourceRequestCpu: getConfig("${config.key.toUpperCase()}_CPU", String),
                    resourceRequestMemory: getConfig("${config.key.toUpperCase()}_MEMORY", String),
                    resourceRequestEphemeralStorage: getConfig("${config.key.toUpperCase()}_STORAGE", String),
                    resourceLimitCpu: getConfig("${config.key.toUpperCase()}_CPU", String),
                    resourceLimitMemory: getConfig("${config.key.toUpperCase()}_MEMORY", String),
                    resourceLimitEphemeralStorage: getConfig("${config.key.toUpperCase()}_STORAGE", String),
                    envVars: [] + (defaultContainerEnv + config.value).collect { env -> ctx.envVar(key: env.key, value: env.value) }
            )
        }
    }

    def <T> T podTemplate(String inheritAgentLabel, Map additionalContainerConfig, Closure<T> closure) {
        ctx.log.info "Template build node inherited from ${inheritAgentLabel}"

        ctx.podTemplate(
                containers: getAdditionalContainers(additionalContainerConfig),
                inheritFrom: inheritAgentLabel ?: 'builder'
        ) {
            def result = closure()

            result
        }
    }
}
