package kr.uracle.ums.core.controller.send;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.core.common.TargetUserKind;
import kr.uracle.ums.core.exception.ValidationException;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.service.send.thread.AllOrganUserToRedisSender;
import kr.uracle.ums.core.service.send.thread.AllUmsUserToRedisSender;
import kr.uracle.ums.core.service.send.thread.BaseThreadRedis;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RcsSend extends BaseSend {
	protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	

	
	@Override
	protected Map<String,Object> send(ReqUmsSendVo reqUmsSendVo, String requestUri, Locale locale) throws Exception {
		
    	// resoponseBody 
    	Map<String,Object> resultBodyMap = new HashMap<String,Object>();
    	
    	// STEP 1 :  공통 필수 항목 체크
    	if( StringUtils.isBlank(reqUmsSendVo.getRCS_OBJECT()) && StringUtils.isBlank(reqUmsSendVo.getRCS_MSG()))throw new ValidationException("[RCS_MSG]", locale);
		if(StringUtils.isNotBlank(reqUmsSendVo.getRCS_OBJECT())){
			try{
				gson.fromJson(reqUmsSendVo.getRCS_OBJECT(), Map.class);
			}catch(JsonSyntaxException e){
				throw new ValidationException("RCS_OBJECT is Not Json Format", locale);
			}
		}
    	
        // STEP 3 : 발송처리 데이타(UmsSendMsgBean) 만들기
        UmsSendMsgBean umsSendMsgBean = super.getUmsSendMsgBean(reqUmsSendVo);
        umsSendMsgBean.setRCS_TYPE(reqUmsSendVo.getSEND_TYPE().toString());
        
    	//예약 발송 여부
        boolean isReserve = false;
        if(StringUtils.isNotEmpty(umsSendMsgBean.getRESERVEDATE())) isReserve = true;
        
    	// STEP 4 : 발송 종류 구분 (AU=>전체UMS회원, OU=>전체조직도유저, MU=>UMS 아이디로 타겟팅, MP=>푸시아이디로 타겟팅, NM=>비회원/핸드폰번호필수, MC=>회원CSV, NC=>비회원CSV)
        TargetUserKind type = null;
        try {
        	type = TargetUserKind.valueOf(reqUmsSendVo.getTARGET_USER_TYPE());        	
        }catch(IllegalArgumentException e) {
        	throw new ValidationException("[TARGET_USER_TYPE]", locale);
        }
        
        BaseThreadRedis sendThreadRedis = null;
        switch(type) {
    	case AU:	// UMS회원 전체 
    		 sendThreadRedis = new AllUmsUserToRedisSender(umsSendMsgBean, reqUmsSendVo.getLIMITSECOND(),reqUmsSendVo.getLIMITCNT());
             resultBodyMap = sendThreadRedis.processInfo(); 
             if(isReserve) return resultBodyMap;
             sendThreadRedis.start();
             break;
    	case OU:	// 조직도회원 전체
    		 sendThreadRedis = new AllOrganUserToRedisSender(umsSendMsgBean);
             resultBodyMap = sendThreadRedis.processInfo();
             if (isReserve) return resultBodyMap;
             sendThreadRedis.start();
             break;
    	case MC:	// UMS회원 CSV - CSV파일 필수. #{아이디} 필수
//    		resultBodyMap = umsCsvMemberSendService.umsRCSCsvMemberSend(umsSendMsgBean);
//    		break;
    		throw new ValidationException("[NOT SUPPORTED TARGET SEND TYPE]", locale);
    	case NC:	// UMS비회원 CSV - CSV파일 필수. #{아이디} #{핸드폰번호} #{이름} 필수
    		resultBodyMap = umsCsvSendService.umsRcsCsvSend(umsSendMsgBean);
    		break;
    	case NM:	// UMS비회원 - CUIDS파라터 필수. {"아이디":["핸드폰번호","이름"],...}
    		if(StringUtils.isBlank(reqUmsSendVo.getCUIDS())) throw new ValidationException("[CUIDS]", locale);
    		Map<String,List<String>> sendUsersMap = null;
            try{
            	sendUsersMap = gson.fromJson(reqUmsSendVo.getCUIDS(), new TypeToken<Map<String,List<String>>>(){}.getType());
            }catch (Exception jsonEx){ throw new ValidationException("[CUIDS]", locale); }
            
            // 대상자 정보 없으면 실패 처리
            if(ObjectUtils.isEmpty(sendUsersMap))throw new ValidationException("[CUIDS]", locale);
            
            umsSendMsgBean.setTARGET_USERS(sendUsersMap); // 비회원 타겟팅 유저 셋팅.
            resultBodyMap = umsSendService.umsRcsSend(umsSendMsgBean);
    		break;
    	default:
    		if(StringUtils.isBlank(reqUmsSendVo.getCUIDS())) throw new ValidationException("[CUIDS]", locale);
            resultBodyMap = umsSendMemberService.umsMemberUserRCSSend(umsSendMsgBean);
    		break;
    }
        
		return resultBodyMap;
	}


}
