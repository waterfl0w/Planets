package edu.um.planet;

import java.util.LinkedList;
import java.util.List;

public class CircularBuffer<T> {

    private final int size;
    private List<T> data = new LinkedList<>();
    private int index = 0;

    public CircularBuffer(int size) {
        this.size = size;
    }

    public List<T> getData() {
        return data;
    }

    public void add(T value) {
        this.data.add(index, value);
        this.index = index + 1 % this.size - 1;
    }

}
