package kr.uracle.ums.core.common;

import com.google.gson.Gson;
import kr.uracle.ums.codec.redis.config.ErrorManager;
import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.core.batch.*;
import kr.uracle.ums.core.common.license.LicenseValidator;
import kr.uracle.ums.core.core.AuthIPCheckManager;
import kr.uracle.ums.core.core.tps.TpsManager;
import kr.uracle.ums.core.ehcache.PreventIdCacheMgr;
import kr.uracle.ums.core.ehcache.PreventMobileCacheMgr;
import kr.uracle.ums.core.processor.CancleManager;
import kr.uracle.ums.core.processor.SentInfoDbUpManager;
import kr.uracle.ums.core.processor.SentInfoManager;
import kr.uracle.ums.core.processor.edoc.EdocDbMgr;
import kr.uracle.ums.core.processor.kakao.AllimtalkWorkerMgrPool;
import kr.uracle.ums.core.processor.kakao.FriendtalkWorkerMgrPool;
import kr.uracle.ums.core.processor.mms.MmsWorkerMgrPool;
import kr.uracle.ums.core.processor.naver.NaverWorkerMgrPool;
import kr.uracle.ums.core.processor.push.PushWorkerMgrPool;
import kr.uracle.ums.core.processor.rcs.RcsWorkerMgrPool;
import kr.uracle.ums.core.processor.react.ReactMgr;
import kr.uracle.ums.core.processor.redis.RedisPushManager;
import kr.uracle.ums.core.processor.sms.SmsWorkerMgrPool;
import kr.uracle.ums.core.processor.wpush.WPushWorkerMgrPool;
import kr.uracle.ums.core.service.send.split.SplitSenderManager;
import kr.uracle.ums.core.util.amsoft.AmClientPoolMgr;
import kr.uracle.ums.core.util.httppoolclient.HttpPoolClient;
import kr.uracle.ums.core.util.tcpchecker.TcpAliveConManager;
import kr.uracle.ums.tcppitcher.client.amsoft.store.session.ConnectInfoBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.net.InetAddress;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by Y.B.H(mium2) on 2019. 1. 30..
 */
public class UmsInitListener implements ServletContextListener{
	private static final Logger logger = LoggerFactory.getLogger(UmsInitListener.class);
	
	/** context */
	public static ServletContext sc;
	public static WebApplicationContext wContext;
	public static Properties webProperties = null;
	
	private DbSettingLoader dbSettingLoader;
	
	private LgcnsAlmTokTemplBatch lgcnsAlmTokTemplBatch;
	private LotteAlmTokTemplBatch lotteAlmTokTemplBatch;
	private McsNaverTemplBatch mcsNaverTemplBatch;
	private LogTableClearBatch logTableClearBatch;
	private MasterRollChecker masterRollChecker;
	private StatisticsInDbProcessor statisticsInDbProcessor;
	private PreventCacheBatch preventCacheBatch;
	private RedisPushManager redisPushManager;
	private PreventMobileCacheMgr preventMobileCacheMgr;
	private SentInfoManager sentInfoManager;
	private TcpAliveConManager tcpAliveConManager;
	private CancleManager cancleManager;
	private FatigueClearBatch fatigueClearBatch;

	private PushWorkerMgrPool pushManagerPool;
	private WPushWorkerMgrPool wpushManagerPool;
	private AllimtalkWorkerMgrPool altManagerPool;
	private FriendtalkWorkerMgrPool frtManagerPool;
	private NaverWorkerMgrPool naverManagerPool;
	private RcsWorkerMgrPool rcsManagerPool;
	private SmsWorkerMgrPool smsManagerPool;
	private MmsWorkerMgrPool mmsManagerPool;
	private ReactMgr reactMgr;
	private EdocDbMgr edocDbMgr;
	private SplitSenderManager splitSenderManager;

	private SentInfoDbUpManager sentInfoDbUpManager;

	private AmClientPoolMgr amClientPoolMgr = null;

	private static List<String> USE_KKO_PROVIDERS = new ArrayList<>();
	private String NAVER_PROVIDER = "";
	
	public static Set<String> WHITELIST_TARGET = new HashSet<String>();
	//고객요청 발송결과 정보처리 여부 판단하는 필터
	private static Set<String> customResultFilterSet = new HashSet<>();
	private Gson gson = new Gson();

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		logger.info("#########################################################");
		logger.info("####  [URACLE UMS v3.0.0] Web Application Starting...]  ###");
		logger.info("#########################################################");
		try {
			sc = servletContextEvent.getServletContext();
			// Spring Application Context 가져오기
			wContext = WebApplicationContextUtils.getWebApplicationContext(sc);
			webProperties = (Properties) wContext.getBean("myProperties");
		} catch (Exception e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
		// 라이센스 체크
		try {
			LicenseValidator licenseValidator = LicenseValidator.getInstance();
			licenseValidator.setLicenseFileDir("WEB-INF/classes/config/");
			licenseValidator.initialize();
			if (!licenseValidator.validate()) {
				logger.error("#########################################################");
				logger.error("##############      License Error ~~!     ###############");
				logger.error("#############################################################################################");
				logger.error("!!!!!!!!!!! Check the expiration time of the IP server license file.!!!!!!!!!!!!!!!");
				logger.error("#############################################################################################");
				contextDestroyed(servletContextEvent);
				return;
			}
		} catch (Exception e) {
			logger.error("#########################################################");
			logger.error("##############      License Error ~~!     ###############");
			logger.error("#####################################################################################################");
			logger.error("!!!!!!! The license file is incorrect. Please use the license file as received from Uracle !!!!!!!!!");
			logger.error("######################################################################################################");
			contextDestroyed(servletContextEvent);
			return;
		}

		try {
			//서버 정보 설정
			String SERVERID = InetAddress.getLocalHost().getHostName()+webProperties.getProperty("UMS.ID");
			System.setProperty("SERVERID",SERVERID);

			String sysLegacyDbUseYN = webProperties.getProperty("LEGACY.DB.USE", "Y");
			System.setProperty("LEGACY.DB.USE", sysLegacyDbUseYN);
			dbSettingLoader = DbSettingLoader.getInstance();
			dbSettingLoader.init();
			
			// 알림톡 템플릿 배치 구동
			String KKO_PROVIDER = webProperties.getProperty("KKO.PROVIDER","").trim();
			if(KKO_PROVIDER.indexOf(",")>0){
				String[] ALT_PROVIDER_ARR = KKO_PROVIDER.split(",");
				for(String PROVIDER : ALT_PROVIDER_ARR){
					this.USE_KKO_PROVIDERS.add(PROVIDER.trim());
				}
			}else{
				this.USE_KKO_PROVIDERS.add(KKO_PROVIDER);
			}

			for(String kkoProvider : USE_KKO_PROVIDERS){
				if(kkoProvider.equals(Constants.KKO_PROVIDER.LGCNS.toString()) || kkoProvider.equals(Constants.KKO_PROVIDER.AM.toString())){
					// LGCNS or AMSoft
					lgcnsAlmTokTemplBatch = wContext.getBean(LgcnsAlmTokTemplBatch.class);
					lgcnsAlmTokTemplBatch.startScheduler();
				} else if (kkoProvider.equals(Constants.KKO_PROVIDER.LOTTE.toString())) {
					// 롯데 알림톡 동기화 배치 구현
					lotteAlmTokTemplBatch = wContext.getBean(LotteAlmTokTemplBatch.class);
					lotteAlmTokTemplBatch.startScheduler();
				}
			}

			this.NAVER_PROVIDER = webProperties.getProperty("NAVER.PROVIDER","");
			if(Constants.NAVER_PROVIDER.MTS.toString().equals(NAVER_PROVIDER)) {
				mcsNaverTemplBatch = wContext.getBean(McsNaverTemplBatch.class);
				mcsNaverTemplBatch.startScheduler();
			}

			// 로그성 테이블 정리 배치 구동
			logTableClearBatch = wContext.getBean(LogTableClearBatch.class);
			logTableClearBatch.startScheduler();

			//마스터 체크
			masterRollChecker = MasterRollChecker.getInstance();
			masterRollChecker.startScheduler();

			// 발송제한 캐시 DB동기화 처리
			PreventIdCacheMgr preventIdCacheMgr = wContext.getBean(PreventIdCacheMgr.class);
			preventIdCacheMgr.initializeDbSync();

			preventCacheBatch = wContext.getBean(PreventCacheBatch.class);
			preventCacheBatch.startScheduler();

			// 발송통계 수집 프로세서
			statisticsInDbProcessor = wContext.getBean(StatisticsInDbProcessor.class);
			statisticsInDbProcessor.startScheduler();

			//레디스 발송규 Push처리 매니저 구동
			redisPushManager = RedisPushManager.getInstance();
			redisPushManager.startRedisPushThread();

			//발송처리현황 정보 매니저 구동(스케줄러)
			sentInfoManager = wContext.getBean(SentInfoManager.class);
			sentInfoManager.sentInfoStart();

			//피로도 정리 배치
			fatigueClearBatch = wContext.getBean(FatigueClearBatch.class);
			fatigueClearBatch.startScheduler();

			// API 허용 IP체커 구동
			AuthIPCheckManager.getInstance().init();

			// TPM 매너저 구동
			TpsManager.getInstance();

			//TcpAliveConManager
			tcpAliveConManager = wContext.getBean(TcpAliveConManager.class);

			// 취소요청 처리 매니저
			cancleManager = wContext.getBean(CancleManager.class);
			cancleManager.startScheduler();

			// 분할발송처리 매니저 구동
			splitSenderManager = SplitSenderManager.getInstance();
			splitSenderManager.startWork();

			// 발송카운트 업데이트 쓰레드 인스턴스
			sentInfoDbUpManager = wContext.getBean(SentInfoDbUpManager.class);

			// 커스텀 결과정보 필터
			String customResultFilterJson = webProperties.getProperty("CUSTOM.RESULT.FILTER.JSONLIST","");;
			customResultFilterJson = customResultFilterJson.trim();
			if(!"".equals(customResultFilterJson)){
				try{
					customResultFilterSet = gson.fromJson(customResultFilterJson, HashSet.class);
				}catch (Exception e){}
			}

			//AMSoft TCP 서버 연결관리 매니저 구동
			String amTcpServerUseYn = webProperties.getProperty("AMSoft.TCP.USEYN","N");
			if("Y".equals(amTcpServerUseYn.trim())){
				String amTcpRealServerConInfo = webProperties.getProperty("AMSoft.TCP.REAL.SERVERS","");
				amRegServer(amTcpRealServerConInfo, "REAL");

				String amTcpBatchServerConInfo = webProperties.getProperty("AMSoft.TCP.BATCH.SERVERS","");
				amRegServer(amTcpBatchServerConInfo, "BATCH");
			}

			//에러코드 로드
			String errorCodeLoadInfo = webProperties.getProperty("ERRORCODE.LOADINFO", "");
			if(StringUtils.isBlank(errorCodeLoadInfo) || errorCodeLoadInfo.split(",").length <= 0) {
				ErrorManager.getInstance().loadAll();
			}else {
				ErrorManager.getInstance().loadUmsUracle();
				String[] channelLoadInfos = errorCodeLoadInfo.split(",");
				for(String c : channelLoadInfos) {
					c = c.trim();
					String[] info = c.trim().split(":"); 
					if(info.length !=2) {
						ErrorManager.getInstance().loadAll();
						break;
					}
					String channel = info[0];
					for(String provider : info[1].split("/")) {
						String target = channel+"_"+provider;
						ErrorManager.getInstance().loadTarget(target);
					}
				}
			}

			//PreventMobileCacheMgr 발송제한 핸드폰번호 등록
			preventMobileCacheMgr =  wContext.getBean(PreventMobileCacheMgr.class);
			String preventHpNums = webProperties.getProperty("PREVENT.MOBILE.NUM","");
			if(!"".equals(preventHpNums)){
				Set<String> preventChanel = new HashSet<>();
				preventChanel.add(SendType.KKOALT.toString());
				preventChanel.add(SendType.KKOFRT.toString());
				preventChanel.add(SendType.SMS.toString());
				preventChanel.add(SendType.MMS.toString());
				if (preventHpNums.indexOf(",") > 0) {
					String[] preventHpNumsArr = preventHpNums.split(",");
					for (int i = 0; i < preventHpNumsArr.length; i++) {
						preventMobileCacheMgr.putCache(preventHpNumsArr[i].trim(),preventChanel);
					}
				} else {
					preventMobileCacheMgr.putCache(preventHpNums.trim(),preventChanel);
				}
			}
			
			String whiteListUseYn = webProperties.getProperty("WHITELIST.USEYN",  "N");
			if(whiteListUseYn.equals("Y")) {
				String whiteListTarget = webProperties.getProperty("WHITELIST.TARGET",  "");
				String[] list = whiteListTarget.split(",");
				for(String targetInfo : list) {
					WHITELIST_TARGET.add(targetInfo);
				}
			}
			
			
			//프로세스 매니저 초기화
			this.pushManagerPool = wContext.getBean(PushWorkerMgrPool.class);
			this.pushManagerPool.startPushWorker();
			this.wpushManagerPool = wContext.getBean(WPushWorkerMgrPool.class);
			this.wpushManagerPool.startPushWorker();

			this.altManagerPool= wContext.getBean(AllimtalkWorkerMgrPool.class);
			this.altManagerPool.startWorker();
			this.frtManagerPool= wContext.getBean(FriendtalkWorkerMgrPool.class);
			this.frtManagerPool.startWorker();

			this.naverManagerPool = wContext.getBean(NaverWorkerMgrPool.class);
			this.naverManagerPool.startWorker();

			this.rcsManagerPool= wContext.getBean(RcsWorkerMgrPool.class);
			this.rcsManagerPool.startWorker();

			this.smsManagerPool = wContext.getBean(SmsWorkerMgrPool.class);
			this.smsManagerPool.startWorker();
			this.mmsManagerPool = wContext.getBean(MmsWorkerMgrPool.class);
			this.mmsManagerPool.startWorker();

			this.reactMgr = wContext.getBean(ReactMgr.class);
			this.reactMgr.initailize();

			String edocUseYn = webProperties.getProperty("EDOC.USE_YN","N");
			if("Y".equals(edocUseYn)) {
				this.edocDbMgr = wContext.getBean(EdocDbMgr.class);
				this.edocDbMgr.initailize();
			}

		} catch (Exception e) {
			logger.error(String.format("## UMS initialize Error: %s", e.getMessage()), e);
		}
    }

	private void amRegServer(String amTcpServerConInfo, String amServerGroup){
		String[] amTcpServerConInfoArr = amTcpServerConInfo.trim().split(",");
		List<ConnectInfoBean> connectInfoBeanList = new ArrayList<>();
		for(String hostConInfo : amTcpServerConInfoArr) {
			hostConInfo = hostConInfo.trim();
			if("".equals(hostConInfo)){
				continue;
			}
			String[] infoArr = hostConInfo.split(":");
			ConnectInfoBean connectInfoBean = new ConnectInfoBean();
			connectInfoBean.setHostIp(infoArr[0].trim());
			connectInfoBean.setPort(Integer.parseInt(infoArr[1].trim()));
			connectInfoBeanList.add(connectInfoBean);
		}
		if(connectInfoBeanList.size()>0) {
			this.amClientPoolMgr= AmClientPoolMgr.getInstance();
			this.amClientPoolMgr.init();
			this.amClientPoolMgr.regAmServerGroup(connectInfoBeanList, amServerGroup);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		try {
			//종료시 파일큐 버퍼에 담긴 내용 commit();
			if(reactMgr!=null) {
				reactMgr.fileQueueCommit();
				reactMgr.destory();
			}
			if(edocDbMgr!=null) {
				edocDbMgr.fileQueueCommit();
				edocDbMgr.destory();
			}
			if(pushManagerPool!=null){
				pushManagerPool.stopPushWorker();
			}
			if(wpushManagerPool!=null){
				wpushManagerPool.stopPushWorker();
			}
			if(altManagerPool!=null){
				altManagerPool.stopWorker();
			}
			if(frtManagerPool!=null){
				frtManagerPool.stopWorker();
			}
			if(naverManagerPool!=null){
				naverManagerPool.stopWorker();
			}
			if(rcsManagerPool!=null){
				rcsManagerPool.stopWorker();
			}
			if(smsManagerPool!=null){
				smsManagerPool.stopWorker();
			}
			if(mmsManagerPool!=null){
				mmsManagerPool.stopWorker();
			}
			if(tcpAliveConManager!=null) {
				tcpAliveConManager.destroy();
			}
			if(redisPushManager!=null){
				redisPushManager.stopRedisPushThread();
			}
			HttpPoolClient.getInstance().destory();
			if(statisticsInDbProcessor!=null) {
				statisticsInDbProcessor.stopScheduler();
			}
			if(preventCacheBatch!=null) {
				preventCacheBatch.stopScheduler();
			}
			if(masterRollChecker!=null) {
				masterRollChecker.stopScheduler();
			}
			if(logTableClearBatch!=null) {
				logTableClearBatch.stopScheduler();
			}
			for(String kkoProvider : USE_KKO_PROVIDERS){
				if(kkoProvider.equals(Constants.KKO_PROVIDER.LGCNS.toString()) || kkoProvider.equals(Constants.KKO_PROVIDER.AM.toString())){
					// LGCNS or AMSoft
					lgcnsAlmTokTemplBatch.stopScheduler();
				} else if (kkoProvider.equals(Constants.KKO_PROVIDER.LOTTE.toString())) {
					// 롯데 알림톡 동기화 배치 구현
					lotteAlmTokTemplBatch.stopScheduler();
				}
			}
			if(Constants.NAVER_PROVIDER.MTS.toString().equals(NAVER_PROVIDER)) {
				mcsNaverTemplBatch.stopScheduler();
			}
			if(sentInfoManager!=null) {
				sentInfoManager.sentInfoStop();
			}
			TpsManager.getInstance().stopScheduler();
			if(cancleManager!=null) {
				cancleManager.stopScheduler();
			}
			if(fatigueClearBatch!=null){
				fatigueClearBatch.stopScheduler();
			}
			if(splitSenderManager!=null){
				splitSenderManager.destory();
			}
			if(sentInfoDbUpManager!=null){
				sentInfoDbUpManager.destory();
			}
			// AMSOFT TCP Poll 매니저 구동 종료 호출
			if(amClientPoolMgr!=null){
				amClientPoolMgr.destroy();
			}

			// JDBC 드라이버 수동제거
			Enumeration<Driver> drivers = DriverManager.getDrivers();
			while (drivers.hasMoreElements()) {
				Driver driver = drivers.nextElement();
				try {
					DriverManager.deregisterDriver(driver);
				} catch (SQLException e) {
					logger.warn(String.format("Error deregistering driver %s", driver), e);
				}
			}
			logger.info("모든 구동 쓰레드 정상종료 완료");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}
	
	public static boolean checkWHITELIST_TARGET(String identifier) {
		return WHITELIST_TARGET.contains(identifier);
	}
	
	public static Set<String> getWHITELIST_TARGET(){
		return WHITELIST_TARGET;
	}

	public static List<String> getUseKkoProviders() {
		return USE_KKO_PROVIDERS;
	}

	public static Set<String> getCustomResultFilterSet() {
		return customResultFilterSet;
	}
}