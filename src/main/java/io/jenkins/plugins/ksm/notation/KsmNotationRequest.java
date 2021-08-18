package io.jenkins.plugins.ksm.notation;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class KsmNotationRequest {

    private String uid;
    private KsmFieldDataEnumType fieldDataType;
    private String fieldKey;
    private Boolean returnSingle;
    private Integer arrayIndex;
    private String dictKey;

    @DataBoundConstructor
    public KsmNotationRequest(
            String uid,
            KsmFieldDataEnumType fieldDataType,
            String fieldKey,
            Boolean returnSingle,
            Integer arrayIndex,
            String dictKey) {

        if (returnSingle == null) {
            returnSingle = Boolean.TRUE;
        }
        if (arrayIndex == null) {
            arrayIndex = 0;
        }

        this.uid = uid;
        this.fieldDataType = fieldDataType;
        this.fieldKey = fieldKey;
        this.returnSingle = returnSingle;
        this.arrayIndex = arrayIndex;
        this.dictKey = dictKey;
    }

    public String getUid() {
        return uid;
    }
    public KsmFieldDataEnumType getFieldDataType() {
        return fieldDataType;
    }
    public String getFieldKey() {
        return fieldKey;
    }
    public Boolean getReturnSingle() {
        return returnSingle;
    }
    public Integer getArrayIndex() {
        return arrayIndex;
    }
    public String getDictKey() {
        return dictKey;
    }

    @DataBoundSetter
    public void setUid(String uid) {
        this.uid = uid;
    }
    @DataBoundSetter
    public void setFieldDataType(KsmFieldDataEnumType fieldDataType) {
        this.fieldDataType = fieldDataType;
    }
    @DataBoundSetter
    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }
    @DataBoundSetter
    public void setReturnSingle(Boolean returnSingle) {
        this.returnSingle = returnSingle;
    }
    @DataBoundSetter
    public void setArrayIndex(Integer arrayIndex) {
        this.arrayIndex = arrayIndex;
    }
    @DataBoundSetter
    public void setDictKey(String dictKey) {
        this.dictKey = dictKey;
    }
}
