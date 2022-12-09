package kr.uracle.ums.core.common.license;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

//import java.security.NoSuchAlgorithmException;
//import javax.crypto.NoSuchPaddingException;


public class CryptoUtils {
	public static final String DEFAULT_CHARSET_NAME = "utf-8";
	public final static byte[] DEFAULT_CRYPTO_KEY = new byte[]{ 66, 65, 121, 80, 115, 82, 83, 88, 79, 119, 87, 103, 98, 69, 109, 77 };	// "BAyPsRSXOwWgbEmM".getBytes();
	
	
	public final static String CRYPTO_ALGORITHM = "AES";
	public final static String CRYPTO_TRANSFORMATION = "AES/ECB/PKCS5PADDING";
    public final static String CRYPTO_CBC_TRANSFORMATION = "AES/CBC/PKCS5PADDING";
	
	
	
	public static Cipher getChipher(byte[] key, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException
	{
        Cipher cipher = null;
/*        try {
            if(key.length==32){
                cipher = Cipher.getInstance(CRYPTO_CBC_TRANSFORMATION);
                byte[] IVBytes = new byte[16];
                for(int i=0; i<16; i++){
                    IVBytes[i] = key[i];
                }
                SecretKey salt = new SecretKeySpec(key, CRYPTO_ALGORITHM);
                cipher.init(mode, salt, new IvParameterSpec(IVBytes));
            }else {
                cipher = Cipher.getInstance(CRYPTO_TRANSFORMATION);
                SecretKeySpec salt = new SecretKeySpec(key, CRYPTO_ALGORITHM);
                cipher.init(mode, salt);
            }
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }*/

        cipher = Cipher.getInstance(CRYPTO_TRANSFORMATION);
        SecretKeySpec salt = new SecretKeySpec(key, CRYPTO_ALGORITHM);
        cipher.init(mode, salt);
        return cipher;
	}
	
	public static String encrypt(String plaintext) {
		String retval = encrypt(DEFAULT_CRYPTO_KEY, plaintext);
		return retval;
	}
	public static String encrypt(byte []key, String plaintext) {
		String retval = null;
		try {
			retval = encrypt(plaintext, getChipher(key, Cipher.ENCRYPT_MODE));
			
		}  catch (Exception e) {
			e.printStackTrace();
		}
//		catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		} catch (InvalidKeyException e) {
//			e.printStackTrace();
//		} catch (IllegalBlockSizeException e) {
//			e.printStackTrace();
//		} catch (BadPaddingException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		}
		return retval;
	}
	public static String encrypt(String plaintext, Cipher cipher) throws UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
		String retval = encrypt(plaintext, cipher, CryptoUtils.DEFAULT_CHARSET_NAME);
		return retval;
	}
	public static String encrypt(String plaintext, Cipher cipher, String charsetName) throws UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
		String retval = null;
		
		byte[] buffBef = plaintext.getBytes(charsetName);
		byte[] buffAft = encrypt(buffBef, cipher);
		retval = ByteUtils.toHexString(buffAft);
		
		return retval;
	}
	
	public static byte[] encrypt(byte[] data) {
		return encrypt(DEFAULT_CRYPTO_KEY, data);
	}
	public static byte[] encrypt(byte[] key, byte[] data) {
		byte[] retval = null;
		try {
			retval = encrypt(data, getChipher(key, Cipher.ENCRYPT_MODE)) ;
		} catch (Exception e) {
			e.printStackTrace();
		}
//		catch (InvalidKeyException e) {
//			e.printStackTrace();
//		} catch (IllegalBlockSizeException e) {
//			e.printStackTrace();
//		} catch (BadPaddingException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		}
		return retval;
	}
	public static byte[] encrypt(byte[] data, Cipher cipher) throws IllegalBlockSizeException, BadPaddingException {
			return cipher.doFinal(data);
	}
	
	public static void encrypt(InputStream is, OutputStream os) {
		encrypt(DEFAULT_CRYPTO_KEY, is, os);
	}
	public static void encrypt(byte[] key, InputStream is, OutputStream os) {
		try {
			encrypt(is, os, getChipher(key, Cipher.ENCRYPT_MODE));
		} catch (Exception e) {
			e.printStackTrace();
		}
//		catch (InvalidKeyException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	public static void encrypt( InputStream is, OutputStream os, Cipher cipher) throws IOException {
			
		CipherOutputStream cos = new CipherOutputStream(os, cipher);
		byte[] buffer = new byte[8192];
		int bytesRead;
		while((bytesRead = is.read(buffer)) != -1) {
			cos.write(buffer, 0, bytesRead);
		}
		cos.flush();
		cos.close();
		cos = null;
		
	}
	
	
	
	
	
	
	
	public static String decrypt(String hexCipherText)
	{
		String retval = decrypt(DEFAULT_CRYPTO_KEY, hexCipherText);
		return retval;
	}
	public static String decrypt(byte[] key, String hexCipherText)
	{
		String retval = null;
		try {
			retval = decrypt(hexCipherText, getChipher(key, Cipher.DECRYPT_MODE));
		} catch (Exception e) {
			e.printStackTrace();
		}
//		catch (InvalidKeyException e) {
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		} catch (IllegalBlockSizeException e) {
//			e.printStackTrace();
//		} catch (BadPaddingException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		}
		return retval;
	}
	public static String decrypt(String hexCipherText, Cipher cipher) throws UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException 
	{
		String retval = decrypt(hexCipherText, cipher, DEFAULT_CHARSET_NAME);
		return retval;
	}
	public static String decrypt(String hexCipherText, Cipher cipher, String charsetName) throws UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException 
	{
		String retval = null;
		byte bef_buff[] = ByteUtils.toByteArray(hexCipherText);
		byte aft_buff[] = cipher.doFinal(bef_buff);
		retval = new String(aft_buff, charsetName);
		return retval;
	}
	
	
	
	public static byte[] decrypt(byte[] data) {
		return decrypt(DEFAULT_CRYPTO_KEY, data);
	}
	public static byte[] decrypt(byte[] key, byte[] data) {
		byte[] retval = null;
		try {
			retval = decrypt(data, getChipher(key, Cipher.DECRYPT_MODE));
		} catch (Exception e) {
			e.printStackTrace();
		}
//		catch (InvalidKeyException e) {
//			e.printStackTrace();
//		} catch (IllegalBlockSizeException e) {
//			e.printStackTrace();
//		} catch (BadPaddingException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		}
		return retval;
	}
	public static byte[] decrypt(byte[] data, Cipher cipher) throws IllegalBlockSizeException, BadPaddingException {
		return cipher.doFinal(data);
	}
	
	
	
	

	
	
	


	
    public static void decrypt(InputStream in, OutputStream out){
		decrypt(DEFAULT_CRYPTO_KEY, in, out);
    }
    public static void decrypt(byte[] key, InputStream in, OutputStream out){
		try {
			decrypt(in, out, getChipher(key, Cipher.DECRYPT_MODE));
		} catch (Exception e) {
			e.printStackTrace();
		}
//		catch (InvalidKeyException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
    }
    public static void decrypt(InputStream in, OutputStream out, Cipher cipher) throws IOException{
    	byte[] buf = new byte[1024];
        // Bytes read from in will be decrypted
    	 CipherInputStream cin = new CipherInputStream(in, cipher);

        // Read in the decrypted bytes and write the cleartext to out
        int numRead = 0;
        while ((numRead = cin.read(buf)) >= 0) {
            out.write(buf, 0, numRead);
        }
        //out.close();\
        out.flush();
        
        cin.close();
        cin = null;
    }

    
    
    
    
    
    
	public static byte[] decrypt(InputStream is) {
		return decrypt(DEFAULT_CRYPTO_KEY, is);
	}
	public static byte[] decrypt(byte[] key, InputStream is) {
		byte[] retval = null;
		try {
			retval = decrypt(is, getChipher(key, Cipher.DECRYPT_MODE));
		} catch (Exception e) {
			e.printStackTrace();
		}
//		catch (InvalidKeyException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (NoSuchPaddingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		return retval;
	}
	public static byte[] decrypt( InputStream is, Cipher cipher) throws IOException {
		
		CipherInputStream cis = new CipherInputStream(is, cipher);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[8192];
		int bytesRead;
		while((bytesRead = cis.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		cis.close();
		return output.toByteArray();
	}
	

}
