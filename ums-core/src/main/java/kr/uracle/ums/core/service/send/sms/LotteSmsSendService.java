package kr.uracle.ums.core.service.send.sms;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.sms.LotteSmsProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LotteSmsSendService  extends BaseSmsSendService{
    @Override
    public void umsSmsSend(Map<String, List<String>> users, UmsSendMsgBean umsSendMsgBean, Map<String, Map<String, String>> cuidVarMap, boolean isDaeCheSend) {
        if(ObjectUtils.isEmpty(users)){
            logger.warn("Lotte SMS 서비스 - users map is empty");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        for(String cuid :  users.keySet()){
            //{"아이디":["핸드폰번호","이름"]}
            List<String> userInfos = users.get(cuid);
            LotteSmsProcessBean prcsBean = new LotteSmsProcessBean();
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(SendType.SMS.toString());
            prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
            prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
            prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
            prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());
            prcsBean.setROOT_CHANNEL_YN(isDaeCheSend?"N":"Y");

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

            prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
            prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
            prcsBean.setCUID(cuid);

            if(ObjectUtils.isNotEmpty(cuidVarMap) && ObjectUtils.isNotEmpty(cuidVarMap.get(cuid))){
                Map<String, String> varMap = cuidVarMap.get(cuid);
                prcsBean.setMSG_VAR_MAP(varMap);
                prcsBean.setMSG_VARS(gson.toJson(varMap));
            }
            if(ObjectUtils.isNotEmpty(userInfos)){
                prcsBean.setMOBILE_NUM(userInfos.get(0).trim());
                prcsBean.setDEST_NAME(cuid);
                if(userInfos.size()>1){
                    prcsBean.setCNAME(userInfos.get(1));
                }
            }

            // 채널 셋팅
            prcsBean.setTITLE(umsSendMsgBean.getSMS_TITLE());
            prcsBean.setMSG_BODY(umsSendMsgBean.getSMS_MSG());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.setUSER1(umsSendMsgBean.getVAR1());
            prcsBean.setUSER2(umsSendMsgBean.getVAR2());
            if(umsSendMsgBean.getMSG_TYPE().equals("A"))prcsBean.setMKT_FLAG("Y");
            smsWorkerMgrPool.putWork(prcsBean);
        }
    }
    @Override
    public void umsSmsSend(PushEachProcessBean pushEachProcessBean, UmsSendMsgBean umsSendMsgBean) throws Exception {
        if(pushEachProcessBean==null) {
            logger.warn("Lotte SMS 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        LotteSmsProcessBean prcsBean = new LotteSmsProcessBean();
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setSTART_SEND_TYPE(SendType.SMS.toString());
        prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
        prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
        prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
        prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());
        prcsBean.setROOT_CHANNEL_YN("N");

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

        prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
        prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
        prcsBean.setCUID(pushEachProcessBean.getCUID());
        prcsBean.setCNAME(pushEachProcessBean.getCNAME());

        // 채널 셋팅
        prcsBean.setTITLE(umsSendMsgBean.getSMS_TITLE());
        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getSMS_MSG(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setDEST_NAME(pushEachProcessBean.getCUID());
        prcsBean.setUSER1(umsSendMsgBean.getVAR1());
        prcsBean.setUSER2(umsSendMsgBean.getVAR2());
        if(umsSendMsgBean.getMSG_TYPE().equals("A"))prcsBean.setMKT_FLAG("Y");
        smsWorkerMgrPool.putWork(prcsBean);
    }
}
