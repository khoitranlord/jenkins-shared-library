package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class DeployVarsHelperSpec extends WorkflowScriptSpecification {
    def awsHelper = Mock AWSHelper
    def k8sSecretHelper = Mock K8sSecretHelper
    def ssmParameterHelper = Mock SsmParameterHelper


    @Subject
    def helper = new DeployVarsHelper(ctx, libProperties, awsHelper, k8sSecretHelper, ssmParameterHelper)

    def "test get service deploy vars with k8s mode"() {
        given:
        def serviceConfigurations = [name: "serviceName", clusterNamePrefix: "cluster-", namespace: "namespace"]
        def environment = "dev"
        def cloudRegion = "us-west-2"
        def deployVarsMode = "k8s"

        when:
        def result = helper.getServiceDeployVars(deployVarsMode, serviceConfigurations, environment, cloudRegion)

        then:
        1 * awsHelper.command("eks", ["update-kubeconfig", "--name", "${serviceConfigurations.clusterNamePrefix}${environment}"], cloudRegion) >> null
        1 * k8sSecretHelper.readSecretFromCluster(serviceConfigurations.namespace ?: serviceConfigurations.name, "${serviceConfigurations.name}-env-var") >> [
            data: ["key1": "value1", "key2": "value2"]
        ]
        result == ["key1": "value1", "key2": "value2"]
    }

    def "test get service deploy vars with ssm mode"() {
        given:
        def serviceConfigurations = [name: "serviceName"]
        def environment = "dev"
        def cloudRegion = "us-west-2"
        def deployVarsMode = "ssm"

        when:
        def result = helper.getServiceDeployVars(deployVarsMode, serviceConfigurations, environment, cloudRegion)

        then:
        1 * ssmParameterHelper.readSsmValuesFromPath("/$environment/${serviceConfigurations.name}/", cloudRegion) >> ["key1": "value1", "key2": "value2"]
        result == ["key1": "value1", "key2": "value2"]
    }
}
