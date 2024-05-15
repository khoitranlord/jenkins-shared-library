package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties
import io.spartan.jenkinslib.clusters.Cluster

class GCloudHelper extends BaseHelper implements Serializable {
    private CredentialsHelper credentialsHelper
    private ShellHelper shellHelper
    private FileHelper fileHelper

    GCloudHelper(Script ctx, JenkinsLibProperties libProperties, CredentialsHelper credentialsHelper, ShellHelper shellHelper, FileHelper fileHelper) {
        super(ctx, libProperties)
        this.credentialsHelper = credentialsHelper
        this.shellHelper = shellHelper
        this.fileHelper = fileHelper
    }

    void connectAndGetCredentials(String oidcTokenFile, String workloadIdentityFile, String projectId, String region, String clusterName) {
        credentialsHelper.withCredentials([
                [type: 'file', credentialsId: oidcTokenFile, variable: 'ID_TOKEN_FILE_PATH'],
                [type: 'file', credentialsId: workloadIdentityFile, variable: 'CRED_JSON_FILE'],
                [type: 'string', credentialsId: projectId, variable: 'PROJECT_ID'],
                [type: 'string', credentialsId: region, variable: 'REGION'],
                [type: 'string', credentialsId: clusterName, variable: 'CLUSTER_NAME'],
        ]) {
            def templateJsonFile = fileHelper.readFile(ctx.env.'CRED_JSON_FILE') as String
            def credsFileLocation = "${ctx.env.'WORKSPACE'}/creds.json"

            def replacedTemplate = ['ID_TOKEN_FILE_PATH': ctx.env.'ID_TOKEN_FILE_PATH'].inject(templateJsonFile) { content, entry ->
                content.replaceAll($/__${entry.key}__/$, entry.value as String)
            }

            fileHelper.writeFile(credsFileLocation, replacedTemplate)

            auth("login --cred-file=$credsFileLocation")

            config("set project ${ctx.env.'PROJECT_ID'}")
            container("clusters get-credentials ${ctx.env.'CLUSTER_NAME'}", ["--region=${ctx.env.'REGION'}"])
        }
    }

    void connectAndGetCredentials(String environment) {
        connectAndGetCredentials("$Cluster.TOKEN_OIDC_FILE_BASENAME-$environment",
                "$Cluster.WORKLOAD_IDENTITY_FILE_BASENAME-$environment",
                "$Cluster.GCLOUD_PROJECT_ID-$environment",
                "$Cluster.GCLOUD_REGION-$environment",
                "$Cluster.GCLOUD_K8S_CLUSTER-$environment")
    }

    String auth(String command, List params = []) {
        ctx.log.info 'executing gcloud auth'
        gcloud("auth $command", params)
    }

    String config(String command, List params = []) {
        ctx.log.info 'executing gcloud config'
        gcloud("config $command", params)
    }

    String container(String command, List params = []) {
        ctx.log.info 'executing gcloud container'
        gcloud("container $command", params)
    }

    String gcloud(String command, List params = []) {
        shellHelper.shForStdout "gcloud $command ${params.join(' ')}"
    }
}
