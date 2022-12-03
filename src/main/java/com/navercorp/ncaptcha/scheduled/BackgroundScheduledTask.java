package com.navercorp.ncaptcha.scheduled;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.navercorp.ncaptcha.config.RedisConfig;
import com.navercorp.ncaptcha.domain.CommonImageInfo;
import com.navercorp.ncaptcha.domain.ExamImageInfoVO;
import com.navercorp.ncaptcha.mapper.ImageMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisConnectionException;

@Component
public class BackgroundScheduledTask {
	@Autowired
	RedisConfig RedisConfig;
	Jedis jedis = new Jedis();
	@Autowired
	ImageMapper imageMapper;
	
	private static final Logger log = LoggerFactory.getLogger(BackgroundScheduledTask.class);

//	/*
//	 * Func : Redis에 저장된 보기PanoramaImage를 MySQL에 저장된 보기이미지로 전체적으로 교체하는 함수
//	 */
//	@Scheduled(fixedDelay=1800000)	// 1800초(30분)
//	public void changeExamPanoramaImageTask() throws Exception{
//		log.info("[ChangeExamPanoramaImage-backgroundTask] start");
//		long start = System.currentTimeMillis();
//		
//		String examPanoramaImageType = "";							// var : 보기PanoramaImage의 타입(school, bridge..)을 저장하는 변수
//		String examPanoramaImageKey = "";							// var : 보기PanoramaImage의 키값을 저장하는 변수
//		String examPanoramaImageKeyArray[] = new String[2];			// var : 36진수로 변환되지 않은 imageKey(panoTypeCd_bzstNo)를 split한 문자열 배열을 저장하는 변수
//		List<ExamImageInfoVO> examPanoramaImageInfoList = null;		// var : MySQL에서 최대 크기만큼 보기PanoramaImagePool을 가져온 값을 저장하는 ExamImageInfoVO 리스트형식의 변수
//		Pipeline pipeLine = null;
//		try {
//			jedis = RedisConfig.getJedis();
//			// MySQL에서 최대 크기만큼 보기PanoramaImagePool을 가져와 리스트 객체에 저장하는 부분 
//			examPanoramaImageInfoList = imageMapper.selectExamImageInfoList(CommonImageInfo.maxExamPanoramaImagePoolSize);
//			
//			// MySQL에서 보기PanoramaImage를 정상적으로 가져왔을때 Redis내에 교체작업 수행
//			if(examPanoramaImageInfoList.size() > 0) {
//				pipeLine = jedis.pipelined();
//				// MySQL에서 최대 크기만큼 가져온 보기PanoramaImage를 MySQL내에서 isUsed필드를 '1'로 변경하는 부분 (Batch발생시 isUsed=1인 row들은 모두 삭제된다)
//				imageMapper.updateExamImageInfoList(CommonImageInfo.maxExamPanoramaImagePoolSize);
//				// MySQL에서 가져온 보기 PanoramaImagePool의 크기만큼 반복문을 돌려 Redis에 저장된 보기PanoramaImage를 교체하는 부분
//				for(ExamImageInfoVO imageInfo : examPanoramaImageInfoList) {
//					// 이미지의 타입을 저정하는 부분으로 Redis에 저장되는 데이터의 크기를 줄이기 위해 이미지타입을 앞의 두개 문자만 짤라서 사용한다 (school --> sc)
//					examPanoramaImageType = imageInfo.getTypeName().substring(0,2);
//					// imageKey를 split하여 문자열 배열변수에 저장하는 부분
//					examPanoramaImageKeyArray = imageInfo.getImageKey().split("_");
//					// 각각의 panoTypeCd와  bzstNo를 36진수로 변환하여 다시 imageKey를 생성하는 부분
//					examPanoramaImageKey = Long.toString(Integer.parseInt(examPanoramaImageKeyArray[0]), 36) + 
//											"_" + 
//											Long.toString(Integer.parseInt(examPanoramaImageKeyArray[1]), 36);
//					// 보기 PanoramaImagePool에 MySQL에서 가져온 보기이미지를 추가하는 부분
//					pipeLine.rpush(CommonImageInfo.examPanoramaImagePoolKeyHeader + examPanoramaImageType, examPanoramaImageKey);
//					// MySQL에서 가져온 보기 PanoramaImage의 base64정보를 Redis에 저장하는 부분
//					pipeLine.hset(examPanoramaImageKey, CommonImageInfo.base64Field, imageInfo.getB64());
//					// 기존에 Redis에 존재하고 있던 보기 PanoramaImage를 삭제하는 부분
//					pipeLine.lpop(CommonImageInfo.examPanoramaImagePoolKeyHeader + examPanoramaImageType);
//				}
//				pipeLine.sync();
//			}
//		}catch(Exception e) {
//			log.error("[ChangeExamPanoramaImage-backgroundTask] UserMessage  : 보기 PanoramaImage를 MySQL에서 select하여 Redis에 교체저장하는 도중 에러발생");
//			log.error("[ChangeExamPanoramaImage-backgroundTask] SystemMessage: {}", e.getMessage());
//			log.error("[ChangeExamPanoramaImage-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
//		}
//		long end=System.currentTimeMillis();
//		log.info("[ChangeExamPanoramaImage-backgroundTask] end (runtime: " + (end-start)/1000.0 +")");
//	}
//	
//	/*
//	 * Func : 일정시간마다 각 타입(school,bridge..)의 CaptchaImage들의 Score를 재계산하여 일반모드와 어려운모드로 부분하는 함수 
//	 */
//	@Scheduled(fixedDelay=600000)	// 600초(10분)
//	public void scoreCalculateTask() throws Exception{
//		log.info("[ScoreCalculate-backgroundTask] start");
//		long start = System.currentTimeMillis();
//		
//		int captchaImageScore = 0, captchaImageScoreSum = 0, captchaImagePoolSize = 0;	// var : captchaImage의 스코어, 스코어합계, captchaImagePool의 크기를 저장하는 변수
//		double captchaImageScoreAvg = 0.0;												// var : captchaImage의 스코어 평균값을 저장하는 변수
//		Map<String, List<String>> totalCaptchaImagePoolMap = null;						// var : 모든 모드(일반모드/어려운모드)의 CaptchaImagePool을 저장하는 변수
//		List<String> captchaImagePool = null;											// var : 특정 하나의 모드에 대한 CaptchaImagePool을 저장하는 변수
//		String captchaImageMode = "";													// var : CaptchaImagePool이 일반모드인지 어려운모드인지 구분하는 값을 저장하는 변수
//		Pipeline pipeLine = null;
//		Response<List<String>> answerCaptchaImagePoolList = null;
//		Response<List<String>> examCaptchaImagePoolList = null;
//		List<Object> captchaImageScoreSumList = null;
//		try {
//			jedis = RedisConfig.getJedis();
//			// 각각의 타입별로 captchaImagePool의 score를 계산하기 위해 모든 타입에 대해 반복문을 돌리는 부분
//			for(String type : CommonImageInfo.typeResizedList) {
//				pipeLine = jedis.pipelined();
//				// 모든 모드(일반모드/어려운모드)의 CaptchaImagePool을 Redis에서 가져오는 부분
//				answerCaptchaImagePoolList = pipeLine.lrange(CommonImageInfo.captchaImagePoolKeyHeader + type, 0, -1);
//				examCaptchaImagePoolList = pipeLine.lrange(CommonImageInfo.captchaImageHardModePoolKeyHeader + type, 0, -1);
//				pipeLine.sync();
//				
//				// 모든 모드(일반모드/어려운모드)의 CaptchaImagePool을 하나의 HashMap에 저장하는 부분
//				totalCaptchaImagePoolMap = new HashMap<String, List<String>>();
//				totalCaptchaImagePoolMap.put(CommonImageInfo.captchaImageNormalModeKey, answerCaptchaImagePoolList.get());
//				totalCaptchaImagePoolMap.put(CommonImageInfo.captchaImageHardModeKey, examCaptchaImagePoolList.get());
//				
//				// 하나의 타입에 대한 일반모드와 어려운모드의 전체의 score총합과 평균값을 계산하는 부분 (평균값은 captchaImage가 일반모드 또는 어려운모드로 이동시켜야하는지에 대한 기준값의 역할을 한다)
//				pipeLine = jedis.pipelined();
//				for(Entry<String, List<String>> entry : totalCaptchaImagePoolMap.entrySet()) {
//					captchaImagePool = entry.getValue();				
//					for(String captchaImageKey : captchaImagePool) {
//						pipeLine.hget(captchaImageKey, CommonImageInfo.captchaImageScoreField);
//					}
//					// 평균값을 구하기 위해 모든 captchaImagePool의 전체개수를 계산하는 부분
//					captchaImagePoolSize += captchaImagePool.size();
//				}
//				captchaImageScoreSumList = pipeLine.syncAndReturnAll();
//				for(Object captchaImageScoreSumItem : captchaImageScoreSumList) {
//					// captchaImage의 모든 Score의 합계를 계산하는 부분
//					captchaImageScoreSum += Integer.parseInt(String.valueOf(captchaImageScoreSumItem));
//				}
//				captchaImageScoreAvg = (double)captchaImageScoreSum / captchaImagePoolSize;
//				
//				// score평균값을 기준으로 captchaImage를 일반모드에서 어려운모드로, 또는 어려운모드에서 일반모드로 이동시키는 부분
//				for(Entry<String, List<String>> entry : totalCaptchaImagePoolMap.entrySet()) {
//					// 하나의 모드에 대한 CaptchaImagePool을 저장하는 부분
//					captchaImagePool = entry.getValue();	
//					// 해당 모드에 대한 모드명을 저장하는 부분
//					captchaImageMode = entry.getKey();
//					// 하나믜 모드에 대한 CaptchaImagePool을 반복문을 돌려 하나하나씩 평균값과 비교하고 평균값보다 낮을때는 일반모드로, 평균값보다 높을때는 어려운모드로 이동시키는 부분 
//					for(String captchaImageKey : captchaImagePool) {
//						captchaImageScore = Integer.parseInt(jedis.hget(captchaImageKey, CommonImageInfo.captchaImageScoreField));
//						pipeLine = jedis.pipelined();
//						if(captchaImageMode.equals(CommonImageInfo.captchaImageNormalModeKey)) {
//							// 일반모드의 CaptchaImagePool에서 평균Score보다 높은 score를 가진 CaptchaImage가 있다면 일반모드 captchaImagePool에서 삭제하고, 어려운모드 captchaImagePool에 삽입한다. 
//							if(captchaImageScore > captchaImageScoreAvg) {
//								pipeLine.lrem(CommonImageInfo.captchaImagePoolKeyHeader + type, 0, captchaImageKey);
//								pipeLine.rpush(CommonImageInfo.captchaImageHardModePoolKeyHeader + type, captchaImageKey);
//							}
//						}else if(captchaImageMode.equals(CommonImageInfo.captchaImageHardModeKey)) {
//							// 어려운 모드의 CaptchaImagePool에서 평균Score보다 낮은 score를 가진 CaptchaImage가 있다면 어려운 모드 captchaImagePool에서 삭제하고, 일반모드 captchaImagePool에 삽입한다.
//							if(captchaImageScore <= captchaImageScoreAvg) {
//								pipeLine.lrem(CommonImageInfo.captchaImageHardModePoolKeyHeader + type, 0, captchaImageKey);
//								pipeLine.rpush(CommonImageInfo.captchaImagePoolKeyHeader + type, captchaImageKey);
//							}
//						}
//						pipeLine.sync();
//					}
//				}
//				
//				// for문을 통해 반복되므로 이전의 값들이 다음 반복문에 영향을 주지않도록 초기화시켜주는 부분
//				captchaImageScoreSum = 0;
//				captchaImagePoolSize = 0;
//				captchaImageScoreAvg = 0.0;
//			}
//		}catch(Exception e) {
//			log.error("[ScoreCalculate-backgroundTask] UserMessage  : 각 타입별 CaptchaImage Score를 계산하여 쉬운모드/어려운모드로 구분하는 작업 도중 에러발생");
//			log.error("[ScoreCalculate-backgroundTask] SystemMessage: {}", e.getMessage());
//			log.error("[ScoreCalculate-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
//		}
//		long end=System.currentTimeMillis();
//		log.info("[ScoreCalculate-backgroundTask] end (runtime: " + (end-start)/1000.0 +")");
//	}
//	
//	/*
//	 * Func : Redis의 ipAddressList, clientList에서 각각의 ipAddressKey와 clientKey는 TTL에 의해 삭제되었지만 리스트에 남아있는 Key들을 정리해주는 함수
//	 *        사용자가 캡차문제를 풀지 않고(Success를 하지않고) 종료하였을때 TTL에 의해 Key값들은 삭제되지만 List에 있는 Key값들은 그대로 남아있으므로 주기적으로 삭제를 해주는 함수
//	 */
//	@Scheduled(fixedDelay=3600000)	// 3600초(1시간)
//	public void deleteUnusedKeyList() throws Exception{
//		log.info("[DeleteUnusedKey-backgroundTask] start");
//		long start = System.currentTimeMillis();
//		
//		List<String> ipAddressKeyList = null;		// var : ipAddressPool을 리스트 형식으로 저장하는 변수
//		List<String> clientKeyList = null;			// var : clientPool을 리스트 형식으로 저장하는 변수
//		Pipeline pipeLine = null;
//		Response<List<String>> ipAddressPoolList = null;
//		Response<List<String>> clientPoolList = null;
//		List<Object> ipAddressKeyCheckList = null;
//		List<Object> clientKeyCheckList = null;
//		try {
//			jedis = RedisConfig.getJedis();
//			// 전체 ipAddressPool과 clientPool의 리스트를 Redis에서 가져오는 부분
//			pipeLine = jedis.pipelined();
//			ipAddressPoolList = pipeLine.lrange(CommonImageInfo.ipAddressPoolKey, 0, -1);
//			clientPoolList = pipeLine.lrange(CommonImageInfo.clientPoolKey, 0, -1);
//			pipeLine.sync();
//			
//			// ipAddressPool에 저장된 isAbuser필드를 모두 체크하여 ipAddressPool에는 존재하지만 ipAddressKey가 TTL에 의해 삭제된 Key가 있는지 확인하는 부분
//			pipeLine = jedis.pipelined();
//			ipAddressKeyList = ipAddressPoolList.get();
//			for(String ipAddressKey : ipAddressKeyList) {
//				pipeLine.hget(ipAddressKey, CommonImageInfo.isAbuserField);
//			}
//			ipAddressKeyCheckList = pipeLine.syncAndReturnAll();
//			pipeLine = jedis.pipelined();
//			for(Object ipAddressKey : ipAddressKeyCheckList) {
//				if(ipAddressKey != null) {
//					pipeLine.lrem(CommonImageInfo.ipAddressPoolKey, 0, String.valueOf(ipAddressKey));
//				}
//			}
//			pipeLine.sync();
//			
//			// clientPool에 저장된 isAbuser필드를 모두 체크하여 clientPool에는 존재하지만 clientKey가 TTL에 의해 삭제된 Key가 있는지 확인하는 부분
//			pipeLine = jedis.pipelined();
//			clientKeyList = clientPoolList.get();
//			for(String clientKey : clientKeyList) {
//				pipeLine.hget(clientKey, CommonImageInfo.isAbuserField);
//			}
//			clientKeyCheckList = pipeLine.syncAndReturnAll();
//			pipeLine = jedis.pipelined();
//			for(Object clientKey : clientKeyCheckList) {
//				if(clientKey != null) {
//					pipeLine.lrem(CommonImageInfo.clientPoolKey, 0, String.valueOf(clientKey));
//				}
//			}
//			pipeLine.sync();
//		}catch(Exception e) {
//			log.error("[DeleteUnusedKey-backgroundTask] UserMessage  : ClientKeyList와 ipAddressList내에 존재하는 불필요한 Key들을 Redis에서 제거하는 도중 에러발생");
//			log.error("[DeleteUnusedKey-backgroundTask] SystemMessage: {}", e.getMessage());
//			log.error("[DeleteUnusedKey-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
//		}
//		long end = System.currentTimeMillis();
//		log.info("[DeleteUnusedKey-backgroundTask] end (runtime: " + (end-start)/1000.0 + ")");
//	}
}
