package kr.uracle.ums.core.service.send.kko;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.kko.LotteKkoAltProcessBean;
import kr.uracle.ums.core.ehcache.AltTemplateCacheMgr;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.vo.template.AltTemplateBaseVo;
import kr.uracle.ums.core.vo.template.AltTemplateLotteVo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LotteKkoAltSendService extends BaseKkoAltSendService{

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private AltTemplateCacheMgr altTemplateCacheMgr;

    private final Gson nullGson = new GsonBuilder().serializeNulls().create();
    
    @Override
    public void umsKkoAllimTolkSend(Map<String, List<String>> users, UmsSendMsgBean umsSendMsgBean, Map<String, Map<String, String>> cuidVarMap, boolean isDaeCheSend) throws Exception {
        if(ObjectUtils.isEmpty(users)){
            logger.warn("Lotte 알림톡 서비스 - users map is empty");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        for(String cuid : users.keySet()){
            //{"아이디":["핸드폰번호","이름"]}
            List<String> userInfos = users.get(cuid);
            LotteKkoAltProcessBean prcsBean = new LotteKkoAltProcessBean();
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setSTART_SEND_TYPE(SendType.KKOALT.toString());
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
            prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
            prcsBean.setROOT_CHANNEL_YN(isDaeCheSend?"N":"Y");
            prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());

            //알림톡 강조형 셋팅
            if(!"".equals(umsSendMsgBean.getKKO_TITLE())){
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

            prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOALT_SVCID());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.setSEND_NAME(umsSendMsgBean.getSENDERID());
            prcsBean.setMSG_BODY(umsSendMsgBean.getALLIMTALK_MSG());
            prcsBean.setTEMPLATE_CODE(umsSendMsgBean.getALLIMTOLK_TEMPLCODE());
            
            Map<String, Object> kkoJsonMap = new HashMap<String, Object>(2);
            if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_BTNS())) {
                String replaceData = replaceMsg(umsSendMsgBean.getKKO_BTNS(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP());
                List<?> btnsArrays = gson.fromJson(replaceData, new TypeToken<List<?>>(){}.getType());
                kkoJsonMap.put("button", btnsArrays);
            }
            if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_IMG_PATH())){
                Map<String, Object> kkoImageMap = new HashMap<String, Object>(2);
                kkoImageMap.put("img_url", null);
                kkoJsonMap.put("image", kkoImageMap);
            }
            if(kkoJsonMap.size()>0) {
                prcsBean.setKKO_JSON(nullGson.toJson(kkoJsonMap));
            }

            prcsBean.setUSER1(umsSendMsgBean.getVAR1());
            prcsBean.setUSER2(umsSendMsgBean.getVAR2());

            allimtalkWorkerMgrPool.putWork(prcsBean);
        }
    }
    @Override
    public void umsKkoAllimTolkSend(PushEachProcessBean pushEachProcessBean, UmsSendMsgBean umsSendMsgBean) throws Exception {
        if(pushEachProcessBean==null) {
            logger.warn("Lotte 알림톡 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        LotteKkoAltProcessBean prcsBean = new LotteKkoAltProcessBean();

        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setSTART_SEND_TYPE(SendType.KKOALT.toString());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
        prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
        prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());
        prcsBean.setROOT_CHANNEL_YN("N");

        //알림톡 강조형 셋팅
        if(!"".equals(umsSendMsgBean.getKKO_TITLE())){
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

        prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
        prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
        prcsBean.setCUID(pushEachProcessBean.getCUID());
        prcsBean.setCNAME(pushEachProcessBean.getCNAME());
        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())){
            prcsBean.setMSG_VAR_MAP(gson.fromJson(umsSendMsgBean.getREPLACE_VARS(), new TypeToken<Map<String, String>>(){}.getType()));
            prcsBean.setMSG_VARS(umsSendMsgBean.getREPLACE_VARS());
        }
        
        Map<String, Object> kkoJsonMap = new HashMap<String, Object>(2);
        if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_BTNS())) {
            String replaceData = replaceMsg(umsSendMsgBean.getKKO_BTNS(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP());
            List<?> btnsArrays = gson.fromJson(replaceData, new TypeToken<List<?>>(){}.getType());
            kkoJsonMap.put("button", btnsArrays);
        }
        
        if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_IMG_PATH())){
            Map<String, Object> kkoImageMap = new HashMap<String, Object>(2);
            kkoImageMap.put("img_url", null);
            kkoJsonMap.put("image", kkoImageMap);
        }
        
        if(kkoJsonMap.size()>0) {
            prcsBean.setKKO_JSON(gson.toJson(kkoJsonMap));
        }
        try {
            AltTemplateLotteVo altTemplateLotteVo = (AltTemplateLotteVo) altTemplateCacheMgr.getTemplate(umsSendMsgBean.getALLIMTOLK_TEMPLCODE());
            if(altTemplateLotteVo != null && StringUtils.isNotBlank(altTemplateLotteVo.getTITLE())){
                // 등록된 템플릿에서 정보 가져와 셋팅
                prcsBean.setTITLE(altTemplateLotteVo.getTITLE());
            }
        }catch (Exception e){e.printStackTrace();}
        
        prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOALT_SVCID());
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setSEND_NAME(umsSendMsgBean.getSENDERID());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setDEST_NAME(pushEachProcessBean.getCUID());

        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getALLIMTALK_MSG(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setTEMPLATE_CODE(umsSendMsgBean.getALLIMTOLK_TEMPLCODE());
        
        prcsBean.setUSER1(umsSendMsgBean.getVAR1());
        prcsBean.setUSER2(umsSendMsgBean.getVAR2());

        allimtalkWorkerMgrPool.putWork(prcsBean);
    }
}
