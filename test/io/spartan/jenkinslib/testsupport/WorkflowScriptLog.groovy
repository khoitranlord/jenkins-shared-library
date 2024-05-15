package io.spartan.jenkinslib.testsupport

abstract class WorkflowScriptLog {
	abstract void debug(String message)

	abstract void info(String message)

	abstract void warn(String message)

	abstract void error(String message)
}
