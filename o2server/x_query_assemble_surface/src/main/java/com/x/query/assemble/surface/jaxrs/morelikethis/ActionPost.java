package com.x.query.assemble.surface.jaxrs.morelikethis;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

import com.google.gson.JsonElement;
import com.hankcs.lucene.HanLPAnalyzer;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.project.bean.tuple.Pair;
import com.x.base.core.project.config.Config;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.query.assemble.surface.Business;
import com.x.query.core.express.assemble.surface.jaxrs.morelikethis.ActionPostWi;
import com.x.query.core.express.assemble.surface.jaxrs.morelikethis.ActionPostWo;
import com.x.query.core.express.index.Indexs;

import io.swagger.v3.oas.annotations.media.Schema;

class ActionPost extends BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionPost.class);

    ActionResult<Wo> execute(EffectivePerson effectivePerson, JsonElement jsonElement) throws Exception {

        LOGGER.info("execute:{}.", effectivePerson::getDistinguishedName);

        ActionResult<Wo> result = new ActionResult<>();
        Wo wo = new Wo();
        result.setData(wo);

        Wi wi = this.convertToWrapIn(jsonElement, Wi.class);

        String category = wi.getCategory();

        List<String> readers = new ArrayList<>();
        Optional<Pair<String, String>> optionalSummary = Optional.empty();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
            Business business = new Business(emc);
            optionalSummary = this.summary(business, category, wi.getFlag());
            if (optionalSummary.isEmpty()) {
                return result;
            }
            builder.add(new TermQuery(new Term(Indexs.FIELD_ID, optionalSummary.get().first())),
                    BooleanClause.Occur.MUST_NOT);
            String person = business.index().who(effectivePerson, wi.getPerson());
            readers = business.index().determineReaders(person, Indexs.CATEGORY_SEARCH, "");
        }
        Optional<Query> readersQuery = Indexs.readersQuery(readers);
        Optional<Query> filterCategoryQuery = this.filterCategoryQuery(wi.getFilterCategory());
        Optional<Query> filterTypeQuery = this.filterTypeQuery(wi.getFilterType());
        Optional<Query> filterKeyQuery = this.filterKeyQuery(wi.getFilterKey());
        Stream.of(filterCategoryQuery, filterTypeQuery, filterKeyQuery).filter(Optional::isPresent)
                .forEach(o -> builder.add(o.get(), BooleanClause.Occur.MUST));
        if (readersQuery.isPresent()) {
            builder.add(readersQuery.get(), BooleanClause.Occur.MUST);
        }
        Optional<Directory> optional = Indexs.directory(Indexs.CATEGORY_SEARCH, Indexs.KEY_ENTIRE, true);
        if (optional.isEmpty()) {
            return result;
        }
        try (DirectoryReader reader = DirectoryReader.open(optional.get())) {
            IndexSearcher searcher = new IndexSearcher(reader);
            MoreLikeThis moreLikeThis = new MoreLikeThis(reader);
            moreLikeThis.setMinTermFreq(1);
            moreLikeThis.setMinDocFreq(1);
            moreLikeThis.setAnalyzer(new HanLPAnalyzer());
            try (StringReader sr = new StringReader(optionalSummary.get().second())) {
                builder.add(moreLikeThis.like(Indexs.FIELD_SUMMARY, sr), BooleanClause.Occur.MUST);
            }
            Query query = builder.build();
            LOGGER.debug("moreLikeThis query:{}.", query.toString());
            TopDocs topDocs = searcher.search(query, this.moreLikeThisCount(wi.getCount()));
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            Float moreLikeThisScoreThreshold = Config.query().index().getMoreLikeThisScoreThreshold();
            if (null != scoreDocs) {
                for (ScoreDoc scoreDoc : scoreDocs) {
                    if (scoreDoc.score < moreLikeThisScoreThreshold) {
                        break;
                    }
                    org.apache.lucene.document.Document document = reader.document(scoreDoc.doc);
                    ActionPostWo.WoMoreLikeThis woMoreLikeThis = new ActionPostWo.WoMoreLikeThis();
                    woMoreLikeThis.setTitle(
                            Indexs.<String>indexableFieldValue(document.getFields(Indexs.FIELD_TITLE),
                                    Indexs.FIELD_TYPE_STRING));
                    woMoreLikeThis.setFlag(
                            Indexs.<String>indexableFieldValue(document.getFields(Indexs.FIELD_ID),
                                    Indexs.FIELD_TYPE_STRING));
                    woMoreLikeThis.setCategory(
                            Indexs.<String>indexableFieldValue(document.getFields(Indexs.FIELD_CATEGORY),
                                    Indexs.FIELD_TYPE_STRING));
                    woMoreLikeThis.setType(
                            Indexs.<String>indexableFieldValue(document.getFields(Indexs.FIELD_TYPE),
                                    Indexs.FIELD_TYPE_STRING));
                    woMoreLikeThis.setKey(
                            Indexs.<String>indexableFieldValue(document.getFields(Indexs.FIELD_KEY),
                                    Indexs.FIELD_TYPE_STRING));
                    woMoreLikeThis.setCreateTime(
                            Indexs.<Date>indexableFieldValue(document.getFields(Indexs.FIELD_CREATETIME),
                                    Indexs.FIELD_TYPE_DATE));
                    woMoreLikeThis.setUpdateTime(
                            Indexs.<Date>indexableFieldValue(document.getFields(Indexs.FIELD_UPDATETIME),
                                    Indexs.FIELD_TYPE_DATE));
                    woMoreLikeThis.setCreatorPerson(Indexs.<String>indexableFieldValue(
                            document.getFields(Indexs.FIELD_CREATORPERSON),
                            Indexs.FIELD_TYPE_STRING));
                    woMoreLikeThis.setCreatorUnit(Indexs.<String>indexableFieldValue(
                            document.getFields(Indexs.FIELD_CREATORUNIT),
                            Indexs.FIELD_TYPE_STRING));
                    woMoreLikeThis.setKey(document.get(Indexs.FIELD_KEY));
                    woMoreLikeThis.setScore(scoreDoc.score);
                    wo.getMoreLikeThisList().add(woMoreLikeThis);
                }
                wo.setMaxScore(wo.getMoreLikeThisList().stream().map(ActionPostWo.WoMoreLikeThis::getScore)
                        .max(Comparator.naturalOrder()).orElse(0f));
                wo.setMinScore(wo.getMoreLikeThisList().stream().map(ActionPostWo.WoMoreLikeThis::getScore)
                        .min(Comparator.naturalOrder()).orElse(0f));
            }
        }
        return result;
    }

    @Schema(name = "com.x.query.assemble.surface.jaxrs.mlt.ActionPost$Wi")
    public class Wi extends ActionPostWi {

        private static final long serialVersionUID = -4646809016933808952L;

    }

    @Schema(name = "com.x.query.assemble.surface.jaxrs.mlt.ActionPost$Wo")
    public class Wo extends ActionPostWo {

    }

}