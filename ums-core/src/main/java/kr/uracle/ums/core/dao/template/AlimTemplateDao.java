package kr.uracle.ums.core.dao.template;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;

import kr.uracle.ums.core.dao.BaseDao;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Repository
public class AlimTemplateDao extends BaseDao {
	@Autowired(required = true)
	DataSourceTransactionManager dataSourceTransactionManager;

	@Override
	public void afterPropertiesSet() throws Exception {
		namespace = "mybatis.template.allimtolk";
	}
	public Map<String, Object> selectSince() {
		return sqlSessionTemplate.selectOne(String.format("%s.selTemplateSince", namespace));
	}
	public Map<String, Object> selectOne(Map<String,Object> param) {
		return sqlSessionTemplate.selectOne(String.format("%s.selectTemplate", namespace), param);
	}
	public List<Map<String, Object>> selectList(Map<String,Object> param) {
		return sqlSessionTemplate.selectList(String.format("%s.selectTemplate", namespace), param);
	}
	public int insert(Map<String,Object> param) {
		return sqlSessionTemplate.insert(String.format("%s.insTemplateReg", namespace), param);
	}
	public int update(Map<String,Object> param) {
		return sqlSessionTemplate.update(String.format("%s.syncTemplate", namespace), param);
	}

	public void upRelease(List<String> KKOBIZCODES) throws Exception{
		//RDB 트랜젝션
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		TransactionStatus status = dataSourceTransactionManager.getTransaction(def);

		try {
			for (String KKOBIZCODE : KKOBIZCODES) {
				sqlSessionTemplate.update(String.format("%s.upRelease", namespace),KKOBIZCODE);
			} //for문 마침
			dataSourceTransactionManager.commit(status);

		}catch (Exception e){
			dataSourceTransactionManager.rollback(status);
			e.printStackTrace();
			throw new SQLException(e.toString());
		}
	}
}