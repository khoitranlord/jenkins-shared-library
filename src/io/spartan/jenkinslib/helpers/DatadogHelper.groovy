package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class DatadogHelper extends BaseHelper implements Serializable {
    private CredentialsHelper credentialsHelper

    DatadogHelper(Script ctx, JenkinsLibProperties libProperties, CredentialsHelper credentialsHelper) {
        super(ctx, libProperties)
        this.credentialsHelper = credentialsHelper
    }

    List<String> getWebConfig(Script ctx, String serviceName) {
        credentialsHelper.withCredentials([
                [type: 'string', credentialsId: "${serviceName}-dd-application-id", variable: 'ddApplicationId'],
                [type: 'string', credentialsId: "${serviceName}-dd-client-token", variable: 'ddClientToken'],
                [type: 'string', credentialsId: "${serviceName}-dd-service", variable: 'ddService'],
        ]) {
            return [
                "DD_APPLICATION_ID=${ctx.env.'ddApplicationId'}",
                "DD_CLIENT_TOKEN=${ctx.env.'ddClientToken'}",
                "DD_SERVICE=${ctx.env.'ddService'}"
            ]
        } as List<String>

    }
}
