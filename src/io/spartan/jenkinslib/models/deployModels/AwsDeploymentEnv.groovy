package io.spartan.jenkinslib.models.deployModels

import io.spartan.jenkinslib.helpers.AWSHelper
import io.spartan.jenkinslib.helpers.CredentialsHelper
import io.spartan.jenkinslib.helpers.DeployVarsHelper
import io.spartan.jenkinslib.helpers.HelmHelper

class AwsDeploymentEnv extends CloudDeploymentEnv implements Serializable {
    AwsDeploymentEnv(Script ctx, Map<String, ?> jobConfig) {
        super(ctx, jobConfig)
    }

    @Override
    String getDockerRegistry(String environment) {
        getECRRegistry(environment)
    }

    @Override
    String getDockerRepo(String environment) {
        getECRRegistry(environment)
    }

    @Override
    String getRegistryDefaultUsername() {
        'AWS'
    }

    @Override
    String getRegistryAccessToken(String environment) {
        def awsHelper = getHelperForClass AWSHelper
        awsHelper.connect(ctx, environment)
        return awsHelper.command("ecr", ["get-login-password"], cloudRegion)
    }

    @Override
    void createDockerConfigFile(String environment) {
        def tempAccessToken = getRegistryAccessToken(environment)

        def dockerToken = "$registryDefaultUsername:$tempAccessToken".bytes.encodeBase64().toString()
        def defaultEmail = "jenkins-ci@c0x12c.com"
        def registry = getDockerRegistry(environment)

        def configJsonMap = [
            "auths": [
                "$registry": [
                    "auth" : dockerToken,
                    "email": defaultEmail
                ]
            ]
        ]

        ctx.writeJSON file: "${ctx.env.'WORKSPACE'}/.docker/config.json", json: configJsonMap
    }

    @Override
    void upgradeService() {
        def credentialsHelper = getHelperForClass CredentialsHelper
        credentialsHelper.withCredentials([
            [type: 'usernamePassword', credentialsId: "github-app-credentials",  usernameVariable: 'githubUsername', passwordVariable: 'githubPassword'],
            [type: 'string', credentialsId: "helm-chart-url", variable: 'helmChartUrl']
        ]) {
            def awsHelper = getHelperForClass AWSHelper
            awsHelper.connect(ctx, environment)
            awsHelper.command("eks", ["update-kubeconfig", "--name", "${serviceConfigurations.clusterNamePrefix}${environment}"], cloudRegion)
            // replace helm values
            replaceValueToValuesFile()

            // install helm
            def helmHelper = getHelperForClass HelmHelper
            helmHelper.repoAdd(serviceConfigurations.helmRepo as String, "https://${ctx.env.'githubUsername'}:${ctx.env.'githubPassword'}@${ctx.env.'helmChartUrl'}")
            def helmValuesPaths = [getJobConfig('helmValuesPath', String)] + getJobConfig('helmAdditionalFilePaths', List, [''])
            helmHelper.upgradeInstall(
                "\"${serviceConfigurations.deploymentName ?: serviceConfigurations.name}",
                serviceConfigurations.chartPath as String,
                serviceConfigurations.namespace as String ?: "${serviceConfigurations.name}-$environment",
                helmValuesPaths,
                serviceConfigurations.chartVersion as String
            )
        }
    }

    @Override
    void upgradeStaticWebsite() {
        def awsHelper = getHelperForClass AWSHelper
        awsHelper.connect(ctx, environment)

        def deployVarsHelper = getHelperForClass DeployVarsHelper

        def secretDeployVarsMap = deployVarsHelper.getServiceDeployVars(deployVarsMode, serviceConfigurations, environment, cloudRegion)

        awsHelper.copyToS3(serviceConfigurations.sourceDir as String, secretDeployVarsMap.AWS_S3_BUCKET as String, cloudRegion)
        awsHelper.invalidateCloudFront(secretDeployVarsMap.AWS_CLOUDFRONT_ID as String, serviceConfigurations.path as String, cloudRegion)
    }

    String getECRRegistry(String environment) {
        def awsHelper = getHelperForClass AWSHelper
        awsHelper.getEcrUrl(environment)
    }
}
