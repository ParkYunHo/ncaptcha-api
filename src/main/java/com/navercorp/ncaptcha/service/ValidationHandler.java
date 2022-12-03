package com.navercorp.ncaptcha.service;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.navercorp.ncaptcha.config.RedisConfig;
import com.navercorp.ncaptcha.domain.AxisVO;
import com.navercorp.ncaptcha.domain.CommonImageInfo;
import com.navercorp.ncaptcha.domain.TaskParamVO;
import com.navercorp.ncaptcha.domain.UserParamVO;
import com.navercorp.ncaptcha.scheduled.CaptchaImageScoreTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

@Service
public class ValidationHandler {
	@Autowired
	RedisConfig RedisConfig;
	private Jedis jedis = new Jedis();
	@Autowired
	ImageHandler imageHandler;
	@Autowired
	CaptchaImageScoreTask captchaImageScoreTask;
	
	private static final Logger log = LoggerFactory.getLogger(ValidationHandler.class);

	/*
	 * Func : 파라미터로 전달받은 사용자입력좌표와 Redis에 저장된 정답좌표를 비교하여 정답유효성 체크하는 함수
	 */
	public String getCheckValidation(UserParamVO param) throws Exception {
		String result = "";								// var : 유효성체크 결과를 저장하는 변수
		AxisVO inputAxis = param.getUserInputAxis();	// var : 파라미터로 받은 사용자입력좌표를 저장하는 변수
		String clientKey = param.getClientKey();		// var : 파라미터로 받은 clientKey를 저장하는 변수
		String captchaImageKey = "";					// var : 현재 해당 client가 사용하고 있는 captchaImageKey를 저장하는 변수
		String ipAddressKey = "";						// var : 현재 해당 client가 사용하고 있는 IPAddressKey를 저장하는 변수
		int answerXAxis = 0;							// var : 정답좌표의 X축을 저장하는 변수
		int answerYAxis = 0;							// var : 정답좌표의 Y축을 저장하는 변수
		Pipeline pipeLine = null;
		List<Object> clientInfoList = null;
		List<Object> answerAxisInfoList = null;
		try {
			jedis = RedisConfig.getJedis();
			pipeLine = jedis.pipelined();
			pipeLine.hget(clientKey, CommonImageInfo.clientUsingCaptchaImageField);
			pipeLine.hget(clientKey, CommonImageInfo.ipAddressField);
			clientInfoList = pipeLine.syncAndReturnAll();
			// clientKey가 현재 사용하고 있는 captchaImageKey를 변수에 저장하는 부분
			captchaImageKey = String.valueOf(clientInfoList.get(0));
			// clientKey가 현재 사용하고 있는 ipAddressKey를 변수에 저장하는 부분
			ipAddressKey = String.valueOf(clientInfoList.get(1));
			
			pipeLine = jedis.pipelined();
			pipeLine.hget(captchaImageKey, CommonImageInfo.answerXAxisField);
			pipeLine.hget(captchaImageKey, CommonImageInfo.answerYAxisField);
			answerAxisInfoList = pipeLine.syncAndReturnAll();
			// CaptchaImageKey내에 저장된 정답좌표를 변수에 저장하는 부분
			// Y축좌표와 같은 경우 javascript에서 문제텍스트이미지의 세로길이가 포함된 좌표를 보내므로 정답유효성 체크시에도 해당 세로길이를 추가하여 체크해야한다
			answerXAxis = Integer.parseInt(String.valueOf(answerAxisInfoList.get(0)));
			answerYAxis = Integer.parseInt(String.valueOf(answerAxisInfoList.get(1))) + CommonImageInfo.descriptHeight;

			// 파라미터로 전달받은 사용자입력좌표와 Redis에 저장된 정답좌표의 유효성체크를 하는 부분
			if (inputAxis.getxAxis() == answerXAxis && inputAxis.getyAxis() == answerYAxis) {
				result = CommonImageInfo.successAction;

				// 정답유효성체크에 통과하였을 경우, SUCCESS 액션에 따른 score계산 등을 위해 하나의 thread를 별도로 실행시킨다
				TaskParamVO taskParam = new TaskParamVO();
				taskParam.setClientKey(clientKey);
				taskParam.setPreviousCaptchaImageKey(captchaImageKey);
				taskParam.setIpAddressKey(ipAddressKey);
				taskParam.setIssuedCaptchaImageKey(null);
				taskParam.setEmptyCaptchaImagePool(false);
				taskParam.setCaptchaImagePoolKey(null);
				captchaImageScoreTask.startScheduler(result, taskParam);
			} else {
				result = CommonImageInfo.failAction;
			}
		}catch (Exception e) {
			log.error("[getCheckValidation] UserMessage  : Redis에 저장된 정답좌표와 사용자 입력좌표를 비교하던 중에 에러발생");
			log.error("[getCheckValidation] SystemMessage: {}", e.getMessage());
			log.error("[getCheckValidation] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
		}
		return result;
	}

	/*
	 * Func : ipAddress를 확인하여 어뷰저인지를 확인하고, ClientKey가 이미 만들어져있으면 이미 만들어진 ClientKey를, 없으면 새로운 ClientKey를 발급하여 리턴하는 함수 
	 */
	public String getIssuedClientKeyValidation(String requestIPAddress, String requestUserAgent) throws Exception {
		String clientKey = "";						// var : 조건에 따라 리턴할 ClientKey를 저장하는 변수
		String userAgent = "";						// var : 기존에 생성되어 있던 ClientKey의 userAgent를 저장하는 변수 
		int clientRefreshCnt = 0;					// var : 기존에 생성되어 있던 ClientKey의 refreshCnt를 저장하는 변수
		String clientRefreshCntString = "";			// var : 기존에 생성되어 있던 ClientKey의 refreshCnt를 문자열 형식으로 저장하는 변수(값이 null인 경우 정수형으로 변환시 에러가 발생하는 것을 방지하기 위해 사용)
		String resizedIPAddressKey = "";			// var : request패킷에 있는 ipAddress를 36진수로 변환하여 크기를 줄인 ipAddressKey를 저장하는 변수
		String resizedUserAgent = "";				// var : request패킷에 있는 User-Agent에서 OS와 browser정보만 사용하여 만든 값을 저장하는 변수
		boolean isDuplicatedIPAddressKey = false;	// var : ipAddressKey가 이미 존재하는지 유무를 저장하는 변수
		Pipeline pipeLine = null;
		try {
			jedis = RedisConfig.getJedis();
			// request패킷에 있는 ipAddress를 36진수로 변환한 값을 저장하는 부분
			resizedIPAddressKey = getDecimalToBase36(requestIPAddress);
			// request패킷에 있는 User-Agent를 OS와  browser정보만을 사용하여 크기를 줄인 userAgent를 저장하는 부분
			resizedUserAgent = getResizedUserAgent(requestUserAgent);

			// ipAddressPool에 ipAddressKey가 이미 존재하는지 유무를 판단하는 부분
			isDuplicatedIPAddressKey = jedis.lrem(CommonImageInfo.ipAddressPoolKey, 0, resizedIPAddressKey).intValue() > 0 ? true : false;
			// ipAddressPool에서 삭제한 ipAddressKey를 다시 삽입하는 부분
			jedis.rpush(CommonImageInfo.ipAddressPoolKey, resizedIPAddressKey);
			// ipAddressPool에 ipAddress가 존재한다는 것은 clientKey를 발급받고 Success를 하지 않고, 캡차문제를 종료함을 의미
			if (isDuplicatedIPAddressKey) {
				// ipAddressKey에 저장되어 있는 이미 발급된 clientKey를 변수에 저장하는 부분
				clientKey = jedis.hget(resizedIPAddressKey, CommonImageInfo.clientKeyField);
				if (clientKey != null) {
					// ipAddressKey에 저장되어 있는 userAgent를 변수에 저장하는 부분
					userAgent = jedis.hget(resizedIPAddressKey, CommonImageInfo.userAgentField);
					// ipAddressKey에 저장되어 있는 userAgent와 현재 request패킷에 있는 userAgent를 확인하여 다른 OS 또는 다른 Broswer에서 다시 ClientKey를 요청할 시에는 어뷰저로 판단
					if (userAgent.equals(resizedUserAgent)) {
						// (정상적인 사용자) ipAddressKey에 저장되어 있는 userAgnet와 현재 request패킷에 있는 userAgent를 확인하여 같은 OS 그리고 같은 Browser라면 ipAddressKey와 clientKey의 TTL을 다시 정상적으로 설정
						pipeLine = jedis.pipelined();
						pipeLine.expire(resizedIPAddressKey, CommonImageInfo.maxIpAddressKeyTTL); 
						pipeLine.expire(clientKey, CommonImageInfo.maxClientKeyTTL);
						pipeLine.sync();
					} else {
						// (어뷰저) ipAddressKey에 저장되어 있는 userAgent와 현재 request패킷에 있는 userAgent를 확인하여 다른 OS 또는 다른 Broswer에서 다시 ClientKey를 요청한 경우 어뷰저로 판단하여 isAbuser필드를 'true'로 변경
						jedis.hset(resizedIPAddressKey, CommonImageInfo.userAgentField, userAgent);
						setRegistrationAbuser(resizedIPAddressKey, clientKey);
					}
					// 브라우저 새로고침을 통해 캡차문제를 다시 발급받을 시 새로고침 횟수를 늘리는 부분
					clientRefreshCntString = jedis.hget(clientKey, CommonImageInfo.clientRefreshCntField);
					if(clientRefreshCntString == null) {
						clientRefreshCnt = 1;
					}else {
						clientRefreshCnt = Integer.parseInt(clientRefreshCntString) + 1;
					}
					pipeLine = jedis.pipelined();
    				if(clientRefreshCnt >= CommonImageInfo.maxClientRefreshCnt) {
    					// 최대 refreshCnt보다 클 경우, clientKey와 ipAddressKey의 isA(isAbuser)필드를 'true'로 변경하는 부분
    					pipeLine.hset(clientKey, CommonImageInfo.isAbuserField, CommonImageInfo.abuserValue);
    					pipeLine.hset(resizedIPAddressKey, CommonImageInfo.isAbuserField, CommonImageInfo.abuserValue);
    	    		}else {
    	    			// 최대 refreshCnt보다 작은 경우, clientKey의 refreshCnt필드에 '1' 증가된 refresh횟수를 업데이트하는 부분
    	    			pipeLine.hset(clientKey, CommonImageInfo.clientRefreshCntField, String.valueOf(clientRefreshCnt));
    	    		}
    				pipeLine.sync();
				} else {
					// (정상적인 사용자) 캡차문제를 풀기전에 종료하여 SUCCESS 액션이 완료되지 못해 ipAddressList에는 있지만, ipAddressKey의 TTL이 다되어 ipAddressKey가 삭제되었을 경우
					//              어뷰저로 판단할 기준이 없으므로 다시 ClientKey를 발급하고, 정상적인 사용자로 판단한다
					clientKey = getIssuedClientKey(resizedIPAddressKey, resizedUserAgent);
					pipeLine = jedis.pipelined();
					pipeLine.hset(clientKey, CommonImageInfo.isAbuserField, CommonImageInfo.notAbuserValue);
					pipeLine.hset(resizedIPAddressKey, CommonImageInfo.isAbuserField, CommonImageInfo.notAbuserValue);
					pipeLine.expire(clientKey, CommonImageInfo.maxClientKeyTTL);
					pipeLine.sync();
				}
			} else {
				// 정상적으로 캡차문제를 풀고 Success 액션까지 완료하여 ipAddressList에는 삭제되었을때 어뷰저인 경우에는 ipAddressKey를 특정 TTL만큼 살려놓기 때문에 해당 ipAddressKey의 isAbuser필드를 get()하여 어뷰저 유무 판단
				if(jedis.hget(resizedIPAddressKey, CommonImageInfo.isAbuserField) != null) {
					// (어뷰저) Success 액션까지 완료하였지만 어뷰저로 분류되어 ipAddressKey가 살아있고 어뷰저ipAddressKeyTTL 시간이 지나기 전이므로, 새로운 ClientKey를 발급하고 isAbuser필드 'true'로 변경하여 어뷰저로 설정한다
					clientKey = getIssuedClientKey(resizedIPAddressKey, resizedUserAgent);
					setRegistrationAbuser(resizedIPAddressKey, clientKey);
				}else {
					// (정상적인 사용자) Success 액션까지 완료하였으며 정상적인 사용자로 판단되어 ipAddressKey가 삭제된 경우, 새로운 ClientKey를 발급하고 isAbuser필드에 'false'로 변경하여 정상적인 사용자로 분류한다
					clientKey = getIssuedClientKey(resizedIPAddressKey, resizedUserAgent);
					pipeLine = jedis.pipelined();
					pipeLine.hset(clientKey, CommonImageInfo.isAbuserField, CommonImageInfo.notAbuserValue);
					pipeLine.hset(resizedIPAddressKey, CommonImageInfo.isAbuserField, CommonImageInfo.notAbuserValue);
					pipeLine.expire(clientKey, CommonImageInfo.maxClientKeyTTL);
					pipeLine.sync();
				}
			}
			
			// Test Code
			printAllData(clientKey);
			//
		}catch (Exception e) {
			log.error("[getIssuedClientKeyValidation] UserMessage  : ClientKey발행시 유효성체크를 하던 도중 에러발생");
			log.error("[getIssuedClientKeyValidation] SystemMessage: {}", e.getMessage());
			log.error("[getIssuedClientKeyValidation] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
			
			clientKey = "";
		}
		return clientKey;
	}

	/*
	 * Func : 10진수의 ip주소 또는 브라우저버전 정보(format: x.x.x.x)를 36진수로 변환한 값을 리턴하는 함수
	 *        (Redis에 ip주소 또는 브라우저버전 정보를 저장할때 크기를 조금이라도 줄이기 위해 36진수로 변환작업을 거친다)
	 */
	public String getDecimalToBase36(String decimalNum) throws Exception {
		List<String> splitedDecimalNumList = null;		// var : x.x.x.x형식의 10진수 각각의 값을 하나씩 split하여 리스트형식으로 저장하는 변수
		String base36Num = "";							// var : 36진수로 변환한 숫자들은 x.x.x.x형식의 포맷으로 최종적으로 만든 값을 저장하는 변수 
		try {
			splitedDecimalNumList = Arrays.asList(decimalNum.split("\\."));
			for (String splitedDecimalNum : splitedDecimalNumList) {
				base36Num += "." + Long.toString(Long.parseLong(splitedDecimalNum), 36);
			}
			// 반복문을 통해 .x.x.x.x 형식으로 만들어진 36진수 값들의 맨 앞의 (.)을 제거하는 부분 
			base36Num = base36Num.substring(1, base36Num.length());
		} catch (Exception e) {
			log.error("[getDecimalToBase36] UserMessage  : x.x.x.x 형식의 10진수 숫자를 36진수로 변환하는 도중 에러발생");
			log.error("[getDecimalToBase36] SystemMessage: {}", e.getMessage());
			log.error("[getDecimalToBase36] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
			
			base36Num = "";
		}
		return base36Num;
	}

	/*
	 * Func : request패킷의 User-Agent에서 필요한 OS와 Browser정보만을 가지고 userAgent값을 만들어 리턴하는 함수
	 *        (userAgent를 Reids에 저장할때 크기를 줄이기위해 해당 방식사용)  
	 */
	public String getResizedUserAgent(String userAgent) throws Exception {
		String clientOS = "";							// var : userAgent의 OS부분을 저장하는 변수
		String clientBrowser = "";						// var : userAgnet의 browser부분을 저장하는 변수
		String clientBrowserArray[] = new String[2];	// var : userAgnet의 browser부분이 버전정보와 함께 붙어 있는 경우 이를 분리하여 브라우저명과 버전정보를 따로 문자열배열에 저장하는 변수
		String resizedUserAgent = "";					// var : userAgent의 OS와 Browser정보만을 가지고 만든 userAgent를 저장하는 변수
		try {
			// userAgent에서 필요한 값을 쉽게 검색하기 위해 소문자로 문자열을 변경하는 부분
			userAgent = userAgent.toLowerCase();		

			// clientOS 검색하는 부분
			if (userAgent.indexOf(CommonImageInfo.windowsOS) >= 0) {
				// windowsOS인 경우
				clientOS = CommonImageInfo.windowsOS;
			} else if (userAgent.indexOf(CommonImageInfo.macOS) >= 0) {
				// MacOS인 경우
				clientOS = CommonImageInfo.macOS;
			} else if (userAgent.indexOf(CommonImageInfo.unixUserAgent) >= 0) {
				// UnixOS인 경우
				clientOS = CommonImageInfo.unixOS;
			} else if (userAgent.indexOf(CommonImageInfo.androidOS) >= 0) {
				// AndroidOS인 경우
				clientOS = CommonImageInfo.androidOS;
			} else if (userAgent.indexOf(CommonImageInfo.iphoneOS) >= 0) {
				// IPhoneOS인 경우
				clientOS = CommonImageInfo.iphoneOS;
			} else {
				// OS정보를 알수 없는 경우
				clientOS = CommonImageInfo.unKnown;
			}

			// client Browser 검색하는 부분
			if (userAgent.contains(CommonImageInfo.safariBrowser) && userAgent.contains(CommonImageInfo.broswerVersion)) {
				// Safari (Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/534.54.16 (KHTML,like Gecko) Version/5.1.4 Safari/534.54.16)
				clientBrowserArray = (userAgent.substring(userAgent.indexOf(CommonImageInfo.safariBrowser)).split(" ")[0]).split("/");
				clientBrowser = clientBrowserArray[0] + "-" + getDecimalToBase36(clientBrowserArray[1]);
			} else if (userAgent.contains(CommonImageInfo.operaBrowserUp15Version) || userAgent.contains(CommonImageInfo.operaBrowserUnder15Version)) {
				if (userAgent.contains(CommonImageInfo.operaBrowserUnder15Version)) {
					// Opera 15버전 이전
					clientBrowserArray = (userAgent.substring(userAgent.indexOf(CommonImageInfo.operaBrowserUnder15Version)).split(" ")[0]).split("/");
					clientBrowser = clientBrowserArray[0] + "-" + getDecimalToBase36(clientBrowserArray[1]);
				} else if (userAgent.contains(CommonImageInfo.operaBrowserUp15Version)) {
					// Opera 15버전 이후 (Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36 OPR/58.0.3135.65)
					clientBrowserArray = (userAgent.substring(userAgent.indexOf(CommonImageInfo.operaBrowserUp15Version)).split(" ")[0]).split("/");
					clientBrowser = clientBrowserArray[0].replace(CommonImageInfo.operaBrowserUp15Version, CommonImageInfo.operaBrowserUnder15Version)
							 		+ "-"
							 		+ getDecimalToBase36(clientBrowserArray[1]);
				}
			} else if (userAgent.contains(CommonImageInfo.whaleBrowser)) {
				// Whale (Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Whale/1.4.63.11 Safari/537.36)
				clientBrowserArray = (userAgent.substring(userAgent.indexOf(CommonImageInfo.whaleBrowser)).split(" ")[0]).split("/");
				clientBrowser = clientBrowserArray[0] + "-" + getDecimalToBase36(clientBrowserArray[1]);
			} else if (userAgent.contains(CommonImageInfo.edgeBrowser)) {
				// Edge (Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML,like Gecko) Chrome/58.0.3029.110 Safari/537.36 Edge/16.16299)
				clientBrowserArray = (userAgent.substring(userAgent.indexOf(CommonImageInfo.edgeBrowser)).split(" ")[0]).split("/");
				clientBrowser = clientBrowserArray[0] + "-" + getDecimalToBase36(clientBrowserArray[1]);
			} else if (userAgent.contains(CommonImageInfo.chromeBrowser)) {
				// Chrome (Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML,like Gecko) Chrome/72.0.3626.109 Safari/537.36)
				clientBrowserArray = (userAgent.substring(userAgent.indexOf(CommonImageInfo.chromeBrowser)).split(" ")[0]).split("/");
				clientBrowser = clientBrowserArray[0] + "-" + getDecimalToBase36(clientBrowserArray[1]);
			} else if (userAgent.contains(CommonImageInfo.firefoxBrowser)) {
				// firefox (Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:65.0) Gecko/20100101 Firefox/65.0)
				clientBrowserArray = (userAgent.substring(userAgent.indexOf(CommonImageInfo.firefoxBrowser)).split(" ")[0]).split("/");
				clientBrowser = clientBrowserArray[0] + "-" + getDecimalToBase36(clientBrowserArray[1]);
			} else if (userAgent.contains(CommonImageInfo.IEUserAgent)) {
				// IE 10버전 이후 (Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko)
				clientBrowser = CommonImageInfo.IEBrowser + "-" + getDecimalToBase36(userAgent.substring(userAgent.indexOf(CommonImageInfo.IEUserAgent) + 3, userAgent.indexOf(")")));
			} else {
				clientBrowser = CommonImageInfo.unKnown;
			}
			// OS정보와 Browser정보만을 가지고 userAgent를 새로 만드는 부분
			resizedUserAgent = (clientOS + "," + clientBrowser);
		} catch (Exception e) {
			log.error("[getResizedUserAgent] UserMessage  : Client가 요청한 Request패킷의 User-Agent를 필요한 OS와 Browser만 남기는 작업도중 에러발생");
			log.error("[getResizedUserAgent] SystemMessage: {}", e.getMessage());
			log.error("[getResizedUserAgent] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
			
			resizedUserAgent = "";
		}
		return resizedUserAgent;
	}

	/*
	 * Func : 10자리 Random문자열로 ClientKey를 만들고 이미 사용중인 ClientKey가 아닐 경우, 해당 ClinetKey의 값들을 초기화하는 함수
	 */
	public String getIssuedClientKey(String ipAddress, String userAgent) throws Exception {
		String clientKey = "";					// var : 랜덤문자열로 만들어진 clientKey를 저장하는 변수
		boolean isAvailableClientKey = false;	// var : 사용가능한 clientKey인지 유무를 저장하는 변수
		Pipeline pipeLine = null;
		try {
			// 반복문을 돌려 랜덤 문자열로 만들어진 clientKey가 이미 사용중인 ClientKey일때 3번까지 재시도 하는 부분 
			for (int i = 0; i < CommonImageInfo.maxIssuedClientKeyRetryCnt; i++) {
				// Random문자열로 10자리 문자열을 만드는 부분
				clientKey = RandomStringUtils.randomAlphanumeric(CommonImageInfo.maxClientKeyLength);
				// clientPool에서 생성한 clientKey를 검색하여 이미 사용중인 clientKey가 아니라면 true를 리턴한다 
				isAvailableClientKey = jedis.lrem(CommonImageInfo.clientPoolKey, 0, clientKey) > 0 ? false : true;
				if (isAvailableClientKey) {
					// 이미 사용중인 clientKey가 아닌 경우, clientKey의 값들을 초기화하는 한다
					pipeLine = jedis.pipelined();
					pipeLine.hset(clientKey, CommonImageInfo.clientRefreshCntField, CommonImageInfo.emptyValue);
					pipeLine.hset(clientKey, CommonImageInfo.clientFailCntField, CommonImageInfo.emptyValue);
					pipeLine.hset(clientKey, CommonImageInfo.clientUsingCaptchaImageField, "");
					pipeLine.hset(clientKey, CommonImageInfo.ipAddressField, ipAddress);
					pipeLine.hset(ipAddress, CommonImageInfo.clientKeyField, clientKey);
					pipeLine.hset(ipAddress, CommonImageInfo.userAgentField, userAgent);
					pipeLine.sync();
					break;
				}else {
					// 이미 사용중인 clientKey라면 한번더 반복문을 돌리며, lrem()으로 삭제된 clientKey를 다시 clientPool에 추가한다
					jedis.rpush(CommonImageInfo.clientPoolKey, clientKey);
					clientKey = "";
				}
			}
		} catch (Exception e) {
			log.error("[getIssuedClientKey] UserMessage  : 중복된 ClientKey가 없도록 체크한 뒤에 Redis에 Client정보를 초기화하는 도중 에러발생");
			log.error("[getIssuedClientKey] SystemMessage: {}", e.getMessage());
			log.error("[getIssuedClientKey] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
			
			clientKey = "";
		}
		return clientKey;
	}

	/*
	 * Func : 한명의 사용자가 사용하는 ipAddresKey와 clientKey에 isAbuser필드를 'true'로 변경하여 어뷰저로 분류하는 함수 
	 */
	public void setRegistrationAbuser(String ipAddressKey, String clientKey) throws Exception {
		Pipeline pipeLine = null;
		try {
			// ipAddressKey와 clientKey의 isAbuser필드를 'true'로 변경하고 TTL을 재설정하는 부분
			pipeLine = jedis.pipelined();
			pipeLine.hset(ipAddressKey, CommonImageInfo.isAbuserField, CommonImageInfo.abuserValue);
			pipeLine.hset(clientKey, CommonImageInfo.isAbuserField, CommonImageInfo.abuserValue);
			pipeLine.expire(ipAddressKey, CommonImageInfo.maxIpAddressKeyTTL);
			pipeLine.expire(clientKey, CommonImageInfo.maxClientKeyTTL);
			pipeLine.sync();
		} catch (Exception e) {
			log.error("[setRegistrationAbuser] UserMessage  : Redis에 어뷰저 등록시 에러발생");
			log.error("[setRegistrationAbuser] SystemMessage: {}", e.getMessage());
			log.error("[setRegistrationAbuser] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
		}
	}

	// Test Code
	public void printAllData(String clientKey) throws Exception {
		log.info("=============================================================");
    	log.info(" < Client Info >");
    	log.info("{" + clientKey + ", refreshCnt, " + jedis.hget(clientKey, CommonImageInfo.clientRefreshCntField) + "}");
    	log.info("{" + clientKey + ", failCnt, " + jedis.hget(clientKey, CommonImageInfo.clientFailCntField) + "}");
    	log.info("{" + clientKey + ", captchaImage, " + jedis.hget(clientKey, CommonImageInfo.clientUsingCaptchaImageField) + "}");
    	log.info("{" + clientKey + ", isAbuser, " + jedis.hget(clientKey, CommonImageInfo.isAbuserField) + "}");
    	log.info("{" + clientKey + ", ipAddress, " + jedis.hget(clientKey, CommonImageInfo.ipAddressField) + "}");
    	log.info(" < IPAddressList Info >");
    	List<String> ipAddressList = jedis.lrange(CommonImageInfo.ipAddressPoolKey, 0, -1);
    	log.info("{ipAddressList, " + String.join(",", ipAddressList) + "}");
    	log.info(" < IPAddress Info >");
    	String ipAddress = jedis.hget(clientKey, CommonImageInfo.ipAddressField);
    	if(ipAddress == null) {
    		log.info("{empty}");
    	}else {
    		log.info("{" + ipAddress + ", clientKey, " + jedis.hget(ipAddress, CommonImageInfo.clientKeyField) + "}");
        	log.info("{" + ipAddress + ", userAgent, " + jedis.hget(ipAddress, CommonImageInfo.userAgentField) + "}");
        	log.info("{" + ipAddress + ", isAbuser, " + jedis.hget(ipAddress, CommonImageInfo.isAbuserField) + "}");
    	}
    	log.info(" < CaptchaImagePool Size >");
    	for(String type : CommonImageInfo.typeList) {
    		log.info("[" + type + "] normal-" + jedis.llen(CommonImageInfo.captchaImagePoolKeyHeader + type.substring(0,2)) + "  hard-" + jedis.llen(CommonImageInfo.captchaImageHardModePoolKeyHeader + type.substring(0,2)));
    	}
    	log.info("=============================================================");
	}
	//
}
