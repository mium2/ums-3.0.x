package kr.uracle.ums.core.exception;

import java.io.Serializable;

/**
 * Created by Y.B.H(mium2) on 2020. 1. 15..
 */
public class ExistUserException extends Exception implements Serializable {

    public ExistUserException(String message) {
        super(message);
    }
}