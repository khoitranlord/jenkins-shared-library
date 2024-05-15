package io.spartan.jenkinslib.models.buildModels

import io.spartan.jenkinslib.models.JenkinsBuildJobEnv

abstract class BuildJobEnv extends JenkinsBuildJobEnv implements Serializable {
    BuildJobEnv(Script ctx, Map<String, ?> jobConfig) {
        super(ctx, jobConfig)
    }

    abstract void buildStage()

    abstract void codeQualityStage()

    abstract void testStage()
}
