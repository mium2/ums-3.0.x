package kr.uracle.ums.core.controller.send;

import kr.uracle.ums.core.common.TargetUserKind;
import kr.uracle.ums.core.exception.ValidationException;
import kr.uracle.ums.core.processor.bean.SentInfoBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.service.send.thread.AllOrganUserToRedisSender;
import kr.uracle.ums.core.service.send.thread.AllUmsUserToRedisSender;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unchecked")
@Service
public class NaverSend extends BaseSend {

    protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());



    public Map<String,Object> send(ReqUmsSendVo reqUmsSendVo, String requestUri, Locale locale) throws Exception{

        // resoponseBody 변수
        Map<String,Object> resultBodyMap = new HashMap<String,Object>();

        // STEP 1-1 :  네이버톡 필수 항목 체크
        if(StringUtils.isBlank(reqUmsSendVo.getNAVER_TEMPL_ID())) throw new ValidationException("[NAVER_TEMPL_ID]", locale);

        // STEP 1-2 :  네이버톡 메세지 항목 체크
        if(StringUtils.isBlank(reqUmsSendVo.getNAVER_MSG())) throw new ValidationException("올바르지 않은 템플릿코드입니다. 네이버톡 템플릿코드을 외부에서 관리하신다면 네이버톡 메세지를 반드시 입력하셔야 합니다.", locale);


//        // STEP 2 :  필수 항목 체크
//        if(StringUtils.isBlank(reqUmsSendVo.getNAVER_PROFILE())) throw new ValidationException("[네이버 발송 프로필 정보가 없음.]", locale);
//
//        // STEP 3 :  필수 항목 체크
//        if(StringUtils.isBlank(reqUmsSendVo.getNAVER_PARTNERKEY())) throw new ValidationException("[네이버 발송 파트너키(NAVER_PARTNERKEY)가 없음.]", locale);

        // STEP 3 : 발송처리 데이타(UmsSendMsgBean) 만들기
        UmsSendMsgBean umsSendMsgBean = super.getUmsSendMsgBean(reqUmsSendVo);

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
        switch(type) {
            case AU:	// UMS회원 전체
                AllUmsUserToRedisSender allUmsUserToRedisSender = new AllUmsUserToRedisSender(umsSendMsgBean, reqUmsSendVo.getLIMITSECOND(),reqUmsSendVo.getLIMITCNT());
                resultBodyMap = allUmsUserToRedisSender.processInfo();
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
//                resultBodyMap = umsCsvMemberSendService.umsNaverCsvMemberSend(umsSendMsgBean);
//                break;
                throw new ValidationException("[NOT SUPPORTED TARGET SEND TYPE]", locale);
            case NC:	// UMS비회원 CSV - CSV파일 필수. #{아이디} #{핸드폰번호} #{이름} 필수
                resultBodyMap = umsCsvSendService.umsNaverCsvSend(umsSendMsgBean);
                break;
            case NM:	// UMS비회원 - CUIDS파라터 필수. {"아이디":["핸드폰번호","이름"},...}
                if(StringUtils.isBlank(reqUmsSendVo.getCUIDS())) throw new ValidationException("[CUIDS]", locale);
                Map<String, List<String>> sendUsersMap = null;
                try{
                    sendUsersMap = gson.fromJson(reqUmsSendVo.getCUIDS(), Map.class);
                }catch (Exception jsonEx){ throw new ValidationException("[CUIDS]", locale); }

                umsSendMsgBean.setTARGET_USERS(sendUsersMap); // 비회원 타겟팅 유저 셋팅.
                resultBodyMap = umsSendService.umsNaverSend(umsSendMsgBean,locale);
                break;
            default:
                if(StringUtils.isBlank(reqUmsSendVo.getCUIDS())) throw new ValidationException("[CUIDS]", locale);
                resultBodyMap = umsSendMemberService.umsMemberUserNaverSend(umsSendMsgBean,locale);
                break;
        }

        /// 예약발송,발송완료 되었을 경우는 모두 완료처리로 함.
        Map<String,String> processMap = new HashMap<String, String>();
        processMap.put("processPercent","100");
        processMap.put("processTxt","Completed.");
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