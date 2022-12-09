package kr.uracle.ums.core.service.send.rcs;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.vo.rcs.LguRcsProcessBean;
import kr.uracle.ums.core.common.Constants;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class LguRcsSendService extends BaseRcsSendService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    // 매니저에 RCS 발송 일감 등록
    @Override
    public void umsSend(final Map<String,List<String>> users, final UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> cuidVarMap, boolean isDaeCheSend) throws Exception{
        if(ObjectUtils.isEmpty(users)){
            logger.warn("LGU+ RCS 서비스 - users map is empty");
            return;
        }
        // 대체 발송 여부
        String ROOT_CHANNEL_YN = isDaeCheSend?"N":"Y";
        // 개인화 유무
        boolean isPersonalMsg = ObjectUtils.isNotEmpty(cuidVarMap);
        SendType sendType = SendType.valueOf(umsSendMsgBean.getRCS_TYPE());
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());


        boolean hasImage = Integer.parseInt(umsSendMsgBean.getIMG_GROUP_CNT())>0;
        boolean RCS_IMG_INSERT = umsSendMsgBean.isRCS_IMG_INSERT();
        LguRcsProcessBean prcsBean = null;
        for(String cuid : users.keySet()) {
            //{"아이디":["핸드폰번호","이름"]}
            List<String> userInfos = users.get(cuid);
            prcsBean = new LguRcsProcessBean();
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

            prcsBean.setSTATUS("0");
            prcsBean.setSENDABLE("Y");
            if(sendType == SendType.RCS_MMS && hasImage) {
                prcsBean.setSENDABLE("N");
                // IMG GROUP KEY 채번 방식 - 마스터테이블 구분 + _ + 원장거래키
                prcsBean.setIMG_GROUP_KEY(transType +"_"+ umsSendMsgBean.getUMS_SEQNO());
                prcsBean.setIMG_GROUP_CNT(umsSendMsgBean.getIMG_GROUP_CNT());
            }

            prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
            prcsBean.setMSG_BODY(umsSendMsgBean.getRCS_OBJECT());
            if(StringUtils.isBlank(umsSendMsgBean.getRCS_OBJECT())){
                prcsBean.setTITLE(umsSendMsgBean.getRCS_TITLE());
                prcsBean.setMSG_BODY(umsSendMsgBean.getRCS_MSG());
            }

            prcsBean.setTYPE(Constants.getLguSendType(sendType.toString()));
            prcsBean.setMSG_HEADER(umsSendMsgBean.getMSG_TYPE().equals("I")?"0":"1");
            prcsBean.setMSG_FOOTER(umsSendMsgBean.getFOOTER());
            prcsBean.setMSG_COPYALLOWED(umsSendMsgBean.getCOPY_ALLOWED());

            prcsBean.setFALLBACK_YN("N");
            prcsBean.setMSGBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());
            prcsBean.setDEPT_CODE(umsSendMsgBean.getCUST_TRANSGROUPKEY());
            prcsBean.setBRAND_ID(umsSendMsgBean.getBRAND_ID());
            prcsBean.setBTN_OBJECT(replaceMsg(prcsBean.getBTN_OBJECT(), prcsBean.getCUID(), prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            prcsBean.setBTN_CNT(umsSendMsgBean.getRCS_BTN_CNT());
            prcsBean.setBTN_INSERT_TYPE(umsSendMsgBean.getRCS_BTN_TYPE());

            // 요청자 정보
            prcsBean.setWT_REG_USER_ID(umsSendMsgBean.getSENDERID());
            // 요청 그룹 정보
            prcsBean.setWT_SEND_GRP(umsSendMsgBean.getSENDGROUPCODE());

            rcsWorkerMgrPool.putWork(prcsBean);
        }
        // 이미지를 가진 RCS MMS 경우 마지막 WORK를 추가 등록한다.
        if(hasImage && RCS_IMG_INSERT && prcsBean != null) {
            LguRcsProcessBean forImageBean = new LguRcsProcessBean();
            forImageBean.setTRANS_TYPE(transType);
            forImageBean.setSTART_SEND_TYPE(sendType.toString());
            BeanUtils.copyProperties(prcsBean, forImageBean);
            forImageBean.setImageRegister(true);
            forImageBean.setIMG_PATHS(umsSendMsgBean.getRCS_IMG_PATH());
            rcsWorkerMgrPool.putWork(forImageBean);
        }

    }

    @Override
    public void umsReSend(final PushEachProcessBean pushEachProcessBean, final UmsSendMsgBean umsSendMsgBean) throws Exception{
        if(pushEachProcessBean==null) {
            logger.warn("LGU+ RCS 서비스 - PushEachProcessBean is null");
            return;
        }

        SendType sendType = SendType.valueOf(umsSendMsgBean.getRCS_TYPE());
        boolean hasImage = Integer.parseInt(umsSendMsgBean.getIMG_GROUP_CNT())>0;
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        LguRcsProcessBean prcsBean = new LguRcsProcessBean();
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

        if(sendType == SendType.RCS_MMS && hasImage) {
            prcsBean.setSENDABLE("N");
            // IMG GROUP KEY 채번 방식 - 마스터테이블 구분 + _ + 원장거래키
            prcsBean.setIMG_GROUP_KEY(umsSendMsgBean.getTRANS_TYPE() +"_"+ umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setIMG_GROUP_CNT(umsSendMsgBean.getIMG_GROUP_CNT());
        }

        prcsBean.setMOBILE_NUM(pushEachProcessBean.getMOBILE_NUM());
        prcsBean.setCALLBACK_NUM(umsSendMsgBean.getCALLBACK_NUM());
        prcsBean.setMSG_BODY(umsSendMsgBean.getRCS_OBJECT());
        if(StringUtils.isBlank(umsSendMsgBean.getRCS_OBJECT())){
            prcsBean.setTITLE(umsSendMsgBean.getRCS_TITLE());
            prcsBean.setMSG_BODY(umsSendMsgBean.getRCS_MSG());
        }
        prcsBean.setMSG_BODY(replaceMsg(prcsBean.getMSG_BODY(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));

        prcsBean.setSTATUS("0");
        prcsBean.setSENDABLE("Y");
        prcsBean.setTYPE(Constants.getLguSendType(sendType.toString()));
        prcsBean.setMSG_HEADER(umsSendMsgBean.getMSG_TYPE().equals("I")?"0":"1");
        prcsBean.setMSG_FOOTER(umsSendMsgBean.getFOOTER());
        prcsBean.setMSG_COPYALLOWED(umsSendMsgBean.getCOPY_ALLOWED());
        prcsBean.setFALLBACK_YN("N");
        prcsBean.setMSGBASE_ID(umsSendMsgBean.getRCS_MSGBASE_ID());
        prcsBean.setDEPT_CODE(umsSendMsgBean.getCUST_TRANSGROUPKEY());
        prcsBean.setBRAND_ID(umsSendMsgBean.getBRAND_ID());
        prcsBean.setBTN_OBJECT(replaceMsg(prcsBean.getBTN_OBJECT(),pushEachProcessBean.getCUID(),pushEachProcessBean.getCNAME(),pushEachProcessBean.getMSG_VAR_MAP()));
        prcsBean.setBTN_CNT(umsSendMsgBean.getRCS_BTN_CNT());
        prcsBean.setBTN_INSERT_TYPE(umsSendMsgBean.getRCS_BTN_TYPE());
        prcsBean.setWT_REG_USER_ID(umsSendMsgBean.getSENDERID());
        prcsBean.setWT_SEND_GRP(umsSendMsgBean.getSENDGROUPCODE());
        rcsWorkerMgrPool.putWork(prcsBean);

        // 이미지를 가진 RCS MMS 경우 마지막 WORK를 추가 등록한다.
        if(hasImage) {
            LguRcsProcessBean forImageBean = new LguRcsProcessBean();
            forImageBean.setTRANS_TYPE(transType);
            forImageBean.setSTART_SEND_TYPE(sendType.toString());
            BeanUtils.copyProperties(prcsBean, forImageBean);
            forImageBean.setImageRegister(true);
            forImageBean.setIMG_PATHS(umsSendMsgBean.getRCS_IMG_PATH());
            rcsWorkerMgrPool.putWork(forImageBean);
        }
    }
}
