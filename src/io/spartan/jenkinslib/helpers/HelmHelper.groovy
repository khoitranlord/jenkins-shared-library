package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.JenkinsLibProperties

class HelmHelper extends BaseHelper implements Serializable {
    ShellHelper shellHelper

    HelmHelper(Script ctx, JenkinsLibProperties libProperties, ShellHelper shellHelper) {
        super(ctx, libProperties)
        this.shellHelper = shellHelper
    }

    void repoAdd(String name, String url) {
        doHelm('repo add', [name, url])
    }

    void upgradeInstall(String releaseName, String chartPath, String namespace, List valuesPaths, String chartVersion) {
        def command = 'upgrade --install'
        def namespaceParam = namespace ? "--create-namespace -n $namespace" : ''
        def valuesPathParams = valuesPaths.size() > 0 ? valuesPaths.findAll { it }.collect { "-f $it" }.join(' ') : ''
        def chartVersionParams = chartVersion ? "--version $chartVersion" : ''
        def helmTimeout = '--timeout 20m'
        def waitDeploySuccess = '--wait'

        doHelm(command, [releaseName, chartPath, namespaceParam, valuesPathParams, chartVersionParams, helmTimeout, waitDeploySuccess])
    }

    void doHelm(String command, List params = []) {
        shellHelper.sh "helm $command ${params.join(' ')}"
    }
}
