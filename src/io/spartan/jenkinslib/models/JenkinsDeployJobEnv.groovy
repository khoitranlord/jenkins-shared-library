package io.spartan.jenkinslib.models

class JenkinsDeployJobEnv extends JenkinsJobEnv implements Serializable {

    JenkinsDeployJobEnv(Script ctx, Map<String, ?> jobConfig) {
        super(ctx, jobConfig)
    }

    void deployStageClosure() {
        ctx.log.info 'executing deploy closure on node'
        getJobConfig('deployStageClosure', Closure).call(ctx, this)
    }
}
