package com.tkck.test.resume;

import com.tkck.app.resume.ResumeWorkflowServiceImpl;
import com.tkck.domain.resume.adapter.repository.IResumeWorkflowRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResumeWorkflowServiceBehaviorTest {

    @Test
    public void shouldQueryOnlyOngoingInterviewAsActive() {
        ResumeWorkflowServiceImpl service = new ResumeWorkflowServiceImpl();
        IResumeWorkflowRepository repository = mock(IResumeWorkflowRepository.class);
        ReflectionTestUtils.setField(service, "resumeWorkflowRepository", repository);
        when(repository.queryActiveInterviewSessionId()).thenReturn(null);

        Assert.assertNull(service.queryActiveInterviewDetail());
        verify(repository).queryActiveInterviewSessionId();
    }
}
