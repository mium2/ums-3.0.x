package kr.uracle.ums.core.dao.status;

import org.springframework.stereotype.Repository;

@Repository
public class StatusLagacyDao extends BaseStatusDao {
	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.status.lagacy";
	}
}