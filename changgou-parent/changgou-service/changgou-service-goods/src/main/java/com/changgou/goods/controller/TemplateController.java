package com.changgou.goods.controller;

import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.goods.pojo.Template;
import com.changgou.goods.service.TemplateService;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/template")
@CrossOrigin
@Api("模板管理") // 比如手机，电视....，一个种类的商品关联一个模板这样就可以缩小它规格的范围
public class TemplateController {

    @Autowired
    private TemplateService templateService;

    /***
     * Template分页条件搜索实现
     * @param template
     * @param page
     * @param size
     * @return
     */
    @PostMapping(value = "/search/{page}/{size}" )
    @ApiOperation("Template分页条件搜索实现")
    public Result<PageInfo> findPage(@RequestBody(required = false)  Template template, @PathVariable  int page, @PathVariable  int size){
        //调用TemplateService实现分页条件查询Template
        PageInfo<Template> pageInfo = templateService.findPage(template, page, size);
        return new Result(true, StatusCode.OK,"查询成功",pageInfo);
    }

    /***
     * Template分页搜索实现
     * @param page:当前页
     * @param size:每页显示多少条
     * @return
     */
    @GetMapping(value = "/search/{page}/{size}" )
    @ApiOperation("Template分页搜索实现")
    public Result<PageInfo> findPage(@PathVariable  int page, @PathVariable  int size){
        //调用TemplateService实现分页查询Template
        PageInfo<Template> pageInfo = templateService.findPage(page, size);
        return new Result<PageInfo>(true,StatusCode.OK,"查询成功",pageInfo);
    }

    /***
     * 多条件搜索品牌数据
     * @param template
     * @return
     */
    @PostMapping(value = "/search" )
    @ApiOperation("多条件搜索品牌数据")
    public Result<List<Template>> findList(@RequestBody(required = false)  Template template){
        //调用TemplateService实现条件查询Template
        List<Template> list = templateService.findList(template);
        return new Result<List<Template>>(true,StatusCode.OK,"查询成功",list);
    }

    /***
     * 根据ID删除品牌数据
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}" )
    @ApiOperation("根据ID删除品牌数据")
    public Result delete(@PathVariable Integer id){
        //调用TemplateService实现根据主键删除
        templateService.delete(id);
        return new Result(true,StatusCode.OK,"删除成功");
    }

    /***
     * 修改Template数据
     * @param template
     * @param id
     * @return
     */
    @PutMapping(value="/{id}")
    @ApiOperation("修改Template数据")
    public Result update(@RequestBody  Template template,@PathVariable Integer id){
        //设置主键值
        template.setId(id);
        //调用TemplateService实现修改Template
        templateService.update(template);
        return new Result(true,StatusCode.OK,"修改成功");
    }

    /***
     * 新增Template数据
     * @param template
     * @return
     */
    @PostMapping
    @ApiOperation("新增Template数据")
    public Result add(@RequestBody   Template template){
        //调用TemplateService实现添加Template
        templateService.add(template);
        return new Result(true,StatusCode.OK,"添加成功");
    }

    /***
     * 根据ID查询Template数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询Template数据")
    public Result<Template> findById(@PathVariable Integer id){
        //调用TemplateService实现根据主键查询Template
        Template template = templateService.findById(id);
        return new Result<Template>(true,StatusCode.OK,"查询成功",template);
    }

    /***
     * 查询Template全部数据
     * @return
     */
    @GetMapping
    @ApiOperation("查询Template全部数据")
    public Result<List<Template>> findAll(){
        //调用TemplateService实现查询所有Template
        List<Template> list = templateService.findAll();
        return new Result<List<Template>>(true, StatusCode.OK,"查询成功",list) ;
    }
}
