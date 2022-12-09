package kr.uracle.ums.core.dao.setting;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import kr.uracle.ums.core.dao.BaseDao;

@Repository
public class SettingSmsDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.sms";
	}
	public List<Map<String, Object>> selectList(Map<String, Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.listSmsSender", namespace), param);
	}
	public int insert(Map<String, Object> param) {
		return sqlSessionTemplate.insert(String.format("%s.insertSmsSender", namespace), param);
	}
	public int delete(Map<String, Object> param) {
		return sqlSessionTemplate.delete(String.format("%s.deleteSmsSender", namespace), param);
	}
}
