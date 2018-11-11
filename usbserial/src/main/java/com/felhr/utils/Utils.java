package com.felhr.utils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Predicate;

import java.util.Collection;
import java.util.List;


public class Utils {
    public static <T> List<T> removeIf(Collection<T> c, Predicate<? super T> predicate) {
        return Stream.of(c.iterator())
                .filterNot(predicate)
                .collect(Collectors.<T>toList());
    }
}
