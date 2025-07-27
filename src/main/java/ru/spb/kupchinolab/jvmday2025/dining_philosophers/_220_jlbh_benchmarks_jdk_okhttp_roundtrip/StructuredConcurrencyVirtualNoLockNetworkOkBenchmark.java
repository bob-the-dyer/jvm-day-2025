package ru.spb.kupchinolab.jvmday2025.dining_philosophers._220_jlbh_benchmarks_jdk_okhttp_roundtrip;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class StructuredConcurrencyVirtualNoLockNetworkOkBenchmark implements JLBHTask {
    private final static int port = 8007;
    private final static int concurrentExecutions = 1_000;
    private NanoSampler requestResponseSampler;
    private JLBH jlbh;
    private Vertx vertx;
    private HttpServer server;
    private OkHttpClient client;

    public static void main(String[] args) {
        try {
            System.out.println(InetAddress.getByName("localhost"));
            System.out.println(InetAddress.getLoopbackAddress().getCanonicalHostName());
            System.out.println(Arrays.toString(InetAddress.getLocalHost().getAddress()));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } finally {

        }

        JLBHOptions jlbhOptions = new JLBHOptions()
                .warmUpIterations(20)
                .throughput(10)
                .iterations(10)
                .runs(3)
                .recordOSJitter(true)
                .accountForCoordinatedOmission(false)
                .jlbhTask(new StructuredConcurrencyVirtualNoLockNetworkOkBenchmark());
        new JLBH(jlbhOptions).start();
    }

    @Override
    public void init(JLBH init) {
        System.out.println("in init");
        this.jlbh = init;
        requestResponseSampler = this.jlbh.addProbe("request-response");

        client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(500, 5, TimeUnit.MINUTES))
                .build();

        vertx = Vertx.vertx();
        server = vertx.createHttpServer();

        String responseAsString;

        try (InputStream in = Path.of("16KB_file.txt").toFile().toURI().toURL().openStream()) {
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


    @Override
    public void run(long startTimeNs) {
        try (StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure(null, Thread.ofVirtual().factory())) {
            IntStream.rangeClosed(1, concurrentExecutions).forEach(_ -> scope.fork(() -> {
                long reqResStartNs = System.nanoTime();
                URL url = URI.create("http://127.0.0.1:" + port + "/payload").toURL();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    return response.body().string();
                } finally {
                    requestResponseSampler.sampleNanos(System.nanoTime() - reqResStartNs);
                }
            }));
            scope.join();
            scope.throwIfFailed();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            this.jlbh.sample(System.nanoTime() - startTimeNs);
        }
    }

    @Override
    public void complete() {
        System.out.println("in complete start");
        server.close().await();
        vertx.close().await();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        System.out.println("in complete finish");
    }
}