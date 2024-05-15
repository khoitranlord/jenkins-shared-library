package io.spartan.jenkinslib.models.buildModels

import io.spartan.jenkinslib.helpers.GradleHelper
import io.spartan.jenkinslib.helpers.SonarQubeHelper
import io.spartan.jenkinslib.helpers.TimeoutHelper
import io.spartan.jenkinslib.helpers.WorkflowHelper
import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification

class GradleBuildEnvSpec extends WorkflowScriptSpecification {
    def workflowHelper = Mock WorkflowHelper
    def timeoutHelper = Mock TimeoutHelper
    def gradleHelper = Mock GradleHelper
    def sonarQubeHelper = Mock SonarQubeHelper

    def setup() {
        injector.getHelperForClass(WorkflowHelper) >> workflowHelper
        injector.getHelperForClass(TimeoutHelper) >> timeoutHelper
        injector.getHelperForClass(GradleHelper) >> gradleHelper
        injector.getHelperForClass(SonarQubeHelper) >> sonarQubeHelper
    }

    def 'test gradle build stage for PR'() {
        given:
        def buildStageName = 'Build'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                gradleStageName     : buildStageName,
                gradleStageEnabled  : true,
                gradleStageTimeout  : timeout,
                devDeploymentEnabled : false,
                prodDeploymentEnabled: false,
                cacheEnabled        : true
        ])

        when:
        buildEnv.buildStage()

        then:
        1 * ctx.env.getProperty('BRANCH_NAME') >> 'PR-123'
        1 * workflowHelper.conditionalStage(buildStageName, true, _) >> { stageName, condition, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * gradleHelper.execute('app:example:assemble', [], ['--build-cache'])
    }

    def 'test gradle build stage for release dev'() {
        given:
        def buildStageName = 'Build'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                gradleStageName     : buildStageName,
                gradleStageEnabled  : true,
                gradleStageTimeout  : timeout,
                devDeploymentEnabled : true,
                prodDeploymentEnabled: false,
                cacheEnabled        : true
        ])

        when:
        buildEnv.buildStage()

        then:
        1 * workflowHelper.conditionalStage(buildStageName, true, _) >> { stageName, condition, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * gradleHelper.execute("app:example:shadowJar", ['-x check -x test'], ['--build-cache'])
    }

    def 'test gradle build stage for release prod'() {
        given:
        def buildStageName = 'Build'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                gradleStageName     : buildStageName,
                gradleStageEnabled  : true,
                gradleStageTimeout  : timeout,
                devDeploymentEnabled : false,
                prodDeploymentEnabled: true,
                cacheEnabled        : true
        ])

        when:
        buildEnv.buildStage()

        then:
        1 * workflowHelper.conditionalStage(buildStageName, true, _) >> { stageName, condition, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * gradleHelper.execute('app:example:shadowJar', ['-x check -x test'], ['--build-cache'])
    }

    def 'test gradle build stage for PR with custom build command'() {
        given:
        def buildStageName = 'Build'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                gradleStageName     : buildStageName,
                gradleStageEnabled  : true,
                gradleStageTimeout  : timeout,
                gradleBuildCommands : ['assemble', 'ktlintCheck'],
                devDeploymentEnabled : false,
                prodDeploymentEnabled: false,
                cacheEnabled        : true
        ])

        when:
        buildEnv.buildStage()

        then:
        1 * workflowHelper.conditionalStage(buildStageName, true, _) >> { stageName, condition, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * gradleHelper.execute('assemble', [], ['--build-cache'])
        1 * gradleHelper.execute('ktlintCheck', [], ['--build-cache'])
    }

    def 'test gradle build stage for PR with custom build params'() {
        given:
        def buildStageName = 'Build'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                gradleStageName     : buildStageName,
                gradleStageEnabled  : true,
                gradleStageTimeout  : timeout,
                gradleBuildParams   : ['-x test'],
                devDeploymentEnabled : false,
                prodDeploymentEnabled: false,
                cacheEnabled        : true
        ])

        when:
        buildEnv.buildStage()

        then:
        1 * ctx.env.getProperty('BRANCH_NAME') >> 'PR-123'
        1 * workflowHelper.conditionalStage(buildStageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * gradleHelper.execute('app:example:assemble', ['-x test'], ['--build-cache'])
    }

    def 'test gradle build stage for PR with disable cache'() {
        given:
        def buildStageName = 'Build'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                gradleStageName     : buildStageName,
                gradleStageEnabled  : true,
                gradleStageTimeout  : timeout,
                devDeploymentEnabled : false,
                prodDeploymentEnabled: false,
                cacheEnabled        : false
        ])

        when:
        buildEnv.buildStage()

        then:
        1 * ctx.env.getProperty('BRANCH_NAME') >> 'PR-123'
        1 * workflowHelper.conditionalStage(buildStageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * gradleHelper.execute('app:example:assemble', [], [])
    }

    def 'test codeQualityStage when build PR'() {
        given:
        def stageName = 'Code Quality'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                codeQualityStageName   : stageName,
                codeQualityStageEnabled: true,
                codeQualityStageTimeout: timeout,
                qualityStageEnabled     : true,
                devDeploymentEnabled    : false,
                prodDeploymentEnabled   : false,
                cacheEnabled           : true
        ])

        when:
        buildEnv.codeQualityStage()

        then:
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * sonarQubeHelper.wrapWithSonarQubeServer(_) >> { cls -> cls[0]() }

        and:
        1 * gradleHelper.execute('sonar', [], ['--build-cache'])
    }

    def 'test codeQualityStage when build PR with custom command and params'() {
        given:
        def stageName = 'Code Quality'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                codeQualityStageName   : stageName,
                codeQualityStageEnabled: true,
                codeQualityStageTimeout: timeout,
                codeQualityCommand     : 'sonar-scanner',
                codeQualityParams      : ['-Dparams1', '-Dparams2'],
                qualityStageEnabled     : true,
                devDeploymentEnabled    : false,
                prodDeploymentEnabled   : false,
                cacheEnabled           : true
        ])

        when:
        buildEnv.codeQualityStage()

        then:
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * sonarQubeHelper.wrapWithSonarQubeServer(_) >> { cls -> cls[0]() }

        and:
        1 * gradleHelper.execute('sonar-scanner', ['-Dparams1', '-Dparams2'], ['--build-cache'])
    }

    def 'test testStage when build PR'() {
        given:
        def stageName = 'Test'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                testStageName       : stageName,
                testStageEnabled    : true,
                testStageTimeout    : timeout,
                devDeploymentEnabled : false,
                prodDeploymentEnabled: false,
                cacheEnabled        : true
        ])

        when:
        buildEnv.testStage()

        then:
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * gradleHelper.execute('app:example:test', [], ['--build-cache'])
    }

    def 'test testStage when build PR with custom command and params'() {
        given:
        def stageName = 'Test'
        def timeout = 5
        def buildEnv = new GradleBuildEnv(ctx, [
                serviceConfigurations: [
                    name: "example"
                ],
                testStageName       : stageName,
                testStageEnabled    : true,
                testStageTimeout    : timeout,
                testCommand         : 'test-command',
                testParams          : ['-Dparams1', '-Dparams2'],
                devDeploymentEnabled : false,
                prodDeploymentEnabled: false,
                cacheEnabled        : true
        ])

        when:
        buildEnv.testStage()

        then:
        1 * workflowHelper.conditionalStage(stageName, true, _) >> { s, c, cls -> cls() }
        1 * timeoutHelper.withTimeout(timeout, _ as Closure) >> { t, cls -> cls() }

        and:
        1 * gradleHelper.execute('test-command', ['-Dparams1', '-Dparams2'], ['--build-cache'])
    }
}
