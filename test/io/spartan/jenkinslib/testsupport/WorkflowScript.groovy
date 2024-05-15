package io.spartan.jenkinslib.testsupport

abstract class WorkflowScript extends Script {

    // workflow methods
    // ----- ----- ----- ----- ----- ----- ----- ----- ----- -----

    abstract <T> T node(String label, Closure c)

    abstract <T> T wrap(Map m, Closure<T> c)

    abstract <T> T withCredentials(List<Map> list, Closure<T> c)

    abstract <T> T withEnv(List<String> list, Closure<T> c)

    abstract <T> T timestamps(Closure<T> c)

    abstract void sleep(Map<String, ?> options)

    abstract <T> T timeout(int timeout, Closure<T> c)

    abstract <T> T timeout(Map<String, ?> options, Closure<T> c)

    abstract void waitUntil(Closure<Boolean> c)

    abstract String sh(Map s)

    abstract void echo(String s)

    abstract void error(String s)

    abstract boolean fileExists(String fileName)

    abstract Map<String, ?> usernamePassword(Map<String, String> m)

    abstract Map<String, ?> file(Map<String, String> m)

    abstract Map<String, ?> string(Map<String, String> m)

    abstract <T> T dir(String name, Closure<T> c)

    abstract String readFile(Map map)

    abstract String readFile(String filePath)

    abstract void writeFile(Map map)

    abstract <T> T configFileProvider(List files, Closure<T> c)

    abstract Object configFile(Map options)

    abstract Map<String, ?> httpRequest(Map options)

    abstract Object readYaml(Map options)

    abstract <T> T stage(String name, Closure c)

    abstract Map readProperties(Map options)

    abstract String libraryResource(String path)

    abstract <T> T withSonarQubeEnv(String id, Closure<T> c)

    abstract void cleanWs(Map m)

    abstract <T> T container(Map map, Closure<T> c)

    abstract <T> T podTemplate(Map map, Closure<T> c)

    abstract void containerTemplate(Map map)

    abstract Object envVar(Map map)

    abstract void slackSend(Map map)

    abstract void markStageSkipped(String name)

    abstract void writeJSON(Map options)

    abstract Object readJSON(Map options)
}
