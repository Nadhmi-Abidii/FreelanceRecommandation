package com.towork.common;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EnumUtils {

    public static <T extends Enum<T>> List<String> getEnumNames(Class<T> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public static <T extends Enum<T>> List<T> getEnumValues(Class<T> enumClass) {
        return Arrays.asList(enumClass.getEnumConstants());
    }

    public static <T extends Enum<T>> boolean isValidEnum(Class<T> enumClass, String value) {
        try {
            Enum.valueOf(enumClass, value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static <T extends Enum<T>> T getEnumValue(Class<T> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid enum value: " + value + " for enum: " + enumClass.getSimpleName());
        }
    }
}
