import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.exception.MacroCodeException;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;

import java.util.*;

public class TestMain2 {

    private String MACRO_USE_CODE = "MACRO_001,MACRO_002,MACRO_003,MACRO_004";
    private Map<String, MacroChannel> MACRO_CHANNEL_MAP = new HashMap<String, MacroChannel>();

    private Map<String, Map<MacroChannel, Set<MacroChannel>>> macroMappingMap = new HashMap<>(); // {"MACRO_001":{"PUSH":{"KKO"},"KKO":{"SMS},"SMS":null}}, "MACRO_002":{"PUSH":{"KKO","RCS"},"KKO":{"SMS"},"RCS":{"SMS"},"SMS":null}}
    private Map<String, Set<MacroChannel>> macroFirstChannelMap = new HashMap<>(); // 키 : 매크로코드, 값 : 시작 발송채널Set {"MACRO_001":{"PUSH"},"MACRO_002":{"PUSH","RCS","KKO","SMS"}}
    private Map<String, Set<MacroChannel>> macroLastChannelMap = new HashMap<>(); // 키 : 매크로코드, 값 : 마지막 종단 발송채널Set {"MACRO_001":{"SMS"}}

    public static enum MacroChannel {
        PUSH, NAVERT, KKO, RCS, SMS;
    }

    public TestMain2(){
        MACRO_CHANNEL_MAP.put(MacroChannel.PUSH.toString(), MacroChannel.PUSH);
        MACRO_CHANNEL_MAP.put(MacroChannel.NAVERT.toString(), MacroChannel.NAVERT);
        MACRO_CHANNEL_MAP.put(MacroChannel.RCS.toString(), MacroChannel.RCS);
        MACRO_CHANNEL_MAP.put(MacroChannel.KKO.toString(), MacroChannel.KKO);
        MACRO_CHANNEL_MAP.put(MacroChannel.SMS.toString(), MacroChannel.SMS);
    }

    public static void main(String[] args) {
        try {
            new TestMain2().testKomoran();
//            UmsSendMsgBean umsSendMsgBean = new UmsSendMsgBean(TransType.BATCH.toString());
////            umsSendMsgBean.setPUSH_MSG("푸시발송 테스트");
//            umsSendMsgBean.setALLIMTOLK_TEMPLCODE("1000006");
//            umsSendMsgBean.setFRIENDTOLK_MSG("친구톡 메세지 발송");
//            umsSendMsgBean.setRCS_MSG("RCS 발송");
//            umsSendMsgBean.setSMS_MSG("SMS 발송");
//            umsSendMsgBean.setSEND_MACRO_CODE("MACRO_001");
//
////            propertiesMap.put("MACRO_001"," PUSH > RCS > KKO>SMS");
////            propertiesMap.put("MACRO_002"," PUSH,RCS,KKO");
////            propertiesMap.put("MACRO_003"," PUSH > RCS,KKO>SMS");
////            propertiesMap.put("MACRO_004"," PUSH,RCS,KKO,SMS");
//
////            Set<SendType> nextSendTypeSet = new TestMain2().getFirstSendChannel(umsSendMsgBean);
//
//            Set<SendType> nextSendTypeSet = new TestMain2().getNextSendChannel(umsSendMsgBean, MacroChannel.PUSH);
//            if(nextSendTypeSet.size()>0) {
//                for (SendType sendType : nextSendTypeSet) {
//                    System.out.println(sendType.toString());
//                }
//            }else{
//                System.out.println("발송채널이 없습니다.");
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testKomoran(){
        long startTime = System.currentTimeMillis();
        Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
        String strToAnalyze = "코모란은 Java에서 사용하는 대표적인 오픈소스 형태소 분석기 중 하나이다. 사용하기가 매우 편리하고, 형태소 분석 성능 역시 뛰어나기 때문에 Java든 Python이든 많이 사용하고 있다. \n" +
                "출처: https://needjarvis.tistory.com/740 [자비스가 필요해:티스토리]";
        KomoranResult analyzeResultList = komoran.analyze(strToAnalyze);

//        System.out.println(analyzeResultList.getPlainText());

//        List<Token> tokenList = analyzeResultList.getTokenList();
//        for (Token token : tokenList) {
//            System.out.format("(%2d, %2d) %s/%s\n", token.getBeginIndex(), token.getEndIndex(), token.getMorph(), token.getPos());
//        }

        List<String> nounList = analyzeResultList.getNouns();

        long processTime = System.currentTimeMillis()-startTime;

        for (String noun : nounList) {
            System.out.println(noun);
        }

        System.out.println("분석시간 : "+processTime);

    }



    public Set<SendType> getFirstSendChannel(UmsSendMsgBean umsSendMsgBean) throws Exception{
        if(!checkMacroCode(umsSendMsgBean.getSEND_MACRO_CODE())){
            // 등록되어 있지 않는 매로 코드일 경우 예외처리
            throw new MacroCodeException("등록되지 않은 매크로코드 입니다. 만약, 설정에 매크로코드가 있다면 등록한 채널이 올바른지 확인 요망.("+umsSendMsgBean.getSEND_MACRO_CODE()+")");
        }else{
            Set<MacroChannel> firstMacroChannels = macroFirstChannelMap.get(umsSendMsgBean.getSEND_MACRO_CODE());
            Set<SendType> nextSendTypeChannels = new HashSet<>();
            MacroChannel firstTierMacroChannel = null; // 첫번째 발송계층 중에 하나의 발송채널.
            for(MacroChannel fristMacroChannel : firstMacroChannels){
                firstTierMacroChannel = fristMacroChannel;
                getSendTypeChannel(umsSendMsgBean,fristMacroChannel,nextSendTypeChannels);
            }
            // 첫번째 발송계층에 사용자가 발송정보를 입력한 경우.
            if(nextSendTypeChannels.size()>0){
                return nextSendTypeChannels;
            }else {
                // 첫번째 발송계층 중 하나의 발송채널을 이용하여 다음발송계층 대체발송채널을 구한다.
                if (!isLastSendChannel(umsSendMsgBean.getSEND_MACRO_CODE(), firstTierMacroChannel)) {
                    Set<SendType> tmpNextSendTypeChannels = getNextSendChannel(umsSendMsgBean, firstTierMacroChannel);
                    if (tmpNextSendTypeChannels.size() > 0) {
                        nextSendTypeChannels.addAll(tmpNextSendTypeChannels);
                    }
                }
            }
            return nextSendTypeChannels;
        }
    }

    public Set<SendType> getNextSendChannel(UmsSendMsgBean umsSendMsgBean, MacroChannel failSendChannel) throws Exception{

        Set<SendType> nextSendTypeChannels = new HashSet<>();
        if(!checkMacroCode(umsSendMsgBean.getSEND_MACRO_CODE())){
            // 등록되어 있지 않는 매클로 코드일 경우 예외처리
            throw new MacroCodeException("등록되지 않은 매크로코드 입니다. 만약, 설정에 매크로코드가 있다면 등록한 채널이 올바른지 확인 요망.("+umsSendMsgBean.getSEND_MACRO_CODE()+")");
        }else{

            // 마지막 발송채널이 아닐경우 계속해서 마지막 발송채널까지 조회하는 로직 구현.
            if(isLastSendChannel(umsSendMsgBean.getSEND_MACRO_CODE(),failSendChannel)){
                // 발송마지막 채널이므로 대체발송 없음
                return nextSendTypeChannels;
            }else {
                Map<MacroChannel, Set<MacroChannel>> macroCodeOrderMap = macroMappingMap.get(umsSendMsgBean.getSEND_MACRO_CODE());

                // nextSendTypeChannels에 대체발송 채널 정보를 구해옴.
                Set<MacroChannel> nextMappingChannelSet = nextSendChannel(umsSendMsgBean, macroCodeOrderMap, failSendChannel, nextSendTypeChannels);

                if(nextSendTypeChannels.size()>0){
                    return nextSendTypeChannels;
                }else{
                    Set<MacroChannel> tmpNextMappingChannelSet = new HashSet<>();//
                    Set<MacroChannel> chkNextMappingChannelSet = new HashSet<>();
                    chkNextMappingChannelSet.addAll(nextMappingChannelSet);

                    while(chkNextMappingChannelSet.size() > 0) {
                        // 대체발송 매크로채널이 있었으나 사용자가 대체발송정보를 입력하지 않은 경우 해당. 다음 매크로 대체발송 채널 검증
                        tmpNextMappingChannelSet.clear(); //Macro 대체발송 채널이 존재했으나 사용자가 해당 채널에 발송메세지를 입력하지 않았을 경우 대체발송채널이 없으므로 그 다음 대체발송 채널을 찾기 위해
                        if (chkNextMappingChannelSet.size() > 0) {
                            for (MacroChannel nextMappingFailChannel : chkNextMappingChannelSet) {
                                // 마지막 대체발송 종단채널 여부 확인하여 종단 채널이 아닐경우 다음 매크로 대체발송 요청.
                                if (!isLastSendChannel(umsSendMsgBean.getSEND_MACRO_CODE(), nextMappingFailChannel)) {
                                    Set<MacroChannel> respNextMappingChannelSet = nextSendChannel(umsSendMsgBean, macroCodeOrderMap, nextMappingFailChannel, nextSendTypeChannels);
                                    tmpNextMappingChannelSet.addAll(respNextMappingChannelSet);
                                }
                            }
                            if (nextSendTypeChannels.size() > 0) {
                                return nextSendTypeChannels;
                            } else {
                                chkNextMappingChannelSet.clear();
                                chkNextMappingChannelSet.addAll(tmpNextMappingChannelSet);
                                continue;
                            }
                        }
                    }
                }
            }
        }
        return nextSendTypeChannels;
    }


    private Set<MacroChannel> nextSendChannel(UmsSendMsgBean umsSendMsgBean, Map<MacroChannel, Set<MacroChannel>> channelOrderMap, MacroChannel failSendMacroChannel, Set<SendType> nextSendTypeChannels){
        // macroMappingMap = {"MACRO_001":{"PUSH":["KKO"],"KKO":["SMS],"SMS":null}], "MACRO_002":{"PUSH":["KKO","RCS"],"KKO":["SMS"],"RCS":["SMS"],"SMS":null}}
        // channelOrderMap = {"PUSH":["KKO"],"KKO":["SMS],"SMS":null}

        Set<MacroChannel> nextMappingChannelSet = new HashSet<>(); // 다음 대체발송채널의 메세지등록을 않했을 경우 그 다음 맵핑된 대체발송채널 등록.

        // channelOrderMap과 umsSendMsgBean 발송정보를 이용하여 다음 대체발송 채널을 구한다.
        if(nextSendTypeChannels==null){
            nextSendTypeChannels = new HashSet<>();
        }

        // 발송실패채널의 대채발송이 매크로에 등록되어 있는지 확인하고 있을 경우 발송메세지입력을 확인한다.
        // 대체발송 매크로가 등록되어 있으나 발송매세지가 없을 경우는 다음 발송실패채널을 찾는다.
        if(channelOrderMap.containsKey(failSendMacroChannel)){
            // 대체발송 채널이 셋팅되어 있을 경우 해당 채널의 발송메세지 유무를 확인한다.
            Set<MacroChannel> nextMacroSendChannels = channelOrderMap.get(failSendMacroChannel);
            if(nextMacroSendChannels==null){
                // 해당 실패시 대체발송 채널이 없으므로 다음발송채널 빈값으로 리턴.
                return nextMappingChannelSet;
            }

            for(MacroChannel nextMacroChannel : nextMacroSendChannels){
                nextMappingChannelSet.add(nextMacroChannel);
                // 매크로에 정의된 채널로 사용자 입력값을 확인하여 실질적인 발송가능하면 nextSendTypeChannels에 등록한다.
                getSendTypeChannel(umsSendMsgBean, nextMacroChannel, nextSendTypeChannels);
            }

        }

        return nextMappingChannelSet;
    }

    /**
     * 매크로에 정의된 채널이 사용자 입력값을 확인하여 발송이 가능한지 확인하여 발송타입으로 변환처리 해주는 메쏘드
     */
    private void getSendTypeChannel(UmsSendMsgBean umsSendMsgBean, MacroChannel macroChannel, Set<SendType> nextSendTypeChannels){
        switch (macroChannel){
            case PUSH:
                if(!"".equals(umsSendMsgBean.getPUSH_MSG())){
                    nextSendTypeChannels.add(SendType.PUSH);
                }
                break;
            case KKO:
                if(!"".equals(umsSendMsgBean.getALLIMTOLK_TEMPLCODE())){
                    nextSendTypeChannels.add(SendType.KKOALT);
                }else if(!"".equals(umsSendMsgBean.getFRIENDTOLK_MSG())){
                    nextSendTypeChannels.add(SendType.KKOFRT);
                }
                break;
            case RCS:
                //이부분은 나중에 명확히 RCS 발송타입 구분을 확인하여 수정이 필요함.
                if(!"".equals(umsSendMsgBean.getRCS_TEMPL_ID())){
                    if(!"".equals(umsSendMsgBean.getRCS_OBJECT())){
                        //스타일 템플릿
                        nextSendTypeChannels.add(SendType.RCS_CELL);
                    }else{
                        // 서술형 템플릿

                    }
                }else if(!"".equals(umsSendMsgBean.getRCS_MSG())){
                    if(!"".equals(umsSendMsgBean.getRCS_IMG_PATH())){
                        nextSendTypeChannels.add(SendType.RCS_MMS);
                    }else{
                        if(umsSendMsgBean.getRCS_MSG().length()>90){
                            nextSendTypeChannels.add(SendType.RCS_LMS);
                        }else{
                            if("0".equals(umsSendMsgBean.getRCS_BTN_CNT())){

                            }else if("1".equals(umsSendMsgBean.getRCS_BTN_CNT())){
                                nextSendTypeChannels.add(SendType.RCS_SMS);
                            }else{
                                nextSendTypeChannels.add(SendType.RCS_LMS);
                            }
                        }
                    }
                }
                break;
            case SMS:
                if(!"".equals(umsSendMsgBean.getSMS_MSG())) {
                    if (!"".equals(umsSendMsgBean.getMMS_IMGURL()) || !"".equals(umsSendMsgBean.getSMS_TITLE())) {
                        nextSendTypeChannels.add(SendType.MMS);
                    } else {
                        try {
                            if (umsSendMsgBean.getSMS_MSG().getBytes("EUC-KR").length > 90) {
                                nextSendTypeChannels.add(SendType.LMS);
                            }else {
                                nextSendTypeChannels.add(SendType.SMS);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case NAVERT:
                break;
        }
    }

    /**
     * 최초 매크로코드 확인 요청시 설정한 매크로코드 셋팅하고 이후 부터는 요청한 매크로 코드가 사용가능한 매크로 코드인지 점검하여 리턴
     * @param macroCode
     * @return
     */
    private boolean checkMacroCode(String macroCode){
        // 최초 한번만 실행.
        if(macroMappingMap.size()==0){
            Map<String,String> propertiesMap = new HashMap<>();
            propertiesMap.put("MACRO_001"," PUSH > RCS > KKO>SMS");
            propertiesMap.put("MACRO_002"," PUSH,RCS,KKO");
            propertiesMap.put("MACRO_003"," PUSH > RCS,KKO>SMS");
            propertiesMap.put("MACRO_004"," PUSH,RCS,KKO,SMS");

            String[] MACRO_USE_CODE_ARR = MACRO_USE_CODE.split(","); // ex)MACRO_001,MACRO_002,MACRO_003
            for(int i=0; i<MACRO_USE_CODE_ARR.length; i++){
                String MACRO_CODE = MACRO_USE_CODE_ARR[i].trim(); //ex)MACRO_003
//                String macroOrder = UmsInitListener.webProperties.getProperty(MACRO_CODE,"").trim();  // ex)PUSH>KKO,RCS>SMS
                String macroOrder = propertiesMap.get(MACRO_CODE).trim();
                if(!"".equals(macroOrder)){
                    String[] macroOrderArr = macroOrder.split(">");
                    // Key = 발송채널, Value = 발송채널실패시 대체발송채널 정보
                    Map<MacroChannel, Set<MacroChannel>> macroOrderChannelMap = new HashMap<>(); // ex){"PUSH":{"KKO,RCS"}, "KKO":{"SMS"}, "RCS":{"SMS"}} 형태로 들어가야 함.

                    for(int j=0; j<macroOrderArr.length; j++){ // ex) macroOrderArr : ["PUSH","KKO,RCS","SMS"]
                        String keyChannel = macroOrderArr[j].trim();
                        // keyChannel이 쉼표로 배열일 수 있다. ex)["KKO,RCS"]
                        String[] keyChannelArr = keyChannel.split(",");
                        Set<MacroChannel> firstChannelSet = new HashSet<>();
                        Set<MacroChannel> lastChannelSet = new HashSet<>();
                        for(int z=0; z<keyChannelArr.length; z++){
                            String fromChannel = keyChannelArr[z].trim();
                            if(!MACRO_CHANNEL_MAP.containsKey(fromChannel)){
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                System.out.println("!!! 알수 없는 채널을 매크로에 등록(설정정보확인). 등록불가 :"+MACRO_CODE+" 알수없는 채널: "+keyChannel);
                                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                continue;
                            }
                            MacroChannel fromMacroChannel = MACRO_CHANNEL_MAP.get(fromChannel);
                            if(j==0){ //첫번째 매크로 발송채널일 경우
                                firstChannelSet.add(fromMacroChannel);
                            }
                            lastChannelSet.add(fromMacroChannel);
                            int nextIndex = j+1; // 발송실패시 대체발송 다음 채널이 있는지 확인을 위해
                            if(macroOrderArr.length>nextIndex) {
                                String toChannel = macroOrderArr[nextIndex].trim();
                                // 발송채널을 키로 대체발송채널리스트를 값으로 맵을 만들어 주는 메쏘드를 호출한다.
                                try {
                                    makeMacroOrderMap(macroOrderChannelMap, fromMacroChannel, toChannel); // 응답 정보 ex){"PUSH":["RCS","KKO"]}
                                }catch (Exception e){
                                    System.out.println("매크로 발송채널 대체발송채널 맵만드는 중 에러 : {}"+e.toString());
                                    e.printStackTrace();
                                    // 대체발송채널을 만드는 중 에러가 발생할 경우 발송채널에 대체발송없도록 넣믐
                                    macroOrderChannelMap.put(fromMacroChannel,null); //ex){"발송체널":null}
                                    continue;
                                }
                            }else if(macroOrderArr.length==nextIndex){
                                // 마지막일 경우 키만 넣고 값은 null로 넣는다.
                                macroOrderChannelMap.put(fromMacroChannel,null); //ex){"SMS":null}
                            }
                        }
                        if(j==0){//첫번째 매크로 발송채널일 경우 매크로 첫번째 발송채널 정보에 넣는다.
                            macroFirstChannelMap.put(MACRO_CODE,firstChannelSet);
                        }
                        macroLastChannelMap.put(MACRO_CODE,lastChannelSet); // 마지막 종단발송채널을 계속 덮어씌움.
                    }
                    macroMappingMap.put(MACRO_CODE,macroOrderChannelMap);
                }
            }
        }

        if(macroMappingMap.containsKey(macroCode)){
            return true;
        }else{
            return false;
        }

    }

    private void makeMacroOrderMap(Map<MacroChannel, Set<MacroChannel>> macroOrderMap, MacroChannel fromMacroChannel, String toChannel) throws Exception{
        String[] toChannelArr = toChannel.split(",");

        Set<MacroChannel> toChannelSet = new HashSet<>();
        for (int z = 0; z < toChannelArr.length; z++) {
            String toChannelVal = toChannelArr[z].trim();
            if (!MACRO_CHANNEL_MAP.containsKey(toChannelVal)) {
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("알수 없는 채널을 매크로에 등록(설정정보확인).알수 없는 채널:"+ toChannelVal);
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                macroOrderMap.put(fromMacroChannel, null);
            }else {
                toChannelSet.add(MACRO_CHANNEL_MAP.get(toChannelVal));
                macroOrderMap.put(fromMacroChannel, toChannelSet);
            }
        }
    }


    private MacroChannel sendTypeToMacroChannel(SendType sendType){
        switch (sendType){
            case PUSH:
                return MacroChannel.PUSH;
            case KKOALT:
                return MacroChannel.KKO;
            case KKOFRT:
                return MacroChannel.KKO;
            case RCS_SMS:
                return MacroChannel.RCS;
            case RCS_LMS:
                return MacroChannel.RCS;
            case RCS_MMS:
                return MacroChannel.RCS;
            case RCS_CELL:
                return MacroChannel.RCS;
            case SMS:
                return MacroChannel.SMS;
            case LMS:
                return MacroChannel.SMS;
            case MMS:
                return MacroChannel.SMS;
        }
        return null;
    }

    private boolean isLastSendChannel(String macroCode, MacroChannel macroChannel){
        boolean returnValue = true;

        if(macroLastChannelMap.containsKey(macroCode)){
            Set<MacroChannel> lastChannelSet = macroLastChannelMap.get(macroCode);
            if(lastChannelSet.contains(macroChannel)){
                returnValue = true;
            }else {
                returnValue = false;
            }
        }
        return returnValue;
    }
}
