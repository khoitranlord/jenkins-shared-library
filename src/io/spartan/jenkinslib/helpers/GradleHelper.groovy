package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class GradleHelper extends BaseHelper implements Serializable {
    private ShellHelper shellHelper

    GradleHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
    }

    void execute(String command, List<String> taskParams = [], List<String> gradleParams = []) {
        shellHelper.sh buildGradleCommand(command, gradleParams, taskParams)
    }

    private static String buildGradleCommand(String command, List<String> gradleParams, List<String> taskParams) {
        "./gradlew ${gradleParams.unique(true).join ' '} $command ${taskParams.unique(true).join ' '}"
    }
}
