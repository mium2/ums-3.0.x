package kr.uracle.ums.core.dao.naver;

import kr.uracle.ums.codec.redis.vo.BaseProcessBean;

public interface INaverDao {
    public int inNaverSendMsg(BaseProcessBean baseNaverProcessBean) throws Exception;
}
