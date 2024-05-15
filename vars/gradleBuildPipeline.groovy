import io.spartan.jenkinslib.models.buildModels.GradleBuildEnv
import static groovy.lang.Closure.DELEGATE_ONLY

def <T> T call(Closure<Void> jobConfig) {
    Script ctx = jobConfig.owner
    ctx.buildPipeline {
        buildJobEnvClosure = { Script c, Map<String, ?> config -> new GradleBuildEnv(c, config) }

        cacheEnabled = true

        // build
        gradleStageEnabled = true
        gradleStageTimeout = 15
        gradleStageName = 'Build'
        gradleBuildCommands = null
        gradleBuildParams = null

        // test
        testStageName = 'Test'
        testCommand = null
        testParams = []

        // code quality
        codeQualityStageName = 'Code Quality'
        codeQualityCommand = 'sonar'
        codeQualityParams = []

        promoteImageEnabled = true

        serverPlatform = "k8s"

        owner = ctx
        // evaluate the config given to this directive. do this last, so later configs overwrite earlier ones
        def closureConfig = delegate
        // delegate the config closure given by the Jenkinsfile to the same delegate this closure is evaluated to
        jobConfig.resolveStrategy = DELEGATE_ONLY
        jobConfig.delegate = closureConfig
        jobConfig()
    }
}
