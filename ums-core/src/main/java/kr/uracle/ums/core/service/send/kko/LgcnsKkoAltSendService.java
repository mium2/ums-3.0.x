package kr.uracle.ums.core.service.send.kko;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.kko.LgcnsKkoAltProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class LgcnsKkoAltSendService extends BaseKkoAltSendService{

    @Override
    public void umsKkoAllimTolkSend(final Map<String,List<String>> users, final UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> cuidVarMap, boolean isDaeCheSend) throws Exception{
        if(ObjectUtils.isEmpty(users)){
            logger.warn("LGCNS ALIMTOK 서비스 - users map is empty");
            return;
        }
        // 대체 발송 여부
        String ROOT_CHANNEL_YN = isDaeCheSend?"N":"Y";
        // 개인화 유무
        boolean isPersonalMsg = ObjectUtils.isNotEmpty(cuidVarMap);

        String btnLink1 = null;
        String btnLink2 = null;
        String btnLink3 = null;
        String btnLink4 = null;
        String btnLink5 = null;
        if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_BTNS())){
            try{
                List<Map<String,Object>> kkoBtnList = gson.fromJson(umsSendMsgBean.getKKO_BTNS(), ArrayList.class);
                for(int i=0; i<kkoBtnList.size(); i++){
                    String kkoBtnJson = gson.toJson(kkoBtnList.get(i));
                    switch (i){
                        case 0:
                            btnLink1 = kkoBtnJson;
                            break;
                        case 1:
                            btnLink2 = kkoBtnJson;
                            break;
                        case 2:
                            btnLink3 = kkoBtnJson;
                            break;
                        case 3:
                            btnLink4 = kkoBtnJson;
                            break;
                        case 4:
                            btnLink5 = kkoBtnJson;
                            break;
                    }
                }
            }catch (Exception e){
                logger.error("KKO_BTNS : {}", umsSendMsgBean.getKKO_BTNS());
                logger.error("알림톡 챗버블버튼 에러로 인해 버튼적용 못함 : {}", e.toString());
            }
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        for(String cuid : users.keySet()){
            List<String> userInfos = users.get(cuid); //{"아이디":["핸드폰번호","이름"]}
            // 알림톡 발송 처리 와 UMS 결과처리하는 프로세서에 등록.
            LgcnsKkoAltProcessBean prcsBean = new LgcnsKkoAltProcessBean();
            prcsBean.setSTART_SEND_TYPE(SendType.KKOALT.toString());
            prcsBean.setTRANS_TYPE(transType);

            //알림톡 강조형 셋팅
            if(!"".equals(umsSendMsgBean.getKKO_TITLE())){
                prcsBean.setCONTENTS_TYPE("007");
                prcsBean.setTITLE(umsSendMsgBean.getKKO_TITLE());
            }

            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setROOT_CHANNEL_YN(ROOT_CHANNEL_YN);
            prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
            prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
            prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());

            prcsBean.setCUST_TRANSKEY(umsSendMsgBean.getCUST_TRANSKEY());
            prcsBean.setCUST_TRANSGROUPKEY(umsSendMsgBean.getCUST_TRANSGROUPKEY());
            prcsBean.setVAR1(umsSendMsgBean.getVAR1());
            prcsBean.setVAR2(umsSendMsgBean.getVAR2());
            prcsBean.setVAR3(umsSendMsgBean.getVAR3());
            prcsBean.setVAR4(umsSendMsgBean.getVAR4());
            prcsBean.setVAR5(umsSendMsgBean.getVAR5());
            prcsBean.setVAR6(umsSendMsgBean.getVAR6());
            prcsBean.setVAR7(umsSendMsgBean.getVAR7());
            prcsBean.setVAR8(umsSendMsgBean.getVAR8());
            prcsBean.setVAR9(umsSendMsgBean.getVAR9());

            //공통 발송자 정보 셋팅. 통계 및 발송수 제한에 사용될 수 있음.
            prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
            prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
            prcsBean.setCUID(cuid);
            prcsBean.setTEMPLATE_CODE(umsSendMsgBean.getALLIMTOLK_TEMPLCODE());

            prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOALT_SVCID());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.setMSG_BODY(umsSendMsgBean.getALLIMTALK_MSG());
            
            if(isPersonalMsg) {
                prcsBean.setMSG_VAR_MAP(cuidVarMap.get(cuid));
                prcsBean.setMSG_VARS(gson.toJson(prcsBean.getMSG_VAR_MAP()));
            }
            if(ObjectUtils.isNotEmpty(userInfos)){
                prcsBean.setMOBILE_NUM(userInfos.get(0).trim());
                if(userInfos.size()>1){
                    prcsBean.setCNAME(userInfos.get(1));
                }
            }

            if(btnLink1 != null)prcsBean.setKKO_BTN_LINK1(replaceMsg(btnLink1, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(btnLink2 != null)prcsBean.setKKO_BTN_LINK1(replaceMsg(btnLink2, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(btnLink3 != null)prcsBean.setKKO_BTN_LINK1(replaceMsg(btnLink3, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(btnLink4 != null)prcsBean.setKKO_BTN_LINK1(replaceMsg(btnLink4, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(btnLink5 != null)prcsBean.setKKO_BTN_LINK1(replaceMsg(btnLink5, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            prcsBean.setREGISTER_BY(umsSendMsgBean.getSENDERID());
            allimtalkWorkerMgrPool.putWork(prcsBean);

        }

    }

    @Override
    public void umsKkoAllimTolkSend(final PushEachProcessBean pushEachProcessBean, final UmsSendMsgBean umsSendMsgBean) throws Exception{
        if(pushEachProcessBean==null) {
            logger.warn("LGCNS ALIMTOK 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        LgcnsKkoAltProcessBean prcsBean = new LgcnsKkoAltProcessBean();
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setSTART_SEND_TYPE(SendType.KKOALT.toString());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
        prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
        prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());

        //알림톡 강조형 셋팅
        if(!"".equals(umsSendMsgBean.getKKO_TITLE())){
            prcsBean.setCONTENTS_TYPE("007");
            prcsBean.setTITLE(umsSendMsgBean.getKKO_TITLE());
        }

        prcsBean.setCUST_TRANSKEY(umsSendMsgBean.getCUST_TRANSKEY());
        prcsBean.setCUST_TRANSGROUPKEY(umsSendMsgBean.getCUST_TRANSGROUPKEY());
        prcsBean.setVAR1(umsSendMsgBean.getVAR1());
        prcsBean.setVAR2(umsSendMsgBean.getVAR2());
        prcsBean.setVAR3(umsSendMsgBean.getVAR3());
        prcsBean.setVAR4(umsSendMsgBean.getVAR4());
        prcsBean.setVAR5(umsSendMsgBean.getVAR5());
        prcsBean.setVAR6(umsSendMsgBean.getVAR6());
        prcsBean.setVAR7(umsSendMsgBean.getVAR7());
        prcsBean.setVAR8(umsSendMsgBean.getVAR8());
        prcsBean.setVAR9(umsSendMsgBean.getVAR9());

        prcsBean.setROOT_CHANNEL_YN("N");
        prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
        prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
        prcsBean.setCUID(pushEachProcessBean.getCUID());
        prcsBean.setCNAME(pushEachProcessBean.getCNAME());

        prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOALT_SVCID());
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getALLIMTALK_MSG(), pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));

        prcsBean.setTEMPLATE_CODE(umsSendMsgBean.getALLIMTOLK_TEMPLCODE());
        prcsBean.setREGISTER_BY(umsSendMsgBean.getSENDERID());

        if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_BTNS())){
            try{
                List<Map<String,Object>> kkoBtnList = gson.fromJson(umsSendMsgBean.getKKO_BTNS(), ArrayList.class);
                for(int i=0; i<kkoBtnList.size(); i++){
                    String kkoBtnJson = gson.toJson(kkoBtnList.get(i));
                    switch (i){
                        case 0:
                            prcsBean.setKKO_BTN_LINK1(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                        case 1:
                            prcsBean.setKKO_BTN_LINK2(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                        case 2:
                            prcsBean.setKKO_BTN_LINK3(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                        case 3:
                            prcsBean.setKKO_BTN_LINK4(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                        case 4:
                            prcsBean.setKKO_BTN_LINK5(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                    }
                }

            }catch (Exception e){
                logger.error("KKO_BTNS : {}", umsSendMsgBean.getKKO_BTNS());
                logger.error("알림톡 챗버블버튼 에러로 인해 버튼적용 못함 : {}", e.toString());
            }
        }

        allimtalkWorkerMgrPool.putWork(prcsBean);

    }
}
