package ru.spb.kupchinolab.jvmday2025.dining_philosophers._199_jmh_benchmarks_united;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._020_reentrant_pivot.ReentrantPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._030_synchronized_pivot.SynchronizedPhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._100_vertx_pivot.VerticlePhilosopher;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._100_vertx_pivot.VirtualVerticlePhilosopher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.vertx.core.ThreadingModel.VIRTUAL_THREAD;
import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class UnitedPhilosophersBenchmark {

    static private Vertx vertx;
    static private HttpServer server;
    static private OkHttpClient client;
    private final static int port = 8007;

    private static Consumer<Integer> constructNoopEating(Blackhole blackhole) {
        return blackhole::consume;
    }

    private static Consumer<Integer> constructSleepingEating(Blackhole blackhole) {
        return stats -> {
            try {
                Thread.sleep(Duration.ofNanos(16_000)); //emulating - read sequentially from SSD with speed of 1MB in 1M nanosec
                blackhole.consume(stats);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private static Consumer<Integer> constructOkHttpEating(Blackhole blackhole) {
        return (stats) -> {
            try {
                URL url = URI.create("http://127.0.0.1:" + port + "/payload").toURL();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseAsString = response.body().string();
                    blackhole.consume(responseAsString);
                    blackhole.consume(stats);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Consumer<Integer> constructActiveWaitingEating(Blackhole blackhole) {
        return stats -> {
            long startTimeInNanos = System.nanoTime();
            long currentTimeInNanos;
            do {
                currentTimeInNanos = System.nanoTime();
            } while (currentTimeInNanos < startTimeInNanos + 16_000); //emulating - read sequentially from SSD with speed of 1MB in 1M nanosec
            blackhole.consume(currentTimeInNanos);
            blackhole.consume(stats);
        };
    }

    @Benchmark
    public void test_reentrant_lock_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructNoopEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_noop_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, constructNoopEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_noop_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, constructNoopEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_noop_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, constructNoopEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_sleeping_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructSleepingEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_sleeping_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, constructSleepingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_sleeping_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, constructSleepingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_sleeping_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, constructSleepingEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_ok_http_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructOkHttpEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_ok_http_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, constructOkHttpEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_ok_http_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, constructOkHttpEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_ok_http_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, constructOkHttpEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_active_waiting_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructActiveWaitingEating(blackhole));
    }

    @Benchmark
    public void test_reentrant_lock_active_waiting_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), ReentrantPhilosopher::from, constructActiveWaitingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_active_waiting_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofVirtual().factory(), SynchronizedPhilosopher::from, constructActiveWaitingEating(blackhole));
    }

    @Benchmark
    public void test_synchronized_active_waiting_philosophers_with_platform_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(Thread.ofPlatform().factory(), SynchronizedPhilosopher::from, constructActiveWaitingEating(blackhole));
    }

//    @Benchmark
//    public void test_verticle_noop_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions());
//    }

//    @Benchmark
//    public void test_sleeping_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructSleepingEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
//    }

//    @Benchmark
//    public void test_ok_http_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructOkHttpEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
//    }

//    @Benchmark
//    public void test_active_waiting_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructActiveWaitingEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
//    }

//    @Benchmark
//    public void test_virtual_noop_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
//    }

//    @Benchmark
//    public void test_virtual_sleeping_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructSleepingEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
//    }

//    @Benchmark
//    public void test_virtual_ok_http_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructOkHttpEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
//    }

//    @Benchmark
//    public void test_virtual_active_waiting_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructActiveWaitingEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD));
//    }

    private void test_classical_philosophers_internal(ThreadFactory factory, Function<List<Object>, ? extends Callable<Integer>> philosopherSupplier, Consumer<Integer> eating) throws InterruptedException, BrokenBarrierException {
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
        try (StructuredTaskScope.ShutdownOnSuccess<Integer> scope = new StructuredTaskScope.ShutdownOnSuccess<>(null, factory)) {
            philosophers.forEach(scope::fork);
            barrier.await();
            scope.join();
        }
    }

//    private void test_verticle_philosophers_internal(Function<List<Object>, ? extends VerticleBase> philosopherSupplier, Consumer<Integer> eating, DeploymentOptions deploymentOptions) throws InterruptedException {
//        Vertx vertx = Vertx.vertx();
//        CountDownLatch allVerticlesDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
//        CountDownLatch allVerticlesUnDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
//        CountDownLatch finishEatingLatch = new CountDownLatch(1);
//        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
//            vertx.deployVerticle(philosopherSupplier.apply(List.of(i, eating)), deploymentOptions).onComplete(_ -> allVerticlesDeployedLatch.countDown());
//        }
//        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
//            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
//            vertx.deploymentIDs().forEach(deploymentId -> vertx.undeploy(deploymentId).onComplete(_ -> allVerticlesUnDeployedLatch.countDown()));
//            vertx.close().onComplete(_ -> finishEatingLatch.countDown());
//        });
//        allVerticlesDeployedLatch.await();
//        System.out.println("all verticles deployed at " + Instant.now());
//        System.out.println("start eating at " + Instant.now());
//        vertx.eventBus().publish("start_barrier", "Go-go-go!");
//        finishEatingLatch.await();
//        allVerticlesUnDeployedLatch.await();
//    }

    public static void main(String[] args) throws RunnerException {

        startHttpServer();
        initHttpClint();

        Options opt = new OptionsBuilder()
                .include(UnitedPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(7)
                .jvmArgs("--enable-preview", "-Xmx4096m")
                .build();
        new Runner(opt).run();

        stopHttpServer();
        stopHttpClient();

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

    private static void initHttpClint() {
        client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(500, 5, TimeUnit.MINUTES))
                .build();

    }

    private static void stopHttpClient() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

}
