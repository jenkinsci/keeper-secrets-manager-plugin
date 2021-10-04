package io.jenkins.plugins.ksm.notation;

import io.jenkins.plugins.ksm.KsmSecret;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class KsmNotationItem {

    private String destination;
    private String envVar;
    private String filePath;
    private String notation;
    private String uid;
    private KsmFieldDataEnumType fieldDataType;
    private String fieldKey;
    private Boolean returnSingle;
    private Integer arrayIndex;
    private String dictKey;
    private Object value;
    private boolean allowFailure;
    private String error;

    @DataBoundConstructor
    public KsmNotationItem(
            String destination,
            String envVar,
            String filePath,
            String notation,
            String uid,
            KsmFieldDataEnumType fieldDataType,
            String fieldKey,
            Boolean returnSingle,
            Integer arrayIndex,
            String dictKey,
            boolean allowFailure) {

        if (returnSingle == null) {
            returnSingle = Boolean.TRUE;
        }
        if (arrayIndex == null) {
            arrayIndex = 0;
        }

        this.destination = destination;
        this.envVar = envVar;
        this.filePath = filePath;

        this.notation = notation;
        this.uid = uid;
        this.fieldDataType = fieldDataType;
        this.fieldKey = fieldKey;
        this.returnSingle = returnSingle;
        this.arrayIndex = arrayIndex;
        this.dictKey = dictKey;
        this.allowFailure = allowFailure;
    }
    // TODO - This is a stopgap for parser errors.
    public KsmNotationItem(
            String destination,
            String envVar,
            String filePath,
            String error
    ) {
        this.destination = destination;
        this.envVar = envVar;
        this.filePath = filePath;
        this.error = error;
    }

    public String getName() {
        return KsmSecret.buildSecretName(getDestination(), getEnvVar(), getFilePath());
    }

    public String getDestination() { return destination; }
    public String getEnvVar() {
        return envVar;
    }
    public String getFilePath() {
        return filePath;
    }
    public String getNotation() {
        return notation;
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
    public Object getValue() {
        return value;
    }
    public boolean getAllowFailure() {
        return allowFailure;
    }
    public String getError() {
        return error;
    }

    @DataBoundSetter
    public void setDestination(String destination) {
        this.destination = destination;
    }
    @DataBoundSetter
    public void setEnvVar(String envVar) {
        this.envVar = envVar;
    }
    @DataBoundSetter
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    @DataBoundSetter
    public void setNotation(String notation) {
        this.notation = notation;
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
    @DataBoundSetter
    public void setValue(Object value) {
        this.value = value;
    }
    @DataBoundSetter
    public void setAllowFailure(boolean allowFailure) {
        this.allowFailure = allowFailure;
    }
    @DataBoundSetter
    public void setError(String error) {
        this.error = error;
    }

    public boolean isDestinationEnvVar() {
        return destination.equals(KsmSecret.destinationEnvVar);
    }

    public void clearError() {
        this.error = null;
    }
}
