package ru.spb.kupchinolab.jvmday2025.dining_philosophers._230_jmh_benchmarks_jdk_http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._020_reentrant_pivot.ReentrantPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._030_synchronized_pivot.SynchronizedPhilosopher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class HttpPhilosophersBenchmark {

    static private Vertx vertx;
    static private HttpServer server;
    private final static int port = 8007;

    private static Consumer<Integer> constructHttpEating(Blackhole blackhole) {
        return (stats) -> {
            try (var in = URI.create("http://127.0.0.1:" + port + "/payload").toURL().openStream()) {
                int length = in.readAllBytes().length;
                blackhole.consume(length);
                blackhole.consume(stats);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Benchmark
    public void test_reentrant_lock_http_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructHttpEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_http_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, constructHttpEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_http_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, constructHttpEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_http_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, constructHttpEating(blackhole));
    }

    private void test_philosophers_internal(ThreadFactory factory, Function<List<Object>, ? extends Callable<Integer>> philosopherSupplier, Consumer<Integer> eating) throws InterruptedException, BrokenBarrierException {
        List<Chopstick> chopsticks = new ArrayList<>();
        List<Callable<Integer>> philosophers = new ArrayList<>();
        CyclicBarrier barrier = new CyclicBarrier(1 + PHILOSOPHERS_COUNT_BASE);
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : PHILOSOPHERS_COUNT_BASE - 1);
            philosophers.add(philosopherSupplier.apply(List.of(i, leftChopstick, rightChopstick, barrier, eating)));
        }
        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            philosophers.forEach(scope::fork);
            barrier.await();
            scope.join();
        }
    }

    public static void main(String[] args) throws RunnerException {

        startHttpServer();

        Options opt = new OptionsBuilder()
                .include(HttpPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(7)
                .jvmArgs("--enable-preview")
                .build();
        new Runner(opt).run();

        stopHttpServer();
    }

    private static void startHttpServer() {
        vertx = Vertx.vertx();

        server = vertx.createHttpServer();

        String responseAsString;

        try (InputStream in = Path.of("1600B_file.txt").toFile().toURI().toURL().openStream()) {
            byte[] bytes = in.readAllBytes();
            responseAsString = new String(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        server.requestHandler(request -> { //any request
            HttpServerResponse response = request.response();
            response.putHeader("content-type", "text/plain");
            response.end(responseAsString); //<<--- here we can play with the size of the response
        });

        server.listen(port).await();
    }

    private static void stopHttpServer() {
        server.close().await();
        vertx.close().await();
    }
}
