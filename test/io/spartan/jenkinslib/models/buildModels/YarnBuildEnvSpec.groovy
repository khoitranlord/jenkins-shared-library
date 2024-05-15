package io.spartan.jenkinslib.models.buildModels

import io.spartan.jenkinslib.helpers.*
import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification

class YarnBuildEnvSpec extends WorkflowScriptSpecification {
    def workflowHelper = Mock WorkflowHelper
    def timeoutHelper = Mock TimeoutHelper
    def sonarQubeHelper = Mock SonarQubeHelper
    def yarnHelper = Mock YarnHelper
    def credentialsHelper = Mock CredentialsHelper
    def envFileHelper = Mock EnvFileHelper
    def gcpHelper = Mock GCloudHelper
    def awsHelper = Mock AWSHelper
    def k8sSecretHelper = Mock K8sSecretHelper
    def shellHelper = Mock ShellHelper
    def datadogHelper = Mock DatadogHelper
    def deployVarsHelper = Mock DeployVarsHelper

    def setup() {
        injector.getHelperForClass(WorkflowHelper) >> workflowHelper
        injector.getHelperForClass(TimeoutHelper) >> timeoutHelper
        injector.getHelperForClass(SonarQubeHelper) >> sonarQubeHelper
        injector.getHelperForClass(YarnHelper) >> yarnHelper
        injector.getHelperForClass(CredentialsHelper) >> credentialsHelper
        injector.getHelperForClass(EnvFileHelper) >> envFileHelper
        injector.getHelperForClass(GCloudHelper) >> gcpHelper
        injector.getHelperForClass(AWSHelper) >> awsHelper
        injector.getHelperForClass(K8sSecretHelper) >> k8sSecretHelper
        injector.getHelperForClass(ShellHelper) >> shellHelper
        injector.getHelperForClass(DatadogHelper) >> datadogHelper
        injector.getHelperForClass(DeployVarsHelper) >> deployVarsHelper
    }

    def 'test build stage for PR'() {
        given:
        def stageName = 'Build'
        def timeout = 5

        def buildEnv = new YarnBuildEnv(ctx, [
                yarnStageName       : stageName,
                yarnStageEnabled    : true,
                yarnStageTimeout    : timeout,
                yarnBuildCommands   : { c, buildEnv ->
                    def defaultTask = ['install']
                    if (buildEnv.isPullRequestBuild()) {
                        defaultTask + ['lint']
                    } else if (buildEnv.isDevDeploymentEnabled()) {
                        defaultTask + ['build:development']
                    } else if (buildEnv.isProdDeploymentEnabled()) {
                        defaultTask + ['build']
                    }
                },
                devDeploymentEnabled : false,
                prodDeploymentEnabled: false
        ])

        when:
        buildEnv.buildStage()

        then:
        3 * ctx.env.getProperty('BRANCH_NAME') >> 'PR-123'
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * yarnHelper.execute('install', [], [CI: true], null)
        1 * yarnHelper.execute('lint', [], [CI: true], null)
    }

    def 'test build stage for release dev'() {
        given:
        def stageName = 'Build'
        def timeout = 5
        def environment = 'dev'
        def config = [
            yarnStageName        : stageName,
            yarnStageEnabled     : true,
            yarnStageTimeout     : timeout,
            serviceConfigurations: [
                    name             : 'service-name',
                    namespace        : 'service-namespace',
                    clusterNamePrefix: 'cluster-prefix-'
            ],
            yarnBuildCommands    : { c, buildEnv ->
                def defaultTask = ['install']
                if (buildEnv.isPullRequestBuild()) {
                    defaultTask + ['lint']
                } else if (buildEnv.isDevDeploymentEnabled()) {
                    defaultTask + ['build:development']
                } else if (buildEnv.isProdDeploymentEnabled()) {
                    defaultTask + ['build']
                }
            },
            devDeploymentEnabled  : true,
            prodDeploymentEnabled : false,
            deployVendor          : 'aws',
            datadogEnabled        : true,
            deployVarsMode        : 'ssm',
            cloudRegion           : 'us-west-2'
        ]
        def buildEnv = new YarnBuildEnv(ctx, config)

        when:
        buildEnv.buildStage()

        then:
        3 * ctx.env.getProperty('BRANCH_NAME') >> 'release/2024.01.17'
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * awsHelper.connect(ctx, environment)
        1 * deployVarsHelper.getServiceDeployVars(config.deployVarsMode, config.serviceConfigurations, environment, config.cloudRegion)
        1 * envFileHelper.writeEnvFileWithVariable('.env.development', [
            "CI=false",
            "DD_APPLICATION_ID=ddApplicationId",
            "DD_CLIENT_TOKEN=ddClientToken",
            "DD_SERVICE=ddService"
        ])

        and:
        1 * yarnHelper.execute('install', [], [CI: false], null)
        1 * yarnHelper.execute('build:development', [], [CI: false], null)

        and:
            1 * datadogHelper.getWebConfig(ctx, "service-name") >> [
                "DD_APPLICATION_ID=ddApplicationId",
                "DD_CLIENT_TOKEN=ddClientToken",
                "DD_SERVICE=ddService"
            ]
    }

    def 'test code quality stage'() {
        given:
        def stageName = 'Code Quality'
        def timeout = 5

        def buildEnv = new YarnBuildEnv(ctx, [
                codeQualityStageName   : stageName,
                codeQualityStageEnabled: true,
                codeQualityStageTimeout: timeout,
                qualityStageEnabled     : true,
                devDeploymentEnabled    : false,
                prodDeploymentEnabled   : false
        ])

        when:
        buildEnv.codeQualityStage()

        then:
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * sonarQubeHelper.wrapWithSonarQubeServer(_ as Closure) >> { cls -> cls[0]() }
        1 * sonarQubeHelper.setupSonarQubeScanner()
        1 * sonarQubeHelper.waitForQualityGate(false)

        and:
        1 * shellHelper.sh('sonar-scanner ')
    }

    def 'test testStage'() {
        given:
        def stageName = 'Test'
        def timeout = 5

        def buildEnv = new YarnBuildEnv(ctx, [
                testStageName       : stageName,
                testStageEnabled    : true,
                testStageTimeout    : timeout,
                devDeploymentEnabled : false,
                prodDeploymentEnabled: false
        ])

        when:
        buildEnv.testStage()

        then:
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * yarnHelper.execute('test', [], [:], null)
    }
}
