package ru.transfer.query;

/**
 *
 */
public interface Execute {
    String sql();
    Object [] getParams();
}
