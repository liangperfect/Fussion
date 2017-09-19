package com.westalgo.factorycamera.util;

public class Size {

    public int width;
    public int height;

    public Size(int w, int h) {
        this.width = w;
        this.height = h;
    }

    @Override
    public String toString() {
        return width + " x " + height;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Size)) {
            return false;
        }

        Size otherSize = (Size) other;
        return otherSize.width == this.width && otherSize.height == this.height;
    }
}
