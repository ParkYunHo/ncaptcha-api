package com.navercorp.ncaptcha.domain;

public class ExamImageInfoVO {
	private String imageKey;		// var : 보기 Panorama Image의 기본키를 저장하기 위한 변수 
	private String typeName;		// var : 보기 Panorama Image의 타입(school, bridge..)을 저장하기 위한 변수
	private String b64;				// var : 보기 Panorama Image의 Base64를 저장하기 위한 변수
	
	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	public String getImageKey() {
		return imageKey;
	}
	public void setImageKey(String imageKey) {
		this.imageKey = imageKey;
	}
	public String getB64() {
		return b64;
	}
	public void setB64(String b64) {
		this.b64 = b64;
	}
	
}
