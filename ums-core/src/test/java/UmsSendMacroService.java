import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.ehcache.RCSTemplateCache;
import kr.uracle.ums.core.exception.MacroCodeException;
import kr.uracle.ums.core.service.bean.MacroChannelCheckBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import kr.uracle.ums.core.vo.template.RCSTemplateVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class UmsSendMacroService {
    private Gson gson = new Gson();
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private String MACRO_USE_CODE = "MACRO_001,MACRO_002,MACRO_003,MACRO_004";

    private Map<String, MacroChannel> MACRO_CHANNEL_MAP = new HashMap<String, MacroChannel>();

    private Map<String, Map<MacroChannel, Set<MacroChannel>>> macroMappingMap = new HashMap<>(); // {"MACRO_001":{"PUSH":{"KKO"},"KKO":{"SMS},"SMS":null}}, "MACRO_002":{"PUSH":{"KKO","RCS"},"KKO":{"SMS"},"RCS":{"SMS"},"SMS":null}}
    private Map<String, Set<MacroChannel>> macroFirstChannelMap = new HashMap<>(); // 키 : 매크로코드, 값 : 시작 발송채널Set {"MACRO_001":{"PUSH"},"MACRO_002":{"PUSH","RCS","KKO","SMS"}}
    private Map<String, Set<MacroChannel>> macroLastChannelMap = new HashMap<>(); // 키 : 매크로코드, 값 : 마지막 종단 발송채널Set {"MACRO_001":{"SMS"}}


    public static enum MacroChannel {
        PUSH, NAVERT, KKO, RCS, SMS;
    }

    public static Map<String,String> propertiesMap = new HashMap<>();
    static {
        propertiesMap.put("MACRO_001"," PUSH > RCS > KKO>SMS");
        propertiesMap.put("MACRO_002"," PUSH,RCS,KKO");
        propertiesMap.put("MACRO_003"," PUSH > RCS,KKO>SMS");
        propertiesMap.put("MACRO_004"," PUSH,RCS,KKO,SMS");
    }


    public UmsSendMacroService(){
        MACRO_CHANNEL_MAP.put(MacroChannel.PUSH.toString(), MacroChannel.PUSH);
        MACRO_CHANNEL_MAP.put(MacroChannel.NAVERT.toString(), MacroChannel.NAVERT);
        MACRO_CHANNEL_MAP.put(MacroChannel.RCS.toString(), MacroChannel.RCS);
        MACRO_CHANNEL_MAP.put(MacroChannel.KKO.toString(), MacroChannel.KKO);
        MACRO_CHANNEL_MAP.put(MacroChannel.SMS.toString(), MacroChannel.SMS);
    }

    public Set<SendType> getFirstSendChannel(ReqUmsSendVo reqUmsSendVo) throws Exception{
        MacroChannelCheckBean macroChannelCheckBean = makeMacroChannelCheckBean(reqUmsSendVo);
        return getFirstSendChannel(macroChannelCheckBean);
    }
    public Set<SendType> getFirstSendChannel(UmsSendMsgBean umsSendMsgBean) throws Exception{
        MacroChannelCheckBean macroChannelCheckBean = makeMacroChannelCheckBean(umsSendMsgBean);
        return getFirstSendChannel(macroChannelCheckBean);
    }

    private Set<SendType> getFirstSendChannel(MacroChannelCheckBean macroChannelCheckBean) throws Exception{
        if(!checkMacroCode(macroChannelCheckBean.getSEND_MACRO_CODE())){
            // 등록되어 있지 않는 매로 코드일 경우 예외처리
            throw new MacroCodeException("등록되지 않은 매크로코드 입니다. 만약, 설정에 매크로코드가 있다면 등록한 채널이 올바른지 확인 요망.("+macroChannelCheckBean.getSEND_MACRO_CODE()+")");
        }else{
            Set<MacroChannel> firstMacroChannels = macroFirstChannelMap.get(macroChannelCheckBean.getSEND_MACRO_CODE());
            Set<SendType> nextSendTypeChannels = new HashSet<>();
            MacroChannel firstTierMacroChannel = null; // 첫번째 발송계층 중에 하나의 발송채널.
            for(MacroChannel fristMacroChannel : firstMacroChannels){
                firstTierMacroChannel = fristMacroChannel;
                getSendTypeChannel(macroChannelCheckBean,fristMacroChannel,nextSendTypeChannels);
            }
            // 첫번째 발송계층에 사용자가 발송정보를 입력한 경우.
            if(nextSendTypeChannels.size()>0){
                return nextSendTypeChannels;
            }else {
                // 첫번째 발송계층 중 하나의 발송채널을 이용하여 다음발송계층 대체발송채널을 구한다.
                if (!isLastSendChannel(macroChannelCheckBean.getSEND_MACRO_CODE(), firstTierMacroChannel)) {
                    Set<SendType> tmpNextSendTypeChannels = getNextSendChannel(macroChannelCheckBean, firstTierMacroChannel);
                    if (tmpNextSendTypeChannels.size() > 0) {
                        nextSendTypeChannels.addAll(tmpNextSendTypeChannels);
                    }
                }
            }
            return nextSendTypeChannels;
        }
    }

    public Set<SendType> getNextSendChannel(ReqUmsSendVo reqUmsSendVo, SendType sendType) throws Exception{
        MacroChannelCheckBean macroChannelCheckBean = makeMacroChannelCheckBean(reqUmsSendVo);
        return getNextSendChannel(macroChannelCheckBean, sendTypeToMacroChannel(sendType));
    }

    public Set<SendType> getNextSendChannel(UmsSendMsgBean umsSendMsgBean, SendType sendType) throws Exception{
        MacroChannelCheckBean macroChannelCheckBean = makeMacroChannelCheckBean(umsSendMsgBean);
        return getNextSendChannel(macroChannelCheckBean, sendTypeToMacroChannel(sendType));
    }

    private MacroChannelCheckBean makeMacroChannelCheckBean(UmsSendMsgBean umsSendMsgBean){
        MacroChannelCheckBean macroChannelCheckBean = new MacroChannelCheckBean();
        macroChannelCheckBean.setSEND_MACRO_CODE(umsSendMsgBean.getSEND_MACRO_CODE().trim());
        macroChannelCheckBean.setPUSH_MSG(umsSendMsgBean.getPUSH_MSG());
        macroChannelCheckBean.setALLIMTALK_MSG(umsSendMsgBean.getALLIMTALK_MSG());
        macroChannelCheckBean.setFRIENDTOLK_MSG(umsSendMsgBean.getFRIENDTOLK_MSG());

        macroChannelCheckBean.setRCS_MSGBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());
        macroChannelCheckBean.setRCS_TYPE(umsSendMsgBean.getRCS_TYPE());
        macroChannelCheckBean.setRCS_TEMPL_ID(umsSendMsgBean.getRCS_TEMPL_ID());

        macroChannelCheckBean.setSMS_MSG(umsSendMsgBean.getSMS_MSG());
        macroChannelCheckBean.setMMS_IMGURL(umsSendMsgBean.getMMS_IMGURL());
        macroChannelCheckBean.setSMS_TITLE(umsSendMsgBean.getSMS_TITLE());
        return macroChannelCheckBean;
    }

    private MacroChannelCheckBean makeMacroChannelCheckBean(ReqUmsSendVo reqUmsSendVo){
        MacroChannelCheckBean macroChannelCheckBean = new MacroChannelCheckBean();
        macroChannelCheckBean.setSEND_MACRO_CODE(reqUmsSendVo.getSEND_MACRO_CODE().trim());
        macroChannelCheckBean.setPUSH_MSG(reqUmsSendVo.getPUSH_MSG());
        macroChannelCheckBean.setALLIMTALK_MSG(reqUmsSendVo.getALLIMTALK_MSG());
        macroChannelCheckBean.setFRIENDTOLK_MSG(reqUmsSendVo.getFRIENDTOLK_MSG());

        macroChannelCheckBean.setRCS_MSGBASE_ID(reqUmsSendVo.getRCS_MSGBASE_ID());
        macroChannelCheckBean.setRCS_TYPE(reqUmsSendVo.getRCS_TYPE());
        macroChannelCheckBean.setRCS_TEMPL_ID(reqUmsSendVo.getALLIMTOLK_TEMPLCODE());

        macroChannelCheckBean.setSMS_MSG(reqUmsSendVo.getSMS_MSG());
        macroChannelCheckBean.setMMS_IMGURL(reqUmsSendVo.getMMS_IMGURL());
        macroChannelCheckBean.setSMS_TITLE(reqUmsSendVo.getSMS_TITLE());
        return macroChannelCheckBean;
    }

    private Set<SendType> getNextSendChannel(MacroChannelCheckBean macroChannelCheckBean, MacroChannel failSendChannel) throws Exception{

        Set<SendType> nextSendTypeChannels = new HashSet<>();
        if(!checkMacroCode(macroChannelCheckBean.getSEND_MACRO_CODE())){
            // 등록되어 있지 않는 매크로 코드일 경우 예외처리
            throw new MacroCodeException("등록되지 않은 매크로코드 입니다. 만약, 설정에 매크로코드가 있다면 등록한 채널이 올바른지 확인 요망.("+macroChannelCheckBean.getSEND_MACRO_CODE()+")");
        }else{

            // 마지막 발송채널이 아닐경우 계속해서 마지막 발송채널까지 조회하는 로직 구현.
            if(isLastSendChannel(macroChannelCheckBean.getSEND_MACRO_CODE(),failSendChannel)){
                // 발송마지막 채널이므로 대체발송 없음
                return nextSendTypeChannels;
            }else {
                Map<MacroChannel, Set<MacroChannel>> macroCodeOrderMap = macroMappingMap.get(macroChannelCheckBean.getSEND_MACRO_CODE());

                // nextSendTypeChannels에 대체발송 채널 정보를 구해옴.
                Set<MacroChannel> nextMappingChannelSet = nextSendChannel(macroChannelCheckBean, macroCodeOrderMap, failSendChannel, nextSendTypeChannels);

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
                                if (!isLastSendChannel(macroChannelCheckBean.getSEND_MACRO_CODE(), nextMappingFailChannel)) {
                                    Set<MacroChannel> respNextMappingChannelSet = nextSendChannel(macroChannelCheckBean, macroCodeOrderMap, nextMappingFailChannel, nextSendTypeChannels);
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


    private Set<MacroChannel> nextSendChannel(MacroChannelCheckBean macroChannelCheckBean, Map<MacroChannel, Set<MacroChannel>> channelOrderMap, MacroChannel failSendMacroChannel, Set<SendType> nextSendTypeChannels){
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
                getSendTypeChannel(macroChannelCheckBean, nextMacroChannel, nextSendTypeChannels);
            }
        }

        return nextMappingChannelSet;
    }

    /**
     * 매크로에 정의된 채널이 사용자 입력값을 확인하여 발송이 가능한지 확인하여 발송타입으로 변환처리 해주는 메쏘드
     */
    private void getSendTypeChannel(MacroChannelCheckBean macroChannelCheckBean, MacroChannel macroChannel, Set<SendType> nextSendTypeChannels){
        switch (macroChannel){
            case PUSH:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getPUSH_MSG())){
                    nextSendTypeChannels.add(SendType.PUSH);
                }
                break;
            case KKO:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getALLIMTALK_MSG())){
                    nextSendTypeChannels.add(SendType.KKOALT);
                }else if(StringUtils.isNotBlank(macroChannelCheckBean.getFRIENDTOLK_MSG())){
                    nextSendTypeChannels.add(SendType.KKOFRT);
                }
                break;
            case RCS:
                //TODO : 이부분은 나중에 명확히 RCS 발송타입 구분을 확인하여 수정이 필요함.\
                SendType sendType = getRCSSendType(macroChannelCheckBean);
                if(sendType == null)break;
                nextSendTypeChannels.add(sendType);
                break;
            case SMS:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getSMS_MSG())) {
                    // SMS는 90byte 제한, LMS 2,000byte 제한, title 40byte 제한
                    if ( StringUtils.isNotBlank(macroChannelCheckBean.getMMS_IMGURL()) ) {
                        nextSendTypeChannels.add(SendType.MMS);
                    } else {
                        try {
                            if (StringUtils.isNotBlank(macroChannelCheckBean.getSMS_TITLE())) {
                                nextSendTypeChannels.add(SendType.LMS);
                            }else if(macroChannelCheckBean.getSMS_MSG().getBytes("EUC-KR").length > 90){
                                nextSendTypeChannels.add(SendType.LMS);
                            }else {
                                nextSendTypeChannels.add(SendType.SMS);
                            }
                        } catch (Exception e) {
                            logger.error(e.toString());
                        }
                    }
                }
                break;
            case NAVERT:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getNAVER_MSG())){
                    nextSendTypeChannels.add(SendType.NAVERT);
                }
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
            String[] MACRO_USE_CODE_ARR = MACRO_USE_CODE.split(","); // ex)MACRO_001,MACRO_002,MACRO_003
            for(int i=0; i<MACRO_USE_CODE_ARR.length; i++){
                String MACRO_CODE = MACRO_USE_CODE_ARR[i].trim(); //ex)MACRO_003

                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                // 해당 부분 변경됨.
                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
                // String macroOrder = UmsInitListener.webProperties.getProperty(MACRO_CODE,"").trim();  // ex)PUSH>KKO,RCS>SMS
                String macroOrder = propertiesMap.get(MACRO_CODE).trim();
                ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
                                logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                logger.error("!!! 알수 없는 채널을 매크로에 등록(설정정보확인). 등록불가 : {} 알수없는 채널: {}",MACRO_CODE,keyChannel);
                                logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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
                                    logger.error("매크로 발송채널 대체발송채널 맵만드는 중 에러 : {}",e.toString());
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
                logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                logger.warn("알수 없는 채널을 매크로에 등록(설정정보확인).알수 없는 채널:"+ toChannelVal);
                logger.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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
            case RCS_FREE:
                return MacroChannel.RCS;
            case RCS_CELL:
                return MacroChannel.RCS;
            case RCS_DESC:
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

    public SendType getRCSSendType(MacroChannelCheckBean macroChannelCheckBean) {
        SendType sendType = null;

        String type = macroChannelCheckBean.getRCS_TYPE();
        String templId = macroChannelCheckBean.getRCS_TEMPL_ID();
        String msgbaseId = macroChannelCheckBean.getRCS_MSGBASE_ID();
        int btnCount = macroChannelCheckBean.getRCS_BTN_CNT();
        int imgCount = macroChannelCheckBean.getRCS_IMG_CNT();

        if(StringUtils.isBlank(templId)) {
            if(StringUtils.isBlank(type)|| StringUtils.isBlank(msgbaseId)) return null;
            try{
                sendType = SendType.valueOf(macroChannelCheckBean.getRCS_TYPE());
            }catch (IllegalArgumentException e) {
                logger.error("지원하지 않는 RCS 타입:{}", type);
                return null;
            }
        }else {
            RCSTemplateCache rcsTemplateCache = new RCSTemplateCache();
            RCSTemplateVo rcsTemplateVo = rcsTemplateCache.getTemplate(templId);
            if(rcsTemplateVo == null || StringUtils.isBlank(rcsTemplateVo.getRCS_TYPE())|| StringUtils.isBlank(rcsTemplateVo.getMESSAGEBASE_ID())) return null;

            if(StringUtils.isBlank(rcsTemplateVo.getCONTENT())){

            }
            if(StringUtils.isEmpty(msgbaseId)) msgbaseId = rcsTemplateVo.getMESSAGEBASE_ID();
            try {
                sendType = SendType.valueOf(rcsTemplateVo.getRCS_TYPE().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("지원하지 않는 RCS 타입:{}",rcsTemplateVo.getRCS_TYPE());
                return null;
            }

        }

        if(sendType == SendType.RCS_MMS) {
            if(Constants.getSendType(msgbaseId) != SendType.RCS_MMS) return null;
            if(Constants.getRcsMMSImageCount(msgbaseId) < imgCount) return null;
        }

        return sendType;
    }

    public int getRCSButtonCount(String rcsBtns )throws Exception {
        int btnCount =0;
        if(StringUtils.isBlank(rcsBtns)) return btnCount;
        List<Map<String, List<Object>>> list = gson.fromJson(rcsBtns, new TypeToken<List<Map<String, List<Object>>>>(){}.getType());
        for(Map<String, List<Object>> suggestionsMap : list) {
            List<Object> btnt = suggestionsMap.get("suggestions");
            if(btnt != null) btnCount += btnt.size();
        }
        return btnCount;
    }
}
