package kr.uracle.ums.core.util.httppoolclient;

import org.apache.http.Header;

/**
 * Created by Y.B.H(mium2) on 17. 12. 15..
 */
public class ResponseBean {
    private int statusCode = 200;
    private Header[] headers;
    private String body;

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public void setHeaders(Header[] headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
