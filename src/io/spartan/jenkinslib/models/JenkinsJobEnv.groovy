package io.spartan.jenkinslib.models

import io.spartan.jenkinslib.JenkinsLibInjector
import io.spartan.jenkinslib.JenkinsLibrary
import io.spartan.jenkinslib.helpers.AWSHelper
import io.spartan.jenkinslib.helpers.BaseHelper
import io.spartan.jenkinslib.helpers.CredentialsHelper
import io.spartan.jenkinslib.helpers.GitHelper
import io.spartan.jenkinslib.helpers.GitCryptHelper
import io.spartan.jenkinslib.helpers.TimeoutHelper
import io.spartan.jenkinslib.helpers.TerraformHelper
import io.spartan.jenkinslib.helpers.WorkflowHelper

class JenkinsJobEnv implements Serializable {
    protected static final String DEV_ENV = 'dev'
    protected static final String PROD_ENV = 'prod'

    // the script context of the job
    protected Script ctx
    // the environment variables of the job
    protected GroovyObjectSupport env

    // Job config
    protected Map<String, ?> jobConfig

    JenkinsJobEnv(Script ctx, Map<String, ?> jobConfig = [:]) {
        this.ctx = ctx
        this.env = ctx.env as GroovyObjectSupport

        this.jobConfig = jobConfig
    }

    void setupStage() {
        stageScaffold('setup') {
            ctx.currentBuild.displayName = this.buildNumber
        }
    }

    void informStageClosure() {
        // do nothing by default
    }

    boolean isEnableBuild() {
        getJobConfig('buildEnabled', Boolean)
    }

    String getBranchName() {
        (pullRequestBuild ? env.'CHANGE_BRANCH' : env.'BRANCH_NAME')?.trim()
    }

    boolean isPullRequestBuild() {
        env.'BRANCH_NAME' ==~ /^PR-\d+$/
    }

    boolean isReleaseBranchBuild() {
        branchName ==~ /^release\/.*$/
    }

    boolean isTagBuild() {
        gitTag ==~ /^v.*\.*\.*/
    }

    boolean isTerraformBuildEnabled() {
        getJobConfig('terraformBuildEnabled', Boolean)
    }

    boolean isTerragruntEnabled() {
        getJobConfig('terragruntEnabled', Boolean)
    }

    boolean isDefaultBranchBuild() {
        branchName == getHelperInjector().libProperties.get('JENKINS_DEFAULT_BRANCH', String)
    }

    boolean isEnableDeploy() {
        devDeploymentEnabled || prodDeploymentEnabled
    }

    boolean isPromoteImageEnabled() {
        getJobConfig('promoteImageEnabled', Boolean)
    }

    boolean isDevDeploymentEnabled() {
        getJobConfig('devDeploymentEnabled', Boolean)
    }

    boolean isProdDeploymentEnabled() {
        getJobConfig('prodDeploymentEnabled', Boolean)
    }

    boolean isEnvFileEnabled() {
        getJobConfig('envFileEnabled', Boolean)
    }

    boolean isEnableEnvVar() {
        getJobConfig('envVarEnabled', Boolean)
    }

    String getGitTag() {
        ctx.env.'TAG_NAME'?.trim()
    }

    Map getServiceConfigurations() {
        getJobConfig('serviceConfigurations', Map)
    }

    String getCloudName() {
        getJobConfig('cloudName', String)
    }

    String getCloudRegion() {
        getJobConfig('cloudRegion', String, 'us-west-2')
    }

    String getServerPlatform() {
        getJobConfig('serverPlatform', String)
    }

    String getInfraPlatform() {
        getJobConfig('infraPlatform', String)
    }

    boolean isQualityStageEnabled() {
        getJobConfig('qualityStageEnabled', Boolean)
    }

    boolean isEnableIgnoreChanges() {
        getJobConfig('ignoreChangesEnabled', Boolean)
    }

    boolean isDatadogEnabled() {
        getJobConfig('datadogEnabled', Boolean)
    }

    String getDeployVarsMode() {
        getJobConfig('deployVarsMode', String)
    }

    protected String getEnvironment() {
        if (devDeploymentEnabled) {
            DEV_ENV
        } else if (prodDeploymentEnabled) {
            PROD_ENV
        } else {
            ctx.error 'neither dev nor prod deployment enabled!! Abort'
        }
    }

    List<String> getWorkingDirs(Boolean ignoreChangesEnabled = false) {
        def workingDirs = getJobConfig('workingDirs', List)
        if (!ignoreChangesEnabled) {
            return workingDirs
        }
        def gitHelper = getHelperForClass GitHelper
        def sourceRef = gitHelper.parseShortRev("HEAD")
        def targetRef = pullRequestBuild ? gitHelper.parseShortRev("origin/master") : gitHelper.getLatestCommitFromReleaseBranch(1)
        return workingDirs.findAll { dir ->
            gitHelper.checkForFileChange(dir as String, sourceRef, targetRef)
        } as List<String>
    }

    def <T> T stageScaffold(String stageId, Closure<T> closure) {
        def result = getHelperForClass(WorkflowHelper).conditionalStage(getJobConfig("${stageId}StageName", String), getJobConfig("${stageId}StageEnabled", Boolean, true)) {
            def result = getHelperForClass(TimeoutHelper).withTimeout getJobConfig("${stageId}StageTimeout", Integer), {
                def stageClosure = getJobConfig("${stageId}StageClosure", Closure)
                if (stageClosure != null) {
                    ctx.log.info "executing custom stage closure for stage with id '$stageId'"
                    stageClosure.call(ctx, this) as T
                } else {
                    ctx.log.info "executing default stage closure for stage with id '$stageId'"
                    getJobConfig("${stageId}StageBeginClosure", Closure)?.call(ctx, this)
                    def result = closure?.call()
                    getJobConfig("${stageId}StageEndClosure", Closure)?.call(ctx, this)
                    result
                }
            }
            result
        }
        result
    }

    def <T> T getJobConfig(String key, Class<T> expectedType = Object, T defaultValue = null) {
        def jobConfigValue = this.jobConfig.get(key)
        // in case this is a closure returning the actual value, call it
        def closureValue = jobConfigValue in Closure && !expectedType.isAssignableFrom(Closure) ?
                jobConfigValue.call(*([ctx, this])) : jobConfigValue

        if (closureValue == null) {
            defaultValue
        } else {
            if (expectedType.isAssignableFrom(closureValue.getClass())) {
                expectedType.cast closureValue
            } else if (String.isAssignableFrom(expectedType) && GString.isAssignableFrom(closureValue.getClass())) {
                closureValue as T
            } else if (closureValue in String && List.isAssignableFrom(expectedType)) {
                // convert string of type 'value1, value2, value3, ...' to list ['value1', 'value2', 'value3', ...]
                (closureValue as String).split(',').collect { it.trim() } as T
            } else if (closureValue in String && Map.isAssignableFrom(expectedType)) {
                // convert string of type 'key1=value1, key2=value2, ...' to map [key1: 'value1', key2: 'value2', ...]
                (closureValue as String).split(',').collectEntries { it.trim().split('=', 2).collect { it.trim() } } as T
            } else {
                ctx.error "failed to return job config at key '$key' of type '${closureValue.getClass().canonicalName}' as '${expectedType.canonicalName}'"
            }
        }
    }

    JenkinsLibInjector getHelperInjector() {
        if (!JenkinsLibrary.getInjector()) {
            JenkinsLibrary.init(ctx, jobConfig.extraLibProperties as Map<String, ?>)
        }
        JenkinsLibrary.getInjector()
    }

    def <T extends BaseHelper> T getHelperForClass(Class<T> cls) {
      getHelperInjector().getHelperForClass cls
    }

    void executeTerraformCommand(String command, List taskParams, String workingDir = null) {
        def terraformHelper = getHelperForClass TerraformHelper
        terraformHelper.execute(command, taskParams, workingDir, terragruntEnabled)
    }

    void terraformInitStage(String workingDir) {
        executeTerraformCommand('init', ['-upgrade'], workingDir)
    }

    void gitCryptStage(String environment) {
        def credentialsHelper = getHelperForClass CredentialsHelper
        def gitCryptHelper = getHelperForClass GitCryptHelper
        credentialsHelper.withCredentials([
                [type: 'string', credentialsId: "git-crypt-key-$environment", variable: 'GIT_CRYPT_KEY'],
        ]) {
            def gitCryptKey = ctx.env.'GIT_CRYPT_KEY'
            gitCryptHelper.runGitCrypt(gitCryptKey as String)
        }
    }

    void authenticationStage(String environment) {
        def terraformHelper = getHelperForClass TerraformHelper
        def awsHelper = getHelperForClass AWSHelper

        terraformHelper.authenticate()
        awsHelper.connect(ctx, environment)
    }
}
