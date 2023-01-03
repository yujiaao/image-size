package net.freeapis.core;

/**
 * @author xwx
 */
public class Ifd {
    int id;
    Integer offset;

    public Ifd(int id, int offset) {
        this.id = id;
        this.offset = offset;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
