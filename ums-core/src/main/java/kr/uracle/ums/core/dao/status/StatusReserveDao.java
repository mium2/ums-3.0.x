package kr.uracle.ums.core.dao.status;

import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public class StatusReserveDao extends BaseStatusDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.reserve";
	}
	public int delete(Map<String,Object> param) {
		return sqlSessionTemplate.delete(String.format("%s.delStatusReserve", namespace), param);
	}
}