package com.changgou.entity;

import lombok.Data;

import java.io.Serializable;


@Data
public class AIFaceBean implements Serializable { //前端给后端的数据
	private String imgdata;//二进制字节流-图片信息
	private String error_code;
	private String error_msg;
	private float score;

}
