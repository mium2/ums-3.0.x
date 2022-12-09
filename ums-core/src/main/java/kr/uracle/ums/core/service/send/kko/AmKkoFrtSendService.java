package kr.uracle.ums.core.service.send.kko;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.kko.AmKkoFrtProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AmKkoFrtSendService extends BaseKkoFrtSendService {
    @Override
    public void umsKkoFriendTolkSend(Map<String, List<String>> users, UmsSendMsgBean umsSendMsgBean, Map<String, Map<String, String>> cuidVarMap, boolean isDaeCheSend) throws Exception {
        if(ObjectUtils.isEmpty(users)){
            logger.warn("AM FRIENDTOK 서비스 - users map is empty");
            return;
        }
        // 대체 발송 여부
        String ROOT_CHANNEL_YN = isDaeCheSend?"N":"Y";
        // 개인화 유무
        boolean isPersonalMsg = ObjectUtils.isNotEmpty(cuidVarMap);

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        for(String cuid : users.keySet()){
            //{"아이디":["핸드폰번호","이름"]}
            List<String> userInfos = users.get(cuid);
            AmKkoFrtProcessBean prcsBean = new AmKkoFrtProcessBean();
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(SendType.KKOALT.toString());
            prcsBean.setROOT_CHANNEL_YN(ROOT_CHANNEL_YN);
            prcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
            prcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
            prcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());
            if(isPersonalMsg) {
                prcsBean.setMSG_VAR_MAP(cuidVarMap.get(cuid));
                prcsBean.setMSG_VARS(gson.toJson(prcsBean.getMSG_VAR_MAP()));
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

            prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOFRT_SVCID());
            prcsBean.setTITLE(umsSendMsgBean.getTITLE());
            prcsBean.setMSG_BODY(umsSendMsgBean.getFRIENDTOLK_MSG());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.setUSER_ID(cuid);
            
            if(ObjectUtils.isNotEmpty(userInfos)){
                prcsBean.setMOBILE_NUM(userInfos.get(0).trim());
                if(userInfos.size()>1){
                    prcsBean.setCNAME(userInfos.get(1));
                }
            }
            
            if(!StringUtils.isBlank(umsSendMsgBean.getKKO_BTNS())){
                prcsBean.setKKO_BTNS(replaceMsg(umsSendMsgBean.getKKO_BTNS(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            }

            prcsBean.setIMG_SRCS(replaceMsg(umsSendMsgBean.getKKO_IMG_PATH(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            prcsBean.setKKO_IMG_LINK_URL(replaceMsg(umsSendMsgBean.getKKO_IMG_LINK_URL(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            friendtalkWorkerMgrPool.putWork(prcsBean);
        }

    }
    @Override
    public void umsKkoFriendTolkSend(PushEachProcessBean pushEachProcessBean, UmsSendMsgBean umsSendMsgBean) throws Exception {
        if(pushEachProcessBean==null) {
            logger.warn("AM FRIENDTOK 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        AmKkoFrtProcessBean prcsBean = new AmKkoFrtProcessBean();
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setSTART_SEND_TYPE(SendType.KKOFRT.toString());
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

        prcsBean.setSERVICE_ID(umsSendMsgBean.getKKOFRT_SVCID());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setTITLE(umsSendMsgBean.getTITLE());
        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getFRIENDTOLK_MSG(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setUSER_ID(pushEachProcessBean.getCUID());

        if(!StringUtils.isBlank(umsSendMsgBean.getKKO_BTNS())){
            prcsBean.setKKO_BTNS(replaceMsg(umsSendMsgBean.getKKO_BTNS(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
        }
        prcsBean.setIMG_SRCS(replaceMsg(umsSendMsgBean.getKKO_IMG_PATH(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setKKO_IMG_LINK_URL(replaceMsg(umsSendMsgBean.getKKO_IMG_LINK_URL(), pushEachProcessBean.getCUID(), pushEachProcessBean.getCNAME(), pushEachProcessBean.getMSG_VAR_MAP()));
        friendtalkWorkerMgrPool.putWork(prcsBean);

    }
}
