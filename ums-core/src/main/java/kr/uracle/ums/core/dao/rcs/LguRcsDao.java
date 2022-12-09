package kr.uracle.ums.core.dao.rcs;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.rcs.LguRcsProcessBean;
import kr.uracle.ums.core.extention.service.division.DivisionRollManager;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class LguRcsDao implements IRcsDao{
    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;
    @Autowired(required = true)
    private DivisionRollManager divisionRollManager;
    @Override
    public int inRcsSend(BaseProcessBean baseProcessBean) throws Exception {
    	LguRcsProcessBean rcsProcessBean = (LguRcsProcessBean)baseProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseProcessBean, SendType.RCS_SMS, rcsProcessBean.getPROVIDER());
        if(tableName!=null){
            rcsProcessBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.rcs.lgu.inRcsSendDynamic", rcsProcessBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.rcs.lgu.inRcsSend", rcsProcessBean);
        }
    }

    @Override
    public int inRcsImg(BaseProcessBean baseProcessBean) throws Exception {
    	LguRcsProcessBean rcsProcessBean = (LguRcsProcessBean)baseProcessBean;
        return sqlSessionTemplate.insert("mybatis.rcs.lgu.inRcsImg", rcsProcessBean);
    }

}
