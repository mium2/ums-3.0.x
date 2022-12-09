import com.google.gson.Gson;
import kr.uracle.ums.core.util.AES256Cipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Y.B.H(mium2) on 2019. 2. 20..
 */
public class TestMain {
    Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public static void main(String[] args){

//        new TestMain().adminTempMsg();
        try {
//            new TestMain().checkMacroCode("MACRO_001");

//            new TestMain().makeJson();
            new TestMain().getReplaceVars();
        }catch (Exception e){
            e.printStackTrace();
        }

//        try {
//            Calendar temp = Calendar.getInstance();
//            temp.add(Calendar.MONTH, -1 * 6);
//            SimpleDateFormat format2 = new SimpleDateFormat("yyyyMM");
//            String chkDate = String.format("%s01", format2.format(temp.getTime()));
//            System.out.println("### chkDate : " + chkDate);

//            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
//            Calendar cal1 = Calendar.getInstance();
//            Calendar cal2 = Calendar.getInstance();
//
//            cal1.setTime(sdf.parse("20190604"));
//            cal2.setTime(sdf.parse("20190611"));
//
//            while (cal1.getTimeInMillis() <= cal2.getTimeInMillis()) {
//                System.out.println(sdf.format(cal1.getTime()));
//                cal1.add(Calendar.DATE, 1);
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }

//        System.out.println(DateUtil.getDate(-30,""));
//        String TEMPLET_AUTHKEY = "q6O8Vgq21bdelnCz";
//        String encStr = "1840001706유라클연구소";

//        try {
//            String aaa = AES256Cipher.getInstance().encrypt(encStr, TEMPLET_AUTHKEY);
//            System.out.println(aaa);
//        } catch (GeneralSecurityException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        new TestMain().getReplaceVars();
//        new TestMain().makeChgPersonalMsg();
//        new TestMain().pushReplaceAll();
//        String reserveDate = "2019-03-31 13:12 ";
//        try {
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//            simpleDateFormat.setLenient(false);
//
//            simpleDateFormat.format(simpleDateFormat.parse(reserveDate));
//            System.out.println("OK");
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        long nowTimeMil = System.currentTimeMillis();
//
//        System.out.println("-"+nowTimeMil+"2");


    }

    private void makeJson(){
        try {
            Gson gson = new Gson();
//            #{날짜}에 신청하신 증명서발급이 완료되었습니다.
            Map<String,String> map = new HashMap<>();
            map.put("#{날짜}","2022-07-08");

            System.out.println(gson.toJson(map));

//
//            "{\"#{날짜}\":\"2022-07-08\"}"
//            String replaceVars = "{\"#{날짜}\":\"2022-07-08\"}";
//            ("#{날짜}:\"2022-07-08\"}").toString();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void adminTempMsg(){
        try{
            //임시 엑셀메세지 정보를 저장한다
            String regexp1 = "%([a-zA-Z0-9]+)%";

            String pushMsg = "안녕하세요! %VAR1님! 어쩌고 저쩌고 모두보두 즐거워 합니다.";


            Pattern pattern = Pattern.compile(regexp1);
            Matcher matcher = pattern.matcher(pushMsg);

            if(matcher.find()){
                System.out.println("치환변수 있음(O)");
            }else{
                System.out.println("치환변수 없음(X)");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void makeLgcnsAuthKey(){
        // 암호화 : AES/ECB/PKCS5Padding
        String clinetComID  = "1840001706";
        String LGCNS_AUTHKEY = "q6O8Vgq21bdelnCz";
        try {
            String aaa = AES256Cipher.getInstance().encrypt(clinetComID, LGCNS_AUTHKEY);
            System.out.println(aaa);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    // 변수 #{이름} 형식의 변수 찾아서 분리하기
    private void getReplaceVars(){
        try {
            List<String> matchList = new ArrayList<String>();

            String pushMsg = "#{이름}님! #{31414789#$%^#$%^}체크 #{}가입을 축하드립니다 #{계좌번호(13)}! 가입 기념으로 꿀머니 #{꿀머니포인트=YYYY-MM-DD}원을 #{만'기일\"(8)}보내 드립니다. #{오늘날짜:(30)}이후에 사용가능합니다.-유라클 연구소-";
            String EXAMPLE_TEST = "사용자 정보 : email=#{email}, 아이디=#{account}";

            String regexp = "#\\{[\\S]+\\}";
//            String regexp = "#\\{[가-힣a-zA-Z0-9\\[\\]\\/?.,;:|\\)*~`!^\\-_+<>@\\#$%&\\=\\(\\'\\\"]+\\}";
//            String regexp = "#\\{(\\w+)\\}";  // 영문일 경우
//            String regexp = "#\\{([가-힣]+)\\}"; // 한글일 경우

            Pattern pattern  =  Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(pushMsg);

            while(matcher.find()){
                matchList.add(matcher.group());
            }
            System.out.println("matchList : "+ matchList.toString());

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void pushReplaceAll(){
        String pushMsg = "#{이름}(#{아이디})님! 가입을 축하드립니다! 가입 기념으로 꿀머니 #{꿀머니포인트}원을 보내 드립니다. #{오늘날짜}이후에 사용가능합니다.-유라클 연구소-";
        String makepushMsg = pushMsg.replaceAll("#\\{이름}","%CNAME%")
            .replaceAll("#\\{아이디","%CID%");

        System.out.println("makePushMsg : "+makepushMsg);

    }

    // #{이름} ==> %CNAME%
    private void makeChgPushCommonVars(){
        try {
            // CSV파일 해더값 가져옴.
            String pushMsg = "#{이름}님! 가입을 축하드립니다! 가입 기념으로 꿀머니 #{꿀머니포인트}원을 보내 드립니다. #{오늘날짜}이후에 사용가능합니다.-유라클 연구소-";
            String regexp = "#\\{([가-힣]+)\\}"; // 한글일 경우

            Pattern pattern  =  Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(pushMsg);

            StringBuffer pushMsgSB = new StringBuffer();
            while(matcher.find()){
                System.out.print( "Start index: " + matcher.start());
                System.out.println( " End index: " + matcher.end() + " ");

                String propertyName = matcher.group(1);
                System.out.println("propertyName:"+propertyName);
                matcher.appendReplacement( pushMsgSB, "홍길동");
            }
            matcher.appendTail( pushMsgSB);
            System.out.println(pushMsgSB.toString());

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void makePattenChange(){
        try{
            String regexp = "#\\{([가-힣]+)\\}";
            Pattern pattern = Pattern.compile(regexp);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // CSV파일이 정보를 이용하여 변경.#{이름} ==> 홍길동 변환
    private void makeChgPersonalMsg(){
        try {
            // CSV파일 해더값 가져옴.
            String pushMsg = "#{이름}님! 가입을 축하드립니다! 가입 기념으로 꿀머니 #{꿀머니포인트}원을 보내 드립니다. #{오늘날짜}이후에 사용가능합니다.-유라클 연구소-";
            String regexp = "#\\{([가-힣]+)\\}"; // 한글일 경우

            Pattern pattern  =  Pattern.compile(regexp);
            Matcher matcher = pattern.matcher(pushMsg);

            StringBuffer pushMsgSB = new StringBuffer();
            while(matcher.find()){
                System.out.print( "Start index: " + matcher.start());
                System.out.println( " End index: " + matcher.end() + " ");

                String propertyName = matcher.group(1);
                System.out.println("propertyName:"+propertyName);
                matcher.appendReplacement( pushMsgSB, "홍길동");
            }
            matcher.appendTail( pushMsgSB);
            System.out.println(pushMsgSB.toString());

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void makeTempMsg(){
        try{
            //임시 엑셀메세지 정보를 저장한다
            String regexp1 = "#\\{([가힣a-zA-Z0-9]+)\\}";

            String pushMsg = "Excel Message Send information";
            String regexp2 = "#\\{([가-힣a-zA-Z0-9]+)\\}";

            Pattern pattern = Pattern.compile(regexp1);
            Matcher matcher = pattern.matcher(pushMsg);

            StringBuffer pushMsgSB = new StringBuffer();
            while(matcher.find()){
                System.out.println("Start index : " + matcher.start());
                System.out.println("End index : " + matcher.end() + " ");

                String propertyName = matcher.group(1);
                System.out.println("propertyName : " + propertyName);
                matcher.appendReplacement( pushMsgSB, "홍길동");
            }

            Pattern pattern1 = Pattern.compile(regexp2);
            Matcher matcher1 = pattern1.matcher(pushMsg);

            while(matcher.find()){
                logger.info("First index start point : " + matcher.start());
                logger.info("Last index end point : " + matcher.end());

                String propertyName = matcher.group(1);
                logger.info("propertyName : " + propertyName);
                matcher1.appendReplacement( pushMsgSB, "홍길동");
            }

            matcher.appendTail(pushMsgSB);
            System.out.println(pushMsgSB.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String cutString(String pm_sString, int pm_iSize, String pm_sAppend) {
        if(pm_sString == null) return "";

        String lm_sRetStr = "";
        int lm_iStrSize = 0;
        char[] charArray = pm_sString.toCharArray();
        byte[] lm_oByteArray = pm_sString.getBytes();

        // Byte 길이를 먼저 검사하여 입력된 길이보다 긴 경우만 처리
        if(lm_oByteArray.length > pm_iSize){
            for(int i=0; i<pm_sString.length(); i++){
                // 2Byte가 하나의 char인 경우
                if(charArray[i] > '\u00ff'){
                    lm_iStrSize += 2;
                } else {
                    lm_iStrSize++;
                }

                if(lm_iStrSize > pm_iSize){
                    break;
                }else{
                    lm_sRetStr += charArray[i];
                }
            }
            // append를 뒷 부분에 추가
            lm_sRetStr += pm_sAppend;

            return lm_sRetStr;
        }
        return pm_sString;
    }

    public static String appendBR(String pm_sString) {
        if(pm_sString == null) return null;

        BufferedReader lm_oReader = new BufferedReader(new StringReader(pm_sString));
        String lm_sLine = null;
        StringBuffer lm_oBuffer = new StringBuffer();
        try{
            while((lm_sLine = lm_oReader.readLine()) != null){
                if(lm_oBuffer.length() != 0) lm_oBuffer.append("<BR>");
                lm_oBuffer.append(lm_sLine);
            }
        }catch (Exception e){
            return null;
        }
        return lm_oBuffer.toString();
    }

    private static String appendAR(String pm_rString) {
        if(pm_rString == null) return null;

        BufferedReader lm_oReader = new BufferedReader(new StringReader(pm_rString));
        String lm_sLine = null;
        StringBuffer lm_oBuffer = new StringBuffer();
        try{
            while((lm_sLine = lm_oReader.readLine()) != null){
                if(lm_oBuffer.length() != 0) lm_oBuffer.append("<BR>");
                lm_oBuffer.append(lm_sLine);
            }
        }catch (Exception e){
            return null;
        }

        return lm_oBuffer.toString();
    }

    private static String appendCR(String pm_cString){
        if(pm_cString == null) return null;

        BufferedReader lm_oReader = new BufferedReader((new StringReader(pm_cString)));
        String lm_sLine = null;
        StringBuffer lm_oBuffer = new StringBuffer();
        try{
            while((lm_sLine = lm_oReader.readLine()) != null){
                if(lm_oBuffer.length() != 0) lm_oBuffer.append("<BR>");
                lm_oBuffer.append(lm_sLine);
            }
        }catch(Exception e){
            return null;
        }
        return lm_oBuffer.toString();
    }

    private static String appendDR(String pm_dString){
        if(pm_dString == null) return null;

        BufferedReader lm_oReader = new BufferedReader((new StringReader(pm_dString)));
        String lm_sLine = null;
        StringBuffer lm_oBuffer = new StringBuffer();

        try{
            while((lm_sLine = lm_oReader.readLine()) != null){
                if(lm_oBuffer.length() != 0) lm_oBuffer.append("<BR>");
                lm_oBuffer.append(lm_sLine);
            }
        }catch(Exception e){
            return null;
        }
        return lm_oBuffer.toString();
    }

    private static String appendER(String pm_eString){
        if(pm_eString == null) return null;

        BufferedReader lm_oReader = new BufferedReader(new StringReader(pm_eString));
        String lm_sLine = null;
        StringBuffer lm_oBuffer = new StringBuffer();

        try{
            while((lm_sLine = lm_oReader.readLine()) != null){
                if(lm_oBuffer.length() != 0) lm_oBuffer.append("<BR>");
                lm_oBuffer.append(lm_sLine);
            }
        }catch (Exception e){
            return null;
        }
        return lm_oBuffer.toString();
    }

    private static String appendFR(String pm_fString){
        if(pm_fString == null) return null;

        BufferedReader lm_oReader = new BufferedReader(new StringReader(pm_fString));
        String lm_sLine = null;
        StringBuffer lm_oBuffer = new StringBuffer();

        try{
            while((lm_sLine=lm_oReader.readLine())!=null){
                if(lm_oBuffer.length() != 0) lm_oBuffer.append("<BR>");
                lm_oBuffer.append(lm_sLine);
            }
        }catch(Exception e){
            return null;
        }
        return lm_oBuffer.toString();
    }

    private boolean checkMacroCode(String macroCode) throws Exception{

        String MACRO_USE_CODE = "MACRO_001,MACRO_002 , MACRO_003,MACRO_004 ";

        Set<String> useMacroCodeSet = new HashSet<>();
        Map<String,Map<String, List<String>>> macroMappingMap = new HashMap<>();
        Map<String,Set<String>> macroLastChannel = new HashMap<>(); // 키 : 매크로코드, 값 : 마지막 종단 발송채널Set


        Set<String> MACRO_CHANNEL_SET = new HashSet<String>();
        MACRO_CHANNEL_SET.add("PUSH");
        MACRO_CHANNEL_SET.add("RCS");
        MACRO_CHANNEL_SET.add("KKO");
        MACRO_CHANNEL_SET.add("SMS");

        Map<String,String> propertiesMap = new HashMap<>();
        propertiesMap.put("MACRO_001"," PUSH > RCS > KKO>SMS");
        propertiesMap.put("MACRO_002"," PUSH > RCS > KKO");
        propertiesMap.put("MACRO_003"," PUSH > RCS,KKO>SMS");
        propertiesMap.put("MACRO_004"," PUSH > RCS,KKO>");

        // 최초 한번만 실행.
        if(useMacroCodeSet.size()==0){
            String[] MACRO_USE_CODE_ARR = MACRO_USE_CODE.split(","); // ex)MACRO_001,MACRO_002,MACRO_003
            for(int i=0; i<MACRO_USE_CODE_ARR.length; i++){
                String MACRO_CODE = MACRO_USE_CODE_ARR[i].trim(); //ex)MACRO_003
                String macroOrder = propertiesMap.get(MACRO_CODE);
                if(macroOrder==null){
                    continue;
                }
                macroOrder = macroOrder.trim(); // ex)PUSH>KKO,RCS>SMS

                if(!"".equals(macroOrder)){
                    String[] macroOrderArr = macroOrder.split(">");
                    // Key = 발송채널, Value = 발송채널실패시 대체발송채널 정보
                    Map<String, List<String>> macroOrderMap = new HashMap<>(); // ex){"PUSH":["KKO,RCS"], "KKO":["SMS"], "RCS":["SMS"]} 형태로 들어가야 함.

                    for(int j=0; j<macroOrderArr.length; j++){ // ex) macroOrderArr : ["PUSH","KKO,RCS","SMS"]
                        String keyChannel = macroOrderArr[j].trim();
                        // keyChannel이 쉼표로 배열일 수 있다. ex)["KKO,RCS"]
                        String[] keyChannelArr = keyChannel.split(",");

                        Set<String> lastChannelSet = new HashSet<>();
                        for(int z=0; z<keyChannelArr.length; z++){
                            String fromChannel = keyChannelArr[z].trim();
                            if(!MACRO_CHANNEL_SET.contains(fromChannel)){
                                logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                logger.error("!!! 알수 없는 채널을 매크로에 등록(설정정보확인). 등록불가 : {} 알수없는 채널: {}",MACRO_CODE,keyChannel);
                                logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                continue;
                            }
                            lastChannelSet.add(fromChannel);
                            int nextIndex = j+1; // 발송실패시 대체발송 다음 채널이 있는지 확인을 위해
                            if(macroOrderArr.length>nextIndex) {
                                String toChannel = macroOrderArr[nextIndex].trim();
                                // 발송채널을 키로 대체발송채널리스트를 값으로 맵을 만들어 주는 메쏘드를 호출한다.
                                makeMacroOrderMap(macroOrderMap,fromChannel,toChannel); // 응답 정보 ex){"PUSH":["RCS","KKO"]}
                            }else if(macroOrderArr.length==nextIndex){
                                // 마지막일 경우 키만 넣고 값은 null로 넣는다.
                                macroOrderMap.put(fromChannel,null); //ex){"SMS":null}

                            }
                        }
                        macroLastChannel.put(MACRO_CODE,lastChannelSet); // 마지막 종단발송채널을 계속 덮어씌움.


                    }
                    macroMappingMap.put(MACRO_CODE,macroOrderMap);
                    useMacroCodeSet.add(MACRO_CODE);
                }
            }
        }
        if(useMacroCodeSet.contains(macroCode)){
            return true;
        }else{
            return false;
        }

    }

    private void makeMacroOrderMap(Map<String, List<String>> macroOrderMap, String fromChannel, String toChannel) throws Exception{
        Set<String> MACRO_CHANNEL_SET = new HashSet<String>();
        MACRO_CHANNEL_SET.add("PUSH");
        MACRO_CHANNEL_SET.add("RCS");
        MACRO_CHANNEL_SET.add("KKO");
        MACRO_CHANNEL_SET.add("SMS");

        String[] toChannelArr = toChannel.split(",");

        List<String> toChannelList = new ArrayList<>();
        for (int z = 0; z < toChannelArr.length; z++) {
            String toChannelVal = toChannelArr[z].trim();
            if (!MACRO_CHANNEL_SET.contains(toChannelVal)) {
                logger.warn("알수 없는 채널을 매크로에 등록(설정정보확인).알수 없는 채널:"+ toChannelVal);
                macroOrderMap.put(fromChannel, null);
            }else {
                toChannelList.add(toChannelVal);
                macroOrderMap.put(fromChannel, toChannelList);
            }
        }
    }

}
