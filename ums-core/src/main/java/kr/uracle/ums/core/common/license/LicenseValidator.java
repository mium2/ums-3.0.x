package kr.uracle.ums.core.common.license;

import kr.uracle.ums.core.common.license.exception.InvalidLicenseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Y.B.H(mium2) on 16. 4. 26..
 */
public class LicenseValidator implements ResourceLoaderAware{
    private final static Logger log = LoggerFactory.getLogger("kr.uracle.ums.common.license");
    private final static String LICENSE_APP_ID_PROPERTY_NAME = "application_id";
    private final static String LICENSE_EXPIRATION_DATE_PROPERTY_NAME = "expiration_date";
    private final static String LICENSE_IP_ADDRESS_PROPERTY_NAME = "ip_address";
    private final static int SECRET_KEY_LENGTH = 16;

    private String licenseFileSrc = "WEB-INF/classes/config/license.key";

    private String indexFileSrc = "WEB-INF/classes/config/license.cer";

    private ResourceLoader resourceLoader;

    private Properties licenseProp = new Properties();

    private volatile HashMap<String, Object> m_licenseInfoMap = new HashMap<String, Object>();

    private static LicenseValidator instance = new LicenseValidator();

    private HashSet<String> appIdSet = new HashSet<String>();

    public static LicenseValidator getInstance() {
        return instance;
    }

    private LicenseValidator(){}


    public void setLicenseFileDir(String homeDir) throws Exception{

        File licenseFile = ResourceUtils.getFile("classpath:/config/license.key");
        File indexFile = ResourceUtils.getFile("classpath:/config/license.cer");
        this.licenseFileSrc = licenseFile.getAbsolutePath();
        this.indexFileSrc = indexFile.getAbsolutePath();
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public synchronized void initialize() throws Exception {
        File licenseFile = null;
        File indexFile = null;
        try {
            licenseFile = new File(licenseFileSrc);
            indexFile = new File(indexFileSrc);
        }catch (Exception e){}

        if(licenseFile==null  || indexFile==null){
            throw new FileNotFoundException(String.format("Please use the license file as received from Uracle. [%s , %s]", licenseFile, indexFile));
        }


        byte[] hash = MessageDigestUtils.getMD5Hash(new FileInputStream(licenseFile));
        byte[] indexDecrypted = CryptoUtils.decrypt(hash, new FileInputStream(indexFile));

        licenseProp.load(new FileInputStream(licenseFile));
        if(!(licenseProp.containsKey(LICENSE_APP_ID_PROPERTY_NAME) &&
            (licenseProp.containsKey(LICENSE_EXPIRATION_DATE_PROPERTY_NAME) || licenseProp.containsKey(LICENSE_IP_ADDRESS_PROPERTY_NAME)))) {
            throw new InvalidLicenseException("This is an INVALID license file.");
        }

        ByteArrayInputStream indexIs = new ByteArrayInputStream(indexDecrypted);
        Properties indexProp = new Properties();
        indexProp.load(indexIs);


        // ----------------------------------------------------------------------------------------
        // DEBUG
        // ----------------------------------------------------------------------------------------
        Enumeration<Object> keys = indexProp.keys();
        while(keys.hasMoreElements())
        {
            Object key = keys.nextElement();
//            log.debug("["+key+"]=" + indexProp.get(key));
        }
        // ------------------------------------------------

        String appidList = licenseProp.getProperty(LICENSE_APP_ID_PROPERTY_NAME);
        CollectionUtils.mergePropertiesIntoMap(licenseProp, m_licenseInfoMap);

        m_licenseInfoMap.put("LICENSE_APP_ID_PROPERTY_NAME", appidList);
        String[] applications = StringUtils.tokenizeToStringArray(appidList, "; \t");
        for(String application : applications) {
            // app아이디 hashset에 저장
            appIdSet.add(application);
            if (!(indexProp.containsKey(application) &&
                licenseProp.containsKey(application + ".sn") &&
                licenseProp.containsKey(application + ".mpsn"))) {

                // ----------------------------------------------------------------------------------------
                // DEBUG
                // ----------------------------------------------------------------------------------------
                if(!indexProp.containsKey(application) )
                {
                    log.error("appid cannot find!! : [" + application + "]");
                }
                if(!indexProp.containsKey(application + ".sn") )
                {
                    log.error("sn cannot find!! ["+application+"]");
                }
                if(!indexProp.containsKey(application+ ".mpsn"))
                {
                    log.error("mpsn cannot find!! ["+application+"]");
                }
                // ----------------------------------------------------------------------------------------
                throw new InvalidLicenseException("This is an INVALID license file. Could not find a valid certificate pair for this license. [app_id : "+application+"]");
            }

            String[] indexes = StringUtils.tokenizeToStringArray(indexProp.getProperty(application), ";, \t");
            String secretKey = parseSecretKey(indexes, licenseProp.getProperty(application + ".sn"));

            m_licenseInfoMap.put(application + ".secret", secretKey.getBytes());

            if(log.isDebugEnabled()) {
                log.debug("License loaded : '" + application + "'");
            }
        }
        appIdSet.add("com.upns.push.test");
    }

    public boolean validate() {
        String expiration = (String) m_licenseInfoMap.get(LICENSE_EXPIRATION_DATE_PROPERTY_NAME);
        if(!StringUtils.isEmpty(expiration)) {
            try {
                log.debug("### LICENSE_EXPIRATION_DATE:"+expiration);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Date today = new Date();
                if(!format.parse(expiration).after(today)) {
                    return false;
                }
            } catch (ParseException e) {
            }
        }

        if(!StringUtils.isEmpty(m_licenseInfoMap.get(LICENSE_IP_ADDRESS_PROPERTY_NAME))) {
            try {
                String[] licensedAddresses = StringUtils.tokenizeToStringArray((String) m_licenseInfoMap.get(LICENSE_IP_ADDRESS_PROPERTY_NAME), "; \t");
                Set<String> addresses = getInetAddresses();
                addresses.add("127.0.0.1");
                addresses.add("localhost");

                for(String licensedAddress : licensedAddresses) {
                    log.debug("### licensedAddress:"+licensedAddress);
                    if(addresses.contains(licensedAddress)) {
                        return true;
                    }
                }
            } catch (SocketException e) {
            }
        }

        return false;
    }

    public boolean validate(String appid, String serial) {
        Object oMpsn = m_licenseInfoMap.get(appid + ".mpsn");
        return oMpsn != null && oMpsn.equals(serial);
    }
    public boolean validate(String appid, byte[] serial) {
        String oMpsn = (String)m_licenseInfoMap.get(appid + ".mpsn");
        return oMpsn != null && Arrays.equals(oMpsn.getBytes(), serial);
    }

    public byte[] getSecretKey(String appid) {
        return (byte[]) this.m_licenseInfoMap.get(appid + ".secret");
    }

    public String[] getAppIds() {
        return StringUtils.tokenizeToStringArray((String) m_licenseInfoMap.get(LICENSE_APP_ID_PROPERTY_NAME), "; \t");
    }

    public HashSet<String> getAppIdSet(){
        return appIdSet;
    }

    public boolean chkAppid(String appid){
//        log.trace("#### appIdSet size : "+appIdSet.size());

        if(appIdSet.contains(appid)){
            return true;
        }else{
            return false;
        }
    }

    private Set<String> getInetAddresses() throws SocketException {
        Set<String> addresses = new HashSet<String>();
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for(NetworkInterface net : Collections.list(nets)) {
            Enumeration<InetAddress> inetAddresses = net.getInetAddresses();
            for(InetAddress inet : Collections.list(inetAddresses)) {
                if(!inet.isLoopbackAddress()) {
                    addresses.add(inet.getHostAddress());
                }
            }
        }
        return (addresses.isEmpty() ? null : addresses);
    }

    private String parseSecretKey(String[] indexes, String serial) {
        StringBuffer buffer = new StringBuffer(SECRET_KEY_LENGTH);
        for(String index : indexes) {
            int idx = Integer.parseInt(index);
            buffer.append(serial.substring(idx, idx+1));
        }
        return buffer.toString();
    }
}
