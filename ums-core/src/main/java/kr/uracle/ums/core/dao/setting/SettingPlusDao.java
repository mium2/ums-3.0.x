package kr.uracle.ums.core.dao.setting;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import kr.uracle.ums.core.dao.BaseDao;

@Repository
public class SettingPlusDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.plus";
	}
	public List<Map<String, Object>> selectList() {
		return sqlSessionTemplate.selectList(String.format("%s.list", namespace));
	}
	public List<Map<String, Object>> selectList(Map<String,Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.list", namespace), param);
	}
	public int insert(Map<String,Object> param) {
		return sqlSessionTemplate.insert(String.format("%s.regdit", namespace), param);
	}
	public Object update(Map<String, Object> param) {
		return sqlSessionTemplate.update(String.format("%s.modify", namespace), param);
	}
	public Object remove(Map<String, Object> param) {
		return sqlSessionTemplate.delete(String.format("%s.remove", namespace), param);
	}
}