package kr.uracle.ums.core.service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import kr.uracle.ums.core.dao.ums.MonitoringDao;

@Service
public class UmsMonitoringService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
	private final Gson gson = new Gson();
	
	private final int MAX_BYTE = 4000;
	
	private final long EXPIRY_STANDARD_MILSEC = 3*60*1000; 
	
    @Autowired(required = true)
    protected MonitoringDao monitoringDao;
    
    public String storeDatas(Map<String, Object> requestBodyMap) throws Exception{
    	Object chart = requestBodyMap.get("CHART");
    	Object summary = requestBodyMap.get("SUMMARY");
    	String jsonStrChart =  gson.toJson(chart);
    	String jsonStrSummary =  gson.toJson(summary);
    	
    	if(jsonStrChart.length() > MAX_BYTE) {
    		requestBodyMap.put("HISTROY_CHART1", jsonStrChart.substring(0, MAX_BYTE));
    		requestBodyMap.put("HISTROY_CHART2", jsonStrChart.substring(MAX_BYTE));
    	}else {
    		requestBodyMap.put("HISTROY_CHART1", jsonStrChart);
    		requestBodyMap.put("HISTROY_CHART2", "");
    	}
    	requestBodyMap.put("SUMMARY", jsonStrSummary);
    	requestBodyMap.put("UPDATE_MILITIME", System.currentTimeMillis());
    	
    	requestBodyMap.remove("CHART");
    	
    	int uptCnt = monitoringDao.storeDatas(requestBodyMap);
    	if(uptCnt <= 0) return "DB 입력 실패";
    	return null;
    }
    
    public List<Map<String, Object>> getChartDatas() throws Exception{
    	List<Map<String, Object>> chartMaptList = monitoringDao.getChartDatas();
    	for(Map<String, Object> chartMap : chartMaptList) {
  
    		String chartHistory = (String) chartMap.get("HISTROY_CHART");
    		String chartHistory2 = (String) chartMap.get("HISTROY_CHART2");
    		if(StringUtils.isNotBlank(chartHistory2)) {
    			chartMap.remove("HISTROY_CHART2");
    			chartHistory+=chartHistory2;
    		}
    		chartMap.put("HISTROY_CHART", gson.fromJson(chartHistory, new TypeToken<Map<String, Object>>(){}.getType()));
    	}
    	return chartMaptList;
    }
    
    public List<Map<String, Object>> getSummaryDatas() throws Exception{
    	List<Map<String, Object>> summaryMaptList = monitoringDao.getSummaryDatas();
    	long now = System.currentTimeMillis();
    	for(Map<String, Object> infoMap : summaryMaptList) {
    		long UPDATE_MILITIME = ((BigDecimal)infoMap.get("UPDATE_MILITIME")).longValue();
    		String jsonStrSummary =  (String) infoMap.get("SUMMARY");
    		infoMap.put("SUMMARY", gson.fromJson(jsonStrSummary, new TypeToken<Map<String, Object>>(){}.getType()));
    		infoMap.put("STATUS", (now-UPDATE_MILITIME) < EXPIRY_STANDARD_MILSEC);

    	}
    	return summaryMaptList;
    }
}
