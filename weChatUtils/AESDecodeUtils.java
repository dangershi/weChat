import org.apache.commons.codec.binary.Base64;  
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;  
import javax.crypto.spec.IvParameterSpec;  
import javax.crypto.spec.SecretKeySpec;  

import java.security.spec.AlgorithmParameterSpec;  
  
/**
 * 	@ClassName: AESDecodeUtils.java
 *  @Description: 解密小程序信息，获取用户手机号
 *  @author: 鹿丸
 */
public class AESDecodeUtils {  
  
    private static final Logger log = LoggerFactory.getLogger(AESDecodeUtils.class);
    
    /**
     * 转换为byte[]格式，并对小程序返回的加密数据进行解密
     * @param
     * @return 
     * @author  鹿丸
     */
    public static String decodeBase64(String[] args) throws Exception {  
        byte[] sessionKey = Base64.decodeBase64(args[0].getBytes());
        byte[] ivData = Base64.decodeBase64(args[1].getBytes());
        byte[] encrypData = Base64.decodeBase64(args[2].getBytes());  
        String resultData = decrypt(sessionKey,ivData,encrypData);
        log.info("解密后数据为：{}",resultData);
        return resultData;  
    }  
  
    public static String decrypt(byte[] key, byte[] iv, byte[] encData) throws Exception {  
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);  
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");  
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");  
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);  
        //解析解密后的字符串  
        return new String(cipher.doFinal(encData),"UTF-8");  
    }  
}  

