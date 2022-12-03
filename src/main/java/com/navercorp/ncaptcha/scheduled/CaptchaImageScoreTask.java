package com.navercorp.ncaptcha.scheduled;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import com.navercorp.ncaptcha.config.RedisConfig;
import com.navercorp.ncaptcha.domain.CommonImageInfo;
import com.navercorp.ncaptcha.domain.TaskParamVO;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

@Component
public class CaptchaImageScoreTask {
	Jedis jedis = new Jedis();
	
	@Value("${" + CommonImageInfo.projectEnvironment + ".batch.url.setPanoramaImagePool}")
	private String setPanoramaImagePoolAPI;													// var : panoramaImagePool을 생성하는 Batch의 API URL을 저장하는 변수		
	@Value("${" + CommonImageInfo.projectEnvironment + ".batch.url.setCaptchaImagePool}")
	private String setCaptchaImagePoolAPI;													// var : captchaImagePool을 생성하는 Batch의 API URL을 저장하는 변수
	@Value("${" + CommonImageInfo.projectEnvironment + ".batch.token}")
	private String authenticatedToken;														// var : RemoteBuild API 호출시 인증토큰을 저장하는 변수
	
	private ThreadPoolTaskScheduler scheduler;
	
	private static final Logger log = LoggerFactory.getLogger(CaptchaImageScoreTask.class);
	
	/*
	 * Func : Thread를 종료하는 함수
	 */
	public void stopScheduler(){
		scheduler.shutdown();
	}
	
	/*
	 * Func : Thread를 시작하는 함수로 사용자의 액션(SUCCESS, FAIL, REFRESH, ISSEUD)에 따라 다른 메서드를 호출시키는 함수
	 *        CaptchaImagePool이 부족한 경우 RemoteBuild가 실행중인지 아닌지를 판단하여 RemoteBuild 실행
	 */
	public void startScheduler(String action, TaskParamVO taskParam) throws Exception {
        scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
		
        try {
        	// ImageHandler Service단에서 CaptchaImagePool을 계산하였을때 일정개수 이하로 떨어졌을 경우
        	if(taskParam.isEmptyCaptchaImagePool()) {
        		if(CommonImageInfo.isAvailableRemoteBuildBatchJob) {
        			// Remote Build를 사용가능한 경우 PanoramaImagePool을 확인하여 Remote Build 실행
        			selectRemoteBuildBatchJob();
            	}else {
            		// Remote Build이 사용불가한 경우 현재 배치스크립트가 실행중인지 확인하기 위하여 CaptchaImagePool Size를 체크 
            		// (isAvailableRemoteBuildBatchJob=false)인 경우는 현재 RemoteBuild가 실행중인 상태이고, CaptchaImagePool이 아직 채워지지 않은 상태 
            		if(checkAvailableRemoteBuildBatchJob()) {
            			// CaptchaImagePool이 사용가능한 상태로 채워져 있을때 (RemoteBuild가 실행 완료되었을때) PanoramaImagePool을 확인하여 Remote Build 실행
            			selectRemoteBuildBatchJob();
            		}
            	}
        		// CaptchaImagePool이 50% 이상 소진되었을 때에는 score기능을 매 액션이 발생할때마다 재계산한다.
        		// 특정시간마다 score 재계산 task가 실행되기 전에 captchaImagePool이 모두 소진되면 score기능이 무의미하므로 해당 기능추가 
        		setScoreCalculate();
    		}
        	
        	// 파라미터로 전달받은 action(SUCCESS, FAIL, REFRESH, ISSUED)에 따라 다른 메서드를 호출하는 부분
            switch(action) {
	            case CommonImageInfo.issuedAction:
	            	// ISSUED : 캡차이미지 발행시 
	            	scheduler.execute(getActionTask(taskParam, CommonImageInfo.issuedAction, ""));
	            	break;
	            case CommonImageInfo.refreshAction:
	            	// REFERSH : 사용자가 캡차이미지에 대한 새로고침을 눌렀을때
	            	scheduler.execute(getActionTask(taskParam, CommonImageInfo.refreshAction, CommonImageInfo.refreshScore));
	            	break;
	            case CommonImageInfo.failAction:
	            	// FAIL : 사용자가 캡차문제를 틀렸을때
	            	scheduler.execute(getActionTask(taskParam, CommonImageInfo.failAction, CommonImageInfo.failScore));
	            	break;
	            case CommonImageInfo.successAction:
	            	// SUCCESS : 사용자가 캡차문제를 맞췄을때의
	            	scheduler.execute(getActionTask(taskParam, CommonImageInfo.successAction, CommonImageInfo.successScore));
	            	break;
            }
        }catch(Exception e) {
        	log.error("[startScheduler-backgroundTask] UserMessag   : CaptchaImagePoolSize를 체크하고 파라미터로 넘어온 Action에 맞는 Thread를 실행시키는 도중 에러발생");
			log.error("[startScheduler-backgroundTask] SystemMessage: {}", e.getMessage());
			log.error("[startScheduler-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
        }
    }
	
	/*
	 * Func : Thread로 액션에 따라 메서드를 실행시키는 함수로써 issuedCnt, refreshCnt, failCnt 등을 계산해야되는지 유무를 확인하는 함수
	 */
	private Runnable getActionTask(TaskParamVO taskParam, String action, String actionScore) {
		return () -> {
			String clientKey = "";			// var : 파라미터로 넘겨받은 특정 액션을 취한 Client를 구분하기 위한 키값(ClientKey)을 저장하는 변수
			String captchaImageKey = "";	// var : 파라미터로 넘겨받은 Client에게 리턴된 이미지에 대한 키값(imageKey)을 저장하는 변수
			try {
				// 파라미터로 넘겨받은 clientKey를 변수에 저장하는 부분
				clientKey = taskParam.getClientKey();				
				
				// 파라미터로 넘겨받은 ClientKey의 refreshCnt, failCnt를 계산하여 CaptchaImage의 Issued count 계산에 포함할지 유무를 체크하는 부분
				if(getIssuedImageValidation(taskParam, action)) {
					// 파라미터로 넘겨받은 ClientKey가 해당 CaptchaImage 한 개에 몇번 관여했는지 확인하고 score 계산에 포함할지 유무를 체크하는 부분  
					if(getImageClientListValidation(taskParam)) {
						// "!(not)"을 사용하여도 되지만 좀더 issuedAction이 아닐때라는 프로세스가 진행된다는 것이 보여지기 쉽도록 "=="를 사용
						if(action.equals(CommonImageInfo.issuedAction) == false) {
							// CaptchaImage의 HistoryPool을 확인하여 history를 추가하는 부분
							checkImageHistoryValidation(taskParam, actionScore);
						}
					}
				}
				// Client의 액션(SUCCESS, FAIL, REFRESH)에 따라 Client의 정보를 업데이트하는 부분
				setClientCountInfo(taskParam, action);
				
				
				
				// Test Code
				if(action.equals(CommonImageInfo.successAction)) captchaImageKey = taskParam.getPreviousCaptchaImageKey();
				else captchaImageKey = taskParam.getIssuedCaptchaImageKey();
				
				printImageInfo(captchaImageKey, clientKey);
				//
			}catch(Exception e) {
				log.error("[getActionTask-backgroundTask] UserMessage  : Client의 액션을 카운트할지 유효성체크를 하는 도중 에러발생");
				log.error("[getActionTask-backgroundTask] SystemMessage: {}", e.getMessage());
				log.error("[getActionTask-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
			}
			// Thread의 성공, 실패와 상관없이 종료시키는 부분
			stopScheduler();
		};
	}
	
	/*
	 * Func : Redis의 PanoramaImagePool 크기를 확인하고 이에 맞는 batch를 호출하는 함수
	 */
    public void selectRemoteBuildBatchJob() throws Exception{
    	boolean isAvailablePanoramaImagePool = true;		// var : CaptchaImagePool을 생성하는 Batch를 실행시킬지, PanoramaImagePool을 생성하는 Batch부터 실행시킬지 유무를 저장하는 변수
    	Pipeline pipeLine = null;
    	List<Object> panoramaImagePoolSizeList = null;
    	try {
    		pipeLine = jedis.pipelined();
    		// 반복문을 돌려 각각의 타입에 대한 정답 PanoramaImagePool의 크기를 체크하고 최소 CaptchaImagePool보다 PanoramaImagePool의 크기가 더 작으면 PanoramaImagePool을 만드는 Batch를 실행시키고
        	// CaptchaImagePool이 PanoramaImagePool의 크기보다 더 크면 바로 CaptchaImagePool을 만드는 Batch를 실행시킨다.
    		for(String type : CommonImageInfo.typeResizedList) {
    			pipeLine.llen(CommonImageInfo.answerPanoramaImagePoolKeyHeader + type);
    			pipeLine.llen(CommonImageInfo.examPanoramaImagePoolKeyHeader + type);
    		}
    		panoramaImagePoolSizeList = pipeLine.syncAndReturnAll();
    		for(Object panoramaImagePoolSize : panoramaImagePoolSizeList) {
    			if(Integer.parseInt(String.valueOf(panoramaImagePoolSize)) <= CommonImageInfo.minCaptchaImagePoolSize) {
    				// false값을 저장하여 PanoramaImagePool을 만드는 Batch만 실행되고, CaptchaImagePool을 만드는 Batch는 실행시키지 않도록 한다
        			// PanoramaImagePool을 만드는 Batch가 실행시에는 빌드후 조치로 CaptchaImagePool을 만드는 Batch를 바로 실행시키므로 해당 방식으로 만듬
    				isAvailablePanoramaImagePool = false;
        			break;
    			}
    		}
        	// 각각의 타입 중에서 하나라도 CaptchaImagePool을 구성하는데 PanoramaImagePool의 크기가 부족하지 않으면 바로 CaptchaImagePool을 만드는 Batch를 실행시킨다
        	if(isAvailablePanoramaImagePool) {
        		setRemoteBuildBatchJob(setCaptchaImagePoolAPI);
        	}
        	// 배치를 실행시켰으므로 CaptchaImagePool이 사용가능한 수준으로 채워질때까지 RemoteBuild를 중복빌드되지 않도록 false값을 유지한다. 
        	CommonImageInfo.isAvailableRemoteBuildBatchJob = false;
    	}catch(Exception e) {
    		log.error("[selectRemoteBuildBatchJob-backgroundTask] UserMessage  : 정답PanoramaImagePool의 크기를 계산하고 그 크기에 따라 다른 배치를 실행시키는 도중 에러발생");
			log.error("[selectRemoteBuildBatchJob-backgroundTask] SystemMessage: {}", e.getMessage());
			log.error("[selectRemoteBuildBatchJob-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
    	}
    }
    
    /*
     * Func : Remote Build가 중복 Build 되지 않게하기 위해 CaptchaImagePool이 사용가능한 상태까지 채워지면 true를 리턴하는 함수	
     */
    public boolean checkAvailableRemoteBuildBatchJob() throws Exception{
    	Pipeline pipeLine = null;
    	List<Object> captchaImagePoolSizeList = null;
    	try {
    		pipeLine = jedis.pipelined();
    		// 반복문을 돌려 각각의 타입에 대한 Redis에 저장된 CaptchaImagePool의 크기를 가져오는 부분 
    		for(String type : CommonImageInfo.typeResizedList) {
    			pipeLine.llen(CommonImageInfo.captchaImagePoolKeyHeader + type);
    		}
    		captchaImagePoolSizeList = pipeLine.syncAndReturnAll();
    		// 반복문을 돌려 각각의 타입 중에서 하나라도 사용가능한 최대 CaptchaImagePool크기만큼 채워지지 않으면 false를 리턴하는 부분
    		for(Object captchaImagePoolSize : captchaImagePoolSizeList) {
    			if(CommonImageInfo.maxAvailableCaptchaImagePoolSize > Integer.parseInt(String.valueOf(captchaImagePoolSize))) {
    				CommonImageInfo.isAvailableRemoteBuildBatchJob = false;
        			return false;
    			}
    		}
        	CommonImageInfo.isAvailableRemoteBuildBatchJob = true;
        	return true;
    	}catch(Exception e) {
    		log.error("[checkAvailableRemoteBuildBatchJob-backgroundTask] UserMessage  : RemoteBuild가 끝났는지 확인하기 위하여 CaptchaImagePool을 체크하는 도중 에러발생");
			log.error("[checkAvailableRemoteBuildBatchJob-backgroundTask] SystemMessage: {}", e.getMessage());
			log.error("[checkAvailableRemoteBuildBatchJob-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
    		return false;
    	}
    }
    
    /*
     * Func : CaptchaImagePoolSize가 특정 사이즈만큼 소진되었는지를 확인하고 일정 사이즈만큼 소진되면 CaptchaImage의 Score가 특정 시간마다 계산되기 전에 바로 계산하는 함수
     */
    public void setScoreCalculate() throws Exception{
		int captchaImageScore = 0, captchaImageScoreSum = 0, captchaImagePoolSize = 0;	// var : captchaImage의 스코어, 스코어합계, captchaImagePool의 크기를 저장하는 변수
		double captchaImageScoreAvg = 0.0;												// var : captchaImage의 스코어 평균값을 저장하는 변수
		Map<String, List<String>> totalCaptchaImagePoolMap = null;						// var : 모든 모드(일반모드/어려운모드)의 CaptchaImagePool을 저장하는 변수
		List<String> captchaImagePool = null;											// var : 특정 하나의 모드에 대한 CaptchaImagePool을 저장하는 변수
		String captchaImageMode = "";													// var : CaptchaImagePool이 일반모드인지 어려운모드인지 구분하는 값을 저장하는 변수
		Pipeline pipeLine = null;
		Response<List<String>> answerCaptchaImagePoolList = null;
		Response<List<String>> examCaptchaImagePoolList = null;
		List<Object> captchaImageScoreSumList = null;
		try {
			// 각각의 타입별로 captchaImagePool의 score를 계산하기 위해 모든 타입에 대해 반복문을 돌리는 부분
			for(String type : CommonImageInfo.typeResizedList) {
				pipeLine = jedis.pipelined();
				// 모든 모드(일반모드/어려운모드)의 CaptchaImagePool을 하나의 HashMap에 저장하는 부분
				answerCaptchaImagePoolList = pipeLine.lrange(CommonImageInfo.captchaImagePoolKeyHeader + type, 0, -1);
				examCaptchaImagePoolList = pipeLine.lrange(CommonImageInfo.captchaImageHardModePoolKeyHeader + type, 0, -1);
				pipeLine.sync();
				
				totalCaptchaImagePoolMap = new HashMap<String, List<String>>();
				totalCaptchaImagePoolMap.put(CommonImageInfo.captchaImageNormalModeKey, answerCaptchaImagePoolList.get());
				totalCaptchaImagePoolMap.put(CommonImageInfo.captchaImageHardModeKey, examCaptchaImagePoolList.get());
				
				// 하나의 타입에 대한 일반모드와 어려운모드의 전체의 score총합과 평균값을 계산하는 부분 (평균값은 captchaImage가 일반모드 또는 어려운모드로 이동시켜야하는지에 대한 기준값의 역할을 한다)
				pipeLine = jedis.pipelined();
				for(Entry<String, List<String>> entry : totalCaptchaImagePoolMap.entrySet()) {
					captchaImagePool = entry.getValue();				
					for(String captchaImageKey : captchaImagePool) {
						pipeLine.hget(captchaImageKey, CommonImageInfo.captchaImageScoreField);
					}
					// 평균값을 구하기 위해 모든 captchaImagePool의 전체개수를 계산하는 부분
					captchaImagePoolSize += captchaImagePool.size();
				}
				captchaImageScoreSumList = pipeLine.syncAndReturnAll();
				for(Object captchaImageScoreSumItem : captchaImageScoreSumList) {
					// captchaImage의 모든 Score의 합계를 계산하는 부분
					captchaImageScoreSum += Integer.parseInt(String.valueOf(captchaImageScoreSumItem));
				}
				captchaImageScoreAvg = (double)captchaImageScoreSum / captchaImagePoolSize;
				
				// score평균값을 기준으로 captchaImage를 일반모드에서 어려운모드로, 또는 어려운모드에서 일반모드로 이동시키는 부분
				for(Entry<String, List<String>> entry : totalCaptchaImagePoolMap.entrySet()) {
					// 하나의 모드에 대한 CaptchaImagePool을 저장하는 부분
					captchaImagePool = entry.getValue();	
					// 해당 모드에 대한 모드명을 저장하는 부분
					captchaImageMode = entry.getKey();
					// 하나믜 모드에 대한 CaptchaImagePool을 반복문을 돌려 하나하나씩 평균값과 비교하고 평균값보다 낮을때는 일반모드로, 평균값보다 높을때는 어려운모드로 이동시키는 부분 
					for(String captchaImageKey : captchaImagePool) {
						captchaImageScore = Integer.parseInt(jedis.hget(captchaImageKey, CommonImageInfo.captchaImageScoreField));
						pipeLine = jedis.pipelined();
						if(captchaImageMode.equals(CommonImageInfo.captchaImageNormalModeKey)) {
							// 일반모드의 CaptchaImagePool에서 평균Score보다 높은 score를 가진 CaptchaImage가 있다면 일반모드 captchaImagePool에서 삭제하고, 어려운모드 captchaImagePool에 삽입한다. 
							if(captchaImageScore > captchaImageScoreAvg) {
								pipeLine.lrem(CommonImageInfo.captchaImagePoolKeyHeader + type, 0, captchaImageKey);
								pipeLine.rpush(CommonImageInfo.captchaImageHardModePoolKeyHeader + type, captchaImageKey);
							}
						}else if(captchaImageMode.equals(CommonImageInfo.captchaImageHardModeKey)) {
							// 어려운 모드의 CaptchaImagePool에서 평균Score보다 낮은 score를 가진 CaptchaImage가 있다면 어려운 모드 captchaImagePool에서 삭제하고, 일반모드 captchaImagePool에 삽입한다.
							if(captchaImageScore <= captchaImageScoreAvg) {
								pipeLine.lrem(CommonImageInfo.captchaImageHardModePoolKeyHeader + type, 0, captchaImageKey);
								pipeLine.rpush(CommonImageInfo.captchaImagePoolKeyHeader + type, captchaImageKey);
							}
						}
						pipeLine.sync();
					}
				}
				
				// for문을 통해 반복되므로 이전의 값들이 다음 반복문에 영향을 주지않도록 초기화시켜주는 부분
				captchaImageScoreSum = 0;
				captchaImagePoolSize = 0;
				captchaImageScoreAvg = 0.0;
			}
		}catch(Exception e) {
			log.error("[ScoreCalculate-backgroundTask] UserMessage  : 각 타입별 CaptchaImage Score를 계산하여 쉬운모드/어려운모드로 구분하는 작업 도중 에러발생");
			log.error("[ScoreCalculate-backgroundTask] SystemMessage: {}", e.getMessage());
			log.error("[ScoreCalculate-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
		}
    }
    
    /*
     * Func : Client의 refreshCnt, failCnt를 확인하여 CaptchaImage발급시 IssuedCnt를 증사킬지 유무를 체크하는 함수
     */
    public boolean getIssuedImageValidation(TaskParamVO taskParam, String action) throws Exception{
    	String clientKey = "";					// var : 파라미터로 전달받은 ClientKey를 저장하는 변수
    	int clientRefreshCnt = 0;				// var : 파라미터로 전달받은 ClientKey의 refreshCnt를 저장하는 변수
		int clientFailCnt = 0;					// var : 파라미터로 전달받은 ClientKey의 failCnt를 저장하는 변수
		String captchaImageKey = "";			// var : 파라미터로 전달받은 신규로 발급하는 CaptchaImageKey를 저장하는 변수
		int captchaImageIssuedCnt = 0;			// var : 신규로 발급하는 CaptchaImage의 발급횟수를 저장하는 변수
		Pipeline pipeLine = null;
		List<Object> clientInfoList = null;
    	try {
    		// 파라미터로 전달받은 ClientKey를 저장하는 부분
    		clientKey = taskParam.getClientKey();			
    		
    		pipeLine = jedis.pipelined();
    		// ClientKey의 refreshCnt를 Redis에서 검색하는 부분
    		pipeLine.hget(clientKey, CommonImageInfo.clientRefreshCntField);
    		// ClientKey의 failCnt를 Redis에서 검색하는 부분
    		pipeLine.hget(clientKey, CommonImageInfo.clientFailCntField);
    		clientInfoList = pipeLine.syncAndReturnAll();
    		// ClientKey의 Redis에서 검색한 refreshCnt를  변수에 저장하는 부분
    		clientRefreshCnt = Integer.parseInt(String.valueOf(clientInfoList.get(0)));
    		// ClientKey의 Redis에서 검색한 failCnt를  변수에 저장하는 부분
    		clientFailCnt = Integer.parseInt(String.valueOf(clientInfoList.get(1)));
    		
    		// Client의 refreshCnt와 failCnt가 최대refreshCnt와 최대failCnt보다 작을 경우에만 CaptchaImage의 issuedCnt를 증가시키는 부분
    		if(clientRefreshCnt < CommonImageInfo.maxClientRefreshCnt && clientFailCnt < CommonImageInfo.maxClientFailCnt) {
    			// 캡차문제를 맞춘 액션과 같은 경우 더이상 캡차이미지를 발급하지 않아도 되므로 successAction과 같은 경우에는 아래의 과정을 생략한다.
    			if(action.equals(CommonImageInfo.successAction) == false) {
    				// 파라미터로 전달받은 신규로 발급하는 CaptchaImageKey를 변수에 저장하는 부분
    				captchaImageKey = taskParam.getIssuedCaptchaImageKey();	
    				// 신규로 발급하는 CaptchaImage의 issuedCnt의 '1' 증가시킨 값을 저장하는 부분
    				captchaImageIssuedCnt = Integer.parseInt(jedis.hget(captchaImageKey, CommonImageInfo.captchaImageIssuedCntField)) + 1;	
    				
    				pipeLine = jedis.pipelined();
    				// CaptchaImage에 대한 issuedCnt를 Redis내에 업데이트하는 부분
    				pipeLine.hset(captchaImageKey, CommonImageInfo.captchaImageIssuedCntField, String.valueOf(captchaImageIssuedCnt));
    				// ClientKey가 현재 사용하고 있는 CaptchaImage를 신규로 발급된 CaptchaImageKey로 Redis내에 업데이트하는 부분
    				pipeLine.hset(clientKey, CommonImageInfo.clientUsingCaptchaImageField, captchaImageKey);
    				
    				// 신규로 발급된 captchaImage의 '1' 증가된 issuedCnt가 최대issuedCnt보다 클 경우 
    				// 1. captchaImagePool에서 해당 captchaImageKey를 삭제하고
    				// 2. deletedList에 해당 captchaImageKey를 추가하고
    				// 3. captchaImageKey와 관련된 리스트 키값(history, clientPool)에 대한 TTL을 재설정한다
    				if(CommonImageInfo.maxCaptchaImageIssuedCnt <= captchaImageIssuedCnt) {
    					// CaptchaImagePool에서 신규로 발급된 CaptchaImageKey를 삭제하는 부분
    					pipeLine.lrem(taskParam.getCaptchaImagePoolKey(), 0, captchaImageKey);
    					// deletedList에 해당 captchaImageKey를 추가하는 부분 (PanoramaImagePool을 만드는 Batch발생시, 해당 리스트에 있는 CaptchaImageKey와 같은 경우 모두 MySQL의 unused_info Table로 이동된다)
    					pipeLine.rpush(CommonImageInfo.deletedCaptchaImagePoolKey, captchaImageKey);
    					// 삭제된 captchaImageKey의 TTL을 재설정하는 부분
    					pipeLine.expire(captchaImageKey, CommonImageInfo.deletedCaptchaImageKeyTTL);
    					// 삭제된 CaptchaImageKey의 HistroyPool에 해당하는 키의 TTL을 재설정하는 부분
    					pipeLine.expire(CommonImageInfo.captchaImageHistoryPoolKeyHeader + captchaImageKey, CommonImageInfo.deletedCaptchaImageKeyTTL);
    					// 삭제된 CaptchaImageKey의 ClientPool에 해당하는 키의 TTL을 재설정하는 부분
    					pipeLine.expire(CommonImageInfo.captchaImageClientPoolKeyHeader + captchaImageKey, CommonImageInfo.deletedCaptchaImageKeyTTL);
    				}
    				pipeLine.sync();
    			}
    			return true;
    		}else {
    			// Client의 refreshCnt와 failCnt가 최대refreshCnt와 최대failCnt보다 클 경우에는 새로 발급된 CaptchaImage의 issuedCnt를 증가시키지 않는다.
    			// (해당 경우에는 Client를 어뷰저로 판단하여 어뷰저가 새로고침 또는 문제틀리기를 통해 캡차이미지롤 계속해서 발급받고 CaptchaImage가 그에따라 소거되는 어뷰징을 막기위한 부분)
    			log.warn("[IssuedCnt-" + clientKey +"] 허용된 새로고침횟수 또는 실패횟수를 초과하여 IssuedCnt에 반영하지 않음");
    		}
    	}catch(Exception e) {
    		log.error("[IssuedImageValidation-backgroundTask] UserMessage  : CaptchaImage발급시 Client의 RefreshCnt와 FailCnt를 체크하고, 최대발행횟수보다 클 경우 CaptchaImage를 삭제하는 작업 도중 에러발생");
			log.error("[IssuedImageValidation-backgroundTask] SystemMessage: {}", e.getMessage());
			log.error("[IssuedImageValidation-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
    	}
    	return false;
    }
    
    /*
     * Func : 하나의 CaptchaImage에 한명의 Client가 몇번 관여하였는지를 CaptchaImageKey에 저장된 ClientPool을 통해 확인하고, 최대관여횟수 초과시에는 History에 Client의 액션을 추가하지 않는 함수
     *        (History에 Client의 액션을 추가하지 않으므로 CaptchaImage의 score에도 영향을 주지않는다)
     */
    public boolean getImageClientListValidation(TaskParamVO taskParam) throws Exception{
    	String captchaImageKey = "";						// var : 파라미터로 전달받은 기존 발급하였던 CaptchaImageKey를 저장하는 변수
		String ipAddressKey = "";							// var : 파라미터로 전달받은 사용자의 ipAddressKey를 저장하는 변수
    	String captchaImageClientPoolKey = "";				// var : 기존 발급하였던 CaptchaImage에 관여한 Client들을 저장하는 List의 키값을 저장하는 변수
		int clientInvolvedInCaptchaImageCnt = 0;			// var : 하나의 CaptchaImage에 한명의 Client가 관여한 횟수를 저장하는 변수
		int captchaImageClientPoolSize = 0;
		Pipeline pipeLine = null;
		List<Object> captchaImageInfoList = null;
		boolean isClientListValid = false;
    	try {
    		// 파라미터로 전달받은 기존 발급되었던 captchaImageKey를 변수에 저장하는 부분 
    		captchaImageKey = taskParam.getPreviousCaptchaImageKey();
    		// 파라미터로 전달받은 사용자의 ipAddressKey를 저장하는 부분
    		ipAddressKey = taskParam.getIpAddressKey();	
    		// 기존 발급하였던 CaptchaImage에 관여한 Client들을 저장하는 List의 키값을 변수에 저장하는 부분
    		if(captchaImageKey.equals("")) {
    			clientInvolvedInCaptchaImageCnt = 0;
    		}else {
    			pipeLine = jedis.pipelined();
    			// 하나의 캡차이미지에 관여한 사용자들의 리스트 키값을 Redis에서 가져오는 부분
    			pipeLine.hget(captchaImageKey, CommonImageInfo.captchaImageClientPoolField);
    			// 하나의 캡차이미지에 관여한 사용자들의 리스트 크기를 Redis에서 가져오는 부분
    			pipeLine.llen(captchaImageClientPoolKey);
    			captchaImageInfoList = pipeLine.syncAndReturnAll();
    			// 하나의 캡차이미지에 관여한 사용자들의 리스트 키값을 저장하는 부분
    			captchaImageClientPoolKey = String.valueOf(captchaImageInfoList.get(0));
    			// 하나의 캡차이미지에 관여한 사용자들의 리스트 크기를 저장하는 부분
    			captchaImageClientPoolSize = Integer.parseInt(String.valueOf(captchaImageInfoList.get(1)));
    			
    			if(captchaImageClientPoolSize > 0) {
            		// lrem()을 통해 해당 리스트에 파라미터로 전달받은 ipAddressKey가 몇개 저장되어 있는지를 확인 (저장된 개수는 하나의 캡차이미지에 한명의 사용자가 관여한 횟수를 나타낸다)
            		clientInvolvedInCaptchaImageCnt = jedis.lrem(captchaImageClientPoolKey, 0, ipAddressKey).intValue();
            	}else {
            		clientInvolvedInCaptchaImageCnt = 0;
            	}
    		}
    																														
    		// lrem으로 clientKey들을 삭제하였으므로 그만큼 다시 ClientPool에 추가사키는 부분
    		// ClientPool은 하나의 CaptchaImage에 관여한 ClientKey들을 리스트형식으로 저장한 Pool로써, 순서와 상관없이 Client들의 개수를 저장하기 위해 사용
    		pipeLine = jedis.pipelined();
    		for(int i=0; i<clientInvolvedInCaptchaImageCnt; i++) {
    			pipeLine.rpush(captchaImageClientPoolKey, ipAddressKey);
    		}
    		
    		// 하나의 CaptchaImage에 한명의 Client가 관여한 횟수가 최대 관여횟수보다 작을 경우에는 ClientPool에 파라미터로 전달받은 ipAddressKey를 추가한다.
    		// (해당 Client의 액션은 history에 포함될 예정이므로 해당 CaptchaImage에 관여한 횟수를 체크하기 위해 ipAddressKey를 추가한다)
    		if(clientInvolvedInCaptchaImageCnt < CommonImageInfo.maxClientInvolvedInCaptchaImageCnt) {
    			pipeLine.rpush(captchaImageClientPoolKey, ipAddressKey);
    			isClientListValid = true; 
    		}else {
    			// 하나의 CaptchaImage에 한명의 Client가 관여한 횟수가 최대 관여횟수보다 클 경우에는 ClientPool에 ClientKey를 추가하지 않고, false를 리턴하여 history에도 Client의 액션이 반영되지 않도록 한다
    			log.warn("[History-" + ipAddressKey +"] 하나의 캡차이미지에 대한 한명의 Client가 허용된 관여횟수를 초과하여 History에 반영하지 않음");
    		}
    		pipeLine.sync();
    	}catch(Exception e) {
    		log.error("[getImageClientListValidation-backgroundTask] UserMessage  : 하나의 CaptchaImage에 한명의 Client가 몇번 관여했는지를 체크하는 도중 에러발생");
			log.error("[getImageClientListValidation-backgroundTask] SystemMessage: {}", e.getMessage());
			log.error("[getImageClientListValidation-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
    	}
    	return isClientListValid;
    }

    /*
     * Func : 하나의 CaptchaImage에 여러 Client가 관여한 history를 체크하고 최대history크기를 넘을 경우에는 FIFO형식으로 오래된 history는 삭제하고 신규 history를 추가하며, 
     *        history에 따른 하나의 CaptchaImage에 대한 score를 재계산하여 업데이트하는 함수
     */
    public void checkImageHistoryValidation(TaskParamVO param, String scoreString) throws Exception{
    	String ipAddressKey = "";						// var : 파라미터로 전달받은 사용자의 ipAddressKey를 저장하는 변수
		String captchaImageKey = "";					// var : 파라미터로 전달받은 사용중인 CaptchaImageKey를 저장하는 변수
		int captchaImageScore = 0;						// var : 파라미터로 전달받은 CaptchaImage의 score를 저장하는 변수
    	String clientPoolKey = "";						// var : 파라미터로 전달받은 CaptchaImage의 ClientPool의 Key를 저장하는 변수 
    	String captchaImageHistoryKey = "";				// var : 파라미터로 전달받은 CaptchaImage의 historyPool의 키를 저장하는 변수 
    	int captchaImageHistoryPoolSize = 0;			// var : 파라미터로 전달받은 CaptchaImage의 historyPool의 크기를 저장하는 변수
    	String deletedHistoryValue = "";				// var : historyPool에서 삭제한 history(ipAddressKey+score)를 저장하는 변수 ("ipAddressKey+1")
		int deletedHistoryValueLength = 0;				// var : 삭제한 history의 길이를 저장하는 변수
		String deletedHistoryScoreString = "";			// var : 삭제된 history에서 score부분 (+score) 문자열을 잘라내어 저장하는 변수 ("ipAddress+1" --> "+1") 
		String deletedIPAddressKey = "";				// var : 삭제한 history에서 ipAddressKey부분을 저장하는 변수 ("ipAddress+1" --> "ipAddress")
		char[] deletedHistoryScoreArray = null;			// var : 문자열로 된 history의 score부분을 char배열로 변경하여 저장하는 변수 ("+1" --> [0]: '+', [1]: '1')
		char deletedHistoryScoreOperator = ' ';			// var : 문자열로 된 history의 score부분에서 연산자를 저장하는 변수 ([0]: '+')
		int deletedHistoryScore = 0;					// var : 문자열로 된 history의 score부분에서 점수부분을 저장하는 변수 ([1]: '1')
		char actionScoreOperator = ' ';					// var : 파라미터로 전달받은 액션에 따른 score문자열에서 연산자 부분을 저장하는 변수 ("+1" --> '+')
		int actionScore = 0; 							// var : 파라미터로 전달받은 액션에 따른 score문자열에서 score부분을 저장하는 변수 ("+1" --> "1")
		String addedCaptchaImageHistoryKey = "";		// var : historyPool에 새로 추가할 history를 저장하는 변수 ("ipAddressKey+1")
		Pipeline pipeLine = null;
		List<Object> captchaImageInfoList = null;
    	try {
    		// 파라미터로 전달받은 사용자의 ipAddressKey를 저장하는 부분
    		ipAddressKey = param.getIpAddressKey();
    		// 파라미터로 전달받은 사용중인 CaptchaImageKey를 저장하는 부분
    		captchaImageKey = param.getPreviousCaptchaImageKey();
    		
    		pipeLine = jedis.pipelined();
    		// 파라미터로 전달받은 captchaImageKey에 해당하는 score를 Redis에서 가져오는 부분
    		pipeLine.hget(captchaImageKey, CommonImageInfo.captchaImageScoreField);
    		// 파라미터로 전달받은 captchaImageKey에 해당하는 이미지에 관여한 사용자들의 리스트 키값을 Redis에서 가져오는 부분
    		pipeLine.hget(captchaImageKey, CommonImageInfo.captchaImageClientPoolField);
    		// 파라미터로 전달받은 captchaImageKey에 해당하는 이미지에 사용자들의 관여한 히스토리를 저장하는 리스트의 키값을 Redis에서 가져오는 부분
    		pipeLine.hget(captchaImageKey, CommonImageInfo.captchaImageHistoryPoolField);
    		captchaImageInfoList = pipeLine.syncAndReturnAll();
    		captchaImageScore = Integer.parseInt(String.valueOf(captchaImageInfoList.get(0)));
    		clientPoolKey = String.valueOf(captchaImageInfoList.get(1));
    		captchaImageHistoryKey = String.valueOf(captchaImageInfoList.get(2));
    		
    		// Redis에서 가져온 히스토리 리스트의 키값을 통해 해당 리스트의 크기를 변수에 저장하는 부분
    		captchaImageHistoryPoolSize	= jedis.llen(captchaImageHistoryKey).intValue();								
        	
        	// CaptchaImage History크기가 최대크기를 넘을 경우 FIFO형식으로 가장 오래전에 추가되었던 History를 삭제하고 최신 History를 추가한다.
        	// (History 크기의 제한을 두어 Redis에 너무 많은 데이터가 저장되지 않도록 한다.) 
        	if(captchaImageHistoryPoolSize >= CommonImageInfo.maxCaptchaImageHistoryPoolSize) {
        		// historyPool에서 삭제한 history(ipAddressKey+score)를 변수에 저장하는 부분 ("ipAddressKey+1")
        		deletedHistoryValue = jedis.lpop(captchaImageHistoryKey);
        		// 삭제한 history의 길이를 변수에 저장하는 부분
        		deletedHistoryValueLength = deletedHistoryValue.length();
        		// 삭제된 history에서 score부분 (+score) 문자열을 잘라내어 변수에 저장하는 부분 ("ipAddress+1" --> "+1")
        		deletedHistoryScoreString = deletedHistoryValue.substring(deletedHistoryValueLength-2, deletedHistoryValueLength);
        		// 삭제한 history에서 ipAddressKey부분을 변수에 저장하는 부분 ("ipAddress+1" --> "ipAddress")
        		deletedIPAddressKey = deletedHistoryValue.replace(deletedHistoryScoreString, ""); 
        		
        		// 문자열로 된 history의 score부분을 char배열로 변경하여 변수에 저장하는  부분 ("+1" --> [0]: '+', [1]: '1')
        		deletedHistoryScoreArray = deletedHistoryScoreString.toCharArray();
        		// 문자열로 된 history의 score부분에서 연산자를 변수에 저장하는 부분 ([0]: '+')
        		deletedHistoryScoreOperator = deletedHistoryScoreArray[0];
        		// 문자열로 된 history의 score부분에서 점수부분을 char에서 int형으로 변환하여 변수에 저장하는 부분([1]: '1')
        		deletedHistoryScore = Character.getNumericValue(deletedHistoryScoreArray[1]);
        		
        		// 삭제되는 history의 연산자에 따라 CaptchaImage의 Score를 재계산하는 부분
        		if(deletedHistoryScoreOperator == '+') {
        			// '+'연산자인 경우, 삭제되는 history이므로 CaptchaImage의 Score에서 '+'된 점수만큼 뺄셈을 하여 score를 맞추는 부분
        			captchaImageScore -= deletedHistoryScore;
        			if(captchaImageScore < 0) captchaImageScore = 0; 
        		}else if(deletedHistoryScoreOperator == '-') {
        			// '-'연산자인 경우, 삭제되는 history이므로 CaptchaImage의 Score에서 '-'된 점수만큼 덧셈을 하여 score를 맞추는 부분
        			captchaImageScore += deletedHistoryScore;
        		}
        		// clientPool에서 삭제된 history의 clientKey를 삭제하여, clientPool에 삭제된 history의 Client가 관여한 횟수를 제외시키는 부분
        		jedis.lrem(clientPoolKey, 1, deletedIPAddressKey);
        	}
        	
        	// captchaImage의 historyPool을 최대history크기만큼 맞춘 다음 사용자의 Action(REFRESH, FAIL, SUCCESS)에 따른 CaptchaImage의 Score를 재계산하는 부분 (SUCCESS: -2, REFRESH: +1, FAIL: +2)
        	// 파라미터로 전달받은 액션에 따른 score문자열에서 연산자 부분을 변수에 저장하는 부분 ("+1" --> '+')
        	actionScoreOperator = scoreString.toCharArray()[0]; 
        	// 파라미터로 전달받은 액션에 따른 score문자열에서 점수부분을 char에서 int형으로 변환하여 변수에 저장하는 부분 ("+1" --> "1")
    		actionScore = Character.getNumericValue(scoreString.toCharArray()[1]);
    		// historyPool에 새로 추가할 history를 변수에 저장하는 부분 ("ipAddressKey+1")
    		addedCaptchaImageHistoryKey = ipAddressKey + scoreString; 
        	
    		// 추가할 history의 연산자에 따라 CaptchaImage의 Score를 재계산하는 부분
    		if(actionScoreOperator == '+') {
    			// '+' 연산자인 경우, 새로 추가할 history이므로 CaptchaImage의 Score에서 정수형 score만큼 덧셈하는 부분
    			captchaImageScore += actionScore;
    		}else if(actionScoreOperator == '-') {
    			// '-' 연산자인 경우, 새로 추가할 history이므로 CaptchaImage의 Score에서 정수형 score만큼 뺄셈하는 부분
    			captchaImageScore -= actionScore;
    			if(captchaImageScore < 0) captchaImageScore = 0;
    		}
    		
    		pipeLine = jedis.pipelined();
    		// captchaImage의 score값에 재계산된 score를 업데이트하는 부분
    		pipeLine.hset(captchaImageKey, CommonImageInfo.captchaImageScoreField, String.valueOf(captchaImageScore));
    		// captchaImage의 historyPool에 추가할 history를 업데이트하는 부분
    		pipeLine.rpush(captchaImageHistoryKey, addedCaptchaImageHistoryKey);
    		pipeLine.sync();
    	}catch(Exception e) {
    		log.error("[checkImageHistoryValidation-backgroundTask] UserMessage  : CaptchaImage의 History크기가 최대크기에 따라 새로운 History를 추가할것인가를 결정하는 부분에서 에러발생");
			log.error("[checkImageHistoryValidation-backgroundTask] SystemMessage: {}", e.getMessage());
			log.error("[checkImageHistoryValidation-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
    	}
    }
    
    /*
     * Func : Client의 액션(SUCCESS, REFRESH, FAIL)에 따라 Client의 refreshCnt, failCnt등을 업데이트하고, 
     *        Success일 경우, 어뷰저 유무에 따라 ipAddressKey를 삭제하거나 TTL로 살려두는 것을 설정하는 함수
     */
    public void setClientCountInfo(TaskParamVO taskParam, String action) throws Exception{
    	int clientRefreshCnt = 0;		// var : 파라미터로 전달받은 ClientKey의 refreshCnt를 저장하는 변수
    	int clientFailCnt = 0;			// var : 파라미터로 전달받은 ClientKey의 failCnt를 저장하는 변수
    	String ipAddressKey = "";		// var : 파라미터로 전달받은 ClientKey의 ipAddressKey를 저장하는 변수
    	String clientKey = "";
    	Pipeline pipeLine = null;
    	try {
    		clientKey = taskParam.getClientKey();
    		ipAddressKey = taskParam.getIpAddressKey();
        	switch(action) {
        		// REFRESH : clientKey의 refreshCnt를 증가시키며, 최대refreshCnt와 비교하여 어뷰저유무 판단
    	    	case CommonImageInfo.refreshAction:
    	    		// 파라미터로 전달받은 clientKey의 refreshCnt를 '1' 증가시킨 값을 저장하는 부분
    	    		clientRefreshCnt = Integer.parseInt(jedis.hget(clientKey, CommonImageInfo.clientRefreshCntField)) + 1;
    	    		// 최대 refreshCnt와 비교하여 어뷰저 유무를 판단하는 부분
    	    		pipeLine = jedis.pipelined();
    				if(clientRefreshCnt >= CommonImageInfo.maxClientRefreshCnt) {
    					// 최대 refreshCnt보다 클 경우, clientKey와 ipAddressKey의 isA(isAbuser)필드를 'true'로 변경하는 부분
    					pipeLine.hset(clientKey, CommonImageInfo.isAbuserField, CommonImageInfo.abuserValue);
    					pipeLine.hset(ipAddressKey, CommonImageInfo.isAbuserField, CommonImageInfo.abuserValue);
    	    		}else {
    	    			// 최대 refreshCnt보다 작은 경우, clientKey의 refreshCnt필드에 '1' 증가된 refresh횟수를 업데이트하는 부분
    	    			pipeLine.hset(clientKey, CommonImageInfo.clientRefreshCntField, String.valueOf(clientRefreshCnt));
    	    		}
    				pipeLine.sync();
    	    		break;
    	    	// FAIL : clientKey의 failCnt를 증가시키며, 최대failCnt와 비교하여 어뷰저유무 판단
    	    	case CommonImageInfo.failAction:
    	    		// 파라미터로 전달받은 clientKey의  failCnt를 '1' 증가시킨 값을 저장하는 부분 
    	    		clientFailCnt = Integer.parseInt(jedis.hget(clientKey, CommonImageInfo.clientFailCntField)) + 1;
    	    		// 최대 failCnt와 비교하여 어뷰저 유무를 판단하는 부분
    	    		pipeLine = jedis.pipelined();
    				if(clientFailCnt >= CommonImageInfo.maxClientFailCnt) {
    					// 최대 refreshCnt보다 클 경우, clientKey와 ipAddressKey의 isA(isAbuser)필드를 'true'로 변경하는 부분
    					pipeLine.hset(clientKey, CommonImageInfo.isAbuserField, CommonImageInfo.abuserValue);
    					pipeLine.hset(ipAddressKey, CommonImageInfo.isAbuserField, CommonImageInfo.abuserValue);
    	    		}else {
    	    			// 최대 failCnt보다 작은 경우, clientKey의 failCnt필드에 '1' 증가된 fail횟수를 업데이트하는 부분
    	    			pipeLine.hset(clientKey, CommonImageInfo.clientFailCntField, String.valueOf(clientFailCnt));
    	    		}
    				pipeLine.sync();
    	    		break;
    	    	// SUCCESS : clientKey에 저장된 isA(isAbuser)값을 보고 어뷰저일 경우 ipAddressKey에 TTL을 적용하고 ClientKey는 삭제하고
    	    	//           정상적인 사용자일 경우, ipAddressKey와 clientKey를 모두 삭제 
    	    	case CommonImageInfo.successAction:
    	    		// clientKey에 저장된 isAbuser필드값을 저장하는 부분
    	    		boolean isAbuser = jedis.hget(clientKey, CommonImageInfo.isAbuserField).equals(CommonImageInfo.abuserValue) ? true : false;
    	    		pipeLine = jedis.pipelined();
    	    		if(isAbuser) {
    	    			// Client가 어뷰저인 경우, ipAddressKey의 TTL을 재설정한다
    	    			// 동일한 Client가 다시 Captcha문제를 사용하려 할때 해당 TTL 시간동안은 어뷰저로 판단하여 어려운모드의 문제를 제출하게 된다
    	    			pipeLine.expire(ipAddressKey, CommonImageInfo.maxIpAddressKeyTTL);
    	    			// Client가 어뷰저인 경우, ipAddressKey의 isAbuser필드를 'true'로 설정
    	    			pipeLine.hset(ipAddressKey, CommonImageInfo.isAbuserField, CommonImageInfo.abuserValue);
    	    			// Client가 어뷰저인 경우, ipAddressKey에 해당하는 clientKey를 빈값으로 설정
    	    			pipeLine.hset(ipAddressKey, CommonImageInfo.clientKeyField, "");
    	    		}else {
    	    			// Client가 정상적인 사용자인 경우, ipAddressKey를 삭제한다.
    	    			pipeLine.del(ipAddressKey);
    	    		}
    	    		// 어뷰저 유무와 관계없이 ipAddressPool과 clientPool에서 ipAddressKey와 clientKey를 각각 삭제하는 부분
    	    		pipeLine.lrem(CommonImageInfo.ipAddressPoolKey, 0, ipAddressKey);
    	    		pipeLine.lrem(CommonImageInfo.clientPoolKey, 0, clientKey);
    	    		// clientKey는 한번 사용이 끝나면 다시 사용되거나 굳이 TTL을 설정할 이유가 없으므로 바로 삭제한다
    	    		pipeLine.del(clientKey);
    	    		pipeLine.sync();
    	    		break;
        	}
    	}catch(Exception e) {
    		log.error("[setClientCountInfo-backgroundTask] UserMessage  : 사용자의 Action에 따른 Client의 Count정보를 수정하는 부분에서 에러발생");
			log.error("[setClientCountInfo-backgroundTask] SystemMessage: {}", e.getMessage());
			log.error("[setClientCountInfo-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
    	}
    }
    
    /*
     * Func : 에러발생 또는 급격한 사용자의 증가로 CaptchaImagePool이 아예 소진되었거나, Captcha Server가 처음 실행되었을때 Batch를 조건을 확인하지 않고 바로 Remote Build 시킨다.
     *        PanoramaAPI를 호출하는 Batch만 실행시키는 이유는 해당 Batch 실행후 Jenkins의 "빌드 후 조치" 기능을 통해 CaptchaImagePool을 만드는 Batch가 자동으로 Build 되기 때문이다.
     */
    public void setRemoteBuildBatchJobDirectly() throws Exception{
    	setRemoteBuildBatchJob(setPanoramaImagePoolAPI);
    }
    
    /*
     * Func : Jenkins의 RemoteBuild API를 호출하는 함수
     */
    public void setRemoteBuildBatchJob(String urlString) throws Exception{
		String authenticatedTokenUrl = "";			// var : 사용자인증 토큰을 암호화한 값을 저장하는 변수
		URL url = null;								// var : remoteBuild API의 url을 저장하는 변수
		HttpURLConnection con = null;				// var : API에 request를 보내기위한 변수
		int responseCode = 0;						// var : 응답코드를 저장하는 변수
		try {
			url = new URL(urlString);
			authenticatedTokenUrl = Base64.getEncoder().encodeToString(authenticatedToken.getBytes());
			
			// RemoteBuild API를 호출하는 부분
			con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod(CommonImageInfo.requestPostMethod);
			con.setRequestProperty(CommonImageInfo.authorizationProperty, "Basic " + authenticatedTokenUrl);
			
			responseCode = con.getResponseCode();
			if(responseCode == 201 || responseCode == 200) {
				log.info("[" + urlString +"] API Call Success");
			}else {
				log.warn("[" + urlString +"] API Call Fail");
			}
		}catch(Exception e) {
			log.error("[setRemoteBuildBatchJob-backgroundTask] UserMessage  : RemoteBuild 실행 API호출시  에러발생");
			log.error("[setRemoteBuildBatchJob-backgroundTask] SystemMessage: {}", e.getMessage());
			log.error("[setRemoteBuildBatchJob-backgroundTask] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
		}
	}
    
    
    // Test Code
    public void printImageInfo(String captchaImageKey, String clientKey) throws Exception{
    	log.info("=============================================================");
    	log.info(" < CaptchaImage Info >");
    	log.info("{" + captchaImageKey + ", issuedCnt, " + jedis.hget(captchaImageKey, CommonImageInfo.captchaImageIssuedCntField) + "}");
    	log.info("{" + captchaImageKey + ", score, " + jedis.hget(captchaImageKey, CommonImageInfo.captchaImageScoreField) + "}");
    	log.info("{" + captchaImageKey + ", userList, " + String.join(", ", jedis.lrange(CommonImageInfo.captchaImageClientPoolKeyHeader + captchaImageKey, 0, jedis.llen(CommonImageInfo.captchaImageClientPoolKeyHeader + captchaImageKey))) + "}");
    	log.info("{" + captchaImageKey + ", history, " + String.join(", ", jedis.lrange(CommonImageInfo.captchaImageHistoryPoolKeyHeader + captchaImageKey, 0, jedis.llen(CommonImageInfo.captchaImageHistoryPoolKeyHeader + captchaImageKey))) + "}");
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
