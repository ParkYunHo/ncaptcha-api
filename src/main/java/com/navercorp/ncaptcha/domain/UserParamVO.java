package com.navercorp.ncaptcha.domain;

public class UserParamVO {
	private String clientKey;			// var : 고유한 client를 식별하기 위한 키를 저장하는 변수
	private String actionType;			// var : {"ISSUED", "REFRESH", "FAIL", "SUCCESS"} 네가지 액션을 구분하기 위한 변수
	private AxisVO userInputAxis;		// var : 사용자가 캡차문제에서 정답이라고 생각하여 선택한 이미지의 좌표위치를 저장하는 변수
	
	public AxisVO getUserInputAxis() {
		return userInputAxis;
	}
	public void setUserInputAxis(AxisVO userInputAxis) {
		this.userInputAxis = userInputAxis;
	}
	public String getClientKey() {
		return clientKey;
	}
	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}
	public String getActionType() {
		return actionType;
	}
	public void setActionType(String actionType) {
		this.actionType = actionType;
	}
}
