package kr.uracle.ums.core.dao.rcs;

import kr.uracle.ums.codec.redis.vo.BaseProcessBean;

public interface IRcsDao {
    public int inRcsSend(BaseProcessBean baseProcessBean) throws Exception;
    public int inRcsImg(BaseProcessBean baseProcessBean) throws Exception;
}
