package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class FileHelperSpec extends WorkflowScriptSpecification {

    @Subject
    def helper = new FileHelper(ctx, libProperties)

    def "test read file"() {
        given:
        def filePath = "file-path"

        when:
        def result = helper.readFile(filePath)

        then:
        1 * ctx.readFile(file: filePath, encoding: 'UTF-8') >> "File content"
        result == "File content"
    }

    def "test write file"() {
        given:
        def filePath = "file-path"
        def content = "File content"

        when:
        helper.writeFile(filePath, content)

        then:
        1 * ctx.writeFile(file: filePath, text: content, encoding: 'UTF-8')
    }

    def "test process template file"() {
        given:
        def filePath = "file-path"
        def variables = ["KEY_1=VALUE_1", "KEY_2=VALUE_2"]

        when:
        def result = helper.processTemplateFile(filePath, variables)

        then:
        1 * ctx.readFile(file: filePath, encoding: 'UTF-8') >> 'File content with __KEY_1__ and __KEY_2__'
        result == 'File content with VALUE_1 and VALUE_2'
    }
}
