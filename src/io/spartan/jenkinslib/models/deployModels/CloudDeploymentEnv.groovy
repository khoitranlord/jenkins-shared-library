package io.spartan.jenkinslib.models.deployModels

import io.spartan.jenkinslib.helpers.CraneHelper
import io.spartan.jenkinslib.helpers.CredentialsHelper
import io.spartan.jenkinslib.helpers.DeployVarsHelper
import io.spartan.jenkinslib.helpers.GitHelper
import io.spartan.jenkinslib.helpers.KanikoHelper
import io.spartan.jenkinslib.helpers.ShellHelper
import io.spartan.jenkinslib.helpers.SlackHelper
import io.spartan.jenkinslib.helpers.YamlHelper
import io.spartan.jenkinslib.models.JenkinsDeployJobEnv

abstract class CloudDeploymentEnv extends JenkinsDeployJobEnv implements Serializable {
    protected static final String DEV_ENV = 'dev'
    protected static final String PROD_ENV = 'prod'

    String _imageTag

    CloudDeploymentEnv(Script ctx, Map<String, ?> jobConfig) {
        super(ctx, jobConfig)
    }

    void terraformApplyStage() {
        stageScaffold('terraformApply') {
            def workingDirs = getWorkingDirs()
            workingDirs.each { workingDir ->
                authenticationStage(environment)
                gitCryptStage(environment)
                terraformInitStage(workingDir)
                executeTerraformCommand('apply', ['-auto-approve', '-no-color'], workingDir)
            }
        }
    }

    void staticWebsiteDeployStage() {
        stageScaffold('staticWebsite') {
            upgradeStaticWebsite()
        }
    }

    void helmDeployStage() {
        stageScaffold('helm') {
            def dockerImageName = serviceConfigurations.name as String
            // if both deploy prod and promoteImage is enabled, promote image, otherwise build and push image
            if (isProdDeploymentEnabled() && isPromoteImageEnabled()) {
                pullAndPushImage(dockerImageName, imageTag, gitTag)

                def additionalImagesConfig = getJobConfig('additionalDockerImages', List)
                additionalImagesConfig.each {
                    pullAndPushImage(it.dockerImageName as String, "${imageTag}${it.dockerImageTagSuffix}" as String, "${gitTag}${it.dockerImageTagSuffix}" as String)
                }
            } else {
                createDockerConfigFile(environment)

                buildAndPushImage(dockerRepoByEnvironment, dockerImageName, environment == PROD_ENV ? gitTag : imageTag)
            }

            upgradeService()
        }
    }

    protected void pullAndPushImage(String dockerImageName, String imageTag, String gitTag) {
        def craneHelper = getHelperForClass CraneHelper

        craneHelper.login registryDefaultUsername, getRegistryAccessToken(DEV_ENV), getDockerRegistry(DEV_ENV)
        craneHelper.pull getDockerRepo(DEV_ENV), dockerImageName, imageTag

        craneHelper.login registryDefaultUsername, getRegistryAccessToken(PROD_ENV), getDockerRegistry(PROD_ENV)
        craneHelper.push getDockerRepo(PROD_ENV), dockerImageName, gitTag
    }

    String getImageTag() {
        def manifestContent = ctx.readJSON file: 'manifest.json'
        if (!_imageTag) {
            def gitHelper = getHelperForClass GitHelper
            _imageTag = "${manifestContent.version}_${gitHelper.parseShortRev()}"
        }

        _imageTag
    }

    protected String getVersionFromManifestFile() {
        def manifestContent = ctx.readJSON file: 'manifest.json'
        manifestContent.version
    }

    void replaceValueToValuesFile() {
        def shellHelper = getHelperForClass ShellHelper
        def deployVarsHelper = getHelperForClass DeployVarsHelper
        def allEnvironmentVariables = shellHelper.shForStdout 'printenv'

        List<String> baseEnvs = [
                "IMAGE_NAME=${serviceConfigurations.name}",
                "IMAGE_TAG=${environment == PROD_ENV && isPromoteImageEnabled() ? gitTag : imageTag}",
                "DOCKER_REPO=${serviceConfigurations.dockerRepoBaseName}$environment",
                "DD_VERSION=${environment == PROD_ENV ? gitTag : environment == DEV_ENV ? versionFromManifestFile : 'v0.0.1'}"
        ]

        List<String> serviceDeployVars = deployVarsHelper.getServiceDeployVars(deployVarsMode, serviceConfigurations, environment, cloudRegion).collect {
            "${it.key}=${it.value}" as String
        }

        List serviceAdditionalDeployVars = getJobConfig('helmAdditionalDeployVars', List)

        def allDeployVars = deployVars() + baseEnvs + serviceDeployVars + serviceAdditionalDeployVars + allEnvironmentVariables.split('\n').toList().findAll { it }

        def yamlHelper = getHelperForClass YamlHelper

        def helmValuesPath = getJobConfig('helmValuesPath', String)
        yamlHelper.writeYamlWithVariable(helmValuesPath, allDeployVars)

        def additionalFilesPaths = getJobConfig('helmAdditionalFilePaths', List, [''])
        additionalFilesPaths.findAll { it }.each { path ->
            yamlHelper.writeYamlWithVariable(path as String, allDeployVars)
        }
    }

    void buildAndPushImage(String dockerRepo, String dockerImage, String shortRev) {
        def kanikoHelper = getHelperForClass KanikoHelper
        def dockerFilePath = getJobConfig('dockerFilePath', String, '/')
        def dockerFileName = getJobConfig('dockerFileName', String, 'Dockerfile')
        def buildArgs = getJobConfig('dockerBuildArgs', Map, [:])

        kanikoHelper.buildAndPush(dockerFilePath, dockerFileName, dockerRepo, dockerImage, shortRev, buildArgs)

        def additionalImagesConfig = getJobConfig('additionalDockerImages', List)
        additionalImagesConfig.each {
            ctx.log.info "Building additional image with Dockerfile ${it.dockerFileName} at ${it.dockerFilePath} and name ${it.dockerImageName}"
            kanikoHelper.buildAndPush(it.dockerFilePath as String, it.dockerFileName as String, dockerRepo, it.dockerImageName as String, "${shortRev}${it.dockerImageTagSuffix}")
        }
    }

    protected String getDockerRepoByEnvironment() {
        getDockerRepo(environment)
    }

    abstract String getDockerRepo(String environment)
    abstract String getDockerRegistry(String environment)
    abstract String getRegistryDefaultUsername()
    abstract String getRegistryAccessToken(String environment)
    abstract void createDockerConfigFile(String environment)
    abstract void upgradeService()
    abstract void upgradeStaticWebsite()

    @Override
    void informStageClosure() {
        stageScaffold('inform') {
            def credentialsHelper = getHelperForClass CredentialsHelper
            def deployVarsHelper = getHelperForClass DeployVarsHelper
            credentialsHelper.withCredentials([
                    [type: 'string', credentialsId: 'slack-channel', variable: 'CHANNEL'],
                    [type: 'string', credentialsId: 'project-name', variable: 'PROJECT_NAME'],
                    [type: 'string', credentialsId: 'service-bot-endpoint', variable: 'SERVICE_BOT_ENDPOINT']
            ]) {
                def shellHelper = getHelperForClass ShellHelper
                def gitAuthor = shellHelper.shForStdout 'git log -1 --pretty=format:\'%an <%ae>\' | xargs'
                def gitCommits = shellHelper.shForStdout 'git --no-pager log --pretty=format:\'%h (%an) %s\' -n3 --no-color |  while read line || [ -n "$line" ]; do echo -n "- $line \\\\n"; done'
                def gitRepo = shellHelper.shForStdout "git config --get remote.origin.url"
                def gitRev = shellHelper.shForStdout 'git rev-parse HEAD'
                def gitBranch = shellHelper.shForStdout 'git rev-parse --abbrev-ref HEAD'

                // inform build result
                def messageVars = [
                        SERVICE_NAME: serviceConfigurations.name as String,
                        BUILD_STATUS: ctx.currentBuild.result as String,
                        ENVIRONMENT : environment.toUpperCase() as String
                ] + [
                        GIT_AUTHOR  : gitAuthor,
                        GIT_COMMITS : gitCommits,
                        GIT_REPO    : gitRepo.replaceAll('\\.git', ''),
                        GIT_REVISION: gitRev
                ] + deployVarsHelper.getServiceDeployVars(deployVarsMode, serviceConfigurations, environment, cloudRegion).collectEntries {
                    ["${it.key}=${it.value}" as String]
                }

                def slackHelper = getHelperForClass SlackHelper
                slackHelper.sendResultToChannel(ctx.env.'CHANNEL' as String, messageVars as Map<String, String>)

                if (gitBranch.contains('release/') && isDevDeploymentEnabled()) {
                    slackHelper.sendResultToChannel("#prj-${ctx.env.'CHANNEL' as String}-${gitBranch.replaceAll('\\.', '-').replaceAll('/', '-')}", messageVars as Map<String, String>)
                } else if (isProdDeploymentEnabled()) {
                    def latestReleaseChannel = shellHelper.shForStdout("curl ${ctx.env.'SERVICE_BOT_ENDPOINT'}/release/slack-channels/latest | jq .data.channel")
                    slackHelper.sendResultToChannel(latestReleaseChannel as String, messageVars as Map<String, String>)
                }
            }
        }
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

    protected List<String> deployVars() {
        [
            // set from pipeline
            "SERVICE_NAME=$serviceConfigurations.name",

            // change by cluster
            "ENVIRONMENT=$environment"
        ]
    }
}
