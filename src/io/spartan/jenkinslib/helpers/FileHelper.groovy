package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class FileHelper extends BaseHelper implements Serializable {
    FileHelper(Script ctx, JenkinsLibProperties libProperties) {
        super(ctx, libProperties)
    }

    String readFile(String filePath) {
        def content = ctx.readFile file: filePath, encoding: 'UTF-8'
        return content
    }

    void writeFile(String filePath, String content) {
        ctx.writeFile file: filePath, text: content, encoding: 'UTF-8'
    }

    String processTemplateFile(String filePath, List<String> variables) {
        String fileContent = readFile(filePath)
        def output = variables.inject fileContent, { content, var ->
            def (name, value) = (var =~ /^([^=]+)=(.*)$/)[0][1..2]
            content.replaceAll($/__${name as String}__/$, value as String)
        }
        return output
    }
}
