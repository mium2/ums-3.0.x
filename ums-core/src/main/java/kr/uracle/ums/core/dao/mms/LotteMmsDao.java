package kr.uracle.ums.core.dao.mms;

import kr.uracle.ums.codec.redis.enums.SendType;
import kr.uracle.ums.codec.redis.vo.BaseProcessBean;
import kr.uracle.ums.codec.redis.vo.mms.LotteMmsProcessBean;
import kr.uracle.ums.core.extention.service.division.DivisionRollManager;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class LotteMmsDao implements IMmsDao{

    @Autowired(required = true)
    @Qualifier("sqlSessionTemplate")
    SqlSessionTemplate sqlSessionTemplate;

    @Autowired(required = true)
    private DivisionRollManager divisionRollManager;
    @Override
    public int inMmsSendMsg(BaseProcessBean baseMmsProcessBean) throws Exception {
        LotteMmsProcessBean prcsBean = (LotteMmsProcessBean)baseMmsProcessBean;
        String tableName = divisionRollManager.getPutTableName(baseMmsProcessBean, SendType.MMS, prcsBean.getPROVIDER());
        if(tableName!=null){
            prcsBean.setTABLE_NAME(tableName);
            return sqlSessionTemplate.insert("mybatis.mms.lotte.inMmsSendDynamic", prcsBean);
        }else {
            return sqlSessionTemplate.insert("mybatis.mms.lotte.inMmsSend", prcsBean);
        }
    }
}
