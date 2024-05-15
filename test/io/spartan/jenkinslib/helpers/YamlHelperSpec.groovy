package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class YamlHelperSpec extends WorkflowScriptSpecification {
    def fileHelper = Mock FileHelper

    @Subject
    def helper = new YamlHelper(ctx, libProperties, fileHelper)


    def "test writeYamlWithVariable method"() {
        given:
        def filePath = "file-path.yaml"
        def variables = ["KEY_1=VALUE_1", "KEY_2=VALUE_2"]
        def expectedContent = 'File content with VALUE_1 and VALUE_2'
        fileHelper.readFile(filePath) >> 'File content with __KEY_1__ and __KEY_2__'

        when:
        def content = helper.writeYamlWithVariable(filePath, variables)

        then:
        content == expectedContent
        1 * fileHelper.processTemplateFile(filePath, variables) >> expectedContent
        1 * fileHelper.writeFile(filePath, expectedContent)
    }
}
