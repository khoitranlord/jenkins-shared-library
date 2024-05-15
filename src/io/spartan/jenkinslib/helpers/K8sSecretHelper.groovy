package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class K8sSecretHelper extends BaseHelper implements Serializable {
    private ShellHelper shellHelper

    K8sSecretHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
    }

    Map<String, ?> readSecretFromCluster(String namespace, String secretName) {
        def secretManifest = shellHelper.shForStdout("kubectl${namespace ? " -n $namespace" : ''} get secret $secretName -oyaml")
        ctx.readYaml text: secretManifest
    }
}
