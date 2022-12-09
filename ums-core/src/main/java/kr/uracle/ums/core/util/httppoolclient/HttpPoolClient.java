package kr.uracle.ums.core.util.httppoolclient;

import com.google.gson.Gson;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
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
import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by Y.B.H(mium2) on 17. 12. 13..
 */
public class HttpPoolClient {
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpPoolClient.class);
    private final String DEFAULT_CHARSET = "UTF-8";

    private int maxConPoolSize = 100;

    private PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

    private CloseableHttpClient httpClient;
    private CloseableHttpClient httpsClient;

    private IdleConnectionMonitorThread idleConnectionMonitorThread = null;

    private static HttpPoolClient instance;
    private Gson gson = new Gson();

    public synchronized static HttpPoolClient getInstance() throws Exception{
        if(instance==null){
            instance = new HttpPoolClient();
            LOGGER.info("========= HttpPoolClient new Instance()=========");
        }
        return instance;
    }

    private HttpPoolClient() throws Exception{
        init(maxConPoolSize);
    }

    public void init(int maxConPoolSize) throws Exception{
        cm.setMaxTotal(maxConPoolSize);
        cm.setDefaultMaxPerRoute(10);
        httpClient = getHttpClient();
        httpsClient = getHttpsClient();

        if(idleConnectionMonitorThread!=null && idleConnectionMonitorThread.isAlive()){
            idleConnectionMonitorThread.shutdown();
            Thread.sleep(1000);
        }

        idleConnectionMonitorThread = new IdleConnectionMonitorThread(cm);
        idleConnectionMonitorThread.start();
    }

    public void destory() throws Exception{
        if(idleConnectionMonitorThread!=null && idleConnectionMonitorThread.isAlive()){
            idleConnectionMonitorThread.shutdown();
        }
        cm.shutdown();
    }

    public ResponseBean sendPost(String url, Map<String,Object> reqParam) throws Exception{
        return sendPost(url,null,reqParam,null,DEFAULT_CHARSET);
    }

    public ResponseBean sendPost(String url, Map<String,Object> reqParam, RequestConfig requestConfig) throws Exception{
        return sendPost(url,null,reqParam,requestConfig,DEFAULT_CHARSET);
    }

    public ResponseBean sendPost(String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqParam) throws Exception{
        return sendPost(url,addHttpHeadParam,reqParam,null,DEFAULT_CHARSET);
    }

    public ResponseBean sendPost(String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqParam, RequestConfig requestConfig) throws Exception{
        return sendPost(url,addHttpHeadParam,reqParam,requestConfig,DEFAULT_CHARSET);
    }

    public ResponseBean sendPost(String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqPostParam, RequestConfig requestConfig, String charset) throws Exception{
        ResponseBean responseBean = new ResponseBean();
        HttpHost httpHost = getUrlParse(url);

        HttpPost httpPost = makeHttpPost(url, addHttpHeadParam, reqPostParam, requestConfig,charset);
        
        CloseableHttpResponse response = null;
        if(httpHost.getSchemeName().equals("https")) {
            response = httpsClient.execute(httpHost, httpPost);
        }else{
            response = httpClient.execute(httpHost, httpPost);
        }
        responseBean.setHeaders(response.getAllHeaders());
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

    public ResponseBean sendJsonPost(String url, Map<String,Object> reqParam) throws Exception{
        return sendJsonPost(url,null,reqParam,null,DEFAULT_CHARSET);
    }

    public ResponseBean sendJsonPost(String url, Map<String,Object> reqParam, RequestConfig requestConfig) throws Exception{
        return sendJsonPost(url,null,reqParam,requestConfig,DEFAULT_CHARSET);
    }

    public ResponseBean sendJsonPost(String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqParam) throws Exception{
        return sendJsonPost(url,addHttpHeadParam,reqParam,null,DEFAULT_CHARSET);
    }

    public ResponseBean sendJsonPost(String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqParam, RequestConfig requestConfig) throws Exception{
        return sendJsonPost(url,addHttpHeadParam,reqParam,requestConfig,DEFAULT_CHARSET);
    }

    public ResponseBean sendJsonPost(String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqPostParam, RequestConfig requestConfig, String charset) throws Exception{
        ResponseBean responseBean = new ResponseBean();
        HttpHost httpHost = getUrlParse(url);

        HttpPost httpPost = makeHttpJsonPost(url, addHttpHeadParam, reqPostParam, requestConfig,charset);

        CloseableHttpResponse response = null;
        if(httpHost.getSchemeName().equals("https")) {
            response = httpsClient.execute(httpHost, httpPost);
        }else{
            response = httpClient.execute(httpHost, httpPost);
        }
        responseBean.setHeaders(response.getAllHeaders());
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

    public ResponseBean sendMultipartPost(String url, Map<String,Object> reqParam, Map<String,File> fileMapParam) throws Exception{
        return sendMultipartPost(url, null, reqParam, fileMapParam, null, DEFAULT_CHARSET);
    }

    public ResponseBean sendMultipartPost(String url, Map<String,Object> reqParam, Map<String,File> fileMapParam, RequestConfig requestConfig) throws Exception{
        return sendMultipartPost(url, null, reqParam, fileMapParam, requestConfig, DEFAULT_CHARSET);
    }

    public ResponseBean sendMultipartPost(String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqParam, Map<String,File> fileMapParam) throws Exception{
        return sendMultipartPost(url, addHttpHeadParam, reqParam, fileMapParam, null, DEFAULT_CHARSET);
    }

    public ResponseBean sendMultipartPost(String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqParam, Map<String,File> fileMapParam, RequestConfig requestConfig) throws Exception{
        return sendMultipartPost(url, addHttpHeadParam, reqParam, fileMapParam, requestConfig, DEFAULT_CHARSET);
    }

    public ResponseBean sendMultipartPost(String url, Map<String,String> addHttpHeadParam, Map<String,Object> reqPostParam, Map<String,File> fileMapParam, RequestConfig requestConfig, String charset) throws Exception {
        ResponseBean responseBean = new ResponseBean();
        HttpHost httpHost = getUrlParse(url);

        HttpPost httpPost = makeMultipartPost(url, addHttpHeadParam, reqPostParam, fileMapParam, requestConfig,charset);

        CloseableHttpResponse response = null;
        if(httpHost.getSchemeName().equals("https")) {
            response = httpsClient.execute(httpHost, httpPost);
        }else{
            response = httpClient.execute(httpHost, httpPost);
        }
        responseBean.setHeaders(response.getAllHeaders());
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

    private HttpPost makeMultipartPost(final String url,final Map<String,String> addHttpHeadParam,final Map<String,Object> reqParam,final Map<String,File> fileMapParam,RequestConfig requestConfig, String charset){
        HttpPost httpPost = new HttpPost(url);

        if(requestConfig!=null) {
            httpPost.setConfig(requestConfig);
        }
        if(addHttpHeadParam!=null){
            for (Map.Entry<String, String> headParam : addHttpHeadParam.entrySet()) {
                httpPost.addHeader(headParam.getKey(), headParam.getValue().toString());
            }
        }

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        Charset chars = Charset.forName(charset);
        builder.setCharset(chars);
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        ContentType contentType = ContentType.create("text/plain", chars);
        if(reqParam!=null) {
            for (Map.Entry<String, Object> parameter : reqParam.entrySet()) {
                builder.addPart(parameter.getKey(), new StringBody(parameter.getValue().toString(), contentType));
            }
        }
        if(fileMapParam!=null) {
            for (Map.Entry<String, File> fileMapEntry : fileMapParam.entrySet()) {
                builder.addPart(fileMapEntry.getKey(), new FileBody(fileMapEntry.getValue()));
            }
        }
        HttpEntity entity = builder.build();
        httpPost.setEntity(entity);

        return httpPost;
    }

    public ResponseBean sendGet(String url) throws Exception{
        return sendGet(url,null,null,DEFAULT_CHARSET);
    }

    public ResponseBean sendGet(String url, Map<String,String> addHttpHeadParam) throws Exception{
        return sendGet(url,addHttpHeadParam,null,DEFAULT_CHARSET);
    }

    public ResponseBean sendGet(String url, Map<String,String> addHttpHeadParam, String charset) throws Exception{
        return sendGet(url,addHttpHeadParam,null,charset);
    }

    public ResponseBean sendGet(String url, String charset) throws Exception{
        return sendGet(url,null,null,charset);
    }

    public ResponseBean sendGet(String url, Map<String,String> addHttpHeadParam, RequestConfig requestConfig, String charset) throws Exception{
        ResponseBean responseBean = new ResponseBean();
        HttpHost httpHost = getUrlParse(url);

        HttpGet httpGet = makeHttpGet(url,addHttpHeadParam,requestConfig);
        CloseableHttpResponse response = null;
        if(httpHost.getSchemeName().equals("https")) {
            response = httpsClient.execute(httpHost, httpGet);
        }else{
            response = httpClient.execute(httpHost, httpGet);
        }

        responseBean.setHeaders(response.getAllHeaders());
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
    
	/*
	 * 
	 * Created by K.K.B on 21. 11. 01.
	 * private synchronized CloseableHttpClient httpClient3() { // 소켓 튜닝
	 * SocketConfig sc = SocketConfig.custom() .setSoTimeout(2000)
	 * .setSoKeepAlive(true) .setTcpNoDelay(true) .setSoReuseAddress(true) .build();
	 * 
	 * return
	 * HttpClients.custom().setDefaultSocketConfig(sc).setConnectionManager(cm).
	 * build(); }
	 */

	private synchronized CloseableHttpClient getHttpsClient() throws Exception {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLSv1.2");
        }catch (NoSuchAlgorithmException e){
            sslContext = SSLContext.getInstance("SSL");
        }
		// set up a TrustManager that trusts everything
		sslContext.init(null, new TrustManager[] { new X509TrustManager() {
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
		return HttpClients.custom().setConnectionManager(cm).setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext)).build();
	}
    
//    private synchronized CloseableHttpClient getHttpsClient() throws Exception{
//
//        SSLContext sslContext = SSLContext.getInstance("SSL");
//        // set up a TrustManager that trusts everything
//        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
//            public X509Certificate[] getAcceptedIssuers() {
//                LOGGER.debug("getAcceptedIssuers =============");
//                return null;
//            }
//
//            public void checkClientTrusted(X509Certificate[] certs, String authType) {
//                LOGGER.debug("checkClientTrusted =============");
//            }
//
//            public void checkServerTrusted(X509Certificate[] certs, String authType) {
//                LOGGER.debug("checkServerTrusted =============");
//            }
//        }}, new SecureRandom());
//        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
//
//        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).setSSLSocketFactory(sslsf).build();
//
//        return httpClient;
//    }

    private HttpPost makeHttpPost(final String url,final Map<String,String> addHttpHeadParam,final Map<String,Object> reqParam, RequestConfig requestConfig, String charset) throws Exception{
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
        return httpPost;
    }
    private HttpPost makeHttpJsonPost(final String url,final Map<String,String> addHttpHeadParam,final Map<String,Object> reqParam, RequestConfig requestConfig, String charset) throws Exception{
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
            StringEntity entity = new StringEntity(gson.toJson(reqParam),charset);
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
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
