package com.navercorp.ncaptcha.domain;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

public class CommonImageInfo {
	// project environment
	public static final String projectEnvironment = "dev";								// var : Captcha Server가 실행되는 환경을 저장하는 변수 (dev, stage, real)
	public static boolean isAvailableRedis = true;										// var : Redis Server가 현재 사용가능한지 유무를 저장하는 변수
	public static boolean isAvailableRemoteBuildBatchJob = true;						// var : BatchJob을 RemoteBuild 할 수 있는 상태인지 유무를 저장하는 변수
																						//       이미 RemoteBuild가 실행되고 있는 도중에 중복해서 RemoteBuild가 실행되지 않도록 해당 변수 선언
	// Image Info
	private static final String typeArray[] = {											// var : 이미지의 타입을 배열형식으로 저장한 변수 (초기화의 편의성을 위해 배열선언)
				"school", "bridge", "crossroad", "apartment", "gas-station", 
				"convenience-store", "fire-station", "subway-station", "daiso"};
	public static final List<String> typeList = Arrays.asList(typeArray);				// var : 배열타입으로 선언된 이미지 타입들을 리스트형식으로 저장하는 변수 (사용시의 편의를 위해 리스트 생성) 
	private static final String typeResizedArray[] = {									// var : 이미지 타입명의 앞 두자리만 사용한 타입을 배열형식으로 저장한 변수
				"sc", "br", "cr", "ap", "ga", "co", "fi", "su", "da"};					//       Redis에 저장할 데이터의 양을 줄이기 위해 불필요하게 타입의 전체명을 저장하지 않으려고 선언한 변수
	public static final List<String> typeResizedList = Arrays.asList(typeResizedArray); // var : Resized된 이미지타입 배열을 리스트형식으로 저장하는 변수 (사용시의 편의를 위해 리스트 생성)
	private static final String usageTypeArray[] = {"answer", "exam"};					// var : 이미지의 용도를 배열형식으로 저장한 변수 (처음 CaptchaServer가 시작될때 MySQL내 용도에 관한 데이터를 저장하는 image_usage_type Table의 값을 초기화하기 위해 저장)
	public static final List<String> usageTypeList = Arrays.asList(usageTypeArray);		// var : 이미지 용도 배열을 리스트 형식으로 저장한 변수
	public static final int width = 300;												// var : 캡차이미지의 전체 가로길이를 저장하는 변수
	public static final int height = 300;												// var : 캡차이미지의 전체 세로길이를 저장하는 변수
	public static final int imageCellSize = 100;										// var : 캡차이미지를 구성하는 각각 9개의 정사각형 한 변의 길이를 저장하는 변수 
	public static final int descriptHeight = 30;										// var : 캡차이미지 상단의 문제텍스트이미지의 세로길이를 저장하는 변수 (정답유효성체크 또는 js에서 Marker의 위치를 설정할때 사용)
	
	// List Header
	public static final String captchaImagePoolKeyHeader = "cil_";						// var : 일반모드의 CaptchaImage들을 저장하고 있는 리스트의 헤더부분을 저장하는 변수
	public static final String captchaImageHardModePoolKeyHeader = "cil_h_";			// var : 어려운모드의 CaptchaImage들을 저장하고 있는 리스트의 헤더부분을 저장하는 변수
	public static final String captchaImageHardModePoolKeySeparator = "_h_";			// var : 일반모드와 어려운모드의 헤더를 구분하는 구분자를 저장하는 변수
	public static final String captchaImageHistoryPoolKeyHeader = "his_";				// var : CaptchaImage에 대해 사용자의 Action(SUCCESS, FAIL, REFRESH) 히스토리들을 저장하는 리스트의 헤더부분을 저장하는 변수
	public static final String captchaImageClientPoolKeyHeader = "cl_";					// var : CaptchaImage에 관여한 사용자의 ClientKey들을 저장하는 리스트의 헤더부분을 저장하는 변수 
	public static final String examPanoramaImagePoolKeyHeader = "e_";					// var : 보기 PanoramaImage들을 저장하고 있는 리스트의 헤더부분을 저장하는 변수
	public static final String answerPanoramaImagePoolKeyHeader = "a_";					// var : 정답 PanoramaImage들을 저장하고 있는 리스트의 헤더부분을 저장하는 변수
	public static final String userAgentHeader = "User-Agent";							// var : Request패킷헤더 부분의 User-Agent 헤더부분을 저장하는 변수
	
	// List Key
	public static final String ipAddressPoolKey = "ipl";								// var : Client들의 IP주소들을 저장하고 있는 리스트의 키값을 저장하는 변수
	public static final String clientPoolKey = "ckl";									// var : Client들의 고유한 키값들을 저장하고 있는 리스트의 키값을 저장하는 변수
	public static final String deletedCaptchaImagePoolKey = "dl";						// var : IssuedCnt에 의해 삭제된 CaptchaImage들의 키값을 저장하고 있는 리스트 키값을 저장하는 변수
	
	// Field
	public static final String base64Field = "b64";										// var : Redis의 Hash형식의 CaptchaImage데이터에서 Base64필드명을 저장하는 변수
	public static final String isAbuserField = "isA";									// var : Redis의 Hash형식의 Client와 ipAddress 데이터에서 어뷰저 유무를 저장하는 데이터의 isAbuser필드명을 저장하는 변수
	public static final String startXAxisField = "sx";									// var : Redis의 Hash형식의 Client 데이터에서 시작좌표의 X축 필드명을 저장하는 변수 
	public static final String startYAxisField = "sy";									// var : Redis의 Hash형식의 Client 데이터에서 시작좌표의 Y축 필드명을 저장하는 변수
	public static final String answerXAxisField = "ax";									// var : Redis의 Hash형식의 CaptchaImage 데이터에서 정답좌표의 X축 필드명을 저장하는 변수
	public static final String answerYAxisField = "ay";									// var : Redis의 Hash형식의  CaptchaImage 데이터에서 정답좌표의 Y축 필드명을 저장하는 변수
	public static final String clientUsingCaptchaImageField = "ci";						// var : Redis의 Hash형식의 Client 데이터에서 현재 Client가 사용중인 CaptchaImage값을 저장하고 있는 필드명을 저장하는 변수
	public static final String clientKeyField = "ck";									// var : Redis의 Hash형식의 ipAddress 데이터에서 해당 ip주소가 사용하고 있는 ClientKey값을 저장하고 있는 필드명을 저장하는 변수
	public static final String userAgentField = "ua";									// var : Redis의 Hash형식의 ipAddress 데이터에서 request패킷의 userAgent값을 저장하고 있는 필드명을 저장하는 변수
	public static final String ipAddressField = "ip";									// var : Redis의 Hash형식의 Client 데이터에서 해당 ClientKey가 사용하고 있는 ipAddress값을 저장하고 있는 필드명을 저장하는 변수
	public static final String clientRefreshCntField = "rc";							// var : Redis의 Hash형식의  Client 데이터에서 새로고침 횟수 값을 저장하고 있는 필드명을 저장하는 변수
	public static final String clientFailCntField = "fc";								// var : Redis의 Hash형식의  Client 데이터에서 캡차문제를 틀린 횟수 값을 저장하고 있는 필드명을 저장하는 변수
	public static final String captchaImageClientPoolField = "cl";						// var : Redis의 Hash형식의  CaptchaImage에서 CaptchaImage에 관여한 사용자의 ClientKey들을 저장하는 리스트의 키값을 저장하고 있는 필드명을 저장하는 변수
	public static final String captchaImageHistoryPoolField = "his";					// var : Redis의 Hash형식의  CaptchaImage에서  CaptchaImage에 대해 사용자의 Action(SUCCESS, FAIL, REFRESH) 히스토리들을 저장하는 리스트의 키값을 저장하고 있는 필드명을 저장하는 변수
	public static final String captchaImageScoreField = "score";						// var : Redis의 Hash형식의 CaptchaImage에서 점수(Score)값을 저장하고 있는 필드명을 저장하는 변수  
	public static final String captchaImageIssuedCntField = "ic";						// var : Redis의 Hash형식의 CaptchaImage에서 발행횟수를 저장하고 있는 필드명을 저장하는 변수
		
	// Action
	public static final String failAction = "FAIL";										// var : 캡차문제를 틀렸을때의 액션명을 저장하는 변수 
	public static final String successAction = "SUCCESS";								// var : 캡차문제를 맞췄을때의 액션명을 저장하는 변수
	public static final String issuedAction = "ISSUED";									// var : 캡차문제를 발행하는 액션명을 저장하는 변수
	public static final String refreshAction = "REFRESH";								// var : 사용자가 캡차문제를 새로고침했을때의 액션명을 저장하는 변수
	
	// OS
	public static final String windowsOS = "windows";									// var : Request패킷의 User-Agent에서 OS가 windows일때의 문자열을 저장하는 변수
	public static final String macOS = "mac";											// var : Request패킷의 User-Agent에서 OS가 Mac일때의 문자열을 저장하는 변수
	public static final String unixUserAgent = "x11";									// var : Request패킷의 User-Agent에서 OS가 Unix일때의 User-Agent내의 구분되는 문자열을 저장하는 변수
	public static final String unixOS = "unix";											// var : Request패킷의 User-Agent에서 OS가 Unix일때의 문자열을 저장하는 변수
	public static final String androidOS = "android";									// var : Request패킷의 User-Agent에서 OS가 Android일때의 문자열을 저장하는 변수
	public static final String iphoneOS = "iphone";										// var : Request패킷의 User-Agent에서 OS가 Iphone일때의 문자열을 저장하는 변수
	public static final String unKnown = "unKnown";										// var : Request패킷의 User-Agent에서 OS 또는 브라우저를 확인할 수 없을때의 문자열을 저장하는 변수
	
	// Browser
	public static final String safariBrowser = "safari";								// var : Request패킷의 User-Agent에서 Browser가 사파리일때의 문자열을 저장하는 변수 
	public static final String broswerVersion = "version";								// var : Request패킷의 User-Agent에서 Browser가 사파리일때를 구분하기 위한 문자열을 저장하는 변수 (Safari 브라우저일때에는 "version"이라는 값이 User-Agent 내에 포함된다)
	public static final String operaBrowserUnder15Version = "opera";					// var : Request패킷의 User-Agent에서 Browser가 15버전 이하의 오페라일때의 문자열을 저장하는 변수 
	public static final String operaBrowserUp15Version = "opr";							// var : Request패킷의 User-Agent에서 Browser가 15버전 이후의 오페라일때의 문자열을 저장하는 변수
	public static final String whaleBrowser = "whale";									// var : Request패킷의 User-Agent에서 Browser가 네이버 웨일일때의 문자열을 저장하는 변수
	public static final String edgeBrowser = "edge";									// var : Request패킷의 User-Agent에서 Browser가 IE Edge일때의 문자열을 저장하는 변수
	public static final String chromeBrowser = "chrome";								// var : Request패킷의 User-Agent에서 Browser가 크롬일때의 문자열을 저장하는 변수
	public static final String firefoxBrowser = "firefox";								// var : Request패킷의 User-Agent에서 Browser가 파이어폭스일때의 문자열을 저장하는 변수
	public static final String IEUserAgent = "rv";										// var : Request패킷의 User-Agent에서 Browser가 IE일때를 구분하기 위한 문자열을 저장하는 변수 (IE일때 User-Agent에서 "rv"라는 값이 포함된다)	
	public static final String IEBrowser = "IE";										// var : Request패킷의 User-Agent에서 Browser가 IE일때의 문자열을 저장하는 변수
	
	// TTL
	public static final int deletedCaptchaImageKeyTTL = 300;							// var : CaptchaImage를 삭제해야될때 CaptchaImage에 관련된 키의 TTL 시간값을 저장하는 변수
	public static final int maxClientKeyTTL = 3600;										// var : clientKey를 생성할때 Redis내에 계속 해당 데이터가 남아있지 않게하기 위해 TTL 시간값을 저장하는 변수
	public static final int maxIpAddressKeyTTL = 3600;									// var : ipAddressKey를 생성할때 Redis내에 계속 해당 데이터가 남아있지 않게하기 위해 TTL 시간값을 저장하는 변수
		
	// CaptchaImage Score
	public static final String refreshScore = "+1";										// var : 사용자가 캡차문제롤 새로고침했을때의 CaptchaImage에 적용되는 스코어값을 저장하는 변수
	public static final String failScore = "+2";										// var : 캡차문제를 틀렸을때의 CaptchaImage에 적용되는 스코어값을 저장하는 변수
	public static final String successScore = "-2";										// var : 캡차문제를 맞췄을때의 CaptchaImage에 적용되는 스코어값을 저장하는 변수
	
	// Count Info
	public static final int maxCaptchaImageIssuedCnt = 10;								// var : CaptchaImage의 최대발행횟수를 저장하는 변수 (해당 횟수를 넘게되면 해당 캡차이미지를 CaptchaImagePool에서 삭제시키고, DeletedList로 이동시킨다. 이동된 CaptchaImage는 Batch실행시 삭제된다)
	public static final int maxClientRefreshCnt = 5;									// var : 사용자의 최대 새로고침 횟수를 저장하는 변수 (해당 횟수를 넘게되면 어뷰저로 판단하여 ClientKey와 ipAddressKey에 isAbuser필드를 'true'로 변경한다)
	public static final int maxClientFailCnt = 5;										// var : 사용자의 최대 실패 횟수를 저장하는 변수 (해당 횟수를 넘게되면 어뷰저로 판단하여 ClientKey와 ipAddressKey에 isAbuser필드를 'true'로 변경한다)
	public static final int maxClientInvolvedInCaptchaImageCnt = 5;						// var : 하나의 CaptchaImage에 한명의 사용자가 최대 관여할 수 있는 횟수를 저장하는 변수 (해당 횟수를 넘게되면 사용자의 액션에 대한 스코어가 CaptchaImage에 반영되지 않는다)
	
	// PoolSize Info
	public static final int maxCaptchaImageHistoryPoolSize = 50;						// var : CaptchaImage의 사용자에 대한 액션을 모두 히스토리로 남길때, 이 히스토리의 최대크기를 저장하는 변수 (해당 최대크기를 넘어서서 액션이 들어왔을때는 FIFO형식으로 가장 오래된 액션을 삭제하고 새로운 액션을 추가하며 스코어를 재계산한다)
	public static final int minCaptchaImagePoolSize = 4;								// var : CaptchaImagePool이 해당 개수 이하로 소진될경우 CaptchaImagePool을 만드는 Batch를 실행하기 위한 변수
	public static final int minCaptchaImageHardModePoolSize = 3;						// var : 어려운모드의 CaptchaImagePool을 사용하기 위한 최소개수를 저장하는 변수 (어려운 모드의 CaptchaImagePool이 해당 개수 이하일 경우에는 일반모드의 CaptchaImagePool을 사용한다)
	public static final int maxAvailableCaptchaImagePoolSize = 7;						// var : RemoteBuild를 통해 CaptchaImagePool이 어느정도 채워질때까지 중복RemoteBuild를 막기위한 변수
	public static final int minNecessaryCalculateScoreCaptchaImagePoolSize = 4;			// var : CaptchaImagePool 해당 개수만큼 소진되었을때 특정 시간마다 score계산되는 Task를 기다리지 말고 바로 Score가 계산될수 있도록하는 변수
	public static final int maxExamPanoramaImagePoolSize = 8;							// var : 보기 PanoramaImagePool의 최대 크기를 저장하는 변수
	public static final int maxClientKeyLength = 10;									// var : ClientKey를 새롭게 생성할때 Random문자열로 결정하는데 이때 Random문자열의 길이를 저장하는 변수
	
	// Result Message
	public static final String serverErrorResultMessage = "Internal Server Error";		// var : CaptchaServer 내부에서 에러발생시 Client에게 리턴하는 메세지를 저장하는 변수
	public static final String clientKeyInvalidResultMessage = "Client Key Invalid";	// var : Client가 캡차이미지 요청시 또는 정답유효성체크 요청시 파라미터로 ClientKey를 전달하지 않았을 경우 Client에게 리턴하는 메세지를 저장하는 변수
	
	// RemoteBuild Info
	public static final String requestPostMethod = "POST";								// var : RemoteBuild 호출을 위한 API 호출시 Request를 위한 메서드 텍스트를 저장하는 변수
	public static final String authorizationProperty = "Authorization";					// var : RemoteBuild 호출을 위한 API 호출시 Request패킷 헤더의 인증 관련된 프로퍼티를 추가하기 위한 키값 텍스트를 저장하는 변수
	
	// ETC
	public static final int maxIssuedClientKeyRetryCnt = 3;								// var : ClientKey를 생성시 최대 재시도 횟수 (Random문자열 생성시 이미 해당 Random문자열을 사용하고 있는 ClientKey가 없는지 확인)
	public static final String abuserValue = "true";									// var : ClientKey 또는 ipAddressKey에서 "isA(isAbuser)"필드의 값을 저장할때 어뷰저일때의 값을 저장하는 변수
	public static final String notAbuserValue = "false";								// var : ClientKey 또는 ipAddressKey에서 "isA(isAbuser)"필드의 값을 저장할때 정상적인 사용자일때의 값을 저장하는 변수
	public static final String emptyValue = "0";										// var : ClientKey의 RefreshCnt, FailCnt 등을 초기화할때 사용하기 위해 "0"을 문자열 형식으로 저장하는 변수
	public static final String captchaImageNormalModeKey = "normal";					// var : 정상적인 사용자를 위한  CaptchaImagePool을 저장하기 위한 HashMap의 키를 저장하는 변수 
	public static final String captchaImageHardModeKey = "hard";						// var : 어뷰저를 위한 CaptchaImagePool을 저장하기 위한 HashMap의 키를 저장하는 변수
}
