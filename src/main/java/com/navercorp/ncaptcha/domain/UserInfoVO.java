package com.navercorp.ncaptcha.domain;

public class UserInfoVO {
	private ImageInfoVO imageInfo;		// var : CaptchaImage의 정보를 객체형식으로 저장하는 변수
	private String result;				// var : {SUCCESS, FAIL} 두가지 캡차 서버 호출결과를 리턴하기 위한 값을 저장하는 변수
	private String clientKey;			// var : 고유한 client를 식별하기 위한 키를 저장하는 변수
	
	public String getClientKey() {
		return clientKey;
	}
	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}
	public ImageInfoVO getImageInfo() {
		return imageInfo;
	}
	public void setImageInfo(ImageInfoVO imageInfo) {
		this.imageInfo = imageInfo;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
}
	
