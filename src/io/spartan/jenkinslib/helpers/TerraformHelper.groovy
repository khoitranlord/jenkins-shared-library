package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class TerraformHelper extends BaseHelper implements Serializable {
    private ShellHelper shellHelper
    private CredentialsHelper credentialsHelper
    private GitHelper gitHelper

    TerraformHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper, CredentialsHelper credentialsHelper, GitHelper gitHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
        this.credentialsHelper = credentialsHelper
        this.gitHelper = gitHelper
    }

    void execute(String command, List params, String directory = null, terragruntEnabled = false) {
        if (directory) {
            ctx.dir directory, {
                shellHelper.sh terraformExecute(command, params, terragruntEnabled)
            }
        } else {
            shellHelper.sh terraformExecute(command, params, terragruntEnabled)
        }
    }

    private static String terraformExecute(String command, List params, terragruntEnabled = false) {
        def binary = "terraform"
        if (terragruntEnabled) {
            binary = "terragrunt run-all --terragrunt-non-interactive --terragrunt-include-external-dependencies"
            if (command == "fmt") {
                command = "hclfmt"
                binary = "terragrunt --terragrunt-check --terragrunt-non-interactive --terragrunt-include-external-dependencies"
                params = []
            }
        }
        "$binary $command ${params.unique(true).join ' '}"
    }

    def authenticate() {
        credentialsHelper.withCredentials([
                [type: 'usernamePassword', credentialsId: "github-token",  usernameVariable: 'githubUsername', passwordVariable: 'githubPassword'],
                [type: 'string', credentialsId: "terraform-repository", variable: 'terraformRepository'],
                [type: 'string', credentialsId: "github-org", variable: 'githubOrg']
        ]) {
            def githubOrg = ctx.env.'githubOrg'
            def terraformRepository = ctx.env.'terraformRepository'
            def githubUsername = ctx.env.'githubUsername'
            def githubPassword = ctx.env.'githubPassword'
            gitHelper.authenticate(githubUsername as String, githubPassword as String, terraformRepository as String, githubOrg as String)
        }
    }
}
