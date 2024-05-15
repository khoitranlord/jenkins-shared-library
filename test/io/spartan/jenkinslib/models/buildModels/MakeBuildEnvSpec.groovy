package io.spartan.jenkinslib.models.buildModels

import io.spartan.jenkinslib.helpers.*
import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification

class MakeBuildEnvSpec extends WorkflowScriptSpecification {

    def workflowHelper = Mock WorkflowHelper
    def timeoutHelper = Mock TimeoutHelper
    def sonarQubeHelper = Mock SonarQubeHelper
    def makeHelper = Mock MakeHelper
    def credentialsHelper = Mock CredentialsHelper
    def envFileHelper = Mock EnvFileHelper
    def gcpHelper = Mock GCloudHelper
    def awsHelper = Mock AWSHelper
    def k8sSecretHelper = Mock K8sSecretHelper
    def shellHelper = Mock ShellHelper

    def setup() {
        injector.getHelperForClass(WorkflowHelper) >> workflowHelper
        injector.getHelperForClass(TimeoutHelper) >> timeoutHelper
        injector.getHelperForClass(SonarQubeHelper) >> sonarQubeHelper
        injector.getHelperForClass(MakeHelper) >> makeHelper
        injector.getHelperForClass(CredentialsHelper) >> credentialsHelper
        injector.getHelperForClass(EnvFileHelper) >> envFileHelper
        injector.getHelperForClass(GCloudHelper) >> gcpHelper
        injector.getHelperForClass(AWSHelper) >> awsHelper
        injector.getHelperForClass(K8sSecretHelper) >> k8sSecretHelper
        injector.getHelperForClass(ShellHelper) >> shellHelper
    }

    def 'test build stage for PR'() {
        given:
        def stageName = 'Build'
        def timeout = 5

        def buildEnv = new MakeBuildEnv(ctx, [
                makeStageName   : stageName,
                makeStageEnabled: true,
                makeStageTimeout: timeout,
        ])

        when:
        buildEnv.buildStage()

        then:
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        0 * makeHelper.execute('build', null, [])
    }

    def 'test build stage for deploy dev'() {
        given:
        def stageName = 'Build'
        def timeout = 5

        def buildEnv = new MakeBuildEnv(ctx, [
                serviceConfigurations: [
                        name             : 'service-name',
                        namespace        : 'service-namespace',
                        clusterNamePrefix: 'cluster-prefix-'
                ],
                makeStageName        : stageName,
                makeStageEnabled     : true,
                makeStageTimeout     : timeout,
                makeBuildCommands    : { c, buildEnv ->
                    if (buildEnv.isDevDeploymentEnabled()) {
                        ['build']
                    }
                },
                devDeploymentEnabled  : true,
                prodDeploymentEnabled : false,
        ])

        when:
        buildEnv.buildStage()

        then:
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * makeHelper.execute('build', [])
    }

    def 'test code quality stage'() {
        given:
        def stageName = 'Code Quality'
        def timeout = 5

        def buildEnv = new MakeBuildEnv(ctx, [
                codeQualityStageName   : stageName,
                codeQualityStageEnabled: true,
                codeQualityStageTimeout: timeout,
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

        def buildEnv = new MakeBuildEnv(ctx, [
                unitTestStageEnabled       : true,
                unitTestStageName          : stageName,
                unitTestStageTimeout       : 5,
                integrationTestStageEnabled: true,
                integrationTestStageName   : stageName,
                integrationTestStageTimeout: 5,
                devDeploymentEnabled        : false,
                prodDeploymentEnabled       : false
        ])

        when:
        buildEnv.testStage()

        then:
        2 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        2 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * makeHelper.execute('unit-test', [])
        1 * makeHelper.execute('integration-test', [])
    }

}
