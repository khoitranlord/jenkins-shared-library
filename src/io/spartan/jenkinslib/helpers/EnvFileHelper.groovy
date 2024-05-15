package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class EnvFileHelper extends BaseHelper implements Serializable {
    private FileHelper fileHelper

    EnvFileHelper(Script ctx, JenkinsLibProperties libProperties, FileHelper fileHelper) {
        super(ctx, libProperties)
        this.fileHelper = fileHelper
    }

    void writeEnvFileWithVariable(String envFilePath, List<String> variables) {
        ctx.log.info "replacing placeholder in $envFilePath"
        if (ctx.fileExists(envFilePath)) {
            fileHelper.writeFile(envFilePath, fileHelper.processTemplateFile(envFilePath, variables))
        } else {
            ctx.log.info "cannot find file $envFilePath, skipped replace place holder"
        }
    }
}
