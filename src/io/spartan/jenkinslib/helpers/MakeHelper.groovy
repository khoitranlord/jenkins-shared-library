package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class MakeHelper extends BaseHelper implements Serializable {
    private ShellHelper shellHelper

    MakeHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
    }

    void execute(String command, List<String> makeParams = [], Map<String, String> env = [:]) {
        shellHelper.sh buildMakeCommand(command, makeParams, env)
    }

    private String buildMakeCommand(String command, List<String> makeParams, Map<String, String> env) {
        def envVariable = env.collect { "$it.key=$it.value" }.unique(true).join ' '
        "${envVariable} make $command ${makeParams.unique(true).join ' '}"
    }
}
