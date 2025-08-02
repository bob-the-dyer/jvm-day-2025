package ru.spb.kupchinolab.jvmday2025.dining_philosophers._130_jmh_benchmarks_vertx_blocking_reading;

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
import ru.spb.kupchinolab.jvmday2025.dining_philosophers._100_vertx_pivot.VerticlePhilosopher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.spb.kupchinolab.jvmday2025.dining_philosophers.Utils.PHILOSOPHERS_COUNT_BASE;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class BlockingReadingVerticlePhilosophersBenchmark {

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
    public void test_verticle_blocking_reading_philosophers(Blackhole blackhole) throws InterruptedException {
        test_verticle_philosophers_internal(VerticlePhilosopher::from, constructOkHttpEating(blackhole), new DeploymentOptions());
    }

    private void test_verticle_philosophers_internal(Function<List<Object>, ? extends VerticleBase> philosopherSupplier, Consumer<Integer> eating, DeploymentOptions deploymentOptions) throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        CountDownLatch allVerticlesDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
        CountDownLatch allVerticlesUnDeployedLatch = new CountDownLatch(PHILOSOPHERS_COUNT_BASE);
        CountDownLatch finishEatingLatch = new CountDownLatch(1);
        for (int i = 0; i < PHILOSOPHERS_COUNT_BASE; i++) {
            vertx.deployVerticle(philosopherSupplier.apply(List.of(i, eating)), deploymentOptions).onComplete(_ -> allVerticlesDeployedLatch.countDown());
        }
        vertx.eventBus().consumer("max_eat_attempts_has_reached", msg -> {
            System.out.println("finish eating at " + Instant.now() + ", msg: " + msg.body());
            vertx.deploymentIDs().forEach(deploymentId -> vertx.undeploy(deploymentId).onComplete(_ -> allVerticlesUnDeployedLatch.countDown()));
            vertx.close().onComplete(_ -> finishEatingLatch.countDown());
        });
        allVerticlesDeployedLatch.await();
        System.out.println("all verticles deployed at " + Instant.now());
        System.out.println("start eating at " + Instant.now());
        vertx.eventBus().publish("start_barrier", "Go-go-go!");
        finishEatingLatch.await();
        allVerticlesUnDeployedLatch.await();
    }

    public static void main(String[] args) throws RunnerException {

        startHttpServer();
        initHttpClint();

        Options opt = new OptionsBuilder()
                .include(BlockingReadingVerticlePhilosophersBenchmark.class.getSimpleName())
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
