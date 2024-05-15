package io.spartan.jenkinslib.models.buildModels


import io.spartan.jenkinslib.models.JenkinsBuildJobEnv

class TerraformPlanEnv extends JenkinsBuildJobEnv implements Serializable {
  TerraformPlanEnv(Script ctx, Map<String, ?> jobConfig) {
    super(ctx, jobConfig)
  }

  List<String> workingDirs() {
    return getWorkingDirs(enableIgnoreChanges)
  }

  void authenticationStage() {
    stageScaffold('authentication') {
      workingDirs().each { workingDir ->
        def environment = workingDir.tokenize('/')[-1]
        authenticationStage(environment)
      }
    }
  }

  void gitCryptStage() {
    stageScaffold('gitCrypt') {
      workingDirs().each { workingDir ->
        def environment = workingDir.tokenize('/')[-1]
        gitCryptStage(environment)
      }
    }
  }

  void terraformFmtStage() {
    stageScaffold('terraformFmt') {
      workingDirs().each { workingDir ->
        executeTerraformCommand('fmt', ['-check'], workingDir)
      }
    }
  }

  void terraformInitStage() {
    stageScaffold('terraformInit') {
      workingDirs().each { workingDir ->
        terraformInitStage(workingDir)
      }
    }
  }

  void terraformValidateStage() {
    stageScaffold('terraformValidate') {
      workingDirs().each { workingDir ->
        executeTerraformCommand('validate', ['-no-color'], workingDir)
      }
    }
  }

  void terraformPlanStage() {
    stageScaffold('terraformPlan') {
      workingDirs().each { workingDir ->
        executeTerraformCommand('plan', ['-input=false', '-no-color', '-lock-timeout=300s', '-refresh=false'], workingDir)
      }
    }
  }
}
