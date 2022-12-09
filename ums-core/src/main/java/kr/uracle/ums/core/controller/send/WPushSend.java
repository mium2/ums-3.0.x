package kr.uracle.ums.core.controller.send;

import com.google.gson.Gson;
import kr.uracle.ums.core.common.TargetUserKind;
import kr.uracle.ums.core.exception.ValidationException;
import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.processor.bean.SentInfoBean;
import kr.uracle.ums.core.service.*;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.service.send.thread.AllOrganUserToRedisSender;
import kr.uracle.ums.core.service.send.thread.AllPushUserToRedisSender;
import kr.uracle.ums.core.service.send.thread.AllUmsUserToRedisSender;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@SuppressWarnings("unchecked")
public class WPushSend extends BaseSend {
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
    private UmsSendMacroService umsSendMacroService;
    @Autowired(required = true)
    private SentInfoManager sentInfoManager;
    @Autowired(required = true)
    private UmsCsvSendService umsCsvSendService;
    @Autowired(required = true)
    private UmsCsvMemberSendService umsCsvMemberSendService;
    @Autowired(required = true)
    private UmsSendService umsSendService;
    @Autowired(required = true)
    private UmsSendMemberService umsSendMemberService;


    public Map<String,Object> send(ReqUmsSendVo reqUmsSendVo, String requestUri, Locale locale) throws Exception{
        // resoponseBody 변수
        Map<String,Object> resultBodyMap = new HashMap<String,Object>();

        // STEP 1 :  공통 필수 항목 체크
        // 웹푸시 도메인 체크
        if(StringUtils.isBlank(reqUmsSendVo.getWPUSH_DOMAIN())) throw new ValidationException("[WPUSH_DOMAIN]", locale);

        // STEP 2 :  웹푸시메세지 체크
        if(StringUtils.isBlank(reqUmsSendVo.getWPUSH_MSG())) throw new ValidationException("[WPUSH_MSG]", locale);

        // STEP 3 : 발송처리 데이타(UmsSendMsgBean) 만들기
        UmsSendMsgBean umsSendMsgBean = super.getUmsSendMsgBean(reqUmsSendVo);

        //예약 발송 여부
        boolean isReserve = false;
        if(StringUtils.isNotBlank(umsSendMsgBean.getRESERVEDATE())) isReserve = true;

        // STEP 4 : 발송 종류 구분 (AU=>전체UMS회원, AP=>전체푸시유저, OU=>전체조직도유저, MU=>UMS 아이디로 타겟팅, MP=>푸시아이디로 타겟팅, NM=>비회원/핸드폰번호필수, MC=>회원CSV, NC=>비회원CSV)
        TargetUserKind type = null;
        try {
            type = TargetUserKind.valueOf(reqUmsSendVo.getTARGET_USER_TYPE());
        }catch(IllegalArgumentException e) {
            throw new ValidationException("[TARGET_USER_TYPE]", locale);
        }
        switch(type) {
            case AP:	// 웹푸시유저 전체
                AllPushUserToRedisSender allPushUserToRedisSender = new AllPushUserToRedisSender(umsSendMsgBean, reqUmsSendVo.getLIMITSECOND(), reqUmsSendVo.getLIMITCNT());
                resultBodyMap = allPushUserToRedisSender.processInfo();

                if(isReserve) return resultBodyMap;

                allPushUserToRedisSender.start();
                break;
            case AU:	// UMS회원 전체
                AllUmsUserToRedisSender allUmsUserToRedisSender = new AllUmsUserToRedisSender(umsSendMsgBean, reqUmsSendVo.getLIMITSECOND(),reqUmsSendVo.getLIMITCNT());
                resultBodyMap = allUmsUserToRedisSender.processInfo();
                if(isReserve) return resultBodyMap;
                allUmsUserToRedisSender.start();
                break;
            case OU:	// 조직도회원 전체
                AllOrganUserToRedisSender allOrganUserToRedisSender = new AllOrganUserToRedisSender(umsSendMsgBean);
                resultBodyMap = allOrganUserToRedisSender.processInfo();
                if(isReserve) return resultBodyMap;
                allOrganUserToRedisSender.start();
                break;
            case MC:	// UMS회원 CSV - CSV파일 필수. #{아이디} 필수
//                resultBodyMap= umsCsvMemberSendService.umsWPushCsvMemberSend(umsSendMsgBean);
//                break;
            	throw new ValidationException("[NOT SUPPORTED TARGET SEND TYPE]", locale);
            case NC:	// UMS비회원 CSV - CSV파일 필수. #{아이디} #{핸드폰번호} #{이름} 필수
                resultBodyMap = umsCsvSendService.umsWPushCsvSend(umsSendMsgBean);
                break;
            case NM:	// UMS비회원 - CUIDS파라터 필수. {"아이디":["핸드폰번호","이름"},...}
                if(StringUtils.isBlank(reqUmsSendVo.getCUIDS())) throw new ValidationException("[CUIDS]", locale);

                try{
                    Map<String, List<String>> sendUsersMap = gson.fromJson(reqUmsSendVo.getCUIDS(), Map.class);
                    umsSendMsgBean.setTARGET_USERS(sendUsersMap); // 비회원 타겟팅 유저 셋팅.
                }catch (Exception jsonEx){
                    throw new ValidationException("[CUIDS]", locale);
                }
                resultBodyMap = umsSendService.umsWPushSend(umsSendMsgBean);
                break;
            default:
                if(StringUtils.isBlank(reqUmsSendVo.getCUIDS())) throw new ValidationException("[CUIDS]", locale);
                resultBodyMap = umsSendMemberService.umsMemberUserWPushSend(umsSendMsgBean,locale);
                break;
        }

        // 발송 성공 처리 응답. 프로세스 처리정보도 보냄.
        // 예약발송,발송완료 되었을 경우는 모두 완료처리로 함.
        Map<String,String> processMap = new HashMap<String, String>();
        processMap.put("processPercent", "100");
        processMap.put("processTxt", "Completed.");
        // 성공일 경우
        if(resultBodyMap.containsKey("PROGRESS_SEQNO")){
            SentInfoBean sentInfoBean = sentInfoManager.getSentInfoMap(reqUmsSendVo.getTRANS_TYPE(), resultBodyMap.get("PROGRESS_SEQNO").toString());
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
