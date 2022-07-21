package ru.uris.test;

import ru.DmN.ReflectionUtils;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.out.println(Arrays.toString(Object.class.getMethods()) + Arrays.toString(Object.class.getDeclaredMethods()));
        System.out.println(Arrays.toString(ReflectionUtils.getAllMethods(Object.class)));
    }
}
