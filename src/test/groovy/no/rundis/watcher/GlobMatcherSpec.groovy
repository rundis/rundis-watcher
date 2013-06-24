package no.rundis.watcher

import spock.lang.Specification

import java.nio.file.Paths

class GlobMatcherSpec extends Specification{
    def testRootPath = Paths.get(new File("src/test/resources").absolutePath)

    def "matches a given file name"() {
        given:
        def matcher = globMatcher(['*.js'])
        def file = testRootPath.resolve("dummy.js")

        expect:
        matcher.matches(file)
    }

    def "does not match a file when extension is not in glob patterns"() {
        given:
        def matcher = globMatcher(['*.js'])
        def file = testRootPath.resolve("dummy.jsf")

        expect:
        !matcher.matches(file)
    }

    def "handles a relative path instance"() {
        given:
        def matcher = globMatcher(['*.js'])
        def file = Paths.get("dummy.js")

        expect:
        matcher.matches(file)
    }

    def "file matches if any of the glob patterns are satisfied"() {
        given:
        def matcher = globMatcher(['*.js', '**/*.js'])
        def file = Paths.get("lib/jalla/dummy.js")

        expect:
        matcher.matches(file)
    }

    def "file outside of rootpath does not match"() {
        given:
        def matcher = globMatcher(['*.js'])
        def file = Paths.get("file://tmp/jalla/dill.js")

        expect:
        !matcher.matches(file)
    }


    private GlobMatcher globMatcher(List globPatterns) {
        new GlobMatcher(testRootPath, globPatterns)
    }

}
