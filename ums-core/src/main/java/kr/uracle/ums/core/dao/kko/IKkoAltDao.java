package kr.uracle.ums.core.dao.kko;

import kr.uracle.ums.codec.redis.vo.BaseProcessBean;

public interface IKkoAltDao {
    public int inAltSendMsg(BaseProcessBean baseKkoAltProcessBean) throws Exception;
}
