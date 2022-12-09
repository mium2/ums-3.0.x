package kr.uracle.ums.core.service.send.mms;

import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.mms.KtMmsProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.util.DateUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class KtMmsSendService extends BaseMmsSendService {
    @Value(("${KTSMS.CLIENT_COM_ID:NONE}"))
    public String USER_ID;

    /**
     * UMS UI 발송: (T_UMS_SEND)원장 저장 테이블.
     * @param users
     * @param umsSendMsgBean
     */
    public void umsMmsSend(final Map<String,List<String>> users, final UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> cuidVarMap, boolean isDaeCheSend){
        if(ObjectUtils.isEmpty(users)){
            logger.warn("KT MMS 서비스 - users map is empty");
            return;
        }

        int imgCnt = 0;
        String imageInfo = null;
        SendType sendType = SendType.LMS;
        // 대체 발송 여부
        String ROOT_CHANNEL_YN = isDaeCheSend?"N":"Y";
        // 개인화 유무
        boolean isPersonalMsg = ObjectUtils.isNotEmpty(cuidVarMap);

        if(StringUtils.isNotBlank(umsSendMsgBean.getMMS_IMGURL())) {
            sendType = SendType.MMS;
            try{
                List<String> mmsImgPaths = gson.fromJson(umsSendMsgBean.getMMS_IMGURL(), new TypeToken<List<String>>(){}.getType());
                if(mmsImgPaths!=null){
                    StringBuilder sb = new StringBuilder();
                    imgCnt = mmsImgPaths.size();
                    for(int i=0; i<imgCnt; i++){
                        if(i==0) {
                            sb.append(mmsImgPaths.get(0) + "^1^0");
                        }else{
                            sb.append("|"+mmsImgPaths.get(0) + "^1^0");
                        }
                    }
                    imageInfo = sb.toString();
                }
            }catch(Exception e){
                imageInfo = umsSendMsgBean.getMMS_IMGURL()+"^1^0";
                imgCnt = 1;
            }
        }

        for(String cuid : users.keySet()){
            //{"아이디":["핸드폰번호","이름"]}
            List<String> userInfos = users.get(cuid);
            TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
            KtMmsProcessBean prcsBean = new KtMmsProcessBean();
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(sendType.toString());
            prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
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

            prcsBean.setUSER_ID(USER_ID);
            prcsBean.setNOW_DATE(DateUtil.getSysDateTime());
            prcsBean.setSEND_DATE(DateUtil.getSysDateTime());

            prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
            prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
            prcsBean.setCUID(cuid);

            prcsBean.setTITLE(umsSendMsgBean.getSMS_TITLE());
            prcsBean.setMSG_BODY(umsSendMsgBean.getSMS_MSG());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            if(isPersonalMsg) {
                prcsBean.setMSG_VAR_MAP(cuidVarMap.get(cuid));
                prcsBean.setMSG_VARS(gson.toJson(prcsBean.getMSG_VAR_MAP()));
            }

            if(ObjectUtils.isNotEmpty(userInfos)){
                String mobileNum = userInfos.get(0).trim();
                String cname = "고객";
                if(userInfos.size()>1){
                    cname  = userInfos.get(1);
                }
                prcsBean.setCNAME(cname);
                prcsBean.setMOBILE_NUM(mobileNum);
                prcsBean.setDEST_COUNT(1);
                prcsBean.setDEST_INFO(cname+"^"+mobileNum);
            }

            if(StringUtils.isNotBlank(imageInfo)){
                prcsBean.setCONTENT_DATA(replaceMsg(imageInfo, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
                prcsBean.setCONTENT_COUNT(imgCnt);
                prcsBean.setFILECNT(imgCnt);
            }
            mmsWorkerMgrPool.putWork(prcsBean);
        }

    }

    /**
     * 푸시발송에서 개별로 발송 실패 대체 알림톡 발송일감 처리 등록.
     * UMS UI 발송: (T_UMS_SEND)원장 저장 테이블.
     * @param pushEachProcessBean
     * @param umsSendMsgBean
     * @return
     */
    public void umsMmsSend(final PushEachProcessBean pushEachProcessBean, final UmsSendMsgBean umsSendMsgBean) throws Exception{
        if(pushEachProcessBean==null) {
            logger.warn("KT MMS 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        KtMmsProcessBean prcsBean = new KtMmsProcessBean();
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setSTART_SEND_TYPE(SendType.LMS.toString());
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

        prcsBean.setUSER_ID(USER_ID);
        prcsBean.setNOW_DATE(DateUtil.getSysDateTime());
        prcsBean.setSEND_DATE(DateUtil.getSysDateTime());

        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setTITLE(umsSendMsgBean.getSMS_TITLE());
        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getSMS_MSG(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));

        prcsBean.setDEST_COUNT(1);
        prcsBean.setDEST_INFO(pushEachProcessBean.getCNAME()+"^"+pushEachProcessBean.getMOBILE_NUM());
        try{
            List<String> mmsImgPaths = gson.fromJson(umsSendMsgBean.getMMS_IMGURL(), new TypeToken<List<String>>(){}.getType());
            if(mmsImgPaths!=null){
                StringBuilder sb = new StringBuilder();
                int imgCnt = 0;
                for(int i=0; i<mmsImgPaths.size(); i++){
                    if(i==0) {
                        sb.append(mmsImgPaths.get(0) + "^1^0");
                    }else{
                        sb.append("|"+mmsImgPaths.get(0) + "^1^0");
                    }
                    imgCnt++;
                }
                prcsBean.setSTART_SEND_TYPE(SendType.MMS.toString());
                prcsBean.setCONTENT_DATA(replaceMsg(sb.toString(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                prcsBean.setCONTENT_COUNT(imgCnt);
                prcsBean.setFILECNT(imgCnt);
            }
        }catch(Exception e){
            prcsBean.setSTART_SEND_TYPE(SendType.MMS.toString());
            prcsBean.setCONTENT_DATA(umsSendMsgBean.getMMS_IMGURL()+"^1^0");
            prcsBean.setCONTENT_COUNT(1);
            prcsBean.setFILECNT(1);
        }
        mmsWorkerMgrPool.putWork(prcsBean);

    }
}

