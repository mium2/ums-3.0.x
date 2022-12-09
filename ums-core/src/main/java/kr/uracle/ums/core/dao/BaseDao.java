package kr.uracle.ums.core.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseDao implements InitializingBean {
	@Autowired(required = true)
	protected SqlSessionTemplate sqlSessionTemplate;
	protected String namespace;
}
