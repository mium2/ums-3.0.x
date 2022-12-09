package kr.uracle.ums.core.dao.kko;

import kr.uracle.ums.codec.redis.vo.BaseProcessBean;

public interface IKkoFrtDao {
    public int inFrtSendMsg(BaseProcessBean baseKkoFrtProcessBean) throws Exception;
}
