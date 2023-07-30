package com.changgou.entity;

import lombok.Data;

import java.util.HashMap;
import java.util.List;


@Data
public class AIBaiduFaceBean {
	private String log_id;
	private String error_msg;
	private String error_code;
	private String cached;
	private String timestamp;
	private Result result;

	@Data
	public static class Result{
		private String face_token;//这张人脸的唯一标识
		private List<HashMap<String,Object>> user_list;

	}
//	@Data
////	public static class UserList{
////		private float score;
////		private String group_id;
////		private String user_id;
////		private String user_info;
////
////	}
}
