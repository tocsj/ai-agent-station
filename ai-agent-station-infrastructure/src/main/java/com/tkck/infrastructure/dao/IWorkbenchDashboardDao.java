package com.tkck.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface IWorkbenchDashboardDao {

    List<Map<String, Object>> queryAgentCardRows(@Param("range") String range);

    List<Map<String, Object>> queryRecentRunRows(@Param("limit") int limit);
}
