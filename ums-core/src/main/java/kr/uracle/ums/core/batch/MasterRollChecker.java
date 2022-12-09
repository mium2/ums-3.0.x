package kr.uracle.ums.core.batch;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import kr.uracle.ums.core.common.UmsInitListener;

/**
 * 마스터롤 수행여부를 체크하여 관리 하는 클래스
 * @author knigh
 *
 */

public class MasterRollChecker {
	private final Logger logger = LoggerFactory.getLogger(MasterRollChecker.class);
	
	private static MasterRollChecker instance = new MasterRollChecker();
	public static MasterRollChecker getInstance() {
		return instance;
	}
	
	private SqlSessionTemplate sqlSessionTemplate;
	
	private ThreadPoolTaskScheduler scheduler;
	
	private boolean isMaster = false;
	public boolean isMaster() {
		return isMaster;
	}
	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}

	public int loopCnt = 0;
	
	private Map<String, Object> param = new HashMap<String, Object>();
	public String getProcessorKey() {
		return (String) param.get("PROCESSEOR_KEY");
	}
	
	private MasterRollChecker() {
		sqlSessionTemplate = (SqlSessionTemplate) UmsInitListener.wContext.getBean("sqlSessionTemplate");
		try {
			param.put("PROCESSEOR_KEY", String.format("%s_%s", InetAddress.getLocalHost().getHostName(), ((Properties) UmsInitListener.wContext.getBean("myProperties")).getProperty("UMS.ID","")));
			
			List<Map<String, Object>> list = sqlSessionTemplate.selectList("mybatis.batch.selMasterCheck", param);
			if (list.isEmpty()) {
				sqlSessionTemplate.insert("mybatis.batch.insMasterCheck", param);
			} else {
				if ("Y".equals(list.get(0).get("MASTER_YN"))) {
					isMaster = true;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	public void startScheduler() {
		logger.info("### MasterRollChecker BATCH START!");
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		// 스케쥴러가 시작되는 부분
		scheduler.schedule(getRunnable(), getTrigger());
	}
	public void stopScheduler() {
		scheduler.shutdown();
		logger.info("### MasterRollChecker BATCH STOP!");
	}
	private Runnable getRunnable(){
		return new Runnable(){
			@Override
			public void run() {
				startMasterRollCheck();
			}
		};
	}
	private Trigger getTrigger() {
		// 작업 주기 설정
		return new PeriodicTrigger(60, TimeUnit.SECONDS);
	}
	public void startMasterRollCheck() {
		try {
			if (isMaster) {
				int applyRowCnt = sqlSessionTemplate.update("mybatis.batch.uptMasterHealthCheck", param);
				if(applyRowCnt==0){
					isMaster = false;
				}
			} else {
				List<Map<String, Object>> list = sqlSessionTemplate.selectList("mybatis.batch.selMasterCheck");
				if (list.isEmpty()) {
					if (sqlSessionTemplate.update("mybatis.batch.uptMasterChange", param) > 0) {
						isMaster = true;
					}
				} else {
//					long diff = ((Timestamp) list.get(0).get("NOW")).getTime() - ((Timestamp) list.get(0).get("HEALTH_CHECK")).getTime();
					Object nowDateObj = list.get(0).get("NOW");
					Object healthDateObj = list.get(0).get("HEALTH_CHECK");
					Timestamp nowTimeStamp = null;
					Timestamp healthTimeStamp = null;
					// 현재 시간
					if(nowDateObj instanceof LocalDateTime){
						LocalDateTime nowLocalDateTime = (LocalDateTime)nowDateObj;
						nowTimeStamp = Timestamp.valueOf(nowLocalDateTime);
					}else{
						nowTimeStamp = (Timestamp)nowDateObj;
					}
					// 헬스 업데이트 시간
					if(healthDateObj instanceof LocalDateTime){
						LocalDateTime healthLocalDateTime = (LocalDateTime)healthDateObj;
						healthTimeStamp = Timestamp.valueOf(healthLocalDateTime);
					}else{
						healthTimeStamp = (Timestamp)healthDateObj;
					}
					long diff = nowTimeStamp.getTime() - healthTimeStamp.getTime();
					if (diff > 120000) {
						sqlSessionTemplate.update("mybatis.batch.uptMasterChange", list.get(0));
						if (sqlSessionTemplate.update("mybatis.batch.uptMasterChange", param) > 0) {
							isMaster = true;
						}
					}
				}
			}
		}catch (Exception e){
			isMaster = false;
			logger.error("마스터롤 체크시 에러 "+e.toString());
			e.printStackTrace();
		}
		logger.debug("isMaster:"+isMaster);

	}
}