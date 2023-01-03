package net.freeapis.core;

/**
 * @author xwx
 */
public class Exif {
    boolean is_big_endian;
    int ifd;
    int  tag;
    int format;
    int count;
    int entry_offset;
    int data_length;
    int data_offset;
    byte[] value;
    boolean is_subifd_link;

    public Exif(boolean is_big_endian, int ifd, int tag, int format, int count, int entry_offset, int data_length, int data_offset, byte[] value, boolean is_subifd_link) {
        this.is_big_endian = is_big_endian;
        this.ifd = ifd;
        this.tag = tag;
        this.format = format;
        this.count = count;
        this.entry_offset = entry_offset;
        this.data_length = data_length;
        this.data_offset = data_offset;
        this.value = value;
        this.is_subifd_link = is_subifd_link;
    }

    public boolean isIs_big_endian() {
        return is_big_endian;
    }

    public void setIs_big_endian(boolean is_big_endian) {
        this.is_big_endian = is_big_endian;
    }

    public int getIfd() {
        return ifd;
    }

    public void setIfd(int ifd) {
        this.ifd = ifd;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getFormat() {
        return format;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getEntry_offset() {
        return entry_offset;
    }

    public void setEntry_offset(int entry_offset) {
        this.entry_offset = entry_offset;
    }

    public int getData_length() {
        return data_length;
    }

    public void setData_length(int data_length) {
        this.data_length = data_length;
    }

    public int getData_offset() {
        return data_offset;
    }

    public void setData_offset(int data_offset) {
        this.data_offset = data_offset;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public boolean isIs_subifd_link() {
        return is_subifd_link;
    }

    public void setIs_subifd_link(boolean is_subifd_link) {
        this.is_subifd_link = is_subifd_link;
    }
}
