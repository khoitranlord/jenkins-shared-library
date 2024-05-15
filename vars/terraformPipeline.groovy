import io.spartan.jenkinslib.models.buildModels.TerraformPlanEnv

import static groovy.lang.Closure.DELEGATE_ONLY

def <T> T call(Closure<Void> jobConfig) {
    Script ctx = jobConfig.owner
    ctx.buildPipeline {
        buildJobEnvClosure = { Script c, Map<String, ?> config -> new TerraformPlanEnv(c, config) }

        terraformBuildEnabled = true

        gitCryptStageName = 'Git Crypt'
        gitCryptStageEnabled = false
        gitCryptStageTimeout = 2

        authenticationStageName = 'Authentication'
        authenticationStageEnabled = true
        authenticationStageTimeout = 3

        terraformFmtStageName = 'Terraform Fmt'
        terraformFmtStageEnabled = { Script c, TerraformPlanEnv env ->
            env.isPullRequestBuild()
        }
        terraformFmtStageTimeout = 3

        terraformPlanStageName = 'Terraform Plan'
        terraformPlanStageEnabled = { Script c, TerraformPlanEnv env ->
            env.isPullRequestBuild()
        }
        terraformPlanStageTimeout = 5

        terraformValidateStageName = 'Terraform Validate'
        terraformValidateStageEnabled = { Script c, TerraformPlanEnv env ->
            env.isPullRequestBuild()
        }
        terraformValidateStageTimeout = 3

        infraPlatform = "terraform"

        owner = ctx
        // evaluate the config given to this directive. do this last, so later configs overwrite earlier ones
        def closureConfig = delegate
        // delegate the config closure given by the Jenkinsfile to the same delegate this closure is evaluated to
        jobConfig.resolveStrategy = DELEGATE_ONLY
        jobConfig.delegate = closureConfig
        jobConfig()
    }
}
