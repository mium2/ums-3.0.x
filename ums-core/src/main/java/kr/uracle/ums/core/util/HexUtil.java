package kr.uracle.ums.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 7..
 */
public class HexUtil {
    private static Logger logger = LoggerFactory.getLogger(HexUtil.class);

    private static String getHash(String msg, String algorithm) {
        if (msg == null) {
            return "";
        }
        byte[] defaultBytes = msg.getBytes();
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            md.update(defaultBytes);
            byte messageDigest[] = md.digest();

            hexString.append(byteToHex(messageDigest));
        } catch (NoSuchAlgorithmException nsae) {
            logger.error("", nsae);
            hexString.append(msg);
        }
        return hexString.toString();
    }

    /**
     * MD5 hash value를 구한다. (32 chars)
     *
     * @param msg
     * @return
     */
    public static String getMD5(final String msg) {
        return getHash(msg, "MD5");
    }

    /**
     * SHA-1 hash value를 구한다. (40 chars)
     *
     * @param msg
     * @return
     */
    public static String getSHA1(final String msg) {
        return getHash(msg, "SHA-1");
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}