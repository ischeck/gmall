package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParam;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;

import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
public class ListServiceImpl implements ListService{

    public static final String INDEX_NAME_GMALL="gmall";

    public static final String TYPE_NAME_GMALL="SkuInfo";

    @Autowired
    JestClient jestClient;

    @Autowired
    RedisUtil redisUtil;



    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";
   /* @Test
    public void testEs() throws IOException {
        String query="{\n" +
                "  \"query\": {\n" +
                "    \"match\": {\n" +
                "      \"actorList.name\": \"张译\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();

        SearchResult result = jestClient.execute(search);

        List<SearchResult.Hit<HashMap, Void>> hits = result.getHits(HashMap.class);

        for (SearchResult.Hit<HashMap, Void> hit : hits) {
            HashMap source = hit.source;
            System.err.println("source = " + source);

        }

    }*/

    /**
     * 存入数据
     * @param skuLsInfo
     */
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo){
        Index index= new Index.Builder(skuLsInfo).index(INDEX_NAME_GMALL).type(TYPE_NAME_GMALL).id(skuLsInfo.getId()).build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 查询数据
     * @param skuLsParam
     * @return
     */
    public SkuLsResult searchSkuInfoList(SkuLsParam skuLsParam){
        String query = makeQueryStringForSearch(skuLsParam);
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult=null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParam, searchResult);
        return skuLsResult;

    }


    /**
     * 使用api生成dsl查询语言
     * @param skuLsParam
     * @return
     */
    public String makeQueryStringForSearch(SkuLsParam skuLsParam){
        SearchSourceBuilder searchSourceBuilder =new SearchSourceBuilder();

        //复合查询
        BoolQueryBuilder boolQueryBuilder=new BoolQueryBuilder();

        if(skuLsParam.getKeyword()!=null){
            //关键词
            MatchQueryBuilder queryBuilder=new MatchQueryBuilder("skuName",skuLsParam.getKeyword());
            boolQueryBuilder.must(queryBuilder);

            //高亮
            HighlightBuilder highlightBuilder=new HighlightBuilder();
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");
            highlightBuilder.field("skuName");
            searchSourceBuilder.highlight(highlightBuilder);
        }

        if(skuLsParam.getCatalog3Id()!=null){
            //三级分类过滤
            TermQueryBuilder termQueryBuilder=new TermQueryBuilder("catalog3Id",skuLsParam.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }


        if(skuLsParam.getValueId()!=null&&skuLsParam.getValueId().length>0){
            //平台属性过滤
            for (int i = 0; i < skuLsParam.getValueId().length; i++) {
                String valueId= skuLsParam.getValueId()[i];
                TermQueryBuilder termQueryBuilder=new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }

        }

        searchSourceBuilder.query(boolQueryBuilder);


        //分页
        int from=(skuLsParam.getPageNo()-1)*skuLsParam.getPageSize();

        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParam.getPageSize());

        //排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //聚合
        TermsBuilder groupby_valueId = AggregationBuilders.terms("groupby_valueId").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_valueId);

        System.out.println("searchSourceBuilder.toString() = " + searchSourceBuilder.toString());
        return searchSourceBuilder.toString();
    }

    /**
     * 构建出需要的数据
     * @param skuLsParam
     * @param searchResult
     * @return
     */

    public SkuLsResult makeResultForSearch(SkuLsParam skuLsParam,SearchResult searchResult){
        SkuLsResult skuLsResult=new SkuLsResult();
        List<SkuLsInfo> skuLsInfoList=new ArrayList<>(skuLsParam.getPageSize());
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source;
            if (hit.highlight!=null) {
                List<String> list = hit.highlight.get("skuName");
                String skuNameHl = list.get(0);
                skuLsInfo.setSkuName(skuNameHl);
            }
            skuLsInfoList.add(skuLsInfo);
        }

        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        skuLsResult.setTotal( searchResult.getTotal().intValue());



        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_valueId = aggregations.getTermsAggregation("groupby_valueId");
        List<TermsAggregation.Entry> buckets = groupby_valueId.getBuckets();

        List<String> valueIdList=new ArrayList<>(buckets.size());
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            valueIdList.add(valueId);
        }
        skuLsResult.setValueIdList(valueIdList);

        return skuLsResult;
    }


    public void incrHotScore(String skuId){

        Jedis jedis = redisUtil.getJedis();

        Double hotScore = jedis.zincrby("hotScore", 1, skuId);

        if(hotScore%10==0){
            updateHotScore(  skuId ,  hotScore);
        }

    }


    private void updateHotScore(String skuId ,Double hotScore){
        String updateJson="{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\":\""+hotScore+"\"\n" +
                "  }\n" +
                "}";


        Update update = new Update.Builder(updateJson).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
             jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ;
    }

}
