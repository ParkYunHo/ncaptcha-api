package com.navercorp.ncaptcha.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ClientController {
	/*
	 * Func : Client에게 보여지는 화면을 테스트하기 위한 jsp를 호출하는 메서드 
	 *        scopeCaptchaClient.jsp를 호출시 javascript를 통해 동적으로 HTML객체들이 생성된다. 
	 */
	@RequestMapping("/scopeCaptcha")
	private String scopeCaptchaClient() throws Exception{
		return "scopeCaptchaClient";
	}
}
