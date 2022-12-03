package com.navercorp.ncaptcha.controller;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.navercorp.ncaptcha.domain.CommonImageInfo;
import com.navercorp.ncaptcha.domain.UserInfoVO;
import com.navercorp.ncaptcha.domain.UserParamVO;
import com.navercorp.ncaptcha.service.ImageHandler;
import com.navercorp.ncaptcha.service.ValidationHandler;

@RestController
public class ApiCallController {
	@Autowired
	private ImageHandler imageHandler;
	@Autowired
	private ValidationHandler validationHandler;

	private static final Logger log = LoggerFactory.getLogger(ApiCallController.class);

	/*
	 * Func(GET) : Client에게 사용자인증키(ClientKey)를 리턴하는 메서드로 Client의 IP주소와 User-Agent정보를 파라미터로 넘긴다
	 */
	@RequestMapping(value="/getClientKey")
	private UserInfoVO getClientKey(HttpServletRequest request, HttpServletResponse response) throws Exception{
		UserInfoVO result = null;				// var : Client에게 키값과 Result를 리턴하기 위한 객체 변수
		String ipAddress = "";					// var : Request들어온 패킷의 ip주소를 저장하는 변수
		String userAgent = "";					// var : Request들어온 패킷의 User-Agent정보를 저장하는 변수
		try {
			result = new UserInfoVO();
			ipAddress = request.getRemoteAddr();
			userAgent = request.getHeader(CommonImageInfo.userAgentHeader);

			// ClientKey를 발급해주는 Service를 호출하고 이에 ClientKey를 결과값으로 리턴받는다. 
			// 이때 정상적으로 ClientKey가 생성되면 ClientKey값이 리턴되지만 에러발생시 빈값("")이 리턴되며 이를 토대로 Result Message를 결정한다
			result.setClientKey(validationHandler.getIssuedClientKeyValidation(ipAddress, userAgent));
			if(result.getClientKey().equals("")) {
				result.setResult(CommonImageInfo.serverErrorResultMessage);
			}else {
				result.setResult(CommonImageInfo.successAction);
			}
		}catch(Exception e) {
			result = new UserInfoVO();
			result.setResult(CommonImageInfo.serverErrorResultMessage);
			
			log.error("[getClientKey] UserMessage  : Client키를 발행하는 Controller에서 Client키와 Result를 UserInfoVO() 객체에 저장하는 도중 에러발생");
			log.error("[getClientKey] SystemMessage: {}", e.getMessage());
			log.error("[getClientKey] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
		}
		return result;
	}
	
	/*
	 * Func(POST) : Client에게 CaptchaImage를 리턴하는 메서드로 ClinetKey를 파라미터로 받아서 사용자를 인증한다
	 */
	@RequestMapping(value="/getCaptchaImage", method=RequestMethod.POST)
	private UserInfoVO getCaptchaImage(@RequestBody UserParamVO param, HttpServletResponse response) throws Exception{
		response.setContentType("application/json;charset=UTF-8");
		UserInfoVO result = null;			// var : Client에게 CaptchaImage정보와 Result를 리턴하기 위한 객체 변수
		try {
			result = new UserInfoVO();
			// 사용자가 파라미터로 전달한 ClientKey의 존재유무를 확인하는 부분
			if(param.getClientKey() != null) {
				// ClientKey가 존재할때, CaptchaImage정보를 리턴하는 Service를 호출한다
				result.setImageInfo(imageHandler.getCaptchaImageInfo(param));
				if(result.getImageInfo() != null) {
					// CaptchaImage정보를 정상적으로 가져왔을때, Success를 리턴한다
					result.setResult(CommonImageInfo.successAction);
				}else {
					// CaptchaImage정보를 정상적으로 가져오지 못했을때, Server error를 리턴한다
					result.setResult(CommonImageInfo.serverErrorResultMessage);
				}
			}else {
				// ClientKey가 존재하지 않을때, ClientKey에러임을 사용자에게 리턴한다
				result.setResult(CommonImageInfo.clientKeyInvalidResultMessage);
			}
		}catch(Exception e) {
			result = new UserInfoVO();
			result.setResult(CommonImageInfo.serverErrorResultMessage);
			
			log.error("[getCaptchaImage] UserMessage  : CaptchaImage를 리턴하는 Controller에서 CaptchaImage정보와 Result를 UserInfoVO() 객체에 저장하는 도중 에러발생");
			log.error("[getCaptchaImage] SystemMessage: {}", e.getMessage());
			log.error("[getCaptchaImage] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
		}
		return result;
	}
	
	/*
	 * Func(POST) : Client의 캡차문제에 대한 입력값을 전달받아 정답 유효성체크를 하는 메서드
	 */
	@RequestMapping(value="/checkValidation", method=RequestMethod.POST)
	private UserInfoVO checkValidation(@RequestBody UserParamVO param, HttpServletResponse response) throws Exception{
		UserInfoVO result = null;			// var : Client에게 Result와 실패시 다시 CaptchaImage정보를 리턴하기 위한 객체변수
		String vaildationResult = "";		// var : 사용자입력값에 대한 정답유효성체크 결과를 저장하는 변수
		try {
			result = new UserInfoVO();
			// 사용자가 파라미터로 전달한 ClientKey의 존재유무를 확인하는 부분
			if(param.getClientKey() != null) {
				// ClientKey가 존재할때, 정답유효성체크 결과를 리턴하는 Service를 호출한다
				vaildationResult = validationHandler.getCheckValidation(param); 
				if(vaildationResult.equals(CommonImageInfo.successAction)) {
					// 정답유효성체크 결과가 성공(Success)일때, Success를 리턴한다
					result.setResult(CommonImageInfo.successAction);
				}else if(vaildationResult.equals(CommonImageInfo.failAction)){
					// 정답유효성체크 결과가 실패 (Fail)일때, 새로운 CaptchaImage정보를 리턴하는 Service를 호출한다 
					param.setActionType(CommonImageInfo.failAction);
					result.setImageInfo(imageHandler.getCaptchaImageInfo(param));
					
					if(result.getImageInfo() != null) {
						// CaptchaImage정보를 정상적으로 가져왔을때, Fail로 사용자의 입력값이 틀렸다는 것을 알려주고 새로운 캡차이미지를 리턴한다.
						result.setResult(CommonImageInfo.failAction);
					}else {
						// CaptchaImage정보를 정상적으로 가져오지 못했을때, Server error를 리턴한다
						result.setResult(CommonImageInfo.serverErrorResultMessage);
					}
				}else {
					result.setResult(CommonImageInfo.serverErrorResultMessage);
				}
			}else {
				// ClientKey가 존재하지 않을때, ClientKey에러임을 사용자에게 리턴한다
				result.setResult(CommonImageInfo.clientKeyInvalidResultMessage);
			}
		}catch(Exception e) {
			result = new UserInfoVO();
			result.setResult(CommonImageInfo.serverErrorResultMessage);
			
			log.error("[checkValidation] UserMessage  : 사용자입력값에 대한 정답유효성체크 Controller에서 정답유효성체크 결과와 Result를 UserInfoVO() 객체에 저장하는 도중 에러발생");
			log.error("[checkValidation] SystemMessage: {}", e.getMessage());
			log.error("[checkValidation] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
		}
		return result;
	}
}
