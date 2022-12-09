package kr.uracle.ums.core.processor;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.enums.TransType;
import kr.uracle.ums.core.processor.bean.SentInfoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 15..
 */
@Service
public class SentInfoManager {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired(required = true)
    private SentInfoDbUpManager sentInfoDbUpManager;
    private static Map<String, SentInfoBean> sentInfoMap = new ConcurrentHashMap<String,SentInfoBean>();

    //대량발송처리시 레디스에 담은 카운터를 따로 관리하여 프로그래스바 진행을 마치게 하기 위해
    private static Map<String,SentInfoBean> messSentInfoMap = new ConcurrentHashMap<String,SentInfoBean>();

    private ScheduledExecutorService m_scheduler;
    private ScheduledFuture chekcerHandler;
    private static final int NUM_SCHEDULER_TIMER_THREAD = 2;
    private static final int KEEPALIVE_SECS = 5; // 5초에 한번씩 체크


    public void sentInfoStart(){
        try {
            m_scheduler = Executors.newScheduledThreadPool(NUM_SCHEDULER_TIMER_THREAD);
            schedulerStart();
        }catch(Exception e){
            logger.error(e.toString());
            e.printStackTrace();
        }
    }

    public void sentInfoStop(){
        try {
            if (chekcerHandler != null) {
                chekcerHandler.cancel(true);
            }
            m_scheduler.shutdown();
        }catch(Exception e){
            logger.error(e.toString());
            e.printStackTrace();
        }
    }
    final Runnable dbPutWorker = new Runnable() {
        public void run() {
            try {
                putDBSentInfo();
            }catch (Exception e){
                logger.error(e.toString());
                e.printStackTrace();
            }
        }
    };

    private void schedulerStart() {
        if (chekcerHandler != null) {
            chekcerHandler.cancel(false);
        }
        chekcerHandler = m_scheduler.scheduleWithFixedDelay(dbPutWorker, KEEPALIVE_SECS, KEEPALIVE_SECS, TimeUnit.SECONDS);
    }

    /** 현재까지 등록된 DB 처리 일감 등록 **/
    private synchronized void putDBSentInfo(){
        Set<Map.Entry<String, SentInfoBean>> set1 = sentInfoMap.entrySet();
        if(set1==null){
            return;
        }
        for (Map.Entry<String, SentInfoBean> e : set1) {
            try {
                SentInfoBean cloneSentInfoBean = e.getValue().clone();
                sentInfoDbUpManager.putWork(cloneSentInfoBean);
            } catch (CloneNotSupportedException e1) {
                logger.error(e1.toString());
            }

        }
        sentInfoMap.clear();
    }

    public synchronized void setInitTotalSendCnt(TransType TRANS_TYPE, long seqno, int totalSendCnt) {
        String SentInfoID = TRANS_TYPE.toString() + seqno;
        SentInfoBean sentInfoBean = sentInfoMap.get(SentInfoID);
        if(sentInfoBean==null){
            sentInfoBean = new SentInfoBean(TRANS_TYPE);
        }
        sentInfoBean.setUMS_SEQNO(seqno);
        sentInfoBean.setREQ_SEND_CNT(totalSendCnt);
        sentInfoMap.put(SentInfoID, sentInfoBean);
    }

    public synchronized void setInitThreadSendCnt(TransType TRANS_TYPE, String processSeqno, int totalSendCnt) {
        String SentInfoID = TRANS_TYPE.toString() + processSeqno;
        SentInfoBean sentInfoBean = messSentInfoMap.get(SentInfoID);
        if(sentInfoBean==null){
            sentInfoBean = new SentInfoBean(TRANS_TYPE);
        }
        sentInfoBean.setREQ_SEND_CNT(totalSendCnt);
        messSentInfoMap.put(SentInfoID, sentInfoBean);
    }

    public synchronized void addSent_T(TransType TRANS_TYPE, String processSeqno){
        String SentInfoID = TRANS_TYPE.toString()+processSeqno;
        if(messSentInfoMap.containsKey(SentInfoID)){
            SentInfoBean sentInfoBean = messSentInfoMap.get(SentInfoID);
            sentInfoBean.setSUCC_CNT(sentInfoBean.getSUCC_CNT() + 1);

            if(sentInfoBean.getREQ_SEND_CNT()<=(sentInfoBean.getSUCC_CNT()+sentInfoBean.getFAIL_CNT())){
                messSentInfoMap.remove(SentInfoID);
            }else{
                messSentInfoMap.put(SentInfoID, sentInfoBean);
            }
        }
    }

    /**
     * 성공카운트는 DB에 따로 업데이트 할 필요가 없다 발송시 초기 세팅값으로 처리 함.
     * @param seqno
     * @param sendType
     */
    public synchronized void addSendSucc(TransType TRANS_TYPE, long seqno, SendType sendType){
        String SentInfoID = TRANS_TYPE.toString()+seqno;
        SentInfoBean sentInfoBean =null;
        if(sentInfoMap.containsKey(SentInfoID)){
            sentInfoBean = sentInfoMap.get(SentInfoID);

        }else{
            sentInfoBean = new SentInfoBean(TRANS_TYPE);
            sentInfoBean.setUMS_SEQNO(seqno);

        }
        sentInfoBean.setSUCC_CNT(sentInfoBean.getSUCC_CNT() + 1);

        // 채널별 발송성공카운트 정보
        switch (sendType){
            case PUSH:
                sentInfoBean.setPUSH_SEND_CNT(sentInfoBean.getPUSH_SEND_CNT()+1);
                break;
            case WPUSH:
                sentInfoBean.setWPUSH_SEND_CNT(sentInfoBean.getWPUSH_SEND_CNT()+1);
                break;
            case KKOALT:
                sentInfoBean.setKKOALT_SEND_CNT(sentInfoBean.getKKOALT_SEND_CNT()+1);
                break;
            case KKOFRT:
                sentInfoBean.setKKOFRT_SEND_CNT(sentInfoBean.getKKOFRT_SEND_CNT()+1);
                break;
            case SMS:
                sentInfoBean.setSMS_SEND_CNT(sentInfoBean.getSMS_SEND_CNT()+1);
                sentInfoBean.setSMS_TOTAL_SEND_CNT(sentInfoBean.getSMS_TOTAL_SEND_CNT()+1);
                break;
            case LMS:
                sentInfoBean.setLMS_SEND_CNT(sentInfoBean.getLMS_SEND_CNT()+1);
                sentInfoBean.setSMS_TOTAL_SEND_CNT(sentInfoBean.getSMS_TOTAL_SEND_CNT()+1);
                break;
            case MMS:
                sentInfoBean.setMMS_SEND_CNT(sentInfoBean.getMMS_SEND_CNT()+1);
                sentInfoBean.setSMS_TOTAL_SEND_CNT(sentInfoBean.getSMS_TOTAL_SEND_CNT()+1);
                break;
            case RCS_SMS:
                sentInfoBean.setRCS_SMS_SEND_CNT(sentInfoBean.getRCS_SMS_SEND_CNT()+1);
                sentInfoBean.setRCS_TOTAL_SEND_CNT(sentInfoBean.getRCS_TOTAL_SEND_CNT()+1);
                break;
            case RCS_LMS:
                sentInfoBean.setRCS_LMS_SEND_CNT(sentInfoBean.getRCS_LMS_SEND_CNT()+1);
                sentInfoBean.setRCS_TOTAL_SEND_CNT(sentInfoBean.getRCS_TOTAL_SEND_CNT()+1);
                break;
            case RCS_MMS:
                sentInfoBean.setRCS_MMS_SEND_CNT(sentInfoBean.getRCS_MMS_SEND_CNT()+1);
                sentInfoBean.setRCS_TOTAL_SEND_CNT(sentInfoBean.getRCS_TOTAL_SEND_CNT()+1);
                break;
            case RCS_CELL:
                sentInfoBean.setRCS_CELL_SEND_CNT(sentInfoBean.getRCS_CELL_SEND_CNT()+1);
                sentInfoBean.setRCS_TOTAL_SEND_CNT(sentInfoBean.getRCS_TOTAL_SEND_CNT()+1);
                break;
            case RCS_DESC:
                sentInfoBean.setRCS_DESC_SEND_CNT(sentInfoBean.getRCS_DESC_SEND_CNT()+1);
                sentInfoBean.setRCS_TOTAL_SEND_CNT(sentInfoBean.getRCS_TOTAL_SEND_CNT()+1);
                break;
            case RCS_FREE:
                sentInfoBean.setRCS_FREE_SEND_CNT(sentInfoBean.getRCS_FREE_SEND_CNT()+1);
                sentInfoBean.setRCS_TOTAL_SEND_CNT(sentInfoBean.getRCS_TOTAL_SEND_CNT()+1);
                break;
            case NAVERT:
                sentInfoBean.setNAVER_SEND_CNT(sentInfoBean.getNAVER_SEND_CNT()+1);
                break;
        }
        sentInfoMap.put(SentInfoID, sentInfoBean);
    }


    public synchronized void setSendSucc(TransType TRANS_TYPE, long seqno, int succCnt, SendType sendType){
        String SentInfoID = TRANS_TYPE.toString()+seqno;
        if(sentInfoMap.containsKey(SentInfoID)){
            SentInfoBean sentInfoBean = sentInfoMap.get(SentInfoID);
            sentInfoBean.setSUCC_CNT(sentInfoBean.getSUCC_CNT() + succCnt);
            sentInfoMap.put(SentInfoID, sentInfoBean);
        }else{
            SentInfoBean sentInfoBean = new SentInfoBean(TRANS_TYPE);
            sentInfoBean.setUMS_SEQNO(seqno);
            sentInfoBean.setSUCC_CNT(sentInfoBean.getSUCC_CNT() + succCnt);
            sentInfoMap.put(SentInfoID, sentInfoBean);
        }
    }


    public synchronized void addSendFail(TransType TRANS_TYPE, long seqno,SendType sendType,boolean isFinalFail){
        String SentInfoID = TRANS_TYPE.toString()+seqno;

        SentInfoBean sentInfoBean = new SentInfoBean(TRANS_TYPE);
        if(sentInfoMap.containsKey(SentInfoID)){
            sentInfoBean = sentInfoMap.get(SentInfoID);
        }

        sentInfoBean.setUMS_SEQNO(seqno);
        sentInfoBean.setFAIL_CNT(sentInfoBean.getFAIL_CNT() + 1);

        if(isFinalFail){
            sentInfoBean.setFINAL_FAIL_CNT(sentInfoBean.getFINAL_FAIL_CNT()+1);
        }
        // 채널별 발송실패카운트 정보
        switch (sendType){
            case PUSH:
                sentInfoBean.setPUSH_FAIL_CNT(sentInfoBean.getPUSH_FAIL_CNT()+1);
                break;
            case WPUSH:
                sentInfoBean.setWPUSH_FAIL_CNT(sentInfoBean.getWPUSH_FAIL_CNT()+1);
                break;
            case KKOALT:
                sentInfoBean.setKKOALT_FAIL_CNT(sentInfoBean.getKKOALT_FAIL_CNT()+1);
                break;
            case KKOFRT:
                sentInfoBean.setKKOFRT_FAIL_CNT(sentInfoBean.getKKOFRT_FAIL_CNT()+1);
                break;
            case SMS:
                sentInfoBean.setSMS_FAIL_CNT(sentInfoBean.getSMS_FAIL_CNT()+1);
                sentInfoBean.setSMS_TOTAL_FAIL_CNT(sentInfoBean.getSMS_TOTAL_FAIL_CNT()+1);
                break;
            case LMS:
                sentInfoBean.setLMS_FAIL_CNT(sentInfoBean.getLMS_FAIL_CNT()+1);
                sentInfoBean.setSMS_TOTAL_FAIL_CNT(sentInfoBean.getSMS_TOTAL_FAIL_CNT()+1);
                break;
            case MMS:
                sentInfoBean.setMMS_FAIL_CNT(sentInfoBean.getMMS_FAIL_CNT()+1);
                sentInfoBean.setSMS_TOTAL_FAIL_CNT(sentInfoBean.getSMS_TOTAL_FAIL_CNT()+1);
                break;
            case RCS_SMS:
                sentInfoBean.setRCS_SMS_FAIL_CNT(sentInfoBean.getRCS_SMS_FAIL_CNT()+1);
                sentInfoBean.setRCS_TOTAL_FAIL_CNT(sentInfoBean.getRCS_TOTAL_FAIL_CNT()+1);
                break;
            case RCS_LMS:
                sentInfoBean.setRCS_LMS_FAIL_CNT(sentInfoBean.getRCS_LMS_FAIL_CNT()+1);
                sentInfoBean.setRCS_TOTAL_FAIL_CNT(sentInfoBean.getRCS_TOTAL_FAIL_CNT()+1);
                break;
            case RCS_MMS:
                sentInfoBean.setRCS_MMS_FAIL_CNT(sentInfoBean.getRCS_MMS_FAIL_CNT()+1);
                sentInfoBean.setRCS_TOTAL_FAIL_CNT(sentInfoBean.getRCS_TOTAL_FAIL_CNT()+1);
                break;
            case RCS_CELL:
                sentInfoBean.setRCS_CELL_FAIL_CNT(sentInfoBean.getRCS_CELL_FAIL_CNT()+1);
                sentInfoBean.setRCS_TOTAL_FAIL_CNT(sentInfoBean.getRCS_TOTAL_FAIL_CNT()+1);
                break;
            case RCS_DESC:
                sentInfoBean.setRCS_DESC_FAIL_CNT(sentInfoBean.getRCS_DESC_FAIL_CNT()+1);
                sentInfoBean.setRCS_TOTAL_FAIL_CNT(sentInfoBean.getRCS_TOTAL_FAIL_CNT()+1);
                break;
            case RCS_FREE:
                sentInfoBean.setRCS_FREE_FAIL_CNT(sentInfoBean.getRCS_FREE_FAIL_CNT()+1);
                sentInfoBean.setRCS_TOTAL_FAIL_CNT(sentInfoBean.getRCS_TOTAL_FAIL_CNT()+1);
                break;
            case NAVERT:
                sentInfoBean.setNAVER_FAIL_CNT(sentInfoBean.getNAVER_FAIL_CNT()+1);
                break;
        }
        sentInfoMap.put(SentInfoID, sentInfoBean);
    }

    @Deprecated
    private synchronized void removeSentInfo(TransType TRANS_TYPE, long seqno){
        removeSentInfo(TRANS_TYPE.toString(), seqno);
    }
    private synchronized void removeSentInfo(String TRANS_TYPE, long seqno){
        String SentInfoID = TRANS_TYPE+seqno;
        logger.debug("SEND COUNT Saved Map Delete : OK  SEQNO:{}, 사이즈:{}", SentInfoID, sentInfoMap.size());
        sentInfoMap.remove(SentInfoID);
    }


    public synchronized void setSendFail(TransType TRANS_TYPE, long seqno,int failCnt,SendType sendType,boolean isFinalFail){
        String SentInfoID = TRANS_TYPE.toString()+seqno;
        SentInfoBean sentInfoBean = new SentInfoBean(TRANS_TYPE);

        if(sentInfoMap.containsKey(SentInfoID)){
            sentInfoBean = sentInfoMap.get(SentInfoID);
        }

        sentInfoBean.setUMS_SEQNO(seqno);
        sentInfoBean.setFAIL_CNT(sentInfoBean.getFAIL_CNT() + failCnt);

        if(isFinalFail){
            sentInfoBean.setFINAL_FAIL_CNT(sentInfoBean.getFINAL_FAIL_CNT()+failCnt);
        }
        sentInfoMap.put(SentInfoID, sentInfoBean);
    }



    public synchronized SentInfoBean getSentInfoMap(TransType TRANS_TYPE, String seqno) {
        SentInfoBean cloneSentInfoBean = null;
        String SentInfoID = TRANS_TYPE.toString() + seqno;

        if(seqno.startsWith("T_")){
            SentInfoBean sentInfoBean = messSentInfoMap.get(SentInfoID);
            if (sentInfoBean != null) {
                try {
                    cloneSentInfoBean = sentInfoBean.clone();
                } catch (Exception e) {
                    logger.error("SentInfoBean clone exception");
                }
            }
        }else {
            SentInfoBean sentInfoBean = sentInfoMap.get(SentInfoID);
            if (sentInfoBean != null) {
                try {
                    cloneSentInfoBean = sentInfoBean.clone();
                } catch (Exception e) {
                    logger.error("SentInfoBean clone exception");
                }
            }
        }
        return cloneSentInfoBean;
    }

    public synchronized Map<String, SentInfoBean> getCloneSentInfoMap() {
        Map<String,SentInfoBean> cloneMap = new HashMap<String, SentInfoBean>();
        Set<Map.Entry<String, SentInfoBean>> set1 = sentInfoMap.entrySet();
        for (Map.Entry<String, SentInfoBean> e : set1) {
            cloneMap.put(e.getKey(), e.getValue());
        }
        return cloneMap;
    }

}
