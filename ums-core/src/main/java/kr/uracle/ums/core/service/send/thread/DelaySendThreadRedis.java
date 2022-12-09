package kr.uracle.ums.core.service.send.thread;

import com.google.gson.Gson;
import kr.uracle.ums.core.processor.redis.RedisPushManager;
import kr.uracle.ums.core.service.bean.UmsSendMsgBean;
import kr.uracle.ums.core.service.send.split.SplitSendLogUtil;
import kr.uracle.ums.core.service.send.split.SplitSenderManager;
import kr.uracle.ums.codec.redis.message.UmsSendMsgRedisBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DelaySendThreadRedis extends BaseThreadRedis{
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Logger reqSendLogger = LoggerFactory.getLogger("sentRedisLogger");

    private SplitSendLogUtil splitSendLogUtil = null;
    private int SPLIT_PAGE_CNT = 0;
    private Map<String, Map<String,String>> cuidVarMap;

    private Map<String, List<String>> userMaps = new HashMap<String,List<String>>();

    private int MULTIKEY_SIZE = 0;

    private Gson gson = new Gson();

    public DelaySendThreadRedis(final Map<String,List<String>> userMaps, UmsSendMsgBean umsSendMsgBean, Map<String,Map<String,String>> _cuidVarMap, int limitSecond, int limitCnt){
        super(umsSendMsgBean);
        this.umsSendMsgBean = umsSendMsgBean;
        this.userMaps = userMaps;
        this.LIMITSECOND = limitSecond;
        this.MULTIKEY_SIZE = limitCnt;
        this.cuidVarMap = _cuidVarMap;
    }

    @Override
    protected int send() {
        try {
            int DELAY_TIME = LIMITSECOND;
            long sendTime = System.currentTimeMillis();

            Set<String> cuidSet = userMaps.keySet();
            Map<String,List<String>> sendTargetUsers = new HashMap<String, List<String>>();
            for(String cuid : cuidSet) {
                //타겟 발송대상자 담음.
                sendTargetUsers.put(cuid,userMaps.get(cuid));
                if(sendTargetUsers.size()%MULTIKEY_SIZE==0) {
                    // 발송메세지를 분할하여 파일에 쓴다.
                    //마지막 라인을 작성 후 해당 파일의 정보를 인덱스에 올린다.
                    long delaySendTime = sendTime + (SPLIT_PAGE_CNT * DELAY_TIME * 1000);
                    SPLIT_PAGE_CNT++;
                    writeSplitSendMsgs(sendTargetUsers, delaySendTime);
                }
                TOTAL_SEND_CNT++;
            }
            // 보내지 않고 남은 발송타켓팅 대상자 보낸다.
            if(sendTargetUsers.size()>0){
                // 마지막 남은 발송메세지를 분할하여 파일에 쓴다.
                // 발송메세지를 분할하여 파일에 쓴다.
                //마지막 라인을 작성 후 해당 파일의 정보를 인덱스에 올린다
                long delaySendTime = sendTime + (SPLIT_PAGE_CNT * DELAY_TIME * 1000);
                SPLIT_PAGE_CNT++;
                writeSplitSendMsgs(sendTargetUsers, delaySendTime);
                TOTAL_SEND_CNT++;
            }
        }catch (Exception e){
            e.printStackTrace();
            logger.error("자연발송 실패 : {}",e.toString());
        }

        return TOTAL_SEND_CNT;
    }

    private synchronized void writeSplitSendMsgs(Map<String,List<String>> sendTargetUsers, long delaySendTime) throws Exception{
        boolean isVarReplace = false; //치환처리여부
        //sendTargetUsers 들어간 형식 :  {"AAA":["핸드폰번호","이름"],"BBB":null,"CCC":["핸드폰번호","이름"]...} - 건건이 레디스 발송큐 담기
        if (this.cuidVarMap != null && this.cuidVarMap.size() > 0) {
            isVarReplace = true; //치환처리로직 태움.
        }
        // 발송메세지를 분할하여 파일에 쓴다.
        if(splitSendLogUtil==null){
            splitSendLogUtil = new SplitSendLogUtil(umsSendMsgBean.getUMS_SEQNO() + "", "1");
            logger.debug("[first] SPLIT_PAGE_CNT:" + SPLIT_PAGE_CNT);
        }else{
            //분할발송 그룹 카운트가 바뀌었으므로 바파뀐일로 쓴다
            splitSendLogUtil.changeWriteFile(umsSendMsgBean.getUMS_SEQNO() + "", SPLIT_PAGE_CNT + "");
            logger.debug("[change]SPLIT_PAGE_CNT:" + SPLIT_PAGE_CNT);
        }

        //파일 첫번째 라인에 보낼 시간 셋팅한다
        splitSendLogUtil.write(delaySendTime + "");

        Set<Map.Entry<String,List<String>>> sendTargetEntrySet=sendTargetUsers.entrySet();
        for(Map.Entry<String,List<String>> entry : sendTargetEntrySet){
            // 개인화 메세지 치환처리
            if(isVarReplace) {
                Map<String, String> personalMap = this.cuidVarMap.get(entry.getKey());
                umsSendMsgBean.setREPLACE_VAR_MAP(personalMap);
            }
            UmsSendMsgRedisBean umsSendMsgRedisBean = super.makeUmsSendMsgRedisBean(umsSendMsgBean);
            umsSendMsgRedisBean.setTARGET_CUID(entry.getKey());
            if(entry.getValue()!=null){
                umsSendMsgRedisBean.setTARGET_PHONEINFOS(entry.getValue());
            }
            String jsonUmsSendMsgRedisBean = gson.toJson(umsSendMsgRedisBean);
            splitSendLogUtil.write(jsonUmsSendMsgRedisBean);
//            logger.debug("분할발송 메세지 : "+jsonUmsSendMsgRedisBean);
        }

        //체크쓰레드가 감시하는 메모리에 읽어들일 파일 정보 및 시간 저장함.
        SplitSenderManager.getInstance().putDelayFileInfo(umsSendMsgBean.getUMS_SEQNO() + "", SPLIT_PAGE_CNT + "", delaySendTime);
        sendTargetUsers.clear();
    }

    protected void sendTaskToRedis(String CUID, List<String> HPInfos, String msgVars) throws Exception{
        UmsSendMsgRedisBean umsSendMsgRedisBean = super.makeUmsSendMsgRedisBean(umsSendMsgBean);
        umsSendMsgRedisBean.setTARGET_CUID(CUID);
        if(HPInfos!=null){
            umsSendMsgRedisBean.setTARGET_PHONEINFOS(HPInfos);
        }
        //레디스 발송처리 로그저장
        reqSendLogger.info(umsSendMsgRedisBean.toString());
        RedisPushManager.getInstance().putWork(umsSendMsgRedisBean);
    }

    private void callSendTaskToRedis(Map<String,List<String>> sendTargetUsers)throws Exception{
        boolean isVarReplace = false; //치환처리여부
        //sendTargetUsers 들어간 형식 :  {"AAA":["핸드폰번호","이름"],"BBB":null,"CCC":["핸드폰번호","이름"]...} - 건건이 레디스 발송큐 담기
        if (this.cuidVarMap != null && this.cuidVarMap.size() > 0) {
            isVarReplace = true; //치환처리로직 태움.
        }
        Set<Map.Entry<String,List<String>>> sendTargetEntrySet=sendTargetUsers.entrySet();
        for(Map.Entry<String,List<String>> entry : sendTargetEntrySet){
            // 개인화 메세지 치환처리
            if(isVarReplace) {
                Map<String, String> personalMap = this.cuidVarMap.get(entry.getKey());
                umsSendMsgBean.setREPLACE_VAR_MAP(personalMap);
            }
            sendTaskToRedis(entry.getKey(), entry.getValue(), null);
        }
        sendTargetUsers.clear();
    }

    @Override
    public Map<String, Object> processInfo() throws Exception {
        // Do nothing
        return null;
    }
}
