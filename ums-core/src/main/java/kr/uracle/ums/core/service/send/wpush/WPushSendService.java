package kr.uracle.ums.core.service.send.wpush;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.ehcache.PreventIdCacheMgr;
import kr.uracle.ums.core.processor.push.PushEachProcessBean;
import kr.uracle.ums.core.processor.push.PushFailProcessBean;
import kr.uracle.ums.core.processor.wpush.WPushWorkerMgrPool;
import kr.uracle.ums.core.service.UmsSendMacroService;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.service.send.thread.DelaySendThreadRedis;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 모드 웹푸시 발송은 해당 클래스를 이용하여 개별발송 또는 리스트발송처리함.
 */
@Service
public class WPushSendService {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private WPushWorkerMgrPool wpushWorkerMgrPool;
    @Autowired(required = true)
    private Gson gson;
    @Autowired(required = true)
    private PreventIdCacheMgr preventIdCacheMgr;
    @Autowired(required = true)
    private UmsSendMacroService umsSendMacroService;
    @Value("${UMS.USE_PREVENT_SEND:N}")
    private String USE_PREVENT_SEND;

    
    // 치환할 문자가 들어있을 경우 무조건 UPMC에 단건으로 발송 하도록 처리 한다.
    public void wpushMapListSend(final Map<String, List<String>> wpushUserMaps, final UmsSendMsgBean umsSendMsgBean, boolean isDaeCheSend) throws Exception{
        wpushMapListSend(wpushUserMaps, umsSendMsgBean, null, isDaeCheSend);
    }

    public void wpushMapListSend(final Map<String,List<String>> wpushUserMaps, final UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> cuidVarMap, boolean isDaeCheSend) throws Exception {
        // 대체 발송 여부
        String ROOT_CHANNEL_YN = isDaeCheSend?"N":"Y";
        // 개인화 유무
        boolean isPersonalMsg = ObjectUtils.isNotEmpty(cuidVarMap);

        // 이곳에서 SPLIT_MSG_CNT,DELAY_SECOND를 체크하고 DELAY_SECOND가 0이 아니고 SPLIT_MSG_CNT보다 pushUserMaps사이즈가 클경우 쓰레드를 만들어 레디스에 담도록 처리.
        int delaySecond = 0;
        int splitCnt = 0;
        try {
            delaySecond = Integer.parseInt(umsSendMsgBean.getDELAY_SECOND().trim());
            splitCnt = Integer.parseInt(umsSendMsgBean.getSPLIT_MSG_CNT().trim());
        }catch (NumberFormatException e){
            delaySecond = 0;
            logger.error("지연시간 또는 분할발송 카운트가 올바르지 않아 지연발송이 적용되지 않았습니다.");
        }

        // 대체발송이 아니면서 지연발송일 경우만 지연발송 처리하는 쓰레드를 만들어 레디스큐에 담는다.
        if (!isDaeCheSend && delaySecond > 0 && wpushUserMaps.size() > splitCnt) {
            DelaySendThreadRedis delaySendThreadRedis = new DelaySendThreadRedis(wpushUserMaps, umsSendMsgBean, cuidVarMap, delaySecond, splitCnt);
            delaySendThreadRedis.start();
            return;
        }

        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        // 아이디 푸시발송제한 기능을 사용할 경우와 메세지가 치환할 문자가 들어 있을 경우 무조건 단껀으로 보내도록 처리.
        Set<String> cuidSet = wpushUserMaps.keySet();
        for (String cuid : cuidSet) {
            // 2020.01.17 로직 추가 : [채널별 수신관리]아이디 푸시발송제한 검사로직 구현
            if (preventIdCacheMgr.isPreventUserFromID(umsSendMsgBean.getAPP_ID(), cuid, SendType.WPUSH.toString(), umsSendMsgBean.getMSG_TYPE())) {
                // 발송제한 유저이므로 실패처리로 넣는다.
                PushFailProcessBean failPrcsBean = new PushFailProcessBean();
                failPrcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
                failPrcsBean.setTRANS_TYPE(transType);
                failPrcsBean.setSTART_SEND_TYPE(SendType.WPUSH.toString());
                failPrcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
                failPrcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
                failPrcsBean.setROOT_CHANNEL_YN(ROOT_CHANNEL_YN);
                failPrcsBean.setMIN_START_TIME(umsSendMsgBean.getMIN_START_TIME());
                failPrcsBean.setMAX_END_TIME(umsSendMsgBean.getMAX_END_TIME());
                failPrcsBean.setFATIGUE_YN(umsSendMsgBean.getFATIGUE_YN());

                failPrcsBean.setCUST_TRANSKEY(umsSendMsgBean.getCUST_TRANSKEY());
                failPrcsBean.setCUST_TRANSGROUPKEY(umsSendMsgBean.getCUST_TRANSGROUPKEY());
                failPrcsBean.setVAR1(umsSendMsgBean.getVAR1());
                failPrcsBean.setVAR2(umsSendMsgBean.getVAR2());
                failPrcsBean.setVAR3(umsSendMsgBean.getVAR3());
                failPrcsBean.setVAR4(umsSendMsgBean.getVAR4());
                failPrcsBean.setVAR5(umsSendMsgBean.getVAR5());
                failPrcsBean.setVAR6(umsSendMsgBean.getVAR6());
                failPrcsBean.setVAR7(umsSendMsgBean.getVAR7());
                failPrcsBean.setVAR8(umsSendMsgBean.getVAR8());
                failPrcsBean.setVAR9(umsSendMsgBean.getVAR9());

                if(isPersonalMsg) {
                    failPrcsBean.setMSG_VAR_MAP(cuidVarMap.get(cuid));
                    failPrcsBean.setMSG_VARS(gson.toJson(cuidVarMap.get(cuid)));
                }
                failPrcsBean.setCUID(cuid);
                List<String> userInfoArr = wpushUserMaps.get(cuid);
                if(ObjectUtils.isNotEmpty(userInfoArr)){
                    failPrcsBean.setMOBILE_NUM(userInfoArr.get(0).trim());
                    if(userInfoArr.size()>1){
                        failPrcsBean.setCNAME(userInfoArr.get(1));
                    }
                }

                failPrcsBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
                failPrcsBean.setMESSAGE(umsSendMsgBean.getPUSH_MSG());
                failPrcsBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
                failPrcsBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
                failPrcsBean.setPROCESS_END(true); // 최종실패 여부: 메모리에서 관리되는 발송 실패카운터에 실패로 등록하도록 하기위해.
                failPrcsBean.setERRCODE(ErrorManager.ERR_5002);
                failPrcsBean.setRESULTMSG(ErrorManager.getInstance().getMsg(ErrorManager.ERR_5002)+", [웹푸시 수신채널거부] 발송제한 아이디 : " + cuid);
                wpushWorkerMgrPool.putWork(failPrcsBean);
                continue;
            }

            PushEachProcessBean prcsBean = new PushEachProcessBean();
            prcsBean.setMASTERTABLE_SEQNO(umsSendMsgBean.getUMS_SEQNO());
            prcsBean.setTRANS_TYPE(transType);
            prcsBean.setSTART_SEND_TYPE(SendType.WPUSH.toString());
            prcsBean.setUMS_MSG_TYPE(umsSendMsgBean.getMSG_TYPE());
            //공통 발송자 정보 셋팅. 통계 및 발송수 제한에 사용될 수 있음.
            prcsBean.setSENDERID(umsSendMsgBean.getSENDERID());
            prcsBean.setSENDGROUPCODE(umsSendMsgBean.getSENDGROUPCODE());
            prcsBean.setROOT_CHANNEL_YN(ROOT_CHANNEL_YN); // 대체발송의 의한 발송여부 셋팅
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

            if (isPersonalMsg) {
                prcsBean.setMSG_VAR_MAP(cuidVarMap.get(cuid));
                prcsBean.setMSG_VARS(gson.toJson(prcsBean.getMSG_VAR_MAP()));
            }
            Set<SendType> cloneFailRetrySendType = new HashSet<>();
            if (umsSendMsgBean.getFAIL_RETRY_SENDTYPE() != null && umsSendMsgBean.getFAIL_RETRY_SENDTYPE().size() > 0) {
                cloneFailRetrySendType.addAll(umsSendMsgBean.getFAIL_RETRY_SENDTYPE());
            }
            if (StringUtils.isBlank(umsSendMsgBean.getSENDERCODE())) {
                prcsBean.setSENDERCODE(umsSendMsgBean.getSENDERID());
            } else {
                prcsBean.setSENDERCODE(umsSendMsgBean.getSENDERCODE());
            }
            prcsBean.setFAIL_RETRY_SENDTYPE(cloneFailRetrySendType);

            prcsBean.setAPP_ID(umsSendMsgBean.getAPP_ID());
            prcsBean.setTITLE(umsSendMsgBean.getTITLE());
            prcsBean.setMESSAGE(umsSendMsgBean.getPUSH_MSG());
            prcsBean.setBADGENO(umsSendMsgBean.getBADGENO());

            prcsBean.setWPUSH_DOMAIN(umsSendMsgBean.getWPUSH_DOMAIN());
            prcsBean.setWPUSH_TITLE(umsSendMsgBean.getWPUSH_TITLE());
            prcsBean.setWPUSH_MSG(umsSendMsgBean.getWPUSH_MSG());
            prcsBean.setWPUSH_LINK(umsSendMsgBean.getWPUSH_LINK());
            prcsBean.setWPUSH_ICON(umsSendMsgBean.getWPUSH_ICON());
            prcsBean.setWPUSH_BADGENO(umsSendMsgBean.getWPUSH_BADGENO());
            try {
                // 대체발송이 웹푸시 다음 대체발송이 푸시일 경우 그 다음 대체발송을 구해서 넣어야 한다.
                if (cloneFailRetrySendType.contains(SendType.PUSH)) {
                    Set<SendType> failRetrySendType2 = umsSendMacroService.getNextSendChannelSet(umsSendMsgBean, SendType.PUSH);
                    prcsBean.setFAIL_RETRY_SENDTYPE2(failRetrySendType2);
                }
            } catch (Exception ex) {
                logger.info("웹푸시 > 푸시 실패시 다음 대체발송채널 구하는 중 실패 : {}", ex.toString());
            }

            prcsBean.setPRIORITY(umsSendMsgBean.getPRIORITY());
            prcsBean.setDB_IN(umsSendMsgBean.getDB_IN());
            prcsBean.setRESERVEDATE(umsSendMsgBean.getRESERVEDATE());
            prcsBean.setSERVICECODE(umsSendMsgBean.getSERVICECODE());
            prcsBean.setPUSH_TYPE("E");
            prcsBean.setPUSH_FAIL_SMS_SEND(umsSendMsgBean.getPUSH_FAIL_SMS_SEND()); //무조건 Y로 보내서 실패시 T_PUSH_SMS테이블에 남게 처리 하고 UMS Agent가 SMS메세지작성 여부에 따라 SMS발송 여부 판단.
            prcsBean.setPUSH_FAIL_WAIT_MIN(umsSendMsgBean.getPUSH_FAIL_WAIT_MIN());
            prcsBean.setALT_REPLACE_VARS(umsSendMsgBean.getREPLACE_VARS());

            List<String> userInfoArr = wpushUserMaps.get(cuid);
            prcsBean.setCUID(cuid);
            if (userInfoArr != null) {
                String mobileNum = userInfoArr.get(0);
                String cname = userInfoArr.get(1);
                if (mobileNum != null) {
                    prcsBean.setMOBILE_NUM(mobileNum);
                }
                if (cname != null && !"".equals(cname)) {
                    prcsBean.setCNAME(cname);
                }
            }

            prcsBean.setEXT(replaceMsg(umsSendMsgBean.getEXT(), cuid, prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            prcsBean.setWPUSH_EXT(replaceMsg(umsSendMsgBean.getWPUSH_EXT(), cuid, prcsBean.getCNAME(), prcsBean.getMSG_VAR_MAP()));
            
            wpushWorkerMgrPool.putWork(prcsBean);
        }

    }

    private String replaceMsg(String msg ,String cuid, String cname, Map<String,String> msgVarMap){
        if(!"".equals(msg) && msg.indexOf("#{")>-1) {
            try {
                msg = msg.replace("#{아이디}", cuid);
                // 푸시대상자에서 발송일 경우 핸드폰 번호가 없어서 이름이 빈값이다. 푸시에 치환처리 할 수 있도록 빈값일 경우는 치환하지 않는다.
                if (!"".equals(cname)) {
                    msg = msg.replace("#{이름}", cname);
                }

                if (msgVarMap.size() > 0) {
                    // 개인화 메세지 치환처리
                    Set<Map.Entry<String, String>> personalMapSet = msgVarMap.entrySet();
                    for (Map.Entry<String, String> personalEntry : personalMapSet) {
                        msg = msg.replace(personalEntry.getKey(), personalEntry.getValue());
                    }
                }
            }catch (Exception e){
                logger.error(e.toString());
            }

        }
        return msg;
    }
}
