package com.rsa.utils;

import com.google.gson.annotations.SerializedName;

import java.util.Comparator;
import java.util.Objects;

public class Pair<F, S> {
    @SerializedName(value = "first")
    public F first;
    @SerializedName(value = "second")
    public S second;

    private Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }

    public F key() {
        return first;
    }

    public S value() {
        return second;
    }

    /**
     * A pair is equal if both parts are equal().
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Pair) {
            Pair<F, S> other = (Pair<F, S>) o;
            return first.equals(other.first)
                    && second.equals(other.second);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return first.toString() + ":" + second.toString();
    }

    public static class PairComparator<T extends Pair<?, Comparable>> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            return o1.second.compareTo(o2.second);
        }
    }
}
