package kr.uracle.ums.core.dao.common;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 4. 16..
 */
@Repository
public class InitDataDao {
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;

    public List<Map<String,Object>> selCallBackNum(Map<String, Object> param) throws Exception{
        return sqlSessionTemplate.selectList("mybatis.common.selCallBackNum", param);
    }

    public List<Map<String,Object>> selMacroCodeInfo(Map<String, Object> param) throws Exception{
        return sqlSessionTemplate.selectList("mybatis.common.selMacroCode", param);
    }
}
