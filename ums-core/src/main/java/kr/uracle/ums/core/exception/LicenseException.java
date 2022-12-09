package kr.uracle.ums.core.exception;

import kr.uracle.ums.codec.redis.config.ErrorManager;

import java.io.Serializable;

public class LicenseException extends Exception implements Serializable {
    public LicenseException(String message) {
        super("라이센스에러 : "+message);
    }
    public String getErrorCode() {
        return ErrorManager.ERR_1100;
    }

}