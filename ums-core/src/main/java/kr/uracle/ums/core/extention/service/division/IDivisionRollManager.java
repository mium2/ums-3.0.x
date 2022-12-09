package kr.uracle.ums.core.extention.service.division;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;

public interface IDivisionRollManager {
    public String getPutTableName(BaseProcessBean baseProcessBean, SendType sendType, String channelProvider);
}
