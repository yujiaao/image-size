package net.freeapis.core;


/**
 * @author xwx
 */
public class Size {
    int width;
   int height;
   String type;
   String mime;
   String  wUnits="px";
    String hUnits="px";

    public Size(int width, int height, String type, String mime, String wUnits, String hUnits) {
        this.width = width;
        this.height = height;
        this.type = type;
        this.mime = mime;
        this.wUnits = wUnits;
        this.hUnits = hUnits;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public String getwUnits() {
        return wUnits;
    }

    public void setwUnits(String wUnits) {
        this.wUnits = wUnits;
    }

    public String gethUnits() {
        return hUnits;
    }

    public void sethUnits(String hUnits) {
        this.hUnits = hUnits;
    }
}
