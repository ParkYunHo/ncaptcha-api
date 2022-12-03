package com.navercorp.ncaptcha;

import java.util.Arrays;
import java.util.List;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.navercorp.ncaptcha.domain.CommonImageInfo;
import com.navercorp.ncaptcha.mapper.ImageMapper;
import com.navercorp.ncaptcha.scheduled.CaptchaImageScoreTask;

import redis.clients.jedis.Jedis;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.navercorp.ncaptcha.mapper")
public class NcaptchaApplication {
	@Autowired
	ImageMapper imageMapper;
	@Autowired
	CaptchaImageScoreTask captchaImageTask;
	
	private static final Logger log = LoggerFactory.getLogger(NcaptchaApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(NcaptchaApplication.class, args);
	}
	
	/*
	 * Func : Captcha Server가 시작될때, MySQL의 타입정보를 가지고 있는 image_type Table과 용도정보를 가지고 있는 image_usage_type Table의 값들을 초기화하고,
	 *        PanoramaImagePool을 만들고 '빌드후 조치'를 통해 CaptchaImagePool까지 만들도록 RemoteBuild를 바로 시키는 함수
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void initBackgroundScheduler() throws Exception{
		int imageTypeCnt = 0;
		int imageUsageTypeCnt = 0;
		try {
			// image_type Table(school, bridge..)이 비어있는지 확인하기 위해 모든 row들의 개수를 MySQL에서 select하여 변수에 저장하는 부분
			imageTypeCnt = imageMapper.selectImageTypeListCnt();
			// image_usage_type Table(answer,exam)이 비어있는지 확인하기 위해 모든 row들의 개수를 MySQL에서 select하여 변수에 저장하는 부분 
			imageUsageTypeCnt = imageMapper.selectImageUsageTypeListCnt();
			
			// image_type Table이 비어있는 경우 CaptchaServer에 저장된 타입리스트를 해당 테이블에 저장하는 부분 
			if(imageTypeCnt <= 0) {
				imageMapper.insertImageTypeList(CommonImageInfo.typeList);
				log.info("Image type init");
			}
			// image_usage_type Table이 비어있는 경우 CaptchaServer에 저장된 용도리스트를 해당 테이블에 저장하는 부분
			if(imageUsageTypeCnt <= 0) {
				imageMapper.insertImageUsageTypeList(CommonImageInfo.usageTypeList);
				log.info("Image usage type init");
			}
			
			// Captcha Server가 시작될때 PanoramaImagePool을 만드는 Batch Job도 바로 실행시키는 부분
//			captchaImageTask.setRemoteBuildBatchJobDirectly();
		}catch(Exception e) {
			log.error("[initBackgroundScheduler] UserMessage  : MySQL image_type, image_usage_type Table에 각각의 정보를 초기화하는 부분에서 에러발생");
			log.error("[initBackgroundScheduler] SystemMessage: {}", e.getMessage());
			log.error("[initBackgroundScheduler] StackTrace   :\n" + Arrays.asList(e.getStackTrace()).toString().replace(",", "\n"));
		}
	}
}

