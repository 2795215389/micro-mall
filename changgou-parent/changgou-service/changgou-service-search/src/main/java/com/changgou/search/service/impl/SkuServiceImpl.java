package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.entity.Result;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.dao.SkuEsMapper;
import com.changgou.search.pojo.SkuInfo;
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



    @Override
    public Map<String, Object> search(Map<String, String> searchMap) {

        // 搜索条件封装
        // 组合条件：如果用户选择了指定的品牌种类等，那么就需要缩小范围
        // BoolQuery: must,must_not,should,filter
        NativeSearchQueryBuilder nativeSearchQueryBuilder = getSearchQueryBuilder(searchMap);

        // 分页
        // 分页,给定默认值
        Integer pageNum = searchMap.get("pageNum") == null ? 1 : Integer.parseInt(searchMap.get("pageNum"));
        Integer pageSize = searchMap.get("pageSize") == null ? 10 : Integer.parseInt(searchMap.get("pageSize"));
        nativeSearchQueryBuilder.withPageable(PageRequest.of(pageNum - 1, pageSize));

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
//        if(searchMap == null || StringUtils.isEmpty(searchMap.get("category"))){
//            // 搜索出【页面中选择的条件】,同理规格品牌也是如此
//            List<String> categoryList = searchCategoryList(nativeSearchQueryBuilder);
//            resultMap.put("categoryList", categoryList);
//        }
//        if(searchMap == null || StringUtils.isEmpty(searchMap.get("brand"))){
//            List<String> brandList = searchBrandList(nativeSearchQueryBuilder);
//            resultMap.put("brandList", brandList);
//        }
//        if(searchMap == null || StringUtils.isEmpty(searchMap.get("brand"))){
//            Map<String,Set<String>> specMap = searchSpecList(nativeSearchQueryBuilder);
//            resultMap.put("specList", specMap);
//        }
        // 优化,只向ES发送一次请求
        Map<String, Object> cbsGroupMap = searchCbsGroup(nativeSearchQueryBuilder, searchMap);
        resultMap.putAll(cbsGroupMap);
        // 放入分页参数，解决feign调用出现空指针问题
        resultMap.put("pageNum", pageNum);
        resultMap.put("pageSize", pageSize);
        return resultMap;
    }

    private  NativeSearchQueryBuilder getSearchQueryBuilder(Map<String, String> searchMap) {
        //创建查询对象 的构建对象
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        // BoolQuery: must,must_not,should,filter；用must会算分，filter不算分性能更好
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //搜索框关键字
        if(!CollectionUtils.isEmpty(searchMap)){
            String keywords = searchMap.get("keywords");
            if (!StringUtils.isEmpty(keywords)) {
                // 设置查询的条件:根据【搜搜框输入的关键词】搜索;
                boolQueryBuilder.filter(QueryBuilders.queryStringQuery(keywords).field("name"));
            }
            // 用户选择了种类，品牌等，缩小范围。不需要分词用term
            if (!StringUtils.isEmpty(searchMap.get("category"))) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("categoryName", searchMap.get("category")));
            }

            if (!StringUtils.isEmpty(searchMap.get("brand"))) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("brandName", searchMap.get("brand")));
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
            }


            // 排序实现【综合，销量，价格...】，升序降序
            String sortFiled = searchMap.get("sortFiled");
            String sortRule = searchMap.get("sortRule");
            if (!StringUtils.isEmpty(sortFiled) && !StringUtils.isEmpty(sortRule)) {
                // 指定【排序域，排序规则】
                nativeSearchQueryBuilder.withSort(new FieldSortBuilder(sortFiled).order(SortOrder.valueOf(sortRule)));
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


    // 优化，原本向ES发送多个请求。降低请求数【IO操作】。将【分类，品牌，规格】分成一组查询
    public Map<String, Object> searchCbsGroup(NativeSearchQueryBuilder nativeSearchQueryBuilder, Map<String, String> searchMap){
        Map<String, Object>  cbsGroupMap = new HashMap<>();

        if(searchMap == null || StringUtils.isEmpty(searchMap.get("category"))){
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("categoryList").field("categoryName"));
        }
        if(searchMap == null || StringUtils.isEmpty(searchMap.get("brand"))){
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("brandList").field("brandName"));
        }
        if(searchMap == null || StringUtils.isEmpty(searchMap.get("spec"))){
            nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("specList").field("spec.keyword"));
        }

        AggregatedPage<SkuInfo> aggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        if(searchMap == null || StringUtils.isEmpty(searchMap.get("category"))){
            StringTerms categoryTerms =(StringTerms) aggregatedPage.getAggregation("categoryList");
            List<String> categoryList = getGroupList(categoryTerms);
            cbsGroupMap.put("categoryList", categoryList);
        }
        if(searchMap == null || StringUtils.isEmpty(searchMap.get("brand"))){
            StringTerms brandTerms =(StringTerms) aggregatedPage.getAggregation("brandList");
            List<String> brandList = getGroupList(brandTerms);
            cbsGroupMap.put("brandList", brandList);
        }
        if(searchMap == null || StringUtils.isEmpty(searchMap.get("spec"))){
            StringTerms specTerms =(StringTerms) aggregatedPage.getAggregation("specList");
            List<String> specList = getGroupList(specTerms);
            Map<String, Set<String>> allSpecMap = getAllSpecMap(specList);
            cbsGroupMap.put("specList", allSpecMap);
        }
        return cbsGroupMap;
    }

    // 获取分组集合数据
    public List<String> getGroupList (StringTerms stringTerms){
        List<String> fieldList = new ArrayList<>();
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            String fieldName = bucket.getKeyAsString();
            fieldList.add(fieldName);
        }
        return fieldList;
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

        Map<String, Set<String>> allSpec = getAllSpecMap(specList);
        return allSpec;
    }

    private static Map<String, Set<String>> getAllSpecMap(List<String> specList) {
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
