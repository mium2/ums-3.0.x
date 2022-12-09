package kr.uracle.ums.core.batch;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import kr.uracle.ums.core.util.XMLParseUtil;
/**
 * 로그성 테이블을 주기적 하루에 한번 정도 체크하여 삭제하는 배치
 * @author lwg15873
 *
 */

@Service
@SuppressWarnings("unchecked")
public class LogTableClearBatch {
	private final Logger logger = LoggerFactory.getLogger(LogTableClearBatch.class);
	
	private final SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM");
	private final SimpleDateFormat format2 = new SimpleDateFormat("yyyyMM");
	private final SimpleDateFormat format3 = new SimpleDateFormat("yyyyMMddhhmmss");
	
	private final Map<String, Object> param = new HashMap<String, Object>();
	private String WORKTRACE = "";

	@Autowired(required = true)
	@Qualifier("sqlSessionTemplate")
	private SqlSessionTemplate sqlSessionTemplate;
	
	private final URL xmlFileUrl;
	
	private ThreadPoolTaskScheduler scheduler;
	
	public LogTableClearBatch() {
		this.xmlFileUrl = LogTableClearBatch.class.getClassLoader().getResource("config/logTableClear.xml");
	}
	public void stopScheduler() {
		scheduler.shutdown();
		logger.info("### LogTableClearBatch BATCH STOP!");
	}

	public void startScheduler() {
		logger.info("###LogTableClearBatch ready....");
		this.WORKTRACE = MasterRollChecker.getInstance().getProcessorKey();

		scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
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
					param.put("PROCESSORID", String.format("LogTableClearBatch_%s", format3.format(new Date())));
					sqlSessionTemplate.insert("mybatis.batch.insProcessorInfo", param);
					batchExc();
					sqlSessionTemplate.update("mybatis.batch.uptProcessorInfo", param);
					param.clear();
				}catch (Exception e){}
			}
		};
	}

	private Trigger getTrigger() {
		// 작업 주기 설정
//		return new PeriodicTrigger(30, TimeUnit.SECONDS);
		return new CronTrigger("0 0 3 * * *");
	}

	public void batchExc() {
		long startTime = System.currentTimeMillis();
		logger.info("###LogTableClearBatch start....");
		param.put("RESULTCODE","SUCC");
		param.put("RESULTMSG","SUCCESS");
		try {
			StringBuilder workTraceSb = new StringBuilder();
			Map<String, Object> xml = (Map<String, Object>) XMLParseUtil.XmlToMap(xmlFileUrl.getPath()).get("batch");
			List<Map<String, Object>> list = (List<Map<String, Object>>) xml.get("job");
			for (Map<String, Object> param : list) {

				try {
					String TABLE_NAME = "";
					String MONTH = "";
					String WHERE_COLUM = "";
					String WHERE_COLUM_TYPE = "";
					String USE_YN = "N";
					boolean USE_YN_BOOL = false;
					int MONTH_INT = 6;
					String DEL_DATE = "";
					if(param.containsKey("table")){
						TABLE_NAME = param.get("table").toString();
					}
					if(param.containsKey("month")){
						MONTH = param.get("month").toString();
						try{
							MONTH_INT = Integer.parseInt(MONTH);
						}catch (Exception e){
							e.printStackTrace();
						}
					}
					if(param.containsKey("base_col")){
						WHERE_COLUM = param.get("base_col").toString();
					}
					if(param.containsKey("base_col_type")){
						WHERE_COLUM_TYPE = param.get("base_col_type").toString();
					}

					if(param.containsKey("use_yn")){
						if("Y".equals(param.get("use_yn").toString().toUpperCase())){
							USE_YN_BOOL = true;
						}else{
							USE_YN_BOOL = false;
						}
					}

					Calendar temp = Calendar.getInstance();
					temp.add(Calendar.MONTH, -1 * MONTH_INT);

					if(!USE_YN_BOOL || "".equals(TABLE_NAME)){
						continue;
					}

					if("T_UMS_SEND_BATCH".equals(TABLE_NAME) || "T_UMS_SEND_REAL".equals(TABLE_NAME)){
						Map<String,Object> dbParam = new HashMap<String,Object>();
						DEL_DATE = String.format("%s-01", format1.format(temp.getTime()));
						dbParam.put("DEL_DATE",DEL_DATE);
						dbParam.put("TABLE_NAME",TABLE_NAME);

						long MAX_DEL_DETAIL_SEQNO = 0;
						// 삭제기간을 기준으로 가장 큰 DETAIL_SEQNO를 구한다.
						Map<String,Object> dbResultMap = sqlSessionTemplate.selectOne("mybatis.batch.selUmsSendDetailMaxDelSeqno",dbParam);
						if(dbResultMap==null || !dbResultMap.containsKey("DETAIL_SEQNO")){
							continue;
						}else{
							MAX_DEL_DETAIL_SEQNO = Long.parseLong(dbResultMap.get("DETAIL_SEQNO").toString());
						}
						dbParam.put("DETAIL_SEQNO",MAX_DEL_DETAIL_SEQNO);
						// 삭제되는  가장 큰 DETAIL_SEQNO를 이용하여 가장 큰 UMS_SEQNO를 구한다.
						long MAX_DEL_UMS_SEQNO = 0;
						Map<String,Object> dbResultMap2 = sqlSessionTemplate.selectOne("mybatis.batch.selUmsSendMaxDelSeqno",dbParam);
						if(dbResultMap2==null || !dbResultMap2.containsKey("UMS_SEQNO")){
							continue;
						}else{
							MAX_DEL_UMS_SEQNO = Long.parseLong(dbResultMap2.get("UMS_SEQNO").toString());
						}
						dbParam.put("DETAIL_SEQNO",MAX_DEL_DETAIL_SEQNO);
						// 삭제되어야 하는 가장 큰 DETAIL_SEQNO를 이용하여 UMS_DETAIL 테이블을 정리한다.
						int applyDetailRow = sqlSessionTemplate.delete("mybatis.batch.delUmsDetail",dbParam);
						logger.info("["+TABLE_NAME+"_DETAIL] delete cnt :"+applyDetailRow+ ", date : "+dbParam.get("DEL_DATE").toString()+ ", DETAIL_SEQNO:"+MAX_DEL_DETAIL_SEQNO);

						//  DETAIL_SEQNO를 이용하여 UMS_DETAIL 삭제된 UMS_SEQNO를 이용하여 UMS_SEND 테이블을 정리한다.
						dbParam.put("UMS_SEQNO",MAX_DEL_UMS_SEQNO);
						int applySendRow = sqlSessionTemplate.delete("mybatis.batch.delUmsSend",dbParam);
						logger.info("["+TABLE_NAME+"] delete cnt :"+applySendRow+ ", date : "+dbParam.get("DEL_DATE").toString()+ ", UMS_SEQNO:"+MAX_DEL_UMS_SEQNO);
						workTraceSb.append("["+TABLE_NAME+"] "+dbParam.get("DEL_DATE").toString()+ " ,DEL CNT:"+applySendRow+".");

						// 발송카운트 테이블 정리.
						int applySendCountRow = sqlSessionTemplate.delete("mybatis.batch.delUmsSendCount",dbParam);
						logger.info("["+TABLE_NAME+"_COUNT] delete cnt :"+applySendCountRow+ ", date : "+dbParam.get("DEL_DATE").toString()+ ", UMS_SEQNO:"+MAX_DEL_UMS_SEQNO);
						workTraceSb.append("["+TABLE_NAME+"_COUNT] "+dbParam.get("DEL_DATE").toString()+ " ,DEL CNT:"+applySendCountRow+".");

					}else if("T_PUSH_MSG_RECV_MASTER".equals(TABLE_NAME)){
						Map<String,Object> dbParam = new HashMap<String,Object>();
						DEL_DATE = String.format("%s-01", format1.format(temp.getTime()));
						dbParam.put("DEL_DATE",DEL_DATE);
						long MAX_DEL_PUSH_SEQNO = 0;
						Map<String,Object> dbResultMap = sqlSessionTemplate.selectOne("mybatis.batch.selPushMaxDelSeqno",dbParam);
						if(dbResultMap==null || !dbResultMap.containsKey("SEQNO")){
							continue;
						}else{
							MAX_DEL_PUSH_SEQNO = Long.parseLong(dbResultMap.get("SEQNO").toString());
						}

						dbParam.put("SEQNO",MAX_DEL_PUSH_SEQNO);
						// 삭제되어야 하는 가장 큰 DETAIL_SEQNO를 이용하여 UMS_DETAIL 테이블을 정리한다.
						int applyRow = sqlSessionTemplate.delete("mybatis.batch.delPushMsgRecvMaster",dbParam);
						logger.info("["+TABLE_NAME+"] delete cnt :"+applyRow+ ", date : "+dbParam.get("DEL_DATE").toString()+ ", SEQNO:"+MAX_DEL_PUSH_SEQNO);

						// 삭제되어야 하는 가장 큰 SEQNO를 이용하여 T_PUSH_SUCCESS 테이블을 정리한다.
						int applySuccRow = sqlSessionTemplate.delete("mybatis.batch.delPushSuccess",dbParam);
						logger.info("[T_PUSH_SUCCESS] delete cnt :"+applySuccRow+ ", date : "+dbParam.get("DEL_DATE").toString()+ ", SEQNO:"+MAX_DEL_PUSH_SEQNO);

						// 삭제되어야 하는 가장 큰 SEQNO를 이용하여 T_PUSH_FAIL 테이블을 정리한다.
						int applyFailRow = sqlSessionTemplate.delete("mybatis.batch.delPushFail",dbParam);
						logger.info("[T_PUSH_FAIL] delete cnt :"+applyFailRow+ ", date : "+dbParam.get("DEL_DATE").toString()+ ", SEQNO:"+MAX_DEL_PUSH_SEQNO);

						workTraceSb.append("["+TABLE_NAME+"] "+dbParam.get("DEL_DATE").toString()+ " ,DEL CNT:"+applyRow+".");

					}else{
						Map<String,Object> dbParam = new HashMap<String,Object>();
						if (!"".equals(WHERE_COLUM_TYPE) && WHERE_COLUM_TYPE.startsWith("VARCHAR")) {
							dbParam.put("base_value", String.format("%s01", format2.format(temp.getTime())));
						} else {
							dbParam.put("base_value", String.format("%s01", format2.format(temp.getTime())));
						}
						if("".equals(WHERE_COLUM)){
							continue;
						}else{
							dbParam.put("TABLE_NAME",TABLE_NAME);
							dbParam.put("base_col",WHERE_COLUM);
							dbParam.put("base_col_type",WHERE_COLUM_TYPE);
						}
						int applyRow = sqlSessionTemplate.delete("mybatis.batch.logTableClear", dbParam);
						logger.info("["+TABLE_NAME+"] delete cnt :"+applyRow+ ", date : "+dbParam.get("base_value").toString());

						workTraceSb.append("["+TABLE_NAME+"] "+dbParam.get("base_value").toString()+ " ,DEL CNT:"+applyRow+".");
					}

				} catch (Exception e) {
					e.printStackTrace();
					param.put("RESULTCODE","FAIL");
					workTraceSb = new StringBuilder();
					workTraceSb.append(e.getMessage());
					logger.error(e.getMessage(), e);
				}
			}
			param.put("RESULTMSG",workTraceSb.toString());
			long executeTime = System.currentTimeMillis()-startTime;
			logger.info("LogTableClearBatch complate!! excute TimeMillis : "+executeTime);
		} catch (Exception e) {
			param.put("RESULTCODE","FAIL");
			param.put("RESULTMSG",e.getMessage());
			logger.error(e.getMessage(), e);
		}
	}
}