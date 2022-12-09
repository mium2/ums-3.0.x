package kr.uracle.ums.core.dao.setting;

import org.springframework.stereotype.Repository;

import kr.uracle.ums.core.dao.BaseDao;

import java.util.List;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 2. 15..
 */
@Repository
public class CustomFieldDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.customfild";
	}
	public void inCustomFild(Map<String, Object> dbParamMap) throws Exception {
		sqlSessionTemplate.insert(String.format("%s.inCustomFild", namespace), dbParamMap);
	}
	public void delCustomFild(Map<String, Object> dbParamMap) throws Exception {
		sqlSessionTemplate.delete(String.format("%s.delCustomFild", namespace), dbParamMap);
	}
	public void upCustomFild(Map<String, Object> dbParamMap) throws Exception {
		sqlSessionTemplate.update(String.format("%s.upCustomFild", namespace), dbParamMap);
	}
	public List<Map<String, Object>> getCustomFild(Map<String, Object> dbPramMap) throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selCustomFild", namespace), dbPramMap);
	}
}