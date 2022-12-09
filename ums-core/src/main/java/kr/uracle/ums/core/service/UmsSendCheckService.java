package kr.uracle.ums.core.service;

import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.ehcache.PreventMobileCacheMgr;
import kr.uracle.ums.core.processor.CancleManager;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import kr.uracle.ums.core.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class UmsSendCheckService {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    @Autowired(required = true)
    private PreventMobileCacheMgr preventMobileCacheMgr;

    @Autowired(required = true)
    private CancleManager cancleManager;

    @Autowired(required = true)
    private UmsSendCommonService sendCommonService;

    @Value("${PHONENUM.ENC_YN:Y}")
    private String PHONE_ENC_YN;

    public boolean chkSend(BaseProcessBean baseBean, UmsResultBaseBean umsResultBaseBean, boolean isFatigue) {
        try {
            String RECEIVE_MOBILE_NO = baseBean.getMOBILE_NUM();
            boolean whiteFilter = !UmsInitListener.getWHITELIST_TARGET().isEmpty();
            if(whiteFilter && (UmsInitListener.checkWHITELIST_TARGET(RECEIVE_MOBILE_NO) == false)) {
                logger.debug("화이트 리스트 필터로 인한 무시 처리:"+RECEIVE_MOBILE_NO);
                umsResultBaseBean.setSEND_RESULT("FF");
                umsResultBaseBean.setSUCC_STATUS("0");
                umsResultBaseBean.setERRCODE(ErrorManager.ERR_5007);
                umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5007)); 
                umsResultBaseBean.setPROCESS_END("Y");
                return false;
            }

            // 추가 : 취소요청 확인 후 취소요청에 의한 실패처리
            // 발송 전 취소요청 확인 취소가 들어온 UMS_SEQNO일 경우 취소로 인한 실패처리로직 구현
            if (cancleManager.isCancleUmsSeqno("" + baseBean.getMASTERTABLE_SEQNO())) {
                umsResultBaseBean.setSEND_RESULT("FF");
                umsResultBaseBean.setSUCC_STATUS("0");
                umsResultBaseBean.setERRCODE(ErrorManager.ERR_5005);
                umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5005)); 
                umsResultBaseBean.setPROCESS_END("Y");//UMS-AGENT는 알림톡로그테이블을 감시하므로 알림톡발송테이블에 넣치 못한 경우는 프로세스 종료임.
                return false;
            }

            // 피로도 체크
            if (isFatigue) {
                boolean isSendAble = sendCommonService.chkFatigue(baseBean.getCUID());
                if (!isSendAble) {
                    // 피로도 체크에 의한 실패처리
                    umsResultBaseBean.setSEND_RESULT("FF");
                    umsResultBaseBean.setERRCODE(ErrorManager.ERR_5006);
                    umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5006)); 
                    umsResultBaseBean.setPROCESS_END("Y");// UMS-AGENT는 친구톡로그테이블을 감시하므로 알림톡발송테이블에 넣치 못한 경우는 프로세스 종료임.
                    return false;
                }
            }

            boolean isValidationCheck = true;
            String errMsg = "";
            if ("".equals(RECEIVE_MOBILE_NO)) {
                errMsg = "핸드폰번호가 존재하지 않습니다.";
                isValidationCheck = false;
            } else {
                if("N".equals(PHONE_ENC_YN)){
                    // 핸드폰번호 암호화 처리 되어 있지 않을때만 유효성 체크하도록 한다.
                    if (!StringUtil.checkHpNum(RECEIVE_MOBILE_NO)) {
                        errMsg = "핸드폰번호가 올바르지 않습니다.";
                        isValidationCheck = false;
                    }
                }
            }
            // STEP 2 : 핸드폰번호 유무 체크
            if (!isValidationCheck) {
                umsResultBaseBean.setSEND_RESULT("FF");
                umsResultBaseBean.setSUCC_STATUS("0");
                umsResultBaseBean.setERRCODE(ErrorManager.ERR_5001);
                umsResultBaseBean.setRESULTMSG( ErrorManager.getInstance().getMsg(ErrorManager.ERR_5001)+", "+errMsg + "(" + baseBean.getCUID() + ")");
                umsResultBaseBean.setPROCESS_END("Y");//UMS-AGENT는 알림톡로그테이블을 감시하므로 알림톡발송테이블에 넣치 못한 경우는 프로세스 종료임.
                return false;
            }

            // 2020-01-20 채널별 수신관리 사용자 체크
            if (preventMobileCacheMgr.isPreventUserFromMobile(RECEIVE_MOBILE_NO, umsResultBaseBean.getSEND_TYPE(), baseBean.getUMS_MSG_TYPE())) {
                umsResultBaseBean.setSEND_RESULT("FF");
                umsResultBaseBean.setSUCC_STATUS("0");
                umsResultBaseBean.setERRCODE(ErrorManager.ERR_5002);
                umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5001)+", [수신채널거부] 발송제한 핸드폰번호 : " + StringUtil.astarPhoneNum(RECEIVE_MOBILE_NO) + ", 아이디:" + baseBean.getCUID());
                umsResultBaseBean.setPROCESS_END("Y");//UMS-AGENT는 알림톡로그테이블을 감시하므로 알림톡발송테이블에 넣치 못한 경우는 프로세스 종료임.
                return false;
            }

            // 발송 유효 시간 체크
            if (sendTimeCheck(baseBean.getMIN_START_TIME(), baseBean.getMAX_END_TIME()) == false) {
                umsResultBaseBean.setSEND_RESULT("FF");
                umsResultBaseBean.setSUCC_STATUS("0");
                umsResultBaseBean.setERRCODE(ErrorManager.ERR_5008);
                umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5008)); 
                umsResultBaseBean.setPROCESS_END("Y");//UMS-AGENT는 알림톡로그테이블을 감시하므로 알림톡발송테이블에 넣치 못한 경우는 프로세스 종료임.
                return false;
            }


        }catch (Exception e){
            umsResultBaseBean.setSEND_RESULT("FF");
            umsResultBaseBean.setSUCC_STATUS("0");
            umsResultBaseBean.setERRCODE(ErrorManager.ERR_500);
            umsResultBaseBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_500)+", "+e.getMessage());
            umsResultBaseBean.setPROCESS_END("Y");
            logger.error(e.toString());
            return false;
        }
        return true;
    }

    private boolean sendTimeCheck(String start, String end) {

        if(StringUtils.isNotBlank(start)) start = start.replaceAll("\\D", "");
        if(StringUtils.isNotBlank(end)) end = end.replaceAll("\\D", "");
        int minTime = StringUtils.isBlank(start)?0:Integer.parseInt(start);
        int maxTime = StringUtils.isBlank(end)?0:Integer.parseInt(end);
        if(minTime <= 0 && maxTime<=0 ) return true;
        int now = Integer.parseInt(DATE_TIME_FORMAT.format(LocalTime.now()));
        if(minTime >0 && now < minTime) return false;
        if(maxTime >0 && now > maxTime) return false;

        return true;
    }
}
