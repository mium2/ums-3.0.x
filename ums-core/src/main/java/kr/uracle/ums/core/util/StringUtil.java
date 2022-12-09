package kr.uracle.ums.core.util;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Y.B.H(mium2) on 2019. 2. 20..
 */
public class StringUtil {
    public static String replace( String source , String originStr , String replaceStr ){
        StringBuffer returnValue = new StringBuffer();
        int findIndex;
        String remainStr = source;

        while( remainStr != null ){
            findIndex = remainStr.indexOf( originStr );

            if( findIndex == -1 ){
                returnValue.append( remainStr );
                break;
            }else{
                returnValue.append( remainStr.substring( 0 , findIndex ) );
                returnValue.append( replaceStr );
            }

            if( remainStr.length() < originStr.length() ){
                returnValue.append( remainStr );
                break;
            }else{
                remainStr = remainStr.substring( findIndex + originStr.length() );
            }

        }

        return returnValue.toString();
    }

    // 인자로 오는 문자열이 실제 내용이 있는지 검사
    public static boolean isEmpty( String arg ){
        if( arg == null )
            return true;
        if( arg.trim().equals("") )
            return true;

        return false;
    }

    // 인자로 들어오는 문자열을 정수형으로 반환한다. 두번째 인자는 정수형으로 변환이
    // 실패하는 경우 기본값으로 반환하는 값이다.
    public static int parseInt( String strArg , int defaultValue ){
        int returnValue;

        try{
            returnValue = Integer.parseInt( strArg.trim() );
        }catch( Exception e ){
            returnValue = defaultValue;
        }

        return returnValue;
    }
    // 인자로 들어오는 문자열을 long형으로 반환한다. 두번째 인자는 long형으로 변환이
    // 실패하는 경우 기본값으로 반환하는 값이다.
    public static long parseLong( String strArg , long defaultValue ){
        long returnValue;

        try{
            returnValue = Long.parseLong( strArg.trim() );
        }catch( Exception e ){
            returnValue = defaultValue;
        }

        return returnValue;
    }

    // 숫자를 문자열 다루기 쉬운 형태로 반환한다. 두번째인자의 수만큼의 자리수를 채워서
    // 문자열로 반환한다. 예를 들면 2자리로 채운다면 1은 01로 반환된다.
    public static String toZoroString( int intArg , int figure ){
        StringBuffer returnValue = new StringBuffer();

        if( (intArg < 0 ) || (figure < 2) )
            return null;

        int origin = intArg;					// 원래 숫자
        int tempInt = intArg;
        String tempStr = String.valueOf( intArg );	// 숫자의 문자열 형태
        int length = tempStr.length();			// 숫자의 원래길이

        if( length < figure )				// '0'으로 자리수를 채운다.
        {
            for( int i = length ; i < figure ; i++ ){
                returnValue.append( "0" );
            }
        }

        returnValue.append( tempStr );

        return returnValue.toString();
    }

    public static String toJavaStringLiteral(String str, boolean useRaw) {
        StringBuffer buf = new StringBuffer(str.length() * 6); // x -> \u1234
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
                case '\b': buf.append("\\b"); break; // NOI18N
                case '\t': buf.append("\\t"); break; // NOI18N
                case '\n': buf.append("\\n"); break; // NOI18N
                case '\f': buf.append("\\f"); break; // NOI18N
                case '\r': buf.append("\\r"); break; // NOI18N
                case '\"': buf.append("\\\""); break; // NOI18N
                case '\\': buf.append("\\\\"); break; // NOI18N
                default:
                    if (c >= 0x0020 && (useRaw || c <= 0x007f))
                        buf.append(c);
                    else {
                        buf.append("\\u"); // NOI18N
                        String hex = Integer.toHexString(c);
                        for (int j = 0; j < 4 - hex.length(); j++)
                            buf.append('0');
                        buf.append(hex);
                    }
            }
        }
        return buf.toString();
    }

    public static String fromJavaStringLiteral(String str){
        StringBuffer buf = new StringBuffer();

        for(int i = 0; i < str.length(); i++){
            char c = str.charAt(i);

            switch(c){
                case '\\':
                    if(i == str.length() - 1){
                        buf.append('\\');
                        break;
                    }
                    c = str.charAt(++i);
                    switch(c){
                        case 'n':
                            buf.append('\n');
                            break;
                        case 't':
                            buf.append('\t');
                            break;
                        case 'r':
                            buf.append('\r');
                            break;
                        case 'u':
                            int value=0;
                            for (int j=0; j<4; j++) {
                                c = str.charAt(++i);
                                switch (c) {
                                    case '0': case '1': case '2': case '3': case '4':
                                    case '5': case '6': case '7': case '8': case '9':
                                        value = (value << 4) + c - '0';
                                        break;
                                    case 'a': case 'b': case 'c':
                                    case 'd': case 'e': case 'f':
                                        value = (value << 4) + 10 + c - 'a';
                                        break;
                                    case 'A': case 'B': case 'C':
                                    case 'D': case 'E': case 'F':
                                        value = (value << 4) + 10 + c - 'A';
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                                }
                            }
                            buf.append((char)value);
                            break;
                        default:
                            buf.append(c);
                            break;
                    }
                    break;
                default:
                    buf.append(c);
            }
        }
        return buf.toString();
    }

    public boolean chkRequest(String _chkStr){
        String[] chkStr = {";", "--", "'", "..", "\\"};
        for(int i=0; i<chkStr.length; i++){
            if(_chkStr.indexOf(chkStr[i])>0){
                return false;
            }
        }
        return true;
    }


    public static String euckrToUtf8(String str)
    {
        if(str.equals(""))
            return "";
        String result = null;

        try {
            byte[] raws = str.getBytes("euc-kr");
            result = new String( raws, "utf-8" );
        }catch( java.io.UnsupportedEncodingException e ) {
        }
        return result;
    }

    public static String utf8ToEuckr(String str)
    {
        if(str.equals(""))
            return "";
        String result = null;

        try {
            byte[] raws = str.getBytes("utf-8");
            result = new String( raws, "euc-kr" );
        }catch( java.io.UnsupportedEncodingException e ) {
        }
        return result;
    }
    /** * Convert the int to an byte array.
     * * @param integer The integer
     * * @return The byte array
     * */
    public static byte[] intToByteArray(final int integer) {
        ByteBuffer buff = ByteBuffer.allocate(Integer.SIZE / 8);
        buff.putInt(integer);
        buff.order(ByteOrder.BIG_ENDIAN);
        return buff.array();
    }

    /*** Convert the byte array to an int.
     * ** @param bytes The byte array
     * * @return The integer
     * */
    public static int byteArrayToInt(byte[] bytes) {
        final int size = Integer.SIZE / 8;
        ByteBuffer buff = ByteBuffer.allocate(size);
        final byte[] newBytes = new byte[size];
        for (int i = 0; i < size; i++) {
            if (i + bytes.length < size) {
                newBytes[i] = (byte) 0x00;
            }else{
                newBytes[i] = bytes[i + bytes.length - size];
            }
        }
        buff = ByteBuffer.wrap(newBytes);
        buff.order(ByteOrder.BIG_ENDIAN);
        return buff.getInt();
    }

    public static String getCalcStr(byte[] bystStr, int offset, int len) {
        String returnStr = "";
        try{
            int bytelen =  bystStr.length;
            if(bytelen >= len){
                returnStr = new String(bystStr, offset, len - offset,"KSC5601");
            }
        }catch(Exception e){
            return returnStr;
        }
        return returnStr;
    }

    public static String getCalcStr(String str, int sLoc, int eLoc) {
        byte[] bystStr;
        String rltStr = str;
        try{
            bystStr = str.getBytes();
            int bytelen =  bystStr.length;
            if(bytelen > eLoc){
                rltStr = new String(bystStr, sLoc, eLoc - sLoc);
            }
        } catch(Exception e){
            return rltStr;
        }
        return rltStr;
    }

    public static String getCharacterSet(File csvFile) throws Exception {
        java.io.FileInputStream fis = new java.io.FileInputStream(csvFile);
        byte[] buf = new byte[4096];
        UniversalDetector detector = new UniversalDetector(null);
        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
//   	     System.out.println("Detected encoding buf size = " + buf.length+" / csv file size = " + csvFile.length());
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        if (encoding != null) {
            System.out.println("Detected encoding = " + encoding);
        } else {
            System.out.println("No encoding detected.");
        }
        detector.reset();
        return encoding;
    }

    public static String getCharSet(InputStream inputStream) {

        String encoding = null;

        try {
            byte[] buf = new byte[4096];
            UniversalDetector detector = new UniversalDetector(null);
            int nread;

            while ((nread = inputStream.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }

            detector.dataEnd();
            encoding = detector.getDetectedCharset();
            detector.reset();

            inputStream.close();

        } catch (Exception e) {
        }

        return encoding;
    }

    public static boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    public static String astarPhoneNum(final Object phoneNumObj){
        String returnPhoneNum = "";
        try {
            if(phoneNumObj==null){
                return returnPhoneNum;
            }
            String phoneNum = phoneNumObj.toString();
            if (phoneNum != null) {
                if (phoneNum.length() > 6) {
                    char[] ch = phoneNum.toCharArray();
                    for (int i = 3; i < ch.length - 4; i++) {
                        ch[i] = '*';
                    }
                    returnPhoneNum = String.valueOf(ch);
                }
            }
        }catch (Exception e){

        }
        return returnPhoneNum;
    }

    public static String astarName(final String userName){
        String replaceString = userName;

        String pattern = "";
        if(userName.length() == 2) {
            pattern = "^(.)(.+)$";
        } else {
            pattern = "^(.)(.+)(.)$";
        }

        Matcher matcher = Pattern.compile(pattern).matcher(userName);

        if(matcher.matches()) {
            replaceString = "";

            for(int i=1;i<=matcher.groupCount();i++) {
                String replaceTarget = matcher.group(i);
                if(i == 2) {
                    char[] c = new char[replaceTarget.length()];
                    Arrays.fill(c, '*');

                    replaceString = replaceString + String.valueOf(c);
                } else {
                    replaceString = replaceString + replaceTarget;
                }

            }
        }
        return replaceString;
    }

    public static int bigDicimalToInt(Object bigDicimalNum){
        double d_tot_cnt = Double.parseDouble(bigDicimalNum.toString());
        return (int)d_tot_cnt;
    }

    public static long bigDicimalToLong(Object bigDicimalNum){
        double d_tot_cnt = Double.parseDouble(bigDicimalNum.toString());
        return (long)d_tot_cnt;
    }

    public static boolean checkHpNum(final String hpNum){
        String pattern = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$";
        boolean regex = Pattern.matches(pattern, hpNum);
        return regex;
    }
}
