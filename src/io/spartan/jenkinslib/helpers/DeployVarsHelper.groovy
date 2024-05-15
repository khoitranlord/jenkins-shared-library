package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class DeployVarsHelper extends BaseHelper implements Serializable {
    private AWSHelper awsHelper
    private K8sSecretHelper k8sSecretHelper
    private SsmParameterHelper ssmParameterHelper

    DeployVarsHelper(Script ctx, JenkinsLibProperties libProperties, AWSHelper awsHelper, K8sSecretHelper k8sSecretHelper, SsmParameterHelper ssmParameterHelper) {
        super(ctx, libProperties)
        this.awsHelper = awsHelper
        this.k8sSecretHelper = k8sSecretHelper
        this.ssmParameterHelper = ssmParameterHelper
    }

    Map<String, String> getServiceDeployVars(String deployVarsMode, Map<String, String> serviceConfigurations, String environment, String cloudRegion) {
        if (deployVarsMode == "k8s") {
            awsHelper.command("eks", ["update-kubeconfig", "--name", "${serviceConfigurations.clusterNamePrefix}${environment}"], cloudRegion)

            k8sSecretHelper.readSecretFromCluster(
                    serviceConfigurations.namespace as String ?: "${serviceConfigurations.name}",
                    "${serviceConfigurations.name}-env-var"
            ).data as Map<String, String>
        } else if (deployVarsMode == "ssm") {
            ssmParameterHelper.readSsmValuesFromPath("/$environment/${serviceConfigurations.name}/", cloudRegion)
        } else {
            [:]
        }
    }
}

