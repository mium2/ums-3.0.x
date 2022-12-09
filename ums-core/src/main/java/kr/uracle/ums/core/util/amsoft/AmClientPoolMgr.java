package kr.uracle.ums.core.util.amsoft;

import io.netty.channel.ChannelHandlerContext;
import kr.uracle.ums.core.util.amsoft.result.ResultSqlMgr;
import kr.uracle.ums.tcppitcher.client.amsoft.AmTcpClient;
import kr.uracle.ums.tcppitcher.client.amsoft.store.session.ConnectInfoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class AmClientPoolMgr {
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private static final AmClientPoolMgr instance = new AmClientPoolMgr();
    private AmSendMsgECache amSendMsgECache;
    private AmClientPool amClientPoolReal = null;
    private AmClientPool amClientPoolBatch = null;

    private boolean isInit = false;

    private AmClientPoolMgr(){}

    public static AmClientPoolMgr getInstance(){
        return instance;
    }

    public void destroy() {
        try {
            stopScheduler();
            //발송요청 실패처리 T_UMS_LOG 테이블에 담는 매니저 중지
            ResultSqlMgr.getInstance().destory();
            AmECacheExpireWorker.getINSTANCE().stopScheduler();
            if(amSendMsgECache!=null) {
                amSendMsgECache.shutdown();
            }

            closeAll();
        }catch (Exception e){}
    }

    public synchronized void init(){
        if(!isInit) {
            isInit = true;
            // 발송메세지 ACK ehcache로 관리
            amSendMsgECache = AmSendMsgECache.getInstance();
            amSendMsgECache.init("classpath:/config/ehcache.xml", true);

            // AM 발송요청 메세지 만료 체크
            AmECacheExpireWorker.getINSTANCE().startScheduler();

            //발송요청 실패처리 T_UMS_LOG 테이블에 담는 매니저 구동
            ResultSqlMgr.getInstance().initailize();
        }
    }

    public void regAmServerGroup(List<ConnectInfoBean> connectInfoBeanList, String amServerGroup){
        if("REAL".equals(amServerGroup)) {
            amClientPoolReal = new AmClientPool("REAL");
            amClientPoolReal.init(connectInfoBeanList);
        }else{
            amClientPoolBatch = new AmClientPool("BATCH");
            amClientPoolBatch.init(connectInfoBeanList);
        }
    }


    /**
     * 사용 후 반환된 세션 세션풀에 등록.
     * @param amTcpClient
     */
    public synchronized void freeConnection(AmTcpClient amTcpClient, String amServerGroup) {
        if("REAL".equals(amServerGroup)){
            amClientPoolReal.freeConnection(amTcpClient);
        }else{
            amClientPoolBatch.freeConnection(amTcpClient);
        }
    }

    /**
     * 종료된 세션 정리.
     * @param ctx
     */
    public synchronized void removeConnectedTcpClient(ChannelHandlerContext ctx, String amServerGroup){
        if("REAL".equals(amServerGroup)){
            amClientPoolReal.removeConnectedTcpClient(ctx);
        }else{
            amClientPoolBatch.removeConnectedTcpClient(ctx);
        }
    }

    public AmTcpClient getAmTcpClient(String amServerGroup){
        AmTcpClient amTcpClient = null;
        if("REAL".equals(amServerGroup)){
            amTcpClient = amClientPoolReal.getAmTcpClient();
        }else{
            amTcpClient = amClientPoolBatch.getAmTcpClient();
        }
        return amTcpClient;
    }

    public synchronized void closeAll() {
        if(amClientPoolReal!=null) {
            amClientPoolReal.closeAll();
        }
        if(amClientPoolBatch!=null) {
            amClientPoolBatch.closeAll();
        }
    }

    public void stopScheduler() {
        if(amClientPoolReal!=null) {
            amClientPoolReal.stopScheduler();
        }
        if(amClientPoolBatch!=null) {
            amClientPoolBatch.stopScheduler();
        }
    }

}
