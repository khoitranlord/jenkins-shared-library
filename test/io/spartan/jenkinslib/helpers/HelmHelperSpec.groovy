package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class HelmHelperSpec extends WorkflowScriptSpecification {
    def shellHelper = Mock ShellHelper

    @Subject
    def helper = new HelmHelper(ctx, libProperties, shellHelper)

    def 'test upgradeInstall helm chart'() {
        given:
        def releaseName = 'release-name'
        def chartPath = 'chart-path'
        def namespace = 'namespace'
        def valuesFile = 'values.yaml'
        def valuesExtendFile = 'values.extend.yaml'
        def chartVersion = '0.1.0'

        when:
        helper.upgradeInstall(releaseName, chartPath, namespace, [valuesFile, valuesExtendFile], chartVersion)

        then:
        1 * shellHelper.sh("helm upgrade --install $releaseName $chartPath --create-namespace -n $namespace -f $valuesFile -f $valuesExtendFile --version $chartVersion --timeout 20m --wait")
    }
}
