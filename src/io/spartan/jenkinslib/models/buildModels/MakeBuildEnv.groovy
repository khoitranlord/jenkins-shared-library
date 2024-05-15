package io.spartan.jenkinslib.models.buildModels

import io.spartan.jenkinslib.helpers.MakeHelper
import io.spartan.jenkinslib.helpers.CredentialsHelper
import io.spartan.jenkinslib.helpers.SonarQubeHelper
import io.spartan.jenkinslib.helpers.ShellHelper

class MakeBuildEnv extends BuildJobEnv implements Serializable {

    MakeBuildEnv(Script ctx, Map<String, ?> jobConfig) {
        super(ctx, jobConfig)
    }

    @Override
    void buildStage() {
        stageScaffold('githubAuth') {
            def shellHelper = getHelperForClass ShellHelper
            def credentialsHelper = getHelperForClass CredentialsHelper
            credentialsHelper.withCredentials([
                    [type: 'usernamePassword', credentialsId: 'github-credentials', usernameVariable: 'githubUsername', passwordVariable: 'githubPassword']
            ]) {
                shellHelper.sh "git config --global url.\"https://${ctx.env.githubUsername}:${ctx.env.githubPassword}@github.com\".insteadOf https://github.com"
            }
        }

        stageScaffold('make') {
            def buildCommands = getJobConfig('makeBuildCommands', List, ['build'])
            def buildParams = getJobConfig('makeBuildParams', List, [])
            buildCommands.each { buildCommand ->
                buildAndExecuteMakeCommand(buildCommand as String, buildParams)
            }
        }
    }

    @Override
    void codeQualityStage() {
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

    @Override
    void testStage() {
        stageScaffold('unitTest') {
            def unitTestCommand = getJobConfig('unitTestCommand', String, 'unit-test')
            def unitTestParams = getJobConfig('unitTestParams', List, [])
            buildAndExecuteMakeCommand(unitTestCommand, unitTestParams)
        }
        stageScaffold('integrationTest') {
            def integrationTestCommand = getJobConfig('integrationTestCommand', String, 'integration-test')
            def integrationTestParams = getJobConfig('integrationTestParams', List, [])
            buildAndExecuteMakeCommand(integrationTestCommand, integrationTestParams)
        }
    }

    void buildAndExecuteMakeCommand(String command, List makeParams = []) {
        def makeHelper = getHelperForClass MakeHelper
        makeHelper.execute command, makeParams
    }

}
