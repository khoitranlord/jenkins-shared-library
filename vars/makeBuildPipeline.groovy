import io.spartan.jenkinslib.models.buildModels.MakeBuildEnv
import static groovy.lang.Closure.DELEGATE_ONLY

def <T> T call(Closure<Void> jobConfig) {
    Script ctx = jobConfig.owner
    ctx.buildPipeline {
        buildJobEnvClosure = { Script c, Map<String, ?> config -> new MakeBuildEnv(c, config) }

        nodeBuildLabel = { c, buildEnv ->
            if (buildEnv.isPullRequestBuild()) {
                'builder'
            } else {
                'heavy'
            }
        }

        //setup
        githubAuthStageEnabled = false
        githubAuthStageName = 'Github Authentication'
        githubAuthStageTimeout = 5

        // build
        makeStageEnabled = true
        makeStageName = 'Build'
        makeStageTimeout = 15
        makeBuildCommands = { c, buildEnv ->
            if (buildEnv.isDevDeploymentEnabled()) {
                ['build']
            }
        }
        makeBuildParams = null

        // unitTest
        unitTestStageEnabled = { c, buildEnv ->
            buildEnv.isPullRequestBuild()
        }
        unitTestStageName = 'Unit Test'
        unitTestStageTimeout = 10
        unitTestCommand = 'unit-test'
        unitTestParams = []

        // integrationTest
        integrationTestStageEnabled = { c, buildEnv ->
            buildEnv.isPullRequestBuild()
        }
        integrationTestStageName = 'Integration Test'
        integrationTestStageTimeout = 10
        integrationTestCommand = 'integration-test'
        integrationTestParams = []

        // code quality
        codeQualityStageName = 'Code Quality'
        codeQualityStageEnabled = false
        codeQualityStageStageTimeout = 10
        codeQualityCommand = 'sonar-scanner'
        codeQualityParams = []

        ignoreTestPaths = []

        promoteImageEnabled = false

        additionalDockerImages = []

        owner = ctx
        // evaluate the config given to this directive. do this last, so later configs overwrite earlier ones
        def closureConfig = delegate
        // delegate the config closure given by the Jenkinsfile to the same delegate this closure is evaluated to
        jobConfig.resolveStrategy = DELEGATE_ONLY
        jobConfig.delegate = closureConfig
        jobConfig()
    }
}
