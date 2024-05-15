import io.spartan.jenkinslib.models.JenkinsJobEnv

import static io.spartan.jenkinslib.pipelines.DefaultPipeline.defaultPipeline
import static groovy.lang.Closure.DELEGATE_ONLY

def <T> T call(Closure<Void> pipelineClosure) {
    // the closure owner is the Script object of the Jenkinsfile
    def ctx = pipelineClosure.owner

    defaultPipeline {
        extraLibProperties = [:]

        // define cloud provider
        cloudName = null
        cloudRegion = null

        // return `false` here to skip the build. will set build result to `NOT_BUILT`, if not set otherwise.
        buildEnabled = false

        // enable SonarQube scanning
        qualityStageEnabled = false

        // enable Git ignore changes
        ignoreChangesEnabled = false

        // return `false` to skip the deploy to dev env
        devDeploymentEnabled = false

        // return `false` to skip the deploy to prod env
        prodDeploymentEnabled = false

        // setup stage
        setupStageName = 'Setup'
        setupStageEnabled = true

        // defaults to `MINUTES`; deactivated if null or <0
        setupStageTimeout = 10
        setupStageClosure = null

        buildStageClosure = { Script c, JenkinsJobEnv jobEnv -> }
        deployStageClorsure = { Script c, JenkinsJobEnv jobEnv -> }

        // inform stage
        informStageName = 'Inform'
        informStageEnabled = false
        informStageTimeout = 5
        informStageClosure = null

        // setup platform
        serverPlatform = null
        infraPlatform = null

        // deploy variables mode - should be null, k8s,...
        deployVarsMode = null
        // ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

        // set the owner of this closure to the Script object of the Jenkinsfile
        owner = ctx

        // evaluate the config given to this directive. do this last, so later configs overwrite earlier ones
        def closureConfig = delegate
        // delegate the config closure given by the Jenkinsfile to the same delegate this closure is evaluated to
        pipelineClosure.resolveStrategy = DELEGATE_ONLY
        pipelineClosure.delegate = closureConfig
        pipelineClosure()
    }
}
