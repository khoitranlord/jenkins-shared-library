void info(String message) {
	echo message
}

void warn(String message) {
	ansiColor('xterm') {
		// print text in yellow (33)
		echo "\u001B[33mWARNING: ${message}\u001B[0m"
	}
}

void error(String message) {
	ansiColor('xterm') {
		// print text in red (31)
		echo "\u001B[31mERROR: ${message}\u001B[0m"
	}
}
