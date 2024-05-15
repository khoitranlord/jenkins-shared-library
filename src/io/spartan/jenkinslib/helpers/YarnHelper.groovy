package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class YarnHelper extends BaseHelper implements Serializable {
    private ShellHelper shellHelper

    YarnHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
    }

    void setupNodeVersion() {
        def toolName = getConfig('JENKINS_NODE_JS_VERSION', String)
        def tool = ctx.tool name: toolName, type: 'nodejs'
        ctx.env.'PATH' = "$tool/bin:${ctx.env.'PATH'}"
    }

    void execute(String task, List<String> params = [], Map<String, String> env = [:], String directory = null) {
        if (directory) {
            ctx.dir directory, {
                shellHelper.sh buildCommand(task, params, env)
            }
        } else {
            shellHelper.sh buildCommand(task, params, env)
        }
    }

    private static String buildCommand(String task, List<String> params, Map<String, String> env) {
        def envVariable = env.collect {"$it.key=$it.value" }.unique(true).join ' '
        "$envVariable yarn $task ${params.unique(true).join ' '}"
    }
}
