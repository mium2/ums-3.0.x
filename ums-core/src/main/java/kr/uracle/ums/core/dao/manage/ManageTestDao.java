package kr.uracle.ums.core.dao.manage;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import kr.uracle.ums.core.dao.BaseDao;

@Repository
public class ManageTestDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.manage.test";
	}
	public List<Map<String, Object>> selectManageTestGroup() {
		return sqlSessionTemplate.selectList(String.format("%s.selectManageTestGroup", namespace));
	}
	public int selectGroupId() {
		return sqlSessionTemplate.selectOne(String.format("%s.selectGroupId", namespace));
	}
	public int insert(Map<String, Object> param) {
		return sqlSessionTemplate.insert(String.format("%s.insManageTestGroup", namespace), param);
	}
	public int delete(Map<String, Object> param) {
		return sqlSessionTemplate.delete(String.format("%s.delManageTestGroup", namespace), param);
	}
	public List<Map<String, Object>> selectManageTestGroupMember(Map<String, Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selectManageTestGroupMemger", namespace), param);
	}
	public int insertMemger(Map<String, Object> param) {
		return sqlSessionTemplate.insert(String.format("%s.insManageTestGroupMember", namespace), param);
	}
	public Object deleteMember(Map<String, Object> param) {
		return sqlSessionTemplate.delete(String.format("%s.delManageTestGroupMember", namespace), param);
	}
}
