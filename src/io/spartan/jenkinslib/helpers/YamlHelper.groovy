package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class YamlHelper extends BaseHelper implements Serializable {
    private FileHelper fileHelper

    YamlHelper(Script ctx, JenkinsLibProperties libProperties, FileHelper fileHelper) {
        super(ctx, libProperties)
        this.fileHelper = fileHelper
    }

    String writeYamlWithVariable(String manifestYamlFilePath, List<String> deployVariables) {
        def content = fileHelper.processTemplateFile(manifestYamlFilePath, deployVariables)
        fileHelper.writeFile(manifestYamlFilePath, content)
        return content
    }
}
