package com.changgou.goods.controller;

import com.changgou.entity.Result;
import com.changgou.entity.StatusCode;
import com.changgou.goods.pojo.Spec;
import com.changgou.goods.service.SpecService;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/spec")
@CrossOrigin
@Api("规格管理")
public class SpecController {

    @Autowired
    private SpecService specService;

    /***
     * Spec分页条件搜索实现
     * @param spec
     * @param page
     * @param size
     * @return
     */
    @PostMapping(value = "/search/{page}/{size}" )
    @ApiOperation("Spec分页条件搜索实现")
    public Result<PageInfo> findPage(@RequestBody(required = false)  Spec spec, @PathVariable  int page, @PathVariable  int size){
        //调用SpecService实现分页条件查询Spec
        PageInfo<Spec> pageInfo = specService.findPage(spec, page, size);
        return new Result(true, StatusCode.OK,"查询成功",pageInfo);
    }

    /***
     * Spec分页搜索实现
     * @param page:当前页
     * @param size:每页显示多少条
     * @return
     */
    @GetMapping(value = "/search/{page}/{size}" )
    @ApiOperation("Spec分页搜索实现")
    public Result<PageInfo> findPage(@PathVariable  int page, @PathVariable  int size){
        //调用SpecService实现分页查询Spec
        PageInfo<Spec> pageInfo = specService.findPage(page, size);
        return new Result<PageInfo>(true,StatusCode.OK,"查询成功",pageInfo);
    }

    /***
     * 多条件搜索品牌数据
     * @param spec
     * @return
     */
    @PostMapping(value = "/search" )
    @ApiOperation("多条件搜索品牌数据")
    public Result<List<Spec>> findList(@RequestBody(required = false)  Spec spec){
        //调用SpecService实现条件查询Spec
        List<Spec> list = specService.findList(spec);
        return new Result<List<Spec>>(true,StatusCode.OK,"查询成功",list);
    }

    /***
     * 根据ID删除品牌数据
     * @param id
     * @return
     */
    @DeleteMapping(value = "/{id}" )
    @ApiOperation("根据ID删除品牌数据")
    public Result delete(@PathVariable Integer id){
        //调用SpecService实现根据主键删除
        specService.delete(id);
        return new Result(true,StatusCode.OK,"删除成功");
    }

    /***
     * 修改Spec数据
     * @param spec
     * @param id
     * @return
     */
    @PutMapping(value="/{id}")
    @ApiOperation("修改Spec数据")
    public Result update(@RequestBody  Spec spec,@PathVariable Integer id){
        //设置主键值
        spec.setId(id);
        //调用SpecService实现修改Spec
        specService.update(spec);
        return new Result(true,StatusCode.OK,"修改成功");
    }

    /***
     * 新增Spec数据
     * @param spec
     * @return
     */
    @PostMapping
    @ApiOperation("新增Spec数据")
    public Result add(@RequestBody   Spec spec){
        //调用SpecService实现添加Spec
        specService.add(spec);
        return new Result(true,StatusCode.OK,"添加成功");
    }

    /***
     * 根据ID查询Spec数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询Spec数据")
    public Result<Spec> findById(@PathVariable Integer id){
        //调用SpecService实现根据主键查询Spec
        Spec spec = specService.findById(id);
        return new Result<Spec>(true,StatusCode.OK,"查询成功",spec);
    }

    /***
     * 查询Spec全部数据
     * @return
     */
    @GetMapping
    @ApiOperation("查询Spec全部数据")
    public Result<List<Spec>> findAll(){
        //调用SpecService实现查询所有Spec
        List<Spec> list = specService.findAll();
        return new Result<List<Spec>>(true, StatusCode.OK,"查询成功",list) ;
    }


    /**
     * 根据商品分类的ID 查询该分类对应的 规格的列表
     *
     */


    @GetMapping("/category/{id}")
    @ApiOperation("根据商品分类的ID 查询该分类对应的 规格的列表")
    public Result<List<Spec>> findByCategoryId(@PathVariable(name="id") Integer id){
        List<Spec> specList = specService.findByCategoryId(id);
        return new Result<List<Spec>>(true,StatusCode.OK,"查询规格的列表成功",specList);
    }
}
