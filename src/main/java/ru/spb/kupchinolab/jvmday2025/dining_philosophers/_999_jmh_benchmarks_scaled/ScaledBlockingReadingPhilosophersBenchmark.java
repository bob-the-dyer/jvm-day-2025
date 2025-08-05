package ru.spb.kupchinolab.jvmday2025.dining_philosophers._999_jmh_benchmarks_scaled;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ScaledBlockingReadingPhilosophersBenchmark {

    static private Vertx vertx;
    static private HttpServer server;
    static private OkHttpClient client;
    private final static int port = 8007;

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

    @Benchmark
    public void _001_test_0001K_0010K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 1, 1);
    }

    @Benchmark
    public void _002_test_0001K_0010K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 1, 1);
    }

    @Benchmark
    public void _010_test_0010K_0010K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 10, 1);
    }

    @Benchmark
    public void _020_test_0010K_0010K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 10, 1);
    }

//    @Benchmark
//    public void _030_test_0010K_0010K_virtual_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 1);
//    }
//
//    @Benchmark
//    public void _040_test_0010K_0010K_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 1);
//    }

    @Benchmark
    public void _050_test_0100K_0010K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 100, 1);
    }

    @Benchmark
    public void _060_test_0100K_0010K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 100, 1);
    }

//    @Benchmark
//    public void _070_test_0100K_0010K_virtual_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 100, 1);
//    }
//
//    @Benchmark
//    public void _080_test_0100K_0010K_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 100, 1);
//    }

    @Benchmark
    public void _090_test_0010K_0100K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 10, 10);
    }

    @Benchmark
    public void _100_test_0010K_0100K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 10, 10);
    }

//    @Benchmark
//    public void _110_test_0010K_0100K_virtual_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 10);
//    }
//
//    @Benchmark
//    public void _120_test_0010K_0100K_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 10);
//    }

    @Benchmark
    public void _130_test_0100K_0100K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 100, 10);
    }

    @Benchmark
    public void _140_test_0100K_0100K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 100, 10);
    }

    @Benchmark
    public void _142_test_0100K_1000K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 100, 100);
    }

    @Benchmark
    public void _144_test_0100K_1000K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 100, 100);
    }

//    @Benchmark
//    public void _150_test_0100K_0100K_virtual_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 100, 10);
//    }
//
//    @Benchmark
//    public void _160_test_0100K_0100K_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 100, 10);
//    }

    @Benchmark
    public void _170_test_1000K_0010K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 1000, 1);
    }

    @Benchmark
    public void _180_test_1000K_0010K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 1000, 1);
    }

    @Benchmark
    public void _182_test_1000K_0100K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 1000, 10);
    }

    @Benchmark
    public void _184_test_1000K_0100K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 1000, 10);
    }

//    @Benchmark
//    public void _190_test_1000K_0010K_virtual_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 1000, 1);
//    }
//
//    @Benchmark
//    public void _200_test_1000K_0010K_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 1000, 1);
//    }

    @Benchmark
    public void _210_test_0010K_1000K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 10, 100);
    }

    @Benchmark
    public void _220_test_0010K_1000K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 10, 100);
    }

//    @Benchmark
//    public void _230_test_0010K_1000K_virtual_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 100);
//    }
//
//    @Benchmark
//    public void _240_test_0010K_1000K_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 10, 100);
//    }

    @Benchmark
    public void _250_test_1000K_1000K_reentrant_lock_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(ReentrantPhilosopher::from, constructOkHttpEating(blackhole), 1000, 100);
    }

    @Benchmark
    public void _260_test_1000K_1000K_synchronized_philosophers_with_virtual_threads(Blackhole blackhole) throws InterruptedException, BrokenBarrierException {
        test_classical_philosophers_internal(SynchronizedPhilosopher::from, constructOkHttpEating(blackhole), 1000, 100);
    }

//    @Benchmark
//    public void _270_test_1000K_1000K_virtual_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VirtualVerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 1000, 100);
//    }
//
//    @Benchmark
//    public void _280_test_1000K_1000K_verticle_philosophers(Blackhole blackhole) throws InterruptedException {
//        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructNoopEating(blackhole), new DeploymentOptions().setThreadingModel(VIRTUAL_THREAD), 1000, 100);
//    }

    private void test_classical_philosophers_internal(BiFunction<List<Object>, Integer, ? extends Callable<Integer>> philosopherSupplier, Consumer<Integer> eating, int philosophersMultiplier, int eatingMultiplier) throws InterruptedException, BrokenBarrierException {
        List<Chopstick> chopsticks = new ArrayList<>();
        List<Callable<Integer>> philosophers = new ArrayList<>();
        int philosophersCount = PHILOSOPHERS_COUNT_BASE * philosophersMultiplier;
        CyclicBarrier barrier = new CyclicBarrier(1 + philosophersCount);
        for (int i = 0; i < philosophersCount; i++) {
            Chopstick cs = new Chopstick(i);
            chopsticks.add(cs);
        }
        for (int i = 0; i < philosophersCount; i++) {
            Chopstick leftChopstick = chopsticks.get(i);
            Chopstick rightChopstick = chopsticks.get(i != 0 ? i - 1 : philosophersCount - 1);
            philosophers.add(philosopherSupplier.apply(List.of(i, leftChopstick, rightChopstick, barrier, eating), eatingMultiplier));
        }
        try (StructuredTaskScope.ShutdownOnSuccess<Integer> scope = new StructuredTaskScope.ShutdownOnSuccess<>()) {
            philosophers.forEach(scope::fork);
            barrier.await();
            scope.join();
        }
    }

//    private void test_verticle_philosophers_internal(BiFunction<List<Object>, Integer, ? extends VerticleBase> philosopherSupplier, Consumer<Integer> eating, DeploymentOptions deploymentOptions, int philosophersMultiplier, int eatingMultiplier) throws InterruptedException {
//        Vertx vertx = Vertx.vertx();
//        CountDownLatch allVerticlesDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
//        int philosophersCount = PHILOSOPHERS_COUNT_BASE * philosophersMultiplier;
//        CountDownLatch finishEatingLatch = new CountDownLatch(1);
//        for (int i = 0; i < philosophersCount; i++) {
//            vertx.deployVerticle(philosopherSupplier.apply(List.of(i, eating), eatingMultiplier), deploymentOptions).onComplete(_ -> allVerticlesDeployedLatch.countDown());
//        }
//        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
//            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
//            vertx.close().onComplete(_ -> finishEatingLatch.countDown());
//        });
//        allVerticlesDeployedLatch.await();
//        System.out.println("all verticles deployed at " + Instant.now());
//        System.out.println("start eating at " + Instant.now());
//        vertx.eventBus().publish("start_barrier", "Go-go-go!");
//        finishEatingLatch.await();
//    }

    public static void main(String[] args) throws RunnerException {

        startHttpServer();
        initHttpClint();

        Options opt = new OptionsBuilder()
                .include(ScaledBlockingReadingPhilosophersBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .measurementIterations(3)
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
