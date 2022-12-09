package kr.uracle.ums.core.dao.manage;

import kr.uracle.ums.core.ehcache.PreventIdCacheMgr;
import kr.uracle.ums.core.ehcache.PreventMobileCacheMgr;
import kr.uracle.ums.core.exception.ExistUserException;
import kr.uracle.ums.core.service.bean.PreventUserBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Y.B.H(mium2) on 2020. 1. 13..
 */
@Repository
public class PreventSendUserDao {
    @Autowired(required = true)
    protected SqlSessionTemplate sqlSessionTemplate;

    @Autowired(required = true)
    PreventIdCacheMgr preventIdCacheMgr;
    @Autowired(required = true)
    PreventMobileCacheMgr preventMobileCacheMgr;

    protected String namespace;

    public List<PreventUserBean> selectPreventUsers(Map<String, Object> param) throws Exception{
        return sqlSessionTemplate.selectList("mybatis.manage.preventUser.selectPreventUsers", param);
    }

    public int checkPreventUsersFromID(String USERID) throws Exception{
        int applyRow = sqlSessionTemplate.insert("mybatis.manage.preventUser.checkPreventUsersFromID", USERID);
        return applyRow;
    }

    public int insertPreventUser(PreventUserBean preventUserBean, Set<String> rejectChannelSet) throws Exception{
        int existRow = sqlSessionTemplate.selectOne("mybatis.manage.preventUser.checkPreventUsersFromID",preventUserBean.getUSERID());
        if(existRow>0){
            throw new ExistUserException("이미 등록된 아이디입니다");
        }
        int applyRow = 0;
        try {
            applyRow = sqlSessionTemplate.insert("mybatis.manage.preventUser.insertPreventUser", preventUserBean);
            preventIdCacheMgr.putCache(preventUserBean.getUSERID(), rejectChannelSet);
            preventMobileCacheMgr.putCache(preventUserBean.getMOBILE(), rejectChannelSet);
        }catch (DuplicateKeyException ex){
            throw  new ExistUserException("이미 등록된 핸드폰번호입니다.");
        }
        return applyRow;
    }

    public Object deletePreventUser(Map<String, Object> param) throws Exception{
        return sqlSessionTemplate.delete("mybatis.manage.preventUser.delPreventUser", param);
    }

    public Object editPreventUser(Map<String, Object> param) throws Exception{
        return sqlSessionTemplate.update("mybatis.manage.preventUser.updatePreventUser", param);
    }
}
