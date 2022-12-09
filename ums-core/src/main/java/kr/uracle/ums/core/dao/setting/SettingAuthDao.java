package kr.uracle.ums.core.dao.setting;

import kr.uracle.ums.core.dao.BaseDao;
import kr.uracle.ums.core.vo.setting.AdminUserVo;
import kr.uracle.ums.core.vo.setting.AuthGroupVo;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class SettingAuthDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.authgroup";
	}
	public int selMaxAuthGroupCode() {
		return sqlSessionTemplate.selectOne(String.format("%s.selMaxAuthGroupCode", namespace));
	}
	public List<AuthGroupVo> getAuthGroupList() throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selAuthGroupList", namespace));
	}
	public List<AdminUserVo> selAdminUseAuthGroup(Map<String, Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selAdminUserUseAuthCode", namespace), param);
	}
	public int inAuthGroup(Map<String, Object> param) {
		return sqlSessionTemplate.insert(String.format("%s.inAuthGroup", namespace), param);
	}
	public int upAuthGroup(Map<String, Object> param) {
		return sqlSessionTemplate.update(String.format("%s.upAuthGroup", namespace), param);
	}
	public int delAuthGroup(Map<String, Object> param) {
		return sqlSessionTemplate.delete(String.format("%s.delAuthGroup", namespace), param);
	}
}