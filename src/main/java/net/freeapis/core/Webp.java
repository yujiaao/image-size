package net.freeapis.core;

import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.util.Objects;

import static net.freeapis.core.ByteUtil.*;

/**
 * @author xwx
 */
public class Webp implements Image.Parser {


    static byte[] SIG_RIFF = str2arr("RIFF");
    static byte[] SIG_WEBP = str2arr("WEBP");


    Size parseVP8(ByteBuffer data, int offset) {

        byte[] bytes = new byte[6];
        data.position(offset);
        data.get(bytes, 0, 6);
        data.position(0);

        if (bytes[3] != 0x9D || bytes[4] != 0x01 || bytes[5] != 0x2A) {
            // bad code block signature
            return null;
        }

        return new Size(
                readUInt16LE(data, offset + 6) & 0x3FFF,
                readUInt16LE(data, offset + 8) & 0x3FFF,
                "webp",
                "image/webp",
                "px",
                "px");
    }


    Size parseVP8L(ByteBuffer data, int offset) {
        if (data.get(offset) != 0x2F) {
            return null;
        }

        long bits = readUInt32LE(data, offset + 1);

        return new Size(
                (int) ((bits & 0x3FFF) + 1),
                (int) (((bits >> 14) & 0x3FFF) + 1),
                "webp",
                "image/webp",
                "px",
                "px"
        );
    }


    Size parseVP8X(ByteBuffer data, int offset) {
        return new Size(
                // TODO: replace with `data.readUIntLE(8, 3) + 1`
                //       when 0.10 support is dropped
                ((data.get(offset + 6) << 16) | (data.get(offset + 5) << 8) | data.get(offset + 4)) + 1,
                (data.get(offset + 9) << offset) | (data.get(offset + 8) << 8) | data.get(offset + 7) + 1,
                "webp",
                "image/webp",
                "px",
                "px"
        );
    }


    Size parse(ByteBuffer data) {
        if (!isValid(data)) {
            return null;
        }

        int offset = 12;
        Size result = null;
        int exif_orientation = 0;
        long fileLength = readUInt32LE(data, 4) + 8;

        if (fileLength > data.limit()) {
            return null;
        }

        while (offset + 8 < fileLength) {
            if (data.get(offset) == 0) {
                // after each chunk of odd size there should be 0 byte of padding, skip those
                offset++;
                continue;
            }

            byte[] bytes = new byte[4];
            data.get(bytes, offset, 4);
            String header = new String(bytes);
            long length = readUInt32LE(data, offset + 4);

            if (Objects.equals(header, "VP8 ") && length >= 10) {
                result = parseVP8(data, offset + 8);
            } else if (Objects.equals(header, "VP8L") && length >= 9) {
                result = parseVP8L(data, offset + 8);
            } else if (Objects.equals(header, "VP8X") && length >= 10) {
                result = parseVP8X(data, offset + 8);
            } else if (Objects.equals(header, "EXIF")) {
                exif_orientation = 0;//TODO exif.get_orientation(data.slice(offset + 8, offset + 8 + length));

                // exif is the last chunk we care about, stop after it
                offset = Integer.MAX_VALUE;
            }

            offset += 8 + length;
        }

        if (result != null) {
            return result;
        }

        if (exif_orientation > 0) {
            //result.orientation = exif_orientation;
        }

        return result;
    }

    @Override
    public boolean isValid(ByteBuffer buffer) {
        if (buffer.limit() < 16) {
            return false;
        }

        // check /^RIFF....WEBPVP8([ LX])$/ signature
        if (!sliceEq(buffer, 0, SIG_RIFF) && !sliceEq(buffer, 8, SIG_WEBP)) {
            return false;
        }
        return true;
    }

    @Override
    public Pair<Integer, Integer> size(ByteBuffer buffer) {
        Size size = parse(buffer);
        if (size != null) {
            return Pair.of(size.width, size.height);
        }
        return null;
    }
}
