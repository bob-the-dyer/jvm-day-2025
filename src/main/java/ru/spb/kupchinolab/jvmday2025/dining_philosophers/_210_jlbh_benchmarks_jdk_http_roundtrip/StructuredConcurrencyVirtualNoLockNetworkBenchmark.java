package ru.spb.kupchinolab.jvmday2025.dining_philosophers._210_jlbh_benchmarks_jdk_http_roundtrip;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import net.openhft.chronicle.core.util.NanoSampler;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.IntStream;

public class StructuredConcurrencyVirtualNoLockNetworkBenchmark implements JLBHTask {
    private final static int port = 8007;
    private final static int concurrentExecutions = 400;
    private NanoSampler requestResponseSampler;
    private JLBH jlbh;
    private Vertx vertx;
    private HttpServer server;

    public static void main(String[] args) {
        JLBHOptions jlbhOptions = new JLBHOptions()
                .warmUpIterations(20)
                .throughput(10)
                .iterations(10)
                .runs(3)
                .recordOSJitter(true)
                .accountForCoordinatedOmission(false)
                .jlbhTask(new StructuredConcurrencyVirtualNoLockNetworkBenchmark());
        new JLBH(jlbhOptions).start();
    }

    @Override
    public void init(JLBH init) {
        this.jlbh = init;
        requestResponseSampler = this.jlbh.addProbe("request-response");

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

        server.listen(StructuredConcurrencyVirtualNoLockNetworkBenchmark.port).await();
    }


    @Override
    public void run(long startTimeNs) {
        try (StructuredTaskScope.ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure(null, Thread.ofVirtual().factory())) {
            IntStream.rangeClosed(1, concurrentExecutions).forEach(_ -> scope.fork(() -> {
                long reqResStartNs = System.nanoTime();
                try (var in = URI.create("http://127.0.0.1:" + port + "/payload").toURL().openStream()) {
                    return in.readAllBytes().length;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    requestResponseSampler.sampleNanos(System.nanoTime() - reqResStartNs);
                }
            }));
            scope.join();
            scope.throwIfFailed();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            this.jlbh.sample(System.nanoTime() - startTimeNs);
        }
    }

    @Override
    public void complete() {
        server.close().await();
        vertx.close().await();
    }
}