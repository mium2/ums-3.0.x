package kr.uracle.ums.core.service.send.split;
import com.google.gson.Gson;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.processor.redis.RedisPushManager;
import kr.uracle.ums.codec.redis.message.UmsSendMsgRedisBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Y.B.H(mium2)
 */
public class SplitSenderThread extends Thread {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private Gson gson = new Gson();
    private String ThreadName;
    private SplitSenderManager splitSenderManager;
    private boolean isRun = true;
    private int CHK_SLEEP_TIME = 1;

    public SplitSenderThread(String name, SplitSenderManager _splitSenderManager){
        super(name);
        this.splitSenderManager = _splitSenderManager;
        this.ThreadName=getName();

    }

    @Override
    public void run() {
        this.CHK_SLEEP_TIME = Integer.parseInt(UmsInitListener.webProperties.getProperty("SPLIT_SENDFILE_CHECK_TIME", "1"));
        logger.info("### [SPLIT SEND] Check sleep time:"+CHK_SLEEP_TIME);
        int loopCnt = 0;
        while(isRun){
            try {
                ///push 전송 로직
                UmsSendMsgRedisBean umsSendMsgRedisBean = null;
                //발송시간을 체크하여 발송 시간이 지난 파일들을 발송 처리 한다
                List<String> sendFileNameList= SplitSenderManager.getInstance().getSplitSendFile();
                if(sendFileNameList!=null && sendFileNameList.size()>0){
                    for(String sendFileInfo : sendFileNameList){
                        logger.debug("####### 발송파일 :"+sendFileInfo);
                        String[] sendFileInfoArr = sendFileInfo.split("\\|");
                        // 발송시간이 된 분할 발송 파일들 발송처리

                        putRedisQueue(sendFileInfoArr[0],sendFileInfoArr[1]);
                    }
                }
            } catch (Exception e){
                logger.error("!!!! [SPLIT SEND ERROR] CAUSE : {}",e.toString());
                e.printStackTrace();
            }
        }
    }

    public void putRedisQueue(String seqno, String fileName) throws Exception{
        String mLogDir = splitSenderManager.getmLogDir();
        String logFilePath = mLogDir + File.separatorChar + seqno + File.separator + fileName;

        FileReader fr = null;
        BufferedReader br = null;

        final File f = new File(logFilePath);
        if(f.exists()) {
            try {
                fr = new FileReader(f.getAbsolutePath());
                br = new BufferedReader(fr);
                String str = null;
                int i=0;
                while ((str = br.readLine()) != null) {
                    if(i>0){ //첫번째 라인은 발송일자이므로 패스
                        try {
                            UmsSendMsgRedisBean umsSendMsgRedisBean = gson.fromJson(str, UmsSendMsgRedisBean.class);
                            RedisPushManager.getInstance().putWork(umsSendMsgRedisBean);

                        }catch (Exception e){
                            logger.error("분할발송 JSON 퍄싱에러 : "+ str);
                        }
                    }
                    i++;
                }
            }catch (Exception e){
                e.printStackTrace();
                logger.error("[INFO] INDEX NO LINE: "+e.toString());
            }finally {
                if(br!=null)try{br.close();}catch (IOException e){}
                if(fr!=null)try{fr.close();}catch (IOException e){}
            }
        }
        //발송큐에 담은 파일은 삭제처리 한다.
        splitSenderManager.removeLogFile(seqno, fileName);
        //발송체크 메모리에서도 삭제
        splitSenderManager.removeChkDelayFileInfo(seqno, fileName);

    }
    public boolean isRun() {
        return isRun;
    }

    public void setIsRun(boolean isRun) {
        this.isRun = isRun;
    }

    public void setCHK_SLEEP_TIME(int sleep_time) {
        if(CHK_SLEEP_TIME<sleep_time){
            CHK_SLEEP_TIME=sleep_time;
        }
    }
}

