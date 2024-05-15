package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class TerraformHelperSpec extends WorkflowScriptSpecification {
    def shellHelper = Mock ShellHelper
    def credentialsHelper = Mock CredentialsHelper
    def gitHelper = Mock GitHelper

    @Subject
    def helper = new TerraformHelper(ctx, libProperties, shellHelper, credentialsHelper, gitHelper)

    def 'test terraform execute'() {
        given:
        def command = 'apply'
        def args = ['--auto-approve']

        when:
        helper.execute(command, args, directory)

        then:
        expectedSwitchDir * ctx.dir(directory, _ as Closure) >> { _, cls -> cls() }

        and:
        1 * shellHelper.sh("terraform $command --auto-approve")

        where:
        directory                   || expectedSwitchDir
        'terraform/environment/dev' || 1
        null                        || 0
    }

    def 'test terragrunt execute'() {
        given:
        def command = 'apply'
        def args = []

        when:
        helper.execute(command, args, directory, true)

        then:
        expectedSwitchDir * ctx.dir(directory, _ as Closure) >> { _, cls -> cls() }

        and:
        1 * shellHelper.sh("terragrunt run-all --terragrunt-non-interactive --terragrunt-include-external-dependencies $command ")

        where:
        directory                   || expectedSwitchDir
        'terraform/environment/dev' || 1
        null                        || 0
    }

    def 'test terragrunt fmt execute'() {
        given:
        def command = 'fmt'
        def args = []

        when:
        helper.execute(command, args, directory, true)

        then:
        expectedSwitchDir * ctx.dir(directory, _ as Closure) >> { _, cls -> cls() }

        and:
        1 * shellHelper.sh("terragrunt --terragrunt-check --terragrunt-non-interactive --terragrunt-include-external-dependencies hclfmt ")

        where:
        directory                   || expectedSwitchDir
        'terraform/environment/dev' || 1
        null                        || 0
    }
}
