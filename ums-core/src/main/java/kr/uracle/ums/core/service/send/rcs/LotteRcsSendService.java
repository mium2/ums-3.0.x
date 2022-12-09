package kr.uracle.ums.core.service.send.rcs;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.rcs.LotteRcsProcessBean;
import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LotteRcsSendService extends BaseRcsSendService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public void umsSend(Map<String, List<String>> users, UmsSendMsgBean umsSendMsgBean, Map<String, Map<String, String>> cuidVarMap, boolean isDaeCheSend) throws Exception {
        if(ObjectUtils.isEmpty(users)){
            logger.warn("Lotte RCS 서비스 - users map is empty");
            return;
        }

        int sendType = Integer.parseInt(Constants.getLotteSendType(umsSendMsgBean.getRCS_TYPE()));
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        for(String cuid : users.keySet()){
            //{"아이디":["핸드폰번호","이름"]}
            List<String> userInfos = users.get(cuid);
            LotteRcsProcessBean prcsBean = new LotteRcsProcessBean();
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(umsSendMsgBean.getRCS_TYPE());
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
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.setSEND_NAME(umsSendMsgBean.getSENDERID());

            prcsBean.setMSG_TYPE(sendType);
            prcsBean.setSEND_TYPE(umsSendMsgBean.getRCS_TYPE());
            prcsBean.setMESSAGEBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());

            String rcsBody = umsSendMsgBean.getRCS_OBJECT();
            if(StringUtils.isBlank(rcsBody)){
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("description", umsSendMsgBean.getRCS_MSG());
                if(sendType == 11 || sendType == 12) jsonObject.addProperty("title", StringUtils.isBlank(umsSendMsgBean.getRCS_TITLE())?"":umsSendMsgBean.getRCS_TITLE());
                rcsBody = jsonObject.toString();

            }
            prcsBean.setMSG_BODY(rcsBody);
            prcsBean.setBUTTONS(replaceMsg(umsSendMsgBean.getBTN_OBJECT(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            
            if(umsSendMsgBean.getMSG_TYPE().equals("A"))prcsBean.setMKT_FLAG("Y");
            prcsBean.setHEADER(umsSendMsgBean.getMSG_TYPE().equals("I")?"0":"1");
            prcsBean.setFOOTER(umsSendMsgBean.getFOOTER());
            prcsBean.setCOPY_ALLOWED(umsSendMsgBean.getCOPY_ALLOWED());

            prcsBean.setUSER1(umsSendMsgBean.getVAR1());
            prcsBean.setUSER2(umsSendMsgBean.getVAR2());

            rcsWorkerMgrPool.putWork(prcsBean);

        }
    }

    @Override
    public void umsReSend(PushEachProcessBean pushEachProcessBean, UmsSendMsgBean umsSendMsgBean) throws Exception {
        if(pushEachProcessBean==null) {
            logger.warn("Lotte RCS 서비스 - PushEachProcessBean is null");
            return;
        }
        int sendType = Integer.parseInt(Constants.getLotteSendType(umsSendMsgBean.getRCS_TYPE()));
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        LotteRcsProcessBean prcsBean = new LotteRcsProcessBean();
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setSTART_SEND_TYPE(umsSendMsgBean.getRCS_TYPE());
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

        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())) {
            Map<String,String> rcsVarMap = gson.fromJson(umsSendMsgBean.getREPLACE_VARS(), new TypeToken<Map<String, String>>(){}.getType());
            prcsBean.setMSG_VAR_MAP(rcsVarMap);
            prcsBean.setMSG_VARS(umsSendMsgBean.getREPLACE_VARS());
        }

        // 채널 셋팅
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setSEND_NAME(umsSendMsgBean.getSENDERID());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setDEST_NAME(pushEachProcessBean.getCUID());

        prcsBean.setMSG_TYPE(sendType);
        prcsBean.setSEND_TYPE(umsSendMsgBean.getRCS_TYPE());
        prcsBean.setMESSAGEBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());

        String rcsBody = umsSendMsgBean.getRCS_OBJECT();
        if(StringUtils.isBlank(rcsBody)){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("description", umsSendMsgBean.getRCS_MSG());
            if(sendType == 11 || sendType == 12) jsonObject.addProperty("title", StringUtils.isBlank(umsSendMsgBean.getRCS_TITLE())?"":umsSendMsgBean.getRCS_TITLE());
            rcsBody = jsonObject.getAsString();
        }
        prcsBean.setMSG_BODY(replaceMsg(rcsBody, pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setBUTTONS(replaceMsg(umsSendMsgBean.getBTN_OBJECT(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
        
        if(umsSendMsgBean.getMSG_TYPE().equals("A"))prcsBean.setMKT_FLAG("Y");
        prcsBean.setHEADER(umsSendMsgBean.getMSG_TYPE().equals("I")?"0":"1");
        prcsBean.setFOOTER(umsSendMsgBean.getFOOTER());
        prcsBean.setCOPY_ALLOWED(umsSendMsgBean.getCOPY_ALLOWED());

        prcsBean.setUSER1(umsSendMsgBean.getVAR1());
        prcsBean.setUSER2(umsSendMsgBean.getVAR2());

        rcsWorkerMgrPool.putWork(prcsBean);
    }
}
