package kr.uracle.ums.core.dao.template;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import kr.uracle.ums.core.dao.BaseDao;

@Repository
public class TemplateDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.templete";
	}
	public List<Map<String, Object>> selectList(Map<String,Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selectTemplate", namespace), param);
	}
	public int insert(Map<String,Object> param) {
		return sqlSessionTemplate.insert(String.format("%s.insertTemplate", namespace), param);
	}
	public int update(Map<String,Object> param) {
		return sqlSessionTemplate.update(String.format("%s.updateTemplate", namespace), param);
	}
}