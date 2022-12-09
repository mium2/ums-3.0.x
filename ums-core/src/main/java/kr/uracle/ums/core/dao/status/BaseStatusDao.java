package kr.uracle.ums.core.dao.status;

import java.util.List;
import java.util.Map;

import kr.uracle.ums.core.dao.BaseDao;

public abstract class BaseStatusDao extends BaseDao {
	public List<Map<String, Object>> selectList(Map<String,Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selStatusList", namespace), param);
	}
	public List<Map<String, Object>> selectFailList(Map<String,Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selStatusFailList", namespace), param);
	}
}