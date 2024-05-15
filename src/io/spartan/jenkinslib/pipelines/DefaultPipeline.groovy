package io.spartan.jenkinslib.pipelines

import io.spartan.jenkinslib.JenkinsLibrary
import io.spartan.jenkinslib.helpers.NodeHelper
import io.spartan.jenkinslib.helpers.PodTemplateHelper
import io.spartan.jenkinslib.models.JenkinsBuildJobEnv
import io.spartan.jenkinslib.models.JenkinsDeployJobEnv

import static groovy.lang.Closure.DELEGATE_ONLY

class DefaultPipeline {
    static void defaultPipeline(Closure<Void> pipelineClosure) {
        // the closure owner is the Script object of the Jenkinsfile
        def ctx = pipelineClosure.owner as Script

        // get job configuration from pipeline closure
        def jobConfigs = [:]
        pipelineClosure.resolveStrategy = DELEGATE_ONLY
        pipelineClosure.delegate = jobConfigs
        pipelineClosure()

        JenkinsBuildJobEnv buildJobEnv
        if (jobConfigs.buildJobEnvClosure) {
            buildJobEnv = (jobConfigs.buildJobEnvClosure as Closure<JenkinsBuildJobEnv>).call(ctx, jobConfigs)
        } else {
            buildJobEnv = new JenkinsBuildJobEnv(ctx, jobConfigs)
        }

        JenkinsDeployJobEnv deployJobEnv
        if (jobConfigs.deployJobEnvClosure) {
            deployJobEnv = (jobConfigs.deployJobEnvClosure as Closure<JenkinsDeployJobEnv>).call(ctx, jobConfigs)
        } else {
            deployJobEnv = new JenkinsDeployJobEnv(ctx, jobConfigs)
        }

        if (!buildJobEnv.isEnableBuild() && !deployJobEnv.isEnableDeploy()) {
            if (!ctx.currentBuild.result) {
                // notify the non build branch as success
                ctx.currentBuild.result = 'SUCCESS'
            }
            return
        }

        ctx.timestamps {
            try{
                JenkinsLibrary.init ctx, jobConfigs.extraLibProperties as Map<String, ?>
                def injector = JenkinsLibrary.injector
                def nodeHelper = injector.getHelperForClass NodeHelper
                def podTemplateHelper = injector.getHelperForClass PodTemplateHelper

                podTemplateHelper.podTemplate buildJobEnv.getJobConfig('nodeBuildLabel', String) as String, buildJobEnv.getJobConfig('additionalContainerConfig', Map), {
                    nodeHelper.node ctx.env.'POD_LABEL' as String, {
                        try {
                            ctx.checkout ctx.scm
                            buildJobEnv.setupStage()
                            if (buildJobEnv.isEnableBuild() || (deployJobEnv.isProdDeploymentEnabled() && !deployJobEnv.isPromoteImageEnabled())) {
                                buildJobEnv.buildStageClosure()
                            }
                            if (deployJobEnv.isEnableDeploy()) {
                                deployJobEnv.deployStageClosure()
                            }
                        } catch (e) {
                            if (ctx.currentBuild.result in [null, 'SUCCESS']) {
                                ctx.currentBuild.result = 'FAILURE'
                            }
                            throw e
                        } finally {
                            deployJobEnv.informStageClosure()
                        }
                    }
                }
            } catch (e) {
                ctx.log.error "job failed: ${e.message ?: '<EMPTY>'}"
            }
        }

    }
}
