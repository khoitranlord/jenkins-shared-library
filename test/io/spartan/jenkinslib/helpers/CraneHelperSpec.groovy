package io.spartan.jenkinslib.helpers

import io.spartan.jenkinslib.testsupport.WorkflowScriptSpecification
import spock.lang.Subject

class CraneHelperSpec extends WorkflowScriptSpecification {
    def shellHelper = Mock ShellHelper
    def fileHelper = Mock FileHelper

    @Subject
    def helper = new CraneHelper(ctx, libProperties, shellHelper, fileHelper)

    def 'test login with registry'() {
        given:
        def registry = 'image.registry.test'
        def username = 'username'
        def password = 'secret'

        when:
        helper.login(registry, username, password)

        then:
        1 * fileHelper.writeFile('password', password)
        1 * shellHelper.sh("cat password | crane auth login $registry -u $username --password-stdin")
        1 * ctx.fileExists('password') >> fileExist
        expectedCleanupCall * shellHelper.sh('rm password')

        where:
        fileExist || expectedCleanupCall
        true      || 1
        false     || 0
    }

    def 'test crane pull command'() {
        given:
        def repo = 'image.registry.test/image-group'
        def imageName = 'image-name'
        def imageTag = 'image-tag'

        when:
        helper.pull(repo, imageName, imageTag)

        then:
        1 * shellHelper.sh("crane pull $repo/$imageName:$imageTag $imageName")
    }

    def 'test crane push command'() {
        given:
        def repo = 'image.registry.test/image-group'
        def imageName = 'image-name'
        def imageTag = 'image-tag'

        when:
        helper.push(repo, imageName, imageTag)

        then:
        1 * shellHelper.sh("crane push $imageName $repo/$imageName:$imageTag")
    }
}
