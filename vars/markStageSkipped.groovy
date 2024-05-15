import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def <T> T call(String stageName) {
	Utils.markStageSkippedForConditional(stageName)
}
