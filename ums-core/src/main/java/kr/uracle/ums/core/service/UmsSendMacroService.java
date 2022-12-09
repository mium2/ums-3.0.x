package kr.uracle.ums.core.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.dao.common.InitDataDao;
import kr.uracle.ums.core.ehcache.RCSTemplateCache;
import kr.uracle.ums.core.exception.MacroCodeException;
import kr.uracle.ums.core.service.bean.MacroChannelCheckBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class UmsSendMacroService {
	
	private final long REFRESH_MACROMAP_CYCLE = 3*60*1000;
	private long lastrefreshTime = 0;
	
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
   
    @Autowired(required=true)
    private InitDataDao initDataDao;
    
    @Autowired(required = true)
    private RCSTemplateCache rcsTemplateCache;
    
    @Autowired(required = true)
    protected Gson gson;

    @Value("${LOTTE.BARCODE.USEYN:N}")
    private String LOTTE_BARCODE_USEYN;
    
    private Map<String,MacroChannel> MACRO_CHANNEL_MAP = new HashMap<String,MacroChannel>();
    
    private Map<String, String> macroMap = new HashMap<>();//키:매크로코드 값:메크로코드내용 {"MACRO_001":"PUSH>KKO>SMS"}
    private Map<String, Map<MacroChannel, Set<MacroChannel>>> macroMappingMap = new HashMap<>(); // {"MACRO_001":{"PUSH":{"KKO"},"KKO":{"SMS},"SMS":null}}, "MACRO_002":{"PUSH":{"KKO","RCS"},"KKO":{"SMS"},"RCS":{"SMS"},"SMS":null}}
    private Map<String, Set<MacroChannel>> macroFirstChannelMap = new HashMap<>(); // 키 : 매크로코드, 값 : 시작 발송채널Set {"MACRO_001":{"PUSH"},"MACRO_002":{"PUSH","RCS","KKO","SMS"}}
    private Map<String, Set<MacroChannel>> macroLastChannelMap = new HashMap<>(); // 키 : 매크로코드, 값 : 마지막 종단 발송채널Set {"MACRO_001":{"SMS"}}


    public static enum MacroChannel {
        PUSH, WPUSH, NAVERT, KKO, RCS, SMS;
    }

    public UmsSendMacroService(){
        MACRO_CHANNEL_MAP.put(MacroChannel.PUSH.toString(),MacroChannel.PUSH);
        MACRO_CHANNEL_MAP.put(MacroChannel.WPUSH.toString(),MacroChannel.WPUSH);
        MACRO_CHANNEL_MAP.put(MacroChannel.NAVERT.toString(),MacroChannel.NAVERT);
        MACRO_CHANNEL_MAP.put(MacroChannel.RCS.toString(),MacroChannel.RCS);
        MACRO_CHANNEL_MAP.put(MacroChannel.KKO.toString(),MacroChannel.KKO);
        MACRO_CHANNEL_MAP.put(MacroChannel.SMS.toString(),MacroChannel.SMS);
    }

    public Set<SendType> getFirstSendChannel(ReqUmsSendVo reqUmsSendVo) throws Exception{
        MacroChannelCheckBean macroChannelCheckBean = makeMacroChannelCheckBean(reqUmsSendVo);
        Set<SendType> sendTypeSet = getFirstSendChannel(macroChannelCheckBean);
        
        String macroCode = macroChannelCheckBean.getSEND_MACRO_CODE();
        if(StringUtils.isNotBlank(macroCode)) {
//        	reqUmsSendVo.setSEND_MACRO_ORDER(getMacroContent(macroCode));
        	reqUmsSendVo.setSEND_MACRO_ORDER(getMacroContent(macroCode,macroChannelCheckBean));
        }else {
//        	reqUmsSendVo.setSEND_MACRO_ORDER(getMacroContent("MACRO_001"));
            reqUmsSendVo.setSEND_MACRO_ORDER(getMacroContent("MACRO_001",macroChannelCheckBean));
        }
        
        return sendTypeSet;
    }

    private Set<SendType> getFirstSendChannel(MacroChannelCheckBean macroChannelCheckBean) throws Exception{
    	// 등록되어 있지 않는 매크로 코드일 경우 예외처리
        if(checkMacroCode(macroChannelCheckBean.getSEND_MACRO_CODE()) == false){
            throw new MacroCodeException("등록되지 않은 매크로코드 입니다. 만약, 설정에 매크로코드가 있다면 등록한 채널이 올바른지 확인 요망.("+macroChannelCheckBean.getSEND_MACRO_CODE()+")");
        }
        // 요청한 매크로코드의 첫번째 발송해야할 채널리스트를 받는다.
        Set<MacroChannel> firstMacroChannels = macroFirstChannelMap.get(macroChannelCheckBean.getSEND_MACRO_CODE());
        Set<SendType> nextSendTypeChannels = new HashSet<>();
        MacroChannel firstTierMacroChannel = null; // 첫번째 발송계층 중에 하나의 발송채널.
        // 첫번째 매크로에 등록된 발송할 채널들 중 실제적으로 해당채널에 발송메세지를 입력했는지를 확인하여 실직적은 첫번째 발송채널인지 확인한다.
        for(MacroChannel fristMacroChannel : firstMacroChannels){
            firstTierMacroChannel = fristMacroChannel;
            getSendTypeChannel(macroChannelCheckBean,fristMacroChannel,nextSendTypeChannels);
        }
        // 첫번째 발송계층에 사용자가 발송정보를 입력한 경우.
        if(nextSendTypeChannels.size()>0) return nextSendTypeChannels;
        
        
        // 첫번째 발송계층 중 하나의 발송채널을 이용하여 다음발송계층 대체발송채널을 구한다.
        if (isLastSendChannel(macroChannelCheckBean.getSEND_MACRO_CODE(), firstTierMacroChannel) == false) {
            Set<SendType> tmpNextSendTypeChannels = getNextSendChannel(macroChannelCheckBean, firstTierMacroChannel);
            if (tmpNextSendTypeChannels.size() > 0) {
                nextSendTypeChannels.addAll(tmpNextSendTypeChannels);
            }
        }
        
        return nextSendTypeChannels;
        
    }

    public Set<SendType> getNextSendChannelSet(UmsSendMsgBean umsSendMsgBean, SendType sendType) throws Exception{
    	Set<SendType> nextSendTypeChannels = new HashSet<>();
    	
    	MacroChannelCheckBean macroChannelCheckBean = makeMacroChannelCheckBean(umsSendMsgBean);
    	String channel =  changeTypeSendToMacro(sendType.toString());
   
    	List<String> channelOrder = Arrays.asList(umsSendMsgBean.getSEND_MACRO_ORDER().split(">")) ;
    	if(channelOrder == null || channelOrder.size()<=0)return nextSendTypeChannels;
    	int index = channelOrder.indexOf(channel);
        if(index<0) return nextSendTypeChannels;
    	for(int i = index+1; i<channelOrder.size(); i++) {
    		String nextChannel =  channelOrder.get(i);
    		String[] nextChannelArray = nextChannel.split(",");
    		for(String macroChannel :nextChannelArray) {
    			if(macroChannel.startsWith("KKO")) {
    				macroChannel = "KKO";
    			}
    			getSendTypeChannel(macroChannelCheckBean, MacroChannel.valueOf(macroChannel), nextSendTypeChannels);
    		}
    		if(nextSendTypeChannels.size()>0){
    		    break;
            }
    	}
        return nextSendTypeChannels;
    }

    private String changeTypeSendToMacro(String sendType) {
    	String macroType = sendType;
    	switch(sendType) {
    		case "RCS_SMS": case "RCS_LMS": case "RCS_MMS": case "RCS_FREE": case "RCS_CELL": case "RCS_DESC":
	    		macroType = "RCS";
	    		break;
//            case "KKOALT": case "KKOFRT":
//                macroType = "KKO";
//                break;
            case "SMS": case "LMS:": case "MMS:":
                macroType = "SMS";
                break;
    	}
    	return macroType;
    }

    private MacroChannelCheckBean makeMacroChannelCheckBean(UmsSendMsgBean umsSendMsgBean){
        MacroChannelCheckBean macroChannelCheckBean = new MacroChannelCheckBean();
        macroChannelCheckBean.setSEND_MACRO_CODE(umsSendMsgBean.getSEND_MACRO_CODE().trim());
        macroChannelCheckBean.setPUSH_MSG(umsSendMsgBean.getPUSH_MSG());
        macroChannelCheckBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
        macroChannelCheckBean.setALLIMTALK_MSG(umsSendMsgBean.getALLIMTALK_MSG());
        macroChannelCheckBean.setFRIENDTOLK_MSG(umsSendMsgBean.getFRIENDTOLK_MSG());
        macroChannelCheckBean.setRCS_TYPE(umsSendMsgBean.getRCS_TYPE());
        macroChannelCheckBean.setRCS_TEMPL_ID(umsSendMsgBean.getRCS_TEMPL_ID());
        macroChannelCheckBean.setRCS_MSGBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());
        if(ObjectUtils.isNotEmpty(umsSendMsgBean.getRCS_IMG_PATH())) {
            macroChannelCheckBean.setRCS_IMG_CNT(umsSendMsgBean.getRCS_IMG_PATH().size());
        }
        macroChannelCheckBean.setRCS_BTN_CNT(Integer.parseInt(umsSendMsgBean.getRCS_BTN_CNT()));
        macroChannelCheckBean.setSMS_MSG(umsSendMsgBean.getSMS_MSG());
        macroChannelCheckBean.setMMS_IMGURL(umsSendMsgBean.getMMS_IMGURL());
        macroChannelCheckBean.setSMS_TITLE(umsSendMsgBean.getSMS_TITLE());
        macroChannelCheckBean.setNAVER_MSG(umsSendMsgBean.getNAVER_MSG());
        macroChannelCheckBean.setVAR9(umsSendMsgBean.getVAR9());
        return macroChannelCheckBean;
    }

    private MacroChannelCheckBean makeMacroChannelCheckBean(ReqUmsSendVo reqUmsSendVo) throws Exception{
        MacroChannelCheckBean macroChannelCheckBean = new MacroChannelCheckBean();
        macroChannelCheckBean.setSEND_MACRO_CODE(reqUmsSendVo.getSEND_MACRO_CODE().trim());
        macroChannelCheckBean.setPUSH_MSG(reqUmsSendVo.getPUSH_MSG());
        macroChannelCheckBean.setWPUSH_MSG(reqUmsSendVo.getWPUSH_MSG());
        macroChannelCheckBean.setALLIMTALK_MSG(reqUmsSendVo.getALLIMTALK_MSG());
        macroChannelCheckBean.setFRIENDTOLK_MSG(reqUmsSendVo.getFRIENDTOLK_MSG());
        macroChannelCheckBean.setRCS_TYPE(reqUmsSendVo.getRCS_TYPE());
        macroChannelCheckBean.setRCS_TEMPL_ID(reqUmsSendVo.getRCS_TEMPL_ID());
        macroChannelCheckBean.setRCS_MSGBASE_ID(reqUmsSendVo.getRCS_MSGBASE_ID());
        macroChannelCheckBean.setNAVER_MSG(reqUmsSendVo.getNAVER_MSG());
        macroChannelCheckBean.setVAR9(reqUmsSendVo.getVAR9());
        // RCS 이미지 갯수
        if(StringUtils.isNotBlank(reqUmsSendVo.getRCS_IMG_PATH())){
        	try {				
        		List<String> imgList = gson.fromJson(reqUmsSendVo.getRCS_IMG_PATH(), new TypeToken<List<String>>(){}.getType());
        		macroChannelCheckBean.setRCS_IMG_CNT(imgList.size());
			} catch (Exception e) {
				throw new Exception("RCS IMG PATH fommat error, ERROR MESSAGE : "+e.getMessage());
			}
        }
        if(ObjectUtils.isNotEmpty(reqUmsSendVo.getRCS_IMG_FILES())) macroChannelCheckBean.setRCS_IMG_CNT(reqUmsSendVo.getRCS_IMG_FILES().length);
         
        // RCS 버튼 갯수
        macroChannelCheckBean.setRCS_BTN_CNT(0);
        if(StringUtils.isNotBlank(reqUmsSendVo.getBTN_OBJECT())) {
        	try {
				int count = this.getRCSButtonCount(reqUmsSendVo.getBTN_OBJECT());
				macroChannelCheckBean.setRCS_BTN_CNT(count);
			} catch (Exception e) {
				throw new Exception("RCS 버튼 포맷 에러"+e.toString());
			}
        }
        macroChannelCheckBean.setSMS_MSG(reqUmsSendVo.getSMS_MSG());
        macroChannelCheckBean.setMMS_IMGURL(reqUmsSendVo.getMMS_IMGURL());
        if(reqUmsSendVo.getMMS_IMG_FILES() != null && reqUmsSendVo.getMMS_IMG_FILES().length>1)macroChannelCheckBean.setMMS_IMGURL("MMS");
        macroChannelCheckBean.setSMS_TITLE(reqUmsSendVo.getSMS_TITLE());
        return macroChannelCheckBean;
    }

    private Set<SendType> getNextSendChannel(MacroChannelCheckBean macroChannelCheckBean, MacroChannel failSendChannel) throws Exception{

        Set<SendType> nextSendTypeChannels = new HashSet<>();
        String macroCode = macroChannelCheckBean.getSEND_MACRO_CODE();
        // 등록되어 있지 않는 매크로 코드일 경우 예외처리
        if(checkMacroCode(macroCode) == false){
            throw new MacroCodeException("등록되지 않은 매크로코드 입니다. 만약, 설정에 매크로코드가 있다면 등록한 채널이 올바른지 확인 요망.("+macroCode+")");
        }
      
        // 마지막 발송채널이 아닐경우 계속해서 마지막 발송채널까지 조회하는 로직 구현. 발송마지막 채널이므로 대체발송 없음
        if(isLastSendChannel(macroCode,failSendChannel)){
            return nextSendTypeChannels;
        }

        Map<MacroChannel, Set<MacroChannel>> macroCodeOrderMap = macroMappingMap.get(macroCode);

        // nextSendTypeChannels에 대체발송 채널 정보를 구해옴.
        Set<MacroChannel> nextMappingChannelSet = nextSendChannel(macroChannelCheckBean, macroCodeOrderMap, failSendChannel, nextSendTypeChannels);

        if(nextSendTypeChannels.size()>0){
            return nextSendTypeChannels;
        }
       
        Set<MacroChannel> tmpNextMappingChannelSet = new HashSet<>();
        Set<MacroChannel> chkNextMappingChannelSet = new HashSet<>();
        chkNextMappingChannelSet.addAll(nextMappingChannelSet);

        while(chkNextMappingChannelSet.size() > 0) {
            // 대체발송 매크로채널이 있었으나 사용자가 대체발송정보를 입력하지 않은 경우 해당. 다음 매크로 대체발송 채널 검증
            tmpNextMappingChannelSet.clear(); //Macro 대체발송 채널이 존재했으나 사용자가 해당 채널에 발송메세지를 입력하지 않았을 경우 대체발송채널이 없으므로 그 다음 대체발송 채널을 찾기 위해
            if (chkNextMappingChannelSet.size() > 0) {
                for (MacroChannel nextMappingFailChannel : chkNextMappingChannelSet) {
                    // 마지막 대체발송 종단채널 여부 확인하여 종단 채널이 아닐경우 다음 매크로 대체발송 요청.
                    if (!isLastSendChannel(macroCode, nextMappingFailChannel)) {
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
            case WPUSH:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getWPUSH_MSG())){
                    nextSendTypeChannels.add(SendType.WPUSH);
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
                // RCS는 RCS_TYPE으로 타입을 지정한다.
            	if(StringUtils.isBlank(macroChannelCheckBean.getRCS_TYPE()))break;
            	try {
            		SendType sendType = SendType.valueOf(macroChannelCheckBean.getRCS_TYPE());            		
            		nextSendTypeChannels.add(sendType);
            	}catch(Exception e) { break; }
            	
                break;
            case SMS:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getSMS_MSG())) {
                    // SMS는 90byte 제한, LMS 2,000byte 제한, title 40byte 제한
                    if ( StringUtils.isNotBlank(macroChannelCheckBean.getMMS_IMGURL())) {
                    	nextSendTypeChannels.add(SendType.MMS);
                    } else if(LOTTE_BARCODE_USEYN.equalsIgnoreCase("Y") && StringUtils.isNotBlank(macroChannelCheckBean.getVAR9())){
                        nextSendTypeChannels.add(SendType.MMS);
                    }else {
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
    	long now = System.currentTimeMillis();
        if(macroMappingMap.size()==0 || now-lastrefreshTime>REFRESH_MACROMAP_CYCLE){
            try {
            	lastrefreshTime = now;
                List<Map<String, Object>> macroCodeList = initDataDao.selMacroCodeInfo(new HashMap<String, Object>());
                if(macroCodeList!=null && macroCodeList.size()>0){
                    for(int i=0; i<macroCodeList.size(); i++){
                        Map<String,Object> macroInfoMap = macroCodeList.get(i);
                        String MACRO_CODE = macroInfoMap.get("MACROCODE").toString().trim(); //ex)MACRO_003
                        String macroOrder =  macroInfoMap.get("MACRO_ORDER").toString().trim();  // ex)PUSH>KKO,RCS>SMS
                        // KKOALT, KKOFRT ==> KKO로 치환처리
                        macroOrder = macroOrder.replace("KKOALT", "KKO")
                                .replace("KKOFRT","KKO");
                        if(!"".equals(macroOrder)){
                        	macroMap.put(MACRO_CODE, macroOrder);
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
                }else{
                    logger.error("DB에 매크로코드 셋팅이 되어 있지 않습니다.");
                    return false;
                }
            }catch (Exception e){
                e.printStackTrace();
                logger.error("DB에 연결이 되지 않았거나 쿼리에러가 발생하였습니다.");
                logger.error(e.toString());
                return false;
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
            case WPUSH:
                return MacroChannel.WPUSH;
            case KKOALT: case KKOFRT:
                return MacroChannel.KKO;
            case RCS_SMS: case RCS_LMS: case RCS_MMS: case RCS_FREE: case RCS_CELL: case RCS_DESC:
                return MacroChannel.RCS;
            case SMS: case LMS: case MMS:
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
    
    public boolean isMacroSendChannel(String macroCode, SendType sendType) {
    	Map<MacroChannel, Set<MacroChannel>> channels = macroMappingMap.get(macroCode);
    	MacroChannel macroChannel = sendTypeToMacroChannel(sendType);
    	return channels.containsKey(macroChannel);
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

    public String getMacroContent(String macroCode, MacroChannelCheckBean macroChannelCheckBean){
        String macroOrderStr = macroMap.get(macroCode);
        if(macroOrderStr==null){
            return "";
        }
        List<String> channelOrder = Arrays.asList(macroOrderStr.split(">")) ;
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i<channelOrder.size(); i++) {
            String nextChannel =  channelOrder.get(i);
            String[] nextChannelArray = nextChannel.split(",");
            String sendMacroChannel = "";
            for(int j=0; j<nextChannelArray.length; j++) {
                String chkMacroChannel = nextChannelArray[j];
                String macroChannel = getSendMacroChannel(macroChannelCheckBean, MacroChannel.valueOf(chkMacroChannel));
                if(macroChannel==null) continue;
                if("".equals(sendMacroChannel)){
                    sendMacroChannel = macroChannel;
                }else{
                    sendMacroChannel = sendMacroChannel+","+macroChannel;
                }
            }
            if(!"".equals(sendMacroChannel)){
                if(sb.length()==0){
                    sb.append(sendMacroChannel);
                }else{
                    sb.append(">"+sendMacroChannel);
                }
            }

        }
        return sb.toString();
    }

    private String getSendMacroChannel(MacroChannelCheckBean macroChannelCheckBean, MacroChannel macroChannel){
        String returnMacroChannel = null;
        switch (macroChannel){
            case PUSH:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getPUSH_MSG())){
                    returnMacroChannel = MacroChannel.PUSH.toString();
                }
                break;
            case WPUSH:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getWPUSH_MSG())){
                    returnMacroChannel = MacroChannel.WPUSH.toString();
                }
                break;
            case KKO:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getALLIMTALK_MSG())){
                    returnMacroChannel = SendType.KKOALT.toString();
                }else if(StringUtils.isNotBlank(macroChannelCheckBean.getFRIENDTOLK_MSG())){
                    returnMacroChannel = SendType.KKOFRT.toString();
                }
                break;
            case RCS:
                // RCS는 RCS_TYPE으로 타입을 지정한다.
                if(StringUtils.isNotBlank(macroChannelCheckBean.getRCS_TYPE())){
                    returnMacroChannel = MacroChannel.RCS.toString();
                }
                break;
            case SMS:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getSMS_MSG())) {
                    returnMacroChannel = MacroChannel.SMS.toString();
                }
                break;
            case NAVERT:
                if(StringUtils.isNotBlank(macroChannelCheckBean.getNAVER_MSG())){
                    returnMacroChannel = MacroChannel.NAVERT.toString();
                }
                break;
        }
        return returnMacroChannel;
    }
}
