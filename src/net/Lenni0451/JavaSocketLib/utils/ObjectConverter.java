package net.Lenni0451.JavaSocketLib.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectConverter {
	
	public static byte[] objectToByteArray(final Object input) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        objectOut.writeObject(input);
        return byteOut.toByteArray();
    }
    
	public static Object byteArrayToObject(final byte[] input) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(input);
        ObjectInputStream objectIn = new ObjectInputStream(byteIn);
        try {
        	return objectIn.readObject();
		} catch (Exception e) {}
        return null;
    }
	
}