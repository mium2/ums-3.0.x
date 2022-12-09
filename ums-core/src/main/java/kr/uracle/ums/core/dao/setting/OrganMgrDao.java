package kr.uracle.ums.core.dao.setting;

import kr.uracle.ums.core.dao.BaseDao;
import kr.uracle.ums.core.vo.setting.OrganizationVo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 2. 12..
 */
@Repository
public class OrganMgrDao extends BaseDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.organ";
	}
	public List<OrganizationVo> selOrganization() throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selOrganization", namespace));
	}
	// 조직도 자식노드들 조회
	public List<Map<String, Object>> selChildOrgan(Map<String, String> dbParamMap) throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selChildOrgan", namespace), dbParamMap);
	}
	// root Depth 조직도에 등록되어 있는 유저들 조회
	public List<Map<String, Object>> selRootDepthUser() throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selRootDepthUser", namespace));
	}
	// 1Depth 조직도에 등록되어 있는 유저들 조회
	public List<Map<String, Object>> selOneDepthUser() throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selOneDepthUser", namespace));
	}
	// 요청한 노드에 등록되어 있는 유저들 조회.
	public List<Map<String, Object>> selOrganRegUser(Map<String, Object> dbParamMap) throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selOrganRegUser", namespace), dbParamMap);
	}
	// 조직도 코드 정보조회
	public List<Map<String, Object>> selOrganCodeList() throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selOrganCodeList", namespace));
	}
	// 조직도 업데이트
	public int upOrganizaion(Map<String, Object> dbParamMap) throws Exception {
		return sqlSessionTemplate.update(String.format("%s.upOrganizaion", namespace), dbParamMap);
	}
	// 조직도 유저 등록유무 체크
	public int selChkOrganMember(Map<String, String> dbParamMap) throws Exception {
		return sqlSessionTemplate.selectOne(String.format("%s.isExistOrganMember", namespace), dbParamMap);
	}
	// 조직도회원 테이블 등록
	public void inOrganMember(Map<String, Object> dbParamMap) throws Exception {
		sqlSessionTemplate.insert(String.format("%s.inOrganMember", namespace), dbParamMap);
	}
	// 조직도에 등록되어 있는 유저 삭제
	public int delOrganMembers(Map<String, Object> dbParamMap) throws Exception {
		return sqlSessionTemplate.delete(String.format("%s.delOrganMembers", namespace), dbParamMap);
	}
	// 선택 조직도 삭제
	public List<OrganizationVo> delOrganOne(Map<String, Object> dbParamMap) throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.delOrganOne", namespace), dbParamMap);
	}
	// 가장 큰 조직도 아이디 구함.
	public OrganizationVo selMaxOrganId2(Map<String, String> dbParamMap) throws Exception {
		return sqlSessionTemplate.selectOne(String.format("%s.selMaxOrganId", namespace), dbParamMap);
	}
	public OrganizationVo selMaxOrganId(Map<String, Object> dbParamMap) throws Exception {
		return sqlSessionTemplate.selectOne(String.format("%s.selMaxOrganId", namespace), dbParamMap);
	}
	// 요청한 조직아이디로 조직 정보조회
	public OrganizationVo selOrganInfoOne(Map<String, String> dbParamMap) throws Exception {
		return sqlSessionTemplate.selectOne(String.format("%s.selOrganInfoOne", namespace), dbParamMap);
	}
	public int inOrganizaion(OrganizationVo organizationVo) throws Exception {
		return sqlSessionTemplate.insert(String.format("%s.inOrganization", namespace), organizationVo);
	}

	// 조직도회원 전체발송 조회
	public List<Map<String,Object>> selOranSendMember(Map<String, Object> dbParamMap) throws Exception {
		return sqlSessionTemplate.selectList(String.format("%s.selOranSendMember", namespace), dbParamMap);
	}

	// 조직도 회원 전체 카운트 조회
	public int selAllOrganMemberCnt() throws Exception {
		return sqlSessionTemplate.selectOne(String.format("%s.selAllOrganMemberCnt", namespace));
	}
}