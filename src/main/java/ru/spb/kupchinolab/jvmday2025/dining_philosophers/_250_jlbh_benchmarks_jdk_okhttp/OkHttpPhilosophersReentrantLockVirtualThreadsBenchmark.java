package ru.spb.kupchinolab.jvmday2025.dining_philosophers._250_jlbh_benchmarks_jdk_okhttp;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import net.openhft.chronicle.core.util.NanoSampler;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers.Chopstick;
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._020_reentrant_pivot.ReentrantPhilosopher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

public class OkHttpPhilosophersReentrantLockVirtualThreadsBenchmark implements JLBHTask {

    private JLBH jlbh;

    private NanoSampler eatingSampler;

    static private Vertx vertx;
    static private HttpServer server;
    static private OkHttpClient client;
    private final static int port = 8007;

    private static Consumer<Integer> constructOkHttpEating() {
        return (_) -> {
            try {
                URL url = URI.create("http://127.0.0.1:" + port + "/payload").toURL();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    String responseAsString = response.body().string();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public void test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads(NanoSampler eatingSampler) throws InterruptedException, BrokenBarrierException {
        test_philosophers_internal(Thread.ofVirtual().factory(), ReentrantPhilosopher::from, constructOkHttpEating(), eatingSampler);
    }

    private void test_philosophers_internal(ThreadFactory factory, BiFunction<List<Object>, Integer, ? extends Callable<Integer>> philosopherSupplier, Consumer<Integer> eating, NanoSampler eatingSampler) throws InterruptedException, BrokenBarrierException {
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
            philosophers.add(philosopherSupplier.apply(List.of(i, leftChopstick, rightChopstick, barrier, eating), 1000));
        }
        try (ShutdownOnSuccess<Integer> scope = new ShutdownOnSuccess<>(null, factory)) {
            philosophers.forEach(scope::fork);
            barrier.await();
            long eatingStart = System.nanoTime();
            scope.join();
            eatingSampler.sampleNanos(System.nanoTime() - eatingStart);
        }
    }

    public static void main(String[] args) {
        JLBHOptions lth = new JLBHOptions()
                .warmUpIterations(1)
                .iterations(1)
                .throughput(1)
                .runs(1)
                .recordOSJitter(true)
                .accountForCoordinatedOmission(false)
                .jlbhTask(new OkHttpPhilosophersReentrantLockVirtualThreadsBenchmark());
        new JLBH(lth).start();
    }

    @Override
    public void init(JLBH jlbh) {
        this.jlbh = jlbh;
        eatingSampler = jlbh.addProbe("eating with ReentrantLock+VirtualThreads");

        startHttpServer();
        initHttpClint();

    }

    @Override
    public void complete() {
        stopHttpServer();
        stopHttpClient();
    }

    @Override
    public void run(long startTimeNS) {
        try {
            test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads(eatingSampler);
            jlbh.sample(System.nanoTime() - startTimeNS);
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
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
