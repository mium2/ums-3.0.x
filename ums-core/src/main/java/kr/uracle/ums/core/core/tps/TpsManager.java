package kr.uracle.ums.core.core.tps;

import com.google.gson.Gson;
import kr.uracle.ums.core.common.UmsInitListener;
import kr.uracle.ums.core.service.UmsMonitoringService;
import kr.uracle.ums.core.vo.monitor.MonitorServerVo;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Y.B.H(mium2) on 17. 3. 17..
 */
@SuppressWarnings("unchecked")
public class TpsManager {
    private Logger logger = LoggerFactory.getLogger(TpsManager.class);
    private SqlSessionTemplate sqlSessionTemplate;
    private UmsMonitoringService umsMonitoringService;
//    private RedisTemplate redisTemplate;
    private int push_queue_size = 0;
    private int push_max_input_cnt = 0;
    private int push_max_process_cnt = 0;
    private int push_pre_input_cnt = 0;
    private int push_now_input_cnt = 0;
    private int push_pre_process_cnt = 0;
    private int push_now_process_cnt = 0;
    private List<TpsInfoBean> push_tpsHistoryList = new ArrayList<TpsInfoBean>();

    private int wpush_queue_size = 0;
    private int wpush_max_input_cnt = 0;
    private int wpush_max_process_cnt = 0;
    private int wpush_pre_input_cnt = 0;
    private int wpush_now_input_cnt = 0;
    private int wpush_pre_process_cnt = 0;
    private int wpush_now_process_cnt = 0;
    private List<TpsInfoBean> wpush_tpsHistoryList = new ArrayList<TpsInfoBean>();

    private int kkoat_queue_size = 0;
    private int kkoat_max_input_cnt = 0;
    private int kkoat_max_process_cnt = 0;
    private int kkoat_pre_input_cnt = 0;
    private int kkoat_now_input_cnt = 0;
    private int kkoat_pre_process_cnt = 0;
    private int kkoat_now_process_cnt = 0;
    private List<TpsInfoBean> kkoat_tpsHistoryList = new ArrayList<TpsInfoBean>();

    private int kkoft_queue_size = 0;
    private int kkoft_max_input_cnt = 0;
    private int kkoft_max_process_cnt = 0;
    private int kkoft_pre_input_cnt = 0;
    private int kkoft_now_input_cnt = 0;
    private int kkoft_pre_process_cnt = 0;
    private int kkoft_now_process_cnt = 0;
    private List<TpsInfoBean> kkoft_tpsHistoryList = new ArrayList<TpsInfoBean>();

    private int sms_queue_size = 0;
    private int sms_max_input_cnt = 0;
    private int sms_max_process_cnt = 0;
    private int sms_pre_input_cnt = 0;
    private int sms_now_input_cnt = 0;
    private int sms_pre_process_cnt = 0;
    private int sms_now_process_cnt = 0;
    private List<TpsInfoBean> sms_tpsHistoryList = new ArrayList<TpsInfoBean>();

    private int rcs_queue_size = 0;
    private int rcs_max_input_cnt = 0;
    private int rcs_max_process_cnt = 0;
    private int rcs_pre_input_cnt = 0;
    private int rcs_now_input_cnt = 0;
    private int rcs_pre_process_cnt = 0;
    private int rcs_now_process_cnt = 0;
    private List<TpsInfoBean> rcs_tpsHistoryList = new ArrayList<TpsInfoBean>();

    private int naver_queue_size = 0;
    private int naver_max_input_cnt = 0;
    private int naver_max_process_cnt = 0;
    private int naver_pre_input_cnt = 0;
    private int naver_now_input_cnt = 0;
    private int naver_pre_process_cnt = 0;
    private int naver_now_process_cnt = 0;
    private List<TpsInfoBean> naver_tpsHistoryList = new ArrayList<TpsInfoBean>();

    private String chek_date = "";
    private static TpsManager instance = null;
    private Thread tpsThread;
    protected boolean running = true;

    private String UMSID = "UMS";
    private String SELF_UMS_URL = "";

    private Gson gson = new Gson();
    private ThreadPoolTaskScheduler scheduler;

    public enum TPSSERVERKIND {PUSH,WPUSH,KKOALT,KKOFRT,SMS,RCS,NAVER};

    private TpsManager(){
        this.UMSID = System.getProperty("SERVERID");
        String SERVERNAME = UmsInitListener.webProperties.getProperty("UMS.ID","UMS");
        this.SELF_UMS_URL = UmsInitListener.webProperties.getProperty("UMS.SELF.URL","");
        if(!this.SELF_UMS_URL.endsWith("/")){
            this.SELF_UMS_URL = this.SELF_UMS_URL+"/";
        }
        this.SELF_UMS_URL = this.SELF_UMS_URL + "getSystemInfo.ums";
        this.sqlSessionTemplate = (SqlSessionTemplate)UmsInitListener.wContext.getBean("sqlSessionTemplate");
        this.umsMonitoringService = UmsInitListener.wContext.getBean(UmsMonitoringService.class);

        MonitorServerVo monitorServerVo = new MonitorServerVo();
        monitorServerVo.setSERVERID(UMSID);
        monitorServerVo.setSERVERNAME(SERVERNAME);
        monitorServerVo.setGROUPID("UMS");
        monitorServerVo.setSERVERTYPE("UMS");
        monitorServerVo.setMONITOR_URL(SELF_UMS_URL);
        int checkCnt = this.sqlSessionTemplate.selectOne("mybatis.monitor.selUmsServerInfo", monitorServerVo);
        if(checkCnt==0) {
            this.sqlSessionTemplate.insert("mybatis.monitor.inUmsServerInfo", monitorServerVo);
        }else {
            this.sqlSessionTemplate.insert("mybatis.monitor.upUmsServerInfo", monitorServerVo);
        }

        startScheduler();
    }

    public static TpsManager getInstance() {
        if(instance==null){
            instance = new TpsManager();
        }
        return instance;
    }

    public int getPre_input_cnt(TPSSERVERKIND tpsserverkind){
        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            return kkoat_pre_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            return kkoft_pre_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            return sms_pre_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            return rcs_pre_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            return naver_pre_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            return wpush_pre_input_cnt;
        }else{
            return push_pre_input_cnt;
        }
    }

    public int getPre_process_cnt(TPSSERVERKIND tpsserverkind) {
        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            return kkoat_pre_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            return kkoft_pre_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            return sms_pre_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            return rcs_pre_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            return naver_pre_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            return wpush_pre_process_cnt;
        }else{
            return push_pre_process_cnt;
        }
    }

    public int getQueue_size(TPSSERVERKIND tpsserverkind) {
        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            return kkoat_queue_size;
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            return kkoft_queue_size;
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            return sms_queue_size;
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            return rcs_queue_size;
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            return naver_queue_size;
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            return wpush_queue_size;
        }else{
            return push_queue_size;
        }
    }

    public String getChek_date() {
        return chek_date;
    }

    public synchronized void addInputCnt(TPSSERVERKIND tpsserverkind) {
        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            this.kkoat_now_input_cnt = kkoat_now_input_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            this.kkoft_now_input_cnt = kkoft_now_input_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            this.sms_now_input_cnt = sms_now_input_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            this.rcs_now_input_cnt = rcs_now_input_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            this.naver_now_input_cnt = naver_now_input_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            this.wpush_now_input_cnt = wpush_now_input_cnt+1;
        }else{
            this.push_now_input_cnt = push_now_input_cnt+1;
        }

    }

    public synchronized void addProcessCnt(TPSSERVERKIND tpsserverkind) {
        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            this.kkoat_now_process_cnt = kkoat_now_process_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            this.kkoft_now_process_cnt = kkoft_now_process_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            this.sms_now_process_cnt = sms_now_process_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            this.rcs_now_process_cnt = rcs_now_process_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            this.naver_now_process_cnt = naver_now_process_cnt+1;
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            this.wpush_now_process_cnt = wpush_now_process_cnt+1;
        }else{
            this.push_now_process_cnt = push_now_process_cnt+1;
        }
    }

    public synchronized void addInputCnt(TPSSERVERKIND tpsserverkind, int addCnt) {
        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            this.kkoat_now_input_cnt = kkoat_now_input_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            this.kkoft_now_input_cnt = kkoft_now_input_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            this.sms_now_input_cnt = sms_now_input_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            this.rcs_now_input_cnt = rcs_now_input_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            this.naver_now_input_cnt = naver_now_input_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            this.wpush_now_input_cnt = wpush_now_input_cnt+addCnt;
        }else{
            this.push_now_input_cnt = push_now_input_cnt+addCnt;
        }

    }

    public synchronized void addProcessCnt(TPSSERVERKIND tpsserverkind, int addCnt) {
        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            this.kkoat_now_process_cnt = kkoat_now_process_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            this.kkoft_now_process_cnt = kkoft_now_process_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            this.sms_now_process_cnt = sms_now_process_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            this.rcs_now_process_cnt = rcs_now_process_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            this.naver_now_process_cnt = naver_now_process_cnt+addCnt;
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            this.wpush_now_process_cnt = wpush_now_process_cnt+addCnt;
        }else{
            this.push_now_process_cnt = push_now_process_cnt+addCnt;
        }
    }

    protected synchronized void tpsCountReset(){
        // push 처리 TPS를 알기위해 처리
        /////////////////////////////////////////////////////////////
        if (push_max_input_cnt < push_now_input_cnt) {
            push_max_input_cnt = push_now_input_cnt;
        }
        push_pre_input_cnt = push_now_input_cnt;
        push_now_input_cnt = 0;
        // 최대 TPS를 알기 위해
        if (push_max_process_cnt < push_now_process_cnt) {
            push_max_process_cnt = push_now_process_cnt;
        }
        push_pre_process_cnt = push_now_process_cnt;
        push_now_process_cnt = 0;
        // 웹push 처리 TPS를 알기위해 처리
        /////////////////////////////////////////////////////////////
        if (wpush_max_input_cnt < wpush_now_input_cnt) {
            wpush_max_input_cnt = wpush_now_input_cnt;
        }
        wpush_pre_input_cnt = wpush_now_input_cnt;
        wpush_now_input_cnt = 0;
        // 최대 TPS를 알기 위해
        if (wpush_max_process_cnt < wpush_now_process_cnt) {
            wpush_max_process_cnt = wpush_now_process_cnt;
        }
        wpush_pre_process_cnt = wpush_now_process_cnt;
        wpush_now_process_cnt = 0;
        /////////////////////////////////////////////////////////////
        // 카카오알림톡 처리 TPS를 알기위해 처리
        /////////////////////////////////////////////////////////////
        if (kkoat_max_input_cnt < kkoat_now_input_cnt) {
            kkoat_max_input_cnt = kkoat_now_input_cnt;
        }
        kkoat_pre_input_cnt = kkoat_now_input_cnt;
        kkoat_now_input_cnt = 0;
        // 최대 TPS를 알기 위해
        if (kkoat_max_process_cnt < kkoat_now_process_cnt) {
            kkoat_max_process_cnt = kkoat_now_process_cnt;
        }
        kkoat_pre_process_cnt = kkoat_now_process_cnt;
        kkoat_now_process_cnt = 0;
        /////////////////////////////////////////////////////////////
        // 카카오친구톡 처리 TPS를 알기위해 처리
        /////////////////////////////////////////////////////////////
        if (kkoft_max_input_cnt < kkoft_now_input_cnt) {
            kkoft_max_input_cnt = kkoft_now_input_cnt;
        }
        kkoft_pre_input_cnt = kkoft_now_input_cnt;
        kkoft_now_input_cnt = 0;
        // 최대 TPS를 알기 위해
        if (kkoft_max_process_cnt < kkoft_now_process_cnt) {
            kkoft_max_process_cnt = kkoft_now_process_cnt;
        }
        kkoft_pre_process_cnt = kkoft_now_process_cnt;
        kkoft_now_process_cnt = 0;
        /////////////////////////////////////////////////////////////
        // sms 처리 TPS를 알기위해 처리
        /////////////////////////////////////////////////////////////
        if (sms_max_input_cnt < sms_now_input_cnt) {
            sms_max_input_cnt = sms_now_input_cnt;
        }
        sms_pre_input_cnt = sms_now_input_cnt;
        sms_now_input_cnt = 0;
        // 최대 TPS를 알기 위해
        if (sms_max_process_cnt < sms_now_process_cnt) {
            sms_max_process_cnt = sms_now_process_cnt;
        }
        sms_pre_process_cnt = sms_now_process_cnt;
        sms_now_process_cnt = 0;
        /////////////////////////////////////////////////////////////
        // RCS 처리 TPS를 알기위해 처리
        /////////////////////////////////////////////////////////////
        if (rcs_max_input_cnt < rcs_now_input_cnt) {
            rcs_max_input_cnt = rcs_now_input_cnt;
        }
        rcs_pre_input_cnt = rcs_now_input_cnt;
        rcs_now_input_cnt = 0;
        // 최대 TPS를 알기 위해
        if (rcs_max_process_cnt < rcs_now_process_cnt) {
            rcs_max_process_cnt = rcs_now_process_cnt;
        }
        rcs_pre_process_cnt = rcs_now_process_cnt;
        rcs_now_process_cnt = 0;
        /////////////////////////////////////////////////////////////
        // NAVER 처리 TPS를 알기위해 처리
        /////////////////////////////////////////////////////////////
        if (naver_max_input_cnt < naver_now_input_cnt) {
            naver_max_input_cnt = naver_now_input_cnt;
        }
        naver_pre_input_cnt = naver_now_input_cnt;
        naver_now_input_cnt = 0;
        // 최대 TPS를 알기 위해
        if (naver_max_process_cnt < naver_now_process_cnt) {
            naver_max_process_cnt = naver_now_process_cnt;
        }
        naver_pre_process_cnt = naver_now_process_cnt;
        naver_now_process_cnt = 0;
        /////////////////////////////////////////////////////////////
        // 현재시간 셋팅
        SimpleDateFormat formatter=new SimpleDateFormat("yyyyMMddHHmmss");
        chek_date = formatter.format(new java.util.Date());
    }

    public int getMax_input_cnt(TPSSERVERKIND tpsserverkind) {
        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            return kkoat_max_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            return kkoft_max_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            return sms_max_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            return rcs_max_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            return naver_max_input_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            return wpush_max_input_cnt;
        }else{
            return push_max_input_cnt;
        }
    }

    public long getMax_process_cnt(TPSSERVERKIND tpsserverkind) {
        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            return kkoat_max_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            return kkoft_max_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            return sms_max_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            return rcs_max_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            return naver_max_process_cnt;
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            return wpush_max_process_cnt;
        }else{
            return push_max_process_cnt;
        }
    }

    protected void putTpsInfo(TPSSERVERKIND tpsserverkind, TpsInfoBean tpsInfoBean){

        if(tpsserverkind== TPSSERVERKIND.KKOALT) {
            if (kkoat_tpsHistoryList.size() >= 30) {
                kkoat_tpsHistoryList.remove(0);
            }
            kkoat_tpsHistoryList.add(tpsInfoBean);
        }else if(tpsserverkind== TPSSERVERKIND.KKOFRT) {
            if (kkoft_tpsHistoryList.size() >= 30) {
                kkoft_tpsHistoryList.remove(0);
            }
            kkoft_tpsHistoryList.add(tpsInfoBean);
        }else if(tpsserverkind== TPSSERVERKIND.SMS){
            if (sms_tpsHistoryList.size() >= 30) {
                sms_tpsHistoryList.remove(0);
            }
            sms_tpsHistoryList.add(tpsInfoBean);
        }else if(tpsserverkind== TPSSERVERKIND.RCS){
            if (rcs_tpsHistoryList.size() >= 30) {
                rcs_tpsHistoryList.remove(0);
            }
            rcs_tpsHistoryList.add(tpsInfoBean);
        }else if(tpsserverkind== TPSSERVERKIND.NAVER){
            if (naver_tpsHistoryList.size() >= 30) {
                naver_tpsHistoryList.remove(0);
            }
            naver_tpsHistoryList.add(tpsInfoBean);
        }else if(tpsserverkind== TPSSERVERKIND.WPUSH){
            if (wpush_tpsHistoryList.size() >= 30) {
                wpush_tpsHistoryList.remove(0);
            }
            wpush_tpsHistoryList.add(tpsInfoBean);
        }else{
            if (push_tpsHistoryList.size() >= 30) {
                push_tpsHistoryList.remove(0);
            }
            push_tpsHistoryList.add(tpsInfoBean);
        }
    }

    public List<TpsInfoBean> getPush_tpsHistoryList() {
        return push_tpsHistoryList;
    }
    public List<TpsInfoBean> getKkoat_tpsHistoryList() {
        return kkoat_tpsHistoryList;
    }
    public List<TpsInfoBean> getKkoft_tpsHistoryList() {
        return kkoft_tpsHistoryList;
    }
    public List<TpsInfoBean> getSms_tpsHistoryList() {
        return sms_tpsHistoryList;
    }
    public List<TpsInfoBean> getRcs_tpsHistoryList() {
        return rcs_tpsHistoryList;
    }
    public List<TpsInfoBean> getNaver_tpsHistoryList() {
        return naver_tpsHistoryList;
    }
    public List<TpsInfoBean> getWPUSH_tpsHistoryList() {
        return wpush_tpsHistoryList;
    }

    // 모니터링 정보 리턴
    public Map<String,Object> getSummaryData(){
        Map<String,Object> rootSummaryMap = new HashMap<>();
        Map<String,String> pushSummaryMap = new HashMap<>();
        pushSummaryMap.put("INPUT",""+push_now_input_cnt);
        pushSummaryMap.put("OUTPUT",""+push_now_process_cnt);
        pushSummaryMap.put("MAX_INPUT",""+push_max_input_cnt);
        pushSummaryMap.put("MAX_OUTPUT",""+push_max_process_cnt);
        pushSummaryMap.put("CHKDATE", chek_date);

        Map<String,String> wpushSummaryMap = new HashMap<>();
        wpushSummaryMap.put("INPUT",""+wpush_now_input_cnt);
        wpushSummaryMap.put("OUTPUT",""+wpush_now_process_cnt);
        wpushSummaryMap.put("MAX_INPUT",""+wpush_max_input_cnt);
        wpushSummaryMap.put("MAX_OUTPUT",""+wpush_max_process_cnt);
        wpushSummaryMap.put("CHKDATE", chek_date);

        Map<String,String> kkoatSummaryMap = new HashMap<>();
        kkoatSummaryMap.put("INPUT",""+kkoat_now_input_cnt);
        kkoatSummaryMap.put("OUTPUT",""+kkoat_now_process_cnt);
        kkoatSummaryMap.put("MAX_INPUT",""+kkoat_max_input_cnt);
        kkoatSummaryMap.put("MAX_OUTPUT",""+kkoat_max_process_cnt);
        kkoatSummaryMap.put("CHKDATE", chek_date);

        Map<String,String> kkoftSummaryMap = new HashMap<>();
        kkoftSummaryMap.put("INPUT",""+kkoft_now_input_cnt);
        kkoftSummaryMap.put("OUTPUT",""+kkoft_now_process_cnt);
        kkoftSummaryMap.put("MAX_INPUT",""+kkoft_max_input_cnt);
        kkoftSummaryMap.put("MAX_OUTPUT",""+kkoft_max_process_cnt);
        kkoftSummaryMap.put("CHKDATE", chek_date);

        Map<String,String> rcsSummaryMap = new HashMap<>();
        rcsSummaryMap.put("INPUT",""+rcs_now_input_cnt);
        rcsSummaryMap.put("OUTPUT",""+rcs_now_process_cnt);
        rcsSummaryMap.put("MAX_INPUT",""+rcs_max_input_cnt);
        rcsSummaryMap.put("MAX_OUTPUT",""+rcs_max_process_cnt);
        rcsSummaryMap.put("CHKDATE", chek_date);

        Map<String,String> naverSummaryMap = new HashMap<>();
        naverSummaryMap.put("INPUT",""+naver_now_input_cnt);
        naverSummaryMap.put("OUTPUT",""+naver_now_process_cnt);
        naverSummaryMap.put("MAX_INPUT",""+naver_max_input_cnt);
        naverSummaryMap.put("MAX_OUTPUT",""+naver_max_process_cnt);
        naverSummaryMap.put("CHKDATE", chek_date);
        
        Map<String,String> smsSummaryMap = new HashMap<>();
        smsSummaryMap.put("INPUT",""+sms_now_input_cnt);
        smsSummaryMap.put("OUTPUT",""+sms_now_process_cnt);
        smsSummaryMap.put("MAX_INPUT",""+sms_max_input_cnt);
        smsSummaryMap.put("MAX_OUTPUT",""+sms_max_process_cnt);
        smsSummaryMap.put("CHKDATE", chek_date);

        rootSummaryMap.put("PUSH", pushSummaryMap);
        rootSummaryMap.put("WPUSH", wpushSummaryMap);
        rootSummaryMap.put("KKOAT", kkoatSummaryMap);
        rootSummaryMap.put("KKOFT", kkoftSummaryMap);
        rootSummaryMap.put("RCS", rcsSummaryMap);
        rootSummaryMap.put("NAVER", naverSummaryMap);
        rootSummaryMap.put("SMS", smsSummaryMap);

        return rootSummaryMap;
    }
    // 발송현황차트 정보리턴
    public Map<String,Object> getLineChartDatas(){
        SimpleDateFormat formatter=new SimpleDateFormat("HH:mm");
        String nowDateStr = formatter.format(new java.util.Date());

        Map<String,Object> chartDataMap = new HashMap<String,Object>();

        //////////////////////////////////////////////////////////////////////
        // 푸시
        //////////////////////////////////////////////////////////////////////
        List<String> pushInputDatas = new ArrayList<String>();
        List<String> pushOutputDatas = new ArrayList<String>();
        List<String> pushLabelDatas = new ArrayList<String>();
        for(TpsInfoBean tpsInfoBean : push_tpsHistoryList){
            pushInputDatas.add(tpsInfoBean.getINPUT_CNT());
            pushOutputDatas.add(tpsInfoBean.getOUT_CNT());
            pushLabelDatas.add(tpsInfoBean.getCHECK_DATE().substring(8,10)+":"+tpsInfoBean.getCHECK_DATE().substring(10,12));
        }

        //조회한 싯점의 실시간 푸시발송카운트 등록
        pushInputDatas.add(""+push_now_input_cnt);
        pushOutputDatas.add(""+push_now_process_cnt);
        pushLabelDatas.add(nowDateStr);

        List<Map<String,List>> pushDataList = new ArrayList<>();
        Map<String,List> pushInput = new HashMap<>();
        Map<String,List> pushOutput = new HashMap<>();
        Map<String,List> pushLabel = new HashMap<>();
        pushInput.put("INPUT",pushInputDatas);
        pushOutput.put("OUTPUT",pushOutputDatas);
        pushLabel.put("LABEL",pushLabelDatas);

        pushDataList.add(pushInput);
        pushDataList.add(pushOutput);
        pushDataList.add(pushLabel);
        chartDataMap.put("PUSH",pushDataList);

        //////////////////////////////////////////////////////////////////////
        // 웹푸시
        //////////////////////////////////////////////////////////////////////
        List<String> wpushInputDatas = new ArrayList<String>();
        List<String> wpushOutputDatas = new ArrayList<String>();
        List<String> wpushLabelDatas = new ArrayList<String>();
        for(TpsInfoBean tpsInfoBean : wpush_tpsHistoryList){
            wpushInputDatas.add(tpsInfoBean.getINPUT_CNT());
            wpushOutputDatas.add(tpsInfoBean.getOUT_CNT());
            wpushLabelDatas.add(tpsInfoBean.getCHECK_DATE().substring(8,10)+":"+tpsInfoBean.getCHECK_DATE().substring(10,12));
        }
        //조회한 싯점의 실시간 푸시발송카운트 등록
        wpushInputDatas.add(""+wpush_now_input_cnt);
        wpushOutputDatas.add(""+wpush_now_process_cnt);
        wpushLabelDatas.add(nowDateStr);


        List<Map<String,List>> wpushDataList = new ArrayList<>();
        Map<String,List> wpushInput = new HashMap<>();
        Map<String,List> wpushOutput = new HashMap<>();
        Map<String,List> wpushLabel = new HashMap<>();
        pushInput.put("INPUT",wpushInputDatas);
        pushOutput.put("OUTPUT",wpushOutputDatas);
        pushLabel.put("LABEL",wpushLabelDatas);

        wpushDataList.add(wpushInput);
        wpushDataList.add(wpushOutput);
        wpushDataList.add(wpushLabel);
        chartDataMap.put("WPUSH",wpushDataList);

        //////////////////////////////////////////////////////////////////////
        // 알림톡
        //////////////////////////////////////////////////////////////////////
        List<String> kkoatInputDatas = new ArrayList<String>();
        List<String> kkoatOutputDatas = new ArrayList<String>();
        List<String> kkoatLabelDatas = new ArrayList<String>();
        for(TpsInfoBean tpsInfoBean : kkoat_tpsHistoryList){
            kkoatInputDatas.add(tpsInfoBean.getINPUT_CNT());
            kkoatOutputDatas.add(tpsInfoBean.getOUT_CNT());
            kkoatLabelDatas.add(tpsInfoBean.getCHECK_DATE().substring(8,10)+":"+tpsInfoBean.getCHECK_DATE().substring(10,12));
        }
        //조회한 싯점의 실시간 알림톡발송카운트 등록
        kkoatInputDatas.add(""+kkoat_now_input_cnt);
        kkoatOutputDatas.add(""+kkoat_now_process_cnt);
        kkoatLabelDatas.add(nowDateStr);

        List<Map<String,List>> kkoatDataList = new ArrayList<>();
        Map<String,List> kkoatInput = new HashMap<>();
        Map<String,List> kkoatOutput = new HashMap<>();
        Map<String,List> kkoatLabel = new HashMap<>();
        kkoatInput.put("INPUT",kkoatInputDatas);
        kkoatOutput.put("OUTPUT",kkoatOutputDatas);
        kkoatLabel.put("LABEL",kkoatLabelDatas);

        kkoatDataList.add(kkoatInput);
        kkoatDataList.add(kkoatOutput);
        kkoatDataList.add(kkoatLabel);
        chartDataMap.put("KKOALT",kkoatDataList);

        //////////////////////////////////////////////////////////////////////
        // 친구톡
        //////////////////////////////////////////////////////////////////////
        List<String> kkoftInputDatas = new ArrayList<String>();
        List<String> kkoftOutputDatas = new ArrayList<String>();
        List<String> kkoftLabelDatas = new ArrayList<String>();
        for(TpsInfoBean tpsInfoBean : kkoft_tpsHistoryList){
            kkoftInputDatas.add(tpsInfoBean.getINPUT_CNT());
            kkoftOutputDatas.add(tpsInfoBean.getOUT_CNT());
            kkoftLabelDatas.add(tpsInfoBean.getCHECK_DATE().substring(8,10)+":"+tpsInfoBean.getCHECK_DATE().substring(10,12));
        }
        //조회한 싯점의 실시간 친구톡발송카운트 등록
        kkoftInputDatas.add(""+kkoft_now_input_cnt);
        kkoftOutputDatas.add(""+kkoft_now_process_cnt);
        kkoftLabelDatas.add(nowDateStr);

        List<Map<String,List>> kkoftDataList = new ArrayList<>();
        Map<String,List> kkoftInput = new HashMap<>();
        Map<String,List> kkoftOutput = new HashMap<>();
        Map<String,List> kkoftLabel = new HashMap<>();
        kkoftInput.put("INPUT",kkoftInputDatas);
        kkoftOutput.put("OUTPUT",kkoftOutputDatas);
        kkoftLabel.put("LABEL",kkoftLabelDatas);

        kkoftDataList.add(kkoftInput);
        kkoftDataList.add(kkoftOutput);
        kkoftDataList.add(kkoftLabel);
        chartDataMap.put("KKOFRT",kkoftDataList);

        //////////////////////////////////////////////////////////////////////
        // SMS
        //////////////////////////////////////////////////////////////////////
        List<String> smsInputDatas = new ArrayList<String>();
        List<String> smsOutputDatas = new ArrayList<String>();
        List<String> smsLabelDatas = new ArrayList<String>();
        for(TpsInfoBean tpsInfoBean : sms_tpsHistoryList){
            smsInputDatas.add(tpsInfoBean.getINPUT_CNT());
            smsOutputDatas.add(tpsInfoBean.getOUT_CNT());
            smsLabelDatas.add(tpsInfoBean.getCHECK_DATE().substring(8,10)+":"+tpsInfoBean.getCHECK_DATE().substring(10,12));
        }
        //조회한 싯점의 실시간 SMS발송카운트 등록
        smsInputDatas.add(""+sms_now_input_cnt);
        smsOutputDatas.add(""+sms_now_process_cnt);
        smsLabelDatas.add(nowDateStr);

        List<Map<String,List>> smsDataList = new ArrayList<>();
        Map<String,List> smsInput = new HashMap<>();
        Map<String,List> smsOutput = new HashMap<>();
        Map<String,List> smsLabel = new HashMap<>();
        smsInput.put("INPUT",smsInputDatas);
        smsOutput.put("OUTPUT",smsOutputDatas);
        smsLabel.put("LABEL",smsLabelDatas);

        smsDataList.add(smsInput);
        smsDataList.add(smsOutput);
        smsDataList.add(smsLabel);
        chartDataMap.put("SMS",smsDataList);

        //////////////////////////////////////////////////////////////////////
        // RCS
        //////////////////////////////////////////////////////////////////////
        List<String> rcsInputDatas = new ArrayList<String>();
        List<String> rcsOutputDatas = new ArrayList<String>();
        List<String> rcsLabelDatas = new ArrayList<String>();
        for(TpsInfoBean tpsInfoBean : rcs_tpsHistoryList){
            rcsInputDatas.add(tpsInfoBean.getINPUT_CNT());
            rcsOutputDatas.add(tpsInfoBean.getOUT_CNT());
            rcsLabelDatas.add(tpsInfoBean.getCHECK_DATE().substring(8,10)+":"+tpsInfoBean.getCHECK_DATE().substring(10,12));
        }
        //조회한 싯점의 실시간 RCS발송카운트 등록
        rcsInputDatas.add(""+rcs_now_input_cnt);
        rcsOutputDatas.add(""+rcs_now_process_cnt);
        rcsLabelDatas.add(nowDateStr);

        List<Map<String,List>> rcsDataList = new ArrayList<>();
        Map<String,List> rcsInput = new HashMap<>();
        Map<String,List> rcsOutput = new HashMap<>();
        Map<String,List> rcsLabel = new HashMap<>();
        rcsInput.put("INPUT",rcsInputDatas);
        rcsOutput.put("OUTPUT",rcsOutputDatas);
        rcsLabel.put("LABEL",rcsLabelDatas);

        rcsDataList.add(rcsInput);
        rcsDataList.add(rcsOutput);
        rcsDataList.add(rcsLabel);
        chartDataMap.put("RCS",rcsDataList);

        //////////////////////////////////////////////////////////////////////
        // NAVER
        //////////////////////////////////////////////////////////////////////
        List<String> naverInputDatas = new ArrayList<String>();
        List<String> naverOutputDatas = new ArrayList<String>();
        List<String> naverLabelDatas = new ArrayList<String>();
        for(TpsInfoBean tpsInfoBean : naver_tpsHistoryList){
            naverInputDatas.add(tpsInfoBean.getINPUT_CNT());
            naverOutputDatas.add(tpsInfoBean.getOUT_CNT());
            naverLabelDatas.add(tpsInfoBean.getCHECK_DATE().substring(8,10)+":"+tpsInfoBean.getCHECK_DATE().substring(10,12));
        }
        //조회한 싯점의 실시간 NAVER발송카운트 등록
        naverInputDatas.add(""+naver_now_input_cnt);
        naverOutputDatas.add(""+naver_now_process_cnt);
        naverLabelDatas.add(nowDateStr);

        List<Map<String,List>> naverDataList = new ArrayList<>();
        Map<String,List> naverInput = new HashMap<>();
        Map<String,List> naverOutput = new HashMap<>();
        Map<String,List> naverLabel = new HashMap<>();
        naverInput.put("INPUT",naverInputDatas);
        naverOutput.put("OUTPUT",naverOutputDatas);
        naverLabel.put("LABEL",naverLabelDatas);

        naverDataList.add(naverInput);
        naverDataList.add(naverOutput);
        naverDataList.add(naverLabel);
        chartDataMap.put("NAVER",naverDataList);

        return chartDataMap;
    }

    public void startScheduler() {
        logger.info("### TPS MANAGER 스케줄 START!");
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        // 스케쥴러가 시작되는 부분
        scheduler.schedule(getRunnable(), getTrigger());
    }
    public void stopScheduler() {
        scheduler.shutdown();
        logger.info("### TPS MANAGER 스케줄 STOP!");
    }

    private Trigger getTrigger() {
        // 작업 주기 설정
        return new PeriodicTrigger(60, TimeUnit.SECONDS);
    }

    private Runnable getRunnable(){
        return new Runnable(){
            @Override
            public void run() {

                tpsCountReset();

                // 푸시 카운트 메모리에 저장
                TpsInfoBean  pushTpsInfoBean = new TpsInfoBean();
                pushTpsInfoBean.setSERVER_KIND(TPSSERVERKIND.PUSH.toString());
                pushTpsInfoBean.setINPUT_CNT("" + push_pre_input_cnt);
                pushTpsInfoBean.setOUT_CNT("" + push_pre_process_cnt);
                pushTpsInfoBean.setMAX_INPUT_CNT("" + push_max_input_cnt);
                pushTpsInfoBean.setMAX_OUTPUT_CNT("" + push_max_process_cnt);
                pushTpsInfoBean.setQUEUE_SIZE("" + push_queue_size);
                pushTpsInfoBean.setCHECK_DATE(chek_date);
                putTpsInfo(TPSSERVERKIND.PUSH,pushTpsInfoBean);

                // 카카오알림톡 카운트 메모리에 저장
                TpsInfoBean kkoatTpsInfoBean = new TpsInfoBean();
                kkoatTpsInfoBean.setSERVER_KIND(TPSSERVERKIND.KKOALT.toString());
                kkoatTpsInfoBean.setINPUT_CNT("" + kkoat_pre_input_cnt);
                kkoatTpsInfoBean.setOUT_CNT("" + kkoat_pre_process_cnt);
                kkoatTpsInfoBean.setMAX_INPUT_CNT("" + kkoat_max_input_cnt);
                kkoatTpsInfoBean.setMAX_OUTPUT_CNT("" + kkoat_max_process_cnt);
                kkoatTpsInfoBean.setQUEUE_SIZE("" + kkoat_queue_size);
                kkoatTpsInfoBean.setCHECK_DATE(chek_date);
                putTpsInfo(TPSSERVERKIND.KKOALT,kkoatTpsInfoBean);

                // 카카오친구톡 카운트 메모리에 저장
                TpsInfoBean kkoftTpsInfoBean = new TpsInfoBean();
                kkoftTpsInfoBean.setSERVER_KIND(TPSSERVERKIND.KKOFRT.toString());
                kkoftTpsInfoBean.setINPUT_CNT("" + kkoft_pre_input_cnt);
                kkoftTpsInfoBean.setOUT_CNT("" + kkoft_pre_process_cnt);
                kkoftTpsInfoBean.setMAX_INPUT_CNT("" + kkoft_max_input_cnt);
                kkoftTpsInfoBean.setMAX_OUTPUT_CNT("" + kkoft_max_process_cnt);
                kkoftTpsInfoBean.setQUEUE_SIZE("" + kkoft_queue_size);
                kkoftTpsInfoBean.setCHECK_DATE(chek_date);
                putTpsInfo(TPSSERVERKIND.KKOFRT,kkoftTpsInfoBean);

                // SMS 카운트 메모리에 저장
                TpsInfoBean smsTpsInfoBean = new TpsInfoBean();
                smsTpsInfoBean.setSERVER_KIND(TPSSERVERKIND.SMS.toString());
                smsTpsInfoBean.setINPUT_CNT("" + sms_pre_input_cnt);
                smsTpsInfoBean.setOUT_CNT("" + sms_pre_process_cnt);
                smsTpsInfoBean.setMAX_INPUT_CNT("" + sms_max_input_cnt);
                smsTpsInfoBean.setMAX_OUTPUT_CNT("" + sms_max_process_cnt);
                smsTpsInfoBean.setQUEUE_SIZE("" + sms_queue_size);
                smsTpsInfoBean.setCHECK_DATE(chek_date);
                putTpsInfo(TPSSERVERKIND.SMS,smsTpsInfoBean);

                // 웹푸시 카운트 메모리에 저장
                TpsInfoBean wpushTpsInfoBean = new TpsInfoBean();
                wpushTpsInfoBean.setSERVER_KIND(TPSSERVERKIND.WPUSH.toString());
                wpushTpsInfoBean.setINPUT_CNT("" + wpush_pre_input_cnt);
                wpushTpsInfoBean.setOUT_CNT("" + wpush_pre_process_cnt);
                wpushTpsInfoBean.setMAX_INPUT_CNT("" + wpush_max_input_cnt);
                wpushTpsInfoBean.setMAX_OUTPUT_CNT("" + wpush_max_process_cnt);
                wpushTpsInfoBean.setQUEUE_SIZE("" + wpush_queue_size);
                wpushTpsInfoBean.setCHECK_DATE(chek_date);
                putTpsInfo(TPSSERVERKIND.WPUSH,wpushTpsInfoBean);

                // RCS 카운트 메모리에 저장
                TpsInfoBean rcsTpsInfoBean = new TpsInfoBean();
                rcsTpsInfoBean.setSERVER_KIND(TPSSERVERKIND.RCS.toString());
                rcsTpsInfoBean.setINPUT_CNT("" + rcs_pre_input_cnt);
                rcsTpsInfoBean.setOUT_CNT("" + rcs_pre_process_cnt);
                rcsTpsInfoBean.setMAX_INPUT_CNT("" + rcs_max_input_cnt);
                rcsTpsInfoBean.setMAX_OUTPUT_CNT("" + rcs_max_process_cnt);
                rcsTpsInfoBean.setQUEUE_SIZE("" + rcs_queue_size);
                rcsTpsInfoBean.setCHECK_DATE(chek_date);
                putTpsInfo(TPSSERVERKIND.RCS,smsTpsInfoBean);

                // 네이버 카운트 메모리에 저장
                TpsInfoBean naverTpsInfoBean = new TpsInfoBean();
                naverTpsInfoBean.setSERVER_KIND(TPSSERVERKIND.NAVER.toString());
                naverTpsInfoBean.setINPUT_CNT("" + naver_pre_input_cnt);
                naverTpsInfoBean.setOUT_CNT("" + naver_pre_process_cnt);
                naverTpsInfoBean.setMAX_INPUT_CNT("" + naver_max_input_cnt);
                naverTpsInfoBean.setMAX_OUTPUT_CNT("" + naver_max_process_cnt);
                naverTpsInfoBean.setQUEUE_SIZE("" + naver_queue_size);
                naverTpsInfoBean.setCHECK_DATE(chek_date);
                putTpsInfo(TPSSERVERKIND.NAVER,naverTpsInfoBean);

                try {
                    // UMS에 모니터링정보 수집할수 있도록 호출
                    Map<String,Object> reqParam = new HashMap<>();
                    reqParam.put("PROGRAM_ID","UMS");
                    reqParam.put("SERVER_ID",UMSID);
                    reqParam.put("SERVER_NAME",UMSID);
                    reqParam.put("MONITOR_URL",SELF_UMS_URL);
                    reqParam.put("REQUESTER_ID",UMSID);
                    reqParam.put("CHART",getLineChartDatas());
                    reqParam.put("SUMMARY",getSummaryData());

                    umsMonitoringService.storeDatas(reqParam);

                }catch (Exception e){
                    logger.error("UMS 모니터링 정보 호출시 에러발생. 이유 : "+e.toString());
                }
            }
        };
    }
}
