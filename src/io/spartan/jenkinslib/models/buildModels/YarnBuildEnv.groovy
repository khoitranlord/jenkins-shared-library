package io.spartan.jenkinslib.models.buildModels

import io.spartan.jenkinslib.helpers.AWSHelper
import io.spartan.jenkinslib.helpers.DatadogHelper
import io.spartan.jenkinslib.helpers.DeployVarsHelper
import io.spartan.jenkinslib.helpers.EnvFileHelper
import io.spartan.jenkinslib.helpers.ShellHelper
import io.spartan.jenkinslib.helpers.SonarQubeHelper
import io.spartan.jenkinslib.helpers.YarnHelper

class YarnBuildEnv extends BuildJobEnv implements Serializable {
    YarnBuildEnv(Script ctx, Map<String, ?> jobConfig) {
        super(ctx, jobConfig)
    }

    @Override
    void buildStage() {
        stageScaffold('yarn') {
            def buildCommands = getJobConfig('yarnBuildCommands', List, [])
            def buildParams = getJobConfig('yarnBuildParams', List, [])
            def buildEnv = getJobConfig('yarnBuildEnv', Map, [:])
            def defaultEnv = [
                    CI: pullRequestBuild
            ]
            def allBuildEnv = defaultEnv + buildEnv
            if (!pullRequestBuild) {

                def deployVarsHelper = getHelperForClass DeployVarsHelper
                // replace env files
                def variables = allBuildEnv.collect { "$it.key=$it.value" }

                def awsHelper = getHelperForClass AWSHelper
                awsHelper.connect(ctx, environment)

                variables += deployVarsHelper.getServiceDeployVars(deployVarsMode, serviceConfigurations, environment, cloudRegion).collect {
                    "${it.key}=${it.value}" as String
                }

                if (datadogEnabled) {
                    def datadogHelper = getHelperForClass DatadogHelper
                    variables += datadogHelper.getWebConfig(ctx, serviceConfigurations.name as String)
                }
                def envFile = isDevDeploymentEnabled() ? '.env.development' : isProdDeploymentEnabled() ? '.env.production' : ''
                if (envFile) {
                    def envFileHelper = getHelperForClass EnvFileHelper
                    envFileHelper.writeEnvFileWithVariable(envFile, variables as List<String>)
                }
            }
            buildCommands.each { buildCommand ->
                buildAndExecuteYarnCommand(buildCommand as String, buildParams, allBuildEnv)
            }
        }
    }

    @Override
    void codeQualityStage() {
        if (qualityStageEnabled) {
            stageScaffold('codeQuality') {
                def codeQualityCommand = getJobConfig('codeQualityCommand', String, 'sonar-scanner')
                def codeQualityParams = getJobConfig('codeQualityParams', List, [])
                def sonarQubeHelper = getHelperForClass SonarQubeHelper
                sonarQubeHelper.wrapWithSonarQubeServer {
                    sonarQubeHelper.setupSonarQubeScanner()
                    getHelperForClass(ShellHelper).sh "$codeQualityCommand ${codeQualityParams.join ' '}"
                }
                sonarQubeHelper.waitForQualityGate false
            }
        }
    }

    @Override
    void testStage() {
        stageScaffold('test') {
            def testCommand = getJobConfig('testCommand', String, 'test')
            def testParams = getJobConfig('testParams', List, [])
            buildAndExecuteYarnCommand(testCommand, testParams)
        }
    }

    void buildAndExecuteYarnCommand(String command, List<String> params = [], Map env = [:]) {
        def workingDir = getJobConfig('workingDirectory', String)
        def yarnHelper = getHelperForClass YarnHelper
        yarnHelper.execute(command, params, env, workingDir)
    }
}
