package kr.uracle.ums.core.common.license;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestUtils {
	private static String algorithm = "MD5";
	
	public static byte[] digest(byte[] byteArray) {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			return md.digest(byteArray);
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	
	public static byte[] getMD5Hash(InputStream is) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			DigestInputStream dis = new DigestInputStream(is, md);
			byte[] buffer = new byte[8192];
			while(dis.read(buffer) != -1){}
			
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
}
