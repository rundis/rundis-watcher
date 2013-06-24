package no.rundis.watcher

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class RundisWatcherSpec extends Specification {


    def "create watches given directory and subdirectories"() {
        given:
        def testRootPath = Paths.get(new File("src/test/resources").absolutePath)
        def listener = Mock(PathEventListener)
        def watcher = RundisWatcher.create(testRootPath, ['*.js'], listener)

        expect:
        watcher.keys.values().contains(testRootPath)
        watcher.keys.values().contains(testRootPath.resolve("sub1"))
        watcher.keys.values().contains(testRootPath.resolve("sub2"))
    }

    def "create file triggers pathevent"() {
        given:
        def testRootPath = createTestDir()
        def listener = Mock(PathEventListener)
        def dummyFile = new File(testRootPath.toFile(), "dummy.txt")


        when:
        def service = Executors.newFixedThreadPool(2)
        Future future = service.submit(new Runnable() {
            @Override
            void run() {
                RundisWatcher.create(testRootPath, ['*.*'], listener)
            }
        })
        sleep(100)
        dummyFile << "dill"

        try {
            future.get(10, TimeUnit.SECONDS)
        } catch(Exception e) {
            //expected}
        }
        service.shutdown()

        then:
        1 * listener.pathChange(_,_)
    }


    Path createTestDir() {
        Path path = Files.createTempDirectory(null)
        path.toFile().deleteOnExit()
        return path;
    }

}
