package kr.uracle.ums.core.service.send.mms;

import com.google.gson.reflect.TypeToken;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.mms.LguMmsProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LguMmsSendService extends BaseMmsSendService {
    /**
     * UMS UI 발송: (T_UMS_SEND)원장 저장 테이블.
     * @param users
     * @param umsSendMsgBean
     */
    public void umsMmsSend(final Map<String,List<String>> users, final UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> cuidVarMap, boolean isDaeCheSend){
        if(ObjectUtils.isEmpty(users)){
            logger.warn("LGU+ MMS 서비스 - users map is empty");
            return;
        }

        SendType sendType = SendType.LMS;
        String filePath1 = null;
        String filePath2 = null;
        String filePath3 = null;
        String filePath4 = null;
        String filePath5 = null;
        int imgCnt = 0;
        if(StringUtils.isNotBlank(umsSendMsgBean.getMMS_IMGURL())) {
            sendType = SendType.MMS;
            try{
                List<String> mmsImgPaths = gson.fromJson(umsSendMsgBean.getMMS_IMGURL(), new TypeToken<List<String>>(){}.getType());
                if(mmsImgPaths!=null){
                    imgCnt = mmsImgPaths.size();
                    for(int i=0; i<imgCnt; i++){
                        switch (i){
                            case 0:
                                filePath1 = mmsImgPaths.get(i);
                                break;
                            case 1:
                                filePath2 = mmsImgPaths.get(i);
                                break;
                            case 2:
                                filePath3 = mmsImgPaths.get(i);
                                break;
                            case 3:
                                filePath4 = mmsImgPaths.get(i);
                                break;
                            case 4:
                                filePath5 = mmsImgPaths.get(i);
                                break;
                        }

                    }
                }
            }catch(Exception e){
                imgCnt = 1;
                filePath1 = umsSendMsgBean.getMMS_IMGURL();
            }
        }

        // 대체 발송 여부
        String ROOT_CHANNEL_YN = isDaeCheSend?"N":"Y";
        // 개인화 유무
        boolean isPersonalMsg = ObjectUtils.isNotEmpty(cuidVarMap);

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        for(String cuid : users.keySet()){
            //{"아이디":["핸드폰번호","이름"]}
            List<String> userInfos = users.get(cuid);
            LguMmsProcessBean prcsBean = new LguMmsProcessBean();
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
            prcsBean.setSTART_SEND_TYPE(sendType.toString());;
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

            prcsBean.setTITLE(umsSendMsgBean.getSMS_TITLE());
            prcsBean.setMSG_BODY(umsSendMsgBean.getSMS_MSG());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());

            prcsBean.setID(umsSendMsgBean.getSENDERID());
            prcsBean.setPOST(umsSendMsgBean.getSENDGROUPCODE());
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
            
            if(filePath1 != null) prcsBean.setFILE_PATH1(replaceMsg(filePath1, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(filePath2 != null) prcsBean.setFILE_PATH2(replaceMsg(filePath2, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(filePath3 != null) prcsBean.setFILE_PATH3(replaceMsg(filePath3, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(filePath4 != null) prcsBean.setFILE_PATH4(replaceMsg(filePath4, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(filePath5 != null) prcsBean.setFILE_PATH5(replaceMsg(filePath5, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            prcsBean.setFILE_CNT(imgCnt);

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
            logger.warn("LGU+ MMS 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        LguMmsProcessBean prcsBean = new LguMmsProcessBean();
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
        prcsBean.setSTART_SEND_TYPE(SendType.LMS.toString());;
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

        prcsBean.setTITLE(umsSendMsgBean.getSMS_TITLE());
        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getSMS_MSG(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());

        prcsBean.setID(umsSendMsgBean.getSENDERID());
        prcsBean.setPOST(umsSendMsgBean.getSENDGROUPCODE());

        if(StringUtils.isNotBlank(umsSendMsgBean.getMMS_IMGURL())) {
            try{
                List<String> mmsImgPaths = gson.fromJson(umsSendMsgBean.getMMS_IMGURL(), new TypeToken<List<String>>(){}.getType());
                if(mmsImgPaths!=null){
                    StringBuilder sb = new StringBuilder();
                    int imgCnt = 0;
                    for(int i=0; i<mmsImgPaths.size(); i++){
                        switch (i){
                            case 0:
                                if(mmsImgPaths.get(i) != null) prcsBean.setFILE_PATH1(replaceMsg(mmsImgPaths.get(i), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                                break;
                            case 1:
                                if(mmsImgPaths.get(i) != null) prcsBean.setFILE_PATH2(replaceMsg(mmsImgPaths.get(i), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                                break;
                            case 2:
                                if(mmsImgPaths.get(i) != null) prcsBean.setFILE_PATH3(replaceMsg(mmsImgPaths.get(i), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                                break;
                            case 3:
                                if(mmsImgPaths.get(i) != null) prcsBean.setFILE_PATH4(replaceMsg(mmsImgPaths.get(i), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                                break;
                            case 4:
                                if(mmsImgPaths.get(i) != null) prcsBean.setFILE_PATH5(replaceMsg(mmsImgPaths.get(i), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                                break;
                        }
                        imgCnt++;
                    }
                    prcsBean.setSTART_SEND_TYPE(SendType.MMS.toString());;
                    prcsBean.setFILE_CNT(imgCnt);
                }
            }catch(Exception e){
                prcsBean.setSTART_SEND_TYPE(SendType.MMS.toString());;
                prcsBean.setFILE_CNT(1);
                prcsBean.setFILE_PATH1(umsSendMsgBean.getMMS_IMGURL());
            }
        }

        mmsWorkerMgrPool.putWork(prcsBean);

    }
}