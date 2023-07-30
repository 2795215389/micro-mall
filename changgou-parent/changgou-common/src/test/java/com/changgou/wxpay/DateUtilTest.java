package com.changgou.wxpay;

import com.changgou.entity.DateUtil;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * author:JiangSong
 * Date:2023/7/29
 **/


public class DateUtilTest {

    @Test
    public void testGetMenus() {
        List<Date> dateMenus = DateUtil.getDateMenus();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
        for (Date date : dateMenus) {
            System.out.println(sdf.format(date));
        }
    }

    @Test
    public void testGetDates() {
        List<Date> dates = DateUtil.getDates(12);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
        for (Date date : dates) {
            System.out.println(sdf.format(date));
        }
    }
}
