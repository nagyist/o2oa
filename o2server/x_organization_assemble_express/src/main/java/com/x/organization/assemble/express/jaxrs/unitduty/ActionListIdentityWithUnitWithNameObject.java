package com.x.organization.assemble.express.jaxrs.unitduty;

import java.util.*;

import com.x.organization.core.entity.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonElement;
import com.x.base.core.container.EntityManagerContainer;
import com.x.base.core.container.factory.EntityManagerContainerFactory;
import com.x.base.core.project.annotation.FieldDescribe;
import com.x.base.core.project.cache.Cache.CacheKey;
import com.x.base.core.project.cache.CacheManager;
import com.x.base.core.project.gson.GsonPropertyObject;
import com.x.base.core.project.http.ActionResult;
import com.x.base.core.project.http.EffectivePerson;
import com.x.base.core.project.tools.ListTools;
import com.x.organization.assemble.express.Business;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

class ActionListIdentityWithUnitWithNameObject extends BaseAction {

	@SuppressWarnings("unchecked")
	ActionResult<List<Wo>> execute(EffectivePerson effectivePerson, JsonElement jsonElement) throws Exception {
		try (EntityManagerContainer emc = EntityManagerContainerFactory.instance().create()) {
			Wi wi = this.convertToWrapIn(jsonElement, Wi.class);
			ActionResult<List<Wo>> result = new ActionResult<>();
			Business business = new Business(emc);
			List<String> names = new ArrayList<>();
			List<String> units = new ArrayList<>();
			if (StringUtils.isNotEmpty(wi.getName())) {
				names.add(wi.getName());
			}
			if (ListTools.isNotEmpty(wi.getNameList())) {
				names.addAll(wi.getNameList());
			}
			if (StringUtils.isNotEmpty(wi.getUnit())) {
				units.add(wi.getUnit());
			}
			if (ListTools.isNotEmpty(wi.getUnitList())) {
				units.addAll(wi.getUnitList());
			}
			names = ListTools.trim(names, true, true);
			units = ListTools.trim(units, true, true);
			CacheKey cacheKey = new CacheKey(this.getClass(), names, units, wi.getRecursiveUnit());
			Optional<?> optional = CacheManager.get(cacheCategory, cacheKey);
			if (optional.isPresent()) {
				result.setData((List<Wo>) optional.get());
			} else {
				List<Wo> wos = this.list(business, names, units, wi.getRecursiveUnit());
				CacheManager.put(cacheCategory, cacheKey, wos);
				result.setData(wos);
			}
			return result;
		}
	}

	public static class Wi extends GsonPropertyObject {

		@FieldDescribe("组织职务名称")
		private String name;

		@FieldDescribe("组织")
		private String unit;

		@FieldDescribe("组织职务名称(多值)")
		private List<String> nameList;

		@FieldDescribe("组织(多值)")
		private List<String> unitList;

		@FieldDescribe("是否递归下级组织（默认false）")
		private Boolean recursiveUnit;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getUnit() {
			return unit;
		}

		public void setUnit(String unit) {
			this.unit = unit;
		}

		public List<String> getNameList() {
			return nameList;
		}

		public void setNameList(List<String> nameList) {
			this.nameList = nameList;
		}

		public List<String> getUnitList() {
			return unitList;
		}

		public void setUnitList(List<String> unitList) {
			this.unitList = unitList;
		}

		public Boolean getRecursiveUnit() {
			return recursiveUnit;
		}

		public void setRecursiveUnit(Boolean recursiveUnit) {
			this.recursiveUnit = recursiveUnit;
		}

	}

	public static class Wo extends com.x.base.core.project.organization.Identity {

		private String matchUnitName;
		private String matchUnitLevelName;
		private Integer matchUnitLevel;
		private String matchUnitDutyName;
		private String matchUnitDutyId;
		private Integer matchUnitDutyNumber;

		public String getMatchUnitName() {
			return matchUnitName;
		}

		public void setMatchUnitName(String matchUnitName) {
			this.matchUnitName = matchUnitName;
		}

		public String getMatchUnitLevelName() {
			return matchUnitLevelName;
		}

		public void setMatchUnitLevelName(String matchUnitLevelName) {
			this.matchUnitLevelName = matchUnitLevelName;
		}

		public Integer getMatchUnitLevel() {
			return matchUnitLevel;
		}

		public void setMatchUnitLevel(Integer matchUnitLevel) {
			this.matchUnitLevel = matchUnitLevel;
		}

		public String getMatchUnitDutyName() {
			return matchUnitDutyName;
		}

		public void setMatchUnitDutyName(String matchUnitDutyName) {
			this.matchUnitDutyName = matchUnitDutyName;
		}

		public String getMatchUnitDutyId() {
			return matchUnitDutyId;
		}

		public void setMatchUnitDutyId(String matchUnitDutyId) {
			this.matchUnitDutyId = matchUnitDutyId;
		}

		public Integer getMatchUnitDutyNumber() {
			return matchUnitDutyNumber;
		}

		public void setMatchUnitDutyNumber(Integer matchUnitDutyNumber) {
			this.matchUnitDutyNumber = matchUnitDutyNumber;
		}
	}

	private List<Wo> list(Business business, List<String> names, List<String> units, Boolean recursiveUnit) throws Exception {
		List<Wo> wos = new ArrayList<>();
		List<UnitDuty> os = new ArrayList<>();
		Map<String, Unit> unitMap = new HashMap<>();
		if(units.isEmpty()){
			EntityManager em = business.entityManagerContainer().get(UnitDuty.class);
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<UnitDuty> cq = cb.createQuery(UnitDuty.class);
			Root<UnitDuty> root = cq.from(UnitDuty.class);
			Predicate p = root.get(UnitDuty_.name).in(names);
			os = em.createQuery(cq.select(root).where(p)).getResultList();
		}else{
			List<Unit> unitList = business.unit().pick(units);
			if(!unitList.isEmpty()){
				units.clear();
				for(Unit unit : unitList){
					units.add(unit.getId());
					unitMap.put(unit.getId(), unit);
					if(BooleanUtils.isTrue(recursiveUnit)){
						List<Unit> subUnitList = business.unit().listSubNestedObject(unit);
						for (Unit subunit:subUnitList) {
							unitMap.put(subunit.getId(), subunit);
							units.add(subunit.getId());
						}
					}
				}
				units = ListTools.trim(units, true, true);
				EntityManager em = business.entityManagerContainer().get(UnitDuty.class);
				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<UnitDuty> cq = cb.createQuery(UnitDuty.class);
				Root<UnitDuty> root = cq.from(UnitDuty.class);
				Predicate p = root.get(UnitDuty_.name).in(names);
				p = cb.and(p, root.get(UnitDuty_.unit).in(units));
				os = em.createQuery(cq.select(root).where(p)).getResultList();
			}
		}

		for (UnitDuty o : os) {
			int i = 0;
			for (Identity identity : business.identity().pick(o.getIdentityList())) {
				Unit matchUnit = unitMap.get(o.getUnit());
				if(matchUnit == null){
					matchUnit = business.unit().pick(o.getUnit());
					unitMap.put(matchUnit.getId(), matchUnit);
				}
				Unit unit = unitMap.get(identity.getUnit());
				if(unit == null){
					unit = business.unit().pick(identity.getUnit());
					unitMap.put(unit.getId(), unit);
				}
				Person person = business.person().pick(identity.getPerson());
				Wo wo = this.convertToIdentity(matchUnit, unit, person, identity);
				i++;
				wo.setMatchUnitDutyNumber(i);
				wo.setMatchUnitDutyId(o.getId());
				wo.setMatchUnitDutyName(o.getName());
				wos.add(wo);
			}
		}
		return wos;
	}

	private Wo convertToIdentity(Unit matchUnit, Unit unit, Person person, Identity identity) throws Exception {
		Wo wo = new Wo();
		if (null != matchUnit) {
			wo.setMatchUnitLevelName(matchUnit.getLevelName());
			wo.setMatchUnitName(matchUnit.getName());
			wo.setMatchUnitLevel(matchUnit.getLevel());
		}
		if (null != unit) {
			wo.setUnit(unit.getDistinguishedName());
		}else{
			wo.setUnit(identity.getUnit());
		}
		if (null != person) {
			wo.setPerson(person.getDistinguishedName());
		}else{
			wo.setPerson(identity.getPerson());
		}
		if (null != identity) {
			wo.setDescription(identity.getDescription());
			wo.setDistinguishedName(identity.getDistinguishedName());
			wo.setName(identity.getName());
			wo.setOrderNumber(identity.getOrderNumber());
			wo.setUnique(identity.getUnique());
			wo.setUnitName(identity.getUnitName());
			wo.setUnitLevel(identity.getUnitLevel());
			wo.setUnitLevelName(identity.getUnitLevelName());
		}
		return wo;
	}

}
