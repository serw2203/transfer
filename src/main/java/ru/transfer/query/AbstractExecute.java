package ru.transfer.query;

/**
 *
 */
public abstract class AbstractExecute implements Execute {
    private Object[] params;

    @Override
    public abstract String sql();

    @Override
    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

}
