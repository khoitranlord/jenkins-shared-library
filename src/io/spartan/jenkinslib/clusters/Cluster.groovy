package io.spartan.jenkinslib.clusters

final class Cluster {
    static List getCredentialsEnv(String environment) {
        [
                [type: 'string', credentialsId: "$GCLOUD_K8S_CLUSTER-$environment", variable: 'GCLOUD_K8S_CLUSTER'],
                [type: 'string', credentialsId: "$GCLOUD_PROJECT_ID-$environment", variable: 'GCLOUD_PROJECT_ID'],
                [type: 'string', credentialsId: "$GCLOUD_PROJECT_ID-$environment", variable: 'GCP_PROJECT'],
                [type: 'string', credentialsId: "$GCLOUD_REGION-$environment", variable: 'GCLOUD_REGION'],
                [type: 'string', credentialsId: "$GCLOUD_REGION-$environment", variable: 'GCP_REGION'],
                [type: 'string', credentialsId: "$BASE_DOMAIN-$environment", variable: 'BASE_DOMAIN'],
                [type: 'string', credentialsId: "$WORKLOAD_IDENTITY_PROVIDER-$environment", variable: 'WORKLOAD_IDENTITY_PROVIDER'],
                [type: 'string', credentialsId: "$PIPELINE_OPS_SERVICE_ACCOUNT-$environment", variable: 'PIPELINE_OPS_SERVICE_ACCOUNT'],
                [type: 'string', credentialsId: PAT, variable: 'GITHUB_TOKEN'],
                [type: 'string', credentialsId: "helm-chart-url", variable: 'HELM_CHART_URL'],
        ]
    }

    static final String PAT = 'pat'
    static final String BASE_DOMAIN = 'base-domain'
    static final String GCLOUD_K8S_CLUSTER = 'gcloud-k8s-cluster'
    static final String GCLOUD_PROJECT_ID = 'gcloud-project-id'
    static final String GCLOUD_REGION = 'gcloud-region'

    static final String WORKLOAD_IDENTITY_PROVIDER = 'workload-identity-provider'
    static final String PIPELINE_OPS_SERVICE_ACCOUNT = 'pipeline-ops-service-account'

    static final String TOKEN_OIDC_FILE_BASENAME = 'oidc-id-token-file'
    static final String WORKLOAD_IDENTITY_FILE_BASENAME = 'workload-identity-file'

}
