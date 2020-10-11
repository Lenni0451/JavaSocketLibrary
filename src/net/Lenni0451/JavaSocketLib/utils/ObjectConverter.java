package net.Lenni0451.JavaSocketLib.utils;

import java.io.*;

public class ObjectConverter {

    public static byte[] objectToByteArray(final Object input) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        objectOut.writeObject(input);
        return byteOut.toByteArray();
    }

    public static <T> T byteArrayToObject(final byte[] input) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(input);
        ObjectInputStream objectIn = new ObjectInputStream(byteIn);
        try {
            return (T) objectIn.readObject();
        } catch (Exception e) {
        }
        return null;
    }

}
