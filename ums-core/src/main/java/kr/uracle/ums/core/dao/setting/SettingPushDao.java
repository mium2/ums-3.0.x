package kr.uracle.ums.core.dao.setting;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import kr.uracle.ums.core.dao.BaseDao;

@Repository
public class SettingPushDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.push";
	}
	public List<Map<String, Object>> selectList(Map<String, Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selectPushService", namespace), param);
	}
	public List<Map<String, Object>> selectServiceList() {
		return sqlSessionTemplate.selectList(String.format("%s.selectPushServiceList", namespace));
	}
	public int insert(Map<String, Object> param) {
		return sqlSessionTemplate.insert(String.format("%s.insPushService", namespace), param);
	}
	public int update(Map<String, Object> param) {
		return sqlSessionTemplate.update(String.format("%s.uptPushService", namespace), param);
	}
	public int delete(Map<String, Object> param) {
		return sqlSessionTemplate.delete(String.format("%s.delPushService", namespace), param);
	}
}