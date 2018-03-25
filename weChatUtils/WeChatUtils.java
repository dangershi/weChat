import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aiiju.util.common.PropertiesUtil;
import com.aiiju.util.http.StringUtils;
import com.aiiju.util.redis.JedisUtil;
import com.alibaba.fastjson.JSONObject;

/**
 * 	@ClassName: WeiXinUtils.java
 *  @Description: 微信公众号和小程序的通用工具类（部分内容涉及保密信息未上传）
 *  @author: 鹿丸
 */
public class WeiXinUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(WeiXinUtils.class);
	//公众号appid和secret
	private static final String APPID = PropertiesUtil.getPropertyByKey("WECHAT_APPID");
	private static final String APPSECRET = PropertiesUtil.getPropertyByKey("WECHAT_SECRET");
	//作为微信第三方平台开发的appid
    private static final String COMPONENT_APPID = PropertiesUtil.getPropertyByKey("COMPONENT_APPID");
    //作为小程序第三方开发的appid
    private static final String SP_COMPONENT_APPID = PropertiesUtil.getPropertyByKey("SP_COMPONENT_APPID");
	

    /**
     * 调用微信接口获取请求用户信息的access_token
     * @param code 用来换取用户token的code
     */
	public static JSONObject getUserInfoAccessToken(String code) {
        JSONObject json = null;
        try {
            String url = String.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
            		APPID, APPSECRET, code);
            logger.info("请求access_token，参数URL："+url);
            String tokens = sendHttpGetRequest(url);
            json = JSONObject.parseObject(tokens, JSONObject.class);
            logger.info("请求access_token成功. [result={}]", json);
        } catch (Exception e) {
            logger.error("请求access_token失败. [error={}]", e);
        }
        return json;
    }
	
    /**
     * 调用微信接口获取用户信息(昵称,头像等)
     * @param accessToken
     * @param openId
     */
    public static JSONObject getUserInfo(String accessToken, String openId) {
        String url = "https://api.weixin.qq.com/sns/userinfo?access_token=" + accessToken + "&openid=" + openId + "&lang=zh_CN";
        JSONObject userInfo = null;
        try {
            String tokens = sendHttpGetRequest(url);
            userInfo = JSONObject.parseObject(tokens, JSONObject.class);
            logger.info("请求用户信息成功. [result={}]", userInfo);
        } catch (Exception e) {
            logger.error("请求用户信息失败. [error={}]", e);
        }
        return userInfo;
    }
    
    @SuppressWarnings("resource")
	public static String sendHttpGetRequest(String url) throws Exception{
    	HttpGet httpGet = new HttpGet(url);
        HttpClient httpclient = new DefaultHttpClient(); 
        HttpResponse response = httpclient.execute(httpGet);
        HttpEntity httpEntity = response.getEntity();
        String result = EntityUtils.toString(httpEntity, "utf-8");
        return result;
    }
    
    /**
     * 获取公众号的全局access_token
     */
    public static String getBaseAccessToken() throws Exception{ 
    	JedisUtil jedis = JedisUtil.getInstance();
    	String baseAccessToken = jedis.get("WECHAT_ACCESS_TOKEN");
    	if(StringUtils.isNotBlank(baseAccessToken)){
    		return baseAccessToken;
    	}else{
    		//缓存中没有,或已经失效
		    String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="+APPID+"&secret="+ APPSECRET;	    
		    String res = sendHttpGetRequest(url);
		    logger.info("向微信获取access_token,返回={}",res);
		    JSONObject jsonObj = JSONObject.parseObject(res);
		    baseAccessToken = jsonObj.getString("access_token");
		    Integer expiresTime = Integer.parseInt(jsonObj.getString("expires_in"));
		    //将baseAccessToken缓存，并提前半小时刷新
		    jedis.set("WECHAT_ACCESS_TOKEN", baseAccessToken, expiresTime - 60 * 30);
    	}
    	  logger.info("程序获取获取access_token,返回={}",baseAccessToken);
    	return baseAccessToken;
    }
    
    
    /**
     * 获取全局的jsapi_ticket
     */
    public static String getJsapiTicket() throws Exception{
    	JedisUtil jedis = JedisUtil.getInstance();
    	String jsapiTicket = jedis.get("WECHAT_JS_API_TICKET");
    	if(StringUtils.isNotBlank(jsapiTicket)){
    		return jsapiTicket;
    	}else{
    		//缓存中没有,或已经失效
	    	String baseAccessToken = getBaseAccessToken();
	    	String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token="+ baseAccessToken +"&type=jsapi";
	    	String res = sendHttpGetRequest(url);
	    	JSONObject jsonObj = JSONObject.parseObject(res);
	    	jsapiTicket = jsonObj.getString("ticket");
	    	Integer expiresTime = Integer.parseInt(jsonObj.getString("expires_in"));
	    	//将jsapiTicket缓存
    		jedis.set("WECHAT_JS_API_TICKET", jsapiTicket, expiresTime-1800);
    	}
    	return jsapiTicket;
    }
    
    /**
     * 生成JS调用微信接口的签名参数
     * @param url当前页面的URL
     */
    public static JSONObject createSignature(String url) throws Exception{
    	String signature = "jsapi_ticket="+getJsapiTicket();
    	String noncestr = UUID.randomUUID().toString().replaceAll("-", "");
    	long timestamp = System.currentTimeMillis()/1000;
    	signature = signature+"&noncestr="+noncestr+"&timestamp="+timestamp+"&url="+url;
    	//根据jsapi_ticket等参数进行SHA1加密
    	signature = SHA1(signature);
    	JSONObject json = new JSONObject();
    	json.put("appid", APPID);
    	json.put("noncestr", noncestr);
    	json.put("signature", signature.toLowerCase());
    	json.put("timestamp", timestamp);
    	return json;
    }
    
    /**
     * SHA1 加密算法
     * @param str
     */
    public static String SHA1(String str) {
        try {
        	//如果是SHA加密只需要将"SHA-1"改成"SHA"即可
            MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1"); 
            digest.reset();
            digest.update(str.getBytes());
            byte messageDigest[] = digest.digest();
            // Create Hex String
            StringBuffer hexStr = new StringBuffer();
            // 字节数组转换为 十六进制 数
            for (int i = 0; i < messageDigest.length; i++) {
                String shaHex = Integer.toHexString(messageDigest[i] & 0xFF);
                if (shaHex.length() < 2) {
                    hexStr.append(0);
                }
                hexStr.append(shaHex);
            }
            return hexStr.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 作为第三方开发，获取用户对应客户企业公众号的获取code码的链接codeUrl，通用工具类
     * @param redirectUri 重定向的地址 
     * @param appId 企业公众号
     * @param scope 授权方式，默认静默授权snsapi_base
     */
    public static String getWeChatCodeUrl(String redirectUri, String appId, String scope){
        logger.info("redirectUri:" + redirectUri);
        redirectUri = URLEncoder.encode(redirectUri);
        if (StringUtils.isBlank(scope)) {
            scope = "snsapi_base";
        }
        //拼接授权验证地址获取code码的URL
        String codeUrl = String.format("https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s&component_appid=%s#wechat_redirect",
                appId, redirectUri, scope, "xxxx_state",COMPONENT_APPID);
        System.out.println(codeUrl);
        return codeUrl;
    }
    
    /**
     * 对应主体公众号，生成用于获取code的地址codeUrl给前端
     * @param redirectUri重定向的地址， appId公众号， scope授权方式，
     * @param scope  snsapi_base 静默授权，snsapi_userinfo 非静默授权
     */
    public static String getWeChatCodeUrl(String redirectUri, String scope){
        logger.info("redirectUri:" + redirectUri);
        redirectUri = URLEncoder.encode(redirectUri);
        if (StringUtils.isBlank(scope)) {
            scope = "snsapi_base";
        }
        //拼接授权验证地址获取code码的URL
        String codeUrl = String.format("https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s#wechat_redirect",
                APPID, redirectUri, scope, "xxxx_state");
        logger.info("codeUrl:" + codeUrl);
        return codeUrl;
    }
    
    
    /**
     * 判断用户是否关注了公众号
     * @param accessToken
     * @param openid
     * @return true已关注，false未关注（subscribe=1表示已关注，subscribe=0未关注）
     */
    public static boolean judgeIsSubscribe(String accessToken,String openid){
        Integer subscribe = 0;
        String url = "https://api.weixin.qq.com/cgi-bin/user/info?access_token="+accessToken+"&openid="+openid+"&lang=zh_CN";
        try {  
            URL urlGet = new URL(url);  
            HttpURLConnection http = (HttpURLConnection) urlGet.openConnection();  
            // 必须是get方式请求  
            http.setRequestMethod("GET"); 
            http.setRequestProperty("Content-Type","application/x-www-form-urlencoded");  
            http.setDoOutput(true);  
            http.setDoInput(true);  
            http.connect();  
            InputStream is = http.getInputStream();  
            int size = is.available();  
            byte[] jsonBytes = new byte[size];  
            is.read(jsonBytes);  
            String message = new String(jsonBytes, "UTF-8");  
            JSONObject messageJson = JSONObject.parseObject(message); 
            logger.info("查询到的关注状态信息："+messageJson);  
            String subscribeStr = messageJson.get("subscribe") == null ? null :messageJson.get("subscribe").toString();
            if (null == subscribeStr) {
                logger.info("获取关注状态为空");
            } else {
                subscribe = Integer.parseInt(subscribeStr);
            }
            is.close();  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        return 1==subscribe?true:false;
    }


    
    /**
     * 为自己做小程序时使用，获取微信用户对应某个小程序的session_key和openid等信息
     * @param appid 小程序唯一标识
     * @param secret 小程序的 app secret
     * @param code 登录小程序时获取的 code
     * @return sessionKeyJson 
     */
    public static JSONObject getSmallRoutineSessionKey(String appid, String secret, String code) {
        String url = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code", appid, secret, code);
        logger.info("请求获取session_key的地址为："+url);
        JSONObject sessionKeyJson = null;
        try {
            String sessionKeyStr = sendHttpGetRequest(url);
            sessionKeyJson = JSONObject.parseObject(sessionKeyStr, JSONObject.class);
            logger.info("请求微信登录凭证校验接口成功. [result={}]", sessionKeyJson);
        } catch (Exception e) {
            logger.error("请求微信登录凭证校验接口失败. [error={}]", e);
        }
        return sessionKeyJson;
    }
    
    /**
     * 作为第三方平台时，获取微信用户对应某个小程序的session_key和openid等信息
     * @param appid 小程序唯一标识
     * @param secret 小程序的 app secret
     * @param code 登录小程序时获取的 code
     * @return sessionKeyJson 
     */
    public static JSONObject getSmallProgramSessionKey(String appid, String code, String companyId) {
        JSONObject sessionKeyJson = null;
        try {
            String url = String.format("https://api.weixin.qq.com/sns/component/jscode2session?appid=%s&js_code=%s&grant_type=authorization_code&component_appid=%s&component_access_token=%s",
                    appid, code, SP_COMPONENT_APPID, getComponentAccessToken(companyId));
            logger.info("请求获取session_key的地址为："+url);
            String sessionKeyStr = sendHttpGetRequest(url);
            sessionKeyJson = JSONObject.parseObject(sessionKeyStr, JSONObject.class);
            logger.info("请求session_key信息成功. [result={}]", sessionKeyJson);
        } catch (Exception e) {
            logger.error("请求session_key信息失败. [error={}]", e);
        }
        return sessionKeyJson;
    }
    
   /**
    * 第三方平台获取到的该小程序授权的authorizer_access_token
    * @param companyId
    * @param appId 
    */
	public static String getSmallProgramAuthAccessToken(long companyId, String appId){
        JedisUtil jedisUtil = JedisUtil.getInstance();
        JSONObject jsonObject = null;
        String result = null;
            result = jedisUtil.get("auth_access_token_smallProgram"+appId);
            if(result != null){
                return result;
            }
            jsonObject = getAuthorizerAccessToken(appId);
            if (null != jsonObject &&  0 == jsonObject.getIntValue("code")) {
                result = jsonObject.getJSONObject("data").getString("authorizer_access_token");
                jedisUtil.set("auth_access_token_smallProgram"+appId,result, 60*60);
                return result;
            }
    }
	
}
