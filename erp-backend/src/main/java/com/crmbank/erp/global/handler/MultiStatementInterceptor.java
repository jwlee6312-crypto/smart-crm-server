package com.crmbank.erp.global.handler;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;

@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
@Component
public class MultiStatementInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        
        try {
            Field mappedStatementField = statementHandler.getClass().getDeclaredField("mappedStatement");
            mappedStatementField.setAccessible(true);
            MappedStatement mappedStatement = (MappedStatement) mappedStatementField.get(statementHandler);

            String id = mappedStatement.getId();
            if (id != null && (id.endsWith("_DELETE") || id.endsWith("_DELETE_CASCADE"))) {
                Field statementTypeField = MappedStatement.class.getDeclaredField("statementType");
                statementTypeField.setAccessible(true);
                statementTypeField.set(mappedStatement, StatementType.STATEMENT);
            }
        } catch (Exception ignored) {}

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
