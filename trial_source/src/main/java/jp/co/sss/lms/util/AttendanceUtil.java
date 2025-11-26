package jp.co.sss.lms.util;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.mapper.MSectionMapper;

/**
 * 勤怠管理のユーティリティクラス
 * 
 * @author 東京ITスクール
 */
@Component
public class AttendanceUtil {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private MSectionMapper mSectionMapper;
	@Autowired //Task.26 小松原 2025/11/19
	TrainingTime trainingTime;

	/**
	 * SSS定時・出退勤時間を元に、遅刻早退を判定をする
	 * 
	 * @param trainingStartTime 開始時刻
	 * @param trainingEndTime   終了時刻
	 * @return 遅刻早退を判定メソッド
	 */
	public AttendanceStatusEnum getStatus(TrainingTime trainingStartTime,
			TrainingTime trainingEndTime) {
		return getStatus(trainingStartTime, trainingEndTime, Constants.SSS_WORK_START_TIME,
				Constants.SSS_WORK_END_TIME);
	}

	/**
	 * 与えられた定時・出退勤時間を元に、遅刻早退を判定する
	 * 
	 * @param trainingStartTime 開始時刻
	 * @param trainingEndTime   終了時刻
	 * @param workStartTime     定時開始時刻
	 * @param workEndTime       定時終了時刻
	 * @return 判定結果
	 */
	private AttendanceStatusEnum getStatus(TrainingTime trainingStartTime,
			TrainingTime trainingEndTime, TrainingTime workStartTime, TrainingTime workEndTime) {
		// 定時が不明な場合、NONEを返却する
		if (workStartTime == null || workStartTime.isBlank() || workEndTime == null
				|| workEndTime.isBlank()) {
			return AttendanceStatusEnum.NONE;
		}
		boolean isLate = false, isEarly = false;
		// 定時より1分以上遅く出社していたら遅刻(＝はセーフ)
		if (trainingStartTime != null && trainingStartTime.isNotBlank()) {
			isLate = (trainingStartTime.compareTo(workStartTime) > 0);
		}
		// 定時より1分以上早く退社していたら早退(＝はセーフ)
		if (trainingEndTime != null && trainingEndTime.isNotBlank()) {
			isEarly = (trainingEndTime.compareTo(workEndTime) < 0);
		}
		if (isLate && isEarly) {
			return AttendanceStatusEnum.TARDY_AND_LEAVING_EARLY;
		}
		if (isLate) {
			return AttendanceStatusEnum.TARDY;
		}
		if (isEarly) {
			return AttendanceStatusEnum.LEAVING_EARLY;
		}
		return AttendanceStatusEnum.NONE;
	}

	/**
	 * 中抜け時間を時(hour)と分(minute)に変換
	 *
	 * @param min 中抜け時間
	 * @return 時(hour)と分(minute)に変換したクラス
	 */
	public TrainingTime calcBlankTime(int min) {
		int hour = min / 60;
		int minute = min % 60;
		TrainingTime total = new TrainingTime(hour, minute);
		return total;
	}

	/**
	 * 時刻分を丸めた本日日付を取得
	 * 
	 * @return "yyyy/M/d"形式の日付
	 */
	public Date getTrainingDate() {
		Date trainingDate;
		try {
			trainingDate = dateUtil.parse(dateUtil.toString(new Date()));
		} catch (ParseException e) {
			// DateUtil#toStringとparseは同様のフォーマットを使用しているため、起こりえないエラー
			throw new IllegalStateException();
		}
		return trainingDate;
	}

	/**
	 * 休憩時間取得
	 * 
	 * @return 休憩時間
	 */
	public LinkedHashMap<Integer, String> setBlankTime() {
		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
		map.put(null, "");
		for (int i = 15; i < 480;) {
			int hour = i / 60;
			int minute = i % 60;
			String time;

			if (hour == 0) {
				time = minute + "分";

			} else if (minute == 0) {
				time = hour + "時間";
			} else {
				time = hour + "時" + minute + "分";
			}

			map.put(i, time);

			i = i + 15;

		}
		return map;
	}

	/**
	 * 研修日の判定
	 * 
	 * @param courseId
	 * @param trainingDate
	 * @return 判定結果
	 */
	public boolean isWorkDay(Integer courseId, Date trainingDate) {
		Integer count = mSectionMapper.getSectionCountByCourseId(courseId, trainingDate);
		if (count > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Task.26 時間のマップ
	 * 小松原　2025/11/18
	 */
	public LinkedHashMap<Integer, String> setHourTime() {
		LinkedHashMap<Integer, String> hourTime = new LinkedHashMap<>();
		hourTime.put(null, "");
		for (int i = 0; i < 24; i++) {
			hourTime.put(i, String.format("%02d", i));
		}
		return hourTime;
	}

	/**
	 * Task.26 分のマップ
	 * 小松原　2025/11/18
	 */
	public LinkedHashMap<Integer, String> setMinuteTime() {
		LinkedHashMap<Integer, String> miniteTime = new LinkedHashMap<>();
		miniteTime.put(null, "");
		for (int i = 0; i < 60; i++) {
			miniteTime.put(i, String.format("%02d", i));
		}
		return miniteTime;
	}

	/**
	 * Task.26 時間だけを切り出す
	 * 小松原　2025/11/18
	 */
	public String getHour(String trainingTimeHoure) {
		if (trainingTimeHoure == null || trainingTimeHoure.isEmpty()) {
			return null;
		}
		return trainingTimeHoure.substring(0, 2);
	}

	/**
	 * Task.26 分だけを切り出す
	 * 小松原　2025/11/18
	 */
	public String getMinute(String trainingTimeMinute) {
		//nullや空文字、スペースはnull
		if (trainingTimeMinute == null || trainingTimeMinute.isEmpty()) {
			return null;
		}
		return trainingTimeMinute.substring(trainingTimeMinute.length() - 2);
	}

	/**
	 * 受講時間の算出
	 * @author 小松原　Task.27 2025/11/26
	 */
	public TrainingTime attendingTimeCalculation(TrainingTime trainingStartTime, TrainingTime trainingEndTime) {
		// 就業時間と定時のうち遅い方を、受講開始時間
		TrainingTime startTime = trainingTime.max(trainingStartTime, Constants.SSS_WORK_START_TIME);
		// 就業時間と定時のうち早い方を、受講終了時間
		TrainingTime endTime = trainingTime.min(trainingEndTime, Constants.SSS_WORK_END_TIME);
		// 休憩開始と就業時間のうち早いのを、午前の終了時間
		TrainingTime firstHalfEndTime = trainingTime.min(trainingEndTime,
				Constants.SSS_REST_START_TIME);
		// 休憩終了と就業時間のうち遅いのを、午後の開始時間
		TrainingTime lastHalfStartTime = trainingTime.max(trainingStartTime,
				Constants.SSS_REST_END_TIME);
		// 前半と、後半の有効時間を足した結果を取得
		TrainingTime total = amPMTotalTime(startTime, endTime, firstHalfEndTime,
				lastHalfStartTime);
		return total;
	}
	
	
	/**
	 * 午前・午後の勤務時間の計算
	 * @author 小松原　Task.27 2025/11/26
	 * @param trainingStartTime
	 * @param trainingEndTime
	 * @param firstHalfEndTime
	 * @param lastHalfStartTime
	 * @return
	 */
	public TrainingTime amPMTotalTime(TrainingTime trainingStartTime, TrainingTime trainingEndTime,
			TrainingTime firstHalfEndTime, TrainingTime lastHalfStartTime) {
		TrainingTime firstHalf = new TrainingTime(0, 0);
		TrainingTime lastHalf = new TrainingTime(0, 0);
		//「午前の終了時間 > 勤務開始時間」の場合のみ午前勤務が発生
		if (firstHalfEndTime.compareTo(trainingStartTime) > 0) {
			// 午前勤務時間 = 午前終了時間 - 勤務開始時間
			firstHalf = firstHalfEndTime.subtract(trainingStartTime);
		}
		// 「午後開始時間 < 勤務終了時間」の場合
		if (lastHalfStartTime.compareTo(trainingEndTime) < 0) {
			// 午後勤務時間 = 勤務終了時間 - 午後開始時間
			lastHalf = trainingEndTime.subtract(lastHalfStartTime);
		}
		TrainingTime total = firstHalf.add(lastHalf);
		return total;
	}







}
