package com.navercorp.ncaptcha.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.navercorp.ncaptcha.domain.*;

import io.lettuce.core.dynamic.annotation.Param;

public interface ImageMapper {
	// (select) MySQL에서 rowNo 개수만큼 보기 PanoramaImagePool 이미지정보(Base64, type이름, 이미지키)를 가져오는 메서드
	List<ExamImageInfoVO> selectExamImageInfoList(int rowNo) throws Exception;
	// (update) MySQL에서 rowNo 개수만큼 가져온 PanoramaImagePool의 Table내의 IsUsed필드를 '1'로 변경하는 메서드 
	//          Batch Job발생시 MySQL내에서 Redis로 가져와진 이미지(IsUsed=1) row들을 모두 삭제하기 위해 사용
	void updateExamImageInfoList(int rowNo) throws Exception;
	
	// (select) 이미지 타입정보(school, bridge..)를 저장하고 있는 MySQL의 image_type Table에 저장된 데이터의 총 개수를 가져오는 메서드
	// 			Captcha Server가 실행될때 MySQL의 image_type Table이 비어있는지 확인하기 위해 사용 
	// 			(비어있을때에는 Captcha Server에 저장된 타입 값들을 저장)
	int selectImageTypeListCnt() throws Exception;
	// (select) 이미지 용도정보(answer, exam)를 저장하고 있는 MySQL의 image_usage_type Table에 저장된 데이터의 총 개수를 가져오는 메서드
	// 			Captcha Server가 실행될때 MySQL의 image_usage_type Table이 비어있는지 확인하기 위해 사용 
	// 			(비어있을때에는 Captcha Server에 저장된 사용용도 값들을 저장)
	int selectImageUsageTypeListCnt() throws Exception;
	// (insert) Captcha Server에 저장된 이미지 타입정보(school, bridge..)를 MySQL의 image_type Table에 저장하는 메서드
	// 			Captcha Server가 실행될때 image_type Table이 비어있는지 체크하고 비어있을 경우 사용
	void insertImageTypeList(List<String> imageTypeList) throws Exception;
	// (insert) Captcha Server에 저장된 이미지 용도정보(answer, exam)를 MySQL의 image_usage_type Table에 저장하는 메서드
	// 			Captcha Server가 실행될때 image_usage_type Table이 비어있는지 체크하고 비어있을 경우 사용
	void insertImageUsageTypeList(List<String> imageUsageTypeList) throws Exception;
}
