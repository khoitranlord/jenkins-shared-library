import io.spartan.jenkinslib.models.buildModels.YarnBuildEnv
import io.spartan.jenkinslib.helpers.YarnHelper

import static groovy.lang.Closure.DELEGATE_ONLY

def <T> T call(Closure<Void> jobConfig) {
    Script ctx = jobConfig.owner
    ctx.buildPipeline {
        buildJobEnvClosure = { Script c, Map<String, ?> config -> new YarnBuildEnv(c, config) }

        setupStageClosure = { c, buildEnv ->
            def yarnHelper = buildEnv.getHelperForClass(YarnHelper)
            yarnHelper.setupNodeVersion()
        }

        cacheEnabled = false

        workingDirectory = null

        // build
        yarnStageEnabled = true
        yarnStageTimeout = 15
        yarnStageName = 'Install + Build'
        yarnBuildCommands = { c, buildEnv ->
            def defaultTask = ['install']
            if (buildEnv.isPullRequestBuild()) {
                defaultTask + ['lint']
            } else if (buildEnv.isDevDeploymentEnabled()) {
                defaultTask + ['build:development']
            } else if (buildEnv.isProdDeploymentEnabled()) {
                defaultTask + ['build']
            }
        }
        yarnBuildParams = null
        yarnBuildEnv = null

        // test
        testStageName = 'Test'
        testCommand = 'test'
        testParams = ['--coverage', '--watchAll=false', '--maxWorkers=50%']

        // code quality
        codeQualityStageEnabled = false
        codeQualityStageName = 'Code Quality'
        codeQualityCommand = 'sonar-scanner'
        codeQualityParams = []

        promoteImageEnabled = false

        // datadog configurations
        datadogEnabled = false

        // use S3 + Cloudfront for static websites
        serverPlatform = "staticWebsite"
        deployVarsMode = "ssm"
        
        owner = ctx
        // evaluate the config given to this directive. do this last, so later configs overwrite earlier ones
        def closureConfig = delegate
        // delegate the config closure given by the Jenkinsfile to the same delegate this closure is evaluated to
        jobConfig.resolveStrategy = DELEGATE_ONLY
        jobConfig.delegate = closureConfig
        jobConfig()
    }
}
