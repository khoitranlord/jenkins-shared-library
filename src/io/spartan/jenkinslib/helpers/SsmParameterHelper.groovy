package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class SsmParameterHelper extends BaseHelper implements Serializable {
    private AWSHelper awsHelper

    SsmParameterHelper(Script ctx, JenkinsLibProperties libProperties, AWSHelper awsHelper) {
        super(ctx, libProperties)
        this.awsHelper = awsHelper
    }

    Map<String, String> readSsmValuesFromPath(String path, String awsRegion) {
        def values = ctx.readJSON text: awsHelper.command("ssm", ["get-parameters-by-path", "--path", "'$path'", "--with-decryption"], awsRegion)
        (values["Parameters"] as List).collectEntries { param ->
            [(param["Name"] as String).tokenize('/').last(), param["Value"] as String]
        } as Map<String, String>
    }
}
