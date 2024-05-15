package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class KanikoHelper extends BaseHelper implements Serializable {
    private ShellHelper shellHelper

    KanikoHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
    }

    void buildAndPush(String dockerFilePath, String dockerFileName, String dockerRepo, String image, String tag, Map env = [:]) {
        def dockerFilePathWithoutSlash = dockerFilePath.replaceAll(/^(.*)(\/)$/, '$1')
        def buildArg = env.collect { "--build-arg ${it.key}=${it.value}" }.join ' '
        ctx.container(name: 'kaniko', shell: '/busybox/sh') {
            ctx.withEnv(['PATH+EXTRA=/busybox']) {
                shellHelper.sh """
                #!/busybox/sh
                mkdir -p /kaniko/.docker
                cp `pwd`/.docker/config.json /kaniko/.docker/
                /kaniko/executor $buildArg --context dir://`pwd`$dockerFilePathWithoutSlash/ --dockerfile `pwd`$dockerFilePathWithoutSlash/$dockerFileName --destination $dockerRepo/$image:$tag
            """.trim().stripIndent()
            }
        }
    }
}
