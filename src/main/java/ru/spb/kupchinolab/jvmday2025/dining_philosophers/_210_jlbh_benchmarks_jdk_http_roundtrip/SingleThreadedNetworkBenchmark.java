package ru.spb.kupchinolab.jvmday2025.dining_philosophers._210_jlbh_benchmarks_jdk_http_roundtrip;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

public class SingleThreadedNetworkBenchmark implements JLBHTask {
    private final static int port = 8007;
    private JLBH jlbh;
    private Vertx vertx;
    private HttpServer server;
    private URL url;

    public static void main(String[] args) {
        JLBHOptions jlbhOptions = new JLBHOptions()
                .warmUpIterations(150000)
                .iterations(50000)
                .throughput(10000)
                .runs(3)
                .recordOSJitter(true)
                .accountForCoordinatedOmission(false)
                .jlbhTask(new SingleThreadedNetworkBenchmark());
        new JLBH(jlbhOptions).start();
    }

    @Override
    public void init(JLBH jlbh) {
        this.jlbh = jlbh;

        vertx = Vertx.vertx();
        server = vertx.createHttpServer();

        String responseAsString;

        try (InputStream in = Path.of("16KB_file.txt").toFile().toURI().toURL().openStream()) {
            byte[] bytes = in.readAllBytes();
            responseAsString = new String(bytes);
            url = URI.create("http://127.0.0.1:" + port + "/payload").toURL();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        server.requestHandler(request -> { //any request
            HttpServerResponse response = request.response();
            response.putHeader("content-type", "text/plain");
            response.end(responseAsString); //<<--- here we can play with the size of the response
        });

        server.listen(SingleThreadedNetworkBenchmark.port).await();
    }


    @Override
    public void run(long startTimeNs) {
        try (var in = url.openStream()) {
            in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        jlbh.sample(System.nanoTime() - startTimeNs);
    }

    @Override
    public void complete() {
        server.close().await();
        vertx.close().await();
    }
}