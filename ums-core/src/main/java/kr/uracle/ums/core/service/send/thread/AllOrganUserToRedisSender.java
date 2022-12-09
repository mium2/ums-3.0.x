package kr.uracle.ums.core.service.send.thread;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.codec.redis.message.UmsSendMsgRedisBean;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.dao.setting.OrganMgrDao;
import kr.uracle.ums.core.dao.ums.UmsDao;
import kr.uracle.ums.core.exception.NotExistUserException;
import kr.uracle.ums.core.processor.redis.RedisPushManager;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 해당 클래스는 조직도에 등록된 전체회원 정보를 가져와 매크로에 정의된 첫번째 발송채널 정보를 이용하여 레디스에 개별비회원 발송등록 처리만 한다.
 */
public class AllOrganUserToRedisSender extends BaseThreadRedis {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Logger reqSendLogger = LoggerFactory.getLogger("sentRedisLogger");
    
    private int MULTIKEY_SIZE = 2000;
    
    private OrganMgrDao organMgrDao;
    
    private int putTotalCnt = 0;

    private SendType sendType;

    public AllOrganUserToRedisSender(UmsSendMsgBean umsSendMsgBean){
        super(umsSendMsgBean);
        this.organMgrDao = UmsInitListener.wContext.getBean(OrganMgrDao.class);
        this.umsDao = UmsInitListener.wContext.getBean(UmsDao.class);
    }

    public Map<String,Object> processInfo() throws  Exception{
        init();

        sendType = SendType.valueOf(umsSendMsgBean.getSTART_SEND_KIND());

        TOTAL_SEND_CNT = organMgrDao.selAllOrganMemberCnt();

        if(TOTAL_SEND_CNT==0){
            throw new NotExistUserException("조직도에 등록된 사용자가 존재하지 않습니다.");
        }

        // 원장 총 요청 건수 지정
        umsSendMsgBean.setTOTAL_CNT(TOTAL_SEND_CNT);
        // 예약발송일 경우 체크
        if (StringUtils.isNotBlank(umsSendMsgBean.getRESERVEDATE())) {
            umsDao.inUmsSendReserveMsg(umsSendMsgBean);
        }else{
            // 발송 테이블(T_UMS_SEND)에 발송원장 저장.
            umsDao.inUmsSendMsg(umsSendMsgBean);
            TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
            if (TOTAL_SEND_CNT > 0) {
//                sentInfoManager.setInitTotalSendCnt(transType, umsSendMsgBean.getUMS_SEQNO(), TOTAL_SEND_CNT);
                //프로그래스바를 위해서 발송카운트 셋팅
                sentInfoManager.setInitThreadSendCnt(transType, "T_"+umsSendMsgBean.getUMS_SEQNO(), TOTAL_SEND_CNT);
            }
            returnResultMap.put("PROGRESS_SEQNO","T_"+umsSendMsgBean.getUMS_SEQNO());
        }

        //[공통필수] : Response 발송수 공통로직으로 채널별 발송카운트 UmsSendMsgBean에 셋팅. 원장테이블 발송카운트는 Process에서 처리한다.
        super.setChannelSendCnt(sendType, TOTAL_SEND_CNT);

        //[공통필수] : 채널별 발송 카운트 셋팅 및 발송원장 시퀀스 셋팅
        umsSendCommonService.setRspChannelSendCnt(umsSendMsgBean, returnResultMap);

        return returnResultMap;
    }

    @Override
    protected int send() {
    	try {
    		// DB로 부터 페이징 기능을 이용하여 조회
    		Map<String, Object> dbParamMap = new HashMap<String, Object>();
    		int pageNum = 1;
    		int pageSize = 2000;
    		
    		String preTarget = null;
    		List<String> preTargetInfo = new ArrayList<String>();
    		while (true) {
    			dbParamMap.put("pageNum", pageNum);
    			dbParamMap.put("pageSize", pageSize);
    			List<Map<String, Object>> organMembers = organMgrDao.selOranSendMember(dbParamMap);
    			
    			if (organMembers == null || organMembers.size() == 0) break;
    			
    			for (int i = 0; i < organMembers.size(); i++) {
    				Map<String, Object> organMemberMap = organMembers.get(i);
    				try {
	                      Object DB_memeberID = organMemberMap.get("MEMBERID");
	                      Object DB_mobile = organMemberMap.get("MOBILE");
	                      Object DB_memberName = organMemberMap.get("MEMBERNAME");
	                      
	                      if(DB_memeberID == null) continue;
	                      List<String> hpAndnameInfos = new ArrayList<String>();
	                      hpAndnameInfos.add(DB_mobile == null ? "": DB_mobile.toString());
	                      hpAndnameInfos.add(DB_memberName == null ? "" : DB_memberName.toString());
	                      
	                      umsSendMsgBean.setRCS_IMG_INSERT(false);
	                      
	                      if(i>0) sendTaskToRedis(preTarget, preTargetInfo, null);
	                      preTarget = DB_memeberID.toString();
	                      Collections.copy(preTargetInfo, hpAndnameInfos);
	                      
                  } catch (Exception e) {
	                      TOTAL_SEND_CNT = TOTAL_SEND_CNT-1;
	                      TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
//	                      sentInfoManager.setInitTotalSendCnt(transType, umsSendMsgBean.getUMS_SEQNO(), TOTAL_SEND_CNT);
	                      logger.error(e.toString());
	                      sentInfoManager.addSent_T(transType, "T_"+umsSendMsgBean.getUMS_SEQNO());
                  }
              }
              pageNum++;
    		}
    		if(preTarget != null) {
    			umsSendMsgBean.setRCS_IMG_INSERT(true);
    			sendTaskToRedis(preTarget, preTargetInfo, null);		    			
    		}
    	}catch (Exception e){
    		e.printStackTrace();
    		logger.error("[발송 실패 : {}",e.toString());
    	}
		return TOTAL_SEND_CNT;
    }

    protected void sendTaskToRedis(String CUID, List<String> HPInfos, String msgVars) throws Exception{
        UmsSendMsgRedisBean umsSendMsgRedisBean = makeUmsSendMsgRedisBean(umsSendMsgBean); 
        umsSendMsgRedisBean.setTARGET_CUID(CUID);
        if(HPInfos != null){
            umsSendMsgRedisBean.setTARGET_PHONEINFOS(HPInfos);
        }

        //레디스 발송처리 로그저장
        reqSendLogger.info(umsSendMsgRedisBean.toString());
        RedisPushManager.getInstance().putWork(umsSendMsgRedisBean);
        TransType transType = TransType.valueOf(umsSendMsgBean.getTRANS_TYPE());
        sentInfoManager.addSent_T(transType, "T_"+umsSendMsgBean.getUMS_SEQNO());

        putTotalCnt++;
        
        //속도제한 처리
        if(isLimitSend) {
            try {
                if(putTotalCnt%MULTIKEY_SIZE == 0) {
                    logger.info("" + LIMITSECOND + "초 동안 멈춤");
                    sleep(LIMITSECOND * 1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
