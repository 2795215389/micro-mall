package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.entity.Result;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.dao.SkuEsMapper;
import com.changgou.search.pojo.SkuInfo;
import com.changgou.search.service.SearchResultMapperImpl;
import com.changgou.search.service.SkuService;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 描述
 *
 */
@Service
public class SkuServiceImpl implements SkuService {


    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private SkuEsMapper skuEsMapper;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;


    @Override
    public void importEs() {
        //1.调用 goods微服务的fegin 查询 符合条件的sku的数据
        Result<List<Sku>> skuResult = skuFeign.findByStatus("1");
        List<Sku> data = skuResult.getData();//sku的列表
        //将sku的列表 转换成es中的skuinfo的列表
        List<SkuInfo> skuInfos = JSON.parseArray(JSON.toJSONString(data), SkuInfo.class);
        for (SkuInfo skuInfo : skuInfos) {
            //获取规格的数据  {"电视音响效果":"立体声","电视屏幕尺寸":"20英寸","尺码":"165"}

            //转成MAP  key: 规格的名称  value:规格的选项的值
            Map<String, Object> map = JSON.parseObject(skuInfo.getSpec(), Map.class);
            skuInfo.setSpecMap(map);
        }


        // 2.调用spring data elasticsearch的API 导入到ES中
        skuEsMapper.saveAll(skuInfos);
    }


    /**
     *
     * @param searchMap
     * key:
     *      category     商品分类的过滤查询
     *
     *      brand        商品品牌的过滤查询   TCL
     *
     *      spec_规格的名称   value  规格的值      规格相关
     *
     *      price           value 价格区间的字符串 0-500   3000-*
     *
     *
     *      pageNum   当前的页码
     *      pageSize  不用传递   (写死)
     *
     *
     *      sortField  要排序的字段  price
     *      sortRule   要排序的规则 (ASC DESC)
     *
     *
     * @return
     */

    @Override
    public Map search(Map<String, String> searchMap) {
        //1.获取到关键字
        String keywords = searchMap.get("keywords");

        //2.判断是否为空 如果 为空 给一个默认 值:华为
        if (StringUtils.isEmpty(keywords)) {
            keywords = "华为";
        }
        //3.创建 查询构建对象
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        //4.设置 查询的条件

        // 4.1 商品分类的列表展示: 按照商品分类的名称来分组，就是页面中的【筛选条件】；
        // 分组结果会在JSON数据中显示【skuCategorygroup】：【sku数据】
        //terms  指定分组的一个别名
        //field 指定要分组的【字段名】
        //size 指定查询结果的数量 默认是10个
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategorygroup").field("categoryName").size(50));

        //4.2 商品的品牌的列表展示  按照商品品牌来进行分组
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrandgroup").field("brandName").size(100));

        //4.3 商品的规格的列表展示   按照商品的规格的字段spec 进行分组
        //规则 要求 字段 是一个keyword类型的  spec.keyword
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpecgroup").field("spec.keyword").size(500));


        //4.4 设置高亮的字段 设置前缀 和 后缀

        //设置高亮的字段 针对 商品的名称进行高亮
        nativeSearchQueryBuilder.withHighlightFields(new HighlightBuilder.Field("name"));
        //设置前缀 和 后缀
        nativeSearchQueryBuilder.withHighlightBuilder(new HighlightBuilder().preTags("<em style=\"color:red\">").postTags("</em>"));




        //匹配查询  先分词 再查询  主条件查询
        //参数1 指定要搜索的字段
        //参数2 要搜索的值(先分词 再搜索)
        //nativeSearchQueryBuilder.withQuery(QueryBuilders.matchQuery("name", keywords));
        //从多个字段中搜索数据
        nativeSearchQueryBuilder.withQuery(QueryBuilders.multiMatchQuery(keywords,"name","categoryName","brandName"));



        //========================过滤查询 开始=====================================

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //4.4 过滤查询的条件设置   商品分类的条件
        String category = searchMap.get("category");

        if(!StringUtils.isEmpty(category)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("categoryName", category));
        }
        //4.5 过滤查询的条件设置   商品品牌的条件
        String brand = searchMap.get("brand");

        if(!StringUtils.isEmpty(brand)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("brandName", brand));
        }

        //4.6 过滤查询的条件设置   规格条件

        if(searchMap!=null){
            for (String key : searchMap.keySet()) {//{ brand:"",category:"",spec_网络:"电信4G"}
                if(key.startsWith("spec_"))  {
                    //截取规格的名称
                    boolQueryBuilder.filter(QueryBuilders.termQuery("specMap."+key.substring(5)+".keyword", searchMap.get(key)));
                }
            }
        }
        //4.7 过滤查询的条件设置   价格区间的过滤查询
        String price = searchMap.get("price");// 0-500  3000-*
        if(!StringUtils.isEmpty(price)){
            //获取值 按照- 切割
            String[] split = price.split("-");
            //过滤范围查询
            //0<=price<=500
            if(!split[1].equals("*")) {
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").from(split[0], true).to(split[1], true));
            }else{
                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(split[0]));
            }

        }


        //过滤查询
        nativeSearchQueryBuilder.withFilter(boolQueryBuilder);

      //========================过滤查询 结束=====================================


        //分页查询

        //第一个参数:指定当前的页码  注意: 如果是第一页 数值为0
        //第二个参数:指定当前的页的显示的行
        String pageNum1 = searchMap.get("pageNum");
        Integer pageNum=Integer.valueOf(pageNum1);

        Integer pageSize=30;

        nativeSearchQueryBuilder.withPageable(PageRequest.of(pageNum-1,pageSize));


        //排序操作
        //获取排序的字段 和要排序的规则
        String sortField = searchMap.get("sortField");//price
        String sortRule = searchMap.get("sortRule");//DESC ASC
        if(!StringUtils.isEmpty(sortField) && !StringUtils.isEmpty(sortRule)) {
            //执行排序
            nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort(sortField).order(sortRule.equalsIgnoreCase("ASC")?SortOrder.ASC:SortOrder.DESC));
            //nativeSearchQueryBuilder.withSort(SortBuilders.fieldSort(sortField).order(SortOrder.valueOf(sortRule)));
        }

        //5.构建查询对象(封装了查询的语法)
        NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

        //6.执行查询
        AggregatedPage<SkuInfo> skuInfos = elasticsearchTemplate.queryForPage(nativeSearchQuery, SkuInfo.class,new SearchResultMapperImpl());


        // 6.2 获取聚合分组结果  获取商品分类的列表数据
        StringTerms stringTermsCategory = (StringTerms) skuInfos.getAggregation("skuCategorygroup");
        List<String> categoryList = getStringsCategoryList(stringTermsCategory);


        //6.3 获取 品牌分组结果 列表数据

        StringTerms stringTermsBrand = (StringTerms) skuInfos.getAggregation("skuBrandgroup");
        List<String> brandList = getStringsBrandList(stringTermsBrand);

        //6.4 获取 规格的分组结果 列表数据map
        StringTerms stringTermsSpec = (StringTerms) skuInfos.getAggregation("skuSpecgroup");
        Map<String, Set<String>> specMap = getStringSetMap(stringTermsSpec);

        //7.获取结果  返回map

        List<SkuInfo> content = skuInfos.getContent();//当前的页的集合
        int totalPages = skuInfos.getTotalPages();//总页数
        long totalElements = skuInfos.getTotalElements();//总记录数

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("categoryList", categoryList);//商品分类的列表数据
        resultMap.put("brandList", brandList);   //商品品牌的列表数据
        resultMap.put("specMap", specMap);   //商品规格的列表数据展示
        resultMap.put("rows", content);
        resultMap.put("total", totalElements);
        resultMap.put("totalPages", totalPages);
        return resultMap;
    }

    private Map<String, Set<String>> getStringSetMap(StringTerms stringTermsSpec) {
        //key :规格的名称
        //value :规格名称对应的选项的多个值集合set
        Map<String, Set<String>> specMap = new HashMap<String, Set<String>>();
        Set<String> specValues = new HashSet<String>();
        if (stringTermsSpec != null) {
            //1. 获取分组的结果集
            for (StringTerms.Bucket bucket : stringTermsSpec.getBuckets()) {
                //2.去除结果集的每一行数据()   {"手机屏幕尺寸":"5.5寸","网络":"电信4G","颜色":"白","测试":"s11","机身内存":"128G","存储":"16G","像素":"300万像素"}
                String keyAsString = bucket.getKeyAsString();

                //3.转成JSON 对象  map  key :规格的名称  value:规格名对应的选项的单个值
                Map<String, String> map = JSON.parseObject(keyAsString, Map.class);
                for (Map.Entry<String, String> stringStringEntry : map.entrySet()) {
                    String key = stringStringEntry.getKey();//规格名称   手机屏幕尺寸
                    String value = stringStringEntry.getValue();//规格的名称对应的单个选项值 5.5寸

                    //先从原来的specMap中 获取 某一个规格名称 对应的规格的选项值集合
                    specValues = specMap.get(key);
                    if (specValues == null) {
                        specValues = new HashSet<>();
                    }
                    specValues.add(value);
                    //4.提取map中的值放入到返回的map中
                    specMap.put(key, specValues);
                }
            }
        }
        return specMap;
    }

    private List<String> getStringsBrandList(StringTerms stringTermsBrand) {
        List<String> brandList = new ArrayList<>();
        if (stringTermsBrand != null) {
            for (StringTerms.Bucket bucket : stringTermsBrand.getBuckets()) {
                String keyAsString = bucket.getKeyAsString();//品牌的名称 huawei
                brandList.add(keyAsString);
            }
        }
        return brandList;
    }

    /**
     * 获取分组结果   商品分类的分组结果
     *
     * @param stringTermsCategory
     * @return
     */
    private List<String> getStringsCategoryList(StringTerms stringTermsCategory) {
        List<String> categoryList = new ArrayList<>();
        if (stringTermsCategory != null) {
            for (StringTerms.Bucket bucket : stringTermsCategory.getBuckets()) {
                String keyAsString = bucket.getKeyAsString();
                System.out.println(keyAsString);//就是商品分类的数据
                categoryList.add(keyAsString);
            }
        }
        return categoryList;
    }


    public Map<String, Object> searchGoods(Map<String, String> searchMap) {

        // 搜索条件封装
        // 组合条件：如果用户选择了指定的品牌种类等，那么就需要缩小范围
        // BoolQuery: must,must_not,should,filter
        NativeSearchQueryBuilder nativeSearchQueryBuilder = getSearchQueryBuilder(searchMap);


        // 高亮 <em style = ""> </em>
        HighlightBuilder.Field field = new HighlightBuilder.Field("name");
        field.preTags("<em style = \"color:red\">");
        field.postTags("</em>");
        // 碎片长度，超过了就不显示
        field.fragmentSize(20);
        nativeSearchQueryBuilder.withHighlightFields();

        // 执行搜索获取结果集
        Map<String, Object> resultMap = searchList(nativeSearchQueryBuilder);

        // 当用户选择了这些参数，那么就需要缩小范围，没有选择时才全部展示
        if(searchMap == null || StringUtils.isEmpty(searchMap.get("category"))){
            // 搜索出【页面中选择的条件】,同理规格品牌也是如此
            List<String> categoryList = searchCategoryList(nativeSearchQueryBuilder);
            resultMap.put("categoryList", categoryList);
        }
        if(searchMap == null || StringUtils.isEmpty(searchMap.get("brand"))){
            List<String> brandList = searchBrandList(nativeSearchQueryBuilder);
            resultMap.put("brandList", brandList);
        }
        if(searchMap == null || StringUtils.isEmpty(searchMap.get("brand"))){
            Map<String,Set<String>> specMap = searchSpecList(nativeSearchQueryBuilder);
            resultMap.put("specList", specMap);
        }


        return resultMap;
    }

    private  NativeSearchQueryBuilder getSearchQueryBuilder(Map<String, String> searchMap) {
        //创建查询对象 的构建对象
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        // BoolQuery: must,must_not,should,filter
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //搜索框关键字
        if(!CollectionUtils.isEmpty(searchMap)){
            String keywords = searchMap.get("keywords");
            if (!StringUtils.isEmpty(keywords)) {
                // 设置查询的条件:根据【搜搜框输入的关键词】搜索
                boolQueryBuilder.must(QueryBuilders.queryStringQuery(keywords).field("name"));
            }
            // 用户选择了种类，品牌等，缩小范围。不需要分词用term
            if (!StringUtils.isEmpty(searchMap.get("category"))) {
                boolQueryBuilder.must(QueryBuilders.termQuery("categoryName", "category"));
            }

            if (!StringUtils.isEmpty(searchMap.get("brand"))) {
                boolQueryBuilder.must(QueryBuilders.termQuery("brandName", "brand"));
            }
            // 规格有很多种，加一个前缀判断spec_xxx
            for (Map.Entry<String, String> entry : searchMap.entrySet()) {
                String key = entry.getKey();
                if(key.startsWith("spec_")){
                    String value = entry.getValue();
                    // 索引库中的字段specMap.属性；部分词后缀加.keyword
                    boolQueryBuilder.must(QueryBuilders.termQuery("specMap."+key.substring(5)+".keyword", value));
                }
            }

            // 过滤价格：   入参其中一个【0-499元    500-999元   1000-1500元.....     10000元以上】
            String price = searchMap.get("price");
            if(!StringUtils.isEmpty(price)){
                price = price.replace("元", "").replace("以上", "");
                String prices[] = price.split("-");
                // 解析完毕，price[0]<=price<=price[1]
                if(prices!=null && prices.length > 0){
                    boolQueryBuilder.must(QueryBuilders.rangeQuery("price")
                            .gt(Integer.parseInt(prices[0])));
                }
                if(prices.length == 2){
                    boolQueryBuilder.must(QueryBuilders.rangeQuery("price").lt(Integer.valueOf(prices[1])));
                }
                // 分页,给定默认值
                Integer pageNum = searchMap.get("pageNum") == null ? 1 : Integer.parseInt(searchMap.get("pageNum"));
                Integer pageSize = searchMap.get("pageSize") == null ? 30 : Integer.parseInt(searchMap.get("pageSize"));
                nativeSearchQueryBuilder.withPageable(PageRequest.of(pageNum - 1, pageSize));

                // 排序实现【综合，销量，价格...】，升序降序
                String sortFiled = searchMap.get("sortFiled");
                String sortRule = searchMap.get("sortRule");
                if (!StringUtils.isEmpty(sortFiled) && !StringUtils.isEmpty(sortRule)) {
                    // 指定【排序域，排序规则】
                    nativeSearchQueryBuilder.withSort(new FieldSortBuilder(sortFiled).order(SortOrder.valueOf(sortRule)));
                }
            }

        }

        // 填充NativeQuery对象
        nativeSearchQueryBuilder.withQuery(boolQueryBuilder);
        return nativeSearchQueryBuilder;
    }

    private Map<String, Object> searchList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        //构建查询对象
        NativeSearchQuery query = nativeSearchQueryBuilder.build();

        //执行查询,将结果数据封装在对象中，需要用【高亮数据】替换非高亮
        AggregatedPage<SkuInfo> skuPage = elasticsearchTemplate.queryForPage(query, SkuInfo.class,
                new SearchResultMapper() {
                    @Override
                    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                        List<T> list = new ArrayList<>();
                        for (SearchHit hit : response.getHits()) {
                            // 非高亮
                            SkuInfo skuInfo = JSON.parseObject(hit.getSourceAsString(), SkuInfo.class);
                            // 解析高亮
                            HighlightField highlightField = hit.getHighlightFields().get("name");
                            if (highlightField != null && highlightField.getFragments() != null){
                                Text[] fragments = highlightField.getFragments();
                                StringBuilder sb = new StringBuilder();
                                for (Text fragment : fragments){
                                    sb.append(fragment.toString());
                                }
                                // 替换
                                skuInfo.setName(sb.toString());
                            }
                            list.add((T) skuInfo);
                        }
                        // 搜索集合数据；分页对象；总条数
                        return new AggregatedPageImpl<>(list, pageable, response.getHits().getTotalHits());
                    }
                });

        //返回结果
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("rows", skuPage.getContent());
        resultMap.put("total", skuPage.getTotalElements());
        resultMap.put("totalPages", skuPage.getTotalPages());
        return resultMap;
    }

    public List<String> searchCategoryList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        /**
         * 搜索出【页面中选择的条件】
         * 聚合操作：统计个数，分组...
         *
         *
         * terms：分组名称【别名】，
         * filed:表示根据【域/列】分组
         */
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategory").field("categoryName"));
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);
        /**
         * 获取分组数据
         * aggregatedPage.getAggregations()获取的是集合，可以根据多个域进行分组
         * get(“分组名”):获取指定域的集合数据   数据样例【手机，充电器，配件】
         */
        List<String> categoryList = new ArrayList<>();
        StringTerms skuCategoryAggregation = aggregatedPage.getAggregations().get("skuCategory");
        for (StringTerms.Bucket bucket : skuCategoryAggregation.getBuckets()) {
            String categoryName = bucket.getKeyAsString();
            categoryList.add(categoryName);
        }
        return categoryList;
    }


    public List<String> searchBrandList(NativeSearchQueryBuilder nativeSearchQueryBuilder){
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("brandList").field("brandName"));
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);
        List<String> brandList = new ArrayList<>();
        StringTerms brandAggregation = aggregatedPage.getAggregations().get("brandList");
        for (StringTerms.Bucket bucket : brandAggregation.getBuckets()) {
            String categoryName = bucket.getKeyAsString();
            brandList.add(categoryName);
        }
        return brandList;
    }

    public Map<String,Set<String>> searchSpecList(NativeSearchQueryBuilder nativeSearchQueryBuilder){
        // spec.keyword:指的是不对该字段分词
        // 默认分页为10，分页设置大一点保证spec比较全
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("specList").field("spec.keyword").size(5000));
        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);
        List<String> specList = new ArrayList<>();
        // 每条数据：{{"电视机尺寸":"20英寸"},{"尺码":"165"},{}}
        StringTerms specAggregation = aggregatedPage.getAggregations().get("specList");
        for (StringTerms.Bucket bucket : specAggregation.getBuckets()) {
            String categoryName = bucket.getKeyAsString();
            specList.add(categoryName);
        }

        // 整理数据
        Map<String,Set<String>> allSpec = new HashMap<>();
        for(String spec : specList){
            Map<String,String> specMap = JSON.parseObject(spec, Map.class);
            for (Map.Entry<String, String> entry : specMap.entrySet()){
                String key = entry.getKey();
                String val = entry.getValue();
                Set<String> spcSet = allSpec.get(key);
                if(spcSet == null){
                    spcSet = new HashSet<>();
                }
                spcSet.add(val);
                allSpec.put(key, spcSet);
            }
        }

        return allSpec;
    }



}
