package com.navercorp.ncaptcha;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.catalina.connector.Response;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import com.navercorp.ncaptcha.config.RedisConfig;
import com.navercorp.ncaptcha.domain.AxisVO;
import com.navercorp.ncaptcha.domain.CommonImageInfo;
import com.navercorp.ncaptcha.domain.ImageInfoVO;
import com.navercorp.ncaptcha.domain.TaskParamVO;
import com.navercorp.ncaptcha.domain.UserParamVO;
import com.navercorp.ncaptcha.scheduled.CaptchaImageScoreTask;
import com.navercorp.ncaptcha.service.ImageHandler;
import com.navercorp.ncaptcha.service.ValidationHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NcaptchaApplicationTests {
	@Autowired
	RedisConfig RedisConfig;
	Jedis jedis = new Jedis();
	@Autowired
	ValidationHandler validationHandler;
	@Autowired
	ImageHandler imageHandler;
	@Autowired
	CaptchaImageScoreTask captchaImageScoreTask; 
	
	private static final Logger log = LoggerFactory.getLogger(NcaptchaApplicationTests.class);
	
	@Test 
	public void redisFlush() throws Exception{
//		captchaImageScoreTask.selectRemoteBuildBatchJob();
//		captchaImageScoreTask.checkAvailableRemoteBuildBatchJob();
//		captchaImageScoreTask.setScoreCalculate();
		
//		TaskParamVO taskParam = new TaskParamVO();
//		taskParam.setClientKey("test123");
//		jedis.hset("test123", "rc", "1");
//		jedis.hset("test123", "fc", "1");
//		captchaImageScoreTask.getIssuedImageValidation(taskParam, "");
		jedis.flushAll();
//		TaskParamVO taskParam = new TaskParamVO(); 
//		captchaImageScoreTask.getImageClientListValidation(taskParam);
		
		
//		Pipeline pipeLine = jedis.pipelined();
//		int sum = 0;
//		// 타입(school, bridge..)에 대한 반복문을 돌려 Redis에 저장된 모든 PanoramaImagePool의 크기를 합산하여 저장하는 부분
//		
//		for(String type : CommonImageInfo.typeResizedList) {
//			pipeLine.llen(CommonImageInfo.answerPanoramaImagePoolKeyHeader + type);
//			pipeLine.llen(CommonImageInfo.examPanoramaImagePoolKeyHeader + type);
////			sum += pipeLine.llen(CommonImageInfo.examPanoramaImagePoolKeyHeader + type).intValue();
//		}
//		List<Object> res = pipeLine.syncAndReturnAll();
//		for(Object item : res) {
//			sum += Integer.parseInt(String.valueOf(item));
//		}
//		System.out.println(sum);
	}
	
//	/* Issued ClientKey */
//	@Test
//	public void getIssuedClientKeyValidationTest() throws Exception{
//		String ipAddress = "127.0.0.1";
//		String userAgent = "(Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Whale/1.4.63.11 Safari/537.36)";
//		String clientKey = "";
//		try {
//			clientKey = validationHandler.getIssuedClientKeyValidation(ipAddress, userAgent);
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		assertThat(clientKey).isNotEqualTo("");
//	}
//	
//	@Test
//	public void getDecimalToBase36Test() throws Exception{
//		String decimalNum = "127.0.0.1";
//		String base36Num = "";
//		try {
//			base36Num = validationHandler.getDecimalToBase36(decimalNum);
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		assertThat(base36Num).isEqualTo("3j.0.0.1");
//	}
//	
//	@Test
//	public void getResizedUserAgentTest() throws Exception{
//		String userAgent = "(Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Whale/1.4.63.11 Safari/537.36)";
//		String resizedUserAgent = "";
//		try {
//			resizedUserAgent = validationHandler.getResizedUserAgent(userAgent);
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		assertThat(resizedUserAgent).isEqualTo("windows,whale-1.4.1r.b");
//	}
//	
//	@Test
//	public void getIssuedClientKeyTest() throws Exception{
//		String ipAddress = "3j.0.0.1";
//		String userAgent = "windows,whale-1.4.1r.b";
//		String clientKey = "";
//		try {
//			clientKey = validationHandler.getIssuedClientKey(ipAddress, userAgent);
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		assertThat(clientKey).isNotEqualTo("");
//		assertThat(jedis.hget(clientKey, CommonImageInfo.clientRefreshCntField)).isEqualTo(CommonImageInfo.emptyValue);
//		assertThat(jedis.hget(clientKey, CommonImageInfo.clientFailCntField)).isEqualTo(CommonImageInfo.emptyValue);
//		assertThat(jedis.hget(clientKey, CommonImageInfo.clientUsingCaptchaImageField)).isEqualTo("");
//		assertThat(jedis.hget(clientKey, CommonImageInfo.ipAddressField)).isEqualTo(ipAddress);
//		assertThat(jedis.hget(ipAddress, CommonImageInfo.clientKeyField)).isEqualTo(clientKey);
//		assertThat(jedis.hget(ipAddress, CommonImageInfo.userAgentField)).isEqualTo(userAgent);
//	}
//	
//	@Test
//	public void setRegistrationAbuserTest() throws Exception{
//		String ipAddress = "3j.0.0.1";
//		String clientKey = "tmpClientKey";
//		try {
//			validationHandler.setRegistrationAbuser(ipAddress, clientKey);
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		assertThat(jedis.hget(ipAddress, CommonImageInfo.isAbuserField)).isEqualTo(CommonImageInfo.abuserValue);
//		assertThat(jedis.hget(clientKey, CommonImageInfo.isAbuserField)).isEqualTo(CommonImageInfo.abuserValue);
//	}
//	
//	
//	/* Issued CaptchaImage */
//	@Test
//	public void getCaptchaImageInfoTest() throws Exception{
//		String clientKey = "";
//		ImageInfoVO imageInfo = null;	
//		UserParamVO param = new UserParamVO(); 
//		param.setClientKey(clientKey);
//		param.setActionType("ISSUED");
//		
//		try {
//			imageInfo = imageHandler.getCaptchaImageInfo(param);
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		
//		assertThat(imageInfo).isNotNull();
//	}
//	
//	@Test
//	public void getCaptchaImageBase64Test() throws Exception{
//		String keyHeader = CommonImageInfo.captchaImagePoolKeyHeader;
//		String b64 = "";
//		UserParamVO param = new UserParamVO();
//		param.setClientKey("tmpClientKey");
//		param.setActionType("ISSUED");
//		try {
//			b64 = imageHandler.getCaptchaImageBase64(param, keyHeader);
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		assertThat(b64).isNotEqualTo("");
//	}
//	
//	@Test
//	public void checkAbuserClientTest() throws Exception{
//		String clientKey = "tmpClientKey";
//		boolean isAbuserClient = false;
//		boolean isNotAbuserClient = true;
//		String captchaImagePoolKey = "";
//		try {
//			jedis.hset(clientKey, CommonImageInfo.isAbuserField, "true");
//			isNotAbuserClient = imageHandler.checkAbuserClient(clientKey);
//			
//			jedis.hset(clientKey, CommonImageInfo.isAbuserField, "true");
//			captchaImagePoolKey = CommonImageInfo.captchaImageHardModePoolKeyHeader + "sc";
//			for(int i=0; i < CommonImageInfo.minCaptchaImageHardModePoolSize; i++) {
//				jedis.rpush(captchaImagePoolKey, "test" + String.valueOf(i));
//			}
//			isAbuserClient = imageHandler.checkAbuserClient(clientKey);
//			
//			for(int i=0; i < CommonImageInfo.minCaptchaImageHardModePoolSize; i++) {
//				jedis.lrem(captchaImagePoolKey, 0, "test" + String.valueOf(i));
//			}
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		
//		assertThat(isNotAbuserClient).isFalse();
//		assertThat(isAbuserClient).isTrue();
//	}
//	
//	@Test
//	public void getStartAxisTest() throws Exception{
//		String clientKey = "";
//		AxisVO startAxis = null;
//		try {
//			startAxis = imageHandler.getStartAxis(clientKey);
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		
//		assertThat(startAxis).isNotNull();
//	}
//	
//	
//	/* vaildation userInput */
//	@Test
//	public void getCheckValidationTest() throws Exception{
//		String clientKey = "tmpClientKey";
//		String captchaImageKey = "tmpCaptchaImageKey";
//		String successResult = "";
//		String failResult = "";
//		AxisVO inputAxis = new AxisVO();
//		inputAxis.setxAxis(100);
//		inputAxis.setyAxis(130);
//		
//		UserParamVO param = new UserParamVO();
//		param.setClientKey(clientKey);
//		param.setUserInputAxis(inputAxis);
//		
//		try {
//			jedis.hset(clientKey, CommonImageInfo.clientUsingCaptchaImageField, captchaImageKey);
//			jedis.hset(captchaImageKey, CommonImageInfo.answerXAxisField, "100");
//			jedis.hset(captchaImageKey, CommonImageInfo.answerYAxisField, "100");
//			
//			successResult = validationHandler.getCheckValidation(param);
//			
//			jedis.hset(captchaImageKey, CommonImageInfo.answerXAxisField, "0");
//			jedis.hset(captchaImageKey, CommonImageInfo.answerYAxisField, "0");
//			
//			failResult = validationHandler.getCheckValidation(param);
//		}catch(Exception e) {
//			log.error(e.getMessage());
//		}
//		
//		assertThat(successResult).isEqualTo(CommonImageInfo.successAction);
//		assertThat(failResult).isEqualTo(CommonImageInfo.failAction);
//	}
}
