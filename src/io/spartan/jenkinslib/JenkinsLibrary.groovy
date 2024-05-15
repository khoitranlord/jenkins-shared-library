package io.spartan.jenkinslib

class JenkinsLibrary {
    private static JenkinsLibProperties LIB_PROPERTIES
    private static JenkinsLibInjector INJECTOR

    static void init(Script ctx, Map<String, ?> extraLibProperties) {
        LIB_PROPERTIES = new JenkinsLibProperties(ctx, extraLibProperties)
        INJECTOR = new JenkinsLibInjector(ctx, LIB_PROPERTIES)

        configureJobProperties(ctx)
    }

    static JenkinsLibInjector getInjector() {
        INJECTOR
    }

    static <T> T getLibraryConfig(String key, Class<T> expectedType = Object) {
        LIB_PROPERTIES.get(key, expectedType)
    }

    static void configureJobProperties(Script ctx) {
        def jobProperties = []
        // disable previous build when a new commit is pushed
        jobProperties.add(ctx.disableConcurrentBuilds(abortPrevious: true))
        ctx.properties(jobProperties)
    }

    private JenkinsLibrary() {}
}
