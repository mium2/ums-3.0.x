package kr.uracle.ums.core.exception;

import java.io.Serializable;

import kr.uracle.ums.codec.redis.config.ErrorManager;

public class MacroCodeException extends Exception implements Serializable {

    public MacroCodeException(String message) {
        super(message);
    }
    public String getErrorCode() {
        return ErrorManager.ERR_1200;
    }
}
