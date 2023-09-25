package net.freeapis.core;

import net.freeapis.core.utils.ExifUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.naming.SizeLimitExceededException;
import java.nio.ByteBuffer;

import static net.freeapis.core.ByteUtil.*;

/**
 * freeapis,Inc.
 * Copyright(C): 2016
 * NOTE: we only support baseline and progressive JPGs here
 * due to the structure of the loader class, we only get a buffer
 * with a maximum size of 4096 bytes. so if the SOF marker is outside
 * if this range we can't detect the file size correctly.
 *
 * @author Administrator
 * @date 2019年09月18日 16:38
 */
class Jpeg implements Image.Parser {

    private static final String JPG_HEADER = "ffd8ff";
    private static final String EXIF_MARKER = "45786966";
    private static final byte[] SIG_EXIF  = str2arr("Exif\u0000\u0000");

    private static final int APP1_DATA_SIZE_BYTES = 2;
    private static final int EXIF_HEADER_BYTES = 6;
    private static final int TIFF_BYTE_ALIGN_BYTES = 2;
    private static final String BIG_ENDIAN_BYTE_ALIGN = "4d4d";
    private static final String LITTLE_ENDIAN_BYTE_ALIGN = "4949";

    // Each entry is exactly 12 bytes
    private static final int IDF_ENTRY_BYTES = 12;
    private static final int NUM_DIRECTORY_ENTRIES_BYTES = 2;


    @Override
    public String getType() {
        return "jpeg";
    }


    @Override
    public boolean isValid(ByteBuffer buffer) {
        // first marker of the file MUST be 0xFFD8,
        // following by either 0xFFE0, 0xFFE2 or 0xFFE3
       // if (data[0] !== 0xFF || data[1] !== 0xD8 || data[2] !== 0xFF) return;

        String SOIMarker = ByteUtil.readHexString(buffer, 0, 3);
        return JPG_HEADER.equals(SOIMarker);
    }

    @Override
    public Pair<Integer, Integer> size(ByteBuffer data) throws SizeLimitExceededException {
        if (data.limit() < 2) {
            return null;
        }

        // first marker of the file MUST be 0xFFD8,
        // following by either 0xFFE0, 0xFFE2 or 0xFFE3
        if (data.get(0) != (byte)0xFF || data.get(1) !=  (byte)0xD8 || data.get(2) !=  (byte)0xFF) {
            return null;
        }

        int offset = 2;

        for (;;) {
            // skip until we see 0xFF, see https://github.com/nodeca/probe-image-size/issues/68
            for (;;) {
                if (data.limit() - offset < 2) {
                    throw new SizeLimitExceededException(data.limit()+"");
                }
                if (data.get(offset++) ==  (byte)0xFF) {
                    break;
                }
            }

            byte  code = data.get(offset++);
            int length;

            // skip padding bytes
            while (code ==  (byte)0xFF){
                code = data.get(offset++);
            }

            // standalone markers, according to JPEG 1992,
            // http://www.w3.org/Graphics/JPEG/itu-t81.pdf, see Table B.1
            if (( (byte)0xD0 <= code && code <=  (byte)0xD9) || code ==  (byte)0x01) {
                length = 0;
            } else if ( (byte)0xC0 <= code && code <=  (byte)0xFE) {
                // the rest of the unreserved markers
                if (data.limit() - offset < 2) {
                    return null;
                }

                length = readUInt16BE(data, offset) - 2;
                offset += 2;
            } else {
                // unknown markers
                return null;
            }

            if (code ==  (byte)0xD9 /* EOI */ || code ==  (byte)0xDA /* SOS */) {
                // end of the datastream
                return null;
            }

            int orientation =0;

            // try to get orientation from Exif segment
            if (code ==  (byte)0xE1 && length >= 10 && sliceEq(data, offset, SIG_EXIF)) {
//                orientation = exif.get_orientation(data.slice(offset + 6, offset + length));
//                byte[] dest = new byte[length];
//                int start = offset + 6;
//                ByteBuffer src = data;
//                for (int i = start+1, j = 0; i < length;i++) {
//                    dest[j] = data.get(i);
//                    j++;
//                }
//                orientation = ExifParser.get_orientation(ByteBuffer.wrap(dest));

            }

            if (length >= 5 &&
                    ( (byte)0xC0 <= code && code <=  (byte)0xCF) &&
                    code !=  (byte)0xC4 && code !=  (byte)0xC8 && code !=  (byte)0xCC) {

                if (data.limit() - offset < length) {
                    return null;
                }

//                var result = {
//                        width:  readUInt16BE(data, offset + 3),
//                        height: readUInt16BE(data, offset + 1),
//                        type:   'jpg',
//                        mime:   'image/jpeg',
//                        wUnits: 'px',
//                        hUnits: 'px'
//      };

                Pair<Integer, Integer> result = Pair.of(readUInt16BE(data, offset + 3), readUInt16BE(data, offset + 1));

//                if (orientation > 0) {
//                    result.orientation = orientation;
//                }

                orientation = ExifUtils.getOrientation(data.array());
                if(orientation==90 || orientation == 270){
                    return Pair.of(result.getRight(),result.getLeft());
                }
                return result;
            }
            offset += length;
        }
    }

    private boolean isEXIF(ByteBuffer buffer) {
        return EXIF_MARKER.equals(ByteUtil.readHexString(buffer, 2, 6));
    }

    private Pair<Integer, Integer> extractSize(ByteBuffer buffer, int index) {
        return Pair.of(
                readUInt16BE(buffer, index + 2),
                readUInt16BE(buffer, index)
        );
    }

    private int validateExifBlock(ByteBuffer buffer, int index) {
        // Skip APP1 Data Size
        ByteBuffer exifBlock = ByteUtil.slice(buffer, APP1_DATA_SIZE_BYTES, index);

        // Consider byte alignment
        String byteAlign = ByteUtil.readHexString(buffer,
                EXIF_HEADER_BYTES, EXIF_HEADER_BYTES + TIFF_BYTE_ALIGN_BYTES);

        // Ignore Empty EXIF. Validate byte alignment
        boolean isBigEndian = BIG_ENDIAN_BYTE_ALIGN.equals(byteAlign);
        boolean isLittleEndian = LITTLE_ENDIAN_BYTE_ALIGN.equals(byteAlign);

        if (isBigEndian || isLittleEndian) {
            return extractOrientation(exifBlock, isBigEndian);
        }

        return -1;
    }

    private int extractOrientation(ByteBuffer exifBlock, boolean isBigEndian) {
        // TODO: assert that this contains 0x002A
        // let STATIC_MOTOROLA_TIFF_HEADER_BYTES = 2
        // let TIFF_IMAGE_FILE_DIRECTORY_BYTES = 4

        // TODO: derive from TIFF_IMAGE_FILE_DIRECTORY_BYTES
        int idfOffset = 8;

        // IDF osset works from right after the header bytes
        // (so the offset includes the tiff byte align)
        int offset = EXIF_HEADER_BYTES + idfOffset;

        int idfDirectoryEntries = ByteUtil.readUInt16(exifBlock, offset, isBigEndian);

        for (int directoryEntryNumber = 0; directoryEntryNumber < idfDirectoryEntries; directoryEntryNumber++) {
            int start = offset + NUM_DIRECTORY_ENTRIES_BYTES + (directoryEntryNumber * IDF_ENTRY_BYTES);
            int end = start + IDF_ENTRY_BYTES;

            // Skip on corrupt EXIF blocks
            if (start > exifBlock.limit()) {
                return -1;
            }

            ByteBuffer block = ByteUtil.slice(exifBlock, start, end);
            int tagNumber = ByteUtil.readUInt16(block, 0, isBigEndian);

            // 0x0112 (decimal: 274) is the `orientation` tag ID
            if (tagNumber == 274) {
                int dataFormat = ByteUtil.readUInt16(block, 2, isBigEndian);
                if (dataFormat != 3) {
                    return -1;
                }

                // unsinged int has 2 bytes per component
                // if there would more than 4 bytes in total it's a pointer
                long numberOfComponents = ByteUtil.readUInt32(block, 4, isBigEndian);
                if (numberOfComponents != 1) {
                    return -1;
                }

                return ByteUtil.readUInt16(block, 8, isBigEndian);
            }
        }
        return -1;
    }

    private void validateBuffer(ByteBuffer buffer, int index) {
        // index should be within buffer limits
        if (index > buffer.limit()) {
            throw new IllegalStateException("Corrupt JPG, exceeded buffer limits");
        }
        // Every JPEG block must begin with a 0xFF
        if (buffer.get(index) != (byte)0xFF) {
            throw new IllegalStateException("Invalid JPG, marker table corrupted");
        }
    }
}
