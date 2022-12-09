package kr.uracle.ums.core.dao.common;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Created by Y.B.H(mium2) on 2019. 3. 4..
 */
@Repository
public class MemberPopDao {
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;

    public List<Map<String,Object>> selPopupMember(Map<String, Object> param) throws Exception{
        return sqlSessionTemplate.selectList("mybatis.common.selPopupMember",param);
    }

    public List<Map<String,Object>> selSearchAllMember(Map<String, Object> dbParamMap) throws Exception {
        return sqlSessionTemplate.selectList("mybatis.common.selSearchAllMember",dbParamMap);
    }
}
