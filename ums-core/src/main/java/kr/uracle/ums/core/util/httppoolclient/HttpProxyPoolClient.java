package kr.uracle.ums.core.util.httppoolclient;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by Y.B.H(mium2) on 17. 12. 19..
 */
public class HttpProxyPoolClient{
private final static Logger LOGGER = LoggerFactory.getLogger(HttpPoolClient.class);
private final String DEFAULT_CHARSET = "UTF-8";

private int maxConPoolSize = 100;

private PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

private CloseableHttpClient httpClient;
private CloseableHttpClient httpsClient;

private IdleConnectionMonitorThread idleProxyConnectionMonitorThread = null;

private static HttpProxyPoolClient instance;

    public synchronized static HttpProxyPoolClient getInstance() throws Exception{
        if(instance==null){
            instance = new HttpProxyPoolClient();
        }
        return instance;
    }

    private HttpProxyPoolClient() throws Exception{
        init(maxConPoolSize);
    }

    public void init(int maxConPoolSize) throws Exception{
        cm.setMaxTotal(maxConPoolSize);
        httpClient = getHttpClient();
        httpsClient = getHttpsClient();

        if(idleProxyConnectionMonitorThread.isAlive()){
            idleProxyConnectionMonitorThread.shutdown();
            Thread.sleep(500);
        }

        idleProxyConnectionMonitorThread = new IdleConnectionMonitorThread(cm);
        idleProxyConnectionMonitorThread.start();
    }

    public ResponseBean sendPorxyPost(HttpServletRequest request, HttpServletResponse response, String url, Map<String,Object> reqParam) throws Exception{
        return sendPorxyPost(request,response, url, null, reqParam, null, null, DEFAULT_CHARSET);
    }

    public ResponseBean sendPorxyPost(HttpServletRequest request, HttpServletResponse response, String url, Map<String,Object> reqParam, RequestConfig requestConfig) throws Exception{
        return sendPorxyPost(request,response, url, null, reqParam, null, requestConfig, DEFAULT_CHARSET);
    }

    public ResponseBean sendPorxyPost(HttpServletRequest request, HttpServletResponse response, String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqParam) throws Exception{
        return sendPorxyPost(request,response, url, addHttpHeadParam, reqParam, null, null, DEFAULT_CHARSET);
    }

    public ResponseBean sendPorxyPost(HttpServletRequest request, HttpServletResponse response, String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqParam, RequestConfig requestConfig) throws Exception{
        return sendPorxyPost(request,response, url, addHttpHeadParam, reqParam, null, requestConfig, DEFAULT_CHARSET);
    }

    public ResponseBean sendPorxyPost(HttpServletRequest proxyReq, HttpServletResponse proxyResp, String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqPostParam, List<Map<String,File>> uploadFileMaps, RequestConfig requestConfig, String charset) throws Exception{
        ResponseBean responseBean = new ResponseBean();
        HttpHost httpHost = getUrlParse(url);
        Enumeration<String> headerNames = proxyReq.getHeaderNames();

        String req_sessionID = "";
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if(headerName.toLowerCase().equals("cookie")) {
                for (Enumeration values = proxyReq.getHeaders(headerName); values.hasMoreElements();) {
                    req_sessionID = (String)values.nextElement();
                }
            }
        }
        if(!req_sessionID.equals("")){
            addHttpHeadParam.put("Cookie",req_sessionID);
        }
        HttpPost httpPost = makeHttpPost(url, addHttpHeadParam, reqPostParam, uploadFileMaps, requestConfig, charset);
        CloseableHttpResponse response = null;
        if(httpHost.getSchemeName().equals("https")) {
            response = httpsClient.execute(httpHost, httpPost);
        }else{
            response = httpClient.execute(httpHost, httpPost);
        }
        Header[] headers = response.getAllHeaders();
        if(response!=null) {
            for (Header header : headers) {
                LOGGER.trace("[HTTP SERVER] Response Key : " + header.getName() + " ,Value : " + header.getValue());
                if ("Set-Cookie".equals(header.getName())) {
                    // http 서버에서 세션아이드를 재발급 했을 경우 다시 클라이언트에 세션아이디를 알려준다.
                    proxyResp.setHeader("Set-Cookie", header.getValue()); // http 연결 서버로 부터 받은 세션아이디를 단말에 넘겨 주기 위해
                }
            }
        }

        responseBean.setHeaders(headers);
        responseBean.setStatusCode(response.getStatusLine().getStatusCode());
        String responseContent = "";
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                byte[] bytes = EntityUtils.toByteArray(entity);
                responseContent = new String(bytes,charset);
                responseBean.setBody(responseContent);
            }
        } finally {
            response.close();
        }

        return responseBean;
    }

    public ResponseBean sendProxyGet(HttpServletRequest request, HttpServletResponse response,String url) throws Exception{
        return sendProxyGet(request,response,url, null, null, DEFAULT_CHARSET);
    }

    public ResponseBean sendProxyGet(HttpServletRequest request, HttpServletResponse response,String url, Map<String,String> addHttpHeadParam) throws Exception{
        return sendProxyGet(request,response,url, addHttpHeadParam, null, DEFAULT_CHARSET);
    }

    public ResponseBean sendProxyGet(HttpServletRequest request, HttpServletResponse response,String url, Map<String,String> addHttpHeadParam, String charset) throws Exception{
        return sendProxyGet(request,response,url, addHttpHeadParam, null, charset);
    }

    public ResponseBean sendProxyGet(HttpServletRequest request, HttpServletResponse response,String url, String charset) throws Exception{
        return sendProxyGet(request,response,url, null, null, charset);
    }

    public ResponseBean sendProxyGet(HttpServletRequest proxyReq, HttpServletResponse proxyResp, String url, Map<String,String> addHttpHeadParam, RequestConfig requestConfig, String charset) throws Exception{
        ResponseBean responseBean = new ResponseBean();
        HttpHost httpHost = getUrlParse(url);

        Enumeration<String> headerNames = proxyReq.getHeaderNames();

        String req_sessionID = "";
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if(headerName.toLowerCase().equals("cookie")) {
                for (Enumeration values = proxyReq.getHeaders(headerName); values.hasMoreElements();) {
                    req_sessionID = (String)values.nextElement();
                }
            }
        }
        if(!req_sessionID.equals("")){
            addHttpHeadParam.put("Cookie",req_sessionID);
        }

        HttpGet httpGet = makeHttpGet(url,addHttpHeadParam,requestConfig);
        CloseableHttpResponse response = null;
        if(httpHost.getSchemeName().equals("https")) {
            response = httpsClient.execute(httpHost, httpGet);
        }else{
            response = httpClient.execute(httpHost, httpGet);
        }

        Header[] headers = response.getAllHeaders();
        if(response!=null) {
            for (Header header : headers) {
                LOGGER.trace("[HTTP SERVER] Response Key : " + header.getName() + " ,Value : " + header.getValue());
                if ("Set-Cookie".equals(header.getName())) {
                    // http 서버에서 세션아이드를 재발급 했을 경우 다시 클라이언트에 세션아이디를 알려준다.
                    proxyResp.setHeader("Set-Cookie", header.getValue()); // http 연결 서버로 부터 받은 세션아이디를 단말에 넘겨 주기 위해
                }
            }
        }

        responseBean.setHeaders(headers);
        responseBean.setStatusCode(response.getStatusLine().getStatusCode());
        String responseContent = "";
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                byte[] bytes = EntityUtils.toByteArray(entity);
                responseContent = new String(bytes,charset);
                responseBean.setBody(responseContent);
            }
        } finally {
            response.close();
        }

        return responseBean;
    }


    private synchronized CloseableHttpClient getHttpClient(){
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .build();
        return httpClient;
    }

    private synchronized CloseableHttpClient getHttpsClient() throws Exception{

//        SSLContext sslContext = SSLContext.getInstance("SSL");
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        // set up a TrustManager that trusts everything
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                LOGGER.debug("getAcceptedIssuers =============");
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                LOGGER.debug("checkClientTrusted =============");
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                LOGGER.debug("checkServerTrusted =============");
            }
        }}, new SecureRandom());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .setSSLSocketFactory(sslsf)
            .build();

        return httpClient;
    }

    private HttpPost makeHttpPost(final String url,final Map<String,String> addHttpHeadParam,final Map<String,Object> reqParam, List<Map<String,File>> uploadFileMaps, RequestConfig requestConfig, String charset) throws Exception{
        HttpPost httpPost = new HttpPost(url);
        if(requestConfig!=null) {
            httpPost.setConfig(requestConfig);
        }
        if(addHttpHeadParam!=null){
            for (Map.Entry<String, String> headParam : addHttpHeadParam.entrySet()) {
                httpPost.addHeader(headParam.getKey(), headParam.getValue().toString());
            }
        }

        if(reqParam!=null) {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (Map.Entry<String, Object> parameter : reqParam.entrySet()) {
                nvps.add(new BasicNameValuePair(parameter.getKey(), parameter.getValue().toString()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, charset));
        }

        //Multipart Entity Builder 생성
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

        // 파라미터 Entity 추가
        if(reqParam!=null) {
            for (Map.Entry<String, Object> parameter : reqParam.entrySet()) {
                multipartEntityBuilder.addPart(parameter.getKey(),new StringBody(parameter.getValue().toString(), ContentType.TEXT_PLAIN));
            }
        }

        // 파일 Entity 추가
        for(Map<String,File> upFileMap : uploadFileMaps) {
            Set<Map.Entry<String,File>> entrySet = upFileMap.entrySet();
            for(Map.Entry<String,File> mapEntry : entrySet){
                ContentBody fileBody = new FileBody(mapEntry.getValue());
                multipartEntityBuilder.addPart(mapEntry.getKey(),fileBody);
            }
//            multipartEntityBuilder.addBinaryBody("attachment", IOUtils.toByteArray(inputStream), ContentType.APPLICATION_OCTET_STREAM, fileName);
        }

        HttpEntity httpMultipartEntity = multipartEntityBuilder.build();
        if(httpMultipartEntity!=null) {
            httpPost.setEntity(httpMultipartEntity);
        }

        return httpPost;
    }

    private HttpGet makeHttpGet(final String url,final Map<String,String> addHttpHeadParam,final RequestConfig requestConfig) throws Exception{
        HttpGet httpGet = new HttpGet(url);
        if(requestConfig!=null) {
            httpGet.setConfig(requestConfig);
        }
        if(addHttpHeadParam!=null){
            for (Map.Entry<String, String> headParam : addHttpHeadParam.entrySet()) {
                httpGet.addHeader(headParam.getKey(), headParam.getValue().toString());
            }
        }
        return httpGet;
    }


    private String getProtocolParse(String url) throws Exception {
        String protocol = "http";
        int findIndex = url.indexOf("://");
        if(findIndex>0){
            protocol = url.substring(0,findIndex).toLowerCase();
        }
        return protocol;
    }

    private HttpHost getUrlParse(String url) throws Exception{
        int findIndex = url.indexOf("://");
        if(findIndex>0){
            String protocol = url.substring(0,findIndex);  // http or https일 경우는 포트를 명시 하지 않을 경우 default 값 80 or 443으로 셋팅한다.
            String fullHostAddressAndUri = url.substring(findIndex+3); // +3 이유 "://" 다음 문자 부터 가져오기 위해.
            String chkProtocol = protocol.trim().toLowerCase();

            String hostNameAndPort = fullHostAddressAndUri; //url이  http://google.com 이렇게만 올수 있기 때문에 그러면 hostNameAndPort에 기본인 google.com을 저장해 둔다.
            //http or https는 컨텍스트 루트가 있을 수 있고 포트가 없을 수 있다.
            int ctxRootChkIndex = fullHostAddressAndUri.indexOf("/");
            if(ctxRootChkIndex>0){
                hostNameAndPort = fullHostAddressAndUri.substring(0,ctxRootChkIndex);
            }

            String hostName = "";
            int hostPort = 0;
            int portChkIndex = hostNameAndPort.indexOf(":");
            // 포트가 없을 경우 http, https는 기본포트로 셋팅
            if(portChkIndex<0){
                if(chkProtocol.equals("http")){
                    hostName = hostNameAndPort;
                    hostPort = 80;
                }else if(chkProtocol.equals("https")){
                    hostName = hostNameAndPort;
                    hostPort = 443;
                }else{
                    throw new UnknownHostException(url);
                }
            }else{
                String[] conAddrsssArr = hostNameAndPort.split(":");
                hostName = conAddrsssArr[0];
                hostPort = Integer.parseInt(conAddrsssArr[1]);
            }

            //분리해 낸 아이피 포트 검증
            if(!hostName.equals("") && hostPort!=0) {
                SocketAddress socketAddress = new InetSocketAddress(hostName, hostPort);
                HttpHost httpHost = new HttpHost(hostName, hostPort, protocol);
                return httpHost;
            }else{
                throw new UnknownHostException(url);
            }

        }else{
            throw new UnknownHostException(url);
        }
    }
}
