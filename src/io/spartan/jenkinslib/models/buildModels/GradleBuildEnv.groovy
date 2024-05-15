package io.spartan.jenkinslib.models.buildModels

import io.spartan.jenkinslib.helpers.GradleHelper
import io.spartan.jenkinslib.helpers.SonarQubeHelper

class GradleBuildEnv extends BuildJobEnv implements Serializable {
    GradleBuildEnv(Script ctx, Map<String, ?> jobConfig) {
        super(ctx, jobConfig)
    }

    @Override
    void buildStage() {
        stageScaffold('gradle') {
            def buildCommands = getJobConfig('gradleBuildCommands', List, ["app:${serviceConfigurations.name}:${pullRequestBuild ? 'assemble' : 'shadowJar'}" ])
            def buildParams = getJobConfig('gradleBuildParams', List, enableDeploy ? ['-x check -x test'] : [])

            buildCommands.each { buildCommand ->
                buildAndExecuteGradleCommand(buildCommand as String, buildParams)
            }
        }
    }

    @Override
    void codeQualityStage() {
        if (qualityStageEnabled) {
            stageScaffold('codeQuality') {
                def codeQualityCommand = getJobConfig('codeQualityCommand', String, 'sonar')
                def codeQualityParams = getJobConfig('codeQualityParams', List, [])
                def sonarQubeHelper = getHelperForClass SonarQubeHelper
                sonarQubeHelper.wrapWithSonarQubeServer {
                    buildAndExecuteGradleCommand(codeQualityCommand, codeQualityParams)
                }
            }
        }
    }

    @Override
    void testStage() {
        stageScaffold('test') {
            def testCommand = getJobConfig('testCommand', String as Class<Object>, "app:${serviceConfigurations.name}:test")
            def testParams = getJobConfig('testParams', List, [])
            buildAndExecuteGradleCommand(testCommand as String, testParams)
        }
    }

    void buildAndExecuteGradleCommand(String command, List taskParams) {
        def gradleParams = []
        if (getJobConfig('cacheEnabled', Boolean)) {
            gradleParams += ['--build-cache']
        }

        def gradleHelper = getHelperForClass GradleHelper
        gradleHelper.execute command, taskParams, gradleParams
    }
}
