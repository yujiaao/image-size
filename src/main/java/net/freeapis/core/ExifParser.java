package net.freeapis.core;

import org.apache.commons.lang3.ArrayUtils;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


/**
 * @author xwx
 */
public class ExifParser {

    ByteBuffer input;
    int start;
    boolean big_endian;
    boolean aborted;

    Stack<Ifd> ifds_to_read = new Stack<>();

    public ExifParser(ByteBuffer jpeg_bin, int exif_start, int exif_end) {
        // Uint8Array, exif without signature (which isn't included in offsets)
        this.input = ByteUtil.slice(jpeg_bin, exif_start, exif_end);

        // offset correction for `on_entry` callback
        this.start = exif_start;

        // Check TIFF header (includes byte alignment and first IFD offset)
        byte[] bytes = new byte[4];
        this.input.get(bytes,0, 4);
        String sig = new String( bytes);

        if (!"II\u002A\0".equals(sig) && !"MM\0\u002A".equals(sig)) {
            throw new RuntimeException("EBADDATA invalid TIFF signature");
        }

        // true if motorola (big endian) byte alignment, false if intel
        this.big_endian = sig.charAt(0) == 'M';
    }

    public static int get_orientation  (ByteBuffer data) {
        AtomicInteger orientation = new AtomicInteger();
        try {
            new ExifParser(data, 0, data.limit()).each( (entry) ->{
                if (entry.ifd == 0 && entry.tag == 0x112 && entry.value !=null
                ) {
                    orientation.set(entry.value[0]);
                    return false;
                }
                return null;
            });
            return orientation.get();
        } catch (Exception err) {
            return -1;
        }
    }

    public void  each (Function<Exif, Boolean> on_entry) {
        // allow premature exit
        this.aborted = false;

        int offset = this.read_uint32(4);

        this.ifds_to_read = new Stack<>();
        ifds_to_read.add(new Ifd(0, offset));


        while (this.ifds_to_read.size() > 0 && !this.aborted) {
            Ifd i = this.ifds_to_read.pop();
            if (i.offset==null) {
                continue;
            }
            this.scan_ifd(i.id, i.offset, on_entry);
        }
    }



// Reads Exif data
//
     Integer exif_format_read  (int format, int offset) {
        Integer v;

        switch (format) {
            case 1: // byte
            case 2: // ascii
                v = (int)this.input.get(offset);
                return v;

            case 6: // sbyte
                v =  (int) this.input.get(offset);
                return v | (v & 0x80) * 0x1fffffe;

            case 3: // short
                v = this.read_uint16(offset);
                return v;

            case 8: // sshort
                v = this.read_uint16(offset);
                return v | (v & 0x8000) * 0x1fffe;

            case 4: // long
                v = this.read_uint32(offset);
                return v;

            case 9: // slong
                v = this.read_uint32(offset);
                return v | 0;

            case 5:  // rational
            case 10: // srational
            case 11: // float
            case 12: // double
                return null; // not implemented

            case 7: // undefined
                return null; // blob

            default:
                // unknown type
                return null;
        }
    }


    boolean is_subifd_link (int ifd, int tag) {
        return (ifd == 0 && tag == 0x8769) || // SubIFD
                (ifd == 0 && tag == 0x8825) || // GPS Info
                (ifd == 0x8769 && tag == 0xA005); // Interop IFD
    }

    int exif_format_length(int format) {
        switch (format) {
            case 1: // byte
            case 2: // ascii
            case 6: // sbyte
            case 7: // undefined
                return 1;

            case 3: // short
            case 8: // sshort
                return 2;

            case 4:  // long
            case 9:  // slong
            case 11: // float
                return 4;

            case 5:  // rational
            case 10: // srational
            case 12: // double
                return 8;

            default:
                // unknown type
                return 0;
        }
    }

    public void scan_ifd (int ifd_no, int offset, Function<Exif, Boolean> on_entry) {
        int entry_count = this.read_uint16(offset);

        offset += 2;

        for (int  i = 0; i < entry_count; i++) {
            int  tag    = this.read_uint16(offset);
            int  format = this.read_uint16(offset + 2);
            int  count  = this.read_uint32(offset + 4);

            int  comp_length    = this.exif_format_length(format);
            int  data_length    = count * comp_length;
            int data_offset    = data_length <= 4 ? offset + 8 : this.read_uint32(offset + 8);
            boolean is_subifd_link = false;

            if (data_offset + data_length > this.input.limit()) {
                throw new RuntimeException("unexpected EOF EBADDATA");
            }

            List<Integer> value =new ArrayList<>();

            int  comp_offset = data_offset;

            for (int  j = 0; j < count; j++, comp_offset += comp_length) {
                Integer item = this.exif_format_read(format, comp_offset);
                if (item == null) {
                    value = null;
                    break;
                }
                value.add(item);
            }

            String v="" ;
            if (value!=null && format == 2) {
                char[] charr = (char[]) ArrayUtils.toPrimitive( value.stream().map(it -> (char) (it.intValue())).toArray());
                 v = utf8_decode(new String(charr));
                if (v!=null && v.charAt(v.length() - 1) == '\0') {
                    v = v.substring(0, v.length()-1);
                }
            }

            if (this.is_subifd_link(ifd_no, tag)) {
                if (value!=null && value.get(0)!=null && value.get(0) > 0) {
                    this.ifds_to_read.push(new Ifd(  tag,
                           value.get(0)));
                    is_subifd_link = true;
                }
            }

            Exif entry = new Exif(
                      this.big_endian,
                        ifd_no,
                         tag,
                         format,
                      count,
                      offset + this.start,
                      data_length,
                        data_offset + this.start,
                         v.getBytes(),
                    is_subifd_link
    );

            if (!on_entry.apply(entry)) {
                this.aborted = true;
                return;
            }

            offset += 12;
        }

        if (ifd_no == 0) {
            this.ifds_to_read.push(new Ifd( 1, this.read_uint32(offset)));
        }
    };

    String utf8_decode(String str) {
        try {
            return URLDecoder.decode(escape(str), StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return str;
        }
    }


    public int read_uint16  (int offset) {
        ByteBuffer d = this.input;
        if (offset + 2 > d.limit()) {
            throw new RuntimeException("unexpected EOF EBADDATA");
        }

        return this.big_endian ?
                d.get(offset) * 0x100 + d.get(offset + 1) :
                d.get(offset) + d.get(offset + 1) * 0x100;
    };


    public int  read_uint32 (int offset) {
        ByteBuffer d = this.input;
        if (offset + 4 > d.limit()) {
            throw new RuntimeException("unexpected EOF EBADDATA");
        }

        return this.big_endian ?
                d.get(offset) * 0x1000000 + d.get(offset + 1) * 0x10000 + d.get(offset + 2) * 0x100 + d.get(offset + 3) :
                d.get(offset) + d.get(offset + 1) * 0x100 + d.get(offset + 2) * 0x10000 + d.get(offset + 3) * 0x1000000;
    };



    /**
     * Convert characters outside the range U+0020 to U+007F to
     * Unicode escapes, and convert backslash to a double backslash.
     */
    public static final String escape(String s) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<s.length(); ) {
            int c = Character.codePointAt(s, i);
            i += getCharCount(c);
            if (c >= ' ' && c <= 0x007F) {
                if (c == '\\') {
                    buf.append("\\\\"); // That is, "\\"
                } else {
                    buf.append((char)c);
                }
            } else {
                boolean four = c <= 0xFFFF;
                buf.append(four ? "\\u" : "\\U");
                buf.append(hex(c, four ? 4 : 8));
            }
        }
        return buf.toString();
    }


    /**
     * Supplies a zero-padded hex representation of an integer (without 0x)
     */
    public static String hex(long i, int places) {
        if (i == Long.MIN_VALUE) {
            return "-8000000000000000";
        }
        boolean negative = i < 0;
        if (negative) {
            i = -i;
        }
        String result = Long.toString(i, 16).toUpperCase(Locale.ENGLISH);
        if (result.length() < places) {
            result = "0000000000000000".substring(result.length(),places) + result;
        }
        if (negative) {
            return '-' + result;
        }
        return result;
    }


    /**
     * Determines how many chars this char32 requires.
     * If a validity check is required, use <code>
     * <a href="../lang/UCharacter.html#isLegal(char)">isLegal()</a></code> on
     * char32 before calling.
     * @param char32 the input codepoint.
     * @return 2 if is in supplementary space, otherwise 1.
     * @stable ICU 2.1
     */
    public static int getCharCount(int char32)
    {
        if (char32 < SUPPLEMENTARY_MIN_VALUE) {
            return 1;
        }
        return 2;
    }

    public static final int SUPPLEMENTARY_MIN_VALUE = 0x10000;


}
