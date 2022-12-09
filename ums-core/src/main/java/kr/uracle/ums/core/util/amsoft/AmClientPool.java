package kr.uracle.ums.core.util.amsoft;

import io.netty.channel.ChannelHandlerContext;
import kr.uracle.ums.tcppitcher.client.amsoft.AmTcpClient;
import kr.uracle.ums.tcppitcher.client.amsoft.store.session.ConnectInfoBean;
import kr.uracle.ums.tcppitcher.codec.messages.PingReqMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AmClientPool {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private Map<String, Vector<AmTcpClient>> amClientPoolsMap = new ConcurrentHashMap<String,Vector<AmTcpClient>>();
    // 연결된 클라이언트 세션관리맵. 키정보: connectInfoBean.getHostIp()+":"+connectInfoBean.getPort()+"_"+clientNo;
    private Map<String,AmTcpClient> connectedAmClientMap = new HashMap<>();

    // AMSoft TCP서버 연결 정보를 관리하는 맵. 설정파일에 연결정보 정의 되어 있음.
    private Map<String, ConnectInfoBean> amConInfoMap = new HashMap<>();
    private AtomicInteger m_current = new AtomicInteger(0);


    private ThreadPoolTaskScheduler scheduler;

    private String masterServerID = null;
    private String originMasterServerID = null;

    private String AM_SERVER_GROUP="";

    public AmClientPool(String amServerGroup){
        this.AM_SERVER_GROUP = amServerGroup;
    }

    public void init(List<ConnectInfoBean> connectInfoBeanList){
        for(int i=0; i<connectInfoBeanList.size(); i++){
            ConnectInfoBean connectInfoBean = connectInfoBeanList.get(i);
            String serverID = makeServerID(connectInfoBean.getHostIp(), connectInfoBean.getPort());
            if (i == 0) {
                this.originMasterServerID = serverID;
            }
            regWatchTcpHost(connectInfoBean);
            try {
                // TCP Server connect
                AmTcpClient amTcpClient = newConnection(connectInfoBean);
                if(amTcpClient!=null) {
                    Vector<AmTcpClient> pools = new Vector<AmTcpClient>();
                    pools.add(amTcpClient);

                    if (amClientPoolsMap.size() == 0) {
                        this.masterServerID = serverID;
                    }
                    amClientPoolsMap.put(serverID, pools);
                }
            } catch (Exception e) {
                logger.error("["+AM_SERVER_GROUP +"]"+ e.toString());
                throw new RuntimeException(e);
            }
        }

        // ping 발송 및 리컨넥트 로직 수행 스케줄러 구동
        startScheduler();
    }

    /**
     * AMSoft 등록된 TCP 서버 중 연결되어 있지 않는 서버를 주기적으로 감시해야되는 서버로 등록.
     * @param connectInfoBean
     */
    public void regWatchTcpHost(ConnectInfoBean connectInfoBean){
        String amServerId = makeServerID(connectInfoBean.getHostIp(),connectInfoBean.getPort());
        amConInfoMap.put(amServerId,connectInfoBean);
    }

    /**
     * 사용 후 반환된 세션 세션풀에 등록.
     * @param amTcpClient
     */
    public synchronized void freeConnection(AmTcpClient amTcpClient) {
        if(amClientPoolsMap.containsKey(amTcpClient.getHostServerId())) {
            Vector<AmTcpClient> pools = amClientPoolsMap.get(amTcpClient.getHostServerId());
            pools.addElement(amTcpClient);
            amClientPoolsMap.put(amTcpClient.getHostServerId(), pools);
            notifyAll();
        }
    }

    /**
     * 종료된 세션 정리.
     * @param ctx
     */
    public synchronized void removeConnectedTcpClient(ChannelHandlerContext ctx){
        String amServerId = (String)ctx.channel().attr(AmTcpClient.SERVERID).get();
        String amClientId = (String)ctx.channel().attr(AmTcpClient.CLIENTID).get();
        // 세션이 종료된 클라이언트 연결된 세션관리맵에서 정리
        connectedAmClientMap.remove(amClientId);
        if(amClientPoolsMap.containsKey(amServerId)) {
            Vector<AmTcpClient> amTcpClients = amClientPoolsMap.get(amServerId);
            for(AmTcpClient amTcpClient : amTcpClients){
                if(amTcpClient.getClientId().equals(amClientId)){
                    try {
                        amTcpClients.remove(amTcpClient);
                        logger.info("["+AM_SERVER_GROUP +"] 발송가능채널에서  UMS-TCP 서버 : " + amServerId + "에 연결된  " + amClientId + " 제거됨.");
                    }finally {
                        break;
                    }
                }
            }
            amClientPoolsMap.put(amServerId,amTcpClients);
        }
    }

    public AmTcpClient getAmTcpClient(){
        AmTcpClient amTcpClient = null;
        if(masterServerID!=null){
            amTcpClient = getConnection(masterServerID, amConInfoMap.get(masterServerID));
        }
        if(amTcpClient!=null) {
            return amTcpClient; // 마스터서버아이디에 연결된 세션이 있으면 해당 클라이언트세션 리턴
        }else{
            Set<String> amServerIdSet  = amClientPoolsMap.keySet();
            for(String amServerID : amServerIdSet){
                // 마스터서버아이디일 경우 스킵
                if(masterServerID.equals(amServerID)){
                    continue;
                }
                amTcpClient = getConnection(amServerID, amConInfoMap.get(amServerID));
                if(amTcpClient!=null && amTcpClient.isConnected()){
                    masterServerID = amServerID;
                    logger.info("["+AM_SERVER_GROUP+"] 마스터롤 서버 변경됨 : "+masterServerID+" (원조 마스터롤 서버아이디 :"+originMasterServerID+")");
                    break;
                }
            }
        }
        return amTcpClient;
    }
    private synchronized AmTcpClient getConnection(String amServerID, ConnectInfoBean connectInfoBean) {
        AmTcpClient connection = null;
        if(amClientPoolsMap.containsKey(amServerID)){ //해당 서버의 풀이 있는지 확인
            Vector<AmTcpClient> pools = amClientPoolsMap.get(amServerID);
            if(pools==null){ //해당서버의 만들어 진 풀이 없으므로 Vector풀 생성
                pools = new Vector<AmTcpClient>();
                connection = newConnection(connectInfoBean);
                logger.info("["+AM_SERVER_GROUP +"] [NEW AMSoft CONNECTION CREATED]");
                amClientPoolsMap.put(amServerID,pools);
                return connection;
            }else{
                if(pools.size() > 0) { //기존에 연결되어 있는 컨넥션 리턴
                    connection = pools.elementAt(0);
                    pools.removeElementAt(0);
                    try {
                        if (!connection.isConnected()) {  // 연결된 클라이언트가 아닐 경우 다시 가져옴
                            connection = getConnection(amServerID,connectInfoBean);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        connection = getConnection(amServerID,connectInfoBean);
                    }
                    return connection;
                }else{  // 해당서버의 반환된 연결사용할수 있는 컨넥션이 하나도 없으면 새로 하나 생성
                    connection = newConnection(connectInfoBean);
                    return connection;
                }
            }
        }

        Vector<AmTcpClient> pools = new Vector<AmTcpClient>();
        connection = newConnection(connectInfoBean);
        logger.debug("["+AM_SERVER_GROUP +"] amServerID:"+amServerID+"     connection:"+connection);
        amClientPoolsMap.put(amServerID, pools);
        return connection;
    }

    public synchronized void closeAll() {
        Set<Map.Entry<String,AmTcpClient>> s1 = connectedAmClientMap.entrySet();
        int i=0;
        for(Map.Entry<String,AmTcpClient> me : s1){
            String clientID = me.getKey();
            AmTcpClient amTcpClient = me.getValue();
            if(amTcpClient!=null){
                amTcpClient.destroy();
                logger.info("["+AM_SERVER_GROUP +"] AMSoft client ["+clientID+"] destroy completed!");
            }
        }
    }

    private AmTcpClient newConnection(ConnectInfoBean connectInfoBean) {
        AmTcpClient amTcpClient = null;
        try {
            int clientNo = m_current.incrementAndGet();
            String clientID = connectInfoBean.getHostIp()+":"+connectInfoBean.getPort()+"_"+clientNo;
            amTcpClient = new AmTcpClient(new AmCallbackHandler(AM_SERVER_GROUP));
            boolean isConnect = amTcpClient.connect(connectInfoBean.getHostIp(), connectInfoBean.getPort(), clientID);
            if(!isConnect){
                amTcpClient = null;
            }else{
                // 연결이 성공한 클라이언트세션을 종료시 정리할 때 사용하기 위해 저장
                connectedAmClientMap.put(clientID,amTcpClient);
            }
        }catch (Exception e){
            logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            logger.error("! ["+AM_SERVER_GROUP +"] [ERROR]AMSoft TCP 서버연결실패 : "+e.getMessage());
            logger.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//            e.printStackTrace();
        }
        return amTcpClient;
    }

    private String makeServerID(String serverIp, int serverPort){
        return serverIp+":"+serverPort;
    }

    public void stopScheduler() {
        scheduler.shutdown();
        logger.info("["+AM_SERVER_GROUP +"] 핑 메세지 로직 재연결 로직 스케줄 STOP!");
    }
    public void startScheduler() {
        logger.info("##########################################");
        logger.info("# ["+AM_SERVER_GROUP +"]핑 메세지 로직 재연결 로직 startScheduler");
        logger.info("##########################################");
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.initialize();
        // 스케쥴러가 시작되는 부분
        scheduler.schedule(getRunnable(), getTrigger());
    }
    private Runnable getRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                // 핑 요청 및 AMSoft 서버 리컨넥션 로직 구현
                try {
                    if (amConInfoMap.size() > 0) {
                        Set<String> amServerIdSet = amConInfoMap.keySet();

                        for (String amServerId : amServerIdSet) {
                            boolean isReconect = true;
                            if (amClientPoolsMap.containsKey(amServerId)) {
                                Vector<AmTcpClient> amTcpClientVector = amClientPoolsMap.get(amServerId);
                                if(amTcpClientVector==null){
                                    continue;
                                }
                                for (AmTcpClient amTcpClient : amTcpClientVector) {
                                    try {
                                        if(amTcpClient!=null && amTcpClient.isConnected()) {
                                            // 연결되어 있는 세션이 있으면 ping 메세지 발송.
                                            PingReqMessage pingReqMessage = new PingReqMessage();
                                            pingReqMessage.setAckYn("N");
                                            amTcpClient.sendMsg(pingReqMessage);
                                            isReconect = false;
                                        }
                                    } catch (Exception e) {}
                                }
                            }

                            if(isReconect){
                                // AMSoft 서버 셋팅은 되어 있으나 연결된 세션이 하나도 없을 경우 재연결시도.
                                ConnectInfoBean connectInfoBean = amConInfoMap.get(amServerId);
                                AmTcpClient amTcpClient = newConnection(connectInfoBean);
                                if(amTcpClient!=null) {
                                    Vector<AmTcpClient> pools = new Vector<AmTcpClient>();
                                    pools.add(amTcpClient);
                                    amClientPoolsMap.put(amServerId, pools);
                                    logger.info("["+AM_SERVER_GROUP +"] [RECONECTED AMSoft CONNECTION CREATED]");
                                }
                            }
                        }
                        // 현재 마스터서버와 설정값의 마스터서버아이디가 다를 경우 체크하여 원래 마스터아이디에 연결세션된 세션이 있으면 마스터아이디를 원래 마스터아이디로 되돌린다.
                        if(!originMasterServerID.equals(masterServerID)){
                            if(amClientPoolsMap.containsKey(originMasterServerID)){
                                Vector<AmTcpClient> pools = amClientPoolsMap.get(originMasterServerID);
                                if(pools!=null && pools.size()>0){
                                    AmTcpClient amTcpClient = pools.get(0);
                                    if(amTcpClient.isConnected()){
                                        masterServerID = originMasterServerID; //원래 셋팅정보에 셋팅된 마스터아이디로 되돌린다.
                                        logger.info("["+AM_SERVER_GROUP+"] 마스터롤 서버 원조로 복귀됨 : "+masterServerID+" (원조 마스터롤 서버아이디 :"+originMasterServerID+")");
                                    }
                                }
                            }
                        }

                    }
                }catch (Exception e){
                    e.printStackTrace();
                    logger.error("["+AM_SERVER_GROUP +"]"+e.toString());
                }
            }
        };
    }
    private Trigger getTrigger() { // 작업 주기 설정
        PeriodicTrigger periodicTrigger = new PeriodicTrigger(30, TimeUnit.SECONDS);
        periodicTrigger.setInitialDelay(30);
        return periodicTrigger;
    }
}
