package kr.uracle.ums.core.dao.ums;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.CustomResultBean;
import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.common.TargetUserKind;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.processor.bean.SentInfoBean;
import kr.uracle.ums.core.processor.bean.UmsResultBaseBean;
import kr.uracle.ums.core.processor.push.PushBasicProcessBean;
import kr.uracle.ums.core.processor.react.ReactProcessBean;
import kr.uracle.ums.core.processor.statistics.StatisticsMgr;
import kr.uracle.ums.core.service.bean.UmsReserveMsgBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 1..
 */
@Repository
public class UmsDao {
    Logger dbErrorlogger = LoggerFactory.getLogger("dbErrorLogger");
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;
    @Value("${UMS.SELF.URL:}")
    private String REG_UMS_HOST;

    @Value("${CUSTOM.RESULT.FILTER.KEY:}")
    private String CUSTOM_RESULT_FILTER_KEY;
    
    @Autowired(required = true)
    private Gson gson;

    @Autowired(required = true)
    private StatisticsMgr statisticsMgr;

    // 발송원장 입력
    public int inUmsSendMsg(UmsSendMsgBean umsSendMsgBean) throws Exception{
        if(StringUtils.isNotBlank(umsSendMsgBean.getORG_RESERVEDATE())){
            umsSendMsgBean.setRESERVEDATE(umsSendMsgBean.getORG_RESERVEDATE());
        }
        
        if(umsSendMsgBean.getTRANS_TYPE()==TransType.REAL.toString()) {
            int applyRow = sqlSessionTemplate.insert("mybatis.ums.send.inUmsSendMsgReal", umsSendMsgBean);
            // 발송카운트 초기화 정보 셋팅
            SentInfoBean sentInfoBean = new SentInfoBean(TransType.REAL);
            sentInfoBean.setUMS_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            sentInfoBean.setREQ_SEND_CNT(umsSendMsgBean.getTOTAL_CNT());
            sentInfoBean.setSUCC_CNT(umsSendMsgBean.getSEND_CNT());
            sentInfoBean.setFAIL_CNT(umsSendMsgBean.getFAIL_CNT());
            sqlSessionTemplate.insert("mybatis.ums.send.inUmsSendCountReal", sentInfoBean);
            return applyRow;
        }else{
            int applyRow = sqlSessionTemplate.insert("mybatis.ums.send.inUmsSendMsgBatch", umsSendMsgBean);
            // 발송카운트 초기화 정보 셋팅
            SentInfoBean sentInfoBean = new SentInfoBean(TransType.BATCH);
            sentInfoBean.setUMS_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            sentInfoBean.setREQ_SEND_CNT(umsSendMsgBean.getTOTAL_CNT());
            sentInfoBean.setSUCC_CNT(umsSendMsgBean.getSEND_CNT());
            sentInfoBean.setFAIL_CNT(umsSendMsgBean.getFAIL_CNT());
            sqlSessionTemplate.insert("mybatis.ums.send.inUmsSendCountBatch", sentInfoBean);
            if(Constants.TARGET_USER_TYPE.NC.toString().equals(umsSendMsgBean.getTARGET_USER_TYPE()) && StringUtils.isNotEmpty(umsSendMsgBean.getCUST_TRANSKEY())){
                // 광고성메세지이고 비회원CSV 대량발송이며 고객사에서 적용한 발송메세지 고유키가 있을 경우 반응률 정보를 위한 테이블 인설트
                ReactProcessBean reactProcessBean = new ReactProcessBean();
                reactProcessBean.setUMS_SEQNO(umsSendMsgBean.getUMS_SEQNO());
                reactProcessBean.setCUST_TRANSGROUPKEY(umsSendMsgBean.getCUST_TRANSGROUPKEY());
                reactProcessBean.setCUST_TRANSKEY(umsSendMsgBean.getCUST_TRANSKEY());
                sqlSessionTemplate.insert("mybatis.ums.send.inUmsReactCountBatch", reactProcessBean);
            }
            return applyRow;
        }
    }

    /**
     * 일반 예약발송
     * @param umsSendMsgBean
     * @return
     * @throws Exception
     */
    public int inUmsSendReserveMsg(UmsSendMsgBean umsSendMsgBean) throws Exception{
        return inUmsSendReserveMsg(umsSendMsgBean, null);
    }

    /**
     * CSV 예약발송
     * @param umsSendMsgBean
     * @param csvSaveInfMap
     * @return
     * @throws Exception
     * 
     * 예약테이블 저장 구조체 - 예약 발송테이블 저장 > AGENT API 콜 처리 > API서버 재처리
     */
    public int inUmsSendReserveMsg(UmsSendMsgBean umsSendMsgBean, Map<String,String> csvSaveInfMap) throws Exception{

        UmsReserveMsgBean umsReserveMsgBean = new UmsReserveMsgBean(umsSendMsgBean.getTRANS_TYPE());
        
        //공통 필드
        umsReserveMsgBean.setMSG_TYPE(umsSendMsgBean.getMSG_TYPE());
        umsReserveMsgBean.setRESERVEDATE(umsSendMsgBean.getRESERVEDATE());
        umsReserveMsgBean.setTARGET_USER_TYPE(umsSendMsgBean.getTARGET_USER_TYPE());
        umsReserveMsgBean.setSENDERID(umsSendMsgBean.getSENDERID());
        umsReserveMsgBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
        umsReserveMsgBean.setSTART_SEND_KIND(umsSendMsgBean.getSTART_SEND_KIND());
        umsReserveMsgBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        umsReserveMsgBean.setSEND_RESERVE_DATE(umsSendMsgBean.getRESERVEDATE().trim());
        umsReserveMsgBean.setREPLACE_VARS(umsSendMsgBean.getREPLACE_VARS());
        umsReserveMsgBean.setCUST_TRANSGROUPKEY(umsSendMsgBean.getCUST_TRANSGROUPKEY());
        umsReserveMsgBean.setCUST_TRANSKEY(umsSendMsgBean.getCUST_TRANSKEY());
        umsReserveMsgBean.setSEND_MACRO_CODE(umsSendMsgBean.getSEND_MACRO_CODE());
        umsReserveMsgBean.setSEND_MACRO_ORDER(umsSendMsgBean.getSEND_MACRO_ORDER());
        umsReserveMsgBean.setVAR1(umsSendMsgBean.getVAR1());
        umsReserveMsgBean.setVAR2(umsSendMsgBean.getVAR2());
        umsReserveMsgBean.setVAR3(umsSendMsgBean.getVAR3());
        umsReserveMsgBean.setVAR4(umsSendMsgBean.getVAR4());
        umsReserveMsgBean.setVAR5(umsSendMsgBean.getVAR5());
        umsReserveMsgBean.setVAR6(umsSendMsgBean.getVAR6());
        umsReserveMsgBean.setVAR7(umsSendMsgBean.getVAR7());
        umsReserveMsgBean.setVAR8(umsSendMsgBean.getVAR8());
        umsReserveMsgBean.setVAR9(umsSendMsgBean.getVAR9());
        umsReserveMsgBean.setADMIN_MSG_ID(umsSendMsgBean.getADMIN_MSG_ID());
        umsReserveMsgBean.setTOTAL_CNT(umsSendMsgBean.getTOTAL_CNT());
        
        if(REG_UMS_HOST.endsWith("/") == false) REG_UMS_HOST = REG_UMS_HOST+"/";
        umsReserveMsgBean.setREG_UMS_HOST(REG_UMS_HOST);
        
        //TODO TARGET_TYPE 정보를 사용하는 곳이 없음 - 불필요 코드로 보임
        umsReserveMsgBean.setTARGET_TYPE("L");
        
        // CSV 발송 여부에 따른 발송 요청 대상자 정보 저장
        if (ObjectUtils.isNotEmpty(csvSaveInfMap)) {
            umsReserveMsgBean.setTARGET_TYPE("C");
            umsReserveMsgBean.setCSV_FILE(csvSaveInfMap.get("saveCsvFileAbsSrc"));
            umsReserveMsgBean.setCSV_ORG_FILENAME(csvSaveInfMap.get("orgCsvFileName"));
        }else {
        	String targetUser = umsSendMsgBean.getCUIDS();
        	if(umsSendMsgBean.getTARGET_USER_TYPE().equals(TargetUserKind.NM.toString())) targetUser = gson.toJson(umsSendMsgBean.getTARGET_USERS());
        	umsReserveMsgBean.setTARGET_USERS_JSON(targetUser);
        }
        
        // PUSH
        umsReserveMsgBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
        umsReserveMsgBean.setPUSH_TYPE(umsSendMsgBean.getPUSH_TYPE());
        umsReserveMsgBean.setTITLE(umsSendMsgBean.getTITLE());
        umsReserveMsgBean.setATTACHFILE(umsSendMsgBean.getATTACHFILE());
        umsReserveMsgBean.setPUSH_MSG(umsSendMsgBean.getPUSH_MSG());
        umsReserveMsgBean.setSOUNDFILE(umsSendMsgBean.getSOUNDFILE());
        umsReserveMsgBean.setBADGENO(umsSendMsgBean.getBADGENO());
        umsReserveMsgBean.setPRIORITY(umsSendMsgBean.getPRIORITY());
        umsReserveMsgBean.setSENDERCODE(umsSendMsgBean.getSENDERCODE());
        umsReserveMsgBean.setSERVICECODE(umsSendMsgBean.getSERVICECODE());
        umsReserveMsgBean.setEXT(umsSendMsgBean.getEXT());
        umsReserveMsgBean.setDB_IN(umsSendMsgBean.getDB_IN());
        umsReserveMsgBean.setPUSH_FAIL_SMS_SEND(umsSendMsgBean.getPUSH_FAIL_SMS_SEND());
        umsReserveMsgBean.setSPLIT_MSG_CNT(umsSendMsgBean.getSPLIT_MSG_CNT());
        umsReserveMsgBean.setDELAY_SECOND(umsSendMsgBean.getDELAY_SECOND());
        umsReserveMsgBean.setPUSH_TEMPL_ID(umsSendMsgBean.getPUSH_TEMPL_ID());

        //웹푸시
        umsReserveMsgBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
        umsReserveMsgBean.setWPUSH_TEMPL_ID(umsSendMsgBean.getWPUSH_TEMPL_ID());
        umsReserveMsgBean.setWPUSH_TITLE(umsSendMsgBean.getWPUSH_TITLE());
        umsReserveMsgBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
        umsReserveMsgBean.setWPUSH_EXT(umsSendMsgBean.getWPUSH_EXT());
        umsReserveMsgBean.setWPUSH_BADGENO(umsSendMsgBean.getWPUSH_BADGENO());
        umsReserveMsgBean.setWPUSH_ICON(umsSendMsgBean.getWPUSH_ICON());
        umsReserveMsgBean.setWPUSH_LINK(umsSendMsgBean.getWPUSH_LINK());
        
        // ALIMTOK
        umsReserveMsgBean.setKKO_TITLE(umsSendMsgBean.getKKO_TITLE());
        umsReserveMsgBean.setALLIMTOLK_TEMPLCODE(umsSendMsgBean.getALLIMTOLK_TEMPLCODE());
        umsReserveMsgBean.setALLIMTALK_MSG(umsSendMsgBean.getALLIMTALK_MSG());
        umsReserveMsgBean.setKKOALT_SVCID(umsSendMsgBean.getKKOALT_SVCID());
        // R.S.W 패치 : 2019-12-13 카카오 친구톡 예약발송시 이미지 정보누락 패치
        umsReserveMsgBean.setKKO_IMG_PATH(umsSendMsgBean.getKKO_IMG_PATH());
        umsReserveMsgBean.setKKO_IMG_LINK_URL(umsSendMsgBean.getKKO_IMG_LINK_URL());
        umsReserveMsgBean.setKKO_BTNS(umsSendMsgBean.getKKO_BTNS());

        // FRNDTOK
        umsReserveMsgBean.setFRIENDTOLK_MSG(umsSendMsgBean.getFRIENDTOLK_MSG());
        umsReserveMsgBean.setFRT_TEMPL_ID(umsSendMsgBean.getFRT_TEMPL_ID());
        umsReserveMsgBean.setKKOFRT_SVCID(umsSendMsgBean.getKKOFRT_SVCID());
        umsReserveMsgBean.setPLUS_ID(umsSendMsgBean.getPLUS_ID());
        
        // SMS
        umsReserveMsgBean.setSMS_MSG(umsSendMsgBean.getSMS_MSG());
        umsReserveMsgBean.setSMS_TEMPL_ID(umsSendMsgBean.getSMS_TEMPL_ID());
        umsReserveMsgBean.setSMS_TITLE(umsSendMsgBean.getSMS_TITLE());
        umsReserveMsgBean.setMMS_IMGURL(umsSendMsgBean.getMMS_IMGURL());

        // RCS
        umsReserveMsgBean.setRCS_TITLE(umsSendMsgBean.getRCS_TITLE());
        umsReserveMsgBean.setRCS_MSG(umsSendMsgBean.getRCS_MSG());
        umsReserveMsgBean.setRCS_MMS_INFO(umsSendMsgBean.getRCS_MMS_INFO());
        umsReserveMsgBean.setRCS_TYPE(umsSendMsgBean.getRCS_TYPE());
        umsReserveMsgBean.setFOOTER(umsSendMsgBean.getFOOTER());
        if(StringUtils.isNotBlank(umsSendMsgBean.getCOPY_ALLOWED()))umsReserveMsgBean.setCOPY_ALLOWED(umsSendMsgBean.getCOPY_ALLOWED());
        umsReserveMsgBean.setEXPIRY_OPTION(umsSendMsgBean.getEXPIRY_OPTION());
        
        umsReserveMsgBean.setAddSEND_CNT(0);
        
        umsReserveMsgBean.setRCS_TEMPL_ID(umsSendMsgBean.getRCS_TEMPL_ID());
        umsReserveMsgBean.setBRAND_ID(umsSendMsgBean.getBRAND_ID());
        umsReserveMsgBean.setRCS_MSGBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());
        umsReserveMsgBean.setRCS_OBJECT(umsSendMsgBean.getRCS_OBJECT());
        umsReserveMsgBean.setBTN_OBJECT(umsSendMsgBean.getBTN_OBJECT());
        umsReserveMsgBean.setRCS_BTN_CNT(umsSendMsgBean.getRCS_BTN_CNT());
        umsReserveMsgBean.setRCS_BTN_TYPE(umsSendMsgBean.getRCS_BTN_TYPE());
        umsReserveMsgBean.setRCS_IMG_INSERT(umsSendMsgBean.isRCS_IMG_INSERT());
        
        umsReserveMsgBean.setIMG_GROUP_KEY(umsSendMsgBean.getIMG_GROUP_KEY());
        umsReserveMsgBean.setIMG_GROUP_CNT(umsSendMsgBean.getIMG_GROUP_CNT());
        umsReserveMsgBean.setRCS_IMG_PATH_JSON(umsSendMsgBean.getRCS_IMG_PATH_JSON());

        //네이버톡
        umsReserveMsgBean.setNAVER_TEMPL_ID(umsSendMsgBean.getNAVER_TEMPL_ID());
        umsReserveMsgBean.setNAVER_PARTNERKEY(umsSendMsgBean.getNAVER_PARTNERKEY());
        umsReserveMsgBean.setNAVER_PROFILE(umsSendMsgBean.getNAVER_PROFILE());
        umsReserveMsgBean.setNAVER_MSG(umsSendMsgBean.getNAVER_MSG());
        umsReserveMsgBean.setNAVER_BUTTONS(umsSendMsgBean.getNAVER_BUTTONS());
        umsReserveMsgBean.setNAVER_IMGHASH(umsSendMsgBean.getNAVER_IMGHASH());

        int applyRow = sqlSessionTemplate.insert("mybatis.ums.send.inReserveUms", umsReserveMsgBean);
        umsSendMsgBean.setRESERVE_SEQNO(umsReserveMsgBean.getRESERVE_SEQNO());
        
        return applyRow;
    }

    public UmsSendMsgBean selUmsPushSendBatch(long umsPushSeqno){
        return sqlSessionTemplate.selectOne("mybatis.ums.send.selUmsSendMsgOneBatch",umsPushSeqno);
    }
    public UmsSendMsgBean selUmsPushSendReal(long umsPushSeqno){
        return sqlSessionTemplate.selectOne("mybatis.ums.send.selUmsSendMsgOneReal",umsPushSeqno);
    }
    /**
     * UMS발송한 상세정보 저장.
     * @param umsResultBaseBean
     * @return
     * @throws Exception
     */
    public int inUmsSendDetail(UmsResultBaseBean umsResultBaseBean, BaseProcessBean prcsBean){
        int insertCnt = 0;
        try {
            // 이곳에 푸시성공/실패가 모두 들어온다. 이곳에서 성공/실패정보를 통계메니저에 등록
            statisticsMgr.putSentResultMsg(umsResultBaseBean);
            
            if (umsResultBaseBean.getTRAN_TYPE() == TransType.REAL) {
                insertCnt = sqlSessionTemplate.insert("mybatis.ums.send.inUmsSendDetailReal", umsResultBaseBean);
            } else {
                insertCnt = sqlSessionTemplate.insert("mybatis.ums.send.inUmsSendDetailBatch", umsResultBaseBean);
            }
        }catch (Exception e){
            // 상세결과 저장시 실패결과 로그저장.
        	e.printStackTrace();
            dbErrorlogger.error(gson.toJson(umsResultBaseBean));
            insertCnt = 0;
        }

        // 해당 조건에 맞는 경우만 정보 저장 하도록 구현. 추가 SI 배치 처리용 결과 데이터 저장 - 설정에 Y로 명시 된 경우만 처리 함
        if(!"".equals(CUSTOM_RESULT_FILTER_KEY)){
            try{
                if(CUSTOM_RESULT_FILTER_KEY.equals("SENDERID")){
                    if(!UmsInitListener.getCustomResultFilterSet().contains(umsResultBaseBean.getSENDERID())){
                        return insertCnt;
                    }
                }else if(CUSTOM_RESULT_FILTER_KEY.equals("SENDGROUPCODE")){
                    if(!UmsInitListener.getCustomResultFilterSet().contains(umsResultBaseBean.getSENDERGROUPCODE())){
                        return insertCnt;
                    }
                }
                prcsBean.setMSG_BODY(umsResultBaseBean.getSEND_MSG());
                prcsBean.setUMS_SEND_RESULT(umsResultBaseBean.getSEND_RESULT());
                prcsBean.setUMS_SUCC_STATUS(umsResultBaseBean.getSUCC_STATUS());
                // 커스텀 깊은 복사
                CustomResultBean customResultBean = new CustomResultBean();
                BeanUtils.copyProperties(prcsBean, customResultBean);

                if(prcsBean instanceof PushBasicProcessBean){
                    PushBasicProcessBean pushBasicProcessBean = (PushBasicProcessBean)prcsBean;
                    if(CUSTOM_RESULT_FILTER_KEY.equals("APP_ID")){
                        if(!UmsInitListener.getCustomResultFilterSet().contains(pushBasicProcessBean.getAPP_ID())){
                            return insertCnt;
                        }
                    }
                    customResultBean.setAPP_ID(pushBasicProcessBean.getAPP_ID());
                    customResultBean.setEXT(pushBasicProcessBean.getEXT());
                }
                sqlSessionTemplate.insert("mybatis.ums.send.inUmsCustomResult", customResultBean);

            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return insertCnt;
    }

    public int inUmsLog(UmsResultBaseBean umsResultBaseBean){
        try {
            return sqlSessionTemplate.insert("mybatis.ums.send.inUmsLog", umsResultBaseBean);
        }catch (Exception e){
            e.printStackTrace();
            dbErrorlogger.error(gson.toJson(umsResultBaseBean));
            return 0;
        }
    }

    /**
     * 반응률 업데이트
     * @param reactProcessBean
     * @return
     * @throws Exception
     */
    public int upUmsReactCountBatch(ReactProcessBean reactProcessBean) throws Exception{
        return sqlSessionTemplate.insert("mybatis.ums.send.upUmsReactCountBatch", reactProcessBean);
    }

}
