package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

import static io.spartan.jenkinslib.clusters.Cluster.*

class GCloudHelperSpec extends WorkflowScriptSpecification {
    def credentialsHelper = Mock CredentialsHelper
    def shellHelper = Mock ShellHelper
    def fileHelper = Mock FileHelper

    @Subject
    def helper = new GCloudHelper(ctx, libProperties, credentialsHelper, shellHelper, fileHelper)

    def 'test connectAndGetCredentials on environment'() {
        given:
        def environment = 'dev'
        def credFilePath = '/tmp/cred-path'
        def idTokenFilePath = '/tmp/token-path'
        def currentWorkSpace = '/home/agent/build-dir'
        def credFileLocation = "$currentWorkSpace/creds.json"
        def projectId = 'project-id'
        def clusterName = 'cluster-name'
        def region = 'region'

        when:
        helper.connectAndGetCredentials(environment)

        then:
        1 * credentialsHelper.withCredentials(_ as List, _ as Closure) >> { list, cls ->
            assert list == [
                    [type: 'file', credentialsId: "$TOKEN_OIDC_FILE_BASENAME-$environment", variable: 'ID_TOKEN_FILE_PATH'],
                    [type: 'file', credentialsId: "$WORKLOAD_IDENTITY_FILE_BASENAME-$environment", variable: 'CRED_JSON_FILE'],
                    [type: 'string', credentialsId: "$GCLOUD_PROJECT_ID-$environment", variable: 'PROJECT_ID'],
                    [type: 'string', credentialsId: "$GCLOUD_REGION-$environment", variable: 'REGION'],
                    [type: 'string', credentialsId: "$GCLOUD_K8S_CLUSTER-$environment", variable: 'CLUSTER_NAME']
            ]
            cls()
        }

        and:
        1 * ctx.env.getProperty('CRED_JSON_FILE') >> credFilePath
        1 * ctx.env.getProperty('WORKSPACE') >> currentWorkSpace
        1 * ctx.env.getProperty('PROJECT_ID') >> projectId
        1 * ctx.env.getProperty('CLUSTER_NAME') >> clusterName
        1 * ctx.env.getProperty('REGION') >> region
        1 * ctx.env.getProperty('ID_TOKEN_FILE_PATH') >> idTokenFilePath

        1 * fileHelper.readFile({ filePath -> filePath == credFilePath}) >> '{"ID_TOKEN_FILE_PATH": "__ID_TOKEN_FILE_PATH__"}'

        and:
        1 * fileHelper.writeFile(
            credFileLocation,
            /{"ID_TOKEN_FILE_PATH": "$idTokenFilePath"}/
        )

        and:
        1 * shellHelper.shForStdout("gcloud auth login --cred-file=$credFileLocation ")
        1 * shellHelper.shForStdout("gcloud config set project $projectId ")
        1 * shellHelper.shForStdout("gcloud container clusters get-credentials $clusterName --region=$region")
    }
}

