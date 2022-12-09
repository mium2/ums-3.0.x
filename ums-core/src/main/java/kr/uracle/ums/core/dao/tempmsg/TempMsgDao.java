package kr.uracle.ums.core.dao.tempmsg;

import kr.uracle.ums.core.dao.BaseDao;
import kr.uracle.ums.core.vo.ReqUmsSendVo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TempMsgDao extends BaseDao {
    @Override
    public void afterPropertiesSet() throws Exception {
        namespace = "mybatis.tempmsg";
    }

    public int inUmsTempMsg(ReqUmsSendVo reqUmsTempVo) throws Exception{
        return sqlSessionTemplate.insert("mybatis.tempmsg.inUmsTempMsg", reqUmsTempVo);
    }

    public int upUmsTempMsg(ReqUmsSendVo reqUmsTempVo) throws Exception{
        return sqlSessionTemplate.update("mybatis.tempmsg.upUmsTempMsg", reqUmsTempVo);
    }

    public Map<String, Object> selUmsTempMsg(Map<String, Object> param) {
        return sqlSessionTemplate.selectOne("mybatis.tempmsg.selUmsTempMsg", param);
    }

    public List<Map<String, Object>> selUmsTempMsgList(Map<String, Object> param) {
        return sqlSessionTemplate.selectList("mybatis.tempmsg.selUmsTempMsg", param);
    }

    public int delUmsTempMsg(Map<String, Object> param) {
        return sqlSessionTemplate.delete("mybatis.tempmsg.delUmsTempMsg", param);
    }
}
