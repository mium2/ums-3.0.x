package kr.uracle.ums.core.dao.ums;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class MonitoringDao {
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;

    // 모니터링 데이터 입력/갱신
    public int storeDatas(Map<String, Object> requestBodyMap) throws Exception{
    	int uptCnt = sqlSessionTemplate.update("mybatis.monitor.updateDatas", requestBodyMap);
    	if(uptCnt <= 0) {
    		uptCnt = sqlSessionTemplate.insert("mybatis.monitor.storeDatas", requestBodyMap);
    	}
    	return uptCnt;
    }
    
    // 모니터링 차트데이터 목록 조회
    public List<Map<String, Object>> getChartDatas() throws Exception{
    	return sqlSessionTemplate.selectList("mybatis.monitor.getChartDatas");
    }
    
    // 모니터링 서머리데이터 목록 조회
    public List<Map<String, Object>> getSummaryDatas() throws Exception{
    	return sqlSessionTemplate.selectList("mybatis.monitor.getSummaryDatas");
    }
    
}
