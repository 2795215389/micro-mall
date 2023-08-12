package com.changgou.log.aop;

import com.alibaba.fastjson.JSON;
import com.changgou.entity.IpInfoUtil;
import com.changgou.entity.ThreadPoolUtil;
import com.changgou.log.pojo.LogInfo;
import com.changgou.log.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


@Aspect
@Component
@Slf4j
public class SystemLogAspect implements HandlerInterceptor {//拦截器

    //防止线程竞技，保证时间是单线程
    /*
    springmvc默认是单例的，每一个请求进入，都会启动一个线程，会存在线程安全问题，
    即最好不要在controller,service层使用全局变量，
    如果存在对全局变量的修改，会出现线程安全问题。
    *
    */
    private static final ThreadLocal<Date> beginTimeThreadLocal=new NamedThreadLocal<>("ThreadLocal beginTime");

    private LogService logservice;
    

    @Pointcut("@annotation(com.changgou.log.aop.SystemLog)")//切面只对该注解生效
    // 等效 @Pointcut("execution(* com.js.mall.consumer.controller.*.*(..))")
    public void controllerAspect(){
        System.out.println("这是一个切点！！！");
    }




    @Before("controllerAspect()")
    public void before(JoinPoint joinPoint){//连接点----也就是方法
        Date beginTime=new Date();
        beginTimeThreadLocal.set(beginTime);
    }

    public static Map<String,Object> getControllerMethodInfo(JoinPoint joinPoint) throws ClassNotFoundException, NoSuchMethodException {
        Map<String,Object> map=new HashMap(16);
        //方法名和形参列表共同组成方法签名。
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();//获取连接点方法对象
        //获取目标类名
//        String targetName=joinPoint.getTarget().getClass().getName();
//        //获取方法名
//        String methodName=joinPoint.getSignature().getName();
//        //获取相关参数
//        Object[] arguments=joinPoint.getArgs();
//        //生成类对象
//        Class targetClass=Class.forName(targetName);
//        //生成该类的方法
//        Method[] methods=targetClass.getMethods();
//        String description="";
//        int type=0;
//        for(Method method:methods){
//            if(!method.getName().equals(methodName)){
//                continue;
//            }
//            //参数个数相等
//            Class[] clazz=method.getParameterTypes();
//            if(clazz.length!=arguments.length){
//                continue;
//            }
//            //参数类型也相等
//            Class<?>[] parameterTypes = method.getParameterTypes();
//            for(Class parameterType: parameterTypes){
//                if(!parameterType.getName().equals("String")){
//                    continue;
//                }
//            }

            //获取注解中的方法描述
            String description=method.getAnnotation(SystemLog.class).description();
            //获取注解中的LogType枚举类中的序号
            int type=method.getAnnotation(SystemLog.class).type().ordinal();//取出序号

            map.put("description",description);
            map.put("type",type);
        return map;
    }


    @AfterReturning("controllerAspect()")
    public void after(JoinPoint joinPoint){//返回coast time
        try{
            //得到请求属性
            ServletRequestAttributes attributes=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
            //得到前端请求，获得众多参数
            HttpServletRequest request=attributes.getRequest();

            Map<String ,Object> map=getControllerMethodInfo(joinPoint);
            LogInfo log=new LogInfo();


//            String token=request.getHeader("Authorization");
//            if(token==null){
//                throw new RuntimeException("无token ，请重新登录");
//            }
//            token=token.split("@")[1];//Authorization@token内容
//
//            Long userid=Long.parseLong(tokenService.getUserId(token));
//
//            UmsAdmin user=userService.findUserById(userid);
//            if(user==null){
//                throw new RuntimeException("用户不存在 ，请重新登录");
//            }
//            if(!tokenService.checkSign(token,user.getPassword())){
//                throw new RuntimeException("token验证失败！");
//            }


            //why随机数：也就是日志中的userId，因为创建用户繁琐，所以直接使用随机数来模拟不同的用户
            //用于之后flink进行统计
            Random random=new Random();

            // 模拟
            int userid1=random.nextInt(100);
            log.setUserid(userid1);
            //日志标题
            log.setName(map.get("description").toString());
            log.setLogType(Integer.parseInt(map.get("type").toString()));

            log.setRequestUrl(request.getRequestURI());
            log.setRequestType(request.getMethod());

            //替换存储格式，Jason的逗号分割符---->&&
            Map <String,String[]> requestParams=request.getParameterMap();
            String objectStr=JSON.toJSONString(requestParams).replace(",","&&");
            //Json双引号替换为单引号进行存储
            log.setRequestParam(objectStr.replace("\"","\'"));
            //请求IP
            log.setIp(IpInfoUtil.getIpAddr(request));
            log.setCreateTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            log.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

            //计算消耗时间
            long beginTime=beginTimeThreadLocal.get().getTime();
            //ThreadLocal使用后最好释放，避免内存泄漏
            beginTimeThreadLocal.remove();
            long endTime=System.currentTimeMillis();
            //耗时
            Long cost=(endTime-beginTime)/1000;
            log.setCostTime(cost.intValue());
            //自己配置参数的线程池，模拟多线程,SaveSystemLogThread线程执行run方法,将日志对象存储到数据库
            ThreadPoolUtil.getPool().execute(new SaveSystemLogThread(log,logservice));

        }catch(Exception e){
            e.printStackTrace();
        }
    }






}
