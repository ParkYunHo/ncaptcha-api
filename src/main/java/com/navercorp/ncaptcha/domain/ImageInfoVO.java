package com.navercorp.ncaptcha.domain;

public class ImageInfoVO {
	private String b64;					// var : captchaImage의 Base64정보를 저장하기 위한 변수		 
	private int width;					// var : captchaImage의 가로길이를 저장하기 위한 변수
	private int height;					// var : captchaImage의 세로길이를 저장하기 위한 변수
	private int cellSize;				// var : captchaImage를 구성하는 9개(정답이미지 1개 + 보기이미지 8개) 정사각형 이미지의 한 변의 길이를 저장하기 위한 변수 
	private int descriptHeight;			// var : captchaImage에 포함되어 있는 상단의 문제텍스트 이미지 세로길이를 저장하기 위한 변수
	private AxisVO startAxis;			// var : javascript의 Marker 시작위치를 알려주기 위해 시작좌표를 저장하기 위한 변수
	
	public int getCellSize() {
		return cellSize;
	}
	public void setCellSize(int cellSize) {
		this.cellSize = cellSize;
	}
	public int getDescriptHeight() {
		return descriptHeight;
	}
	public void setDescriptHeight(int descriptHeight) {
		this.descriptHeight = descriptHeight;
	}
	public AxisVO getStartAxis() {
		return startAxis;
	}
	public void setStartAxis(AxisVO startAxis) {
		this.startAxis = startAxis;
	}
	public String getB64() {
		return b64;
	}
	public void setB64(String b64) {
		this.b64 = b64;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
}
