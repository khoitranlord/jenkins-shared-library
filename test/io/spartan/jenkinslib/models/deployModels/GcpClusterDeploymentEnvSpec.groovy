package io.spartan.jenkinslib.models.deployModels


import io.spartan.jenkinslib.helpers.*
import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification

import static io.spartan.jenkinslib.clusters.Cluster.*

class GcpClusterDeploymentEnvSpec extends WorkflowScriptSpecification {
    def workflowHelper = Mock WorkflowHelper
    def timeoutHelper = Mock TimeoutHelper
    def gcloudHelper = Mock GCloudHelper
    def kanikoHelper = Mock KanikoHelper
    def gitHelper = Mock GitHelper
    def helmHelper = Mock HelmHelper
    def credentialsHelper = Mock CredentialsHelper
    def shellHelper = Mock ShellHelper
    def k8sSecretHelper = Mock K8sSecretHelper
    def yamlHelper = Mock YamlHelper
    def deployVarsHelper = Mock DeployVarsHelper

    def setup() {
        injector.getHelperForClass(WorkflowHelper) >> workflowHelper
        injector.getHelperForClass(TimeoutHelper) >> timeoutHelper
        injector.getHelperForClass(GCloudHelper) >> gcloudHelper
        injector.getHelperForClass(KanikoHelper) >> kanikoHelper
        injector.getHelperForClass(GitHelper) >> gitHelper
        injector.getHelperForClass(HelmHelper) >> helmHelper
        injector.getHelperForClass(CredentialsHelper) >> credentialsHelper
        injector.getHelperForClass(ShellHelper) >> shellHelper
        injector.getHelperForClass(K8sSecretHelper) >> k8sSecretHelper
        injector.getHelperForClass(YamlHelper) >> yamlHelper
        injector.getHelperForClass(DeployVarsHelper) >> deployVarsHelper
    }

    def 'test deployStage for dev'() {
        given:
        def stageName = 'Deploy'
        def currentWorkspace = '/home/agent/build-dir'
        def timeout = 5
        def environment = 'dev'
        def config =  [
                helmStageName        : stageName,
                helmStageEnabled     : true,
                helmStageTimeout     : timeout,
                serviceConfigurations: [
                        name              : 'service-name',
                        dockerRepoBaseName: 'baserepo',
                        namespace         : 'service-namespace',
                        helmRepo          : 'helm-repo',
                        deploymentName    : 'service-name-deployment',
                        chartPath         : 'chart-path',
                        chartVersion      : '0.0.1'
                ],
                helmValuesPath       : 'dev/values.yaml',
                devDeploymentEnabled  : true,
                prodDeploymentEnabled : false,
                promoteImageEnabled  : true,
                cloudRegion: 'us-west-2'
        ]
        def deployEnv = new GcpDeploymentEnv(ctx, config)

        when: 'call deploy stage'
        deployEnv.helmDeployStage()

        then:
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * ctx.env.getProperty('WORKSPACE') >> currentWorkspace
        1 * gcloudHelper.connectAndGetCredentials(environment)
        1 * gcloudHelper.auth('print-access-token') >> 'secret'

        and:
        1 * ctx.writeJSON([file: "$currentWorkspace/.docker/config.json", json: ['auths': ['gcr.io': ['auth': 'Z2Nsb3VkZG9ja2VydG9rZW46c2VjcmV0', 'email': 'jenkins-ci@c0x12c.com']]]])

        and:
        1 * gitHelper.parseShortRev() >> '12345678'
        1 * kanikoHelper.buildAndPush('/', 'Dockerfile', 'baserepodev', 'service-name', '0.0.1_12345678', [:])

        and:
        1 * credentialsHelper.withCredentials([
                [type: 'string', credentialsId: "$GCLOUD_K8S_CLUSTER-dev", variable: 'GCLOUD_K8S_CLUSTER'],
                [type: 'string', credentialsId: "$GCLOUD_PROJECT_ID-dev", variable: 'GCLOUD_PROJECT_ID'],
                [type: 'string', credentialsId: "$GCLOUD_PROJECT_ID-dev", variable: 'GCP_PROJECT'],
                [type: 'string', credentialsId: "$GCLOUD_REGION-dev", variable: 'GCLOUD_REGION'],
                [type: 'string', credentialsId: "$GCLOUD_REGION-dev", variable: 'GCP_REGION'],
                [type: 'string', credentialsId: "$BASE_DOMAIN-dev", variable: 'BASE_DOMAIN'],
                [type: 'string', credentialsId: "$WORKLOAD_IDENTITY_PROVIDER-dev", variable: 'WORKLOAD_IDENTITY_PROVIDER'],
                [type: 'string', credentialsId: "$PIPELINE_OPS_SERVICE_ACCOUNT-dev", variable: 'PIPELINE_OPS_SERVICE_ACCOUNT'],
                [type: 'string', credentialsId: PAT, variable: 'GITHUB_TOKEN'],
                [type: 'string', credentialsId: 'helm-chart-url', variable: 'HELM_CHART_URL'],
        ], _ as Closure) >> { _, cls -> cls() }

        1 * yamlHelper.writeYamlWithVariable("$environment/values.yaml", [
                "SERVICE_NAME=service-name",
                "ENVIRONMENT=$environment",
                "IMAGE_NAME=service-name",
                "IMAGE_TAG=0.0.1_12345678",
                "DOCKER_REPO=baserepodev",
                "DD_VERSION=0.0.1",
                null
        ])

        1 * deployVarsHelper.getServiceDeployVars(config.deployVarsMode, config.serviceConfigurations, environment, config.cloudRegion)

        1 * shellHelper.shForStdout('printenv') >> ''
        3 * ctx.readJSON(file: 'manifest.json') >> [version: '0.0.1']
        1 * ctx.echo("executing default stage closure for stage with id 'helm'")


        and:
        1 * ctx.env.getProperty('GITHUB_TOKEN') >> 'github-token'
        1 * ctx.env.getProperty('HELM_CHART_URL') >> 'chart-url.test'

        1 * helmHelper.repoAdd('helm-repo', 'https://github-token@chart-url.test')
        1 * helmHelper.upgradeInstall('service-name-deployment', 'chart-path', 'service-namespace', ['dev/values.yaml', ''], '0.0.1')
    }
}
