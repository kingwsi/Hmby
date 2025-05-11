package org.example.hmby.utils;

/**
 * description:  <br>
 * date: 2025/5/9 20:48 <br>
 */
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Stream;

public class FixedSizeQueue<E> {
    private final int maxSize;
    private final Deque<E> deque;

    public FixedSizeQueue(int size) {
        this.maxSize = size;
        this.deque = new ArrayDeque<>(size);
    }

    public void add(E element) {
        if (deque.size() >= maxSize) {
            deque.removeFirst(); // 移除最旧的元素
        }
        deque.addLast(element);
    }

    public int size() {
        return deque.size();
    }

    public Stream<E> stream() {
        return deque.stream(); // 支持 Stream 操作
    }

    @Override
    public String toString() {
        return deque.toString();
    }
}

