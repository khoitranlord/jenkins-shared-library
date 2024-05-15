package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class KanikoHelperSpec extends WorkflowScriptSpecification {
    def shellHelper = Mock ShellHelper

    @Subject
    def helper = new KanikoHelper(ctx, libProperties, shellHelper)

    def 'test build and push docker image'() {
        given:
        def dockerFilePath = '/'
        def dockerFileName = 'Dockerfile'
        def dockerRepo = 'image.registry.test'
        def dockerImage = 'test-image'
        def dockerTag = 'latest'

        when:
        helper.buildAndPush(dockerFilePath, dockerFileName, dockerRepo, dockerImage, dockerTag)

        then:
        1 * ctx.container([name: 'kaniko', shell: '/busybox/sh'], _ as Closure) >> { _, cls -> cls() }

        and:
        1 * ctx.withEnv(['PATH+EXTRA=/busybox'], _ as Closure) >> { _, cls -> cls() }

        and:
        1 * shellHelper.sh(_ as String) >> { script ->
            def scriptAsString = script[0] as String
            def ele = scriptAsString.split('\n')
            assert ele.size() == 4
            assert ele[1].trim() ==~ /mkdir -p \/kaniko\/\.docker/
            assert ele[2].trim() ==~ /cp `pwd`\/\.docker\/config\.json \/kaniko\/\.docker\//
            assert ele[3].trim() ==~ /\/kaniko\/executor  --context dir:\/\/`pwd`\/ --dockerfile `pwd`\/$dockerFileName --destination $dockerRepo\/$dockerImage:$dockerTag/
        }

    }
}
