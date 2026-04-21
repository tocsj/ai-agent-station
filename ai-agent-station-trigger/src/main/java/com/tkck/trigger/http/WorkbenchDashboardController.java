package com.tkck.trigger.http;

import com.tkck.api.response.Response;
import com.tkck.domain.workbench.model.entity.WorkbenchDashboardEntity;
import com.tkck.domain.workbench.service.IWorkbenchDashboardService;
import com.tkck.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workbench")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class WorkbenchDashboardController {

    @Resource
    private IWorkbenchDashboardService workbenchDashboardService;

    @GetMapping("/dashboard")
    public Response<WorkbenchDashboardEntity> dashboard(@RequestParam(value = "range", required = false, defaultValue = "7d") String range,
                                                        @RequestParam(value = "recentLimit", required = false, defaultValue = "10") Integer recentLimit) {
        return Response.<WorkbenchDashboardEntity>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(workbenchDashboardService.queryDashboard(range, recentLimit))
                .build();
    }
}
