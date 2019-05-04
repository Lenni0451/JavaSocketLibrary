package net.Lenni0451.JavaSocketLib.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class RSACrypter {
	
	public static byte[] encrypt(final PublicKey publicKey, byte[] toEncrypt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(256);
		SecretKey secretKey = keyGenerator.generateKey();
		
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		toEncrypt = cipher.doFinal(toEncrypt);
		
		cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		byte[] keyBytes = cipher.doFinal(secretKey.getEncoded());
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(keyBytes);
		baos.write(toEncrypt);
		
		return baos.toByteArray();
	}
	
	public static byte[] decrypt(final PrivateKey privateKey, byte[] toDecrypt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		byte[] keyBytes = Arrays.copyOfRange(toDecrypt, 0, 256);
		toDecrypt = Arrays.copyOfRange(toDecrypt, 256, toDecrypt.length);
		
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] decryptedKey = cipher.doFinal(keyBytes);
		
		SecretKey secretKey = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");
		
		cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		return cipher.doFinal(toDecrypt);
	}
	
	public static KeyPair generateKeyPair(final int keySize) throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(keySize);
		return keyPairGenerator.genKeyPair();
	}

	public static PublicKey initPublicKey(final byte[] bytes) throws InvalidKeySpecException, NoSuchAlgorithmException{
		return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
	}

	public static PrivateKey initPrivateKey(final byte[] bytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
		return KeyFactory.getInstance("RSA").generatePrivate(new X509EncodedKeySpec(bytes));
	}
	
}
