package kr.uracle.ums.core.dao.kko;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.kko.LgcnsKkoFrtProcessBean;
import kr.uracle.ums.core.extention.service.division.DivisionRollManager;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class LgcnsKkoFrtDao implements IKkoFrtDao{
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;
    @Autowired(required = true)
    private DivisionRollManager divisionRollManager;
	@Override
    public int inFrtSendMsg(BaseProcessBean baseKkoFrtProcessBean) throws Exception{
        LgcnsKkoFrtProcessBean lgcnsKkoFrtProcessBean = (LgcnsKkoFrtProcessBean)baseKkoFrtProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseKkoFrtProcessBean, SendType.KKOFRT, lgcnsKkoFrtProcessBean.getPROVIDER());
        if(tableName!=null){
            lgcnsKkoFrtProcessBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.friendtolk.lgcns.inFrtSendMsgDynamic", lgcnsKkoFrtProcessBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.friendtolk.lgcns.inFrtSendMsg", lgcnsKkoFrtProcessBean);
        }
    }

}
