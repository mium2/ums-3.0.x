package kr.uracle.ums.core.dao.setting;

import kr.uracle.ums.core.vo.member.MemberVo;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import kr.uracle.ums.core.dao.BaseDao;

import java.util.List;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 2. 21..
 */
@Repository
public class MemberDao extends BaseDao {
	@Autowired(required = true)
	@Qualifier("sqlSessionTemplate")
	private SqlSessionTemplate sqlSessionTemplate;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.member";
	}
	public List<Map<String, Object>> selSettingMember(Map<String, Object> dbParamMap) throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selSettingMember", namespace), dbParamMap);
	}
	public int allDelMember() throws Exception {
		return sqlSessionTemplate.delete(String.format("%s.allDelMember", namespace));
	}
	public List<Map<String, Object>> selPushUser(Map<String, Object> param) throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selPushUser", namespace), param);
	}
	public List<Map<String, Object>> schPushUser(Map<String, Object> param) throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.schPushUser", namespace), param);
	}
	public int delSettingMember(Map<String, Object> param) {
		return sqlSessionTemplate.delete(String.format("%s.delMember", namespace), param);
	}

	public void insertEachMember(MemberVo memberVo) throws Exception {

		sqlSessionTemplate.insert(String.format("%s.inMember", namespace), memberVo);
	}

	public int chkExistMember(MemberVo memberVo) throws Exception {
		return sqlSessionTemplate.selectOne(String.format("%s.chkExistMember", namespace),memberVo);
	}
}