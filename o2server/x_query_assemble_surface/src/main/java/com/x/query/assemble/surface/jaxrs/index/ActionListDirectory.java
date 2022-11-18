package com.x.query.assemble.surface.jaxrs.index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.entity.JpaObject;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.cache.Cache.CacheKey;
import com.x.base.core.project.cache.CacheManager;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.logger.Logger;
import com.x.base.core.project.logger.LoggerFactory;
import com.x.cms.core.entity.AppInfo;
import com.x.processplatform.core.entity.element.Application;
import com.x.query.assemble.surface.Business;
import com.x.query.core.express.index.Indexs;

import io.swagger.v3.oas.annotations.media.Schema;

class ActionListDirectory extends BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionListDirectory.class);

    private static final List<String> APPLICATIONATTRIBUTES = Stream
            .<String>of(JpaObject.id_FIELDNAME, Application.name_FIELDNAME).collect(Collectors.toUnmodifiableList());

    private static final List<String> APPINFOATTRIBUTES = Stream
            .<String>of(JpaObject.id_FIELDNAME, AppInfo.appName_FIELDNAME).collect(Collectors.toUnmodifiableList());

    @SuppressWarnings("unchecked")
    ActionResult<List<Wo>> execute(EffectivePerson effectivePerson) throws Exception {
        LOGGER.info("execute:{}.", effectivePerson::getDistinguishedName);
        ActionResult<List<Wo>> result = new ActionResult<>();
        CacheKey cacheKey = new CacheKey(this.getClass());
        Optional<?> optional = CacheManager.get(cacheCategory, cacheKey);
        if (optional.isPresent()) {
            result.setData((List<Wo>) optional.get());
        } else {
            try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
                List<Wo> wos = new ArrayList<>();
                Business business = new Business(emc);
                wos.add(this.processPlatform(business));
                wos.add(this.cms(business));
                CacheManager.put(cacheCategory, cacheKey, wos);
                result.setData(wos);
            }
        }
        return result;
    }

    private Wo processPlatform(Business business) throws Exception {
        EntityManagerContainer emc = business.entityManagerContainer();
        List<Application> os = emc.fetchAll(Application.class, APPLICATIONATTRIBUTES);
        List<WoKey> keys = os.stream().map(o -> new WoKey(o.getName(), o.getId()))
                .sorted(Comparator.nullsLast(Comparator.comparing(WoKey::getName))).collect(
                        ArrayList::new, List::add,
                        List::addAll);
        Wo wo = new Wo();
        wo.setCategory(Indexs.CATEGORY_PROCESSPLATFORM);
        wo.setKeyList(keys);
        return wo;
    }

    private Wo cms(Business business) throws Exception {
        EntityManagerContainer emc = business.entityManagerContainer();
        List<AppInfo> os = emc.fetchAll(AppInfo.class, APPINFOATTRIBUTES);
        List<WoKey> keys = os.stream().map(o -> new WoKey(o.getAppName(), o.getId()))
                .sorted(Comparator.nullsLast(Comparator.comparing(WoKey::getName))).collect(
                        ArrayList::new, List::add,
                        List::addAll);
        Wo wo = new Wo();
        wo.setCategory(Indexs.CATEGORY_CMS);
        wo.setKeyList(keys);
        return wo;
    }

    @Schema(name = "com.x.query.assemble.surface.jaxrs.index.ActionListDirectory$Wo")
    public class Wo extends GsonPropertyObject {

        @FieldDescribe("分类")
        @Schema(description = "分类")
        private String category;

        @FieldDescribe("标识")
        @Schema(description = "标识")
        private List<WoKey> keyList = new ArrayList<>();

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public List<WoKey> getKeyList() {
            return keyList;
        }

        public void setKeyList(List<WoKey> keyList) {
            this.keyList = keyList;
        }

    }

    @Schema(name = "com.x.query.assemble.surface.jaxrs.index.ActionListDirectory$WoKey")
    public class WoKey extends GsonPropertyObject {

        public WoKey(String name, String key) {
            this.name = name;
            this.key = key;
        }

        @FieldDescribe("名称.")
        @Schema(description = "名称.")
        private String name;

        @FieldDescribe("标识.")
        @Schema(description = "标识.")
        private String key;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

    }

}