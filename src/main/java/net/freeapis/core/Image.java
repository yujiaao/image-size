package net.freeapis.core;

import net.freeapis.core.utils.HttpClientUtils;
import net.freeapis.core.utils.SimpleUrlUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * freeapis,Inc.
 * Copyright(C): 2016
 *
 * <p>图片接口
 *
 * @author Administrator
 * @date 2019年09月18日 14:39
 */
public class Image {

    private static final long MAX_BUFFER_SIZE = 1024 * 10;

    private static final Map<Byte, Parser> IMAGES =
            new HashMap<Byte, Parser>() {{
                put((byte) 0x89, new PNG());
                put((byte) 0xff, new Jpeg());
                put((byte) 0x47, new GIF());
                put((byte) 0x42, new BMP());
                put((byte) 0x38, new PSD());
                put((byte) 0x00, new ICO());
                put((byte) 0x49, new Webp());
            }};

    public static Pair<Integer, Integer> sizeOf(File image) throws IOException {
        long imageSize = image.length();

        int bufferSize = (int) Math.min(imageSize, MAX_BUFFER_SIZE);



       try( FileInputStream fin = new FileInputStream(image)){
         return sizeOf(fin, bufferSize);
       }


    }


    public static Pair<Integer, Integer> sizeOf(InputStream in, int contentLength)  {
        try {
            long imageSize = contentLength;

            int bufferSize = (int) Math.min(imageSize, MAX_BUFFER_SIZE);

            byte[] bytes = new byte[bufferSize];

            in.read(bytes);

            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            Parser parser = selectParser(buffer);

            if (parser != null) {
                return parser.size(buffer);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return Pair.of(0,0);
    }


    public static Pair<Integer, Integer> sizeOf(ByteBuffer buffer, int contentLength)  {
        try {
            Parser parser = selectParser(buffer);

            if (parser != null) {
                return parser.size(buffer);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return Pair.of(0,0);
    }

    private static Parser selectParser(ByteBuffer buffer) {
        byte firstByte = buffer.get(0);

        for (Map.Entry<Byte, Parser> entry : IMAGES.entrySet()) {
            if (entry.getKey() == firstByte
                    && entry.getValue().isValid(buffer)) {
                return entry.getValue();
            }
        }

        for(Map.Entry<Byte, Parser> entry : IMAGES.entrySet()){
            if(entry.getValue().isValid(buffer)){
                return entry.getValue();
            }
        }
        return null;
    }

    public static String typeOf(File image) throws IOException{
        long imageSize = image.length();

        int bufferSize = (int) Math.min(imageSize, MAX_BUFFER_SIZE);

        byte[] bytes = new byte[bufferSize];

        FileInputStream fin = new FileInputStream(image);

        fin.read(bytes);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        Parser parser = selectParser(buffer);

        if(parser != null){
            return parser.getClass().getSimpleName();
        }
        return null;
    }

    interface Parser{

        boolean isValid(ByteBuffer buffer);

        Pair<Integer,Integer> size(ByteBuffer buffer);
    }


    public static Pair<Integer, Integer> doGet(String urlString) {
        return SimpleUrlUtils.get(urlString, Image::sizeOf);
    }


    public static Pair<Integer, Integer> doGetWithPool(String urlString) {
        return HttpClientUtils.get(urlString, Image::sizeOf);
    }

    public static List<Pair<String, Pair<Integer, Integer>>> batchGet(List<String> urlString) {
      // return urlString.stream().parallel().map(it -> Pair.of(it, doGetWithPool(it))).collect(Collectors.toList() );
        return urlString.stream().parallel().map(it -> Pair.of(it, doGet(it))).collect(Collectors.toList() );
    }

}
