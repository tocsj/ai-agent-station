package com.tkck.test.resume;

import com.tkck.app.resume.ResumeWorkflowSchemaInitializer;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResumeWorkflowSchemaInitializerTest {

    @Test
    public void should_map_interview_step1_client_5201_to_model_2008() {
        ResumeWorkflowSchemaInitializer initializer = new ResumeWorkflowSchemaInitializer();
        JdbcTemplate mysql = mock(JdbcTemplate.class);
        ReflectionTestUtils.setField(initializer, "mysqlJdbcTemplate", mysql);

        when(mysql.queryForObject(anyString(), org.mockito.ArgumentMatchers.any(Class.class), org.mockito.ArgumentMatchers.any())).thenReturn(1);

        ReflectionTestUtils.invokeMethod(initializer, "syncAutoRuntimeClientModel");

        verify(mysql, atLeastOnce()).update(
                contains("source_id = ?"),
                eq("2008"),
                eq("5201"),
                eq("2008"));
        verify(mysql).update(contains("source_id IN ('5101', '5104', '5204')"));
    }
}
