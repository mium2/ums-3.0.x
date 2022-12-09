package kr.uracle.ums.core.service.send.kko;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.kko.LotteKkoFrtProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LotteKkoFrtSendService extends BaseKkoFrtSendService {
    private final Gson nullGson = new GsonBuilder().serializeNulls().create();
    
    @Override
    public void umsKkoFriendTolkSend(Map<String, List<String>> users, UmsSendMsgBean umsSendMsgBean, Map<String, Map<String, String>> cuidVarMap, boolean isDaeCheSend) throws Exception {
        if(ObjectUtils.isEmpty(users)){
            logger.warn("Lotte 친구톡 서비스 - users map is empty");
            return;
        }
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        for(String cuid : users.keySet()){
            //{"아이디":["핸드폰번호","이름"]}
            List<String> userInfos = users.get(cuid);
            LotteKkoFrtProcessBean prcsBean = new LotteKkoFrtProcessBean();

            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(SendType.KKOFRT.toString());
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setROOT_CHANNEL_YN(isDaeCheSend?"N":"Y");
            prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
            prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
            prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());
            prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
            prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());

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
            prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOFRT_SVCID());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.setSEND_NAME(umsSendMsgBean.getSENDERID());
            prcsBean.setTITLE(umsSendMsgBean.getTITLE());
            prcsBean.setMSG_BODY(umsSendMsgBean.getFRIENDTOLK_MSG());
            prcsBean.setMKT_FLAG("Y");
            prcsBean.setUSER1(umsSendMsgBean.getVAR1());
            prcsBean.setUSER2(umsSendMsgBean.getVAR2());

            Map<String, Object> kkoJsonMap = new HashMap<String, Object>(2);
            if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_BTNS())) {
                String replaceData = replaceMsg(umsSendMsgBean.getKKO_BTNS(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP());
                List<?> btnsArrays = gson.fromJson(replaceData, new TypeToken<List<?>>(){}.getType());
                kkoJsonMap.put("button", btnsArrays);
            }
            
            //이미지 파일 업로드에 맵핑된 링크 URL 정보 연동 방식 체크
            if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_IMG_PATH())){
                prcsBean.setATTACHED_FILE(replaceMsg(umsSendMsgBean.getKKO_IMG_PATH(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
                
                Map<String, Object> kkoImageMap = new HashMap<String, Object>(2);
                String replaceData = replaceMsg(umsSendMsgBean.getKKO_IMG_LINK_URL()==null?"":umsSendMsgBean.getKKO_IMG_LINK_URL(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP());
                kkoImageMap.put("img_link", StringUtils.isBlank(replaceData)?null:replaceData);
                kkoJsonMap.put("image", kkoImageMap);
                prcsBean.setHAS_IMAGE(true);
            }
            
            // 파일명, url  KKO 등록 안된  >> ATTACHEDFILE
            // 카카오에 등록된 URL  >> KKO JSON
            if(kkoJsonMap.size()>0) {
                prcsBean.setKKO_JSON(nullGson.toJson(kkoJsonMap));
            }
            
            friendtalkWorkerMgrPool.putWork(prcsBean);
        }
    }
    @Override
    public void umsKkoFriendTolkSend(PushEachProcessBean pushEachProcessBean, UmsSendMsgBean umsSendMsgBean) throws Exception {
        if(pushEachProcessBean==null) {
            logger.warn("Lotte 친구톡 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        LotteKkoFrtProcessBean prcsBean = new LotteKkoFrtProcessBean();
        // 공통 셋팅
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setSTART_SEND_TYPE(SendType.KKOALT.toString());
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
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

        // 채널 셋팅
        prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOFRT_SVCID());
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setSEND_NAME(umsSendMsgBean.getSENDERID());
        prcsBean.setTITLE(umsSendMsgBean.getTITLE());
        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getFRIENDTOLK_MSG(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setDEST_NAME(pushEachProcessBean.getCUID());
        prcsBean.setMKT_FLAG("Y");
        prcsBean.setUSER1(umsSendMsgBean.getVAR1());
        prcsBean.setUSER2(umsSendMsgBean.getVAR2());

        Map<String, Object> kkoJsonMap = new HashMap<String, Object>(2);
        if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_BTNS())) {
            String replaceData = replaceMsg(umsSendMsgBean.getKKO_BTNS(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP());
            List<?> btnsArrays = gson.fromJson(replaceData, new TypeToken<List<?>>(){}.getType());
            kkoJsonMap.put("button", btnsArrays);
        }
        
        //이미지 파일 업로드에 맵핑된 링크 URL 정보 연동 방식 체크
        if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_IMG_PATH())){
            prcsBean.setATTACHED_FILE(replaceMsg(umsSendMsgBean.getKKO_IMG_PATH(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));

            Map<String, Object> kkoImageMap = new HashMap<String, Object>(2);
            String replaceData = replaceMsg(umsSendMsgBean.getKKO_IMG_LINK_URL()==null?"":umsSendMsgBean.getKKO_IMG_LINK_URL(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP());
            kkoImageMap.put("img_link", StringUtils.isBlank(replaceData)?null:replaceData);
            kkoJsonMap.put("image", kkoImageMap);
            prcsBean.setHAS_IMAGE(true);
        }
        
        if(kkoJsonMap.size()>0) {
            prcsBean.setKKO_JSON(nullGson.toJson(kkoJsonMap));
        }

        friendtalkWorkerMgrPool.putWork(prcsBean);
    }
}
