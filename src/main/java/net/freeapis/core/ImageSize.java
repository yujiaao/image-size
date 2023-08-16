package net.freeapis.core;

public class ImageSize {

    private int width;

    private int height;

    private String type;

    public ImageSize(int width, int height, String type) {
        this.width = width;
        this.height = height;
        this.type = type;
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

    public static ImageSize unknonw(){
        return new ImageSize(0,0,"");
    }
}
