package kr.uracle.ums.core.dao.rcs;

import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import org.springframework.stereotype.Repository;

@Repository
public class KtRcsDao implements IRcsDao{
    @Override
    public int inRcsSend(BaseProcessBean baseProcessBean) throws Exception {
        throw new Exception("KT RCS는 현재 지원되지 않습니다.");
    }

    @Override
    public int inRcsImg(BaseProcessBean baseProcessBean) throws Exception {
        return 0;
    }

}
