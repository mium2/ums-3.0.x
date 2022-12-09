package kr.uracle.ums.core.service.send.naver;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.naver.MtsNaverProcessBean;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MtsNaverSendService extends BaseNaverSendService{

    @Override
    public void umsNaverSend(Map<String, List<String>> users, UmsSendMsgBean umsSendMsgBean, Map<String, Map<String, String>> cuidVarMap, boolean isDaeCheSend) throws Exception {
        if(ObjectUtils.isEmpty(users)){
            logger.warn("MTS NAVERT 서비스 - users map is empty");
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
            MtsNaverProcessBean prcsBean = new MtsNaverProcessBean();
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(SendType.NAVERT.toString());
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

            // MTS 네이버톡 발송 고유정보셋팅
            prcsBean.setTRAN_REFKEY(""+umsSendMsgBean.getUMS_SEQNO()); // 고객사 발송원장 고유정보
            prcsBean.setSERVICE_ID(umsSendMsgBean.getNAVER_PARTNERKEY());//네이버 파트너키
            prcsBean.setTRAN_TMPL_CD(umsSendMsgBean.getNAVER_TEMPL_ID());
            prcsBean.setTRAN_BUTTON(umsSendMsgBean.getNAVER_BUTTONS());
            prcsBean.setMSG_BODY(umsSendMsgBean.getNAVER_MSG());
            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());

            prcsBean.setTRAN_TMPL_PARAMS(umsSendMsgBean.getREPLACE_VARS());
            prcsBean.setTRAN_TYPE(8);
            prcsBean.setTRAN_STATUS("1");
            prcsBean.setTRAN_REPLACE_TYPE("N");
            prcsBean.setTRAN_IMGHASH(umsSendMsgBean.getNAVER_IMGHASH());

            if(isPersonalMsg) {
                prcsBean.setMSG_VAR_MAP(cuidVarMap.get(cuid));
                prcsBean.setMSG_VARS(gson.toJson(prcsBean.getMSG_VAR_MAP()));
                prcsBean.setTRAN_TMPL_PARAMS(umsSendMsgBean.getREPLACE_VARS());
            }
            if(ObjectUtils.isNotEmpty(userInfos)){
                prcsBean.setMOBILE_NUM(userInfos.get(0).trim());
                if(userInfos.size()>1){
                    prcsBean.setCNAME(userInfos.get(1));
                    prcsBean.setTRAN_USER_NAME(userInfos.get(1));
                }
            }

            naverWorkerMgrPool.putWork(prcsBean);
        }
    }

    @Override
    public void umsNaverSend(PushEachProcessBean pushEachProcessBean, UmsSendMsgBean umsSendMsgBean) throws Exception {
        if(pushEachProcessBean==null) {
            logger.warn("MTS NAVERT 서비스 - PushEachProcessBean is null");
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        MtsNaverProcessBean prcsBean = new MtsNaverProcessBean();
        prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setTRANS_TYPE(transType);
        prcsBean.setSTART_SEND_TYPE(SendType.NAVERT.toString());
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

        if(StringUtils.isNotBlank(umsSendMsgBean.getREPLACE_VARS())){
            prcsBean.setTRAN_TMPL_PARAMS(umsSendMsgBean.getREPLACE_VARS());
        }

        prcsBean.setTRAN_REFKEY(""+umsSendMsgBean.getUMS_SEQNO());
        prcsBean.setSERVICE_ID(umsSendMsgBean.getNAVER_PARTNERKEY());
        prcsBean.setTRAN_TMPL_CD(umsSendMsgBean.getNAVER_TEMPL_ID());
        prcsBean.setTRAN_BUTTON(umsSendMsgBean.getNAVER_BUTTONS());
        prcsBean.setMSG_BODY(replaceMsg(umsSendMsgBean.getNAVER_MSG(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());

        prcsBean.setTRAN_TYPE(8);
        prcsBean.setTRAN_STATUS("1");
        prcsBean.setTRAN_REPLACE_TYPE("N");
        prcsBean.setTRAN_USER_NAME(pushEachProcessBean.getCNAME());

        naverWorkerMgrPool.putWork(prcsBean);
    }
}
