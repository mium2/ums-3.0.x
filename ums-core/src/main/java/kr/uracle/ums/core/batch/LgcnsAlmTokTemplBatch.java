package kr.uracle.ums.core.batch;

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import kr.uracle.ums.core.service.template.AlmTokTemplService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 7..
 */
@Service
public class LgcnsAlmTokTemplBatch {
	private Logger logger = LoggerFactory.getLogger(LgcnsAlmTokTemplBatch.class);
	
	private SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMddhhmmss");
	private Map<String, Object> param = new HashMap<String, Object>();

	private String WORKTRACE = "";
	
	@Autowired(required = true)
	@Qualifier("sqlSessionTemplate")
	private SqlSessionTemplate sqlSessionTemplate;
	
	@Autowired(required = true)
	private AlmTokTemplService almTokTemplService;

	@Value(("${KKO.ALLIMTOLK.INIT_DELAY_MIN:1}"))
	private String INIT_DELAY_MIN;

	@Value("${KKO.ALLIMTOLK.BATCH_MIN:360}")
	private String BATCH_LOOP_MIN;
	
	private ThreadPoolTaskScheduler scheduler;
	
	public void stopScheduler() {
		scheduler.shutdown();
		logger.info("### [LGCNS ALLIMTOLK TEMPLATE] SYNC BATCH STOP!");
	}
	public void startScheduler() {
		logger.info("### [LGCNS ALLIMTOLK TEMPLATE] SYNC BATCH RUN~~!");
		this.WORKTRACE = MasterRollChecker.getInstance().getProcessorKey();

		scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		// 스케쥴러가 시작되는 부분
		scheduler.schedule(getRunnable(), getTrigger());
	}
	private Runnable getRunnable(){
		return new Runnable(){
			@Override
			public void run() {
				if (!MasterRollChecker.getInstance().isMaster()) return;
				try {
					param.put("WORKTRACE", WORKTRACE);
					param.put("PROCESSORID", String.format("LGCNS_KKOALT_Batch_%s", format1.format(new Date())));
					sqlSessionTemplate.insert("mybatis.batch.insProcessorInfo", param);
					templApproveSyncExec();
					sqlSessionTemplate.update("mybatis.batch.uptProcessorInfo", param);
					param.clear();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		};
	}
	private Trigger getTrigger() {	// 작업 주기 설정
		int batchLoopMin = 360;  //6시간에 한번씩
		int batchInitDelayMin = 1; //1분 지연
		try {
			batchLoopMin = Integer.parseInt(BATCH_LOOP_MIN.trim());
			batchInitDelayMin = Integer.parseInt(INIT_DELAY_MIN.trim());
		}catch(Exception e){
			logger.error(e.toString());
		}
		PeriodicTrigger periodicTrigger = new PeriodicTrigger(batchLoopMin, TimeUnit.MINUTES);
		periodicTrigger.setInitialDelay(batchInitDelayMin); //구동후 기본 1분 뒤 실행. 설정값의 의해 변경됨.
		return periodicTrigger;
	}
	private void templApproveSyncExec() {
		// 한 시간에 한번 LGCNS에 알림톡 템플릿 승인정보 결과를 요청하여 동기화 처리 한다.
		// STEP 1 : T_UMS_PROCESSORINFO 테이블에 STATUS와 COMPLETEDDATE을 이용하여 배치가 돌지 말지 결정. 다른 UMS에서 먼저 돌았을 수 있기 때문.
		logger.info("### KKO ALLIMTOLK TEMPLATE SYNC BATCH START~~!");
		
		String RESULTCODE = "S";
		String RESULTMSG = "";
		
		try {
			Map<String,Object> resultDataMap = almTokTemplService.newSyncTemplateStatus(null);
			if (resultDataMap.get("errMsg") != null) {
				throw new Exception((String) resultDataMap.get("errMsg"));
			}
			
			param.put("RESULTCODE", RESULTCODE);
			param.put("RESULTMSG", RESULTMSG);
			
			logger.info("### KKO ALLIMTOLK TEMPLATE SYNC BATCH COMPLATE~~!");
		} catch (Exception e) {
			param.put("RESULTCODE", "E");
			param.put("RESULTMSG", e.toString());
			logger.error(e.getMessage(), e);
		}
	}
}