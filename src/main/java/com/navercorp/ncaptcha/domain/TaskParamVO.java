package com.navercorp.ncaptcha.domain;

import java.util.List;

public class TaskParamVO {
	private boolean isEmptyCaptchaImagePool;	// var : CaptchaImagePool이 사용하기에 충분한 사이즈를 가지고있는지 유무를 저장하는 변수 
	private String issuedCaptchaImageKey;		// var : 새로 발급된 캡차이미지의 키값을 저장하는 변수
	private String previousCaptchaImageKey;		// var : 기존 사용자에게 발급되었던 캡차이미지 키값을 저장하는 변수
	private String captchaImagePoolKey;			// var : Redis내에 리스트로 저장되어 있는 captchaImagePool 자체의 키값을 저장하는 변수 
	private String clientKey;					// var : 고유한 client를 식별하기 위한 키를 저장하는 변수
	private String ipAddressKey; 				// var : CaptchaImage의 History기능을 위해 사용자의 IP주소 키값을 저장하는 변수
	
	public boolean isEmptyCaptchaImagePool() {
		return isEmptyCaptchaImagePool;
	}
	public void setEmptyCaptchaImagePool(boolean isEmptyCaptchaImagePool) {
		this.isEmptyCaptchaImagePool = isEmptyCaptchaImagePool;
	}
	public String getCaptchaImagePoolKey() {
		return captchaImagePoolKey;
	}
	public void setCaptchaImagePoolKey(String captchaImagePoolKey) {
		this.captchaImagePoolKey = captchaImagePoolKey;
	}
	public String getClientKey() {
		return clientKey;
	}
	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}
	public String getIssuedCaptchaImageKey() {
		return issuedCaptchaImageKey;
	}
	public void setIssuedCaptchaImageKey(String issuedCaptchaImageKey) {
		this.issuedCaptchaImageKey = issuedCaptchaImageKey;
	}
	public String getPreviousCaptchaImageKey() {
		return previousCaptchaImageKey;
	}
	public void setPreviousCaptchaImageKey(String previousCaptchaImageKey) {
		this.previousCaptchaImageKey = previousCaptchaImageKey;
	}
	public String getIpAddressKey() {
		return ipAddressKey;
	}
	public void setIpAddressKey(String ipAddressKey) {
		this.ipAddressKey = ipAddressKey;
	}
}
