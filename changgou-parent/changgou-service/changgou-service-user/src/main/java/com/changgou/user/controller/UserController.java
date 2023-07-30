package com.changgou.user.controller;

import com.alibaba.fastjson.JSON;
import com.baidu.aip.face.AipFace;
import com.changgou.entity.*;
import com.changgou.user.pojo.User;
import com.changgou.user.service.UserService;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.ApiOperation;
import org.json.JSONObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;



@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;


    private AipFace aipFace = AIFactoryUtil.getAipFace();

    /***
     * User分页条件搜索实现
     * @param user
     * @param page
     * @param size
     * @return
     */
    @PostMapping(value = "/search/{page}/{size}")
    public Result<PageInfo> findPage(@RequestBody(required = false) User user, @PathVariable int page, @PathVariable int size) {
        //调用UserService实现分页条件查询User
        PageInfo<User> pageInfo = userService.findPage(user, page, size);
        return new Result(true, StatusCode.OK, "查询成功", pageInfo);
    }

    /***
     * User分页搜索实现
     * @param page:当前页
     * @param size:每页显示多少条
     * @return
     */
    @GetMapping(value = "/search/{page}/{size}")
    public Result<PageInfo> findPage(@PathVariable int page, @PathVariable int size) {
        //调用UserService实现分页查询User
        PageInfo<User> pageInfo = userService.findPage(page, size);
        return new Result<PageInfo>(true, StatusCode.OK, "查询成功", pageInfo);
    }

    /***
     * 多条件搜索品牌数据
     * @param user
     * @return
     */
    @PostMapping(value = "/search")
    public Result<List<User>> findList(@RequestBody(required = false) User user) {
        //调用UserService实现条件查询User
        List<User> list = userService.findList(user);
        return new Result<List<User>>(true, StatusCode.OK, "查询成功", list);
    }

    /***
     * 根据ID删除品牌数据
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}")
    public Result delete(@PathVariable String id) {
        //调用UserService实现根据主键删除
        userService.delete(id);
        return new Result(true, StatusCode.OK, "删除成功");
    }

    /***
     * 修改User数据
     * @param user
     * @param id
     * @return
     */
    @PutMapping(value = "/{id}")
    public Result update(@RequestBody User user, @PathVariable String id) {
        //设置主键值
        user.setUsername(id);
        //调用UserService实现修改User
        userService.update(user);
        return new Result(true, StatusCode.OK, "修改成功");
    }

    /***
     * 新增User数据
     * @param user
     * @return
     */
    @PostMapping
    public Result add(@RequestBody User user) {
        //调用UserService实现添加User
        userService.add(user);
        return new Result(true, StatusCode.OK, "添加成功");
    }

    /***
     * 根据ID查询User数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<User> findById(@PathVariable String id) {
        //调用UserService实现根据主键查询User
        User user = userService.findById(id);
        return new Result<User>(true, StatusCode.OK, "查询成功", user);
    }

    /**
     * 加载用户的数据,先对该方法放行，因为Oauth2.0要使用feign查询user数据库
     * @param id
     * @return
     */
    @GetMapping("/load/{id}")
    public Result<User> findByUsername(@PathVariable(name="id") String id) {
        //调用UserService实现根据主键查询User
        User user = userService.findById(id);
        return new Result<User>(true, StatusCode.OK, "查询成功", user);
    }

    /***
     *
     * 希望 拥有admin的角色人才能访问.
     * 查询User全部数据
     * @return
     */
    // @PreAuthorize 表示 在执行方法之前 先进行权限校验,只有拥有 admin角色的用户可以执行该方法.
    @PreAuthorize(value="hasAuthority('admin')")
    @GetMapping
    public Result<List<User>> findAll(HttpServletRequest request) {

        System.out.println("头信息为:"+request.getHeader("Authorization"));


        //调用UserService实现查询所有User
        List<User> list = userService.findAll();
        return new Result<List<User>>(true, StatusCode.OK, "查询成功", list);
    }

    @RequestMapping("/login")
    public Result<User> login(String username, String password, HttpServletResponse response, HttpServletRequest request) {
        //1.从数据库中查询用户名对应的用户的对象
        User user = userService.findById(username);
        if (user == null) {
            //2.判断用户是否为空 为空返回数据
            return new Result<User>(false, StatusCode.LOGINERROR, "用户名或密码错误");
        }

        //3如果不为空格 判断 密码是否正确 正确则登录成功

        if(BCrypt.checkpw(password,user.getPassword())){
            //成功
            Map<String,Object> info = new HashMap<String,Object>();
            info.put("role","USER");
            info.put("success","SUCCESS");
            info.put("username",username);

            //1.生成令牌
            String jwt = JwtUtil.createJWT(UUID.randomUUID().toString(), JSON.toJSONString(info), null);
            //2.设置cookie中
            Cookie cookie = new Cookie("Authorization",jwt);
            response.addCookie(cookie);
            //3.设置头文件中
            response.setHeader("Authorization",jwt);

            return new Result<User>(true, StatusCode.OK, "成功",jwt);
        }else{
            //失败
            return new Result<User>(false, StatusCode.LOGINERROR, "用户名或密码错误");
        }

    }



    @GetMapping(value = "/points/add")
    public Result addPoints(@RequestParam(value="points") Integer points
            ,@RequestParam(value="username") String username ) {

        userService.addPoints(points,username);
        return new Result(true,StatusCode.OK,"添加积分成功");
    }



    @ApiOperation(value="面部识别登录以后返回token")
    @PostMapping(value ="/flogin")
    @ResponseBody//响应体
    // 入参为图片信息
    public Result faceLogin(@RequestBody AIFaceBean faceBean) {
        Map<String,String> tokenMap=new HashMap();
        String groupList="login"; //分组
        //PNG图片
        JSONObject resultObject=aipFace.search(faceBean.getImgdata(),"BASE64",groupList,null);
        AIBaiduFaceBean faceSearchResponse= JSON.parseObject(resultObject.toString(), AIBaiduFaceBean.class);
        if(faceSearchResponse.getError_code().equals("0")&&faceSearchResponse.getError_msg().equals("SUCCESS")){//该图片是成功的
            //这里对人脸先检索 是否已经录入，设置判定条件为返回score大于80 即代表一个人
            HashMap<String,Object> m=  faceSearchResponse.getResult().getUser_list().get(0);

            if(Float.parseFloat(m.get("score").toString())>80f){
                faceBean.setError_code(faceSearchResponse.getError_code());
                faceBean.setError_msg(faceSearchResponse.getError_msg());
                //取出你曾经注册的用户id 也是图片名称
                String username=m.get("user_name").toString();
                //根据userid 取出用户
                User a=userService.findById(username);
                String password=a.getPassword();

                //生成token
                String jwt = JwtUtil.createJWT(username, "", 100000L);
                tokenMap.put("error_code",faceSearchResponse.getError_code());
                tokenMap.put("token",jwt);
                tokenMap.put("username",username);
                tokenMap.put("password",password);
                return new Result(true, 200, "登录成功", tokenMap);

            }else{
                return new Result<>(false, 10004, "人脸不匹配");
            }

        }else{
            return new Result<>(false, 10004, "百度智能云请求失败");
        }


    }


    @ApiOperation(value="用户注册")
    @PostMapping(value ="/reg")
    @ResponseBody
    public Result reg(@RequestBody User user){
        if(userService.findById(user.getUsername())!=null){//已经被注册了
            Map<String,String> tokenMap=new HashMap();
            tokenMap.put("error_code","500");
            return new Result(false,1001, "已经被注册",tokenMap);
        }

        User a=new User();
        BeanUtils.copyProperties(user, a);
        userService.add(user);
        //图片送百度
        String username=a.getUsername().toString();
        if (StringUtils.isEmpty(username)) {
            return new Result(true,10002, "注册成功，但是没有注册人脸");
        }
        String groupId="login";

        HashMap<String,String> options=new HashMap();
        options.put("user_info",a.getPassword());
        //人脸注册部分 前端摄像头传过来的数据的字头是：data:image/png:base64, 需要剔除它
        String b64=user.getPic().substring(22);//取出字头后面的数据即可,真实的图片字节流
        JSONObject object=aipFace.addUser(b64,"BASE64",groupId,username,options);
        System.out.println(object);
        return new Result(true,200,"注册成功");

    }
}
