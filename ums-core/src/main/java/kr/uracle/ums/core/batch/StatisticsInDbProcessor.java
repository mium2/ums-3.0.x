package kr.uracle.ums.core.batch;

import kr.uracle.ums.core.processor.bean.StatisticsBean;
import kr.uracle.ums.core.processor.statistics.StatisticsMgr;

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 3..
 * 주기적으로 StatiacsMgr에게 수집된 발송통계정보가 있는지 확인 하여 DB에 저장한다.
 */
@Service
public class StatisticsInDbProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@Autowired(required = true)
	@Qualifier("sqlSessionTemplate")
	private SqlSessionTemplate sqlSessionTemplate;

	@Autowired(required = true)
	private StatisticsMgr statisticsMgr;

	private ThreadPoolTaskScheduler scheduler;

	public void stopScheduler() {
		scheduler.shutdown();
		logger.info("### StatisticsInDbProcessor STOP!");
	}
	public void startScheduler() {
		logger.info("##########################################");
		logger.info("# StatisticsInDbProcessor startScheduler");
		logger.info("##########################################");
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.initialize();
		// 스케쥴러가 시작되는 부분
		scheduler.schedule(getRunnable(), getTrigger());
	}
	private Runnable getRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				Map<String, StatisticsBean> workMap = statisticsMgr.getSentResultInfoMap();
				if (workMap != null && workMap.size() > 0) {
					Set<Map.Entry<String, StatisticsBean>> workMapSet = workMap.entrySet();
					for (Map.Entry<String, StatisticsBean> me : workMapSet) {
						try {
							StatisticsBean statisticsBean = me.getValue();
							StatisticsBean dbStatisticsBean = sqlSessionTemplate.selectOne("mybatis.statistics.send.selStaticSendOne", statisticsBean);
							if (dbStatisticsBean == null) {
								try {
									sqlSessionTemplate.insert("mybatis.statistics.send.inStaticSend", statisticsBean);
								} catch (Exception e) {
									e.printStackTrace();
									sqlSessionTemplate.update("mybatis.statistics.send.upStaticCount", statisticsBean);
								}
							} else {
								sqlSessionTemplate.update("mybatis.statistics.send.upStaticCount", statisticsBean);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		};
	}
	private Trigger getTrigger() { // 작업 주기 설정
		PeriodicTrigger periodicTrigger = new PeriodicTrigger(60, TimeUnit.SECONDS);
		return periodicTrigger;
	}
}