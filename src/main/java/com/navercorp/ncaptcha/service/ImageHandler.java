package com.navercorp.ncaptcha.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.navercorp.ncaptcha.config.RedisConfig;
import com.navercorp.ncaptcha.domain.AxisVO;
import com.navercorp.ncaptcha.domain.CommonImageInfo;
import com.navercorp.ncaptcha.domain.ImageInfoVO;
import com.navercorp.ncaptcha.domain.UserParamVO;
import com.navercorp.ncaptcha.domain.TaskParamVO;
import com.navercorp.ncaptcha.scheduled.CaptchaImageScoreTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

@Service
public class ImageHandler {
	@Autowired
	RedisConfig RedisConfig;
	Jedis jedis = new Jedis();
	@Autowired
	CaptchaImageScoreTask captchaImageScoreTask;
	
	private static final Logger log = LoggerFactory.getLogger(ImageHandler.class);

	/*
	 * Func : CaptchaImage정보를 ImageInfoVO 객체에 담아 리턴하는 함수
	 */
	public ImageInfoVO getCaptchaImageInfo(UserParamVO param) throws Exception{
		long start = System.currentTimeMillis();
		
		ImageInfoVO imageInfo = null;					// var : 이미지 정보를 저장할 객체 변수
		String clientKey = param.getClientKey();	// var : 파라미터로 전달받은 ClientKey를 저장하는 변수
		String b64 = "";							// var : CaptchaImage의 base64 문자열을 저장할 변수
		try {
			jedis = RedisConfig.getJedis();
			// 캡차이미지를 요청한 client가 어뷰저인지를 판단하여 어뷰저에게는 어려운 모드의 캡차문제를 리턴하고,
			// 정상적인 사용자에게는 일반모드의 캡차문제를 리턴한다.
			if(checkAbuserClient(clientKey)) {
				b64 = getCaptchaImageBase64(param, CommonImageInfo.captchaImageHardModePoolKeyHeader);
			}else {
				b64 = getCaptchaImageBase64(param, CommonImageInfo.captchaImagePoolKeyHeader);
			}
			// captchaImage호출이 실패했을때 빈값("")을 리턴하며 호출 결과에 따라 ImageInfoVO객체에 null을 넣거나 또는 객체를 채우는 부분
			if(b64.equals("")) {
				// captchaImage호출이 실패했음을 Controller에도 알리기 위해 ImageInfoVO객체에도 null을 담아 리턴한다. 
				imageInfo = null;
			}else {
				// captchaImage호출 성공시 captchaImage에 대한 정보를 ImageInfoVO 객체에 담아 리턴한다
				imageInfo = new ImageInfoVO();
				imageInfo.setB64(b64);
				imageInfo.setWidth(CommonImageInfo.width);
				imageInfo.setHeight(CommonImageInfo.height);
				imageInfo.setCellSize(CommonImageInfo.imageCellSize);
				imageInfo.setDescriptHeight(CommonImageInfo.descriptHeight);
				imageInfo.setStartAxis(getStartAxis(clientKey));
			}
		}catch(Exception e) {
			log.error("[getCaptchaImageInfo] UserMessage  : CaptchaImage의 정보들을 ImageInfoVO 객체에 저장하는 도중 에러발생");
			log.error("[getCaptchaImageInfo] SystemMessage: {}", e.getMessage());
			log.error("[getCaptchaImageInfo] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
			imageInfo = null;
		}
		long end = System.currentTimeMillis();
		log.info("run time={}", (end-start)/1000.0);
		
		return imageInfo;
	}
	
	/*
	 * Func : 각 타입별 CaptchaImagePool 크기를 체크하여 제일 크기가 큰 타입중에서 랜덤한 타입의 랜덤한 CaptchaImage의 base64를 리턴하는 함수
	 *        (신규로 발급한 CaptchaImage에 대한 score계산 및 액션에 따른 프로세스는 별도의 thread를 사용하여 동작시킨다)
	 */
	public String getCaptchaImageBase64(UserParamVO param, String keyHeader) throws Exception{
		Random random = new Random();							
		List<String> maxCaptchaImagePoolKeyList = new ArrayList<String>();	// var : CaptchaImagePool크기가 제일 여러 Pool들의 Key값을 저장하는 변수
		int captchaImagePoolSize = 0;										// var : CaptchaImagePool의 크기를 저장하는 변수
		int maxCaptchaImagePoolSize = 0;									// var : CaptchaImagePool 크기의 최대값을 저장하는 변수
		boolean isEmptyCaptchaImagePool = false;							// var : CaptchaImagePool의 크기가 최소 Pool크기보다 작은지 유무를 저장하는 변수
		String captchaImagePoolKey = "";									// var : CaptchaImagePool의 키값을 저장하는 변수
		String b64 = "";													// var : 신규발급되는 CaptchaImage의 base64값을 저장하는 변수
		int selectedCaptchaImagePoolSize = 0;								// var : 최대 크기를 가진 CaptchaImagePool 중에서 랜덤하게 선택된 Pool의 크기를 저장하는 변수
		String selectedCaptchaImageKey = "";								// var : 최대 크기를 가진 CaptchaImagePool 중에서 랜덤하게 하나의 Pool이 선택되고, 이 Pool 내에서 랜덤하게 선택된 CaptchaImageKey를 저장하는 변수
		String previousCaptchaImageKey = "";								// var : 신규 CaptchaImage를 발급하기 전에 발급되었던 CaptchaImageKey를 저장하는 변수 (신규로 발급되기 전에 사용되었던 CaptchaImage에 대한 액션의 Score를 계산하기 위해 사용)  
		String clientKey = "";												// var : 파라미터로 전달받은 ClientKey를 저장하는 변수
		String ipAddressKey = "";											// var : ClientKey에 해당하는 ipAddressKey를 저장하는 변수 (History를 저장하기 위해 사용)
		Pipeline pipeLine = null;
		List<Object> captchaImagePoolSizeList = null;
		List<Object> clientInfoList = null;
		try {
			// 반복문을 돌려 각 타입별 최대크기를 가진 CaptchaImagePool을 찾는 부분 (최대크기을 가진 Pool이 여러개가 있을수도 있으므로 리스트형식으로 저장)
			for(String type : CommonImageInfo.typeResizedList) {
				// captchaImagePool의 쉬운모드 또는 어려운 모드에 따른 keyHeader를 파라미터로 전달받아 captchaImagePool의 키값을 저장하는 부분
				captchaImagePoolKey = keyHeader + type;						
				// captchaImagePool의 크기를 저장하는 부분
				captchaImagePoolSize = jedis.llen(captchaImagePoolKey).intValue();
				
				// 최대크기를 가진 CaptchaImagePool을 찾는 부분
				if(maxCaptchaImagePoolSize < captchaImagePoolSize) {
					maxCaptchaImagePoolKeyList.clear();
					maxCaptchaImagePoolKeyList.add(captchaImagePoolKey);
					maxCaptchaImagePoolSize = captchaImagePoolSize;
				}else if(maxCaptchaImagePoolSize == captchaImagePoolSize){
					maxCaptchaImagePoolKeyList.add(captchaImagePoolKey);
				}
				
				// captchaImagePool의 일반모드와 어려운모드를 합친 전체 사이즈를 체크하는 부분
				// (최소 CaptchaImagePool크기보다 작을때에는 background Task를 동작시킬때 CaptchaImagePool을 만드는 Batch를 실행시키기 위해서 계산)
				pipeLine = jedis.pipelined();
				pipeLine.llen(CommonImageInfo.captchaImagePoolKeyHeader + type);
				pipeLine.llen(CommonImageInfo.captchaImageHardModePoolKeyHeader + type);
				captchaImagePoolSizeList = pipeLine.syncAndReturnAll();
				captchaImagePoolSize += Integer.parseInt(String.valueOf(captchaImagePoolSizeList.get(0)));
				captchaImagePoolSize += Integer.parseInt(String.valueOf(captchaImagePoolSizeList.get(1)));
				
				// CaptchaImagePool의 전체사이즈가 최소 CaptchaImagePool크기보다 작으면 true값을 저장하여 CaptchaImagePool을 만드는 Batch를 실행시킨다
				if(CommonImageInfo.minCaptchaImagePoolSize >= captchaImagePoolSize) {
					isEmptyCaptchaImagePool = true;
				}
			}
			// 최대크기를 가진 CaptchaImagePool 중에서 랜덤한 하나의 Pool을 결정하는 부분
			Collections.shuffle(maxCaptchaImagePoolKeyList);
			captchaImagePoolKey = maxCaptchaImagePoolKeyList.get(0);
			// 최대크기를 가진 CaptchaImagePool 중에서 랜덤하게 결정된 CaptchaImagePool의 크기를 저장하는 부분
			selectedCaptchaImagePoolSize = jedis.llen(captchaImagePoolKey).intValue();
			
			if(selectedCaptchaImagePoolSize <= 0) {
				// CaptchaImagePool이 완전히 비어있을때는 바로 PanoramaImagePool을 구성하는 배치부터 실행시킨다
				// PanoramaImagePool을 만드는 배치가 실행되면 CaptchaImagePool을 만드는 배치를 "빌드후 조치"를 통해 실행되므로 일일히 PanoramaImagePool의 크기를 계산하지 않고 바로 배치실행
				captchaImageScoreTask.setRemoteBuildBatchJobDirectly();
			}else {
				// 랜덤하게 결정된 CaptchaImagePool 내에서 랜덤한 하나의 CaptchaImageKey를 저장하는 부분   
				selectedCaptchaImageKey = jedis.lindex(captchaImagePoolKey, random.nextInt(selectedCaptchaImagePoolSize));
				clientKey = param.getClientKey();
				
				pipeLine = jedis.pipelined();
				pipeLine.hget(selectedCaptchaImageKey, CommonImageInfo.base64Field);
				pipeLine.hget(clientKey, CommonImageInfo.clientUsingCaptchaImageField);
				pipeLine.hget(clientKey, CommonImageInfo.ipAddressField);
				clientInfoList = pipeLine.syncAndReturnAll();
				
				// 결정된 CaptchaImageKey의 Redis 내에 저장된  base64정보를 가져와 변수에 저장하는 부분
				b64 = String.valueOf(clientInfoList.get(0));
				// clientKey를 사용하여 Redis에 저장된 Client가 현재 사용중인 CaptchaImageKey와 ipAddressKey를 변수에 저장하는 부분
				previousCaptchaImageKey = String.valueOf(clientInfoList.get(1));
				ipAddressKey = String.valueOf(clientInfoList.get(2));
				
				// Background Task를 위한 파라미터를 저장하는 부분
				TaskParamVO taskParam = new TaskParamVO();
				taskParam.setEmptyCaptchaImagePool(isEmptyCaptchaImagePool);
				taskParam.setIssuedCaptchaImageKey(selectedCaptchaImageKey);
				taskParam.setPreviousCaptchaImageKey(previousCaptchaImageKey);
				taskParam.setIpAddressKey(ipAddressKey);
				taskParam.setCaptchaImagePoolKey(captchaImagePoolKey);
				taskParam.setClientKey(clientKey);
				// Background Task를 실행하는 부분
				captchaImageScoreTask.startScheduler(param.getActionType(), taskParam);
			}
		}catch(Exception e) {
			log.error("[getCaptchaImageBase64] UserMessage  : CaptchaPoolSize가 제일 큰 타입중에서 랜덤한 CaptchaImage의 Base64정보를 변환하는 도중 에러발생");
			log.error("[getCaptchaImageBase64] SystemMessage: {}", e.getMessage());
			log.error("[getCaptchaImageBase64] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
			b64 = "";
		}
		return b64;
	}
	
	/*
	 * Func : 파라미터로 전달받은 clientKey가 어뷰저인지를 판단하고, 어뷰저일 경우에도 어려운모드의 CaptchaImagePool 크기가 충분할때에만 true를 리턴한다.
	 *        (어려운모드의 CaptchaImagePool 크기가 충분하지 않으면 비슷한 문제가 계속 출제될수도 있으므로 어려운모드의 CaptchaImagePool 크기가 최소크기를 넘지 않으면 사용하지 않는다)
	 */
	public boolean checkAbuserClient(String clientKey) throws Exception{
		String captchaImagePoolKey = "";		// var : CaptchaImagePool의 키값을 저장하는 변수
		int captchaImagePoolSize = 0;			// var : CaptchaImagePool의 크기를 저장하는 변수
		boolean isAbuserClient = false;			// var : Client가 어뷰저인지 유무를 저장하는 변수
		try {
			// clientKey isAbuser필드에 저장된 값을 가져오는 부분
			isAbuserClient = Boolean.valueOf(String.valueOf(jedis.hget(clientKey, CommonImageInfo.isAbuserField)));
			if(isAbuserClient) {
				// client가 어뷰저라고 하더라도 어려운모드의 CaptchaImagePool 크기를 확인하여 하나의 타입이라도 최소 크기보다 크다면 true를 리턴한다
				isAbuserClient = false;
				for(String type : CommonImageInfo.typeResizedList) {
					captchaImagePoolKey = CommonImageInfo.captchaImageHardModePoolKeyHeader + type;
					captchaImagePoolSize = jedis.llen(captchaImagePoolKey).intValue();
					if(captchaImagePoolSize >= CommonImageInfo.minCaptchaImageHardModePoolSize) {
						isAbuserClient = true;
						break;
					}
				}
			}
		}catch(Exception e) {
			log.error("[checkAbuserClient] UserMessage  : Redis에 저장된 ClientKey의 isAbuser를 가져와 1차로 판별하고 CaptchaImageHardModePool의 크기로 2차 판별하던 중에 에러발생");
			log.error("[checkAbuserClient] SystemMessage: {}", e.getMessage());
			log.error("[checkAbuserClient] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
			isAbuserClient = false;
		}
		return isAbuserClient;
	}
	
	/*
	 * Func : client가 요청한 캡차이미지의 시작위치를 Redis에 저장하고, AxisVO 객체에 담아 리턴하는 함수
	 *        (js내에서 빨간색 마커의 시작위치를 알려주기 위해 사용)
	 */
	public AxisVO getStartAxis(String clientKey) throws Exception{
		Random random = new Random();
		AxisVO startAxis = null;		// var : 시작좌표를 저장하기 위한 객체 변수
		int startXAxis = 0;				// var : 시작좌표의 X축을 저장하는 변수
		int startYAxis = 0;				// var : 시작좌표의 Y축을 저장하는 변수
		Pipeline pipeLine = null;
		try {
			startAxis = new AxisVO();
			// 예를 들어 width=height=300, imageCellSize=100일때, 
			// {0, 100, 200} 중에서 랜덤한 값을 얻기 위해 아래와 같은 공식으로 계산하였으며 해당 좌표는 9개의 이미지 중에서 좌측 상단의 좌표를 가르키고 있다
			startXAxis = random.nextInt((CommonImageInfo.width - CommonImageInfo.imageCellSize)/CommonImageInfo.imageCellSize) * CommonImageInfo.imageCellSize;
			startYAxis = random.nextInt((CommonImageInfo.height - CommonImageInfo.imageCellSize)/CommonImageInfo.imageCellSize) * CommonImageInfo.imageCellSize;
			
			// Redis에 시작좌표를 저장하는 부분
			pipeLine = jedis.pipelined();
			pipeLine.hset(clientKey, CommonImageInfo.startXAxisField, String.valueOf(startXAxis));
			pipeLine.hset(clientKey, CommonImageInfo.startYAxisField, String.valueOf(startYAxis));
			pipeLine.sync();
			
			// AxisVO 객체에 시작좌표를 저장하는 부분
			startAxis.setxAxis(startXAxis);
			startAxis.setyAxis(startYAxis);
		}catch(Exception e) {
			log.error("[getStartAxis] UserMessage  : 캡차문제에 대한 시작위치를 설정하던 중에 에러발생");
			log.error("[getStartAxis] SystemMessage: {}", e.getMessage());
			log.error("[getStartAxis] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
			startAxis = null;
		}
		return startAxis;
	}
}
