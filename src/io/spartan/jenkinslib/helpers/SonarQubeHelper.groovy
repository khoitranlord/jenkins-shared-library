package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class SonarQubeHelper extends BaseHelper implements Serializable {
    SonarQubeHelper(Script ctx, JenkinsLibProperties libProperties) {
        super(ctx, libProperties)
    }

    void setupSonarQubeScanner() {
        def toolName = libProperties.get('JENKINS_SONARQUBE_TOOL_NAME', String)
        def tool = ctx.tool name: toolName, type: 'hudson.plugins.sonar.SonarRunnerInstallation'
        ctx.env.'PATH' = "$tool/bin:${ctx.env.'PATH'}"
    }

    void wrapWithSonarQubeServer(Closure closure) {
        ctx.withSonarQubeEnv(getConfig('SONARQUBE_SERVER_ID', String)) {
            closure()
        }
    }

    void waitForQualityGate(boolean abortPipeline) {
        ctx.waitForQualityGate abortPipeline: abortPipeline
    }
}
