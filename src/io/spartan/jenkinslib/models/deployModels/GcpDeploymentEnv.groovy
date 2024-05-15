package io.spartan.jenkinslib.models.deployModels

import io.spartan.jenkinslib.clusters.Cluster
import io.spartan.jenkinslib.helpers.CredentialsHelper
import io.spartan.jenkinslib.helpers.GCloudHelper
import io.spartan.jenkinslib.helpers.HelmHelper
import io.spartan.jenkinslib.helpers.K8sSecretHelper

class GcpDeploymentEnv extends CloudDeploymentEnv implements Serializable {
    GcpDeploymentEnv(Script ctx, Map<String, ?> jobConfig) {
        super(ctx, jobConfig)
    }

    @Override
    String getDockerRepo(String environment) {
        "${serviceConfigurations.dockerRepoBaseName}$environment"
    }

    @Override
    String getDockerRegistry(String environment) {
        "gcr.io"
    }

    @Override
    String getRegistryDefaultUsername() {
        'gclouddockertoken'
    }

    @Override
    String getRegistryAccessToken(String environment) {
        def gcloudHelper = getHelperForClass GCloudHelper
        gcloudHelper.connectAndGetCredentials(environment)
        gcloudHelper.auth('print-access-token')
    }

    @Override
    void createDockerConfigFile(String environment) {
        def tempAccessToken = getRegistryAccessToken(environment)

        def dockerToken = "$registryDefaultUsername:$tempAccessToken".bytes.encodeBase64().toString()
        def defaultEmail = "jenkins-ci@c0x12c.com"

        def configJsonMap = [
                "auths": [
                        "gcr.io": [
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
        credentialsHelper.withCredentials(Cluster.getCredentialsEnv(environment)) {
            // replace helm values
            replaceValueToValuesFile()

            // install helm
            def helmHelper = getHelperForClass HelmHelper
            helmHelper.repoAdd(serviceConfigurations.helmRepo as String, "https://${ctx.env.'GITHUB_TOKEN'}@${ctx.env.'HELM_CHART_URL'}")
            def helmValuesPaths = [getJobConfig('helmValuesPath', String)] + getJobConfig('helmAdditionalFilePaths', List, [''])
            helmHelper.upgradeInstall(
                "${serviceConfigurations.deploymentName ?: serviceConfigurations.name}",
                serviceConfigurations.chartPath as String,
                serviceConfigurations.namespace as String ?: "${serviceConfigurations.name}",
                helmValuesPaths,
                serviceConfigurations.chartVersion as String,
            )
        }
    }

    @Override
    void upgradeStaticWebsite() {
        ctx.log.info "GCP doesn't support Static Website"
    }
}
