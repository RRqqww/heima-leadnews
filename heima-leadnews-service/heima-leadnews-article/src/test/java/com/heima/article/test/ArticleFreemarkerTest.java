package com.heima.article.test;


import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.ArticleApplication;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
public class ArticleFreemarkerTest {

    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;


    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Test
    public void createStaticUrlTest() throws Exception {
        //1.获取文章内容
        /*ApArticleContent apArticleContent =
                apArticleContentMapper.
                        selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, 1390536764510310401L));
*/
        LambdaQueryWrapper<ApArticleContent> apArticleContentLambdaQueryWrapper = new LambdaQueryWrapper<>();
        apArticleContentLambdaQueryWrapper.eq(ApArticleContent::getArticleId,1383827911810011137L);
        ApArticleContent apArticleContent = apArticleContentMapper.selectOne(apArticleContentLambdaQueryWrapper);


        if(apArticleContent != null && StringUtils.isNotBlank(apArticleContent.getContent())){
            //2.文章内容通过freemarker生成html文件（这个代码可以看笔记里面的生成静态文件的代码，在这一部分笔记的最后一点）
            StringWriter out = new StringWriter();  // 这个输出流是要输出的文件
            Template template = configuration.getTemplate("article.ftl");

            Map<String, Object> params = new HashMap<>();
            // 这里写content是要和前端写的一致
            params.put("content", JSONArray.parseArray(apArticleContent.getContent()));
            template.process(params, out);   // 这个地方的out本来要写输出的磁盘地址

            InputStream is = new ByteArrayInputStream(out.toString().getBytes());

            //3.把html文件上传到minio中
            String path = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId() + ".html", is);

            //4.修改ap_article表，保存static_url字段
            ApArticle article = new ApArticle();
            article.setId(apArticleContent.getArticleId());   // 这个id用于下面的跟据id进行更新
            article.setStaticUrl(path);
            apArticleMapper.updateById(article);

        }
    }
}