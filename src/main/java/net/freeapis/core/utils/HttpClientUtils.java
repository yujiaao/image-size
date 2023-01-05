package net.freeapis.core.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * @author xwx
 * @since 11
 */

public class HttpClientUtils {

    static String CONTENT_LENGTH = "content-length";

    private static final Duration timeout = Duration.ofSeconds(20);

    private static final Object lock = new Object();

    private static volatile HttpClient httpClient = null;

    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("httpclient-%d").build();

    private static ThreadPoolExecutor executor = null;

    private HttpClientUtils(){
        if (httpClient == null){
            synchronized (lock){
                if (httpClient == null){

                    executor = new ThreadPoolExecutor(5, 50,
                            1L, TimeUnit.MINUTES,
                            new LinkedBlockingQueue<>(100), namedThreadFactory, new ThreadPoolExecutor.CallerRunsPolicy());

                    executor.allowCoreThreadTimeOut(true);

                    httpClient = HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_1_1)
                            .connectTimeout(timeout)
                            .executor(executor)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .sslContext(sslContext())
                            .proxy(ProxySelector.getDefault())
                            .build();
                }
            }
        }
    }

    /**
     * 创建 HttpUtil
     * @return
     */
    public static HttpClientUtils builder(){
        return new HttpClientUtils();
    }


    /**
     * get请求
     * @param url 地址
     * @return
     */
    public byte[] getBytes(String url){
        byte[] body = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(url))
                    .GET()
                    .timeout(timeout)
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200){
                body = response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return body;
    }


    public static <T> T get(String urlString, BiFunction<InputStream, Integer, T> handler  ){
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(urlString))
                    .GET()
                    .timeout(timeout)
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200){

                handler.apply(response.body(), (int)response.headers()
                        .firstValueAsLong(CONTENT_LENGTH).orElse(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get请求
     * @param url 地址
     * @return
     */
    public String getString(String url){
        String body = "";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(url))
                    .GET()
                    .timeout(timeout)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200){
                body = response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return body;
    }

    /**
     * post请求
     * @param url 地址
     * @return
     */
    public String post(String url){
        return post(url,"");
    }

    /**
     * post请求
     * @param url 地址
     * @param data json字符串
     * @return
     */
    public String post(String url,String data){
        String body = "";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(data, Charset.defaultCharset()))
                    .timeout(timeout)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200){
                body = response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return body;
    }


    /**
     * 生成安全套接字工厂，用于https请求的证书跳过
     * @return
     */
    private SSLContext sslContext(){
        TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustManagers, new SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sc;
    }
}
