package kr.uracle.ums.core.service.send.thread;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.message.UmsSendMsgRedisBean;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.exception.NotExistUserException;
import kr.uracle.ums.core.processor.redis.RedisPushManager;
import kr.uracle.ums.core.service.bean.ReUmsSentBean;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.util.StringUtil;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Y.B.H(mium2) on 2021-04-01.
 */
@Deprecated
public class ReUmsSendThread extends BaseThreadRedis{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Logger reqSendLogger = LoggerFactory.getLogger("sentRedisLogger");
    
    private int MULTIKEY_SIZE = 10000;
    private Map<String,String> reqMap;
    private SqlSessionTemplate sqlSessionTemplate;

    private UmsDao umsDao;
    
    private int totalSendCnt = 0;
    private SendType startSendType = SendType.PUSH;

    public ReUmsSendThread(Map<String,String> _reqMap){
    	super(new UmsSendMsgBean(TransType.REAL.toString()));
        this.reqMap = _reqMap;
        sqlSessionTemplate = (SqlSessionTemplate)UmsInitListener.wContext.getBean("sqlSessionTemplate");
        umsDao = UmsInitListener.wContext.getBean(UmsDao.class);
    }

    public Map<String,Object> processInfo() throws  Exception{
        init();

        ReUmsSentBean reUmsSentBean = sqlSessionTemplate.selectOne("mybatis.status.resend.reSentInfo",reqMap);
        totalSendCnt = (int)(reUmsSentBean.getPUSH_SEND_CNT()+reUmsSentBean.getALT_SEND_CNT()+reUmsSentBean.getFRT_SEND_CNT()+reUmsSentBean.getSMS_CNT()+reUmsSentBean.getFAIL_CNT());
        if(totalSendCnt ==0){
            throw new NotExistUserException("[ReUmsSendThread] UMS에 등록된 사용자가 없습니다.");
        }

        if(SendType.SMS.toString().equals(reUmsSentBean.getSTART_SEND_KIND())) {
            umsSendMsgBean.setSMS_CNT((int)totalSendCnt);
            startSendType = SendType.SMS;
        }else if(SendType.KKOFRT.toString().equals(reUmsSentBean.getSTART_SEND_KIND())){
            umsSendMsgBean.setFRIENDTOLK_CNT((int)totalSendCnt);
            startSendType = SendType.KKOALT;
        }else if(SendType.KKOALT.toString().equals(reUmsSentBean.getSTART_SEND_KIND())){
            umsSendMsgBean.setALLIMTOLK_CNT((int)totalSendCnt);
            startSendType = SendType.KKOFRT;
        }else{
            umsSendMsgBean.setPUSH_SEND_CNT(totalSendCnt);
            startSendType = SendType.PUSH;
        }
        umsSendMsgBean.setSTART_SEND_KIND(reUmsSentBean.getSTART_SEND_KIND());
        umsSendMsgBean.setMSG_TYPE(reUmsSentBean.getMSG_TYPE());
        umsSendMsgBean.setSENDERID(reUmsSentBean.getSENDERID());
        umsSendMsgBean.setSENDGROUPCODE(reUmsSentBean.getSENDGROUPCODE());

        // 푸시 발송정보
        umsSendMsgBean.setAPP_ID(reUmsSentBean.getAPP_ID());
        umsSendMsgBean.setPUSH_MSG(reUmsSentBean.getPUSH_MSG());
        umsSendMsgBean.setSOUNDFILE(reUmsSentBean.getSOUNDFILE());
        umsSendMsgBean.setBADGENO(reUmsSentBean.getBADGENO());
        umsSendMsgBean.setPRIORITY(reUmsSentBean.getPRIORITY());
        umsSendMsgBean.setEXT(reUmsSentBean.getEXT());
        umsSendMsgBean.setSENDERCODE(reUmsSentBean.getSENDERCODE());
        umsSendMsgBean.setSERVICECODE(reUmsSentBean.getSERVICECODE());
        umsSendMsgBean.setTARGET_USER_TYPE(reUmsSentBean.getTARGET_USER_TYPE());

        // 옵션 파라미터
        umsSendMsgBean.setTITLE(reUmsSentBean.getTITLE());
        umsSendMsgBean.setATTACHFILE(reUmsSentBean.getATTACHFILE());

        // 대체발송 필수
        umsSendMsgBean.setCALLBACK_NUM(reUmsSentBean.getCALLBACK_NUM());

        // 알림톡 발송정보
        umsSendMsgBean.setALLIMTOLK_TEMPLCODE(reUmsSentBean.getALLIMTOLK_TEMPLCODE());
        umsSendMsgBean.setKKOALT_SVCID(reUmsSentBean.getKKOALT_SVCID());
        umsSendMsgBean.setREPLACE_VARS(reUmsSentBean.getREPLACE_VARS());

        // 친구톡 발송정보
        umsSendMsgBean.setFRIENDTOLK_MSG(reUmsSentBean.getFRIENDTOLK_MSG());
        umsSendMsgBean.setKKOFRT_SVCID(reUmsSentBean.getKKOFRT_SVCID());
        umsSendMsgBean.setPLUS_ID(reUmsSentBean.getPLUS_ID());
        umsSendMsgBean.setFRT_TEMPL_ID(reUmsSentBean.getFRT_TEMPL_ID());
        umsSendMsgBean.setKKO_IMG_PATH(reUmsSentBean.getKKO_IMG_PATH());
        umsSendMsgBean.setKKO_IMG_LINK_URL(reUmsSentBean.getKKO_IMG_LINK_URL());

        //카카오 챗버블 버튼
        umsSendMsgBean.setKKO_BTNS(reUmsSentBean.getKKO_BTNS());

        // SMS 발송정보
        umsSendMsgBean.setSMS_TITLE(reUmsSentBean.getSMS_TITLE());
        umsSendMsgBean.setSMS_MSG(reUmsSentBean.getSMS_MSG());
        umsSendMsgBean.setSMS_TEMPL_ID(reUmsSentBean.getSMS_TEMPL_ID());
        umsSendMsgBean.setMMS_IMGURL(reUmsSentBean.getMMS_IMGURL());

    	// 원장 총 요청 건수 지정
    	umsSendMsgBean.setTOTAL_CNT(totalSendCnt);
        // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
        umsDao.inUmsSendMsg(umsSendMsgBean);
        if (totalSendCnt > 0) {
        	TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
            sentInfoManager.setInitTotalSendCnt(transType, umsSendMsgBean.getUMS_SEQNO(), totalSendCnt);
            //프로그래스바를 위해서 발송카운트 셋팅
            sentInfoManager.setInitThreadSendCnt(transType, "T_"+umsSendMsgBean.getUMS_SEQNO(), totalSendCnt);
        }

        returnResultMap.put("processSeqno","T_"+umsSendMsgBean.getUMS_SEQNO());
        returnResultMap.put("pushSendCnt","0");
        returnResultMap.put("kkoAltCnt",""+umsSendMsgBean.getALLIMTOLK_CNT());
        returnResultMap.put("kkoFrtCnt",""+umsSendMsgBean.getFRIENDTOLK_CNT());
        returnResultMap.put("smsCnt",""+umsSendMsgBean.getSMS_CNT());
        returnResultMap.put("failCnt",""+umsSendMsgBean.getFAIL_CNT());
        return returnResultMap;
    }

    @Override
    protected int send() {
        try {
            Map<String,Object> dbParamMap = new HashMap<>();
            dbParamMap.put("FETCHCNT",MULTIKEY_SIZE);
            dbParamMap.put("UMS_SEQNO",reqMap.get("UMS_SEQNO"));
            boolean isEnd = false;
            long lastDetailSeqno = 0;
            Set<String> sentCuidChkSet = new HashSet<>(); //중복발송을 방지하기 위해

            while (!isEnd) {
                try {
                    dbParamMap.put("DETAIL_SEQNO",lastDetailSeqno);
                    List<Map<String,Object>> reSendUsers = sqlSessionTemplate.selectList("mybatis.status.resend.selReSentUsers",dbParamMap);

                    for (Map<String,Object> userInfoMap : reSendUsers) {
                        lastDetailSeqno = StringUtil.bigDicimalToLong(userInfoMap.get("DETAIL_SEQNO"));
                        String cuid = userInfoMap.get("CUID").toString();
                        List<String> hPInfos = null;
                        if(userInfoMap.containsKey("MOBILE_NUM")){
                            hPInfos = new ArrayList<>();
                            Object phoneNum = userInfoMap.get("MOBILE_NUM");
                            if(phoneNum!=null){
                                hPInfos.add(phoneNum.toString());
                                if(userInfoMap.containsKey("CNAME") && userInfoMap.get("CNAME")!=null){
                                    hPInfos.add(userInfoMap.get("CNAME").toString());
                                }else{
                                    hPInfos.add(cuid); //이름이 없을경우를 대비해 이름에 아이디를 넣어둠.
                                }
                            }
                        }
                        if(!sentCuidChkSet.contains(cuid)) {
                            sentCuidChkSet.add(cuid);
                            String msgVars = null;

                            if(userInfoMap.containsKey("MSG_VARS")){
                                msgVars = userInfoMap.get("MSG_VARS").toString();
                            }
                            sendTaskToRedis(cuid, hPInfos, msgVars);
                            TOTAL_SEND_CNT++;
                        }
                    }

                    if(reSendUsers==null || reSendUsers.size()==0){
                        isEnd = true;
                    }

                }catch (Exception e){
                    logger.error("[kr.uracle.ums.service.thread.ReUmsSendThread] {} ",e.getMessage());
                    continue;
                }
            }

            int diffSentCnt = totalSendCnt-sentCuidChkSet.size();
            if(diffSentCnt>0){
                // 이렇게 하는 이유는 초기에 가져온 발송전체카운트가 푸시삭제유저가 발생하여 발송수가 줄 경우 ProgressBar가 사라지지 않는 버그를 발생시키지 않기 위해.
                for(int i=0; i<diffSentCnt; i++){
                	TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
                    sentInfoManager.addSent_T(transType , "T_"+umsSendMsgBean.getUMS_SEQNO());
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            logger.error("[AllUmsUserSmsSendThreadRedis ERROR] 발송 실패 : {}",e.toString());
        }
        
		return TOTAL_SEND_CNT;
    }

	@Override
	protected void sendTaskToRedis(String CUID, List<String> HPInfos, String msgVars) throws Exception {
		   UmsSendMsgRedisBean umsSendMsgRedisBean = super.makeUmsSendMsgRedisBean(umsSendMsgBean,msgVars);
	        umsSendMsgRedisBean.setTARGET_CUID(CUID);
	        if(HPInfos!=null){
	            umsSendMsgRedisBean.setTARGET_PHONEINFOS(HPInfos);
	        }

	        //레디스 발송처리 로그저장
	        reqSendLogger.info(umsSendMsgRedisBean.toString());

	        RedisPushManager.getInstance().putWork(umsSendMsgRedisBean);
	        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
	        sentInfoManager.addSent_T(transType, "T_"+umsSendMsgBean.getUMS_SEQNO());
	}

}
