import io.spartan.jenkinslib.models.buildModels.BuildJobEnv
import io.spartan.jenkinslib.models.deployModels.GcpDeploymentEnv
import io.spartan.jenkinslib.models.deployModels.AwsDeploymentEnv
import io.spartan.jenkinslib.models.deployModels.CloudDeploymentEnv

import static groovy.lang.Closure.DELEGATE_ONLY

def <T> T call(Closure<Void> jobConfig) {
    def ctx = jobConfig.owner
    ctx.generalPipeline {
        deployJobEnvClosure = { Script c, Map<String, ?> config ->
            if (config.cloudName == "aws") {
                new AwsDeploymentEnv(c, config)
            } else if (config.cloudName == "gcp") {
                new GcpDeploymentEnv(c, config)
            }
        }
        serviceConfigurations = { Script c, BuildJobEnv env -> }

        additionalContainerConfig = [
            kaniko: [:] // use default env settings
        ]

        buildEnabled = { Script c, BuildJobEnv env ->
            env.isPullRequestBuild() || env.isReleaseBranchBuild()
        }

        devDeploymentEnabled = { Script c, BuildJobEnv env ->
            env.isReleaseBranchBuild()
        }

        prodDeploymentEnabled = { Script c, BuildJobEnv env ->
            env.isTagBuild()
        }

        testStageEnabled = { Script c, BuildJobEnv env ->
            env.isPullRequestBuild()
        }
        testStageTimeout = 30

        codeQualityStageEnabled = false
        codeQualityStageTimeout = 20

        buildStageClosure = { Script c, BuildJobEnv env ->
            if (env.isTerraformBuildEnabled()) {
                env.authenticationStage()
                env.gitCryptStage()
                env.terraformFmtStage()
                env.terraformValidateStage()
                env.terraformPlanStage()
            } else {
                env.buildStage()
                env.testStage()
                env.codeQualityStage()
            }
        }

        deployStageClosure = { Script c, CloudDeploymentEnv env ->
            if (env.getInfraPlatform() == "terraform") {
                env.terraformApplyStage()
            }
            if (env.getServerPlatform() == "staticWebsite") {
                env.staticWebsiteDeployStage()
            }
            else if (env.getServerPlatform() == "k8s"){
                env.helmDeployStage()
            }
        }

        dockerFileName = 'Dockerfile'
        dockerFilePath = '/'
        dockerBuildArgs = [:]

        staticWebsiteStageName = 'Static Website'
        staticWebsiteStageEnabled = { Script c, CloudDeploymentEnv env ->
          env.isEnableDeploy()
        }
        staticWebsiteStageTimeout = 3

        terraformApplyStageName = 'Terraform Apply'
        terraformApplyStageEnabled = { Script c, CloudDeploymentEnv env ->
          env.isEnableDeploy()
        }
        terraformApplyStageTimeout = 20

        helmStageName = 'Deploy Helm'
        helmStageEnabled = { Script c, CloudDeploymentEnv env ->
            env.isEnableDeploy()
        }
        helmStageTimeout = 15
        helmValuesPath = { Script c, CloudDeploymentEnv env ->
            if (env.isDevDeploymentEnabled()) {
                'k8s/dev/values.yaml'
            } else if (env.isProdDeploymentEnabled()) {
                'k8s/prod/values.yaml'
            } else {
                c.error 'unknown environment!! Abort!!'
            }
        }

        helmAdditionalDeployVars = []
        helmAdditionalFilePaths = ['']

        // set to true to use image from dev to deploy production, avoid rebuild
        promoteImageEnabled = false

        // set to true to use image from dev to deploy production, avoid rebuild
        promoteImageEnabled = false

        // set the owner of this closure to the Script object of the Jenkinsfile
        owner = ctx

        // evaluate the config given to this directive. do this last, so later configs overwrite earlier ones
        def closureConfig = delegate
        // delegate the config closure given by the Jenkinsfile to the same delegate this closure is evaluated to
        jobConfig.resolveStrategy = DELEGATE_ONLY
        jobConfig.delegate = closureConfig
        jobConfig()
    }
}
