package net.freeapis.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.BiFunction;

/**
 * @author xwx
 */
public class SimpleUrlUtils {


    public static <T> T get(String urlString, BiFunction<InputStream, Integer, T> handler  ){
        HttpURLConnection urlConnection = null;
        try {
            // 1. 得到访问地址的URL
            URL url = new URL(urlString);
            // 2. 得到网络访问对象java.net.HttpURLConnection
            urlConnection = (HttpURLConnection) url.openConnection();
            /* 3. 设置请求参数（过期时间，输入、输出流、访问方式），以流的形式进行连接 */
            // 设置是否向HttpURLConnection输出
            urlConnection.setDoOutput(false);
            // 设置是否从httpUrlConnection读入
            urlConnection.setDoInput(true);
            // 设置请求方式　默认为GET
            urlConnection.setRequestMethod("GET");
            // 设置是否使用缓存
            urlConnection.setUseCaches(true);
            // 设置此 HttpURLConnection 实例是否应该自动执行 HTTP 重定向
            urlConnection.setInstanceFollowRedirects(true);
            // 设置超时时间
            urlConnection.setConnectTimeout(3000);
            // 连接
            urlConnection.connect();
            // 4. 得到响应状态码的返回值 responseCode
            int code = urlConnection.getResponseCode();
            // 5. 如果返回值正常，数据在网络中是以流的形式得到服务端返回的数据
            // 正常响应
            if (code == 200) {
                // 从流中读取响应信息
                return handler.apply(urlConnection.getInputStream(), urlConnection.getContentLength());
            }

        } catch ( IOException e) {
            System.err.println("转发出错，错误信息："+e.getLocalizedMessage()+";"+e.getClass());
        }finally {
            // 6. 断开连接，释放资源
            if (null != urlConnection){
                try {
                    urlConnection.disconnect();
                }catch (Exception ignored){
                }
            }
        }
        return null;
    }
}
