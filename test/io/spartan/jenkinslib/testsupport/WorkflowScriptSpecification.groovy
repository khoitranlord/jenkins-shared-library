package io.spartan.jenkinslib.testsupport

import io.spartan.jenkinslib.JenkinsLibInjector
import io.spartan.jenkinslib.JenkinsLibProperties
import io.spartan.jenkinslib.JenkinsLibrary
import spock.lang.Specification

abstract class WorkflowScriptSpecification extends Specification {
	def setup() {
		JenkinsLibrary.LIB_PROPERTIES = libProperties
		JenkinsLibrary.INJECTOR = injector
	}


	def env = Mock(GroovyObjectSupport) {
		getProperty('JOB_NAME') >> 'JOB_NAME_VALUE'
		getProperty('BUILD_NUMBER') >> '1'
		getProperty('BUILD_URL') >> 'BUILD_URL_VALUE'
	}

	// this should actually be Mock(Run), but it's harder to mock it
	def currentBuild = Mock(Map)

	def log = Mock(WorkflowScriptLog) {
		debug(_) >> { args -> ctx.echo args[0] }
		info(_) >> { args -> ctx.echo args[0] }
		warn(_) >> { args -> ctx.echo args[0] }
		error(_) >> { args -> ctx.echo args[0] }
	}

	def ctx = Mock(WorkflowScript) {
		error(*_) >> { args ->
			throw new RuntimeException(*args)
		}

		node(_ as String, _ as Closure) >> { String label, Closure c ->
			c()
		}

		timestamps(_ as Closure) >> { Closure c ->
			c()
		}

		timeout(_ as Map, _ as Closure) >> { _, Closure c ->
			c()
		}

		waitUntil(_ as Closure) >> { Closure c ->
			c()
		}

		wrap(_ as Map, _ as Closure) >> { _, Closure c ->
			c()
		}

		withEnv(_ as List<String>, _ as Closure) >> { _, Closure c ->
			c()
		}

		sleep(_ as Map<String, ?>) >> { Map<String, ?> options ->
			// no-op
		}

		getProperty('env') >> env
		getProperty('currentBuild') >> currentBuild
		getProperty('log') >> log
	}

	def libProperties = Mock JenkinsLibProperties
	def injector = Mock(JenkinsLibInjector) {
		getLibProperties() >> this.libProperties
	}

}
