package io.spartan.jenkinslib.models

class JenkinsBuildJobEnv extends JenkinsJobEnv implements Serializable {
    String jobName

    int buildNumber
    String buildUrl

    JenkinsBuildJobEnv(Script ctx, Map<String, ?> jobConfig = [:]) {
        super(ctx, jobConfig)

        jobName = env.'JOB_NAME'

        buildNumber = env.'BUILD_NUMBER'.toInteger()
        buildUrl = env.'BUILD_URL'
    }

    void buildStageClosure() {
        ctx.log.info 'executing build closure on node'
        getJobConfig('buildStageClosure', Closure).call(ctx, this)
    }
}
