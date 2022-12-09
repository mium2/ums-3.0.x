package kr.uracle.ums.core.service.send.kko;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.kko.LgcnsKkoFrtProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LgcnsKkoFrtSendService extends BaseKkoFrtSendService {

    @Override
    public void umsKkoFriendTolkSend(final Map<String,List<String>> users, final UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> cuidVarMap, boolean isDaeCheSend) throws Exception{
        if(ObjectUtils.isEmpty(users)){
            logger.warn("LGCNS FRIENDTOK 서비스 - users map is empty");
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
            LgcnsKkoFrtProcessBean prcsBean = new LgcnsKkoFrtProcessBean();
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(SendType.KKOFRT.toString());
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO()); //UMS 푸시 발송원장 테이블에 SEQ번호를 반드시 넘겨야 한다.
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

            prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOFRT_SVCID());
            prcsBean.setTITLE(umsSendMsgBean.getTITLE());
            prcsBean.setMSG_BODY(umsSendMsgBean.getFRIENDTOLK_MSG());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.setREGISTER_BY(umsSendMsgBean.getSENDERID());

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
            if(btnLink2 != null)prcsBean.setKKO_BTN_LINK2(replaceMsg(btnLink2, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(btnLink3 != null)prcsBean.setKKO_BTN_LINK3(replaceMsg(btnLink3, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(btnLink4 != null)prcsBean.setKKO_BTN_LINK4(replaceMsg(btnLink4, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            if(btnLink5 != null)prcsBean.setKKO_BTN_LINK5(replaceMsg(btnLink5, prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            
            if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_IMG_PATH())){
                prcsBean.setIMG_ATTACH_FLAG("M");
                prcsBean.setKKO_IMG_PATH(replaceMsg(umsSendMsgBean.getKKO_IMG_PATH(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
                prcsBean.setHAS_IMAGE(true);
                
            }
            if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_IMG_LINK_URL())){
                prcsBean.setKKO_IMG_LINK_URL(replaceMsg(umsSendMsgBean.getKKO_IMG_LINK_URL(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            }
            
            friendtalkWorkerMgrPool.putWork(prcsBean);
        }

    }

    @Override
    public void umsKkoFriendTolkSend(final PushEachProcessBean pushEachProcessBean, final UmsSendMsgBean umsSendMsgBean) throws Exception{
        if(pushEachProcessBean==null) {
            logger.warn("LGCNS FRIENDTOK 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        LgcnsKkoFrtProcessBean prcsBean = new LgcnsKkoFrtProcessBean();
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setSTART_SEND_TYPE(SendType.KKOFRT.toString());
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

        prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOFRT_SVCID());
        prcsBean.setTITLE(umsSendMsgBean.getTITLE());
        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getFRIENDTOLK_MSG(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());

        if(!StringUtils.isBlank(umsSendMsgBean.getKKO_BTNS())){
            try{
                List<Map<String,Object>> kkoBtnList = gson.fromJson(umsSendMsgBean.getKKO_BTNS(), ArrayList.class);
                for(int i=0; i<kkoBtnList.size(); i++){
                    String kkoBtnJson = gson.toJson(kkoBtnList.get(i));
                    switch (i){
                        case 0:
                            prcsBean.setKKO_BTN_LINK1(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                        case 1:
                            prcsBean.setKKO_BTN_LINK2(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                        case 2:
                            prcsBean.setKKO_BTN_LINK3(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                        case 3:
                            prcsBean.setKKO_BTN_LINK4(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                        case 4:
                            prcsBean.setKKO_BTN_LINK5(replaceMsg(kkoBtnJson, pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
                            break;
                    }
                }

            }catch (Exception e){
                logger.error("KKO_BTNS : {}", umsSendMsgBean.getKKO_BTNS());
                logger.error("친구톡 챗버블버튼 에러로 인해 버튼적용 못함 : {}", e.toString());
            }
        }
        if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_IMG_PATH())){
            prcsBean.setIMG_ATTACH_FLAG("M");
            prcsBean.setKKO_IMG_PATH(replaceMsg(umsSendMsgBean.getKKO_IMG_PATH(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
            prcsBean.setHAS_IMAGE(true);
        }
        if(StringUtils.isNotBlank(umsSendMsgBean.getKKO_IMG_LINK_URL())){
            prcsBean.setKKO_IMG_LINK_URL(replaceMsg(umsSendMsgBean.getKKO_IMG_LINK_URL(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
        }
        prcsBean.setREGISTER_BY(umsSendMsgBean.getSENDERID());
        friendtalkWorkerMgrPool.putWork(prcsBean);
    }
}
