package kr.uracle.ums.core.service.send.rcs;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.rcs.AmRcsProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AmRcsSendService extends BaseRcsSendService {
    @Override
    public void umsSend(Map<String, List<String>> users, UmsSendMsgBean umsSendMsgBean, Map<String, Map<String, String>> cuidVarMap, boolean isDaeCheSend) throws Exception {
        if(ObjectUtils.isEmpty(users)){
            logger.warn("AM RCS 서비스 - users map is empty");
            return;
        }

        // 대체 발송 여부
        String ROOT_CHANNEL_YN = isDaeCheSend?"N":"Y";
        // 개인화 유무
        boolean isPersonalMsg = ObjectUtils.isNotEmpty(cuidVarMap);

        SendType sendType = SendType.valueOf(umsSendMsgBean.getRCS_TYPE());
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        AmRcsProcessBean prcsBean = null;
        for(Map.Entry<String, List<String>> element : users.entrySet()) {
            String cuid = element.getKey();
            List<String> userInfos = element.getValue();

            prcsBean = new AmRcsProcessBean();
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(sendType.toString());
            prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
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

            prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
            prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
            prcsBean.setCUID(cuid);
            if(isPersonalMsg) {
                Map<String, String> varMap = cuidVarMap.get(cuid);
                prcsBean.setMSG_VAR_MAP(varMap);
                prcsBean.setMSG_VARS(gson.toJson(varMap));
            }

            if(ObjectUtils.isNotEmpty(userInfos)){
                prcsBean.setMOBILE_NUM(userInfos.get(0).trim());
                if(userInfos.size()>1){
                    prcsBean.setCNAME(userInfos.get(1));
                }
            }
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.setUSER_ID(cuid);
            prcsBean.setMSG_BODY(umsSendMsgBean.getRCS_OBJECT());
            if(StringUtils.isBlank(umsSendMsgBean.getRCS_OBJECT())){
                prcsBean.setTITLE(umsSendMsgBean.getRCS_TITLE());
                prcsBean.setMSG_BODY(umsSendMsgBean.getRCS_MSG());
            }

            prcsBean.setRCS_TYPE(sendType.toString());
            prcsBean.setFOOTER(umsSendMsgBean.getFOOTER());
            prcsBean.setCOPY_ALLOW(umsSendMsgBean.getCOPY_ALLOWED());
            prcsBean.setRCS_MSGBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());
            prcsBean.setRCS_BRAND_ID(umsSendMsgBean.getBRAND_ID());
            prcsBean.setBTN_OBJECT(replaceMsg(prcsBean.getBTN_OBJECT(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            rcsWorkerMgrPool.putWork(prcsBean);
        }
    }
    @Override
    public void umsReSend(PushEachProcessBean pushEachProcessBean, UmsSendMsgBean umsSendMsgBean) throws Exception {
        if(pushEachProcessBean==null) {
            logger.warn("AM RCS 서비스 - PushEachProcessBean is null");
            return;
        }
        SendType sendType = SendType.valueOf(umsSendMsgBean.getRCS_TYPE());
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        AmRcsProcessBean prcsBean= new AmRcsProcessBean();
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setSTART_SEND_TYPE(sendType.toString());
        prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
        prcsBean.setROOT_CHANNEL_YN("N");
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

        prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
        prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
        prcsBean.setCUID(pushEachProcessBean.getCUID());
        prcsBean.setCNAME(pushEachProcessBean.getCNAME());

        prcsBean.setMSG_BODY(umsSendMsgBean.getRCS_OBJECT());
        if(StringUtils.isBlank(umsSendMsgBean.getRCS_OBJECT())){
            prcsBean.setTITLE(umsSendMsgBean.getRCS_TITLE());
            prcsBean.setMSG_BODY(umsSendMsgBean.getRCS_MSG());
        }
        prcsBean.setMSG_BODY(replaceMsg(prcsBean.getMSG_BODY(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));

        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setUSER_ID(pushEachProcessBean.getCUID());

        prcsBean.setRCS_TYPE(sendType.toString());
        prcsBean.setFOOTER(umsSendMsgBean.getFOOTER());
        prcsBean.setCOPY_ALLOW(umsSendMsgBean.getCOPY_ALLOWED());
        prcsBean.setRCS_MSGBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());
        prcsBean.setRCS_BRAND_ID(umsSendMsgBean.getBRAND_ID());
        prcsBean.setBTN_OBJECT(replaceMsg(prcsBean.getBTN_OBJECT(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
        
        rcsWorkerMgrPool.putWork(prcsBean);


    }
}
