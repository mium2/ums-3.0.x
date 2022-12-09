package kr.uracle.ums.core.common.license;

public class ByteUtils {
	public static byte[] toByteArray(String hex) {
		if((hex.length() % 2) != 0) {
			throw new IllegalArgumentException("hex string is not corrupted.");
		}
		
		byte[] result = new byte[hex.length()/2];
		for(int i = 0; i < result.length; i++) {
			result[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
		}
		
		return result;
	}
	
	public static String toHexString(byte[] bytes) {
		StringBuffer buf = new StringBuffer(bytes.length * 2);
		
		for(int i = 0; i < bytes.length; i++) {
			buf.append(String.format("%02x", 0xff & bytes[i]).toUpperCase());
		}
		
		return buf.toString();
	}
}
