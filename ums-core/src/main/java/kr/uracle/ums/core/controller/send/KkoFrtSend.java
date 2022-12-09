package kr.uracle.ums.core.controller.send;

import com.google.gson.Gson;
import kr.uracle.ums.core.common.TargetUserKind;
import kr.uracle.ums.core.ehcache.CommonTemplateCacheMgr;
import kr.uracle.ums.core.exception.RequestErrException;
import kr.uracle.ums.core.exception.ValidationException;
import kr.uracle.ums.core.processor.bean.SentInfoBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.service.send.thread.*;
import kr.uracle.ums.core.vo.ReqUmsSendVo;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.*;

@SuppressWarnings("unchecked")
@Service
public class KkoFrtSend extends BaseSend {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private Properties myProperties = null;
    @Autowired(required=true)
    private MessageSource messageSource;
    @Autowired(required = true)
    private Gson gson;

    @Value("${UMS.NAS.YN:N}")
    private String NAS_USE_YN;

    @Autowired(required = true)
    private CommonTemplateCacheMgr commonTemplateCacheMgr;

    public Map<String,Object> send(ReqUmsSendVo reqUmsSendVo, String requestUri, Locale locale) throws Exception{
    	
    	// resoponseBody 변수
    	Map<String,Object> resultBodyMap = new HashMap<String,Object>();

        // STEP 2 :  공통 필수 항목 체크
        if(StringUtils.isBlank(reqUmsSendVo.getFRIENDTOLK_MSG())) throw new ValidationException("[FRIENDTOLK_MSG]", locale);
        if(StringUtils.isBlank(reqUmsSendVo.getKKOFRT_SVCID())) throw new RequestErrException("[친구톡]KKOFRT_SVCID is NOT Exist");
        
        // STEP 3 : 발송처리 데이타(UmsSendMsgBean) 만들기
        UmsSendMsgBean umsSendMsgBean = super.getUmsSendMsgBean(reqUmsSendVo);

        //예약 발송 여부
        boolean isReserve = false;
        if(StringUtils.isNotEmpty(umsSendMsgBean.getRESERVEDATE())) isReserve = true;
        
        // STEP 4 : 발송 종류 구분 :
        TargetUserKind type = null;
        try {
        	type = TargetUserKind.valueOf(reqUmsSendVo.getTARGET_USER_TYPE());        	
        }catch(IllegalArgumentException e) {
        	throw new ValidationException("[TARGET_USER_TYPE]", locale);
        }
        switch(type) {
	    	case AU:	// UMS회원 전체 
	    		AllUmsUserToRedisSender allUmsUserToRedisSender = new AllUmsUserToRedisSender(umsSendMsgBean, reqUmsSendVo.getLIMITSECOND(),reqUmsSendVo.getLIMITCNT());
	            resultBodyMap = allUmsUserToRedisSender.processInfo(); // 푸시 발송 정보
	            if(isReserve) return resultBodyMap;
	            allUmsUserToRedisSender.start();
	            break;
	    	case OU:	// 조직도회원 전체
	    		AllOrganUserToRedisSender allOrganUserToRedisSender = new AllOrganUserToRedisSender(umsSendMsgBean);
	            resultBodyMap = allOrganUserToRedisSender.processInfo();
	            if (isReserve) return resultBodyMap;
	            allOrganUserToRedisSender.start();
	            break;
	    	case MC:	// UMS회원 CSV - CSV파일 필수. #{아이디} 필수
//	    		resultBodyMap = umsCsvMemberSendService.umsFrtCsvMemberSend(umsSendMsgBean);
//	    		break;
	    		throw new ValidationException("[NOT SUPPORTED TARGET SEND TYPE]", locale);
	    	case NC:	// UMS비회원 CSV - CSV파일 필수. #{아이디} #{핸드폰번호} #{이름} 필수
	    		resultBodyMap = umsCsvSendService.umsFrtCsvSend(umsSendMsgBean);
	    		break;
	    	case NM:	// UMS비회원 - CUIDS파라터 필수. {"아이디":["핸드폰번호","이름"},...}
	    		if(StringUtils.isBlank(reqUmsSendVo.getCUIDS())) throw new ValidationException("[CUIDS]", locale);
	    		Map<String,List<String>> sendUsersMap = null;
	            try{
	            	sendUsersMap = gson.fromJson(reqUmsSendVo.getCUIDS(), Map.class);
	            }catch (Exception jsonEx){ throw new ValidationException("[CUIDS]", locale); }
	            
	            umsSendMsgBean.setTARGET_USERS(sendUsersMap); // 비회원 타겟팅 유저 셋팅.
	            resultBodyMap = umsSendService.umsFrtSend(umsSendMsgBean,locale);
	    		break;
	    	default:
	    		if(StringUtils.isBlank(reqUmsSendVo.getCUIDS())) throw new ValidationException("[CUIDS]", locale);
	    		
	    		resultBodyMap = umsSendMemberService.umsMemberUserFrtSend(umsSendMsgBean,locale);
	    		break;
        }

        /// 예약발송,발송완료 되었을 경우는 모두 완료처리로 함.
        Map<String,String> processMap = new HashMap<String, String>();
        processMap.put("processPercent", "100");
        processMap.put("processTxt", "Completed.");
        // 성공일 경우
        if(resultBodyMap.containsKey("processSeqno")){
            SentInfoBean sentInfoBean = sentInfoManager.getSentInfoMap(reqUmsSendVo.getTRANS_TYPE(), resultBodyMap.get("processSeqno").toString());
            if(sentInfoBean != null){
            	int processPercent = 0;
                int totalCnt = sentInfoBean.getREQ_SEND_CNT();
                int processCnt = sentInfoBean.getSUCC_CNT()+sentInfoBean.getFAIL_CNT();

                if(processCnt>0)processPercent = processCnt*100/totalCnt;
                if(processPercent == 0 && processCnt > 0) processPercent = 1;
                
                processMap.put("processPercent", ""+processPercent);
                processMap.put("processTxt", processCnt+"/"+totalCnt);
            } 
            resultBodyMap.put("processInfo", processMap);
        }
        resultBodyMap.put("processInfo", processMap);
        return resultBodyMap;

    }
}
