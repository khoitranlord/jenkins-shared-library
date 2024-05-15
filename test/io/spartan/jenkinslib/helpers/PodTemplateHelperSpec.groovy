package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class PodTemplateHelperSpec extends WorkflowScriptSpecification {
    @Subject
    def helper = new PodTemplateHelper(ctx, libProperties)

    def 'test pod template method with postgres'() {
        given:
        def agentLabel = 'builder-custom'
        def additionalContainerConfig = ['postgres': [POSTGRES_DB: 'custom']]
        def closure = {}

        when:
        helper.podTemplate(agentLabel, additionalContainerConfig, closure)

        then:
        1 * libProperties.get('POSTGRES_DEFAULT_ENV', List) >> ['POSTGRES_DB=local','POSTGRES_USER=local','POSTGRES_PASSWORD=local']
        1 * libProperties.get('POSTGRES_IMAGE', String) >> 'postgis/postgis:14-3.3'
        1 * libProperties.get('POSTGRES_ARGS', String) >> 'postgres -N 500'
        1 * libProperties.get('POSTGRES_COMMAND', String) >> ''
        2 * libProperties.get('POSTGRES_CPU', String) >> '500m'
        2 * libProperties.get('POSTGRES_MEMORY', String) >> '1Gi'
        2 * libProperties.get('POSTGRES_STORAGE', String) >> ''

        1 * ctx.envVar([key: 'POSTGRES_DB', value: 'custom'])
        1 * ctx.envVar([key: 'POSTGRES_USER', value: 'local'])
        1 * ctx.envVar([key: 'POSTGRES_PASSWORD', value: 'local'])

        1 * ctx.containerTemplate({ map ->
            map.name == 'postgres'
            map.image == 'postgis/postgis:14-3.3'
            map.command == ''
            map.args == 'postgres -N 500'
            map.resourceRequestCpu == '500m'
            map.resourceRequestMemory == '1Gi'
            map.resourceRequestEphemeralStorage == ''
            map.resourceLimitCpu == '500m'
            map.resourceLimitMemory == '1Gi'
            map.resourceLimitEphemeralStorage == ''
        })

        and:
        1 * ctx.podTemplate({ map ->
            map.inheritFrom == agentLabel
        }, _ as Closure) >> { _, cls -> cls() }
    }
}
