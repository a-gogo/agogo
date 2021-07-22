/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.puzzle.itc.mobiliar.business.deploy.entity;

import ch.puzzle.itc.mobiliar.business.shakedown.entity.ShakedownTestFilterTypes;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder.Default;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static java.lang.Enum.valueOf;

@Builder(builderClassName = "CustomFilterBuilder", builderMethodName = "internalBuilder")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomFilter {

    @Getter
    private String filterDisplayName;

    @Getter
    private String deploymentTableColumnName;

    @Getter
    private boolean isNullableFilter = false;

    @Setter
    @Getter
    private String joiningTableQuery;

    @Getter
    private FilterType filterType;

    @Setter
    @Getter
    private ComparatorFilterOption comparatorSelection;

    @Setter
    @Getter
    private boolean isSelected;

    @Getter
    private boolean alwaysAutoComplete;

    @Setter
    @Getter
    @Default
    private List<String> dropDownItems = new ArrayList<>();

    @Setter
    @Getter
    @Default
    private Map<String, String> dropDownItemsMap = new HashMap<>();

    @Setter
    private Class<? extends Enum> enumType;

    @Default
    private Object value = null;

    private Long filterIdentifikationNumber;

    private List<ComparatorFilterOption> comparatorSelectionList;

    @Setter
    @Getter
    private ComparatorFilterOption selectedComparatorFilterOption;

    public static CustomFilterBuilder builder(DeploymentFilterTypes deploymentFilterTypes) {
        ComparatorFilterOption defaultComparator = ComparatorFilterOption.eq;

        return new Builder().filterType(deploymentFilterTypes.getFilterType())
                .filterDisplayName(deploymentFilterTypes.getFilterDisplayName())
                .deploymentTableColumnName(deploymentFilterTypes.getFilterTabColumnName())
                .joiningTableQuery(deploymentFilterTypes.getFilterTableJoining())
                .comparatorSelection(defaultComparator);
    }

    public static CustomFilterBuilder builder(ShakedownTestFilterTypes shakedownTestFilterTypes) {
        ComparatorFilterOption defaultComperator = ComparatorFilterOption.eq;
        return new Builder().filterType(shakedownTestFilterTypes.getFilterType())
                .filterDisplayName(shakedownTestFilterTypes.getFilterDisplayName())
                .deploymentTableColumnName(shakedownTestFilterTypes.getFilterTabColumnName())
                .comparatorSelection(defaultComperator);
    }

    public static class Builder extends CustomFilterBuilder {
        Builder() {
            super();
        }
        @Override
        public CustomFilter build() {
            CustomFilter filter = super.build();
            filter.init();
            return filter;
        }
    }

    private void init() {

        this.dropDownItems = new ArrayList<>();
        this.filterIdentifikationNumber = System.currentTimeMillis();
        this.isSelected = true;
        this.joiningTableQuery = joiningTableQuery == null ? "" : joiningTableQuery;
        if (isIntegerType()) {
            // value muss mit 0 initialisiert werden falls Integer type (sonst
            // fkt rendering nicht!)
            this.value = 0;
        } else if (isBooleanType()) {
            // value soll mit true initialisiert werden falls Boolean type
            this.value = true;

            this.dropDownItems.add("true");
            this.dropDownItems.add("false");
        }
        else if (isDateType() || isLabeledDateType()) {
            this.value = new Date();
        }
        else {
            this.value = "";
        }
    }

    public List<ComparatorFilterOption> getComparatorSelectionList() {
        if (comparatorSelectionList == null) {
            comparatorSelectionList = new ArrayList<>();
            for (ComparatorFilterOption filterType : ComparatorFilterOption.values()) {
                comparatorSelectionList.add(filterType);
            }
        }
        return comparatorSelectionList;
    }

    public List<ComparatorFilterOption> getTypedComparatorSelectionList() {
        List<ComparatorFilterOption> result = new ArrayList<>();
        for (ComparatorFilterOption comperatorfilteroption : getComparatorSelectionList()) {
            if (isBooleanType()) {
                if (comperatorfilteroption.equals(ComparatorFilterOption.eq)) {
                    result.add(comperatorfilteroption);
                }
            } else if (isStringType() || isEnumType()) {
                if (comperatorfilteroption.equals(ComparatorFilterOption.eq)) {
                    result.add(comperatorfilteroption);
                }
            } else {
                result.add(comperatorfilteroption);
            }
        }
        return result;
    }

    public void setAlwaysAutoComplete(boolean alwaysAutoComplete) {
        this.alwaysAutoComplete = alwaysAutoComplete;
    }

    @Deprecated
    public void setValue(String filterValue) {
        if (StringUtils.isEmpty(filterValue)) {
            this.value = null;
        }
        else {
            if (isBooleanType()) {
                this.value = Boolean.valueOf(filterValue);
            }
            else if (isStringType() || isEnumType()) {
                this.value = filterValue;
            }
            else if (isIntegerType()) {
                try {
                    this.value = Integer.valueOf(filterValue.trim());
                }
                catch (NumberFormatException e) {
                    this.value = null;
                }
            }
            // Date type is handled differently: The JSF component directly sets its value directly through setDateValue
            else if (isLabeledDateType()) {
                this.value = CustomFilter.convertStringToDate(filterValue);
            }
        }
    }

    public void setValueFromRest(String filterValue) {
        if (StringUtils.isEmpty(filterValue)) {
            this.value = null;
        }
        else {
            switch (filterType) {
                case booleanType:
                    this.value = Boolean.valueOf(filterValue);
                    break;
                case StringType:
                    this.value = filterValue;
                    break;
                case ENUM_TYPE:
                    if (this.getFilterDisplayName().equals(DeploymentFilterTypes.DEPLOYMENT_STATE.getFilterDisplayName())) {
                        this.enumType = DeploymentState.class;
                    }
                    this.value = filterValue;
                    break;
                case IntegerType:
                    try {
                        this.value = Integer.valueOf(filterValue.trim());
                    }
                    catch (NumberFormatException e) {
                        this.value = null;
                    }
                    break;
                case DateType:
                case LabeledDateType:
                    this.value = new Date(Long.valueOf(filterValue));
                    break;
                default:
                    this.value = StringUtils.EMPTY;
                    break;
            }
        }
    }

    public Integer getIntegerValue() {
        if (isIntegerType()) {
            return (Integer) value;
        }
        else {
            return null;
        }
    }

    public boolean isIntegerType() {
        return filterType.equals(FilterType.IntegerType);
    }

    public Boolean getBooleanValue() {
        if (isBooleanType()) {
            return (Boolean) value;
        }
        else {
            return null;
        }
    }

    public boolean isBooleanType() {
        return filterType.equals(FilterType.booleanType);
    }

    public Date getDateValue() {
        if (isDateType() || isLabeledDateType()) {
            return (Date) value;
        }
        else {
            return null;
        }
    }

    public void setDateValue(Date date) {
        if (isDateType()) {
            value = date;
        }
        else {
            value = null;
        }
    }

    public boolean isDateType() {
        return filterType.equals(FilterType.DateType);
    }

    public boolean isLabeledDateType() {
        return filterType.equals(FilterType.LabeledDateType);
    }

    public String getStringValue() {
        if (isStringType()) {
            return (String) value;
        }
        else {
            return null;
        }
    }

    public boolean isStringType() {
        return filterType.equals(FilterType.StringType);
    }

    public boolean isSpecialFilterType() {
        return filterType.equals(FilterType.SpecialFilterType);
    }

    public String getValue() {
        if (value != null) {
            if (isLabeledDateType()) {
                return convertDateToString(getDateValue());
            }
            return value.toString();
        }
        else {
            return null;
        }
    }

    public boolean isEnumType() {
        return filterType.equals(FilterType.ENUM_TYPE);
    }

    public Enum<?> getEnumValue() {
        try {
            return valueOf(enumType, value.toString());
        } catch (IllegalArgumentException e) {
            if (enumType.getSimpleName().equals(DeploymentState.class.getSimpleName())) {
                DeploymentState[] deploymentStates = (DeploymentState[]) enumType.getEnumConstants();
                for (DeploymentState deploymentState : deploymentStates) {
                    if (deploymentState.getDisplayName().equals(value.toString())) {
                        return deploymentState;
                    }
                }
            }
        }
        return null;
    }

    public String getSqlComperator() {
        String result = null;
        if (comparatorSelection != null) {
            if (this.isStringType()) {
                result = comparatorSelection.getSqlStringComperator();
            }
            else if (this.isBooleanType()) {
                result = comparatorSelection.getSqlBoolComperator();
            }
            else {
                result = comparatorSelection.getSqlNumComperator();
            }
        }
        return result;
    }

    public String getParameterName() {
        return this.filterDisplayName.replaceAll(" ", "");
    }

    public boolean hasValidNullValue() {
        return isNullableFilter && value == null;
    }

    public boolean isValidForSqlQuery() {
        return (!filterType.equals(FilterType.SpecialFilterType)
                && (hasValidNullValue() || (value != null && !value.toString().trim().isEmpty()))
                && comparatorSelection != null);
    }

    public boolean hasDropDownItems() {
        return !dropDownItems.isEmpty();
    }

    public void setEnumType(Class<? extends Enum> enumType) {
        this.enumType = enumType;
    }

    public boolean hasDropDownItemsMap() {
        return !dropDownItemsMap.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        CustomFilter filter;

        if (obj instanceof CustomFilter) {
            filter = (CustomFilter) obj;
        }
        else {
            return false;
        }

        if ((this.filterIdentifikationNumber == null && filter.filterIdentifikationNumber != null)
                || (this.filterIdentifikationNumber != null
                        && !this.filterIdentifikationNumber.equals(filter.filterIdentifikationNumber))) {
            return false;
        }

        if ((this.filterType == null && filter.filterType != null)
                || (this.filterType != null && !this.filterType.equals(filter.filterType))) {
            return false;
        }

        if ((this.filterDisplayName == null && filter.filterDisplayName != null)
                || (this.filterDisplayName != null && !this.filterDisplayName.equals(filter.filterDisplayName))) {
            return false;
        }

        if ((this.value == null && filter.value != null) || (this.value != null && !this.value.equals(filter.value))) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return this.filterIdentifikationNumber.hashCode();
    }

    public void setNullableFilter(boolean isNullableFilter) {
        // only stringtype may have null as value
        if (isStringType()) {
            this.isNullableFilter = isNullableFilter;
        }
    }

    public static String convertDateToString(Date date) {
        return String.valueOf(date.getTime());
    }

    public static Date convertStringToDate(String date) {
        return new Date(Long.valueOf(date));
    }

}
