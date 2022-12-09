package kr.uracle.ums.core.util.amsoft;

import com.google.gson.Gson;
import kr.uracle.ums.tcppitcher.client.amsoft.AmTcpClient;
import kr.uracle.ums.tcppitcher.codec.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmProcessor {
    private Logger amLogger = LoggerFactory.getLogger("amSoftLogger");
    private Gson gson = new Gson();
    private static final AmProcessor instance = new AmProcessor();

    private AmProcessor(){
    }

    public static AmProcessor getInstance(){
        return instance;
    }

    /**
     * 이곳의 synchronized 삭제하면 여러개의 쓰레드 갯수만큼 클라이언트세션이 생성될 수 있다. AMSoft가 동기서버일 경우 synchronized 삭제처리.
     * @param msg
     */
    public void sendMsg(BaseBodyMessage msg) throws Exception{
        AmTcpClient amTcpClient = null;
        String AM_HOST_ID = null;

        String AM_SERVER_GROUP = "REAL";
        if("B".equals(msg.getTranType())){
            AM_SERVER_GROUP = "BATCH";
        }
        // AMSoft 연결 서버채널 가져오기
        amTcpClient = AmClientPoolMgr.getInstance().getAmTcpClient(AM_SERVER_GROUP);
        if(amTcpClient==null){
            // TODO : 발송가능한 AMSoft 서버가 없을 경우 실패 로직처리
            throw new RuntimeException("연결된 발송가능한 AMSoft TCP 서버가 없습니다.");
        }else {
            amTcpClient.sendMsg(msg);
            AmSendMsgECache.getInstance().put(msg.getMessageId(), msg);
            if(amTcpClient!=null && amTcpClient.isConnected()){
                AmClientPoolMgr.getInstance().freeConnection(amTcpClient, AM_SERVER_GROUP);
            }
            amLogger.info(gson.toJson(msg));
        }
    }
}
