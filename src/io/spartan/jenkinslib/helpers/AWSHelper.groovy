package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class AWSHelper extends BaseHelper implements Serializable {
    private CredentialsHelper credentialsHelper
    private ShellHelper shellHelper

    AWSHelper(Script ctx, JenkinsLibProperties libProperties, CredentialsHelper credentialsHelper, ShellHelper shellHelper) {
        super(ctx, libProperties)
        this.credentialsHelper = credentialsHelper
        this.shellHelper = shellHelper
    }

    void connect(Script ctx, String environment) {
        try {
            credentialsHelper.withCredentials([
                    [type: 'string', credentialsId: "aws-account-id-${environment}", variable: 'awsAccountId'],
                    [type: 'file', credentialsId: "aws-web-identity-token-file", variable: 'awsWebIdentityTokenFile'],
            ]) {
                def oidcRole = getOidcRole(ctx.env.'awsAccountId')

                shellHelper.sh("mv \$awsWebIdentityTokenFile /tmp/oidc-role-token")

                ctx.env.'AWS_ROLE_ARN' = oidcRole
                ctx.env.'AWS_WEB_IDENTITY_TOKEN_FILE' = '/tmp/oidc-role-token'
                ctx.env.'AWS_ROLE_SESSION_NAME' = 'jenkins-oidc-session'
            }
        } catch (e) {
            ctx.log.info "${e.message}. switch to use 'aws-credentials'."
            credentialsHelper.withCredentials([
                    [type: 'usernamePassword', credentialsId: "aws-credentials", usernameVariable: 'awsAccessKeyId', passwordVariable: 'awsSecretAccessKey'],
            ]) {
                ctx.env.'AWS_ACCESS_KEY_ID' = ctx.env.'awsAccessKeyId'
                ctx.env.'AWS_SECRET_ACCESS_KEY' = ctx.env.'awsSecretAccessKey'
            }
        }

    }

    String command(String command, List params = [], String awsRegion = "us-west-2") {
        return shellHelper.shForStdout("aws $command ${params.join(' ')} --region $awsRegion")
    }

    String getEcrUrl(String environment, String awsRegion = "us-west-2") {
        credentialsHelper.withCredentials([
                [type: 'string', credentialsId: "aws-account-id-${environment}", variable: 'awsAccountId'],
        ]) {
            return "${ctx.env.'awsAccountId'}.dkr.ecr.${awsRegion}.amazonaws.com"
        }
    }

    String getOidcRole(String awsAccountId) {
      credentialsHelper.withCredentials([
          [type: 'string', credentialsId: "jenkins-oidc-name", variable: 'jenkinsOidcName'],
      ]) {
          return "arn:aws:iam::${awsAccountId}:role/${ctx.env.'jenkinsOidcName'}"
      }
    }

    void copyToS3(String sourceDir, String s3Bucket, String awsRegion) {
        command("s3", ["sync", "$sourceDir/", "s3://$s3Bucket", "--delete"], awsRegion)
    }

    void invalidateCloudFront(String cloudfrontId, String path, String awsRegion) {
        command("cloudfront", ["create-invalidation", "--distribution-id", cloudfrontId, "--paths", "'$path'"], awsRegion)
    }
}
