package com.atguigu.gmall.search;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GmallSearchServiceApplicationTests {

    @Reference
    SkuService skuService;//查询sql

    @Autowired
    JestClient jestClient;

    @Test
    public void contextLoads() throws IOException {
       /* // 查询数据 mysql
        List<PmsSkuInfo> pmsSkuInfos = new ArrayList<>();
        pmsSkuInfos  = skuService.getAllSku();
        //转化为es的数据结构
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos){
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
            BeanUtils.copyProperties(pmsSkuInfo,pmsSearchSkuInfo);
            pmsSearchSkuInfos.add(pmsSearchSkuInfo);
        }
        //导入es
        for (PmsSearchSkuInfo pmsSearchSkuInfo:pmsSearchSkuInfos)
        //new Index.Builder(skuLsInfo 数据源).index(index_name_gmall 库).type(type_name_gmall 表).id(skuLsInfo.getId() 主键).build();
        {
            Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall0105").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId()).build();
            jestClient.execute(put);
        }*/

        get();
    }


    // dsl工具的使用
    public void get() throws IOException {

        // jest的dsl工具
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        // filter  多个条件就new TermQueryBuilder多个 并集使用TermsQueryBuilder对象
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId","43");
        boolQueryBuilder.filter(termQueryBuilder);
        // must
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName","华为");
        boolQueryBuilder.must(matchQueryBuilder);
        // query
        searchSourceBuilder.query(boolQueryBuilder);
        // from
        searchSourceBuilder.from(0);
        // size
        searchSourceBuilder.size(20);
        // highlight
        searchSourceBuilder.highlight(null);


       /* // highlight 高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);

        // sort 排序
        searchSourceBuilder.sort("id", SortOrder.DESC);
*/
        // aggs 聚合
        /*TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);
*/


        String dslStr = searchSourceBuilder.toString();

        System.err.println(dslStr);


        // 用api执行复杂查询
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

        Search search = new Search.Builder(dslStr).addIndex("gmall0105").addType("PmsSkuInfo").build();

        SearchResult execute = jestClient.execute(search);

        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);

        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo source = hit.source;

            pmsSearchSkuInfos.add(source);
        }

        System.out.println(pmsSearchSkuInfos.size());
    }



}
