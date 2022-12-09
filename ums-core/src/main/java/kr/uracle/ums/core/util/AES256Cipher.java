package kr.uracle.ums.core.util;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 * Created by IntelliJ IDEA.
 * User: mium2(Yoo Byung Hee)
 * Date: 2015-03-09
 * Time: 오전 11:33
 * To change this template use File | Settings | File Templates.
 */
public class AES256Cipher {
    private static AES256Cipher instance;
    private Key keySpec;
    private String key = ""; //암/복호화를 위한 키값

    private AES256Cipher(){

    }

    public static AES256Cipher getInstance() {
        if(instance==null){
            instance = new AES256Cipher();
        }
        return instance;
    }

    /**
     * AES256 으로 암호화 한다.
     *
     * @param str
     *            암호화할 문자열
     * @return
     * @throws NoSuchAlgorithmException
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException  키값의 길이가 16이하일 경우 발생
     */
    public String encrypt(String str, String _key) throws NoSuchAlgorithmException,
        GeneralSecurityException, UnsupportedEncodingException {
        if(keySpec==null) {
            settingKey(_key);
        }else{
            if(!this.key.equals(_key)){
                settingKey(_key);
            }
        }
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = c.doFinal(str.getBytes("UTF-8"));
        String enStr = new String(Base64.encodeBase64(encrypted));
        return enStr;
    }

    /**
     * AES256으로 암호화된 txt 를 복호화한다.
     *
     * @param str
     *            복호화할 문자열
     * @return
     * @throws NoSuchAlgorithmException
     * @throws GeneralSecurityException
     * @throws UnsupportedEncodingException
     */
    public String decrypt(String str, String _key) throws NoSuchAlgorithmException,
        GeneralSecurityException, UnsupportedEncodingException {
        if(keySpec==null) {
            settingKey(_key);
        }else{
            if(!this.key.equals(_key)){
                settingKey(_key);
            }
        }
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] byteStr = Base64.decodeBase64(str.getBytes());
        return new String(c.doFinal(byteStr), "UTF-8");
    }

    private void settingKey(String _key) throws UnsupportedEncodingException{
        this.key = _key;
        byte[] keyBytes = new byte[16];
        byte[] b = key.getBytes("UTF-8");
        int len = b.length;
        if (len > keyBytes.length) {
            len = keyBytes.length;
        }
        System.arraycopy(b, 0, keyBytes, 0, len);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        this.keySpec = keySpec;
    }
}
