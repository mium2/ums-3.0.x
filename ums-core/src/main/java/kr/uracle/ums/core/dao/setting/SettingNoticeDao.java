package kr.uracle.ums.core.dao.setting;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import kr.uracle.ums.core.dao.BaseDao;

@Repository
public class SettingNoticeDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.notice";
	}
	public List<Map<String, Object>> selectList(Map<String, Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selNotice", namespace), param);
	}
	public List<Map<String, Object>> selectPreDetail(Map<String, Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selPreNotice", namespace), param);
	}
	public List<Map<String, Object>> selectNextDetail(Map<String, Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selNextNotice", namespace), param);
	}
	public int insert(Map<String, Object> param) {
		return sqlSessionTemplate.insert(String.format("%s.insNotice", namespace), param);
	}
	public int update(Map<String, Object> param) {
		return sqlSessionTemplate.update(String.format("%s.uptNotice", namespace), param);
	}
	public int delete(Map<String, Object> param) {
		return sqlSessionTemplate.delete(String.format("%s.delNotice", namespace), param);
	}
}